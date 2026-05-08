package com.apidoc.dto;

import com.apidoc.entity.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 响应定义DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDTO {

    private Long id;
    private Long endpointId;
    private String statusCode;
    private String description;
    private String contentType;
    private String schemaDef;
    private String headers;
    private String examples;
    private Boolean isDefault;

    public static ResponseDTO fromEntity(ApiResponse entity) {
        return ResponseDTO.builder()
                .id(entity.getId())
                .endpointId(entity.getEndpoint().getId())
                .statusCode(entity.getStatusCode())
                .description(entity.getDescription())
                .contentType(entity.getContentType())
                .schemaDef(entity.getSchemaDef())
                .headers(entity.getHeaders())
                .examples(entity.getExamples())
                .isDefault(entity.getIsDefault())
                .build();
    }

    public ApiResponse toEntity() {
        return ApiResponse.builder()
                .statusCode(this.statusCode)
                .description(this.description)
                .contentType(this.contentType)
                .schemaDef(this.schemaDef)
                .headers(this.headers)
                .examples(this.examples)
                .isDefault(this.isDefault != null ? this.isDefault : false)  // ← 添加这一行
                .build();
    }
}
