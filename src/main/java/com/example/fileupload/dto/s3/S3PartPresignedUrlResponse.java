package com.example.fileupload.dto.s3;

public record S3PartPresignedUrlResponse(
        String uploadId,
        String key,
        int partNumber,
        String presignedUrl,
        long expiresInMinutes
) {}

