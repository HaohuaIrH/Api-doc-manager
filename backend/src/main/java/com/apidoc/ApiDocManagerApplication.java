package com.apidoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * API Document Manager - 主应用程序
 * Web服务API接口文档管理工具
 */
@SpringBootApplication
@EnableJpaAuditing
public class ApiDocManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiDocManagerApplication.class, args);
    }
}
