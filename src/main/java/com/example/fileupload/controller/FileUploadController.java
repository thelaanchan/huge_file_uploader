package com.example.fileupload.controller;

import com.example.fileupload.dto.*;
import com.example.fileupload.entity.FileMetadata;
import com.example.fileupload.service.FileUploadService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/init")
    public ResponseEntity<FileInitResponse> initOrResume(@Valid @RequestBody FileInitRequest request) {
        return ResponseEntity.ok(fileUploadService.initOrResumeUpload(request));
    }

    @PostMapping(value = "/{fileId}/chunks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChunkUploadResponse> uploadChunk(
            @PathVariable String fileId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "checksum", required = false) String checksum) throws IOException {

        return ResponseEntity.ok(fileUploadService.uploadChunk(fileId, chunkIndex, file, checksum));
    }

    @GetMapping("/{fileId}/status")
    public ResponseEntity<FileStatusResponse> getStatus(@PathVariable String fileId) {
        return ResponseEntity.ok(fileUploadService.getFileStatus(fileId));
    }

    @GetMapping("/{fileId}/download")
    public void download(@PathVariable String fileId, HttpServletResponse response) throws IOException {
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

    @GetMapping
    public ResponseEntity<List<FileSummaryDto>> listFiles() {
        return ResponseEntity.ok(fileUploadService.listAllFiles());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId) {
        fileUploadService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }
}
