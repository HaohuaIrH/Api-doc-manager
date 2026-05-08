package com.apidoc.dto;

import com.apidoc.entity.TestCase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试用例DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseDTO {

    private Long id;
    private Long endpointId;
    private String endpointPath;
    private String endpointMethod;
    private String name;
    private String description;
    private TestCase.TestType type;
    private TestCase.TestPriority priority;
    private String requestConfig;
    private String expectedResponse;
    private String testData;
    private String precondition;
    private String postcondition;
    private Boolean enabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 生成的代码
    private String curlCommand;
    private String javaCode;
    private String javascriptCode;
    private String pythonCode;
    private String goCode;

    public static TestCaseDTO fromEntity(TestCase entity) {
        return TestCaseDTO.builder()
                .id(entity.getId())
                .endpointId(entity.getEndpoint().getId())
                .endpointPath(entity.getEndpoint().getPath())
                .endpointMethod(entity.getEndpoint().getMethod().name())
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .priority(entity.getPriority())
                .requestConfig(entity.getRequestConfig())
                .expectedResponse(entity.getExpectedResponse())
                .testData(entity.getTestData())
                .precondition(entity.getPrecondition())
                .postcondition(entity.getPostcondition())
                .enabled(entity.getEnabled())
                .createdBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private Long endpointId;
        private String name;
        private String description;
        private TestCase.TestType type;
        private TestCase.TestPriority priority;
        private String requestConfig;
        private String expectedResponse;
        private String testData;
        private String precondition;
        private String postcondition;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerateRequest {
        private Long endpointId;
        private TestCase.TestType type;
        private TestCase.TestPriority priority;
        private String testData;
    }
}
