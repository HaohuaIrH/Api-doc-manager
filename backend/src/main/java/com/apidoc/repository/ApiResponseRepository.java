package com.apidoc.repository;

import com.apidoc.entity.ApiResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiResponseRepository extends JpaRepository<ApiResponse, Long> {

    @Query("SELECT r FROM ApiResponse r WHERE r.endpoint.id = :endpointId")
    List<ApiResponse> findByEndpointId(@Param("endpointId") Long endpointId);

    @Query("SELECT r FROM ApiResponse r WHERE r.endpoint.id = :endpointId AND r.statusCode = :statusCode")
    Optional<ApiResponse> findByEndpointIdAndStatusCode(@Param("endpointId") Long endpointId, @Param("statusCode") String statusCode);

    @Query("SELECT r FROM ApiResponse r WHERE r.endpoint.id = :endpointId AND r.isDefault = true")
    Optional<ApiResponse> findByEndpointIdAndIsDefaultTrue(@Param("endpointId") Long endpointId);

    void deleteByEndpointId(Long endpointId);
}
