#!/bin/bash
#set -x

ONYX_USERNAME="[username here]"
ONYX_API_KEY="[api key here]"

BASE_API_URL="https://onyx.koli.ch/api/v1"

# Local cache directory to reconcile - whatever Onyx itself is pointed at through
# `onyx.local-cache.directory`. Every entry in there is a flat file named for the
# SHA-256 hex digest of the resource path it caches, with no subdirectories and no
# extension, so the directory on its own carries no record of what it holds. The
# only way to read it is to recompute those digests from the API and compare.
CACHE_DIR="/mnt/cache"

# Subtree to reconcile, relative to the user's home directory (no leading or
# trailing slash). Leave empty to reconcile every favorite the user has.
# e.g. "foo" reconciles everything at or below /$ONYX_USERNAME/spare
REMOTE_PATH="foo"

# Fixed byte collation for sort, comm and join. Without it a locale that collates
# case-insensitively or ignores punctuation makes comm silently disagree with sort
# about ordering, and the set differences below come back wrong rather than empty.
export LC_ALL=C

# Reporting is the default and touches nothing on disk. Deleting happens only when
# --cleanup is passed explicitly, so a bare run can never remove a cache file.
CLEANUP=0
while [ $# -gt 0 ]; do
  case "$1" in
    --cleanup)
      CLEANUP=1
      shift
      ;;
    *)
      echo "Usage: $(basename "$0") [--cleanup]"
      echo "  Reports differences between Onyx favorites and the local cache directory."
      echo "  Edit CACHE_DIR and REMOTE_PATH at the top of this script."
      echo
      echo "  Without --cleanup the script is read-only."
      echo "  With --cleanup every EXTRA and MISMATCH cache file is deleted, named one"
      echo "  per line as it goes. FOREIGN files are reported but never deleted."
      exit 1
      ;;
  esac
done

if [ "$ONYX_USERNAME" = "[username here]" ] || [ "$ONYX_API_KEY" = "[api key here]" ]; then
  echo "ERROR: set ONYX_USERNAME and ONYX_API_KEY at the top of this script." >&2
  exit 1
fi

for TOOL in curl jq perl find sort comm join; do
  if ! command -v "$TOOL" >/dev/null 2>&1; then
    echo "ERROR: required tool not found: $TOOL" >&2
    exit 1
  fi
done

# Digest::SHA and URI::Escape ship with perl, but a cut-down install can be missing
# them and the failure would otherwise surface as empty digests rather than an error,
# which would report every favorite as missing from the cache.
if ! perl -MDigest::SHA -MURI::Escape -e1 >/dev/null 2>&1; then
  echo "ERROR: perl is missing Digest::SHA and/or URI::Escape" >&2
  exit 1
fi

if [ ! -d "$CACHE_DIR" ]; then
  echo "ERROR: cache directory does not exist: $CACHE_DIR" >&2
  exit 1
fi

# BSD/macOS stat and GNU/Linux stat spell "size in bytes" differently and neither
# accepts the other's flag, so pick once here rather than probing per file.
if stat -f%z "$CACHE_DIR" >/dev/null 2>&1; then
  STAT_SIZE_FLAG="-f%z"
else
  STAT_SIZE_FLAG="-c%s"
fi

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

encode_path() {
  perl -MURI::Escape -e 'print uri_escape($ARGV[0],"^A-Za-z0-9\-\._~\/");' "$1"
}

