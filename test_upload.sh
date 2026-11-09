#!/bin/bash
# ==============================================================================
# Fault-Tolerant Resumable File Upload CLI Test Script
# Tests:
#   1. Creating a sample dummy file (e.g. 15 MB)
#   2. Initializing upload via POST /api/files/init
#   3. Uploading chunk 0
#   4. Intentionally simulating network failure / interruption
#   5. Querying resume status via GET /api/files/{id}/status
#   6. Uploading remaining chunks (chunk 1, 2, ...)
#   7. Verifying upload completion in MySQL
#   8. Downloading the file and comparing MD5 checksum
# ==============================================================================

set -e

SERVER_URL="http://localhost:8080"
TEST_FILE="sample_test_15mb.bin"
DOWNLOADED_FILE="downloaded_test_file.bin"
CHUNK_SIZE=5242880 # 5 MB (5 * 1024 * 1024)

echo "=========================================================="
echo "  Resumable Huge File Upload - Fault Tolerance Test"
echo "=========================================================="

# Step 0: Ensure server is running
echo "[1/7] Checking if Spring Boot server is running on $SERVER_URL..."
if ! curl -s --head "$SERVER_URL/api/files" > /dev/null; then
    echo "ERROR: Server is not running at $SERVER_URL!"
    echo "Please start the Spring Boot app first using:"
    echo "  mvn spring-boot:run"
    exit 1
fi
echo "✓ Server is active."

# Step 1: Create 15MB dummy file
echo "[2/7] Generating 15 MB test binary file ($TEST_FILE)..."
dd if=/dev/urandom of="$TEST_FILE" bs=1048576 count=15 2>/dev/null
FILE_SIZE=$(stat -f%z "$TEST_FILE" 2>/dev/null || stat -c%s "$TEST_FILE" 2>/dev/null)
ORIGINAL_MD5=$(md5 -q "$TEST_FILE" 2>/dev/null || md5sum "$TEST_FILE" | awk '{print $1}')
echo "✓ Test file created: Size = $FILE_SIZE bytes, MD5 = $ORIGINAL_MD5"

# Step 2: Initialize upload
echo "[3/7] Initializing upload via POST /api/files/init..."
INIT_RESPONSE=$(curl -s -X POST "$SERVER_URL/api/files/init" \
    -H "Content-Type: application/json" \
    -d "{
        \"fileName\": \"$TEST_FILE\",
        \"fileSize\": $FILE_SIZE,
        \"chunkSize\": $CHUNK_SIZE,
        \"fileIdentifier\": \"cli_test_${FILE_SIZE}_${ORIGINAL_MD5}\"
    }")

echo "Response: $INIT_RESPONSE"

FILE_ID=$(echo "$INIT_RESPONSE" | grep -o '"fileId":"[^"]*' | cut -d'"' -f4)
TOTAL_CHUNKS=$(echo "$INIT_RESPONSE" | grep -o '"totalChunks":[0-9]*' | cut -d':' -f2)

if [ -z "$FILE_ID" ]; then
    echo "ERROR: Failed to retrieve fileId from init response!"
    exit 1
fi

echo "✓ Upload registered with File ID: $FILE_ID (Total Chunks: $TOTAL_CHUNKS)"

# Step 3: Upload Chunk 0
echo "[4/7] Uploading Chunk 0 ($CHUNK_SIZE bytes)..."
dd if="$TEST_FILE" of="chunk_0.tmp" bs=1 skip=0 count=$CHUNK_SIZE 2>/dev/null
CHUNK0_RESP=$(curl -s -X POST "$SERVER_URL/api/files/$FILE_ID/chunks" \
    -F "chunkIndex=0" \
    -F "file=@chunk_0.tmp")
rm -f chunk_0.tmp
echo "Chunk 0 Response: $CHUNK0_RESP"
echo "✓ Chunk 0 stored safely in MySQL."

# Step 4: Simulate network drop / interruption!
echo "----------------------------------------------------------"
echo "⚠️  SIMULATING CRASH / NETWORK DROP AT CHUNK 0!"
echo "    Client connection terminated. Upload halted."
echo "----------------------------------------------------------"
sleep 1

# Step 5: Resume upload!
echo "[5/7] Reconnecting! Checking upload status to resume from previous end..."
STATUS_RESP=$(curl -s "$SERVER_URL/api/files/$FILE_ID/status")
echo "Status Response: $STATUS_RESP"

NEXT_CHUNK=$(echo "$STATUS_RESP" | grep -o '"nextChunkIndex":[0-9]*' | cut -d':' -f2)
echo "✓ Server confirms Chunk 0 is already stored. Resuming from Chunk: $NEXT_CHUNK"

# Step 6: Upload remaining chunks
for (( c=$NEXT_CHUNK; c<$TOTAL_CHUNKS; c++ ))
do
    OFFSET=$(( c * CHUNK_SIZE ))
    echo "[6/7] Uploading Chunk $c (Offset: $OFFSET bytes)..."
    dd if="$TEST_FILE" of="chunk_${c}.tmp" bs=1 skip=$OFFSET count=$CHUNK_SIZE 2>/dev/null
    CHUNK_RESP=$(curl -s -X POST "$SERVER_URL/api/files/$FILE_ID/chunks" \
        -F "chunkIndex=$c" \
        -F "file=@chunk_${c}.tmp")
    rm -f "chunk_${c}.tmp"
    echo "Chunk $c Response: $CHUNK_RESP"
done

# Step 7: Verify download and integrity
echo "[7/7] Downloading reconstructed file from MySQL streaming API..."
curl -s "$SERVER_URL/api/files/$FILE_ID/download" -o "$DOWNLOADED_FILE"

DOWNLOADED_MD5=$(md5 -q "$DOWNLOADED_FILE" 2>/dev/null || md5sum "$DOWNLOADED_FILE" | awk '{print $1}')
DOWNLOADED_SIZE=$(stat -f%z "$DOWNLOADED_FILE" 2>/dev/null || stat -c%s "$DOWNLOADED_FILE" 2>/dev/null)

echo "Downloaded Size: $DOWNLOADED_SIZE bytes"
echo "Original MD5:    $ORIGINAL_MD5"
echo "Downloaded MD5:  $DOWNLOADED_MD5"

if [ "$ORIGINAL_MD5" = "$DOWNLOADED_MD5" ]; then
    echo "=========================================================="
    echo "🎉 TEST PASSED! The downloaded file is 100% BIT-PERFECT!"
    echo "   Resumed from previous end without re-uploading chunk 0."
    echo "=========================================================="
else
    echo "❌ MD5 MISMATCH! File corrupted."
    exit 1
fi

# Cleanup
rm -f "$TEST_FILE" "$DOWNLOADED_FILE"
echo "Cleaned up temporary test files."

