package com.example.fileupload;

import com.example.fileupload.config.EnvLoader;
import com.example.fileupload.dto.ChunkUploadResponse;
import com.example.fileupload.dto.FileInitRequest;
import com.example.fileupload.dto.FileInitResponse;
import com.example.fileupload.dto.FileStatusResponse;
import com.example.fileupload.entity.UploadStatus;
import com.example.fileupload.service.FileUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileUploadApplicationTests {

    static {
        EnvLoader.load();
    }

    @Autowired
    private FileUploadService fileUploadService;

    @Test
    void contextLoads() {
        assertNotNull(fileUploadService);
    }

    @Test
    void shouldResumeUploadFromInterruptedChunkAndMatchContent() throws IOException {
        byte[] payload = new byte[25 * 1024];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i % 127);
        }

        int chunkSize = 10 * 1024;
        String fileName = "integration_test_file.bin";
        String fileIdentifier = "test_run_" + System.currentTimeMillis();

        FileInitRequest initRequest = new FileInitRequest(
                fileName,
                (long) payload.length,
                "application/octet-stream",
                chunkSize,
                fileIdentifier
        );

        FileInitResponse initResp = fileUploadService.initOrResumeUpload(initRequest);
        assertNotNull(initResp.fileId());
        assertEquals(3, initResp.totalChunks());
        assertEquals(0, initResp.nextChunkIndex());
        assertFalse(initResp.resumed());

        String fileId = initResp.fileId();

        // Upload first chunk (0)
        byte[] chunk0Bytes = Arrays.copyOfRange(payload, 0, 10 * 1024);
        ChunkUploadResponse chunk0 = fileUploadService.uploadChunk(
                fileId,
                0,
                new MockMultipartFile("file", "c0.bin", "application/octet-stream", chunk0Bytes),
                null
        );
        assertEquals(1, chunk0.uploadedChunksCount());
        assertFalse(chunk0.fileCompleted());

        // Reconnect with same identifier - should resume from chunk 1
        FileInitResponse resumeResp = fileUploadService.initOrResumeUpload(initRequest);
        assertTrue(resumeResp.resumed());
        assertEquals(fileId, resumeResp.fileId());
        assertEquals(1, resumeResp.nextChunkIndex());

        // Upload chunk 1
        byte[] chunk1Bytes = Arrays.copyOfRange(payload, 10 * 1024, 20 * 1024);
        fileUploadService.uploadChunk(
                fileId,
                1,
                new MockMultipartFile("file", "c1.bin", "application/octet-stream", chunk1Bytes),
                null
        );

        // Upload final chunk (2)
        byte[] chunk2Bytes = Arrays.copyOfRange(payload, 20 * 1024, payload.length);
        ChunkUploadResponse finalChunk = fileUploadService.uploadChunk(
                fileId,
                2,
                new MockMultipartFile("file", "c2.bin", "application/octet-stream", chunk2Bytes),
                null
        );
        assertTrue(finalChunk.fileCompleted());
        assertEquals(UploadStatus.COMPLETED, finalChunk.status());

        // Verify status
        FileStatusResponse status = fileUploadService.getFileStatus(fileId);
        assertEquals(UploadStatus.COMPLETED, status.status());
        assertEquals(100.0, status.progressPercentage());

        // Stream and verify byte-level integrity
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        fileUploadService.streamFile(fileId, outputStream);
        assertArrayEquals(payload, outputStream.toByteArray());

        fileUploadService.deleteFile(fileId);
    }
}