# Fetches every favorited FILE at or below one path as `size<TAB>path` lines.
#
# This is one API call for the whole subtree, not a directory walk. Onyx backs the
# favorites route with a sparse DynamoDB GSI - `favorite-index`, partitioned on
# favoriteOwner and sorted on path - and queries it with begins_with against the
# path prefix, so a single request already returns every favorite at any depth
# underneath, filtered server-side to files. Walking /browse instead would cost one
# request per directory and read every row in the tree to find the handful of
# favorites in it.
#   $1 - path relative to the user's home directory, or "" for every favorite
fetch_favorites() {
  local SUBTREE="$1"

  local ROUTE="favorites/$ONYX_USERNAME"
  if [ -n "$SUBTREE" ]; then
    ROUTE="favorites/$(encode_path "$ONYX_USERNAME/$SUBTREE")"
  fi

  local BODY
  BODY="$(mktemp "$WORK_DIR/favorites.XXXXXX")"

  # Status is checked explicitly because curl exits 0 on a 4xx - a 404 body has no
  # .children at all, so jq would yield nothing and every favorite would silently
  # look like it was missing from the cache.
  local STATUS
  STATUS=$(
    curl -s \
      -H"Authorization: Onyx $ONYX_API_KEY" \
      -o "$BODY" \
      -w '%{http_code}' \
      "$BASE_API_URL/$ROUTE"
  )

  if [ "$STATUS" != "200" ]; then
    echo "ERROR fetching favorites (HTTP $STATUS): /$ONYX_USERNAME/$SUBTREE" >&2
    return 1
  fi

  # Size before path, so the path - which may contain anything except a tab - lands
  # in the final field and cannot shift the columns after it.
  jq -r '.children[]?
      | select(.metadata.type == "FILE")
      | "\(.metadata.size // 0)\t\(.path)"' < "$BODY"
}

# Prefixes each `size<TAB>path` line with the SHA-256 hex digest of the path, which
# is exactly the filename Onyx would cache it under: LocalCacheManager names entries
# `DigestUtils.sha256Hex(resource.getPath())`, hashing the full path including its
# leading slash and its extension, with no trailing newline.
#
# Perl reads bytes with no :utf8 layer here, so the digest covers the UTF-8 encoding
# of the path - the same bytes DigestUtils hashes. Adding -CS would decode first and
# silently produce different digests for any non-ASCII path.
#
# One perl process for the whole stream rather than a shasum per line, which for a
# few hundred favorites would be a few hundred forks.
hash_paths() {
  perl -MDigest::SHA=sha256_hex -ne '
      chomp;
      my ($size, $path) = split(/\t/, $_, 2);
      next unless defined $path && length $path;
      print sha256_hex($path), "\t", $size, "\t", $path, "\n";
  '
}

SCOPE_LABEL="/$ONYX_USERNAME"
if [ -n "$REMOTE_PATH" ]; then
  SCOPE_LABEL="/$ONYX_USERNAME/$REMOTE_PATH"
fi

echo "Favorites:  $SCOPE_LABEL"
echo "Cache dir:  $CACHE_DIR"
if [ "$CLEANUP" -eq 1 ]; then
  echo "Mode:       cleanup - EXTRA and MISMATCH cache files will be deleted"
else
  echo "Mode:       report only"
fi
echo

if ! fetch_favorites "$REMOTE_PATH" > "$WORK_DIR/scoped.raw"; then
  exit 1
fi
hash_paths < "$WORK_DIR/scoped.raw" | sort > "$WORK_DIR/scoped.tsv"

# The "not a favorite" comparison has to run against every favorite the user has,
# not just the ones in scope. With REMOTE_PATH narrowed to "spare", a cache entry
# for a favorite living in some other directory would otherwise be reported as
# backing no favorite at all.
if [ -n "$REMOTE_PATH" ]; then
  if ! fetch_favorites "" > "$WORK_DIR/all.raw"; then
    exit 1
  fi
  hash_paths < "$WORK_DIR/all.raw" | sort > "$WORK_DIR/all.tsv"
else
  cp "$WORK_DIR/scoped.tsv" "$WORK_DIR/all.tsv"
fi

cut -f1 "$WORK_DIR/scoped.tsv" | sort -u > "$WORK_DIR/scoped-hashes"
cut -f1 "$WORK_DIR/all.tsv"    | sort -u > "$WORK_DIR/all-hashes"

# Only a name that is exactly 64 hex characters is a cache entry. Anything else got
# there some other way - a .DS_Store from a copy via macOS, an interrupted rsync
# temp file - and is reported on its own rather than counted as an orphan.
( cd "$CACHE_DIR" && find . -maxdepth 1 -type f ) \
  | sed 's|^\./||' \
  | sort > "$WORK_DIR/cache-all"
