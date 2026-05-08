# API 接口文档

## 1. 概述

本文档描述了 API 文档管理系统的所有接口。系统提供项目管理、文档管理、端点管理和参数管理等功能。

### 1.1 基础信息

- **接口前缀**：`/api`
- **数据格式**：JSON
- **字符编码**：UTF-8
- **跨域支持**：已启用（CORS）

### 1.2 认证方式

除登录和注册接口外，其他接口需要在请求头中携带 JWT 令牌。

```
Authorization: Bearer <token>
```

### 1.3 通用响应格式

成功响应：

```json
{
  "id": 1,
  "name": "示例名称",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

错误响应：

```json
{
  "error": "错误描述信息"
}
```

---

## 2. 认证接口

### 2.1 用户登录

**接口地址**：`POST /api/auth/login`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | 字符串 | 是 | 用户名 |
| password | 字符串 | 是 | 密码 |

**请求示例**：

```json
{
  "username": "admin",
  "password": "password123"
}
```

**响应参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| token | 字符串 | JWT 令牌 |
| username | 字符串 | 用户名 |

**响应示例**：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin"
}
```

---

### 2.2 用户注册

**接口地址**：`POST /api/auth/register`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | 字符串 | 是 | 用户名 |
| password | 字符串 | 是 | 密码 |
| email | 字符串 | 否 | 邮箱 |
| fullName | 字符串 | 否 | 姓名 |

**请求示例**：

```json
{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "fullName": "张三"
}
```

**响应参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| token | 字符串 | JWT 令牌 |
| username | 字符串 | 用户名 |

---

## 3. 项目管理接口

### 3.1 获取所有项目

**接口地址**：`GET /api/projects`

**请求参数**：无

**响应参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 项目ID |
| name | 字符串 | 项目名称 |
| description | 字符串 | 项目描述 |
| baseUrl | 字符串 | 基础URL |
| version | 字符串 | 版本号 |

**响应示例**：

```json
[
  {
    "id": 1,
    "name": "用户服务",
    "description": "用户相关接口",
    "baseUrl": "http://localhost:8081",
    "version": "1.0.0"
  }
]
```

---

### 3.2 获取单个项目

**接口地址**：`GET /api/projects/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 项目ID |

**响应参数**：同 3.1

---

### 3.3 创建项目

**接口地址**：`POST /api/projects`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | 字符串 | 是 | 项目名称 |
| description | 字符串 | 否 | 项目描述 |
| baseUrl | 字符串 | 否 | 基础URL |
| version | 字符串 | 否 | 版本号 |

**请求示例**：

```json
{
  "name": "订单服务",
  "description": "订单相关接口",
  "baseUrl": "http://api.example.com",
  "version": "2.0.0"
}
```

---

### 3.4 更新项目

**接口地址**：`PUT /api/projects/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 项目ID |

**请求参数**：同 3.3

---

### 3.5 删除项目

**接口地址**：`DELETE /api/projects/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 项目ID |

**响应状态**：200 OK

---

## 4. 文档管理接口

### 4.1 获取项目下的文档

**接口地址**：`GET /api/documents/project/{projectId}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| projectId | 整数 | 项目ID |

**响应参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 文档ID |
| projectId | 整数 | 所属项目ID |
| name | 字符串 | 文档名称 |
| description | 字符串 | 文档描述 |
| version | 字符串 | 版本号 |
| status | 字符串 | 状态（DRAFT/PUBLISHED/DEPRECATED） |
| tags | 字符串 | 标签 |
| parentId | 整数 | 父文档ID |
| sortOrder | 整数 | 排序顺序 |

**响应示例**：

```json
[
  {
    "id": 1,
    "projectId": 1,
    "name": "用户模块文档",
    "description": "包含用户注册、登录等接口",
    "version": "1.0.0",
    "status": "DRAFT",
    "tags": "用户,认证",
    "parentId": null,
    "sortOrder": 0
  }
]
```

---

### 4.2 获取单个文档

**接口地址**：`GET /api/documents/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 文档ID |

**响应参数**：同 4.1

---

### 4.3 创建文档

**接口地址**：`POST /api/documents`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | 整数 | 是 | 所属项目ID |
| name | 字符串 | 是 | 文档名称 |
| description | 字符串 | 否 | 文档描述 |
| version | 字符串 | 否 | 版本号 |
| status | 字符串 | 否 | 状态 |
| tags | 字符串 | 否 | 标签 |
| parentId | 整数 | 否 | 父文档ID |
| sortOrder | 整数 | 否 | 排序顺序 |

**请求示例**：

```json
{
  "projectId": 1,
  "name": "商品模块文档",
  "description": "商品查询、购买接口",
  "version": "1.0.0",
  "status": "DRAFT",
  "tags": "商品,购物",
  "sortOrder": 1
}
```

**错误响应**：

```json
{
  "error": "Project ID is required"
}
```

---

### 4.4 更新文档

**接口地址**：`PUT /api/documents/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 文档ID |

**请求参数**：同 4.3

---

### 4.5 删除文档

**接口地址**：`DELETE /api/documents/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 文档ID |

**响应状态**：200 OK

---

## 5. 端点管理接口

### 5.1 获取文档下的端点

**接口地址**：`GET /api/endpoints/document/{documentId}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| documentId | 整数 | 文档ID |

