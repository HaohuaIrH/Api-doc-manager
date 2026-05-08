package com.apidoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目 DTO - 避免懒加载问题
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private Long id;
    private String name;
    private String description;
    private String baseUrl;
    private String version;
    private Long ownerId;
    private String ownerUsername;
    private String visibility;
    private String tags;
    private LocalDateTime deletedAt;
}
