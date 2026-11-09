package com.example.fileupload.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_file_identifier", columnList = "file_identifier"),
    @Index(name = "idx_file_status", columnList = "status")
})
public class FileMetadata {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "file_identifier", length = 255, nullable = false)
    private String fileIdentifier;

    @Column(name = "file_name", length = 500, nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private UploadStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public FileMetadata() {
    }

    public FileMetadata(String id, String fileIdentifier, String fileName, Long fileSize,
                        String contentType, Integer chunkSize, Integer totalChunks, UploadStatus status) {
        this.id = id;
        this.fileIdentifier = fileIdentifier;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

