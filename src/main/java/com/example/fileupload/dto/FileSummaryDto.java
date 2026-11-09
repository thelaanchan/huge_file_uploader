package com.example.fileupload.dto;

import com.example.fileupload.entity.UploadStatus;
import java.time.LocalDateTime;

public class FileSummaryDto {

    private String fileId;
    private String fileIdentifier;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Integer totalChunks;
    private Integer uploadedChunksCount;
    private UploadStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FileSummaryDto() {
    }

    public FileSummaryDto(String fileId, String fileIdentifier, String fileName, Long fileSize,
                          String contentType, Integer totalChunks, Integer uploadedChunksCount,
                          UploadStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.fileId = fileId;
        this.fileIdentifier = fileIdentifier;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.totalChunks = totalChunks;
        this.uploadedChunksCount = uploadedChunksCount;
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

