#!/bin/bash
#set -x

ONYX_USERNAME="[username here]"
ONYX_API_KEY="[api key here]"

BASE_API_URL="https://onyx.koli.ch/api/v1"

# Onyx directory to sweep, relative to the user's home directory (no leading/trailing
# slash) - recurses into subdirectories, same as downloader.sh's download_directory.
# e.g. "videos/2021" walks https://onyx.koli.ch/api/v1/browse/$ONYX_USERNAME/videos/2021
REMOTE_PATH="path/to/foo/bar"

VIDEO_EXT_PATTERN='\.(mp4|mov|mpg|mpeg|avi|webm|wmv|m4v|mkv)$'

# Sprite-sheet generation tuning - same defaults as the former standalone screencaps.sh.
MAX_FRAMES=400
TILE_W=340
SCENE_THRESHOLD=0.3
MIN_INTERVAL=15

# Smallest plausible sprite sheet, in bytes. Anything smaller is treated as a failed or
# truncated render rather than a real image - a real sheet of hundreds of tiles runs to
# hundreds of KB, so this only ever catches genuinely broken output.
MIN_SHEET_BYTES=1024

# Optional first argument: only process videos accessed within this many days. Omitting
# it processes every video regardless of last-access time.
#
# Worth using on a large archive: reading a video that has aged into S3
# Intelligent-Tiering's Infrequent or Archive Instant tier promotes it back to Frequent
# Access and restarts its 30/90-day countdown, so a full sweep temporarily inflates the
# storage bill for everything it touches. Videos accessed recently are already in
# Frequent Access, so processing those costs little or nothing extra.
MAX_ACCESS_AGE_DAYS="${1:-}"

