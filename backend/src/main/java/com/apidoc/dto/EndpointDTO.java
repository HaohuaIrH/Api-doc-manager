package com.apidoc.dto;

import com.apidoc.entity.ApiEndpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointDTO {
    private Long id;
    private Long documentId;
    private String path;
    private String method;
    private String summary;
    private String description;
    private Boolean deprecated;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<Map<String, Object>> parameters;
    private List<Map<String, Object>> responses;

    public static EndpointDTO fromEntity(ApiEndpoint entity) {
        return EndpointDTO.builder()
                .id(entity.getId())
                .documentId(entity.getDocument() != null ? entity.getDocument().getId() : null)
                .path(entity.getPath())
                .method(entity.getMethod() != null ? entity.getMethod().name() : null)
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .deprecated(entity.getDeprecated())
                .tags(entity.getTags())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
