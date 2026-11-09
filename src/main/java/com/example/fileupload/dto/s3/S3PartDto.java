package com.example.fileupload.dto.s3;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record S3PartDto(
        @Min(value = 1, message = "Part number must be at least 1")
        int partNumber,

        @NotBlank(message = "ETag is required")
        String eTag
) {}
