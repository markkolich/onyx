#!/bin/bash
#set -x

INPUT="$1"
OUTPUT="${2:-screencaps.jpg}"
COLS=${3:-6}
ROWS=${4:-6}
THUMB_W=${5:-320}
THUMB_H=${6:-180}

TOTAL=$(( COLS * ROWS ))

DURATION=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$INPUT" | cut -d. -f1)
INTERVAL=$(( DURATION / TOTAL ))

echo "Duration: ${DURATION}s | Capturing 1 frame every ${INTERVAL}s"

TMPDIR=$(mktemp -d)
trap "rm -rf $TMPDIR" EXIT

for i in $(seq 0 $(( TOTAL - 1 ))); do
    TIMESTAMP=$(( i * INTERVAL ))
    OUTFILE=$(printf "%s/frame_%03d.jpg" "$TMPDIR" "$i")
    ffmpeg -hide_banner -loglevel error \
        -ss "$TIMESTAMP" \
        -i "$INPUT" \
        -vframes 1 \
        -q:v 2 \
        "$OUTFILE"
    echo "Captured frame $((i+1))/${TOTAL} at ${TIMESTAMP}s"
done

# Tile the individual frames into a collage
ffmpeg -hide_banner -loglevel error \
    -pattern_type glob -i "${TMPDIR}/frame_*.jpg" \
    -vf "scale=${THUMB_W}:${THUMB_H},tile=${COLS}x${ROWS}:padding=4:color=black" \
    -frames:v 1 "$OUTPUT"

echo "Saved to $OUTPUT"
