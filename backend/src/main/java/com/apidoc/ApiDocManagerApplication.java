package com.apidoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Document Manager - 主应用程序
 * Web服务API接口文档管理工具
 * JPA 审计配置见 com.apidoc.config.JpaAuditingConfig
 */
@SpringBootApplication
public class ApiDocManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiDocManagerApplication.class, args);
    }
}
