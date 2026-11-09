package com.example.fileupload.dto;

import com.example.fileupload.entity.UploadStatus;
import java.util.List;

public record FileInitResponse(
        String fileId,
        String fileIdentifier,
        String fileName,
        Long fileSize,
        Integer chunkSize,
        Integer totalChunks,
        UploadStatus status,
        List<Integer> uploadedChunks,
        Integer nextChunkIndex,
        boolean resumed,
        String message
) {}
