# API Document Manager

## 项目简介

API Document Manager 是一款基于前后端分离架构的 Web 服务 API 接口文档管理平台。该平台致力于帮助开发团队高效地撰写规范化、结构化的 API 文档，支持一键生成测试用例，实现文档与开发的紧密联动。无论是后端开发人员、前端工程师还是测试团队，都能从中获得极大的便利。

平台的核心设计理念是「填好接口，生成文档」。程序员只需按照规范的格式填写接口信息，系统便会自动生成符合标准的格式化文档，支持 Markdown 和 LaTeX 两种导出格式，满足不同场景的需求。同时，平台能够根据接口定义自动生成可执行的测试用例，支持 Postman 导入格式和多种编程语言的示例代码，真正实现文档驱动的开发模式。

本项目完全开源，支持 Spring Boot 自动建表和手动 SQL 导入两种数据库初始化方式，方便开发者和团队快速部署和使用。

---

## 核心特性

### 1. 项目与文档管理

平台提供完善的层级管理架构，支持多项目管理和多文档分类。每个项目可包含多个文档，每个文档可定义多个 API 接口端点。支持项目级和文档级的完整 CRUD 操作，包括创建、读取、更新、删除。

### 2. 规范化接口定义

平台提供全面的接口定义功能，支持完整的 HTTP 方法定义（GET、POST、PUT、DELETE、PATCH、OPTIONS、HEAD），以及七种参数位置的精确定义：
- **Header**: 请求头参数
- **Path**: URL 路径参数
- **Query**: 查询字符串参数
- **Request Body**: 请求体参数
- **Response Body**: 响应体参数

每一种参数都支持详细的信息配置，包括参数名称、数据类型、格式规范、默认值、示例值、枚举值以及验证规则，确保接口文档的完整性和准确性。

### 3. 多格式文档导出

文档导出功能是本平台的核心亮点之一。系统支持将接口文档导出为 Markdown 和 LaTeX 两种格式：
- **Markdown 导出**: 兼容 GitHub Flavored Markdown 标准，包含完整的目录结构、代码高亮和表格格式化，适用于日常开发协作和代码仓库文档托管
- **LaTeX 导出**: 兼容标准 LaTeX 语法，支持数学公式渲染和 PDF 直接编译，适合需要高质量排版的正式文档场景
- **项目级导出**: 支持将整个项目的所有文档导出为单个文件，便于项目文档的整体管理

### 4. 自动化测试用例生成

平台能够根据接口定义自动生成测试用例，覆盖各种参数组合和边界情况。生成的测试用例支持多种格式导出：
- **cURL 命令**: 标准命令行工具格式
- **Postman Collection v2.1**: 可直接导入 Postman 使用
- **即时测试**: 直接调用真实 API 接口并返回响应结果

### 5. 全局参数与模板管理

平台提供了强大的参数复用机制：
- **全局参数**: 预定义的参数模板，可在多个接口间复用
- **参数模板**: 支持按文件夹组织参数模板，方便分类管理
- **参数类型**: 支持简单类型和复杂类型参数定义

### 6. 团队协作支持

平台支持多用户协作，提供了完善的用户认证机制（基于 JWT），确保文档的安全访问控制：
- 用户注册与登录
- 项目和文档的访问权限控制
- 支持项目软删除和永久删除
- 支持批量删除操作

---

## 技术栈

### 后端技术

- **框架**: Spring Boot 3.2.0
- **ORM**: Spring Data JPA + Hibernate
- **安全**: Spring Security + JWT Token
- **API 文档**: SpringDoc OpenAPI 3
- **数据库**: MySQL 8.x

### 前端技术

- **框架**: React 18 + TypeScript
- **构建工具**: Vite
- **UI 组件**: Ant Design 5.x
- **状态管理**: Zustand
- **Markdown 渲染**: react-markdown + remark-gfm
- **LaTeX 渲染**: rehype-katex

---

## 项目结构

