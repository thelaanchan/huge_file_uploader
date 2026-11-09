package com.example.fileupload.dto.s3;

public record S3MultipartInitResponse(
        String uploadId,
        String key,
        String bucket,
        int partSize,
        int totalParts
) {}
