package com.apidoc.export;

import com.apidoc.entity.*;
import com.apidoc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkdownExporter {

    private final ApiDocumentRepository documentRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ApiParameterRepository parameterRepository;
    private final ApiResponseRepository responseRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public String exportProject(Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("项目不存在: " + projectId));

            StringBuilder sb = new StringBuilder();
            sb.append(generateProjectHeader(project));

            List<ApiDocument> documents = documentRepository.findByProjectId(projectId);

            Map<String, List<ApiDocument>> groupedDocs = documents.stream()
                    .collect(Collectors.groupingBy(doc -> {
                        String tags = doc.getTags();
                        if (tags != null && tags.contains("[")) {
                            try {
                                String firstTag = tags.substring(1, tags.indexOf("]"));
                                return firstTag;
                            } catch (Exception e) {
                                return "未分类";
                            }
                        }
                        return "未分类";
                    }));

            int docIndex = 1;
            for (Map.Entry<String, List<ApiDocument>> entry : groupedDocs.entrySet()) {
                sb.append("\n## ").append(docIndex).append(". ").append(entry.getKey()).append("\n\n");

                int endpointIndex = 1;
                for (ApiDocument doc : entry.getValue()) {
                    sb.append(exportDocument(doc, docIndex + "." + endpointIndex));
                    endpointIndex++;
                }
                docIndex++;
            }

            sb.append(generateAppendix());
            return sb.toString();
        } catch (Exception e) {
            log.error("导出Markdown文档失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public String exportDocument(ApiDocument document, String prefix) {
        StringBuilder sb = new StringBuilder();

        if (document == null) {
            log.warn("文档为空，跳过导出");
            return sb.toString();
        }

        String name = document.getName();
        sb.append("### ").append(prefix).append(" ").append(name != null ? name : "未命名文档").append("\n\n");

        String description = document.getDescription();
        if (description != null && !description.isEmpty()) {
            sb.append(description).append("\n\n");
        }

        // 直接查询接口，避免懒加载
        List<ApiEndpoint> endpoints = endpointRepository.findByDocumentId(document.getId());

        int endpointCount = 1;
        for (ApiEndpoint endpoint : endpoints) {
            if (endpoint != null) {
                sb.append(exportEndpoint(endpoint, prefix + "." + endpointCount));
                endpointCount++;
            }
        }

        return sb.toString();
    }

    public String exportEndpoint(ApiEndpoint endpoint, String prefix) {
        StringBuilder sb = new StringBuilder();

        String methodBadge = getMethodBadge(endpoint.getMethod().name());
        sb.append("#### ").append(prefix).append(" ").append(methodBadge)
          .append(" `").append(endpoint.getPath()).append("`\n\n");

        String summary = endpoint.getSummary();
        if (summary != null && !summary.isEmpty()) {
            sb.append("*").append(summary).append("*\n\n");
        }

        String description = endpoint.getDescription();
        if (description != null && !description.isEmpty()) {
            sb.append(description).append("\n\n");
        }

        if (endpoint.getDeprecated() != null && endpoint.getDeprecated()) {
            sb.append("> **⚠️ 已废弃**\n\n");
        }

        sb.append("**请求信息**\n\n");
        sb.append("- 请求路径: `").append(endpoint.getPath()).append("`\n");
        sb.append("- 请求方法: ").append(endpoint.getMethod()).append("\n\n");

        // 直接查询参数，避免懒加载
        List<ApiParameter> params = parameterRepository.findByEndpointId(endpoint.getId());
        List<ApiParameter> headerParams = params.stream()
                .filter(p -> p.getLocation() == ApiParameter.ParameterLocation.HEADER)
                .collect(Collectors.toList());
        List<ApiParameter> pathParams = params.stream()
                .filter(p -> p.getLocation() == ApiParameter.ParameterLocation.PATH)
                .collect(Collectors.toList());
        List<ApiParameter> queryParams = params.stream()
                .filter(p -> p.getLocation() == ApiParameter.ParameterLocation.QUERY)
                .collect(Collectors.toList());
        List<ApiParameter> bodyParams = params.stream()
                .filter(p -> p.getLocation() == ApiParameter.ParameterLocation.REQUEST_BODY)
                .collect(Collectors.toList());

        if (!headerParams.isEmpty()) {
            sb.append("**请求头参数**\n\n");
            sb.append("| 参数名 | 类型 | 必填 | 描述 |\n");
            sb.append("|--------|------|------|------|\n");
            for (ApiParameter param : headerParams) {
                sb.append("| ").append(param.getName())
                  .append(" | ").append(param.getDataType() != null ? param.getDataType() : "string")
                  .append(" | ").append(param.getRequired() ? "是" : "否")
                  .append(" | ").append(param.getDescription() != null ? param.getDescription() : "-")
                  .append(" |\n");
            }
            sb.append("\n");
        }

        if (!pathParams.isEmpty()) {
            sb.append("**路径参数**\n\n");
            sb.append("| 参数名 | 类型 | 必填 | 描述 |\n");
            sb.append("|--------|------|------|------|\n");
            for (ApiParameter param : pathParams) {
                sb.append("| ").append(param.getName())
                  .append(" | ").append(param.getDataType() != null ? param.getDataType() : "string")
                  .append(" | ").append(param.getRequired() ? "是" : "否")
                  .append(" | ").append(param.getDescription() != null ? param.getDescription() : "-")
                  .append(" |\n");
            }
            sb.append("\n");
        }

        if (!queryParams.isEmpty()) {
            sb.append("**查询参数**\n\n");
            sb.append("| 参数名 | 类型 | 必填 | 描述 |\n");
            sb.append("|--------|------|------|------|\n");
            for (ApiParameter param : queryParams) {
                sb.append("| ").append(param.getName())
                  .append(" | ").append(param.getDataType() != null ? param.getDataType() : "string")
                  .append(" | ").append(param.getRequired() ? "是" : "否")
                  .append(" | ").append(param.getDescription() != null ? param.getDescription() : "-")
                  .append(" |\n");
            }
            sb.append("\n");
        }

        if (!bodyParams.isEmpty()) {
            sb.append("**请求体参数**\n\n");
            sb.append("| 参数名 | 类型 | 必填 | 描述 |\n");
            sb.append("|--------|------|------|------|\n");
            for (ApiParameter param : bodyParams) {
                sb.append("| ").append(param.getName())
                  .append(" | ").append(param.getDataType() != null ? param.getDataType() : "object")
                  .append(" | ").append(param.getRequired() ? "是" : "否")
                  .append(" | ").append(param.getDescription() != null ? param.getDescription() : "-")
                  .append(" |\n");
                
                if (param.getExample() != null && !param.getExample().isEmpty()) {
                    sb.append("\n").append("示例: `").append(param.getExample()).append("`\n");
                }
            }
            sb.append("\n");
        }

        // 直接查询响应，避免懒加载
        List<ApiResponse> responses = responseRepository.findByEndpointId(endpoint.getId());
        if (!responses.isEmpty()) {
            sb.append("**响应参数**\n\n");
            for (ApiResponse response : responses) {
                if (response.getStatusCode() != null) {
                    sb.append("**状态码 ").append(response.getStatusCode()).append("**");
                    if (response.getDescription() != null && !response.getDescription().isEmpty()) {
                        sb.append(": ").append(response.getDescription());
                    }
                    sb.append("\n\n");
                }
            }
        }

        sb.append("---\n\n");
        return sb.toString();
    }

    private String getMethodBadge(String method) {
        String color;
        switch (method.toUpperCase()) {
            case "GET": color = "green"; break;
            case "POST": color = "blue"; break;
            case "PUT": color = "orange"; break;
            case "DELETE": color = "red"; break;
            case "PATCH": color = "yellow"; break;
            default: color = "gray";
        }
        return "<Badge color=\"" + color + "\">" + method + "</Badge>";
    }

    private String generateProjectHeader(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(project.getName() != null ? project.getName() : "API文档").append("\n\n");
        
        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            sb.append(project.getDescription()).append("\n\n");
        }
        
        if (project.getBaseUrl() != null && !project.getBaseUrl().isEmpty()) {
            sb.append("**Base URL**: `").append(project.getBaseUrl()).append("`\n\n");
        }
        
        if (project.getVersion() != null && !project.getVersion().isEmpty()) {
            sb.append("**Version**: ").append(project.getVersion()).append("\n\n");
        }
        
        sb.append("---\n\n");
        return sb.toString();
    }

    private String generateAppendix() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n---\n\n");
        sb.append("*本文档由 API Document Manager 自动生成*\n");
        return sb.toString();
    }
}
