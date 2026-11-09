package com.example.fileupload.dto;

import com.example.fileupload.entity.UploadStatus;
import java.time.LocalDateTime;

public record FileSummaryDto(
        String fileId,
        String fileIdentifier,
        String fileName,
        Long fileSize,
        String contentType,
        Integer totalChunks,
        Integer uploadedChunksCount,
        UploadStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
