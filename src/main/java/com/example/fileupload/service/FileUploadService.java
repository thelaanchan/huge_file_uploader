package com.example.fileupload.service;

import com.example.fileupload.dto.*;
import com.example.fileupload.entity.FileChunk;
import com.example.fileupload.entity.FileMetadata;
import com.example.fileupload.entity.UploadStatus;
import com.example.fileupload.repository.FileChunkRepository;
import com.example.fileupload.repository.FileMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    private final FileMetadataRepository fileMetadataRepository;
    private final FileChunkRepository fileChunkRepository;

    @Value("${app.upload.default-chunk-size:5242880}")
    private int defaultChunkSize; // 5 MB

    public FileUploadService(FileMetadataRepository fileMetadataRepository,
                             FileChunkRepository fileChunkRepository) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileChunkRepository = fileChunkRepository;
    }

    /**
     * Initializes a new upload or detects an existing upload to resume.
     */
    @Transactional
    public FileInitResponse initOrResumeUpload(FileInitRequest request) {
        int chunkSize = (request.getChunkSize() != null && request.getChunkSize() > 0)
                ? request.getChunkSize()
                : defaultChunkSize;

        long fileSize = request.getFileSize();
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);
        if (totalChunks == 0) {
            totalChunks = 1;
        }

        String identifier = request.getFileIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = request.getFileName() + "_" + fileSize;
        }

        Optional<FileMetadata> existingOpt = fileMetadataRepository.findByFileIdentifier(identifier);

        if (existingOpt.isPresent()) {
            FileMetadata existing = existingOpt.get();
            List<Integer> uploadedChunks = fileChunkRepository.findChunkIndicesByFileId(existing.getId());
            int nextChunkIndex = findNextMissingChunkIndex(uploadedChunks, existing.getTotalChunks());

            boolean isComplete = existing.getStatus() == UploadStatus.COMPLETED
                    || (uploadedChunks.size() >= existing.getTotalChunks() && nextChunkIndex == -1);

            if (isComplete && existing.getStatus() != UploadStatus.COMPLETED) {
                existing.setStatus(UploadStatus.COMPLETED);
                fileMetadataRepository.save(existing);
            }

            log.info("Resuming file upload for identifier: '{}', fileId: '{}', uploaded chunks: {}/{}",
                    identifier, existing.getId(), uploadedChunks.size(), existing.getTotalChunks());

            return new FileInitResponse(
                    existing.getId(),
                    existing.getFileIdentifier(),
                    existing.getFileName(),
                    existing.getFileSize(),
                    existing.getChunkSize(),
                    existing.getTotalChunks(),
                    existing.getStatus(),
                    uploadedChunks,
                    nextChunkIndex,
                    true,
                    isComplete
                            ? "File has already been completely uploaded."
                            : "Resuming existing upload from chunk " + (nextChunkIndex == -1 ? 0 : nextChunkIndex)
            );
        }

        // New upload
        String fileId = UUID.randomUUID().toString();
        String contentType = request.getContentType() != null && !request.getContentType().isBlank()
                ? request.getContentType()
                : "application/octet-stream";

        FileMetadata metadata = new FileMetadata(
                fileId,
                identifier,
                request.getFileName(),
                fileSize,
                contentType,
                chunkSize,
                totalChunks,
                UploadStatus.INITIALIZED
        );

        fileMetadataRepository.save(metadata);

        log.info("Initialized new file upload: fileId='{}', name='{}', size={} bytes, totalChunks={}",
                fileId, request.getFileName(), fileSize, totalChunks);

        return new FileInitResponse(
                fileId,
                identifier,
                request.getFileName(),
                fileSize,
                chunkSize,
                totalChunks,
                UploadStatus.INITIALIZED,
                Collections.emptyList(),
                0,
                false,
                "File upload initialized successfully. Ready to receive chunks starting at index 0."
        );
    }

    /**
     * Uploads and stores a single chunk into MySQL. Commits per chunk.
     */
    @Transactional
    public ChunkUploadResponse uploadChunk(String fileId, int chunkIndex, MultipartFile file, String expectedChecksum)
            throws IOException {

        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File upload not found with ID: " + fileId));

        if (chunkIndex < 0 || chunkIndex >= metadata.getTotalChunks()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid chunk index %d. Valid range is 0 to %d.", chunkIndex, metadata.getTotalChunks() - 1));
        }

        byte[] chunkBytes = file.getBytes();
        if (chunkBytes.length == 0) {
            throw new IllegalArgumentException("Chunk payload cannot be empty.");
        }

        // Integrity verification if client supplied expectedChecksum
        String actualChecksum = DigestUtils.md5DigestAsHex(chunkBytes);
        if (expectedChecksum != null && !expectedChecksum.isBlank()) {
            if (!actualChecksum.equalsIgnoreCase(expectedChecksum.trim())) {
                throw new IllegalArgumentException(String.format(
                        "Checksum verification failed for chunk %d! Expected: %s, Actual: %s",
                        chunkIndex, expectedChecksum, actualChecksum));
            }
        }

        // Save or update chunk (Idempotent: handles retry smoothly)
        Optional<FileChunk> existingChunk = fileChunkRepository.findByFileIdAndChunkIndex(fileId, chunkIndex);
        if (existingChunk.isPresent()) {
            FileChunk chunk = existingChunk.get();
            chunk.setChunkSize(chunkBytes.length);
            chunk.setChunkData(chunkBytes);
            chunk.setChecksum(actualChecksum);
            fileChunkRepository.save(chunk);
            log.info("Updated existing chunk {} for fileId {}", chunkIndex, fileId);
        } else {
            FileChunk chunk = new FileChunk(fileId, chunkIndex, chunkBytes.length, chunkBytes, actualChecksum);
            fileChunkRepository.save(chunk);
            log.info("Persisted chunk {}/{} ({} bytes) for fileId {}",
                    chunkIndex, metadata.getTotalChunks() - 1, chunkBytes.length, fileId);
        }

        // Update metadata status to UPLOADING if currently INITIALIZED
        if (metadata.getStatus() == UploadStatus.INITIALIZED) {
            metadata.setStatus(UploadStatus.UPLOADING);
            fileMetadataRepository.save(metadata);
        }

        int uploadedCount = fileChunkRepository.countByFileId(fileId);
        boolean isCompleted = uploadedCount >= metadata.getTotalChunks();

        if (isCompleted) {
            metadata.setStatus(UploadStatus.COMPLETED);
            fileMetadataRepository.save(metadata);
            log.info("File upload COMPLETED for fileId '{}', total chunks: {}", fileId, uploadedCount);
        }

        double progress = (uploadedCount * 100.0) / metadata.getTotalChunks();

        return new ChunkUploadResponse(
                fileId,
                chunkIndex,
                chunkBytes.length,
                metadata.getTotalChunks(),
                uploadedCount,
                Math.round(progress * 100.0) / 100.0,
                isCompleted,
                metadata.getStatus(),
                isCompleted ? "All chunks uploaded. File is complete!" : "Chunk " + chunkIndex + " uploaded successfully."
        );
    }

    /**
     * Retrieves current status and list of uploaded chunks for resuming.
     */
    @Transactional(readOnly = true)
    public FileStatusResponse getFileStatus(String fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File upload not found with ID: " + fileId));

        List<Integer> uploadedChunks = fileChunkRepository.findChunkIndicesByFileId(fileId);
        int nextChunkIndex = findNextMissingChunkIndex(uploadedChunks, metadata.getTotalChunks());

        long estimatedUploadedBytes = 0;
        int count = uploadedChunks.size();
        if (count > 0) {
            if (count == metadata.getTotalChunks()) {
                estimatedUploadedBytes = metadata.getFileSize();
            } else {
                estimatedUploadedBytes = (long) count * metadata.getChunkSize();
                if (estimatedUploadedBytes > metadata.getFileSize()) {
                    estimatedUploadedBytes = metadata.getFileSize();
                }
            }
        }

        double progress = (count * 100.0) / metadata.getTotalChunks();

        return new FileStatusResponse(
                metadata.getId(),
                metadata.getFileIdentifier(),
                metadata.getFileName(),
                metadata.getFileSize(),
                metadata.getContentType(),
                metadata.getChunkSize(),
                metadata.getTotalChunks(),
                count,
                uploadedChunks,
                nextChunkIndex,
                estimatedUploadedBytes,
                Math.round(progress * 100.0) / 100.0,
                metadata.getStatus(),
                metadata.getCreatedAt(),
                metadata.getUpdatedAt()
        );
    }

    /**
     * Streams the complete file out chunk by chunk to the HTTP response OutputStream.
     * Memory usage stays under one chunk (~5MB) even for 10GB files!
     */
    @Transactional(readOnly = true)
    public void streamFile(String fileId, OutputStream outputStream) throws IOException {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));

        if (metadata.getStatus() != UploadStatus.COMPLETED) {
            throw new IllegalStateException("File upload is not complete yet. Current status: " + metadata.getStatus());
        }

        int totalChunks = metadata.getTotalChunks();
        log.info("Starting chunked stream for file '{}' (total {} chunks)...", metadata.getFileName(), totalChunks);

        for (int i = 0; i < totalChunks; i++) {
            final int chunkIdx = i;
            FileChunk chunk = fileChunkRepository.findByFileIdAndChunkIndex(fileId, i)
                    .orElseThrow(() -> new IllegalStateException("Missing chunk " + chunkIdx + " for file " + fileId));

            outputStream.write(chunk.getChunkData());
            outputStream.flush();
        }

        log.info("Streaming completed successfully for file '{}'", metadata.getFileName());
    }

    /**
     * Lists all files with upload statistics.
     */
    @Transactional(readOnly = true)
    public List<FileSummaryDto> listAllFiles() {
        return fileMetadataRepository.findAllByOrderByCreatedAtDesc().stream().map(meta -> {
            int chunkCount = fileChunkRepository.countByFileId(meta.getId());
            return new FileSummaryDto(
                    meta.getId(),
                    meta.getFileIdentifier(),
                    meta.getFileName(),
                    meta.getFileSize(),
                    meta.getContentType(),
                    meta.getTotalChunks(),
                    chunkCount,
                    meta.getStatus(),
                    meta.getCreatedAt(),
                    meta.getUpdatedAt()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Deletes a file and all its chunks from MySQL.
     */
    @Transactional
    public void deleteFile(String fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));
        fileChunkRepository.deleteByFileId(fileId);
        fileMetadataRepository.delete(metadata);
        log.info("Deleted file and chunks for fileId: {}", fileId);
    }

    public FileMetadata getMetadata(String fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));
    }

    private int findNextMissingChunkIndex(List<Integer> uploadedChunks, int totalChunks) {
        Set<Integer> set = new HashSet<>(uploadedChunks);
        for (int i = 0; i < totalChunks; i++) {
            if (!set.contains(i)) {
                return i;
            }
        }
        return -1; // All uploaded
    }
}

