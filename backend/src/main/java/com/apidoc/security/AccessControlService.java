package com.apidoc.security;

import com.apidoc.entity.ApiDocument;
import com.apidoc.entity.Project;
import com.apidoc.repository.ApiDocumentRepository;
import com.apidoc.repository.ApiEndpointRepository;
import com.apidoc.repository.ApiParameterRepository;
import com.apidoc.repository.ApiResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资源访问控制服务
 *
 * 统一判定 Endpoint / Parameter / Response / GlobalParameter 各链路的访问权限：
 * - 读：项目 owner 或 PUBLIC 项目（与 ProjectService/DocumentService 的可见性语义一致）
 * - 改/删：仅项目 owner
 *
 * 判定方法标注只读事务，便于在 Controller（非事务）中安全地懒加载实体关联；
 * 权限不足抛出 AccessDeniedException，由 GlobalExceptionHandler 映射为 403。
 */
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final ApiDocumentRepository documentRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ApiParameterRepository parameterRepository;
    private final ApiResponseRepository responseRepository;

    /**
     * 获取当前登录用户ID，未登录时抛出异常（映射为 401）
     */
    public Long requireCurrentUserId() {
        Long userId = SecurityContextHelper.getCurrentUserId();
        if (userId == null) {
            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        }
        return userId;
    }

    // ==================== Endpoint 链路 ====================

    /**
     * 用户是否可读文档（用于按文档查询端点前的校验）
     */
    @Transactional(readOnly = true)
    public boolean canReadDocument(Long documentId, Long userId) {
        return documentRepository.findById(documentId)
                .filter(document -> !document.isDeleted())
                .map(document -> isProjectReadable(document.getProject(), userId))
                .orElse(false);
    }

    /**
     * 要求当前用户为文档所属项目的 owner，否则抛出 403
     */
    @Transactional(readOnly = true)
    public void requireDocumentOwnership(Long documentId) {
        Long userId = requireCurrentUserId();
        if (!isProjectOwner(documentRepository.findById(documentId)
                .map(ApiDocument::getProject)
                .orElse(null), userId)) {
            throw new AccessDeniedException("只有项目属主才能修改该文档资源");
        }
    }

    /**
     * 用户是否可读端点
     */
    @Transactional(readOnly = true)
    public boolean canReadEndpoint(Long endpointId, Long userId) {
        return endpointRepository.findById(endpointId)
                .map(endpoint -> isProjectReadable(endpoint.getDocument().getProject(), userId))
                .orElse(false);
    }

    /**
     * 要求当前用户为端点所属项目的 owner，否则抛出 403
     */
    @Transactional(readOnly = true)
    public void requireEndpointOwnership(Long endpointId) {
        Long userId = requireCurrentUserId();
        if (!isProjectOwner(endpointRepository.findById(endpointId)
                .map(endpoint -> endpoint.getDocument().getProject())
                .orElse(null), userId)) {
            throw new AccessDeniedException("只有项目属主才能修改该端点资源");
        }
    }

    // ==================== Parameter 链路 ====================

    /**
     * 要求当前用户为参数所属项目的 owner，否则抛出 403
     */
    @Transactional(readOnly = true)
    public void requireParameterOwnership(Long parameterId) {
        Long userId = requireCurrentUserId();
        if (!isProjectOwner(parameterRepository.findById(parameterId)
                .map(parameter -> parameter.getEndpoint().getDocument().getProject())
                .orElse(null), userId)) {
            throw new AccessDeniedException("只有项目属主才能修改该参数资源");
        }
    }

    // ==================== Response 链路 ====================

    /**
     * 要求当前用户为响应定义所属项目的 owner，否则抛出 403
     */
    @Transactional(readOnly = true)
    public void requireResponseOwnership(Long responseId) {
        Long userId = requireCurrentUserId();
        if (!isProjectOwner(responseRepository.findById(responseId)
                .map(response -> response.getEndpoint().getDocument().getProject())
                .orElse(null), userId)) {
            throw new AccessDeniedException("只有项目属主才能修改该响应定义");
        }
    }

    // ==================== 私有判定 ====================

    private boolean isProjectReadable(Project project, Long userId) {
        if (project == null || project.isDeleted()) {
            return false;
        }
        return isProjectOwner(project, userId) || project.getVisibility() == Project.Visibility.PUBLIC;
    }

    private boolean isProjectOwner(Project project, Long userId) {
        return project != null
                && !project.isDeleted()
                && project.getOwner() != null
                && project.getOwner().getId() != null
                && project.getOwner().getId().equals(userId);
    }
}
