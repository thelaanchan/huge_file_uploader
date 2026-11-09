package com.example.fileupload.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_chunks",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_file_chunk", columnNames = {"file_id", "chunk_index"})
    },
    indexes = {
        @Index(name = "idx_file_chunk_lookup", columnList = "file_id, chunk_index")
    }
)
public class FileChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", length = 36, nullable = false)
    private String fileId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;

    @Lob
    @Column(name = "chunk_data", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] chunkData;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FileChunk() {
    }

    public FileChunk(String fileId, Integer chunkIndex, Integer chunkSize, byte[] chunkData, String checksum) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.chunkSize = chunkSize;
        this.chunkData = chunkData;
        this.checksum = checksum;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public byte[] getChunkData() {
        return chunkData;
    }

    public void setChunkData(byte[] chunkData) {
        this.chunkData = chunkData;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

