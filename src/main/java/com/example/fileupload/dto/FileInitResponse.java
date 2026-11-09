package com.example.fileupload.dto;

import com.example.fileupload.entity.UploadStatus;
import java.util.List;

public class FileInitResponse {

    private String fileId;
    private String fileIdentifier;
    private String fileName;
    private Long fileSize;
    private Integer chunkSize;
    private Integer totalChunks;
    private UploadStatus status;
    private List<Integer> uploadedChunks;
    private Integer nextChunkIndex;
    private boolean resumed;
    private String message;

    public FileInitResponse() {
    }

    public FileInitResponse(String fileId, String fileIdentifier, String fileName, Long fileSize,
                            Integer chunkSize, Integer totalChunks, UploadStatus status,
                            List<Integer> uploadedChunks, Integer nextChunkIndex,
                            boolean resumed, String message) {
        this.fileId = fileId;
        this.fileIdentifier = fileIdentifier;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
        this.status = status;
        this.uploadedChunks = uploadedChunks;
        this.nextChunkIndex = nextChunkIndex;
        this.resumed = resumed;
        this.message = message;
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

    public boolean isResumed() {
        return resumed;
    }

    public void setResumed(boolean resumed) {
        this.resumed = resumed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

