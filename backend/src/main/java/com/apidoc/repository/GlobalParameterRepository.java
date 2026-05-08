package com.apidoc.repository;

import com.apidoc.entity.GlobalParameter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalParameterRepository extends JpaRepository<GlobalParameter, Long> {

    List<GlobalParameter> findByParentIsNull();

    List<GlobalParameter> findByParentIsNotNull();

    List<GlobalParameter> findByParentId(Long parentId);

    @Query("SELECT DISTINCT g FROM GlobalParameter g LEFT JOIN FETCH g.children WHERE g.parent IS NULL ORDER BY g.sortOrder")
    List<GlobalParameter> findAllWithChildren();

    @Query("SELECT DISTINCT g FROM GlobalParameter g LEFT JOIN FETCH g.children WHERE g.parent IS NULL")
    List<GlobalParameter> findAllRootWithChildren();

    @Query("SELECT g FROM GlobalParameter g LEFT JOIN FETCH g.children WHERE g.parent IS NULL ORDER BY g.sortOrder")
    List<GlobalParameter> findAllWithNestedChildren();

    @Query("SELECT g FROM GlobalParameter g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<GlobalParameter> searchByName(@Param("keyword") String keyword);

    Optional<GlobalParameter> findByName(String name);

    @Query("SELECT g FROM GlobalParameter g WHERE g.dataType IN ('STRING', 'INTEGER', 'LONG', 'DOUBLE', 'BOOLEAN') AND g.parent IS NULL")
    List<GlobalParameter> findAllSimpleTypes();

    @Query("SELECT g FROM GlobalParameter g WHERE g.dataType IN ('OBJECT', 'ARRAY') AND g.parent IS NULL")
    List<GlobalParameter> findAllComplexTypes();

    @Query("SELECT COUNT(g) FROM GlobalParameter g WHERE g.name = :name AND g.id != :id")
    long countByNameExcludingId(@Param("name") String name, @Param("id") Long id);
}
