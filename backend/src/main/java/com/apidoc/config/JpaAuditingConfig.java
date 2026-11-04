package com.apidoc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 审计配置
 * 独立于主应用类，避免切片测试（如 @WebMvcTest）加载 JPA 审计组件
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
