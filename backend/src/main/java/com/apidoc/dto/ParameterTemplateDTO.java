package com.apidoc.dto;

import com.apidoc.entity.ParameterTemplate;
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
public class ParameterTemplateDTO {

    private Long id;
    private String folderName;
    private String templateName;
    private List<Map<String, Object>> parameters;
    private Long documentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ParameterTemplateDTO fromEntity(ParameterTemplate entity, List<Map<String, Object>> params) {
        return ParameterTemplateDTO.builder()
                .id(entity.getId())
                .folderName(entity.getFolderName())
                .templateName(entity.getTemplateName())
                .parameters(params)
                .documentId(entity.getDocumentId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ParameterTemplate toEntity() {
        return ParameterTemplate.builder()
                .id(this.id)
                .folderName(this.folderName)
                .templateName(this.templateName)
                .documentId(this.documentId)
                .tenantId(1L)
                .build();
    }
}
