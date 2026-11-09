package com.example.fileupload.controller;

import com.example.fileupload.dto.*;
import com.example.fileupload.entity.FileMetadata;
import com.example.fileupload.service.FileUploadService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*") // Allows access from web client without CORS restrictions
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 1. Initialize or resume upload
     * Client sends file metadata (name, size, optional identifier).
     * Server returns upload status and list of already uploaded chunks if resuming.
     */
    @PostMapping("/init")
    public ResponseEntity<FileInitResponse> initOrResumeUpload(@Valid @RequestBody FileInitRequest request) {
        FileInitResponse response = fileUploadService.initOrResumeUpload(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Upload a single chunk
     * Client uploads chunk bytes for a specific chunk index.
     * Committed independently into MySQL.
     */
    @PostMapping(value = "/{fileId}/chunks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChunkUploadResponse> uploadChunk(
            @PathVariable String fileId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "checksum", required = false) String checksum) throws IOException {

        ChunkUploadResponse response = fileUploadService.uploadChunk(fileId, chunkIndex, file, checksum);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Check upload progress / status
     * Client calls this to inspect which chunks were already received
     * so it can resume after any connection failure.
     */
    @GetMapping("/{fileId}/status")
    public ResponseEntity<FileStatusResponse> getFileStatus(@PathVariable String fileId) {
        FileStatusResponse status = fileUploadService.getFileStatus(fileId);
        return ResponseEntity.ok(status);
    }

    /**
     * 4. Stream and download complete file from MySQL
     * Directly streams chunks in order without loading whole 10GB file into RAM.
     */
    @GetMapping("/{fileId}/download")
    public void downloadFile(@PathVariable String fileId, HttpServletResponse response) throws IOException {
        FileMetadata metadata = fileUploadService.getMetadata(fileId);

        String encodedFileName = URLEncoder.encode(metadata.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        response.setContentType(metadata.getContentType() != null ? metadata.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(metadata.getFileSize()));

        try (OutputStream outputStream = response.getOutputStream()) {
            fileUploadService.streamFile(fileId, outputStream);
        }
    }

    /**
     * 5. List all files
     */
    @GetMapping
    public ResponseEntity<List<FileSummaryDto>> listAllFiles() {
        return ResponseEntity.ok(fileUploadService.listAllFiles());
    }

    /**
     * 6. Delete file and chunks
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId) {
        fileUploadService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }
}

