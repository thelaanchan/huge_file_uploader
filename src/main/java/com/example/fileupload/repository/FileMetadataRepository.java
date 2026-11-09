package com.example.fileupload.repository;

import com.example.fileupload.entity.FileMetadata;
import com.example.fileupload.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

    Optional<FileMetadata> findByFileIdentifier(String fileIdentifier);

    List<FileMetadata> findAllByOrderByCreatedAtDesc();

    boolean existsByFileIdentifierAndStatus(String fileIdentifier, UploadStatus status);
}

