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
 * 测试用例实体 - 基于接口定义自动生成
 */
@Entity
@Table(name = "test_cases")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "document", "parameters", "responses"})
    private ApiEndpoint endpoint;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TestType type = TestType.UNIT;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TestPriority priority = TestPriority.MEDIUM;

    @Column(name = "request_config", nullable = false, columnDefinition = "JSON")
    private String requestConfig;

    @Column(name = "expected_response", columnDefinition = "JSON")
    private String expectedResponse;

    @Column(name = "test_data", columnDefinition = "JSON")
    private String testData;

    @Column(columnDefinition = "TEXT")
    private String precondition;

    @Column(columnDefinition = "TEXT")
    private String postcondition;

    @Column(nullable = false)
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum TestType {
        UNIT,          // 单元测试
        INTEGRATION,   // 集成测试
        SMOKE          // 冒烟测试
    }

    public enum TestPriority {
        HIGH, MEDIUM, LOW
    }
}
