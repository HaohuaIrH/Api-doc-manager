package com.apidoc.repository;

import com.apidoc.entity.ApiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiDocumentRepository extends JpaRepository<ApiDocument, Long> {

    @Query("SELECT d FROM ApiDocument d WHERE d.project.id = :projectId AND d.deletedAt IS NULL")
    List<ApiDocument> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT DISTINCT d FROM ApiDocument d " +
           "LEFT JOIN FETCH d.project " +
           "LEFT JOIN FETCH d.endpoints ep " +
           "LEFT JOIN FETCH ep.parameters " +
           "LEFT JOIN FETCH ep.responses " +
           "WHERE d.id = :documentId AND d.deletedAt IS NULL")
    Optional<ApiDocument> findByIdWithDetails(@Param("documentId") Long documentId);

    @Query("SELECT DISTINCT d FROM ApiDocument d " +
           "LEFT JOIN FETCH d.project " +
           "LEFT JOIN FETCH d.endpoints ep " +
           "LEFT JOIN FETCH ep.parameters " +
           "LEFT JOIN FETCH ep.responses " +
           "WHERE d.project.id = :projectId AND d.deletedAt IS NULL")
    List<ApiDocument> findByProjectIdWithDetails(@Param("projectId") Long projectId);

    @Query("SELECT d FROM ApiDocument d WHERE d.id = :documentId AND d.deletedAt IS NULL")
    Optional<ApiDocument> findByIdWithProject(@Param("documentId") Long documentId);

    @Query("SELECT d FROM ApiDocument d WHERE d.project.id = :projectId AND d.parent IS NULL AND d.deletedAt IS NULL")
    List<ApiDocument> findByProjectIdAndParentIsNull(@Param("projectId") Long projectId);

    @Query("SELECT d FROM ApiDocument d WHERE d.project.id = :projectId AND d.parent.id = :parentId AND d.deletedAt IS NULL")
    List<ApiDocument> findByProjectIdAndParentId(@Param("projectId") Long projectId, @Param("parentId") Long parentId);

    List<ApiDocument> findByStatus(ApiDocument.DocumentStatus status);

    @Query("SELECT d FROM ApiDocument d WHERE d.project.id = :projectId AND d.name LIKE %:keyword% AND d.deletedAt IS NULL")
    List<ApiDocument> searchByName(@Param("projectId") Long projectId, @Param("keyword") String keyword);
}
