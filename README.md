# Resumable Huge File Upload (1GB - 10GB+) into MySQL Database

A production-grade Spring Boot application that enables uploading, storing, and streaming multi-gigabyte files (1GB, 5GB, 10GB+) directly into a **MySQL** database with **fault-tolerant resumability**.

If the network connection drops, the browser crashes, or the server restarts midway through an upload, the upload **automatically resumes from where it left off instead of restarting from the beginning**.

---

## 🚀 Key Features

- **Handles 1GB to 10GB+ Files in MySQL Safely**: Eliminates `OutOfMemoryError` and bypasses MySQL `max_allowed_packet` limits by breaking files into streamable chunks (e.g. 5 MB).
- **Fault-Tolerant Resumability**: Tracks chunk progress in MySQL. If an upload fails at 90%, it queries the server and resumes right at 90%, never wasting time or bandwidth re-uploading completed chunks.
- **Constant Low Memory Footprint (< 20 MB Heap)**: Chunks are committed individually in separate database transactions. The server never loads the full 10GB file into Java heap memory.
- **Zero-RAM Streaming Download**: Downloads stream chunks sequentially from MySQL rows straight to the HTTP response output stream.
- **Checksum Verification**: Supports optional per-chunk MD5/SHA-256 hashes to guarantee bit-for-bit integrity and prevent corrupted uploads.
- **Interactive Web Dashboard**: Built-in HTML5/CSS/JavaScript dashboard featuring drag-and-drop, real-time speed and progress tracking, and a **"Pause / Simulate Failure"** button to test and witness resumption in action.
- **Zero Authentication / Authorization**: Direct API access ready for prototyping or integration with custom security layers.

---

## 🏗️ Architecture & How It Works

Relational databases like MySQL cannot directly ingest a single 10GB BLOB column due to protocol packet limits (`max_allowed_packet` maximum is 1GB) and Java heap exhaustion.

This project solves the problem with a **Chunked Relational Storage** pattern:

```
[ Client / Browser ]
        |
        | 1. POST /api/files/init (fileName, totalSize: 10GB, chunkSize: 5MB)
        v
[ Spring Boot Controller ]
        |
        | 2. Check if fileIdentifier exists in MySQL
        v
[ MySQL: file_metadata ] ---> Returns fileId & already uploaded chunks (e.g. chunks 0..499)
        |
        | 3. Client slices and uploads only remaining chunks:
        |    POST /api/files/{fileId}/chunks (chunk 500, chunk 501, ...)
        v
[ MySQL: file_chunks ] ---> Each chunk (~5MB LONGBLOB) committed independently
        |
        | 4. GET /api/files/{fileId}/download
        v
[ Chunked Stream ] ---------> Sequentially streams chunks directly to client without buffering in RAM
```

### Database Schema

1. **`file_metadata`**:
   - `id`: UUID (Primary Key)
   - `file_identifier`: Unique fingerprint (`fileName_fileSize_lastModified`) for resume detection
   - `file_name`: Original file name
   - `file_size`: Total file size in bytes (`BIGINT`)
   - `content_type`: MIME type
   - `chunk_size`: Size of each chunk in bytes (default: 5 MB)
   - `total_chunks`: Total number of chunks
   - `status`: `INITIALIZED`, `UPLOADING`, `COMPLETED`, `FAILED`
   - `created_at`, `updated_at`: Timestamps

2. **`file_chunks`**:
   - `id`: Auto-increment BIGINT
   - `file_id`: Foreign key reference to `file_metadata(id)`
   - `chunk_index`: 0-based chunk sequence index
   - `chunk_size`: Size in bytes of this chunk
   - `chunk_data`: `LONGBLOB` binary chunk payload
   - `checksum`: MD5 digest for data verification
   - `created_at`: Timestamp
   - **Unique Index**: `(file_id, chunk_index)` prevents duplicate chunk insertion on retries.

---

## 📋 Prerequisites

- **Java**: JDK 21 LTS
- **Maven**: 3.9+
- **MySQL**: 8.0+ or MySQL 9+
- **Database Credentials**:
  - Host: `localhost:3306`
  - Database: `fileupload_db`
  - Username: `root`
  - Password: `5646`

---

## ⚙️ Configuration (.env & .env.example)

Sensitive credentials (MySQL password, host, user) are separated into a **[`.env`](.env)** file that is **automatically ignored** by `.gitignore` so your private credentials are never exposed on GitHub.

A template **[`.env.example`](.env.example)** is provided for team members or CI/CD deployments:

```bash
# Copy template to .env
cp .env.example .env
```

Contents of `.env`:
```properties
SERVER_PORT=8080

# MySQL Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=fileupload_db
DB_USERNAME=root
DB_PASSWORD=5646

# Upload Configuration
MAX_FILE_SIZE=50MB
MAX_REQUEST_SIZE=50MB
DEFAULT_CHUNK_SIZE=5242880
```

---

## 📮 Testing with Postman

A pre-configured **[Postman Collection (`postman_collection.json`)](postman_collection.json)** is included in the project root:

