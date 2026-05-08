package com.apidoc.repository;

import com.apidoc.entity.ParameterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParameterTemplateRepository extends JpaRepository<ParameterTemplate, Long> {

    List<ParameterTemplate> findByDocumentId(Long documentId);

    List<ParameterTemplate> findByFolderName(String folderName);

    List<ParameterTemplate> findByFolderNameAndDocumentId(String folderName, Long documentId);

    Optional<ParameterTemplate> findByFolderNameAndTemplateName(String folderName, String templateName);

    @Query("SELECT DISTINCT p.folderName FROM ParameterTemplate p WHERE p.documentId = :documentId")
    List<String> findDistinctFolderNamesByDocumentId(Long documentId);

    @Query("SELECT p FROM ParameterTemplate p WHERE p.documentId = :documentId ORDER BY p.folderName, p.createdAt")
    List<ParameterTemplate> findAllByDocumentIdOrderByFolderName(Long documentId);

    void deleteByFolderName(String folderName);
}
