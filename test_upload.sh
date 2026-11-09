#!/usr/bin/env bash
set -euo pipefail

SERVER_URL="http://localhost:8080"
TEST_FILE="test_payload.bin"
DOWNLOADED_FILE="test_downloaded.bin"
CHUNK_SIZE=5242880 # 5 MB

echo "Testing upload service at $SERVER_URL"

if ! curl -fsS "$SERVER_URL/api/files" > /dev/null; then
    echo "Server is not reachable at $SERVER_URL"
    exit 1
fi

echo "Creating 15MB test file..."
dd if=/dev/urandom of="$TEST_FILE" bs=1048576 count=15 status=none
FILE_SIZE=$(stat -f%z "$TEST_FILE" 2>/dev/null || stat -c%s "$TEST_FILE")
ORIGINAL_MD5=$(md5 -q "$TEST_FILE" 2>/dev/null || md5sum "$TEST_FILE" | awk '{print $1}')

echo "File: $FILE_SIZE bytes, MD5: $ORIGINAL_MD5"

echo "Initializing upload..."
INIT_RESPONSE=$(curl -fsS -X POST "$SERVER_URL/api/files/init" \
    -H "Content-Type: application/json" \
    -d "{
        \"fileName\": \"$TEST_FILE\",
        \"fileSize\": $FILE_SIZE,
        \"chunkSize\": $CHUNK_SIZE,
        \"fileIdentifier\": \"test_${FILE_SIZE}_${ORIGINAL_MD5}\"
    }")

FILE_ID=$(echo "$INIT_RESPONSE" | grep -o '"fileId":"[^"]*' | cut -d'"' -f4)
TOTAL_CHUNKS=$(echo "$INIT_RESPONSE" | grep -o '"totalChunks":[0-9]*' | cut -d':' -f2)

echo "Upload initialized: fileId=$FILE_ID, totalChunks=$TOTAL_CHUNKS"

echo "Uploading chunk 0..."
dd if="$TEST_FILE" of="c0.tmp" bs=1 skip=0 count=$CHUNK_SIZE status=none
curl -fsS -X POST "$SERVER_URL/api/files/$FILE_ID/chunks" \
    -F "chunkIndex=0" \
    -F "file=@c0.tmp" > /dev/null
rm -f c0.tmp

echo "Simulating client disconnect after chunk 0..."
sleep 1

echo "Checking status to resume..."
STATUS_RESP=$(curl -fsS "$SERVER_URL/api/files/$FILE_ID/status")
NEXT_CHUNK=$(echo "$STATUS_RESP" | grep -o '"nextChunkIndex":[0-9]*' | cut -d':' -f2)

echo "Resuming from chunk $NEXT_CHUNK"

for (( c=$NEXT_CHUNK; c<$TOTAL_CHUNKS; c++ ))
do
    OFFSET=$(( c * CHUNK_SIZE ))
    echo "Uploading chunk $c..."
    dd if="$TEST_FILE" of="c_${c}.tmp" bs=1 skip=$OFFSET count=$CHUNK_SIZE status=none
    curl -fsS -X POST "$SERVER_URL/api/files/$FILE_ID/chunks" \
        -F "chunkIndex=$c" \
        -F "file=@c_${c}.tmp" > /dev/null
    rm -f "c_${c}.tmp"
done

echo "Downloading file..."
curl -fsS "$SERVER_URL/api/files/$FILE_ID/download" -o "$DOWNLOADED_FILE"

DOWNLOADED_MD5=$(md5 -q "$DOWNLOADED_FILE" 2>/dev/null || md5sum "$DOWNLOADED_FILE" | awk '{print $1}')

if [ "$ORIGINAL_MD5" = "$DOWNLOADED_MD5" ]; then
    echo "Checksums match: $ORIGINAL_MD5"
    echo "Test passed."
else
    echo "Checksum mismatch! Original: $ORIGINAL_MD5, Downloaded: $DOWNLOADED_MD5"
    exit 1
fi

rm -f "$TEST_FILE" "$DOWNLOADED_FILE"
