package com.apidoc.service;

import com.apidoc.dto.ApiEndpointDTO;
import com.apidoc.dto.ParameterDTO;
import com.apidoc.dto.ResponseDTO;
import com.apidoc.entity.ApiDocument;
import com.apidoc.entity.ApiEndpoint;
import com.apidoc.entity.ApiParameter;
import com.apidoc.entity.ApiResponse;
import com.apidoc.entity.Project;
import com.apidoc.repository.ApiDocumentRepository;
import com.apidoc.repository.ApiEndpointRepository;
import com.apidoc.repository.ApiParameterRepository;
import com.apidoc.repository.ApiResponseRepository;
import com.apidoc.security.SecurityContextHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API端点服务 - 形式化接口管理，支持用户数据隔离
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApiEndpointService {

    private final ApiEndpointRepository endpointRepository;
    private final ApiDocumentRepository documentRepository;
    private final ApiParameterRepository parameterRepository;
    private final ApiResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取文档下的所有端点（需验证文档访问权限）
     */
    @Transactional(readOnly = true)
    public List<ApiEndpointDTO> getEndpointsByDocumentId(Long documentId) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return List.of();
        }

        // 验证文档访问权限
        if (!hasDocumentAccess(documentId, currentUserId)) {
            return List.of();
        }

        List<ApiEndpoint> endpoints = endpointRepository.findByDocumentId(documentId);
        return endpoints.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取端点详情（需验证访问权限）
     */
    @Transactional(readOnly = true)
    public ApiEndpointDTO getEndpointById(Long id) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        ApiEndpoint endpoint = endpointRepository.findById(id).orElse(null);
        if (endpoint == null) {
            return null;
        }

        // 验证文档访问权限
        if (!hasDocumentAccess(endpoint.getDocument().getId(), currentUserId)) {
            return null;
        }

        return toDTO(endpoint);
    }

    /**
     * 创建端点（需验证文档访问权限）
     */
    public ApiEndpointDTO createEndpoint(ApiEndpointDTO.CreateRequest request) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            throw new SecurityException("User not authenticated");
        }

        // 验证文档访问权限
        if (!hasDocumentAccess(request.getDocumentId(), currentUserId)) {
            throw new SecurityException("No permission to create endpoint in this document");
        }

        ApiDocument document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new RuntimeException("文档不存在: " + request.getDocumentId()));

        // 检查重复
        if (endpointRepository.findByDocumentIdAndPathAndMethod(
                request.getDocumentId(), request.getPath(), request.getMethod()).isPresent()) {
            throw new RuntimeException("该路径和方法的端点已存在");
        }

        ApiEndpoint endpoint = ApiEndpoint.builder()
                .document(document)
                .path(request.getPath())
                .method(request.getMethod())
                .summary(request.getSummary())
                .description(request.getDescription())
                .deprecated(request.getDeprecated() != null ? request.getDeprecated() : false)
                .operationId(request.getOperationId())
                .tags(request.getTags())
                .parameters(new HashSet<>())
                .responses(new HashSet<>())
                .build();

        endpoint = endpointRepository.save(endpoint);

        // 保存参数
        if (request.getParameters() != null) {
            for (ParameterDTO paramDTO : request.getParameters()) {
                ApiParameter param = paramDTO.toEntity();
                param.setEndpoint(endpoint);
                parameterRepository.save(param);
            }
        }

        // 保存响应
        if (request.getResponses() != null) {
            for (ResponseDTO respDTO : request.getResponses()) {
                ApiResponse resp = respDTO.toEntity();
                resp.setEndpoint(endpoint);
                responseRepository.save(resp);
            }
        }

        log.info("创建端点成功: {} {}", request.getMethod(), request.getPath());
        return getEndpointById(endpoint.getId());
    }

    /**
     * 更新端点（需验证访问权限）
     */
    public ApiEndpointDTO updateEndpoint(Long id, ApiEndpointDTO.UpdateRequest request) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        ApiEndpoint endpoint = endpointRepository.findById(id).orElse(null);
        if (endpoint == null) {
            return null;
        }

        // 验证文档访问权限
        if (!hasDocumentAccess(endpoint.getDocument().getId(), currentUserId)) {
            return null;
        }

        // 更新基本信息
        if (request.getPath() != null) endpoint.setPath(request.getPath());
        if (request.getMethod() != null) endpoint.setMethod(request.getMethod());
        if (request.getSummary() != null) endpoint.setSummary(request.getSummary());
        if (request.getDescription() != null) endpoint.setDescription(request.getDescription());
        if (request.getDeprecated() != null) endpoint.setDeprecated(request.getDeprecated());
        if (request.getOperationId() != null) endpoint.setOperationId(request.getOperationId());
        if (request.getTags() != null) endpoint.setTags(request.getTags());

        // 更新参数
        if (request.getParameters() != null) {
            parameterRepository.deleteByEndpointId(id);
            for (ParameterDTO paramDTO : request.getParameters()) {
                ApiParameter param = paramDTO.toEntity();
                param.setEndpoint(endpoint);
                parameterRepository.save(param);
            }
        }

        // 更新响应
        if (request.getResponses() != null) {
            responseRepository.deleteByEndpointId(id);
            for (ResponseDTO respDTO : request.getResponses()) {
                ApiResponse resp = respDTO.toEntity();
                resp.setEndpoint(endpoint);
                responseRepository.save(resp);
            }
        }

        endpoint = endpointRepository.save(endpoint);
        log.info("更新端点成功: {} {}", endpoint.getMethod(), endpoint.getPath());
        return toDTO(endpoint);
    }

    /**
     * 删除端点（需验证访问权限）
     */
    public boolean deleteEndpoint(Long id) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        ApiEndpoint endpoint = endpointRepository.findById(id).orElse(null);
        if (endpoint == null) {
            return false;
        }

        // 验证文档访问权限
        if (!hasDocumentAccess(endpoint.getDocument().getId(), currentUserId)) {
            return false;
        }

        endpointRepository.deleteById(id);
        log.info("删除端点成功: {}", id);
        return true;
    }

    /**
     * 根据项目ID获取所有端点（需验证项目访问权限）
     */
    @Transactional(readOnly = true)
    public List<ApiEndpointDTO> getEndpointsByProjectId(Long projectId) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return List.of();
        }

        // 验证项目访问权限
        if (!hasProjectAccess(projectId, currentUserId)) {
            return List.of();
        }

        List<ApiEndpoint> endpoints = endpointRepository.findByProjectId(projectId);
        return endpoints.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否有权访问文档
     */
    private boolean hasDocumentAccess(Long documentId, Long userId) {
        return documentRepository.findById(documentId)
                .map(doc -> hasProjectAccess(doc.getProject().getId(), userId))
                .orElse(false);
    }

    /**
     * 检查用户是否有权访问项目
     */
    private boolean hasProjectAccess(Long projectId, Long userId) {
        return documentRepository.findById(projectId)
                .map(doc -> doc.getProject())
                .map(project -> {
                    // 项目所有者
                    if (project.getOwner() != null && project.getOwner().getId().equals(userId)) {
                        return true;
                    }
                    // 公开项目
                    if (project.getVisibility() == Project.Visibility.PUBLIC) {
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * 转换为DTO
     */
    private ApiEndpointDTO toDTO(ApiEndpoint endpoint) {
        List<ApiParameter> parameters = parameterRepository.findByEndpointIdOrderBySortOrderAsc(endpoint.getId());
        List<ApiResponse> responses = responseRepository.findByEndpointId(endpoint.getId());

        return ApiEndpointDTO.builder()
                .id(endpoint.getId())
                .documentId(endpoint.getDocument().getId())
                .path(endpoint.getPath())
                .method(endpoint.getMethod())
                .summary(endpoint.getSummary())
                .description(endpoint.getDescription())
                .deprecated(endpoint.getDeprecated())
                .operationId(endpoint.getOperationId())
                .tags(endpoint.getTags())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .parameters(parameters.stream().map(ParameterDTO::fromEntity).collect(Collectors.toList()))
                .responses(responses.stream().map(ResponseDTO::fromEntity).collect(Collectors.toList()))
                .build();
    }
}
