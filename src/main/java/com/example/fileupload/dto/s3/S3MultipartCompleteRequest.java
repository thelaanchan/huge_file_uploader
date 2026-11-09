package com.example.fileupload.dto.s3;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record S3MultipartCompleteRequest(
        @NotBlank(message = "Upload ID is required")
        String uploadId,

        @NotBlank(message = "Object key is required")
        String key,

        @NotEmpty(message = "Parts list cannot be empty")
        @Valid
        List<S3PartDto> parts
) {}

