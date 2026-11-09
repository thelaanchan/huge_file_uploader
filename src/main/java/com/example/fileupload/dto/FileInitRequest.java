package com.example.fileupload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class FileInitRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;

    private String contentType;

    @Positive(message = "Chunk size must be positive if provided")
    private Integer chunkSize;

    private String fileIdentifier;

    public FileInitRequest() {
    }

    public FileInitRequest(String fileName, Long fileSize, String contentType, Integer chunkSize, String fileIdentifier) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.chunkSize = chunkSize;
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

    public String getFileIdentifier() {
        return fileIdentifier;
    }

    public void setFileIdentifier(String fileIdentifier) {
        this.fileIdentifier = fileIdentifier;
    }
}

