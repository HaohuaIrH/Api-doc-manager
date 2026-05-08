package com.apidoc.repository;

import com.apidoc.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByEndpointId(Long endpointId);

    List<TestCase> findByType(TestCase.TestType type);

    List<TestCase> findByPriority(TestCase.TestPriority priority);

    List<TestCase> findByEnabled(Boolean enabled);

    List<TestCase> findByEndpointIdAndEnabled(Long endpointId, Boolean enabled);

    void deleteByEndpointId(Long endpointId);
}