```
api-doc-manager/
│
├── backend/                              # Spring Boot 后端项目
│   ├── src/main/java/com/apidoc/
│   │   ├── controller/                  # REST API 控制器
│   │   │   ├── AuthController.java     # 认证接口
│   │   │   ├── ProjectController.java   # 项目管理
│   │   │   ├── DocumentController.java # 文档管理
│   │   │   ├── EndpointController.java  # 接口端点管理
│   │   │   ├── ParameterController.java # 参数管理
│   │   │   ├── TestCaseController.java # 测试用例
│   │   │   ├── ExportController.java   # 导出功能
│   │   │   ├── GlobalParameterController.java    # 全局参数
│   │   │   └── ParameterTemplateController.java  # 参数模板
│   │   ├── service/                   # 业务逻辑层
│   │   ├── repository/                # 数据访问层
│   │   ├── entity/                   # 实体类
│   │   ├── dto/                      # 数据传输对象
│   │   ├── config/                   # 配置类
│   │   ├── export/                   # 导出服务
│   │   │   ├── MarkdownExporter.java # Markdown 导出
│   │   │   └── LatexExporter.java    # LaTeX 导出
│   │   └── security/                 # 安全配置
│   └── src/main/resources/
│       ├── application.yml            # 应用配置
│       ├── schema.sql                # 数据库建表脚本
│       └── data.sql                  # 初始数据（可选）
│
├── api-doc-frontend/                   # React 前端项目
│   ├── src/
│   │   ├── pages/                   # 页面组件
│   │   │   ├── LoginPage.tsx        # 登录页面
│   │   │   ├── EndpointEditor.tsx    # 接口编辑器
│   │   │   ├── TestCaseGenerator.tsx  # 测试用例生成器
│   │   │   ├── ExportPanel.tsx       # 导出面板
│   │   │   ├── GlobalParameterPanel.tsx    # 全局参数面板
│   │   │   └── ParameterTemplatePanel.tsx  # 参数模板面板
│   │   ├── services/                 # API 服务封装
│   │   │   └── api.ts               # API 接口定义
│   │   ├── stores/                  # Zustand 状态管理
│   │   │   ├── appStore.ts          # 应用状态
│   │   │   └── authStore.ts        # 认证状态
│   │   ├── components/               # 公共组件
│   │   └── App.tsx                  # 应用入口
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
└── README.md                         # 项目说明文档
```

---

## 功能说明

### 项目管理

#### 核心功能
- ✅ 创建项目（名称、描述、Base URL、版本）
- ✅ 获取项目列表（可访问的项目）
- ✅ 获取当前用户创建的项目
- ✅ 获取单个项目详情
- ✅ 更新项目信息
- ✅ 删除项目（软删除 - 设置 deletedAt）
- ✅ 永久删除项目（物理删除）
- ✅ 批量软删除项目
- ✅ 批量永久删除项目

#### API 端点
```
GET    /api/projects              # 获取可访问的项目列表
GET    /api/projects/my           # 获取当前用户的项目
GET    /api/projects/{id}         # 获取单个项目
POST   /api/projects              # 创建项目
PUT    /api/projects/{id}         # 更新项目
DELETE /api/projects/{id}         # 软删除项目
DELETE /api/projects/{id}/permanent  # 永久删除项目
POST   /api/projects/batch-delete # 批量软删除
DELETE /api/projects/batch-delete/permanent # 批量永久删除
```

### 文档管理

#### 核心功能
- ✅ 创建文档（名称、描述、版本、标签）
- ✅ 获取文档列表（按项目）
- ✅ 获取单个文档详情
- ✅ 更新文档信息
- ✅ 删除文档（软删除）
- ✅ 永久删除文档
- ✅ 批量删除文档
- ✅ 批量永久删除文档