if [ -n "$MAX_ACCESS_AGE_DAYS" ] && ! [[ "$MAX_ACCESS_AGE_DAYS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Usage: $(basename "$0") [max-access-age-days]"
  echo "  $(basename "$0") 30   process only videos accessed in the last 30 days"
  echo "  $(basename "$0")      process every video, no last-access filter"
  exit 1
fi

# Generates a scene-change-sampled sprite sheet for one local video file. Inlined
# from the former standalone screencaps.sh so this script has no dependency on
# another file in this directory. Logs and returns non-zero on failure (e.g. no
# frames selected, or ffmpeg itself fails) rather than exit()ing - a bad video here
# shouldn't kill the whole batch.
#   $1 - local input video path
#   $2 - local output sprite sheet path
generate_sprite_sheet() {
  local INPUT="$1"
  local OUTPUT="$2"

  # Select on scene change OR at least MIN_INTERVAL seconds since the last selected
  # frame, whichever comes first. Commas inside the expression are backslash-escaped
  # because ffmpeg's filtergraph syntax also uses commas to separate chained filters.
  local SELECT_EXPR="isnan(prev_selected_t)+gt(scene\\,${SCENE_THRESHOLD})+gt(t-prev_selected_t\\,${MIN_INTERVAL})"

  # Pass 1: count how many frames this actually selects (capped at MAX_FRAMES) so the
  # tile grid is sized to fit -- no wasted black cells, nothing dropped.
  local FRAME_COUNT
  FRAME_COUNT=$(ffprobe -v quiet -f lavfi \
      -i "movie='${INPUT}',select='${SELECT_EXPR}'" \
      -show_entries frame=pts_time -of csv=p=0 | head -n "${MAX_FRAMES}" | wc -l)

  if [ "${FRAME_COUNT}" -eq 0 ]; then
    echo "No frames selected for sprite sheet: ${INPUT}"
    return 1
  fi

  # Roughly-square grid sized to the actual frame count.
  local COLS
  COLS=$(awk -v n="${FRAME_COUNT}" 'BEGIN { c=int(sqrt(n)); if (c*c<n) c++; print c }')
  local ROWS=$(( (FRAME_COUNT + COLS - 1) / COLS ))

  echo "Selected ${FRAME_COUNT} frames -> ${COLS}x${ROWS} grid"

  # Pass 2: same selection, scaled to native aspect ratio (not square), tiled.
  # -nostdin: without it, ffmpeg reads stdin for live interactive commands - this
  # function runs inside walk_directory's `while read ... done < <(...)` loop below,
  # which is also reading stdin, so without -nostdin ffmpeg steals bytes meant for
  # that loop and chokes on them as a malformed command.
  # -y: overwrite the output without prompting. A leftover sheet from a previous failed
  # video would otherwise make ffmpeg stop at an "overwrite?" prompt it can't read an
  # answer to, thanks to -nostdin above.
  if ! ffmpeg -hide_banner -loglevel error -nostdin -y \
      -i "${INPUT}" \
      -vf "select='${SELECT_EXPR}',scale=${TILE_W}:-2,tile=${COLS}x${ROWS}:padding=4:color=black" \
      -fps_mode vfr \
      -frames:v 1 \
      -q:v 3 \
      "${OUTPUT}"; then
    echo "ffmpeg failed generating sprite sheet: ${INPUT}"
    rm -f "${OUTPUT}"
    return 1
  fi

  # ffmpeg exiting 0 isn't proof it produced a usable image - a disk that fills up
  # mid-write (or a run that's aborted partway) can leave behind a zero-byte or
  # truncated sheet that would otherwise sail through and get uploaded as a broken
  # image. Verify the file is actually there and plausibly sized before calling this a
  # success, and delete the bad artifact so nothing downstream can pick it up.
  local SHEET_BYTES=0
  if [ -f "${OUTPUT}" ]; then
    SHEET_BYTES=$(wc -c < "${OUTPUT}" | tr -d '[:space:]')
  fi

  if (( SHEET_BYTES < MIN_SHEET_BYTES )); then
    echo "Sprite sheet missing or too small (${SHEET_BYTES} bytes, expected >= ${MIN_SHEET_BYTES}), skipping upload: ${OUTPUT}"
    rm -f "${OUTPUT}"
    return 1
  fi

  echo "Saved ${FRAME_COUNT}-frame sheet (${COLS}x${ROWS}, ${SHEET_BYTES} bytes) to ${OUTPUT}"
}

# Downloads one video already sitting at the given Onyx path, generates a screencaps
# sprite sheet for it, and uploads that sheet to the Metadata API - the whole
# per-video flow, folded into a function so it can run once per video found while
# walking a directory. Deliberately tolerant of per-file failures (logs and returns
# rather than aborting, hence no `set -e` in this script) - one bad video shouldn't
# stop the rest of the batch.
#   $1 - remote path, already percent-encoded, including the leading username segment
#   $2 - the file's basename (for local temp file naming and logging)
process_video() {
  local ENCODED_REMOTE_PATH="$1"
  local FILE_NAME="$2"

  # Skip videos that already have at least one screencaps object, so re-running this
  # script over the same directory doesn't re-download and re-process (and, per S3
  # Intelligent-Tiering, re-warm the storage tier for) videos already handled by an
  # earlier run.
  #
  # Note "?type=" here but "?key=" on the PUT below - not a typo. On the list route
  # "type" is a prefix used to discover what exists under a kind; every route that
  # names one exact object (this PUT, the DELETE, metadata-download) takes "key".
  local EXISTING_ITEM_COUNT
  EXISTING_ITEM_COUNT=$(
    curl -s \
      -H"Authorization: Onyx $ONYX_API_KEY" \
      "$BASE_API_URL/metadata/$ENCODED_REMOTE_PATH?type=screencaps" \
      | jq -r '.items | length'
  )

  if [ "$EXISTING_ITEM_COUNT" != "0" ]; then
    echo "Already has screencaps, skipping: $FILE_NAME"
    return
  fi

  local PRESIGNED_DOWNLOAD_URL
  PRESIGNED_DOWNLOAD_URL=$(
    curl -si \
      -H"Authorization: Onyx $ONYX_API_KEY" \
      "$BASE_API_URL/download/$ENCODED_REMOTE_PATH" \
      | sed -En 's/^[Ll]ocation: (.*)$/\1/p' | tr -d '\r'
  )

  if [ -z "$PRESIGNED_DOWNLOAD_URL" ]; then
    echo "Error downloading, skipping: $FILE_NAME"
    return
  fi

  echo "Downloading -> $FILE_NAME"
  curl -# -o "$FILE_NAME" \
    --connect-timeout 120 \
    "$PRESIGNED_DOWNLOAD_URL"

  local SPRITE_SHEET="v1.jpg"

  if ! generate_sprite_sheet "$FILE_NAME" "$SPRITE_SHEET"; then
    echo "Error generating sprite sheet, skipping: $FILE_NAME"
    rm -f "$FILE_NAME"
    return
  fi

  local METADATA_KEY="screencaps/$SPRITE_SHEET"

  echo "Requesting presigned metadata upload URL for key: $METADATA_KEY"
  local METADATA_RESPONSE
  METADATA_RESPONSE=$(
    curl -s -X PUT \
      -H"Authorization: Onyx $ONYX_API_KEY" \
      "$BASE_API_URL/metadata/$ENCODED_REMOTE_PATH?key=$METADATA_KEY"
  )

  local PRESIGNED_UPLOAD_URL
  PRESIGNED_UPLOAD_URL=$(echo "$METADATA_RESPONSE" | jq -r '.presignedUploadUrl')

  if [ -z "$PRESIGNED_UPLOAD_URL" ] || [ "$PRESIGNED_UPLOAD_URL" = "null" ]; then
    echo "Error requesting presigned metadata upload URL, skipping: $METADATA_RESPONSE"
    rm -f "$FILE_NAME" "$SPRITE_SHEET"
    return
  fi

  echo "Uploading $SPRITE_SHEET -> metadata key $METADATA_KEY"

  # curl exits 0 on HTTP errors by default, so a rejected upload (expired or malformed
  # presigned URL, S3 signature mismatch, etc.) would otherwise look like success and
  # get reported as "Done". Capture the status code via -w and check it explicitly,
  # alongside curl's own exit status for transport-level failures. -# still writes its
  # progress meter to stderr, so only the status code lands on stdout here.
  local UPLOAD_STATUS
  UPLOAD_STATUS=$(
    curl -# -X PUT -T "$SPRITE_SHEET" \
      --connect-timeout 120 \
      -o /dev/null \
      -w '%{http_code}' \
      "$PRESIGNED_UPLOAD_URL"
  )
  local CURL_EXIT=$?

  if [ "$CURL_EXIT" -ne 0 ]; then
    echo "Error uploading sprite sheet (curl exit $CURL_EXIT), skipping: $FILE_NAME"
    rm -f "$FILE_NAME" "$SPRITE_SHEET"
    return
  fi

  if [ "$UPLOAD_STATUS" -lt 200 ] || [ "$UPLOAD_STATUS" -ge 300 ]; then
    echo "Error uploading sprite sheet (HTTP $UPLOAD_STATUS), skipping: $FILE_NAME"
    rm -f "$FILE_NAME" "$SPRITE_SHEET"
    return
  fi

  rm -f "$FILE_NAME" "$SPRITE_SHEET"

  echo "Done: $FILE_NAME -> metadata key $METADATA_KEY (HTTP $UPLOAD_STATUS)"
}

# Recursively walks a remote directory, calling process_video() for every video file
# found, one at a time - same browse/jq listing pattern as downloader.sh's
# download_directory, but dispatching to screencaps processing instead of a plain
# download.
#   $1 - remote path to browse, already percent-encoded, including the leading username segment, no leading slash
walk_directory() {
  local ENCODED_REMOTE_PATH="$1"

  local LISTING_JSON
  LISTING_JSON=$(
    curl -s \
      -H"Authorization: Onyx $ONYX_API_KEY" \
      "$BASE_API_URL/browse/$ENCODED_REMOTE_PATH"
  )

  # The last-access cutoff is applied in the jq below rather than in bash because jq's
  # own date functions are portable, while parsing an ISO-8601 timestamp with bash's
  # `date` differs between BSD/macOS and GNU/Linux - and this script is meant to run
  # in-region on Linux eventually. Notes on that filter:
  #   - Directories always pass, otherwise recursion would stop at the first one.
  #   - A file with no lastAccessedAt has never been read since it was written, so
  #     createdAt stands in as its last-access instant. That's the right call for the
  #     tiering cost this filter exists to control: a recently uploaded file is still
  #     in Frequent Access regardless of whether anyone has read it back yet.
  #   - fromdateiso8601 can't parse fractional seconds, and Onyx emits them at both
  #     microsecond and nanosecond precision, so they're stripped first.
  #   - An unparseable timestamp is treated as "doesn't pass" rather than being allowed
  #     to abort a long sweep.
  while IFS=$'\t' read -r CHILD_TYPE CHILD_PATH CHILD_NAME; do
    local CHILD_ENCODED_PATH="${CHILD_PATH#/}"

    if [ "$CHILD_TYPE" = "DIRECTORY" ]; then
      walk_directory "$CHILD_ENCODED_PATH"
      continue
    fi

    if ! echo "$CHILD_NAME" | grep -qiE "$VIDEO_EXT_PATTERN"; then
      continue
    fi

    process_video "$CHILD_ENCODED_PATH" "$CHILD_NAME"
  done < <(
    echo "$LISTING_JSON" | jq -r --argjson maxAgeDays "${MAX_ACCESS_AGE_DAYS:-0}" '
      (if $maxAgeDays > 0 then (now - ($maxAgeDays * 86400)) else 0 end) as $cutoff
      | .children[]
      | select(
          .metadata.type == "DIRECTORY"
          or $cutoff == 0
          or (
            ((.metadata.lastAccessedAt // .metadata.createdAt) // null) as $accessed
            | $accessed != null
              and (
                try (($accessed | sub("\\.[0-9]+Z$"; "Z") | fromdateiso8601) >= $cutoff)
                catch false
              )
          )
        )
      | "\(.metadata.type)\t\(.path)\t\(.name)"
    '
  )
}

ENCODED_REMOTE_PATH="$(perl -MURI::Escape -e 'print uri_escape($ARGV[0],"^A-Za-z0-9\-\._~\/");' "$ONYX_USERNAME/$REMOTE_PATH")"

if [ -n "$MAX_ACCESS_AGE_DAYS" ]; then
  echo "Sweeping $REMOTE_PATH (only videos accessed in the last $MAX_ACCESS_AGE_DAYS days)"
else
  echo "Sweeping $REMOTE_PATH (all videos, no last-access filter)"
fi

walk_directory "$ENCODED_REMOTE_PATH"
