-- =====================================================
-- API Document Manager - 多Schema多租户建表脚本
-- 版本: 1.1.0
-- 说明: 用于多Schema隔离模式，每个租户有独立的数据库Schema
-- 使用场景: SaaS平台、企业多租户部署
-- =====================================================

-- =====================================================
-- 架构说明
-- =====================================================
-- 本脚本用于创建多Schema隔离模式:
-- 1. Master Schema (master): 存储租户元数据
-- 2. Tenant Schema (tenant_N): 每个租户独立的Schema
--
-- 配置示例:
--   master: jdbc:mysql://localhost:3306/api_doc_master
--   tenant_1: jdbc:mysql://localhost:3306/api_doc_tenant_1
--   tenant_2: jdbc:mysql://localhost:3306/api_doc_tenant_2
-- =====================================================

-- =====================================================
-- 第一部分: Master Schema 建表
-- =====================================================
-- 在主数据库执行，创建租户管理表

CREATE DATABASE IF NOT EXISTS api_doc_master
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE api_doc_master;

-- =====================================================
-- 租户表（仅在Master Schema中）
-- =====================================================
DROP TABLE IF EXISTS tenants;
CREATE TABLE tenants (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '租户ID',
    name            VARCHAR(100) NOT NULL COMMENT '租户名称',
    code            VARCHAR(50) NOT NULL UNIQUE COMMENT '租户代码',
    schema_name     VARCHAR(100) NOT NULL COMMENT '数据库Schema名称',
    description     VARCHAR(500) COMMENT '租户描述',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/SUSPENDED',
    max_projects    INT DEFAULT 10 COMMENT '最大项目数',
    max_users       INT DEFAULT 50 COMMENT '最大用户数',
    storage_quota   BIGINT DEFAULT 1073741824 COMMENT '存储配额(字节)',
    db_host         VARCHAR(100) DEFAULT 'localhost' COMMENT '数据库主机',
    db_port         INT DEFAULT 3306 COMMENT '数据库端口',
    db_name         VARCHAR(100) COMMENT '数据库名称',
    settings        JSON COMMENT '租户配置',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    expires_at      DATETIME COMMENT '过期时间',
    INDEX idx_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- =====================================================
-- Master Schema 用户表
-- =====================================================
DROP TABLE IF EXISTS master_users;
CREATE TABLE master_users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username        VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(255) NOT NULL COMMENT '密码(加密存储)',
    email           VARCHAR(100) UNIQUE COMMENT '邮箱',
    full_name       VARCHAR(100) COMMENT '真实姓名',
    avatar_url      VARCHAR(500) COMMENT '头像URL',
    role            VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/SUPER_ADMIN/USER',
    tenant_id       BIGINT COMMENT '关联租户ID(平台管理员为空)',
    enabled         TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_at   DATETIME COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Master用户表';

-- =====================================================
-- 初始化平台管理员
-- =====================================================
INSERT INTO master_users (username, password, email, full_name, role, tenant_id) VALUES
('platform_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'platform@example.com', '平台管理员', 'SUPER_ADMIN', NULL);

-- =====================================================
-- 初始化默认租户
-- =====================================================
INSERT INTO tenants (id, name, code, schema_name, description, status, db_name) VALUES
(1, '默认租户', 'default', 'tenant_1', '系统默认租户', 'ACTIVE', 'api_doc_tenant_1');

-- =====================================================
-- 第二部分: Tenant Schema 建表模板
-- =====================================================
-- 为每个租户创建独立的Schema，替换 {{TENANT_ID}} 为实际的租户ID
-- 以下是 tenant_1 的表结构示例

-- 创建租户1的数据库和表
CREATE DATABASE IF NOT EXISTS api_doc_tenant_1
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE api_doc_tenant_1;

-- =====================================================
-- 租户1用户表
-- =====================================================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username        VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(255) NOT NULL COMMENT '密码(加密存储)',
    email           VARCHAR(100) UNIQUE COMMENT '邮箱',
    full_name       VARCHAR(100) COMMENT '真实姓名',
    avatar_url      VARCHAR(500) COMMENT '头像URL',
    role            VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    enabled         TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_at   DATETIME COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 租户1项目表
-- =====================================================
DROP TABLE IF EXISTS projects;
CREATE TABLE projects (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID',
    name            VARCHAR(100) NOT NULL COMMENT '项目名称',
    description     TEXT COMMENT '项目描述',
    base_url        VARCHAR(500) COMMENT '基础URL',
    version         VARCHAR(50) DEFAULT '1.0.0' COMMENT '版本号',
    owner_id        BIGINT NOT NULL COMMENT '所有者ID',
    visibility      VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT '可见性: PUBLIC/PRIVATE',
    status          VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ARCHIVED',
    tags            JSON COMMENT '标签列表',
    settings        JSON COMMENT '项目配置',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner_id (owner_id),
    INDEX idx_visibility (visibility),
    INDEX idx_status (status),
    FULLTEXT INDEX ft_name_desc (name, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- =====================================================
-- 租户1项目成员表
-- =====================================================
DROP TABLE IF EXISTS project_members;
CREATE TABLE project_members (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成员ID',
    project_id      BIGINT NOT NULL COMMENT '项目ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '项目角色: OWNER/ADMIN/MEMBER/VIEWER',
    permissions     JSON COMMENT '自定义权限',
    invited_by      BIGINT COMMENT '邀请人ID',
    invited_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    accepted_at     DATETIME COMMENT '接受时间',
    UNIQUE KEY uk_project_user (project_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

-- =====================================================
-- 租户1API文档表
-- =====================================================
DROP TABLE IF EXISTS api_documents;
CREATE TABLE api_documents (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文档ID',
    project_id      BIGINT NOT NULL COMMENT '项目ID',
    name            VARCHAR(200) NOT NULL COMMENT '文档名称',
    description     TEXT COMMENT '文档描述',
    version         VARCHAR(50) DEFAULT '1.0.0' COMMENT '版本号',
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/DEPRECATED',
    deprecated      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否弃用',
    tags            JSON COMMENT '标签列表',
    parent_id       BIGINT COMMENT '父文档ID',
    sort_order      INT DEFAULT 0 COMMENT '排序顺序',
    content         TEXT COMMENT '文档内容',
    created_by      BIGINT NOT NULL COMMENT '创建人',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API文档表';

-- =====================================================
-- 租户1接口端点表
-- =====================================================
DROP TABLE IF EXISTS api_endpoints;
CREATE TABLE api_endpoints (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '端点ID',
    document_id     BIGINT NOT NULL COMMENT '文档ID',
    path            VARCHAR(500) NOT NULL COMMENT '接口路径',
    method          VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    summary         VARCHAR(200) COMMENT '接口概要',
    description     TEXT COMMENT '接口详细描述',
    deprecated      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否废弃',
    security        JSON COMMENT '安全要求',
    external_docs   JSON COMMENT '外部文档',
    operation_id    VARCHAR(100) COMMENT '操作ID',
    tags            JSON COMMENT '标签',
    schemes         JSON COMMENT '协议',
    content_types   JSON COMMENT 'Content-Type',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_doc_path_method (document_id, path, method),
    INDEX idx_document_id (document_id),
    INDEX idx_method (method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口端点表';

-- =====================================================
-- 租户1参数定义表
-- =====================================================
DROP TABLE IF EXISTS api_parameters;
CREATE TABLE api_parameters (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '参数ID',
    endpoint_id     BIGINT NOT NULL COMMENT '端点ID',
    location        VARCHAR(20) NOT NULL COMMENT '参数位置',
    name            VARCHAR(100) NOT NULL COMMENT '参数名称',
    description     TEXT COMMENT '参数描述',
    required        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必填',
    data_type       VARCHAR(50) COMMENT '数据类型',
    format          VARCHAR(50) COMMENT '格式',
    default_value   VARCHAR(500) COMMENT '默认值',
    example         TEXT COMMENT '示例值',
    schema_def      JSON COMMENT 'Schema定义',
    enum_values     JSON COMMENT '枚举值',
    validation_rules JSON COMMENT '验证规则',
    min_length      INT COMMENT '最小长度',
    max_length      INT COMMENT '最大长度',
    minimum         DECIMAL(20,5) COMMENT '最小值',
    maximum         DECIMAL(20,5) COMMENT '最大值',
    pattern         VARCHAR(200) COMMENT '正则',
    allow_empty     TINYINT(1) DEFAULT 0 COMMENT '允许空值',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_endpoint_id (endpoint_id),
    INDEX idx_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数定义表';

-- =====================================================
-- 租户1响应定义表
-- =====================================================
DROP TABLE IF EXISTS api_responses;
CREATE TABLE api_responses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '响应ID',
    endpoint_id     BIGINT NOT NULL COMMENT '端点ID',
    status_code     VARCHAR(10) NOT NULL COMMENT '状态码',
    description     VARCHAR(500) COMMENT '描述',
    content_type    VARCHAR(100) DEFAULT 'application/json' COMMENT '内容类型',
    schema_def      JSON COMMENT 'Schema',
    headers         JSON COMMENT '响应头',
    examples        JSON COMMENT '示例',
    is_default      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '默认响应',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_endpoint_id (endpoint_id),
    UNIQUE KEY uk_endpoint_status (endpoint_id, status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='响应定义表';

-- =====================================================
-- 租户1测试用例表
-- =====================================================
DROP TABLE IF EXISTS test_cases;
CREATE TABLE test_cases (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用例ID',
    endpoint_id     BIGINT NOT NULL COMMENT '端点ID',
    name            VARCHAR(200) NOT NULL COMMENT '用例名称',
    description     TEXT COMMENT '用例描述',
    type            VARCHAR(20) NOT NULL DEFAULT 'UNIT' COMMENT '类型',
    priority        VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级',
    request_config  JSON NOT NULL COMMENT '请求配置',
    expected_response JSON COMMENT '期望响应',
    test_data       JSON COMMENT '测试数据',
    precondition    TEXT COMMENT '前置条件',
    postcondition   TEXT COMMENT '后置条件',
    enabled         TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用',
    tags            JSON COMMENT '标签',
    created_by      BIGINT COMMENT '创建人',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_endpoint_id (endpoint_id),
    INDEX idx_type (type),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表';

-- =====================================================
-- 租户1文档历史表
-- =====================================================
DROP TABLE IF EXISTS document_versions;
CREATE TABLE document_versions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版本ID',
    document_id     BIGINT NOT NULL COMMENT '文档ID',
    version         VARCHAR(50) NOT NULL COMMENT '版本号',
    content         JSON NOT NULL COMMENT '内容快照',
    change_log      TEXT COMMENT '变更日志',
    created_by      BIGINT NOT NULL COMMENT '创建人',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_document_version (document_id, version),
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档历史表';

-- =====================================================
-- 初始化租户1管理员用户
-- 密码: admin123
-- =====================================================
INSERT INTO users (username, password, email, full_name, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@tenant1.com', '租户管理员', 'ADMIN');

-- =====================================================
-- 第三部分: 新租户创建存储过程
-- =====================================================
-- 用于动态创建新租户的Schema

DELIMITER //

CREATE PROCEDURE create_tenant_schema(
    IN p_tenant_id BIGINT,
    IN p_schema_name VARCHAR(100)
)
BEGIN
    DECLARE sql_stmt VARCHAR(1000);

    -- 创建租户数据库
    SET sql_stmt = CONCAT('CREATE DATABASE IF NOT EXISTS ', p_schema_name,
                          ' DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
    SET @sql = sql_stmt;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    -- 注意: 实际部署时需要执行完整的建表SQL
    -- 可以通过应用层或脚本工具调用tenant_init.sql
END //

DELIMITER ;

-- =====================================================
-- 第四部分: 多租户配置指南
-- =====================================================
/*
多租户模式配置步骤:

1. 配置 application.yml:

# 单租户模式（默认）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/api_doc_db
    username: root
    password: password

# 多Schema模式
spring:
  datasource:
    master:
      url: jdbc:mysql://localhost:3306/api_doc_master
      username: root
      password: password
    tenant:
      url-template: jdbc:mysql://localhost:3306/api_doc_tenant_{tenantId}
      username: root
      password: password

2. 设置租户上下文:
   - HTTP Header: X-Tenant-ID
   - JWT Token: tenantId claim
   - 子域名: tenant1.api-doc.com

3. 创建新租户API:
   POST /api/admin/tenants
   {
     "name": "新租户",
     "code": "new_tenant",
     "schemaName": "tenant_2"
   }
*/
