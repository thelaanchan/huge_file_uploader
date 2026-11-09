package com.example.fileupload.dto.s3;

public record S3SingleUploadInitResponse(
        String uploadUrl,
        String key,
        String bucket,
        long expiresInMinutes
) {}