1. Open **Postman**.
2. Click **Import** (top left).
3. Select or drag-and-drop [`postman_collection.json`](postman_collection.json).
4. The collection contains all 6 endpoints with dynamic variables (`{{baseUrl}}`, `{{fileId}}`) and automatic test scripts!

---

## 🏃 Running the Application

### 1. Ensure MySQL is running and the database exists:
```bash
mysql -u root -p5646 -e "CREATE DATABASE IF NOT EXISTS fileupload_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 2. Build and Run:
Using Maven:
```bash
mvn spring-boot:run
```
Or run the packaged JAR:
```bash
java -jar target/huge-file-upload-1.0.0.jar
```

### 3. Open the Interactive Web UI:
Open your browser and navigate to:
```
http://localhost:8080
```
- Select any large file (e.g., 500MB, 1GB, 5GB, 10GB).
- Click **Start Upload**.
- Click **"Pause / Simulate Failure"** midway to abort the connection.
- Click **"Resume Upload"** — notice the upload resumes immediately from the exact chunk where it was interrupted!

---

## 📡 REST API Reference

### 1. Initialize or Resume Upload
```http
POST /api/files/init
Content-Type: application/json
```
**Request Body:**
```json
{
  "fileName": "large_archive.tar.gz",
  "fileSize": 5368709120,
  "contentType": "application/gzip",
  "chunkSize": 5242880,
  "fileIdentifier": "large_archive.tar.gz_5368709120_1712345678"
}
```
**Response (200 OK):**
```json
{
  "fileId": "6a67f781-b5fe-4ca9-bfd4-1a06981cf3a6",
  "fileIdentifier": "large_archive.tar.gz_5368709120_1712345678",
  "fileName": "large_archive.tar.gz",
  "fileSize": 5368709120,
  "chunkSize": 5242880,
  "totalChunks": 1024,
  "status": "UPLOADING",
  "uploadedChunks": [0, 1, 2, 3, 4],
  "nextChunkIndex": 5,
  "resumed": true,
  "message": "Resuming existing upload from chunk 5"
}
```

---

### 2. Upload Chunk
```http
POST /api/files/{fileId}/chunks
Content-Type: multipart/form-data
```
**Form Data Parameters:**
- `chunkIndex`: (Integer, 0-indexed)
- `file`: Binary chunk payload (`MultipartFile`)
- `checksum`: (Optional) MD5 hex string of chunk bytes

**Response (200 OK):**
```json
{
  "fileId": "6a67f781-b5fe-4ca9-bfd4-1a06981cf3a6",
  "chunkIndex": 5,
  "chunkSize": 5242880,
  "totalChunks": 1024,
  "uploadedChunksCount": 6,
  "progressPercentage": 0.59,
  "fileCompleted": false,
  "status": "UPLOADING",
  "message": "Chunk 5 uploaded successfully."
}
```

---

### 3. Check Status & Uploaded Chunks
```http
GET /api/files/{fileId}/status
```
**Response (200 OK):**
```json
{
  "fileId": "6a67f781-b5fe-4ca9-bfd4-1a06981cf3a6",
  "fileIdentifier": "large_archive.tar.gz_5368709120_1712345678",
  "fileName": "large_archive.tar.gz",
  "fileSize": 5368709120,
  "chunkSize": 5242880,
  "totalChunks": 1024,
  "uploadedChunksCount": 6,
  "uploadedChunks": [0, 1, 2, 3, 4, 5],
  "nextChunkIndex": 6,
  "bytesUploaded": 31457280,
  "progressPercentage": 0.59,
  "status": "UPLOADING",
  "createdAt": "2026-11-09T16:30:00",
  "updatedAt": "2026-11-09T16:30:15"
}
```

---

### 4. Download / Stream Reconstructed File
```http
GET /api/files/{fileId}/download
```
Streams all chunks in order directly from MySQL to the HTTP response stream.

---

### 5. List All Files
```http
GET /api/files
```

---

### 6. Delete File and Chunks
```http
DELETE /api/files/{fileId}
```

---

## 🧪 Automated Fault-Tolerance CLI Test

An automated bash test script [`test_upload.sh`](test_upload.sh) is included:

```bash
chmod +x test_upload.sh
./test_upload.sh
```

**What the script does:**
1. Generates a 15 MB binary file with random data.
2. Computes the original MD5 checksum.
3. Initializes the upload and uploads Chunk 0.
4. **Intentionally simulates a crash / network drop.**
5. Reconnects, queries MySQL for resume status, and retrieves the next expected chunk.
6. Uploads the remaining chunks.
7. Streams the reconstructed file back from MySQL and verifies the downloaded MD5 matches the original 100% bit-for-bit!

---

## 🐙 Uploading to GitHub

To push this project to your GitHub repository:

```bash
# 1. Initialize git (if not already done)
git init

# 2. Add all project files
git add .

# 3. Create initial commit
git commit -m "Initial commit: Resumable huge file upload into MySQL with fault-tolerance"

# 4. Set main branch
git branch -M main

# 5. Add your GitHub repository remote URL
# Replace USERNAME and REPO with your actual GitHub details:
git remote add origin https://github.com/USERNAME/REPO.git

# 6. Push code to GitHub
git push -u origin main
```

