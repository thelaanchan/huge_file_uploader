package com.example.fileupload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FileInitRequest(
        @NotBlank(message = "File name is required")
        String fileName,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        Long fileSize,

        String contentType,

        @Positive(message = "Chunk size must be positive")
        Integer chunkSize,

        String fileIdentifier
) {}
