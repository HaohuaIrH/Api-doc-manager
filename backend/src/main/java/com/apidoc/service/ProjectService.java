package com.apidoc.service;

import com.apidoc.dto.ProjectDTO;
import com.apidoc.entity.Project;
import com.apidoc.entity.User;
import com.apidoc.repository.ProjectRepository;
import com.apidoc.repository.UserRepository;
import com.apidoc.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProjectDTO> findAccessibleProjects() {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return List.of();
        }
        return projectRepository.findAccessibleProjects(currentUserId).stream()
                .filter(project -> !project.isDeleted())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> findMyProjects() {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return List.of();
        }
        return projectRepository.findByOwnerId(currentUserId).stream()
                .filter(project -> !project.isDeleted())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO findById(Long id) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        return projectRepository.findById(id)
                .filter(project -> !project.isDeleted())
                .filter(project -> hasAccess(project, currentUserId))
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional
    public ProjectDTO create(ProjectDTO dto) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("User not authenticated");
        }

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setBaseUrl(dto.getBaseUrl());
        project.setVersion(dto.getVersion() != null ? dto.getVersion() : "1.0.0");
        project.setVisibility(Project.Visibility.PRIVATE);
        project.setTags(dto.getTags());
        project.setOwner(owner);

        return toDTO(projectRepository.save(project));
    }

    @Transactional
    public ProjectDTO update(Long id, ProjectDTO dto) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        return projectRepository.findById(id)
                .filter(project -> !project.isDeleted())
                .filter(project -> isOwner(project, currentUserId))
                .map(project -> {
                    project.setName(dto.getName());
                    project.setDescription(dto.getDescription());
                    project.setBaseUrl(dto.getBaseUrl());
                    project.setVersion(dto.getVersion());
                    project.setTags(dto.getTags());
                    if (dto.getVisibility() != null) {
                        project.setVisibility(Project.Visibility.valueOf(dto.getVisibility()));
                    }
                    return toDTO(projectRepository.save(project));
                })
                .orElse(null);
    }

    @Transactional
    public boolean delete(Long id) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        return projectRepository.findById(id)
                .filter(project -> !project.isDeleted())
                .filter(project -> isOwner(project, currentUserId))
                .map(project -> {
                    project.setDeletedAt(LocalDateTime.now());
                    projectRepository.save(project);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean deletePermanently(Long id) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        return projectRepository.findById(id)
                .filter(project -> isOwner(project, currentUserId))
                .map(project -> {
                    projectRepository.delete(project);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public Map<String, Object> batchDelete(List<Long> ids) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return Map.of(
                "success", false,
                "message", "User not authenticated"
            );
        }

        int successCount = 0;
        int failCount = 0;
        List<Long> failedIds = new java.util.ArrayList<>();

        for (Long id : ids) {
            try {
                if (delete(id)) {
                    successCount++;
                } else {
                    failCount++;
                    failedIds.add(id);
                }
            } catch (Exception e) {
                failCount++;
                failedIds.add(id);
            }
        }

        return Map.of(
            "success", failCount == 0,
            "successCount", successCount,
            "failCount", failCount,
            "failedIds", failedIds
        );
    }

    @Transactional
    public Map<String, Object> batchDeletePermanently(List<Long> ids) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return Map.of(
                "success", false,
                "message", "User not authenticated"
            );
        }

        int successCount = 0;
        int failCount = 0;
        List<Long> failedIds = new java.util.ArrayList<>();

        for (Long id : ids) {
            try {
                if (deletePermanently(id)) {
                    successCount++;
                } else {
                    failCount++;
                    failedIds.add(id);
                }
            } catch (Exception e) {
                failCount++;
                failedIds.add(id);
            }
        }

        return Map.of(
            "success", failCount == 0,
            "successCount", successCount,
            "failCount", failCount,
            "failedIds", failedIds
        );
    }

    private boolean hasAccess(Project project, Long userId) {
        if (project == null) {
            return false;
        }
        if (project.isDeleted()) {
            return false;
        }
        if (project.getOwner() != null && project.getOwner().getId().equals(userId)) {
            return true;
        }
        if (project.getVisibility() == Project.Visibility.PUBLIC) {
            return true;
        }
        return false;
    }

    private boolean isOwner(Project project, Long userId) {
        return project != null
                && project.getOwner() != null
                && project.getOwner().getId().equals(userId);
    }

    private ProjectDTO toDTO(Project project) {
        return ProjectDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .baseUrl(project.getBaseUrl())
                .version(project.getVersion())
                .ownerId(project.getOwner() != null ? project.getOwner().getId() : null)
                .ownerUsername(project.getOwner() != null ? project.getOwner().getUsername() : null)
                .visibility(project.getVisibility() != null ? project.getVisibility().name() : null)
                .tags(project.getTags())
                .deletedAt(project.getDeletedAt())
                .build();
    }
}
