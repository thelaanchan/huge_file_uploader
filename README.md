# Huge File Upload Service

A Spring Boot service providing two high-performance upload strategies for massive files (1GB to 10GB+):

1. **MySQL Chunked Storage**: Resumable chunked upload persisted directly into MySQL database rows with bit-level resumption.
2. **Direct Browser-to-S3 Upload**: Secure presigned URL multipart upload where the browser streams chunks directly to AWS S3. Zero file bytes pass through the application server, saving server RAM, CPU, and network bandwidth.

---

## Features

- **Direct-to-S3 Presigned Uploads**: The server generates time-limited presigned URLs for S3 Multipart Upload. The frontend uploads chunks directly to Amazon S3 via HTTP `PUT`. S3 credentials never leave the backend.
- **MySQL Resumable Chunked Storage**: Files are split into configurable chunk sizes (default 5 MB) and stored as separate blob records. Avoids JVM OutOfMemory errors and MySQL `max_allowed_packet` limits.
- **Fault-Tolerant Resumption**: Incomplete uploads track existing chunks in the database. When retried, the client queries status and uploads only the missing chunks.
- **Low Memory Streaming**: File downloads stream chunk-by-chunk directly to the HTTP response output stream without buffering the entire file into memory.
- **Web UI**: Built-in minimalist browser interface with pause, resume, progress, and speed calculation.

---

## Architecture

1. **`file_metadata`**: Stores file ID, unique client fingerprint, filename, total size, chunk size, total chunk count, and status (`INITIALIZED`, `UPLOADING`, `COMPLETED`).
2. **`file_chunks`**: Stores individual chunk payloads (`LONGBLOB`), chunk index, and MD5 checksums. Indexed by `(file_id, chunk_index)`.

---

## Prerequisites

- Java 21 LTS
- Maven 3.9+
- MySQL 8.0+

---

## Configuration

Copy `.env.example` to `.env` and fill in your MySQL credentials:

```bash
cp .env.example .env
```

Default `.env` values:

```properties
SERVER_PORT=8080

DB_HOST=localhost
DB_PORT=3306
DB_NAME=fileupload_db
DB_USERNAME=root
DB_PASSWORD=5646

MAX_FILE_SIZE=50MB
MAX_REQUEST_SIZE=50MB
DEFAULT_CHUNK_SIZE=5242880
```

> **Note**: `.env` is ignored by Git. Only `.env.example` is tracked.

---

## Building and Running

### 1. Create the database
```sql
CREATE DATABASE IF NOT EXISTS fileupload_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Run with Maven
```bash
mvn spring-boot:run
```

Or build and run the JAR:
```bash
mvn clean package -DskipTests
java -jar target/huge-file-upload-1.0.0.jar
```

### 3. Access the Web UI
Open your browser at:
```
http://localhost:8080
```

---

## REST API

### 1. Initialize or Resume Upload
```http
POST /api/files/init
Content-Type: application/json

{
  "fileName": "large_archive.iso",
  "fileSize": 10737418240,
  "chunkSize": 5242880,
  "fileIdentifier": "large_archive.iso_10737418240"
}
```

Response:
```json
{
  "fileId": "6a67f781-b5fe-4ca9-bfd4-1a06981cf3a6",
  "fileIdentifier": "large_archive.iso_10737418240",
  "fileName": "large_archive.iso",
  "fileSize": 10737418240,
  "chunkSize": 5242880,
  "totalChunks": 2048,
  "status": "INITIALIZED",
  "uploadedChunks": [],
  "nextChunkIndex": 0,
  "resumed": false,
  "message": "Ready to receive chunks."
}
```

### 2. Upload Chunk
```http
POST /api/files/{fileId}/chunks
Content-Type: multipart/form-data