#### API 端点
```
GET    /api/documents/project/{projectId}  # 获取项目的文档列表
GET    /api/documents/{id}                # 获取单个文档
POST   /api/documents                    # 创建文档
PUT    /api/documents/{id}                # 更新文档
DELETE /api/documents/{id}                # 软删除文档
DELETE /api/documents/{id}/permanent      # 永久删除文档
POST   /api/documents/batch-delete       # 批量软删除
DELETE /api/documents/batch-delete/permanent # 批量永久删除
```

### 接口端点管理

#### 核心功能
- ✅ 创建接口端点（路径、方法、摘要、描述、安全配置）
- ✅ 获取接口列表（按文档）
- ✅ 获取单个接口详情
- ✅ 更新接口信息
- ✅ 删除接口
- ✅ 即时测试接口（直接调用真实 API）

#### HTTP 方法支持
- GET、POST、PUT、DELETE、PATCH、OPTIONS、HEAD

#### API 端点
```
GET    /api/endpoints/document/{documentId}  # 获取文档的接口列表
GET    /api/endpoints/{id}                  # 获取单个接口
POST   /api/endpoints                      # 创建接口
PUT    /api/endpoints/{id}                  # 更新接口
DELETE /api/endpoints/{id}                  # 删除接口
POST   /api/endpoints/{id}/test              # 即时测试接口
```

### 参数管理

#### 核心功能
- ✅ 创建参数（名称、类型、位置、必填、示例、验证规则）
- ✅ 获取参数列表（按接口）
- ✅ 更新参数信息
- ✅ 删除参数

#### 参数位置类型
- Header（请求头参数）
- Path（路径参数）
- Query（查询参数）
- Request Body（请求体参数）
- Response Body（响应体参数）

#### 参数属性
- 参数名称、数据类型
- 格式规范、默认值
- 示例值、枚举值
- 长度限制（minLength、maxLength）
- 数值范围（minimum、maximum）
- 正则表达式验证

#### API 端点
```
GET    /api/parameters/endpoint/{endpointId}  # 获取接口的参数列表
POST   /api/parameters                      # 创建参数
PUT    /api/parameters/{id}                # 更新参数
DELETE /api/parameters/{id}                # 删除参数
```

### 全局参数管理

#### 核心功能
- ✅ 创建全局参数（可复用的参数模板）
- ✅ 获取所有全局参数
- ✅ 获取顶级参数
- ✅ 获取简单类型参数
- ✅ 获取复杂类型参数
- ✅ 搜索参数
- ✅ 更新参数
- ✅ 删除参数

#### API 端点
```
GET    /api/global-parameters              # 获取所有全局参数
GET    /api/global-parameters/root       # 获取顶级参数
GET    /api/global-parameters/simple-types # 获取简单类型参数
GET    /api/global-parameters/complex-types # 获取复杂类型参数
GET    /api/global-parameters/search?q={keyword} # 搜索参数
GET    /api/global-parameters/{id}       # 获取单个参数
POST   /api/global-parameters           # 创建参数
PUT    /api/global-parameters/{id}       # 更新参数
DELETE /api/global-parameters/{id}       # 删除参数
```

### 参数模板管理

#### 核心功能
- ✅ 创建参数模板
- ✅ 从接口创建模板
- ✅ 按文件夹组织模板
- ✅ 获取文件夹列表
- ✅ 删除模板
- ✅ 按文件夹删除模板

#### API 端点
```
GET    /api/parameter-templates?documentId={id}     # 获取文档的模板
GET    /api/parameter-templates/folders?documentId={id} # 获取文件夹列表
GET    /api/parameter-templates/folder/{name}?documentId={id} # 获取文件夹中的模板
POST   /api/parameter-templates                   # 创建模板
POST   /api/parameter-templates/from-endpoint/{endpointId} # 从接口创建
DELETE /api/parameter-templates/{id}               # 删除模板
DELETE /api/parameter-templates/folder/{name}?documentId={id} # 按文件夹删除
```

### 测试用例生成

