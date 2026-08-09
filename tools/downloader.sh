#!/bin/bash
#set -x

ONYX_USERNAME="[username here]"
ONYX_API_KEY="[api key here]"

BASE_API_URL="https://onyx.koli.ch/api/v1"

# Path to browse, relative to the user's home directory (no leading/trailing slash).
# e.g. "path/to/foo/bar" browses https://onyx.koli.ch/api/v1/browse/$ONYX_USERNAME/path/to/foo/bar
REMOTE_PATH="path/to/foo/bar"

# Recursively downloads the contents of a remote directory into a local directory.
#   $1 - remote path to browse, already percent-encoded, including the leading username segment, no leading slash
#   $2 - local directory to download into
download_directory() {
  local ENCODED_REMOTE_PATH="$1"
  local LOCAL_DIR="$2"

  mkdir -p "$LOCAL_DIR"

  local LISTING_JSON
  LISTING_JSON=$(
    curl -s \
      -H"Authorization: Onyx $ONYX_API_KEY" \
      "$BASE_API_URL/browse/$ENCODED_REMOTE_PATH"
  )

  while IFS=$'\t' read -r CHILD_TYPE CHILD_PATH CHILD_NAME; do
    local CHILD_ENCODED_PATH="${CHILD_PATH#/}"

    if [ "$CHILD_TYPE" = "DIRECTORY" ]; then
      download_directory "$CHILD_ENCODED_PATH" "$LOCAL_DIR/$CHILD_NAME"
      continue
    fi

    local PRESIGNED_DOWNLOAD_URL
    PRESIGNED_DOWNLOAD_URL=$(
      curl -si \
        -H"Authorization: Onyx $ONYX_API_KEY" \
        "$BASE_API_URL/download/$CHILD_ENCODED_PATH" \
        | sed -En 's/^[Ll]ocation: (.*)$/\1/p' | tr -d '\r'
    )

    if [ ! -z "$PRESIGNED_DOWNLOAD_URL" ]; then
      echo "$LOCAL_DIR/$CHILD_NAME"

      curl -# -o "$LOCAL_DIR/$CHILD_NAME" \
        --connect-timeout 120 \
        "$PRESIGNED_DOWNLOAD_URL"
    else
      echo "Error downloading file, skipping: $LOCAL_DIR/$CHILD_NAME"
    fi
  done < <(echo "$LISTING_JSON" | jq -r '.children[] | "\(.metadata.type)\t\(.path)\t\(.name)"')
}

ENCODED_REMOTE_PATH="$(perl -MURI::Escape -e 'print uri_escape($ARGV[0],"^A-Za-z0-9\-\._~\/");' "$ONYX_USERNAME/$REMOTE_PATH")"

download_directory "$ENCODED_REMOTE_PATH" "$REMOTE_PATH"
