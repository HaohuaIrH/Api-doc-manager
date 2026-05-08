package com.apidoc.dto;

import com.apidoc.entity.ApiEndpoint;
import com.apidoc.entity.ApiParameter;
import com.apidoc.entity.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API端点完整DTO - 包含所有参数和响应定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiEndpointDTO {

    private Long id;
    private Long documentId;
    private String path;
    private ApiEndpoint.HttpMethod method;
    private String summary;
    private String description;
    private Boolean deprecated;
    private String operationId;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 完整参数列表
    private List<ParameterDTO> parameters;

    // 响应列表
    private List<ResponseDTO> responses;

    // 用于创建/更新的请求
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private Long documentId;
        private String path;
        private ApiEndpoint.HttpMethod method;
        private String summary;
        private String description;
        private Boolean deprecated;
        private String operationId;
        private String tags;
        private List<ParameterDTO> parameters;
        private List<ResponseDTO> responses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String path;
        private ApiEndpoint.HttpMethod method;
        private String summary;
        private String description;
        private Boolean deprecated;
        private String operationId;
        private String tags;
        private List<ParameterDTO> parameters;
        private List<ResponseDTO> responses;
    }
}
