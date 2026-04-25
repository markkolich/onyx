#!/bin/bash
#set -x

ONYX_USERNAME="[username here]"
ONYX_API_KEY="[api key here]"

BASE_API_URL="https://onyx.koli.ch/api/v1"

# Requires: bash, curl, dd, jq, perl (URI::Escape)

find "$@" -type f -print0 | while IFS= read -r -d '' FILEPATH; do
  FILE="$(perl -MURI::Escape -e 'print uri_escape($ARGV[0],"^A-Za-z0-9\-\._~\/");' "$FILEPATH")"
  FILESIZE=$(wc -c < "$FILEPATH")

  # Step 1: Initiate the multipart upload. The server creates the DynamoDB resource record,
  # calls S3 CreateMultipartUpload, and returns the uploadId plus a presigned URL per part.
  INITIATE_RESPONSE=$(
    curl -s -X POST \
      -H "Content-Type: application/json" \
      -H "Authorization: Onyx $ONYX_API_KEY" \
      --data '{"size":'"$FILESIZE"',"description":"","visibility":"PRIVATE"}' \
      "$BASE_API_URL/file-multipart/$ONYX_USERNAME/$FILE?recursive=true"
  )

  UPLOAD_ID="$(echo "$INITIATE_RESPONSE" | jq -r '.uploadId')"
  PART_SIZE="$(echo "$INITIATE_RESPONSE" | jq -r '.partSize')"
  PART_COUNT="$(echo "$INITIATE_RESPONSE" | jq '.parts | length')"

  if [ -z "$UPLOAD_ID" ] || [ "$UPLOAD_ID" = "null" ]; then
    echo "Error initiating multipart upload, skipping: $FILEPATH"
    continue
  fi

  # URL-encode the uploadId; AWS uploadIds may contain +, / and = characters.
  UPLOAD_ID_ENCODED="$(perl -MURI::Escape -e 'print uri_escape($ARGV[0]);' "$UPLOAD_ID")"

  echo "Uploading: $FILEPATH ($FILESIZE bytes, $PART_COUNT parts of $PART_SIZE bytes each)"

  WORK_DIR="$(mktemp -d)"
  PARTS_JSON="[]"
  UPLOAD_FAILED=0

  # Step 2: Carve the file into chunks with dd and PUT each chunk to its presigned S3 URL.
  # dd uses block-based addressing: skip=i skips i*PART_SIZE bytes into the source file,
  # so part i (zero-based) starts at byte offset i*PART_SIZE. The last part is naturally
  # truncated to however many bytes remain.
  for (( i=0; i<PART_COUNT; i++ )); do
    PART_NUMBER="$(echo "$INITIATE_RESPONSE" | jq -r ".parts[$i].partNumber")"
    PRESIGNED_URL="$(echo "$INITIATE_RESPONSE" | jq -r ".parts[$i].presignedUrl")"
    PART_FILE="$WORK_DIR/part-$PART_NUMBER"

    dd if="$FILEPATH" bs="$PART_SIZE" skip="$i" count=1 of="$PART_FILE" 2>/dev/null

    # S3 returns the ETag for the uploaded part in the response headers. Collect it so we
    # can pass it back in the complete request. The ETag value includes surrounding quotes
    # (e.g. "d41d8cd98f00b204e9800998ecf8427e"), which S3 requires verbatim in CompleteMultipartUpload.
    ETAG="$(
      curl -# -X PUT \
        --connect-timeout 120 \
        -T "$PART_FILE" \
        -D - \
        "$PRESIGNED_URL" \
        | grep -i "^etag:" | sed 's/^[Ee][Tt][Aa][Gg]:[[:space:]]*//' | tr -d '\r'
    )"

    rm -f "$PART_FILE"

    if [ -z "$ETAG" ]; then
      echo "  Error uploading part $PART_NUMBER, aborting."
      UPLOAD_FAILED=1
      break
    fi

    echo "  Part $PART_NUMBER/$PART_COUNT: ETag $ETAG"

    PARTS_JSON="$(echo "$PARTS_JSON" | jq \
      --argjson pn "$PART_NUMBER" \
      --arg et "$ETAG" \
      '. + [{"partNumber": $pn, "eTag": $et}]')"
  done

  rm -rf "$WORK_DIR"

  # On any part failure, abort the multipart upload so S3 reclaims storage for the uploaded
  # parts and the pre-created DynamoDB record is removed.
  if [ "$UPLOAD_FAILED" -eq 1 ]; then
    echo "  Aborting multipart upload: $FILEPATH"
    curl -s -X DELETE \
      -H "Authorization: Onyx $ONYX_API_KEY" \
      "$BASE_API_URL/file-multipart/$ONYX_USERNAME/$FILE?uploadId=$UPLOAD_ID_ENCODED" \
      > /dev/null
    continue
  fi

  # Step 3: Complete the multipart upload by submitting the ordered list of part ETags.
  # The server calls S3 CompleteMultipartUpload, which assembles the parts into the final object.
  COMPLETE_BODY="$(jq -n --argjson parts "$PARTS_JSON" '{"parts": $parts}')"

  HTTP_STATUS="$(
    curl -s -o /dev/null -w "%{http_code}" -X PUT \
      -H "Content-Type: application/json" \
      -H "Authorization: Onyx $ONYX_API_KEY" \
      --data "$COMPLETE_BODY" \
      "$BASE_API_URL/file-multipart/$ONYX_USERNAME/$FILE?uploadId=$UPLOAD_ID_ENCODED"
  )"

  if [ "$HTTP_STATUS" = "200" ]; then
    echo "Done: $FILEPATH"
  else
    echo "Error completing upload (HTTP $HTTP_STATUS): $FILEPATH"
  fi
done
