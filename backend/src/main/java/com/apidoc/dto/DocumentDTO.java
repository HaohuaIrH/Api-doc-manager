package com.apidoc.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档 DTO - 避免懒加载问题
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private String version;
    private String status;
    private String tags;
    private Long parentId;
    private Long createdBy;
    private Integer sortOrder;
    private LocalDateTime deletedAt;

    @SuppressWarnings("unused")
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unused")
    public void setTagsList(List<String> tagsList) {
        if (tagsList == null || tagsList.isEmpty()) {
            this.tags = null;
        } else {
            try {
                this.tags = objectMapper.writeValueAsString(tagsList);
            } catch (JsonProcessingException e) {
                this.tags = null;
            }
        }
    }

    public List<String> getTagsList() {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(tags, List.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
