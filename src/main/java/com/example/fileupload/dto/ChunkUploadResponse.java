package com.example.fileupload.dto;

import com.example.fileupload.entity.UploadStatus;

public record ChunkUploadResponse(
        String fileId,
        Integer chunkIndex,
        Integer chunkSize,
        Integer totalChunks,
        Integer uploadedChunksCount,
        Double progressPercentage,
        boolean fileCompleted,
        UploadStatus status,
        String message
) {}
