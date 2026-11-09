package com.example.fileupload.controller;

import com.example.fileupload.dto.s3.*;
import com.example.fileupload.service.S3DirectUploadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@CrossOrigin(origins = "*")
public class S3DirectUploadController {

    private final S3DirectUploadService s3DirectUploadService;

    public S3DirectUploadController(S3DirectUploadService s3DirectUploadService) {
        this.s3DirectUploadService = s3DirectUploadService;
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<S3SingleUploadInitResponse> getSingleUploadUrl(
            @Valid @RequestBody S3SingleUploadInitRequest request) {
        return ResponseEntity.ok(s3DirectUploadService.createSingleUploadPresignedUrl(request));
    }

    @PostMapping("/multipart/initiate")
    public ResponseEntity<S3MultipartInitResponse> initiateMultipart(
            @Valid @RequestBody S3MultipartInitRequest request) {
        return ResponseEntity.ok(s3DirectUploadService.initiateMultipartUpload(request));
    }

    @PostMapping("/multipart/part-url")
    public ResponseEntity<S3PartPresignedUrlResponse> getPartUrl(
            @Valid @RequestBody S3PartPresignedUrlRequest request) {
        return ResponseEntity.ok(s3DirectUploadService.generatePartPresignedUrl(
                request.uploadId(), request.key(), request.partNumber()));
    }

    @PostMapping("/multipart/complete")
    public ResponseEntity<S3MultipartCompleteResponse> completeMultipart(
            @Valid @RequestBody S3MultipartCompleteRequest request) {
        return ResponseEntity.ok(s3DirectUploadService.completeMultipartUpload(request));
    }

    @PostMapping("/multipart/abort")
    public ResponseEntity<Map<String, String>> abortMultipart(
            @Valid @RequestBody S3MultipartAbortRequest request) {
        s3DirectUploadService.abortMultipartUpload(request.uploadId(), request.key());
        return ResponseEntity.ok(Map.of("message", "Multipart upload aborted successfully."));
    }

    @GetMapping("/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@RequestParam("key") String key) {
        String downloadUrl = s3DirectUploadService.generateDownloadPresignedUrl(key);
        return ResponseEntity.ok(Map.of("key", key, "downloadUrl", downloadUrl));
    }
}

