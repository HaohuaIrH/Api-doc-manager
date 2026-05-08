package com.apidoc.service;

import com.apidoc.dto.DocumentDTO;
import com.apidoc.entity.ApiDocument;
import com.apidoc.entity.Project;
import com.apidoc.entity.User;
import com.apidoc.repository.ApiDocumentRepository;
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
public class DocumentService {

    private final ApiDocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DocumentDTO> findByProjectId(Long projectId) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return List.of();
        }

        if (!hasProjectAccess(projectId, currentUserId)) {
            return List.of();
        }

        return documentRepository.findByProjectId(projectId).stream()
                .filter(doc -> !doc.isDeleted())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentDTO findById(Long id) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        return documentRepository.findById(id)
                .filter(doc -> !doc.isDeleted())
                .filter(doc -> hasProjectAccess(doc.getProject().getId(), currentUserId))
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional
    public DocumentDTO create(DocumentDTO dto) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("User not authenticated");
        }

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!hasProjectOwnership(project.getId(), currentUserId)) {
            throw new SecurityException("No permission to create document in this project");
        }

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ApiDocument document = new ApiDocument();
        document.setName(dto.getName());
        document.setDescription(dto.getDescription());
        document.setVersion(dto.getVersion() != null ? dto.getVersion() : "1.0.0");
        document.setStatus(ApiDocument.DocumentStatus.DRAFT);
        document.setTags(sanitizeTags(dto.getTags()));
        document.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        document.setProject(project);
        document.setCreatedBy(creator);

        if (dto.getParentId() != null) {
            ApiDocument parent = documentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent document not found"));
            document.setParent(parent);
        }

        return toDTO(documentRepository.save(document));
    }

    @Transactional
    public DocumentDTO update(Long id, DocumentDTO dto) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        return documentRepository.findById(id)
                .filter(doc -> !doc.isDeleted())
                .filter(doc -> hasProjectOwnership(doc.getProject().getId(), currentUserId))
                .map(document -> {
                    document.setName(dto.getName());
                    document.setDescription(dto.getDescription());
                    document.setVersion(dto.getVersion());
                    document.setTags(sanitizeTags(dto.getTags()));
                    if (dto.getStatus() != null) {
                        document.setStatus(ApiDocument.DocumentStatus.valueOf(dto.getStatus()));
                    }
                    return toDTO(documentRepository.save(document));
                })
                .orElse(null);
    }

    @Transactional
    public boolean delete(Long id) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        return documentRepository.findById(id)
                .filter(doc -> !doc.isDeleted())
                .filter(doc -> hasProjectOwnership(doc.getProject().getId(), currentUserId))
                .map(document -> {
                    document.setDeletedAt(LocalDateTime.now());
                    documentRepository.save(document);
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

        return documentRepository.findById(id)
                .filter(doc -> hasProjectOwnership(doc.getProject().getId(), currentUserId))
                .map(document -> {
                    documentRepository.delete(document);
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

    private boolean hasProjectAccess(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .filter(project -> !project.isDeleted())
                .map(project -> {
                    if (project.getOwner() != null && project.getOwner().getId().equals(userId)) {
                        return true;
                    }
                    if (project.getVisibility() == Project.Visibility.PUBLIC) {
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private boolean hasProjectOwnership(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .filter(project -> !project.isDeleted())
                .map(project -> project.getOwner() != null && project.getOwner().getId().equals(userId))
                .orElse(false);
    }

    private DocumentDTO toDTO(ApiDocument document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .projectId(document.getProject() != null ? document.getProject().getId() : null)
                .name(document.getName())
                .description(document.getDescription())
                .version(document.getVersion())
                .status(document.getStatus() != null ? document.getStatus().name() : null)
                .tags(document.getTags())
                .parentId(document.getParent() != null ? document.getParent().getId() : null)
                .createdBy(document.getCreatedBy() != null ? document.getCreatedBy().getId() : null)
                .sortOrder(document.getSortOrder())
                .deletedAt(document.getDeletedAt())
                .build();
    }

    private String sanitizeTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return "[]";
        }
        String trimmed = tags.trim();
        if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
            return "[]";
        }
        return trimmed;
    }
}