Form fields:
- chunkIndex: 0
- file: <binary chunk slice>
- checksum: (optional MD5)
```

### 3. Check Status / Resume Query
```http
GET /api/files/{fileId}/status
```

Response:
```json
{
  "fileId": "6a67f781-b5fe-4ca9-bfd4-1a06981cf3a6",
  "fileIdentifier": "large_archive.iso_10737418240",
  "fileName": "large_archive.iso",
  "fileSize": 10737418240,
  "chunkSize": 5242880,
  "totalChunks": 2048,
  "uploadedChunksCount": 512,
  "uploadedChunks": [0, 1, 2, "..."],
  "nextChunkIndex": 512,
  "bytesUploaded": 2684354560,
  "progressPercentage": 25.0,
  "status": "UPLOADING"
}
```

### 4. Download Reconstructed File
```http
GET /api/files/{fileId}/download
```

### 5. List Files
```http
GET /api/files
```

### 6. Delete File
```http
DELETE /api/files/{fileId}
```

---

## Direct S3 Upload REST API (Zero App Server Load)

### 1. Initiate S3 Multipart Upload
```http
POST /api/s3/multipart/initiate
Content-Type: application/json

{
  "fileName": "large_video.mp4",
  "fileSize": 5368709120,
  "contentType": "video/mp4",
  "partSize": 10485760
}
```
Response:
```json
{
  "uploadId": "VXBsb2FkIElEIGV4YW1wbGU...",
  "key": "uploads/4f19b2ca-large_video.mp4",
  "bucket": "fileupload-bucket",
  "partSize": 10485760,
  "totalParts": 512
}
```

### 2. Generate Presigned URL for Part
```http
POST /api/s3/multipart/part-url
Content-Type: application/json

{
  "uploadId": "VXBsb2FkIElEIGV4YW1wbGU...",
  "key": "uploads/4f19b2ca-large_video.mp4",
  "partNumber": 1
}
```
Response:
```json
{
  "uploadId": "VXBsb2FkIElEIGV4YW1wbGU...",
  "key": "uploads/4f19b2ca-large_video.mp4",
  "partNumber": 1,
  "presignedUrl": "https://fileupload-bucket.s3.amazonaws.com/uploads/...?partNumber=1&uploadId=...&X-Amz-Signature=...",
  "expiresInMinutes": 15
}
```

### 3. Direct Browser-to-S3 Upload (HTTP PUT)
The browser uploads the chunk binary slice directly to the presigned URL:
```http
PUT <presignedUrl>
Content-Type: application/octet-stream

<binary chunk payload>
```
S3 responds with `200 OK` and an `ETag` header (e.g. `ETag: "d41d8cd98f00b204e9800998ecf8427e"`).

### 4. Complete S3 Multipart Upload
Send the list of part numbers and ETags to assemble the file:
```http
POST /api/s3/multipart/complete
Content-Type: application/json

{
  "uploadId": "VXBsb2FkIElEIGV4YW1wbGU...",
  "key": "uploads/4f19b2ca-large_video.mp4",
  "parts": [
    { "partNumber": 1, "eTag": "\"d41d8cd98f00b204e9800998ecf8427e\"" }
  ]
}
```

### 5. Abort Multipart Upload
```http
POST /api/s3/multipart/abort
Content-Type: application/json

{
  "uploadId": "VXBsb2FkIElEIGV4YW1wbGU...",
  "key": "uploads/4f19b2ca-large_video.mp4"
}
```

### 6. Single File Presigned Upload (< 100MB)
```http
POST /api/s3/presigned-url
Content-Type: application/json

{
  "fileName": "avatar.png",
  "contentType": "image/png"
}
```

### 7. Get Presigned Download URL
```http
GET /api/s3/download-url?key=uploads/4f19b2ca-large_video.mp4
```

---

## Testing with Postman

A pre-configured Postman collection is included in the project root:
- File: `postman_collection.json`
- Import into Postman (`Import` -> select `postman_collection.json`).
- Automatically captures and chains `fileId` across requests.

---

## CLI Test Script

Run the automated interruption and resume test:
```bash
./test_upload.sh
```

---

## Git Setup

```bash
git remote add origin https://github.com/<username>/<repo>.git
git branch -M main
git push -u origin main
```