grep -E  '^[0-9a-f]{64}$' "$WORK_DIR/cache-all" | sort > "$WORK_DIR/cache-hashes" || true
grep -Ev '^[0-9a-f]{64}$' "$WORK_DIR/cache-all"        > "$WORK_DIR/cache-foreign" || true

comm -23 "$WORK_DIR/scoped-hashes" "$WORK_DIR/cache-hashes" > "$WORK_DIR/missing-hashes"
comm -12 "$WORK_DIR/scoped-hashes" "$WORK_DIR/cache-hashes" > "$WORK_DIR/present-hashes"
comm -23 "$WORK_DIR/cache-hashes"  "$WORK_DIR/all-hashes"   > "$WORK_DIR/extra-hashes"

# Favorited, but nothing in the cache under its digest.
join -t"$(printf '\t')" -j1 "$WORK_DIR/missing-hashes" "$WORK_DIR/scoped.tsv" \
  | awk -F'\t' '{ printf "MISSING\t%s\t%s\n", $2, $3 }' \
  | sort -t"$(printf '\t')" -k2,2nr > "$WORK_DIR/report-missing"

# Cached, but the cache file is not the size the API says the resource is. Worth
# separating from "missing" because a short cache file is worse than an absent one:
# it is a truncated copy that will be served as though it were complete.
#
# A mismatch can also mean the recorded size is wrong rather than the file, since
# Onyx stores the size the uploader declared rather than measuring the S3 object.
: > "$WORK_DIR/report-mismatch"

# What --cleanup deletes, as `digest<TAB>bytes<TAB>reason<TAB>detail`. The report
# lines above are for reading; this carries the digest, which is the actual filename.
: > "$WORK_DIR/delete-list"

while IFS=$'\t' read -r HASH API_SIZE RPATH; do
  CACHED_SIZE="$(stat $STAT_SIZE_FLAG "$CACHE_DIR/$HASH" 2>/dev/null)"
  if [ -z "$CACHED_SIZE" ]; then
    continue
  fi
  if [ "$CACHED_SIZE" != "$API_SIZE" ]; then
    printf 'MISMATCH\tcached=%s\tapi=%s\t%s\n' \
      "$CACHED_SIZE" "$API_SIZE" "$RPATH" >> "$WORK_DIR/report-mismatch"
    printf '%s\t%s\tMISMATCH\t%s\n' \
      "$HASH" "$CACHED_SIZE" "$RPATH" >> "$WORK_DIR/delete-list"
  fi
done < <(join -t"$(printf '\t')" -j1 "$WORK_DIR/present-hashes" "$WORK_DIR/scoped.tsv")

# In the cache, but matching no favorite anywhere in the account. Note this does not
# by itself mean stale: Onyx's cache is populated on download, not from favorites, so
# these are files that were fetched at some point and are not favorites now. They are
# reclaimable space, not necessarily garbage.
: > "$WORK_DIR/report-extra"
EXTRA_BYTES=0
while read -r HASH; do
  SZ="$(stat $STAT_SIZE_FLAG "$CACHE_DIR/$HASH" 2>/dev/null)"
  [ -z "$SZ" ] && SZ=0
  EXTRA_BYTES=$(( EXTRA_BYTES + SZ ))
  printf 'EXTRA\t%s\t%s\n' "$SZ" "$HASH" >> "$WORK_DIR/report-extra"
  printf '%s\t%s\tEXTRA\t%s\n' "$HASH" "$SZ" "(no favorite)" >> "$WORK_DIR/delete-list"
done < "$WORK_DIR/extra-hashes"

awk '{ printf "FOREIGN\t%s\n", $0 }' "$WORK_DIR/cache-foreign" > "$WORK_DIR/report-foreign"

cat "$WORK_DIR/report-missing" \
    "$WORK_DIR/report-mismatch" \
    "$WORK_DIR/report-extra" \
    "$WORK_DIR/report-foreign"

