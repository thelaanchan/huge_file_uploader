package com.example.fileupload.repository;

import com.example.fileupload.entity.FileChunk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileChunkRepository extends JpaRepository<FileChunk, Long> {

    boolean existsByFileIdAndChunkIndex(String fileId, Integer chunkIndex);

    Optional<FileChunk> findByFileIdAndChunkIndex(String fileId, Integer chunkIndex);

    int countByFileId(String fileId);

    @Query("SELECT c.chunkIndex FROM FileChunk c WHERE c.fileId = :fileId ORDER BY c.chunkIndex ASC")
    List<Integer> findChunkIndicesByFileId(@Param("fileId") String fileId);

    @Query("SELECT c FROM FileChunk c WHERE c.fileId = :fileId ORDER BY c.chunkIndex ASC")
    List<FileChunk> findChunksByFileIdPaged(@Param("fileId") String fileId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM FileChunk c WHERE c.fileId = :fileId")
    void deleteByFileId(@Param("fileId") String fileId);
}

