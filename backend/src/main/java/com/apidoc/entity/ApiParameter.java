package com.apidoc.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API参数实体 - 形式化参数定义
 */
@Entity
@Table(name = "api_parameters")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "document", "parameters", "responses"})
    private ApiEndpoint endpoint;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ParameterLocation location;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean required = false;

    @Column(name = "data_type", length = 50)
    private String dataType;

    @Column(length = 50)
    private String format;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(columnDefinition = "TEXT")
    private String example;

    @Column(name = "schema_def", columnDefinition = "JSON")
    private String schemaDef;

    @Column(name = "enum_values", columnDefinition = "JSON")
    private String enumValues;

    @Column(name = "validation_rules", columnDefinition = "JSON")
    private String validationRules;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(precision = 20, scale = 5)
    private BigDecimal minimum;

    @Column(precision = 20, scale = 5)
    private BigDecimal maximum;

    @Column(length = 200)
    private String pattern;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ParameterLocation {
        HEADER,      // 请求头参数
        PATH,        // 路径参数
        QUERY,       // 查询参数
        REQUEST_BODY, // 请求体
        RESPONSE_BODY // 响应体
    }
}