**响应参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 端点ID |
| documentId | 整数 | 所属文档ID |
| method | 字符串 | HTTP方法 |
| path | 字符串 | 接口路径 |
| summary | 字符串 | 概要 |
| description | 字符串 | 详细描述 |
| deprecated | 布尔值 | 是否废弃 |
| security | 字符串 | 安全机制 |
| tags | 字符串 | 标签 |

**响应示例**：

```json
[
  {
    "id": 1,
    "documentId": 1,
    "method": "GET",
    "path": "/api/users/{id}",
    "summary": "获取用户信息",
    "description": "根据用户ID获取用户详细信息",
    "deprecated": false,
    "security": "Bearer",
    "tags": "用户"
  }
]
```

---

### 5.2 获取单个端点

**接口地址**：`GET /api/endpoints/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 端点ID |

**响应参数**：同 5.1

---

### 5.3 创建端点

**接口地址**：`POST /api/endpoints`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| documentId | 整数 | 是 | 所属文档ID |
| method | 字符串 | 是 | HTTP方法（GET/POST/PUT/DELETE/PATCH） |
| path | 字符串 | 是 | 接口路径 |
| summary | 字符串 | 否 | 概要 |
| description | 字符串 | 否 | 详细描述 |
| deprecated | 布尔值 | 否 | 是否废弃 |
| security | 字符串 | 否 | 安全机制 |
| tags | 字符串 | 否 | 标签 |

**请求示例**：

```json
{
  "documentId": 1,
  "method": "POST",
  "path": "/api/users",
  "summary": "创建用户",
  "description": "创建新用户账号",
  "deprecated": false,
  "security": "Bearer",
  "tags": "用户"
}
```

---

### 5.4 更新端点

**接口地址**：`PUT /api/endpoints/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 端点ID |

**请求参数**：同 5.3

---

### 5.5 删除端点

**接口地址**：`DELETE /api/endpoints/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 端点ID |

**响应状态**：200 OK

---

## 6. 参数管理接口

### 6.1 获取端点下的参数

**接口地址**：`GET /api/parameters/endpoint/{endpointId}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| endpointId | 整数 | 端点ID |

**响应参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 参数ID |
| endpointId | 整数 | 所属端点ID |
| location | 字符串 | 参数位置（PATH/QUERY/BODY/HEADER） |
| name | 字符串 | 参数名称 |
| description | 字符串 | 参数描述 |
| required | 布尔值 | 是否必填 |
| dataType | 字符串 | 数据类型 |
| format | 字符串 | 格式 |
| defaultValue | 字符串 | 默认值 |
| example | 字符串 | 示例值 |
| schemaDef | 字符串 | Schema定义 |
| enumValues | 字符串 | 枚举值 |
| validationRules | 字符串 | 校验规则 |

**响应示例**：

```json
[
  {
    "id": 1,
    "endpointId": 1,
    "location": "PATH",
    "name": "id",
    "description": "用户ID",
    "required": true,
    "dataType": "integer",
    "format": "int64",
    "defaultValue": null,
    "example": "12345",
    "schemaDef": null,
    "enumValues": null,
    "validationRules": null
  }
]
```

---

### 6.2 创建参数

**接口地址**：`POST /api/parameters`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| endpointId | 整数 | 是 | 所属端点ID |
| location | 字符串 | 是 | 参数位置 |
| name | 字符串 | 是 | 参数名称 |
| description | 字符串 | 否 | 参数描述 |
| required | 布尔值 | 否 | 是否必填 |
| dataType | 字符串 | 否 | 数据类型 |
| format | 字符串 | 否 | 格式 |
| defaultValue | 字符串 | 否 | 默认值 |
| example | 字符串 | 否 | 示例值 |
| schemaDef | 字符串 | 否 | Schema定义 |
| enumValues | 字符串 | 否 | 枚举值 |
| validationRules | 字符串 | 否 | 校验规则 |

**请求示例**：

```json
{
  "endpointId": 1,
  "location": "QUERY",
  "name": "page",
  "description": "页码",
  "required": false,
  "dataType": "integer",
  "format": "int32",
  "defaultValue": "1",
  "example": "1"
}
```

---

### 6.3 更新参数

**接口地址**：`PUT /api/parameters/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 参数ID |

**请求参数**：同 6.2

---

### 6.4 删除参数

**接口地址**：`DELETE /api/parameters/{id}`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | 整数 | 参数ID |

**响应状态**：200 OK

---

## 7. 错误码说明

| HTTP状态码 | 说明 |
|------------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或令牌无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 8. 数据类型说明

### 8.1 HTTP方法

| 方法 | 说明 |
|------|------|
| GET | 查询数据 |
| POST | 创建数据 |
| PUT | 更新数据 |
| DELETE | 删除数据 |
| PATCH | 部分更新 |

### 8.2 参数位置

| 位置 | 说明 |
|------|------|
| PATH | 路径参数 |
| QUERY | 查询参数 |
| BODY | 请求体参数 |
| HEADER | 请求头参数 |

### 8.3 文档状态

| 状态 | 说明 |
|------|------|
| DRAFT | 草稿 |
| PUBLISHED | 已发布 |
| DEPRECATED | 已废弃 |

---

## 9. 使用示例

### 9.1 登录并获取令牌

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

### 9.2 创建项目

```bash
curl -X POST http://localhost:8081/api/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"name":"订单服务","description":"订单相关接口"}'
```

### 9.3 获取端点列表

```bash
curl -X GET http://localhost:8081/api/endpoints/document/1 \
  -H "Authorization: Bearer <token>"
```
