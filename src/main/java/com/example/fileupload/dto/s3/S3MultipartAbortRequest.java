package com.example.fileupload.dto.s3;

import jakarta.validation.constraints.NotBlank;

public record S3MultipartAbortRequest(
        @NotBlank(message = "Upload ID is required")
        String uploadId,

        @NotBlank(message = "Object key is required")
        String key
) {}