#### 核心功能
- ✅ 获取测试用例列表（按接口）
- ✅ 自动生成测试用例
- ✅ 删除测试用例（按接口）
- ✅ 导出为 cURL 命令
- ✅ 导出为 Postman Collection v2.1

#### 测试用例类型
- SMOKE（冒烟测试）
- UNIT（单元测试）
- INTEGRATION（集成测试）

#### API 端点
```
GET    /api/testcases/endpoint/{endpointId}    # 获取测试用例
POST   /api/testcases/generate/{endpointId}   # 生成测试用例
DELETE /api/testcases/endpoint/{endpointId}    # 删除测试用例
GET    /api/testcases/export/curl/{endpointId}    # 导出为 cURL
GET    /api/testcases/export/postman/{endpointId}  # 导出为 Postman
```

### 文档导出

#### 核心功能
- ✅ 导出单个文档为 Markdown
- ✅ 导出单个文档为 LaTeX
- ✅ 导出整个项目为 Markdown
- ✅ 导出整个项目为 LaTeX

#### 导出格式特点
- **Markdown**: 包含表格、代码高亮、目录结构
- **LaTeX**: 支持中文排版、数学公式、PDF 编译

#### API 端点
```
GET /api/export/markdown/{documentId}            # 导出文档为 Markdown
GET /api/export/latex/{documentId}              # 导出文档为 LaTeX
GET /api/export/project/{projectId}/markdown     # 导出项目为 Markdown
GET /api/export/project/{projectId}/latex       # 导出项目为 LaTeX
```

### 用户认证

#### 核心功能
- ✅ 用户注册（用户名、密码、邮箱、姓名）
- ✅ 用户登录（返回 JWT 令牌）

#### API 端点
```
POST /api/auth/register   # 用户注册
POST /api/auth/login      # 用户登录
```

---

## 快速开始

### 环境要求

- Java 17 或更高版本
- Node.js 18 或更高版本
- pnpm 包管理器
- MySQL 8.0 或更高版本

### 后端部署

#### 1. 数据库配置

修改 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/api_doc_manager?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

#### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8081`

### 前端部署

#### 1. 安装依赖

```bash
cd api-doc-frontend
pnpm install
```

#### 2. 启动开发服务器

```bash
pnpm dev
```

前端应用将运行在 `http://localhost:5173`

#### 3. 构建生产版本

```bash
pnpm build
```

---

## 数据库设计

### 主要数据表

- **users**: 用户表（用户ID、用户名、密码、邮箱、角色、创建时间）
- **projects**: 项目表（项目ID、名称、描述、Base URL、版本、所有者、可见性、删除时间）
- **api_documents**: 文档表（文档ID、所属项目、名称、描述、版本、状态、标签、删除时间）
- **api_endpoints**: 端点表（端点ID、所属文档、路径、方法、摘要、描述、安全配置）
- **api_parameters**: 参数表（参数ID、所属端点、位置、名称、类型、必填、示例、验证规则）
- **api_responses**: 响应表（响应ID、所属端点、状态码、描述、响应结构）
- **test_cases**: 测试用例表（用例ID、所属端点、名称、类型、测试数据）
- **global_parameters**: 全局参数表（参数ID、名称、类型、描述、值）
- **parameter_templates**: 参数模板表（模板ID、所属文档、文件夹、名称、参数定义）

---

## API 接口参考

详细的 API 接口说明和交互式测试请访问 Swagger UI：

```
http://localhost:8081/swagger-ui.html
```

---

## 配置说明

### JWT 配置

JWT 密钥配置位于 `application.yml`。生产环境中请务必修改默认密钥，使用足够长度和复杂度的随机字符串。

### CORS 配置

如果前端和后端部署在不同域名，需要正确配置跨域资源共享（CORS）。配置位于安全配置类中。

---

## 开源贡献

我们欢迎社区成员贡献代码和改进建议。请通过 GitHub Issues 提交反馈。

---

## 许可证

本项目采用 MIT 许可证开源，您可以自由使用、修改和分发本项目的代码。
