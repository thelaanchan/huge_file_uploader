package com.example.fileupload.dto.s3;

import jakarta.validation.constraints.NotBlank;

public record S3SingleUploadInitRequest(
        @NotBlank(message = "File name is required")
        String fileName,
        String contentType
) {}

