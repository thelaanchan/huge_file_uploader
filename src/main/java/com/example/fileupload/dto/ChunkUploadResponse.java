package com.example.fileupload.dto;

import com.example.fileupload.entity.UploadStatus;

public class ChunkUploadResponse {

    private String fileId;
    private Integer chunkIndex;
    private Integer chunkSize;
    private Integer totalChunks;
    private Integer uploadedChunksCount;
    private Double progressPercentage;
    private boolean fileCompleted;
    private UploadStatus status;
    private String message;

    public ChunkUploadResponse() {
    }

    public ChunkUploadResponse(String fileId, Integer chunkIndex, Integer chunkSize,
                               Integer totalChunks, Integer uploadedChunksCount,
                               Double progressPercentage, boolean fileCompleted,
                               UploadStatus status, String message) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
        this.uploadedChunksCount = uploadedChunksCount;
        this.progressPercentage = progressPercentage;
        this.fileCompleted = fileCompleted;
        this.status = status;
        this.message = message;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Integer getUploadedChunksCount() {
        return uploadedChunksCount;
    }

    public void setUploadedChunksCount(Integer uploadedChunksCount) {
        this.uploadedChunksCount = uploadedChunksCount;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public boolean isFileCompleted() {
        return fileCompleted;
    }

    public void setFileCompleted(boolean fileCompleted) {
        this.fileCompleted = fileCompleted;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

