package com.example.fileupload.dto.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record S3MultipartInitRequest(
        @NotBlank(message = "File name is required")
        String fileName,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        Long fileSize,

        String contentType,

        @Positive(message = "Part size must be positive")
        Integer partSize
) {}

