---
name: onyx
description: Use the Onyx HTTP API to browse, upload, download, delete and annotate files and directories in a self-hosted Onyx instance (S3 + DynamoDB file manager). Covers authentication with API keys, presigned upload/download URLs, the metadata/screencaps API, favorites, short links and API key management. Use whenever asked to list, find, fetch, upload, organise or inspect files stored in Onyx, or to script against onyx.koli.ch.
---

# Onyx API

Onyx stores file **bytes in S3** and **file metadata in DynamoDB**. The API never proxies
file content: uploads and downloads go **directly to S3 via presigned URLs**, and the API
only hands you the URL.

Read "Hazards" before any bulk or automated operation. Several endpoints have
cost and durability consequences that are not obvious from their shape.

## Setup

```bash
BASE="https://onyx.koli.ch/api/v1"
USER="<username>"
# Never inline the key in a command you print. Read it from the environment.
: "${ONYX_API_KEY:?set ONYX_API_KEY first}"
AUTH="Authorization: Onyx $ONYX_API_KEY"
```

Verify the credential before doing anything else. This is the only endpoint with no
side effects, which is why it exists:

```bash
curl -s -f -H "$AUTH" "$BASE/me"
# {"username":"...","type":"API","expiry":"..."}
```

`type` is `USER` (browser cookie session) or `API` (API key). A 401 means the key is
revoked, mistyped, or the header is malformed.

### Auth header gotchas

- The scheme is the literal string `Onyx ` — **capital O, lowercase nyx, one space**. It
  is matched case-sensitively. `onyx `, `ONYX `, `Bearer ` and `Onyx` (no space) all fail.
- Browser sessions use an `_onyx` cookie instead. Both resolve to the same `Session`.
- A wrong key gives **401**. A valid key for the wrong user gives **403**. A 404 can mean
  "not found" *or* "exists but private and you are not the owner" — do not use a 404 to
  conclude a path is free.

### Path encoding

Every route is `/<username>/<path>`, where `path` is percent-encoded **but slashes are
preserved** as separators:

```bash
encode() {
  perl -MURI::Escape -e 'print uri_escape($ARGV[0],"^A-Za-z0-9\-\._~\/");' "$1"
}
P="$(encode "$USER/videos/2021/my file (1).mp4")"
curl -s -f -H "$AUTH" "$BASE/file/$P"
```

Without this, any path containing a space, `#`, `?` or `&` breaks. Note the API's own
route regex permits `!$&'()*+,;=:@` unencoded, so over-encoding is safe but
under-encoding is not.

## Routes

`{u}` = username, `{p}` = encoded path.

### Reading

| Method | Route | Returns |
|---|---|---|
| GET | `/me` | `{username, type, expiry}` |
| GET | `/browse` | `{path, children[]}` — every user's home directory |
| GET | `/browse/{u}` | `{children[]}` — that user's home |
| GET | `/browse/{u}/{p}` | `{children[]}` — one directory listing |
| GET | `/favorites/{u}` | `{path, children[]}` |
| GET | `/favorites/{u}/{p}` | `{path, children[]}` |
| GET | `/file/{u}/{p}` | `ResourceResponse` — one file, no side effects |
| GET | `/directory/{u}[/{p}]` | `ResourceResponse` — one directory |

A `ResourceResponse` is:

```json
{
  "path": "/mark/videos/clip.mp4",
  "parent": "/mark/videos",
  "name": "clip.mp4",
  "metadata": {
    "type": "FILE",
    "visibility": "PRIVATE",
    "owner": "mark",
    "createdAt": "2026-01-01T00:00:00Z",
    "lastAccessedAt": "2026-10-01T00:00:00Z",
    "description": "...",
    "size": 12345,
    "sizeReadable": "12.1 KiB",
    "favorite": false
  }
}
```

`type` is `DIRECTORY` or `FILE`. `visibility` is `PUBLIC` or `PRIVATE`.
`lastAccessedAt`, `size`, `sizeReadable` and `favorite` are **nullable** — always guard
with `// default` in jq, and note that directory `size` is only populated once the
sizer job has run.

