package com.apidoc.repository;

import com.apidoc.entity.ApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long> {

    @Query("SELECT e FROM ApiEndpoint e WHERE e.document.id = :documentId AND e.document.deletedAt IS NULL")
    List<ApiEndpoint> findByDocumentId(@Param("documentId") Long documentId);

    @Query("SELECT DISTINCT e FROM ApiEndpoint e " +
           "LEFT JOIN FETCH e.document d " +
           "LEFT JOIN FETCH d.project " +
           "LEFT JOIN FETCH e.parameters " +
           "LEFT JOIN FETCH e.responses " +
           "WHERE e.document.id = :documentId AND e.document.deletedAt IS NULL AND d.deletedAt IS NULL")
    List<ApiEndpoint> findByDocumentIdWithDetails(@Param("documentId") Long documentId);

    @Query("SELECT e FROM ApiEndpoint e WHERE e.document.id = :documentId AND e.document.deletedAt IS NULL AND e.path = :path AND e.method = :method")
    Optional<ApiEndpoint> findByDocumentIdAndPathAndMethod(@Param("documentId") Long documentId, @Param("path") String path, @Param("method") ApiEndpoint.HttpMethod method);

    @Query("SELECT e FROM ApiEndpoint e WHERE e.deprecated = :deprecated AND e.document.deletedAt IS NULL")
    List<ApiEndpoint> findByDeprecated(@Param("deprecated") Boolean deprecated);

    @Query("SELECT e FROM ApiEndpoint e WHERE e.document.id = :documentId AND e.document.deletedAt IS NULL AND e.path LIKE %:path%")
    List<ApiEndpoint> searchByPath(@Param("documentId") Long documentId, @Param("path") String path);

    @Query("SELECT e FROM ApiEndpoint e WHERE e.document.project.id = :projectId AND e.document.deletedAt IS NULL")
    List<ApiEndpoint> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT DISTINCT e FROM ApiEndpoint e " +
           "LEFT JOIN FETCH e.document d " +
           "LEFT JOIN FETCH d.project " +
           "LEFT JOIN FETCH e.parameters " +
           "LEFT JOIN FETCH e.responses " +
           "WHERE e.document.project.id = :projectId AND e.document.deletedAt IS NULL AND d.deletedAt IS NULL")
    List<ApiEndpoint> findByProjectIdWithDetails(@Param("projectId") Long projectId);

    @Query("SELECT e FROM ApiEndpoint e JOIN e.document d WHERE d.project.id = :projectId AND e.method = :method AND e.document.deletedAt IS NULL")
    List<ApiEndpoint> findByProjectIdAndMethod(@Param("projectId") Long projectId, @Param("method") ApiEndpoint.HttpMethod method);
}
