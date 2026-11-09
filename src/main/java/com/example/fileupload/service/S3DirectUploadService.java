package com.example.fileupload.service;

import com.example.fileupload.dto.s3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class S3DirectUploadService {

    private static final Logger log = LoggerFactory.getLogger(S3DirectUploadService.class);

    private static final int DEFAULT_PART_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Duration PRESIGNED_EXPIRATION = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket:fileupload-bucket}")
    private String bucket;

    public S3DirectUploadService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public S3MultipartInitResponse initiateMultipartUpload(S3MultipartInitRequest request) {
        int partSize = (request.partSize() != null && request.partSize() >= 5 * 1024 * 1024)
                ? request.partSize()
                : DEFAULT_PART_SIZE;

        int totalParts = Math.max(1, (int) Math.ceil((double) request.fileSize() / partSize));
        String key = "uploads/" + UUID.randomUUID() + "-" + sanitizeFileName(request.fileName());

        String contentType = (request.contentType() != null && !request.contentType().isBlank())
                ? request.contentType()
                : "application/octet-stream";

        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        log.info("Initiated S3 multipart upload: key={}, uploadId={}, totalParts={}", key, createResponse.uploadId(), totalParts);

        return new S3MultipartInitResponse(
                createResponse.uploadId(),
                key,
                bucket,
                partSize,
                totalParts
        );
    }

    public S3PartPresignedUrlResponse generatePartPresignedUrl(String uploadId, String key, int partNumber) {
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();

        UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                .signatureDuration(PRESIGNED_EXPIRATION)
                .uploadPartRequest(uploadPartRequest)
                .build();

        String presignedUrl = s3Presigner.presignUploadPart(presignRequest).url().toString();

        return new S3PartPresignedUrlResponse(
                uploadId,
                key,
                partNumber,
                presignedUrl,
                PRESIGNED_EXPIRATION.toMinutes()
        );
    }

    public S3MultipartCompleteResponse completeMultipartUpload(S3MultipartCompleteRequest request) {
        List<CompletedPart> completedParts = request.parts().stream()
                .sorted(Comparator.comparingInt(S3PartDto::partNumber))
                .map(p -> CompletedPart.builder()
                        .partNumber(p.partNumber())
                        .eTag(cleanETag(p.eTag()))
                        .build())
                .toList();

        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(request.key())
                .uploadId(request.uploadId())
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build();

        CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(completeRequest);
        log.info("Completed S3 multipart upload for key={}", request.key());

        return new S3MultipartCompleteResponse(
                request.key(),
                bucket,
                response.location(),
                "Multipart upload completed successfully."
        );
    }

    public void abortMultipartUpload(String uploadId, String key) {
        AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build();

        s3Client.abortMultipartUpload(abortRequest);
        log.info("Aborted S3 multipart upload: key={}, uploadId={}", key, uploadId);
    }

    public S3SingleUploadInitResponse createSingleUploadPresignedUrl(S3SingleUploadInitRequest request) {
        String key = "uploads/" + UUID.randomUUID() + "-" + sanitizeFileName(request.fileName());
        String contentType = (request.contentType() != null && !request.contentType().isBlank())
                ? request.contentType()
                : "application/octet-stream";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_EXPIRATION)
                .putObjectRequest(putObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

        return new S3SingleUploadInitResponse(
                presignedUrl,
                key,
                bucket,
                PRESIGNED_EXPIRATION.toMinutes()
        );
    }

    public String generateDownloadPresignedUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String cleanETag(String eTag) {
        if (eTag == null) return "";
        return eTag.replace("\"", "").trim();
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file.bin";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