SCOPED_COUNT=$(wc -l < "$WORK_DIR/scoped-hashes" | tr -d '[:space:]')
ALL_COUNT=$(wc -l < "$WORK_DIR/all-hashes" | tr -d '[:space:]')
CACHE_COUNT=$(wc -l < "$WORK_DIR/cache-hashes" | tr -d '[:space:]')
MISSING_COUNT=$(wc -l < "$WORK_DIR/missing-hashes" | tr -d '[:space:]')
PRESENT_COUNT=$(wc -l < "$WORK_DIR/present-hashes" | tr -d '[:space:]')
MISMATCH_COUNT=$(wc -l < "$WORK_DIR/report-mismatch" | tr -d '[:space:]')
EXTRA_COUNT=$(wc -l < "$WORK_DIR/extra-hashes" | tr -d '[:space:]')
FOREIGN_COUNT=$(wc -l < "$WORK_DIR/cache-foreign" | tr -d '[:space:]')

RECLAIMED_BYTES=0
DELETED_COUNT=0
FAILED_COUNT=0

if [ "$CLEANUP" -eq 1 ]; then
  # EXTRA means "in the cache but matching no favorite anywhere in the account",
  # which is only meaningful if the account-wide fetch actually returned favorites.
  # A 200 carrying an empty children array would make every single cache entry look
  # orphaned and take the whole directory with it, so refuse rather than trust it.
  if [ "$ALL_COUNT" -eq 0 ] && [ "$CACHE_COUNT" -gt 0 ]; then
    echo >&2
    echo "ERROR: the API reported zero favorites account-wide while the cache holds" >&2
    echo "       $CACHE_COUNT entries, so every entry would look orphaned." >&2
    echo "       Refusing to delete anything." >&2
    exit 1
  fi

  if [ ! -s "$WORK_DIR/delete-list" ]; then
    echo
    echo "Nothing to delete."
  else
    echo
    while IFS=$'\t' read -r HASH SZ REASON DETAIL; do
      # A MISMATCH entry is a favorite, so deleting it is a repair rather than a
      # reclaim: the next download of that resource refills the cache from S3
      # instead of serving the short copy that is there now.
      rm -f -- "$CACHE_DIR/$HASH"
      if [ -e "$CACHE_DIR/$HASH" ]; then
        printf 'FAILED\t%s\t%s\t%s\n' "$REASON" "$HASH" "$DETAIL" >&2
        FAILED_COUNT=$(( FAILED_COUNT + 1 ))
      else
        printf 'DELETED\t%s\t%s\t%s\t%s\n' "$REASON" "$SZ" "$HASH" "$DETAIL"
        DELETED_COUNT=$(( DELETED_COUNT + 1 ))
        RECLAIMED_BYTES=$(( RECLAIMED_BYTES + SZ ))
      fi
    done < "$WORK_DIR/delete-list"
  fi
fi

EXTRA_GIB=$(awk -v b="$EXTRA_BYTES" 'BEGIN { printf "%.1f", b / 1073741824 }')
RECLAIMED_GIB=$(awk -v b="$RECLAIMED_BYTES" 'BEGIN { printf "%.1f", b / 1073741824 }')

echo
echo "Favorites in scope:      $SCOPED_COUNT"
echo "  in cache:              $PRESENT_COUNT"
echo "  missing from cache:    $MISSING_COUNT"
echo "  wrong size in cache:   $MISMATCH_COUNT"
echo "Favorites account-wide:  $ALL_COUNT"
echo "Cache entries:           $CACHE_COUNT"
echo "  not a favorite:        $EXTRA_COUNT (${EXTRA_GIB} GiB)"
echo "  not cache files:       $FOREIGN_COUNT"

if [ "$CLEANUP" -eq 1 ]; then
  echo "Deleted:                 $DELETED_COUNT (${RECLAIMED_GIB} GiB reclaimed)"
  if [ "$FAILED_COUNT" -gt 0 ]; then
    echo "  failed to delete:      $FAILED_COUNT"
  fi
elif [ "$(( EXTRA_COUNT + MISMATCH_COUNT ))" -gt 0 ]; then
  echo
  echo "Re-run with --cleanup to delete the EXTRA and MISMATCH files listed above."
fi

echo
echo "Done."

# A delete that could not happen is reported above but would otherwise leave a clean
# exit status behind it, which hides a permissions problem from anything calling this.
if [ "$FAILED_COUNT" -gt 0 ]; then
  exit 1
fi