### Writing

| Method | Route | Query | Body | Returns |
|---|---|---|---|---|
| POST | `/file/{u}/{p}` | `recursive`, `overwrite` | `{size, description, visibility}` | `{presignedUploadUrl}` + `Location` header |
| PUT | `/file/{u}/{p}` | — | `{description, visibility, favorite}` | `ResourceResponse` |
| DELETE | `/file/{u}/{p}` | `permanent` | — | 204 |
| POST | `/directory/{u}/{p}` | `recursive` | `{description, visibility}` | `ResourceResponse` |
| PUT | `/directory/{u}[/{p}]` | — | `{description, visibility, favorite}` | `ResourceResponse` |
| DELETE | `/directory/{u}/{p}` | `permanent` | — | 204 |

**POST creates, PUT updates metadata.** This is the opposite of what the HTTP verbs
might suggest for uploads: you `POST` to get an upload URL for a *new* file, and `PUT`
only to change `description` / `visibility` / `favorite` on one that already exists.

### Downloading (two steps)

`GET /download/{u}/{p}` returns a **302 with the presigned S3 URL in `Location`**. It
does not return the bytes.

```bash
URL=$(curl -si -H "$AUTH" "$BASE/download/$P" \
  | sed -En 's/^[Ll]ocation: (.*)$/\1/p' | tr -d '\r')
[ -z "$URL" ] && { echo "no presigned URL"; exit 1; }
curl -f -o local.bin --connect-timeout 120 "$URL"
```

Optional `?nocache=true` bypasses the local cache. **Read the `lastAccessedAt` hazard
below before calling this in a loop.**

### Uploading (two steps)

```bash
UP=$(curl -s -f -X POST -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"size":12345,"description":"","visibility":"PRIVATE"}' \
  "$BASE/file/$P?recursive=true" | jq -r '.presignedUploadUrl')

STATUS=$(curl -s -X PUT -T local.bin -o /dev/null -w '%{http_code}' "$UP")
[ "$STATUS" -ge 300 ] && echo "upload failed: HTTP $STATUS"
```

`size` must be the real byte count — it is recorded in DynamoDB, not measured from the
S3 object. `recursive=true` creates missing parent directories.

For very large files there is also `/file-multipart/{u}/{p}` (POST to initiate, PUT to
complete with `{parts:[{partNumber, eTag}]}`).

### Metadata (sidecar objects, e.g. screencaps)

Arbitrary objects attached to a resource under a key namespace.

| Method | Route | Query | Meaning |
|---|---|---|---|
| GET | `/metadata/{u}/{p}` | `type=<prefix>` | list keys under a prefix → `{items:[{key}]}` |
| GET | `/metadata-download/{u}/{p}` | `key=<exact>` | 302 to presigned download URL |
| PUT | `/metadata/{u}/{p}` | `key=<exact>` | `{key, presignedUploadUrl}` |
| DELETE | `/metadata/{u}/{p}` | `key=<exact>` | delete one object |

**`type` vs `key` is the single easiest mistake to make here:**

- **`type`** names a **prefix** and works on the **list route only** (e.g. `type=screencaps`)
- **`key`** names **one exact object** and is required by download, PUT and DELETE
  (e.g. `key=screencaps/v1.jpg`)

Using `type` on a PUT, or `key` on the list, fails with a 400.

```bash
# does this video already have screencaps?
N=$(curl -s -f -H "$AUTH" "$BASE/metadata/$P?type=screencaps" | jq -r '.items | length')
# upload one
UP=$(curl -s -X PUT -H "$AUTH" "$BASE/metadata/$P?key=screencaps/v1.jpg" \
  | jq -r '.presignedUploadUrl')
curl -s -X PUT -T sheet.jpg -o /dev/null -w '%{http_code}' "$UP"
```

### API keys and short links

| Method | Route | Body | Returns |
|---|---|---|---|
| POST | `/apikey` | `{description}` | `{apiKey, keyHash, description, createdAt}` |
| GET | `/apikey` | — | `{keys:[{keyHash, description, createdAt}]}` |
| DELETE | `/apikey/{keyHash}` | — | 204 (`keyHash` is 64 hex chars) |
| POST | `/shortlink/{u}[/{p}]` | — | `{shortLinkUrl}` |

