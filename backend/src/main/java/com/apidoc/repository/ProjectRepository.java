package com.apidoc.repository;

import com.apidoc.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p WHERE p.owner.id = :ownerId AND p.deletedAt IS NULL")
    List<Project> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT p FROM Project p WHERE p.visibility = :visibility AND p.deletedAt IS NULL")
    List<Project> findByVisibility(@Param("visibility") Project.Visibility visibility);

    @Query("SELECT p FROM Project p WHERE (p.owner.id = :userId OR p.visibility = 'PUBLIC') AND p.deletedAt IS NULL")
    List<Project> findAccessibleProjects(@Param("userId") Long userId);

    @Query("SELECT p FROM Project p WHERE (p.name LIKE %:keyword% OR p.description LIKE %:keyword%) AND p.deletedAt IS NULL")
    List<Project> searchProjects(@Param("keyword") String keyword);
}
