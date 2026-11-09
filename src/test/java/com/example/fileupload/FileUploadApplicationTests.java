package com.example.fileupload;

import com.example.fileupload.dto.ChunkUploadResponse;
import com.example.fileupload.dto.FileInitRequest;
import com.example.fileupload.dto.FileInitResponse;
import com.example.fileupload.dto.FileStatusResponse;
import com.example.fileupload.entity.UploadStatus;
import com.example.fileupload.service.FileUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileUploadApplicationTests {

    @Autowired
    private FileUploadService fileUploadService;

    @Test
    @DisplayName("Context Loads")
    void contextLoads() {
        assertNotNull(fileUploadService);
    }

    @Test
    @DisplayName("End-to-End Test: Chunked Upload, Interruption, Resumption, and Stream Verification")
    void testChunkedUploadAndResumption() throws IOException {
        // Create 25 KB simulated file payload
        byte[] fullContent = new byte[25 * 1024];
        for (int i = 0; i < fullContent.length; i++) {
            fullContent[i] = (byte) (i % 127);
        }

        int chunkSize = 10 * 1024; // 10 KB chunks -> 3 chunks total (10KB, 10KB, 5KB)
        String fileName = "test_large_file.dat";
        String fileIdentifier = "unique_test_file_" + System.currentTimeMillis();

        // 1. Initialize upload
        FileInitRequest initRequest = new FileInitRequest(
                fileName,
                (long) fullContent.length,
                "application/octet-stream",
                chunkSize,
                fileIdentifier
        );

        FileInitResponse initResp = fileUploadService.initOrResumeUpload(initRequest);
        assertNotNull(initResp.getFileId());
        assertEquals(3, initResp.getTotalChunks());
        assertEquals(0, initResp.getNextChunkIndex());
        assertFalse(initResp.isResumed());
        assertEquals(UploadStatus.INITIALIZED, initResp.getStatus());

        String fileId = initResp.getFileId();

        // 2. Upload Chunk 0
        byte[] chunk0Bytes = Arrays.copyOfRange(fullContent, 0, 10 * 1024);
        MockMultipartFile chunk0File = new MockMultipartFile("file", "chunk0.bin", "application/octet-stream", chunk0Bytes);
        ChunkUploadResponse chunk0Resp = fileUploadService.uploadChunk(fileId, 0, chunk0File, null);
        assertEquals(1, chunk0Resp.getUploadedChunksCount());
        assertFalse(chunk0Resp.isFileCompleted());
        assertEquals(UploadStatus.UPLOADING, chunk0Resp.getStatus());

        // 3. Simulate failure / interruption!
        // Suppose network dropped after Chunk 0.
        // Client reconnects and calls initOrResumeUpload again with the same fileIdentifier:
        FileInitResponse resumeResp = fileUploadService.initOrResumeUpload(initRequest);
        assertTrue(resumeResp.isResumed(), "Server should detect existing upload and allow resuming");
        assertEquals(fileId, resumeResp.getFileId());
        assertEquals(1, resumeResp.getNextChunkIndex(), "Should tell client to start at chunk 1");
        assertTrue(resumeResp.getUploadedChunks().contains(0), "Chunk 0 should already be recorded");

        // 4. Client resumes upload from chunk 1 instead of starting over!
        byte[] chunk1Bytes = Arrays.copyOfRange(fullContent, 10 * 1024, 20 * 1024);
        MockMultipartFile chunk1File = new MockMultipartFile("file", "chunk1.bin", "application/octet-stream", chunk1Bytes);
        ChunkUploadResponse chunk1Resp = fileUploadService.uploadChunk(fileId, 1, chunk1File, null);
        assertEquals(2, chunk1Resp.getUploadedChunksCount());
        assertFalse(chunk1Resp.isFileCompleted());

        // 5. Upload final chunk (Chunk 2)
        byte[] chunk2Bytes = Arrays.copyOfRange(fullContent, 20 * 1024, fullContent.length);
        MockMultipartFile chunk2File = new MockMultipartFile("file", "chunk2.bin", "application/octet-stream", chunk2Bytes);
        ChunkUploadResponse chunk2Resp = fileUploadService.uploadChunk(fileId, 2, chunk2File, null);
        assertEquals(3, chunk2Resp.getUploadedChunksCount());
        assertTrue(chunk2Resp.isFileCompleted());
        assertEquals(UploadStatus.COMPLETED, chunk2Resp.getStatus());

        // 6. Check final status
        FileStatusResponse statusResp = fileUploadService.getFileStatus(fileId);
        assertEquals(UploadStatus.COMPLETED, statusResp.getStatus());
        assertEquals(100.0, statusResp.getProgressPercentage());
        assertEquals(-1, statusResp.getNextChunkIndex());

        // 7. Verify streaming download reconstructed exact original byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        fileUploadService.streamFile(fileId, outputStream);
        byte[] downloadedBytes = outputStream.toByteArray();

        assertArrayEquals(fullContent, downloadedBytes, "Reconstructed file must match original content bit-for-bit");

        // Clean up
        fileUploadService.deleteFile(fileId);
    }
}

