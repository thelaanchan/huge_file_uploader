package com.example.fileupload.dto.s3;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record S3PartPresignedUrlRequest(
        @NotBlank(message = "Upload ID is required")
        String uploadId,

        @NotBlank(message = "Object key is required")
        String key,

        @Min(value = 1, message = "Part number must be at least 1")
        int partNumber
) {}
