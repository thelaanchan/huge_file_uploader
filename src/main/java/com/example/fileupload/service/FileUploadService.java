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

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    private final FileMetadataRepository fileMetadataRepository;
    private final FileChunkRepository fileChunkRepository;

    @Value("${app.upload.default-chunk-size:5242880}")
    private int defaultChunkSize;

    public FileUploadService(FileMetadataRepository fileMetadataRepository,
                             FileChunkRepository fileChunkRepository) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileChunkRepository = fileChunkRepository;
    }

    @Transactional
    public FileInitResponse initOrResumeUpload(FileInitRequest request) {
        int chunkSize = (request.chunkSize() != null && request.chunkSize() > 0)
                ? request.chunkSize()
                : defaultChunkSize;

        long fileSize = request.fileSize();
        int totalChunks = Math.max(1, (int) Math.ceil((double) fileSize / chunkSize));

        String identifier = (request.fileIdentifier() != null && !request.fileIdentifier().isBlank())
                ? request.fileIdentifier().trim()
                : request.fileName() + "_" + fileSize;

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

            log.info("Found existing upload {} ({}/{} chunks)", existing.getId(), uploadedChunks.size(), existing.getTotalChunks());

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
                    isComplete ? "File upload already completed." : "Resuming from chunk " + (nextChunkIndex == -1 ? 0 : nextChunkIndex)
            );
        }

        String fileId = UUID.randomUUID().toString();
        String contentType = (request.contentType() != null && !request.contentType().isBlank())
                ? request.contentType()
                : "application/octet-stream";

        FileMetadata metadata = new FileMetadata(
                fileId,
                identifier,
                request.fileName(),
                fileSize,
                contentType,
                chunkSize,
                totalChunks,
                UploadStatus.INITIALIZED
        );

        fileMetadataRepository.save(metadata);
        log.info("Created upload {} for '{}' ({} bytes, {} chunks)", fileId, request.fileName(), fileSize, totalChunks);

        return new FileInitResponse(
                fileId,
                identifier,
                request.fileName(),
                fileSize,
                chunkSize,
                totalChunks,
                UploadStatus.INITIALIZED,
                List.of(),
                0,
                false,
                "Ready to receive chunks."
        );
    }

    @Transactional
    public ChunkUploadResponse uploadChunk(String fileId, int chunkIndex, MultipartFile file, String expectedChecksum)
            throws IOException {

        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Upload not found: " + fileId));

        if (chunkIndex < 0 || chunkIndex >= metadata.getTotalChunks()) {
            throw new IllegalArgumentException("Chunk index out of bounds: " + chunkIndex);
        }

        byte[] chunkBytes = file.getBytes();
        if (chunkBytes.length == 0) {
            throw new IllegalArgumentException("Empty chunk payload received");
        }

        String actualChecksum = DigestUtils.md5DigestAsHex(chunkBytes);
        if (expectedChecksum != null && !expectedChecksum.isBlank()) {
            if (!actualChecksum.equalsIgnoreCase(expectedChecksum.trim())) {
                throw new IllegalArgumentException("MD5 checksum mismatch on chunk " + chunkIndex);
            }
        }

        Optional<FileChunk> existingChunk = fileChunkRepository.findByFileIdAndChunkIndex(fileId, chunkIndex);
        if (existingChunk.isPresent()) {
            FileChunk chunk = existingChunk.get();
            chunk.setChunkSize(chunkBytes.length);
            chunk.setChunkData(chunkBytes);
            chunk.setChecksum(actualChecksum);
            fileChunkRepository.save(chunk);
        } else {
            fileChunkRepository.save(new FileChunk(fileId, chunkIndex, chunkBytes.length, chunkBytes, actualChecksum));
        }

        if (metadata.getStatus() == UploadStatus.INITIALIZED) {
            metadata.setStatus(UploadStatus.UPLOADING);
            fileMetadataRepository.save(metadata);
        }

        int uploadedCount = fileChunkRepository.countByFileId(fileId);
        boolean isCompleted = uploadedCount >= metadata.getTotalChunks();

        if (isCompleted) {
            metadata.setStatus(UploadStatus.COMPLETED);
            fileMetadataRepository.save(metadata);
            log.info("Upload completed for {}", fileId);
        }

        double progress = Math.round(((uploadedCount * 100.0) / metadata.getTotalChunks()) * 100.0) / 100.0;

        return new ChunkUploadResponse(
                fileId,
                chunkIndex,
                chunkBytes.length,
                metadata.getTotalChunks(),
                uploadedCount,
                progress,
                isCompleted,
                metadata.getStatus(),
                isCompleted ? "Upload finished." : "Chunk " + chunkIndex + " saved."
        );
    }

    @Transactional(readOnly = true)
    public FileStatusResponse getFileStatus(String fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Upload not found: " + fileId));

        List<Integer> uploadedChunks = fileChunkRepository.findChunkIndicesByFileId(fileId);
        int nextChunkIndex = findNextMissingChunkIndex(uploadedChunks, metadata.getTotalChunks());

        int count = uploadedChunks.size();
        long bytesUploaded = (count == metadata.getTotalChunks())
                ? metadata.getFileSize()
                : Math.min((long) count * metadata.getChunkSize(), metadata.getFileSize());

        double progress = Math.round(((count * 100.0) / metadata.getTotalChunks()) * 100.0) / 100.0;

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
                bytesUploaded,
                progress,
                metadata.getStatus(),
                metadata.getCreatedAt(),
                metadata.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public void streamFile(String fileId, OutputStream outputStream) throws IOException {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Upload not found: " + fileId));

        if (metadata.getStatus() != UploadStatus.COMPLETED) {
            throw new IllegalStateException("File is not fully uploaded. Status: " + metadata.getStatus());
        }

        int totalChunks = metadata.getTotalChunks();
        for (int i = 0; i < totalChunks; i++) {
            final int chunkIdx = i;
            FileChunk chunk = fileChunkRepository.findByFileIdAndChunkIndex(fileId, i)
                    .orElseThrow(() -> new IllegalStateException("Missing chunk " + chunkIdx));

            outputStream.write(chunk.getChunkData());
            outputStream.flush();
        }
    }

    @Transactional(readOnly = true)
    public List<FileSummaryDto> listAllFiles() {
        return fileMetadataRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(m -> new FileSummaryDto(
                        m.getId(),
                        m.getFileIdentifier(),
                        m.getFileName(),
                        m.getFileSize(),
                        m.getContentType(),
                        m.getTotalChunks(),
                        fileChunkRepository.countByFileId(m.getId()),
                        m.getStatus(),
                        m.getCreatedAt(),
                        m.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional
    public void deleteFile(String fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Upload not found: " + fileId));
        fileChunkRepository.deleteByFileId(fileId);
        fileMetadataRepository.delete(metadata);
        log.info("Deleted file {}", fileId);
    }

    public FileMetadata getMetadata(String fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Upload not found: " + fileId));
    }

    private int findNextMissingChunkIndex(List<Integer> uploadedChunks, int totalChunks) {
        Set<Integer> set = new HashSet<>(uploadedChunks);
        for (int i = 0; i < totalChunks; i++) {
            if (!set.contains(i)) {
                return i;
            }
        }
        return -1;
    }
}
