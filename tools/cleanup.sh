#!/bin/bash
#set -x

ONYX_USERNAME="[username here]"
ONYX_API_KEY="[api key here]"

BASE_API_URL="https://onyx.koli.ch/api/v1"

# Path to clean up, relative to the user's home directory (no leading/trailing slash).
# Everything underneath is deleted depth-first, then the directory itself.
# e.g. "foo/bar"
REMOTE_PATH="foo/bar"

# Deletes are issued with permanent=true. On a versioning-enabled bucket that purges
# every version of every object rather than leaving a recoverable delete marker, so
# there is no undo - not through Onyx, and not through S3 version history either.
PERMANENT=true

# Dry run unless --force is passed. A recursive permanent delete driven by a path
# hardcoded at the top of a script is one typo away from destroying the wrong tree, so
# the default is to print what would be deleted and change nothing.
FORCE=false
if [ "${1:-}" = "--force" ]; then
  FORCE=true
elif [ -n "${1:-}" ]; then
  echo "Usage: $(basename "$0") [--force]"
  echo "  $(basename "$0")           list what would be deleted, change nothing"
  echo "  $(basename "$0") --force   actually delete"
  exit 1
fi

# Deletes one resource through the Onyx DELETE API, or just reports it in dry-run mode.
# Checks the HTTP status rather than trusting curl's exit code, which is 0 even on a
# 4xx/5xx - a favorite resource, for instance, comes back 400 and must not be counted
# as deleted.
#   $1 - "file" or "directory", selecting the API endpoint
#   $2 - remote path, already percent-encoded, including the leading username segment
#   $3 - human-readable path, for logging
delete_resource() {
  local KIND="$1"
  local ENCODED_PATH="$2"
  local LABEL="$3"

  if [ "$FORCE" != "true" ]; then
    echo "[dry-run] would delete $KIND: $LABEL"
    return
  fi

  local STATUS
  STATUS=$(
    curl -s -X DELETE \
      -H"Authorization: Onyx $ONYX_API_KEY" \
      -o /dev/null \
      -w '%{http_code}' \
      "$BASE_API_URL/$KIND/$ENCODED_PATH?permanent=$PERMANENT"
  )
  local CURL_EXIT=$?

  if [ "$CURL_EXIT" -ne 0 ]; then
    echo "ERROR deleting $KIND (curl exit $CURL_EXIT): $LABEL"
    return 1
  fi

  if [ "$STATUS" -lt 200 ] || [ "$STATUS" -ge 300 ]; then
    echo "ERROR deleting $KIND (HTTP $STATUS): $LABEL"
    return 1
  fi

  echo "Deleted $KIND: $LABEL"
}

# Walks a remote directory depth-first, deleting files as it encounters them and each
# directory only once its children are gone - same browse/jq listing pattern as
# downloader.sh, just post-order so nothing is removed out from under a pending listing.
#
# Note that Onyx's directory DELETE is already recursive server-side, so deleting the
# top of the tree in one call would also work. This walks instead to log every resource
# individually, which is worth the extra calls when the operation is irreversible.
#   $1 - remote path to browse, already percent-encoded, including the leading username segment, no leading slash
#   $2 - human-readable path, for logging
delete_directory_recursive() {
  local ENCODED_PATH="$1"
  local LABEL="$2"

  local LISTING_JSON
  LISTING_JSON=$(
    curl -s \
      -H"Authorization: Onyx $ONYX_API_KEY" \
      "$BASE_API_URL/browse/$ENCODED_PATH"
  )

  while IFS=$'\t' read -r CHILD_TYPE CHILD_PATH CHILD_NAME; do
    local CHILD_ENCODED_PATH="${CHILD_PATH#/}"

    if [ "$CHILD_TYPE" = "DIRECTORY" ]; then
      delete_directory_recursive "$CHILD_ENCODED_PATH" "$CHILD_PATH"
    else
      delete_resource "file" "$CHILD_ENCODED_PATH" "$CHILD_PATH"
    fi
  done < <(echo "$LISTING_JSON" | jq -r '.children[]? | "\(.metadata.type)\t\(.path)\t\(.name)"')

  # Children are gone, so the directory itself can go now.
  delete_resource "directory" "$ENCODED_PATH" "$LABEL"
}

ENCODED_REMOTE_PATH="$(perl -MURI::Escape -e 'print uri_escape($ARGV[0],"^A-Za-z0-9\-\._~\/");' "$ONYX_USERNAME/$REMOTE_PATH")"

echo "Target: /$ONYX_USERNAME/$REMOTE_PATH"

if [ "$FORCE" = "true" ]; then
  echo "Mode:   PERMANENT DELETE - every version purged, no undo"
  read -r -p "Delete this entire tree? [y/N] " CONFIRM
  if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "Aborted."
    exit 1
  fi
else
  echo "Mode:   dry run - nothing will be deleted (pass --force to execute)"
fi

echo

delete_directory_recursive "$ENCODED_REMOTE_PATH" "/$ONYX_USERNAME/$REMOTE_PATH"

echo
echo "Done."
