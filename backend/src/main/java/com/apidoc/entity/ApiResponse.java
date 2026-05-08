package com.apidoc.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * API响应实体 - 形式化响应定义
 */
@Entity
@Table(name = "api_responses")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "document", "parameters", "responses"})
    private ApiEndpoint endpoint;

    @Column(name = "status_code", nullable = false, length = 10)
    private String statusCode;

    @Column(length = 500)
    private String description;

    @Column(name = "content_type", length = 100)
    private String contentType = "application/json";

    @Column(name = "schema_def", columnDefinition = "JSON")
    private String schemaDef;

    @Column(columnDefinition = "JSON")
    private String headers;

    @Column(columnDefinition = "JSON")
    private String examples;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
