package com.example.fileupload;

import com.example.fileupload.config.EnvLoader;
import com.example.fileupload.dto.s3.S3PartPresignedUrlResponse;
import com.example.fileupload.dto.s3.S3SingleUploadInitRequest;
import com.example.fileupload.dto.s3.S3SingleUploadInitResponse;
import com.example.fileupload.service.S3DirectUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class S3DirectUploadServiceTest {

    static {
        EnvLoader.load();
    }

    @Autowired
    private S3DirectUploadService s3DirectUploadService;

    @Test
    void shouldGenerateSingleUploadPresignedUrl() {
        S3SingleUploadInitRequest request = new S3SingleUploadInitRequest("dataset.csv", "text/csv");
        S3SingleUploadInitResponse response = s3DirectUploadService.createSingleUploadPresignedUrl(request);

        assertNotNull(response.uploadUrl());
        assertTrue(response.uploadUrl().contains("X-Amz-Signature"));
        assertTrue(response.uploadUrl().contains("X-Amz-Algorithm"));
        assertTrue(response.key().startsWith("uploads/"));
        assertEquals("fileupload-bucket", response.bucket());
    }

    @Test
    void shouldGeneratePartPresignedUrl() {
        String uploadId = "dummy-upload-id-12345";
        String key = "uploads/test-file.zip";
        int partNumber = 1;

        S3PartPresignedUrlResponse response = s3DirectUploadService.generatePartPresignedUrl(uploadId, key, partNumber);

        assertNotNull(response.presignedUrl());
        assertTrue(response.presignedUrl().contains("partNumber=1"));
        assertTrue(response.presignedUrl().contains("uploadId=dummy-upload-id-12345"));
        assertTrue(response.presignedUrl().contains("X-Amz-Signature"));
    }

    @Test
    void shouldGenerateDownloadPresignedUrl() {
        String key = "uploads/report.pdf";
        String downloadUrl = s3DirectUploadService.generateDownloadPresignedUrl(key);

        assertNotNull(downloadUrl);
        assertTrue(downloadUrl.contains("X-Amz-Signature"));
        assertTrue(downloadUrl.contains(key));
    }
}

