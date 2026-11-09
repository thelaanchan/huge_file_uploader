package com.example.fileupload.dto;

import com.example.fileupload.entity.UploadStatus;
import java.time.LocalDateTime;
import java.util.List;

public class FileStatusResponse {

    private String fileId;
    private String fileIdentifier;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Integer chunkSize;
    private Integer totalChunks;
    private Integer uploadedChunksCount;
    private List<Integer> uploadedChunks;
    private Integer nextChunkIndex;
    private Long bytesUploaded;
    private Double progressPercentage;
    private UploadStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FileStatusResponse() {
    }

    public FileStatusResponse(String fileId, String fileIdentifier, String fileName, Long fileSize,
                              String contentType, Integer chunkSize, Integer totalChunks,
                              Integer uploadedChunksCount, List<Integer> uploadedChunks,
                              Integer nextChunkIndex, Long bytesUploaded, Double progressPercentage,
                              UploadStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.fileId = fileId;
        this.fileIdentifier = fileIdentifier;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
        this.uploadedChunksCount = uploadedChunksCount;
        this.uploadedChunks = uploadedChunks;
        this.nextChunkIndex = nextChunkIndex;
        this.bytesUploaded = bytesUploaded;
        this.progressPercentage = progressPercentage;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileIdentifier() {
        return fileIdentifier;
    }

    public void setFileIdentifier(String fileIdentifier) {
        this.fileIdentifier = fileIdentifier;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
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

    public List<Integer> getUploadedChunks() {
        return uploadedChunks;
    }

    public void setUploadedChunks(List<Integer> uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    public Integer getNextChunkIndex() {
        return nextChunkIndex;
    }

    public void setNextChunkIndex(Integer nextChunkIndex) {
        this.nextChunkIndex = nextChunkIndex;
    }

    public Long getBytesUploaded() {
        return bytesUploaded;
    }

    public void setBytesUploaded(Long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

