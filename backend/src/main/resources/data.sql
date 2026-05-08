-- =====================================================
-- API Document Manager - 初始基础数据
-- 版本: 1.0.0
-- 说明: 仅插入基础用户数据，项目和文档需要用户手动创建
-- =====================================================

-- 插入基础用户 (密码都是 admin123)
INSERT INTO users (username, password, email, full_name, role, enabled) VALUES
('admin', '$2a$10$8K1p/a0dL1.xQo7Z9HXm3eQ4Qv5V.H.YP7OGVi0FJN0R0p1H0KZK', 'admin@example.com', '系统管理员', 'ADMIN', true),
('developer', '$2a$10$8K1p/a0dL1.xQo7Z9HXm3eQ4Qv5V.H.YP7OGVi0FJN0R0p1H0KZK', 'dev@example.com', '开发者', 'USER', true)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 注意：示例项目和文档已删除，需要用户手动创建
-- 项目、文档、接口等数据请通过 API 或前端界面创建
