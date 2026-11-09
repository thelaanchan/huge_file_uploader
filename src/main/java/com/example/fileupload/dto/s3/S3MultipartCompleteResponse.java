package com.example.fileupload.dto.s3;

public record S3MultipartCompleteResponse(
        String key,
        String bucket,
        String location,
        String message
) {}
