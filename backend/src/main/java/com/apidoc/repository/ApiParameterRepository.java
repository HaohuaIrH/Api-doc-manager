package com.apidoc.repository;

import com.apidoc.entity.ApiParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiParameterRepository extends JpaRepository<ApiParameter, Long> {

    @Query("SELECT p FROM ApiParameter p WHERE p.endpoint.id = :endpointId")
    List<ApiParameter> findByEndpointId(@Param("endpointId") Long endpointId);

    @Query("SELECT p FROM ApiParameter p WHERE p.endpoint.id = :endpointId AND p.location = :location")
    List<ApiParameter> findByEndpointIdAndLocation(@Param("endpointId") Long endpointId, @Param("location") ApiParameter.ParameterLocation location);

    @Query("SELECT p FROM ApiParameter p WHERE p.endpoint.id = :endpointId ORDER BY p.sortOrder ASC")
    List<ApiParameter> findByEndpointIdOrderBySortOrderAsc(@Param("endpointId") Long endpointId);

    @Query("SELECT p FROM ApiParameter p WHERE p.required = :required")
    List<ApiParameter> findByRequired(@Param("required") Boolean required);

    void deleteByEndpointId(Long endpointId);
}