**The plaintext `apiKey` is returned exactly once, on creation.** It is never
retrievable again — only `keyHash` is stored. If you create one on the user's behalf,
hand it to them immediately and tell them it cannot be recovered.

Short links resolve at `GET /s/{code}` (8 hex chars) and redirect.

## Hazards

Read these before any loop, sweep or bulk operation.

**1. Every download writes to DynamoDB.** `GET /download/...` sets `lastAccessedAt` and
recomputes the stored cost on *every* fetch, then writes the resource plus its GSI
projections. The table runs on **provisioned capacity at the 25/25 free-tier limit**, and
measured peaks already reach ~9.9 of the base table's 10 WCU. Never poll this endpoint,
never call it to test whether a file exists (use `GET /file/...`, which is read-only),
and pace bulk sweeps.

**2. Reading an old file costs money.** The bucket uses S3 Intelligent-Tiering. Reading
an object that has aged into Infrequent Access or Archive Instant Access **promotes it
back to Frequent Access and restarts its 30/90-day countdown**, raising the storage bill
for everything touched. Do not bulk-read an old archive to build an index. Filter on
`lastAccessedAt` first and only read what is already warm.

**3. `?permanent=true` is irreversible.** Without it, delete writes an S3 delete marker
and is recoverable. With it, the object and its versions are gone. Always confirm with
the user before sending `permanent=true`, and never infer it.

**4. Downloads can be enormous.** Individual files run to 11+ GB. Return the presigned
URL or download to a file — never pipe response bodies into the conversation.

**5. `curl` exits 0 on HTTP errors.** Use `-f` so 4xx/5xx becomes a non-zero exit, and
check the exit code: **18** = partial file, **22** = HTTP error, **23** = write failure
(disk full). A truncated download otherwise looks like success. Verify the local byte
count against `metadata.size` from the listing.

**6. Never echo the API key.** Read it from `$ONYX_API_KEY`. Do not print it, do not
write it into a script that gets committed — a key has already had to be rotated once
after being committed to this repo.

## Errors

Failures return:

```json
{"statusCode": 400, "message": "Query parameter 'key' is required.", "stackTrace": "..."}
```

| Code | Meaning |
|---|---|
| 400 | bad or missing query parameter — check `type` vs `key` first |
| 401 | not authenticated: key revoked, mistyped, or malformed `Onyx ` prefix |
| 403 | authenticated as the wrong user for this path |
| 404 | not found **or** private and not yours |

## Recursive walk

The listing endpoint is not recursive. Walk it:

```bash
walk() {
  local P="$1" JSON
  JSON=$(curl -s -f -H "$AUTH" "$BASE/browse/$P") || { echo "browse failed: $P" >&2; return; }
  echo "$JSON" | jq -e 'has("children")' >/dev/null 2>&1 || { echo "bad response: $P" >&2; return; }

  # size before name: read's last variable absorbs trailing fields, so a name
  # containing a tab cannot corrupt the size. "-" not "" for a null size, because tab
  # is IFS whitespace and an empty field would silently shift every later column.
  while IFS=$'\t' read -r TYPE CPATH SIZE NAME; do
    if [ "$TYPE" = "DIRECTORY" ]; then
      walk "${CPATH#/}"
    else
      printf '%s\t%s\t%s\n' "$SIZE" "${CPATH#/}" "$NAME"
    fi
  done < <(echo "$JSON" | jq -r '.children[]
    | "\(.metadata.type)\t\(.path)\t\(.metadata.size // "-")\t\(.name)"')
}
walk "$(encode "$USER/videos")"
```

`tools/` in this repo has production versions of these patterns:
`downloader.sh`, `uploader.sh`, `multipart-uploader.sh`, `cleanup.sh` (recursive
permanent delete), and `screencaps.sh` (the fullest example — walk, skip-if-exists,
verified download, ffmpeg, metadata upload).
