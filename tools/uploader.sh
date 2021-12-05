#!/bin/bash
#set -x

ONYX_USERNAME="[username here]"
ONYX_API_KEY="[api key here]"

BASE_API_URL="https://onyx.koli.ch/api/v1"

find "$@" -type f -print0 | while IFS= read -r -d '' FILEPATH; do
  FILE="$(perl -MURI::Escape -e 'print uri_escape($ARGV[0],"^A-Za-z0-9\-\._~\/");' "$FILEPATH")"
  FILESIZE=$(wc -c < "$FILEPATH")

  PRESIGNED_UPLOAD_URL=$(
    curl -si -X POST \
      -H"Content-Type: application/json" \
      -H"Authorization: Onyx $ONYX_API_KEY" \
      --data '{"size":"'"$FILESIZE"'","description":"","visibility":"PRIVATE"}' \
      "$BASE_API_URL/file/$ONYX_USERNAME/$FILE?recursive=true" \
      | sed -En 's/^[Ll]ocation: (.*)$/\1/p' | tr -d '\r'
  )

  if [ ! -z "$PRESIGNED_UPLOAD_URL" ]; then
    echo "$FILEPATH"

    curl -# -X PUT -T "$FILEPATH" \
      --connect-timeout 120 \
      "$PRESIGNED_UPLOAD_URL" > /dev/null
  else
    echo "Error uploading file, skipping: $FILEPATH"
  fi
done
