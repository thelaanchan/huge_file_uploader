package com.example.fileupload.dto;

import com.example.fileupload.entity.UploadStatus;
import java.time.LocalDateTime;
import java.util.List;

public record FileStatusResponse(
        String fileId,
        String fileIdentifier,
        String fileName,
        Long fileSize,
        String contentType,
        Integer chunkSize,
        Integer totalChunks,
        Integer uploadedChunksCount,
        List<Integer> uploadedChunks,
        Integer nextChunkIndex,
        Long bytesUploaded,
        Double progressPercentage,
        UploadStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
