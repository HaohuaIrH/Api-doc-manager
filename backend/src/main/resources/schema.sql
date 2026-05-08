-- =====================================================
-- API Document Manager - Complete Database Schema
-- Version: 1.2.0
--
-- Description: This file contains all database tables for the API Document Manager.
--              It is automatically executed on application startup.
--
-- Usage:
--   1. Spring Boot auto-execution (default): Just start the app
--   2. Manual execution: mysql -u root -p api_doc_db < schema.sql
--   3. Import via Navicat/DBeaver
--
-- Tables included:
--   - tenants, users, projects, api_documents
--   - api_endpoints, api_parameters, api_responses
--   - test_cases, parameter_templates, global_parameters
--   - document_versions, export_records, operation_logs
-- =====================================================

-- =====================================================
-- 1. 租户表（核心多租户表）
-- =====================================================
DROP TABLE IF EXISTS tenants;
CREATE TABLE tenants (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '租户ID',
    name            VARCHAR(100) NOT NULL COMMENT '租户名称',
    code            VARCHAR(50) NOT NULL UNIQUE COMMENT '租户代码',
    schema_name     VARCHAR(100) COMMENT '数据库Schema名称(多Schema模式使用)',
    description     VARCHAR(500) COMMENT '租户描述',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/SUSPENDED',
    max_projects    INT DEFAULT 10 COMMENT '最大项目数',
    max_users       INT DEFAULT 50 COMMENT '最大用户数',
    storage_quota   BIGINT DEFAULT 1073741824 COMMENT '存储配额(字节)',
    settings        JSON COMMENT '租户配置',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    expires_at      DATETIME COMMENT '过期时间',
    INDEX idx_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- =====================================================
-- 2. 用户表（支持多租户）
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
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    enabled         TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_at   DATETIME COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 3. 项目表（支持多租户）
-- =====================================================
DROP TABLE IF EXISTS projects;
CREATE TABLE projects (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID',
    name            VARCHAR(100) NOT NULL COMMENT '项目名称',
    description     TEXT COMMENT '项目描述',
    base_url        VARCHAR(500) COMMENT '基础URL',
    version         VARCHAR(50) DEFAULT '1.0.0' COMMENT '版本号',
    owner_id        BIGINT NOT NULL COMMENT '所有者ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    visibility      VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT '可见性: PUBLIC/PRIVATE',
    status          VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ARCHIVED',
    tags            JSON COMMENT '标签列表',
    settings        JSON COMMENT '项目配置',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at      DATETIME COMMENT '软删除时间',
    INDEX idx_owner_id (owner_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_visibility (visibility),
    INDEX idx_status (status),
    INDEX idx_deleted_at (deleted_at),
    FULLTEXT INDEX ft_name_desc (name, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- =====================================================
-- 4. 项目成员表
-- =====================================================
DROP TABLE IF EXISTS project_members;
CREATE TABLE project_members (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成员ID',
    project_id      BIGINT NOT NULL COMMENT '项目ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '项目角色: OWNER/ADMIN/MEMBER/VIEWER',
    permissions     JSON COMMENT '自定义权限',
    invited_by      BIGINT COMMENT '邀请人ID',
    invited_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    accepted_at     DATETIME COMMENT '接受时间',
    UNIQUE KEY uk_project_user (project_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

-- =====================================================
-- 5. 文档表（支持多租户）
-- =====================================================
DROP TABLE IF EXISTS api_documents;
CREATE TABLE api_documents (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文档ID',
    project_id      BIGINT NOT NULL COMMENT '项目ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name            VARCHAR(200) NOT NULL COMMENT '文档名称',
    description     TEXT COMMENT '文档描述',
    version         VARCHAR(50) DEFAULT '1.0.0' COMMENT '版本号',
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/DEPRECATED',
    deprecated      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否弃用',
    tags            JSON COMMENT '标签列表',
    parent_id       BIGINT COMMENT '父文档ID(用于分类)',
    sort_order      INT DEFAULT 0 COMMENT '排序顺序',
    content         TEXT COMMENT '文档内容(Markdown格式)',
    created_by      BIGINT NOT NULL COMMENT '创建人',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at      DATETIME COMMENT '软删除时间',
    INDEX idx_project_id (project_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_status (status),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_by (created_by),
    INDEX idx_deleted_at (deleted_at),
    FULLTEXT INDEX ft_name_desc (name, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API文档表';

-- =====================================================
-- 6. 接口端点表（支持多租户）
-- =====================================================
DROP TABLE IF EXISTS api_endpoints;
CREATE TABLE api_endpoints (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '端点ID',
    document_id     BIGINT NOT NULL COMMENT '文档ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    path            VARCHAR(500) NOT NULL COMMENT '接口路径',
    method          VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    summary         VARCHAR(200) COMMENT '接口概要',
    description     TEXT COMMENT '接口详细描述',
    deprecated      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否废弃',
    security        JSON COMMENT '安全要求列表',
    external_docs   JSON COMMENT '外部文档链接',
    operation_id    VARCHAR(100) COMMENT '操作ID',
    tags            JSON COMMENT '标签列表',
    schemes         JSON COMMENT '协议类型(HTTP/HTTPS)',
    content_types   JSON COMMENT '支持的Content-Type',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at      DATETIME COMMENT '软删除时间',
    UNIQUE KEY uk_document_path_method (document_id, path, method),
    INDEX idx_document_id (document_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_method (method),
    INDEX idx_deprecated (deprecated),
    INDEX idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口端点表';

-- =====================================================
-- 7. 参数定义表（支持多租户）
-- =====================================================
DROP TABLE IF EXISTS api_parameters;
CREATE TABLE api_parameters (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '参数ID',
    endpoint_id     BIGINT NOT NULL COMMENT '端点ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    location        VARCHAR(20) NOT NULL COMMENT '参数位置: HEADER/PATH/QUERY/REQUEST_BODY/RESPONSE_BODY',
    name            VARCHAR(100) NOT NULL COMMENT '参数名称',
    description     TEXT COMMENT '参数描述',
    required        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必填',
    data_type       VARCHAR(50) COMMENT '数据类型',
    format          VARCHAR(50) COMMENT '格式',
    default_value   VARCHAR(500) COMMENT '默认值',
    example         TEXT COMMENT '示例值',
    schema_def      JSON COMMENT 'JSON Schema定义',
    enum_values     JSON COMMENT '枚举值列表',
    validation_rules JSON COMMENT '校验规则',
    min_length      INT COMMENT '最小长度',
    max_length      INT COMMENT '最大长度',
    minimum         DECIMAL(20,5) COMMENT '最小值',
    maximum         DECIMAL(20,5) COMMENT '最大值',
    pattern         VARCHAR(200) COMMENT '正则表达式',
    allow_empty     TINYINT(1) DEFAULT 0 COMMENT '是否允许空值',
    sort_order      INT DEFAULT 0 COMMENT '排序顺序',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_endpoint_id (endpoint_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_location (location),
    INDEX idx_required (required)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数定义表';

-- =====================================================
-- 8. 响应定义表（支持多租户）
-- =====================================================
DROP TABLE IF EXISTS api_responses;
CREATE TABLE api_responses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '响应ID',
    endpoint_id     BIGINT NOT NULL COMMENT '端点ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    status_code     VARCHAR(10) NOT NULL COMMENT '状态码',
    description     VARCHAR(500) COMMENT '状态码描述',
    content_type    VARCHAR(100) DEFAULT 'application/json' COMMENT '内容类型',
    schema_def      JSON COMMENT '响应体Schema',
    headers         JSON COMMENT '响应头定义',
    examples        JSON COMMENT '响应示例',
    is_default      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为默认响应',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_endpoint_id (endpoint_id),
    INDEX idx_tenant_id (tenant_id),
    UNIQUE KEY uk_endpoint_status (endpoint_id, status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='响应定义表';

-- =====================================================
-- 9. 测试用例表（支持多租户）
-- =====================================================
DROP TABLE IF EXISTS test_cases;
CREATE TABLE test_cases (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '测试用例ID',
    endpoint_id     BIGINT NOT NULL COMMENT '端点ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name            VARCHAR(200) NOT NULL COMMENT '用例名称',
    description     TEXT COMMENT '用例描述',
    type            VARCHAR(20) NOT NULL DEFAULT 'UNIT' COMMENT '类型: UNIT/INTEGRATION/SMOKE',
    priority        VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级: HIGH/MEDIUM/LOW',
    request_config  JSON NOT NULL COMMENT '请求配置',
    expected_response JSON COMMENT '期望响应',
    test_data       JSON COMMENT '测试数据',
    precondition    TEXT COMMENT '前置条件',
    postcondition   TEXT COMMENT '后置条件',
    steps           TEXT COMMENT '执行步骤',
    assertions      TEXT COMMENT '断言',
    enabled         TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    tags            JSON COMMENT '标签',
    created_by      BIGINT COMMENT '创建人',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_run_at     DATETIME COMMENT '最后执行时间',
    last_run_result VARCHAR(20) COMMENT '最后执行结果',
    INDEX idx_endpoint_id (endpoint_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_type (type),
    INDEX idx_priority (priority),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表';

-- =====================================================
-- 10. 文档历史版本表（支持多租户）
-- =====================================================
DROP TABLE IF EXISTS document_versions;
CREATE TABLE document_versions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版本ID',
    document_id     BIGINT NOT NULL COMMENT '文档ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    version         VARCHAR(50) NOT NULL COMMENT '版本号',
    content         JSON NOT NULL COMMENT '版本快照',
    change_log      TEXT COMMENT '变更日志',
    created_by      BIGINT NOT NULL COMMENT '创建人',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_document_version (document_id, version),
    INDEX idx_document_id (document_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档历史版本表';

-- =====================================================
-- 11. 导出记录表
-- =====================================================
DROP TABLE IF EXISTS export_records;
CREATE TABLE export_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    project_id      BIGINT COMMENT '项目ID',
    document_id     BIGINT COMMENT '文档ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    export_type     VARCHAR(20) NOT NULL COMMENT '导出类型: MARKDOWN/LATEX/POSTMAN/OPENAPI',
    format          VARCHAR(20) NOT NULL COMMENT '格式',
    file_path       VARCHAR(500) COMMENT '文件路径',
    file_size       BIGINT COMMENT '文件大小',
    status          VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    error_message   TEXT COMMENT '错误信息',
    requested_by    BIGINT COMMENT '请求者ID',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at    DATETIME COMMENT '完成时间',
    INDEX idx_project_id (project_id),
    INDEX idx_document_id (document_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_export_type (export_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出记录表';

-- =====================================================
-- 12. 操作日志表
-- =====================================================
DROP TABLE IF EXISTS operation_logs;
CREATE TABLE operation_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id         BIGINT COMMENT '操作用户ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    operation_type  VARCHAR(50) NOT NULL COMMENT '操作类型',
    resource_type   VARCHAR(50) COMMENT '资源类型',
    resource_id     BIGINT COMMENT '资源ID',
    action          VARCHAR(100) NOT NULL COMMENT '操作动作',
    ip_address      VARCHAR(50) COMMENT 'IP地址',
    user_agent      VARCHAR(500) COMMENT 'User-Agent',
    request_method  VARCHAR(10) COMMENT '请求方法',
    request_url     VARCHAR(500) COMMENT '请求URL',
    request_params  TEXT COMMENT '请求参数',
    response_status INT COMMENT '响应状态',
    execution_time  BIGINT COMMENT '执行时间(毫秒)',
    error_message   TEXT COMMENT '错误信息',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =====================================================
-- 外键约束
-- =====================================================
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE projects ADD CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE projects ADD CONSTRAINT fk_projects_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE project_members ADD CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;
ALTER TABLE project_members ADD CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE project_members ADD CONSTRAINT fk_pm_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_documents ADD CONSTRAINT fk_docs_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;
ALTER TABLE api_documents ADD CONSTRAINT fk_docs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_documents ADD CONSTRAINT fk_docs_parent FOREIGN KEY (parent_id) REFERENCES api_documents(id) ON DELETE SET NULL;
ALTER TABLE api_documents ADD CONSTRAINT fk_docs_creator FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE api_endpoints ADD CONSTRAINT fk_endpoints_document FOREIGN KEY (document_id) REFERENCES api_documents(id) ON DELETE CASCADE;
ALTER TABLE api_endpoints ADD CONSTRAINT fk_endpoints_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_parameters ADD CONSTRAINT fk_params_endpoint FOREIGN KEY (endpoint_id) REFERENCES api_endpoints(id) ON DELETE CASCADE;
ALTER TABLE api_parameters ADD CONSTRAINT fk_params_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_responses ADD CONSTRAINT fk_responses_endpoint FOREIGN KEY (endpoint_id) REFERENCES api_endpoints(id) ON DELETE CASCADE;
ALTER TABLE api_responses ADD CONSTRAINT fk_responses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE test_cases ADD CONSTRAINT fk_tests_endpoint FOREIGN KEY (endpoint_id) REFERENCES api_endpoints(id) ON DELETE CASCADE;
ALTER TABLE test_cases ADD CONSTRAINT fk_tests_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE document_versions ADD CONSTRAINT fk_versions_document FOREIGN KEY (document_id) REFERENCES api_documents(id) ON DELETE CASCADE;
ALTER TABLE document_versions ADD CONSTRAINT fk_versions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE export_records ADD CONSTRAINT fk_export_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE operation_logs ADD CONSTRAINT fk_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- =====================================================
-- 13. 参数模板表
-- =====================================================
DROP TABLE IF EXISTS parameter_templates;
CREATE TABLE parameter_templates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
    folder_name     VARCHAR(200) NOT NULL COMMENT '文件夹名称',
    template_name   VARCHAR(200) COMMENT '模板名称',
    parameters      JSON COMMENT '参数JSON',
    document_id     BIGINT NOT NULL COMMENT '所属文档ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_folder_name (folder_name),
    INDEX idx_document_id (document_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数模板表';

ALTER TABLE parameter_templates ADD CONSTRAINT fk_templates_document FOREIGN KEY (document_id) REFERENCES api_documents(id) ON DELETE CASCADE;
ALTER TABLE parameter_templates ADD CONSTRAINT fk_templates_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- =====================================================
-- 初始化默认租户
-- =====================================================
INSERT INTO tenants (id, name, code, description, status) VALUES (1, '默认租户', 'default', '系统默认租户，用于单租户模式', 'ACTIVE');

-- =====================================================
-- 初始化管理员用户 (密码: admin123)
-- 注意: 生产环境请修改密码
-- =====================================================
INSERT INTO users (username, password, email, full_name, role, tenant_id) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@example.com', '系统管理员', 'ADMIN', 1);

-- =====================================================
-- 13. 全局参数表（参数变量库）
-- =====================================================
DROP TABLE IF EXISTS global_parameters;
CREATE TABLE global_parameters (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '参数ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name            VARCHAR(100) NOT NULL UNIQUE COMMENT '参数名称',
    data_type       VARCHAR(50) NOT NULL COMMENT '数据类型: STRING/INTEGER/LONG/DOUBLE/BOOLEAN/ARRAY/OBJECT',
    example_value   TEXT COMMENT '示例值',
    description     TEXT COMMENT '参数描述',
    parent_id       BIGINT COMMENT '父参数ID（用于复杂类型）',
    sort_order      INT DEFAULT 0 COMMENT '排序顺序',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_name (name),
    INDEX idx_data_type (data_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全局参数表';

-- =====================================================
-- 外键约束
-- =====================================================
ALTER TABLE global_parameters ADD CONSTRAINT fk_global_params_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE global_parameters ADD CONSTRAINT fk_global_params_parent FOREIGN KEY (parent_id) REFERENCES global_parameters(id) ON DELETE CASCADE;
