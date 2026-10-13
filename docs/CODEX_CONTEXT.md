# CRM Codex Project Context

## 1. 文档定位

本文档提供 `crm-system` 的项目背景、当前能力和文档导航，供跨模块任务或上下文不足时快速加载。它不是接口大全、字段字典、开发规范、任务日志或历史验收报告。

当前事实以生产代码和测试为准。开发执行规则见根目录 `AGENTS.md`；前后端编码细则见 `docs/development/`。

## 2. 项目概览

- 企业内部 B 端 CRM，采用 Spring Boot REST API 与 Vue SPA 的前后端分离架构。
- 后端 context path 为 `/api`，Controller 内部路径不得重复包含 `/api`。
- 系统管理 V1 已具备认证、用户、角色、权限及前端权限控制。
- 客户管理 V1 已具备客户列表、新建客户、数据范围控制和客户统计核心服务。
- 联系人、跟进、线索、商机、合同、数据分析等仍以数据库基础或前端占位为主，不能据此假定已有完整业务 API。
- AI Agent V1 已完成 Spring AI 与通义千问兼容基础设施，以及客户统计核心能力；尚未完成 Tool Calling 编排、正式 AI 对话 API 和 AI 分析前端闭环。

## 3. 当前技术基线

### 后端

- Java 17、Maven、Spring Boot 3.5.16。
- Spring Web、Validation、Security、无状态 JWT 认证。
- MyBatis-Plus 3.5.7、MySQL 8、HikariCP。
- JJWT 0.12.6、Lombok、SLF4J。
- Spring AI 1.1.8，通过 OpenAI 兼容协议接入通义千问。
- JUnit 5、Mockito、AssertJ、MockMvc、Spring Security Test。

### 前端

- Vue 3、Composition API、`<script setup lang="ts">`。
- Vite 5、TypeScript、Vue Router 4、Pinia 2。
- Axios 统一请求实例、Element Plus 与图标库。
- API 默认基地址 `/api`，开发环境由 Vite 代理到后端。

### 数据库

- MySQL 8、`utf8mb4`。
- 主键主要为 BIGINT，并由 MyBatis-Plus `ASSIGN_ID` 生成。
- `database/init.sql` 维护基础 schema；开发/权限数据 SQL 位于 `backend/src/main/resources/db/`。
- 当前未引入 Flyway 或 Liquibase，SQL 脚本默认手工执行。

## 4. 代码导航

```text
crm-system/
├── backend/
│   └── src/main/java/com/company/crm/
│       ├── controller/       HTTP 边界与方法级授权
│       ├── service/impl/     业务、事务、对象级授权
│       ├── mapper/           MyBatis-Plus Mapper
│       ├── entity/           数据库实体
│       ├── dto/              请求与内部传输模型
│       ├── vo/               接口响应模型
│       ├── security/         JWT、UserDetails、401/403、数据范围
│       ├── config/           Security、MyBatis、初始化、时间配置
│       └── exception/        模块异常与 ProblemDetail 处理
├── frontend/src/
│   ├── api/                  按资源组织的 API
│   ├── components/           业务子组件
│   ├── layouts/              后台布局
│   ├── router/               路由与认证/权限守卫
│   ├── stores/               Pinia 状态
│   ├── types/                业务类型
│   ├── utils/                Axios、Token、权限工具
│   └── views/                页面容器
├── database/init.sql
├── docs/
└── task/                     历史任务输入，不是运行时源代码
```

## 5. 认证与当前用户

```text
POST /api/auth/login
  → AuthenticationManager
  → CustomUserDetailsService
  → BCrypt PasswordEncoder
  → JwtService
```

- `JwtAuthenticationFilter` 从 Bearer Token 恢复 `SecurityUser` 和 `SecurityContext`。
- 登录接口公开，其余接口默认要求认证；方法级授权由 `@EnableMethodSecurity` 开启。
- `GET /api/auth/me` 返回当前用户的 id、username、realName、roles、permissions，不暴露安全内部字段。
- 前端 Token 操作集中在 `src/utils/auth.ts`，Axios 自动附加 Bearer Token；401 会清理本地认证状态并返回登录页。
- JWT secret、数据库凭据、AI API Key 等必须来自环境变量或未跟踪本地配置。

## 6. RBAC 与数据范围

```text
sys_user → sys_user_role → sys_role
                           ↓
                    sys_role_permission → sys_permission
```

- 有效角色生成 `ROLE_<role_code>`，有效权限生成 `resource:action` authority。
- `SUPER_ADMIN` 不会隐式拥有全部权限，运行时授权依赖真实角色权限关系。
- Controller 使用 `@PreAuthorize` 做功能权限；Service 对目标对象、角色边界和数据范围做第二层强制校验。
- 当前客户查询使用 `ALL`、`DEPT`、`SELF` 数据范围；解析失败应拒绝访问，不能降级为更宽范围。
- 前端菜单、路由和按钮权限仅改善交互，不能替代后端安全边界。
- 根管理员、角色创建/编辑/配权和用户对象范围的详细规则，应以对应 Service、测试及任务相关模块文档为准。

## 7. 已实现模块摘要

### 系统管理

- 用户：列表、创建、编辑、管理员重置密码、角色绑定。
- 角色：列表、创建、编辑、权限回显与覆盖保存。
- 权限：树、平铺列表、权限总览。
- 前端：认证 Store、路由守卫、侧边栏及用户/角色页面按钮权限控制。

### 客户管理 V1

- `GET /api/customers`：服务端分页、查询条件、真实角色/部门数据范围。
- `POST /api/customers`：创建客户、业务角色归属规则、唯一编号与事务处理。
- 客户统计核心服务：复用客户可见性 SQL，支持时间范围统计和等级聚合。
- 详细业务规则见 `docs/customer-management/CUSTOMER_V1_RULES.md`，测试数据说明见同目录文档。

### AI Agent V1

- Spring AI 1.1.8 与通义千问的 OpenAI 兼容配置已建立，生产默认不主动启用模型。
- 已有隔离的 live smoke test，需要显式环境条件才运行。
- 客户统计服务是后续 Tool Calling 的候选业务能力，但当前并不表示已注册为 AI Tool。
- AI 需求、数据与技术边界见 `docs/ai-agent/`；历史接入验收见 `docs/historical/`。

## 8. 关键工程约束

- Controller 保持薄层；DTO/VO 与 Entity 分离；Service 承担业务与事务；Mapper/SQL 承担持久化。
- SQL 必须显式处理逻辑删除、稳定排序和数据范围；避免 N+1 与前端绕过后端范围过滤。
- BIGINT ID 对前端存在精度风险，对外响应按现有 VO 约定使用字符串或已验证的安全类型。
- 业务异常当前按 Auth/User/Role/Customer 等模块输出 RFC 7807 `ProblemDetail`，尚未统一成单一全局处理器。
- 日志记录稳定标识和失败类型，不记录密码、JWT、Authorization、API Key、完整请求对象或敏感对象 `toString()`。
- 管理员初始化默认关闭，只有显式开关与外部密码满足约束时才创建账号；不会覆盖已有管理员凭据。

## 9. 测试现状

- Controller：`@WebMvcTest` + MockMvc + Security Test，覆盖 401/403、校验、JSON 合同和授权表达式。
- Service/Security：Mockito 单元测试，覆盖角色边界、数据范围、事务前置校验和 JWT 行为。
- Mapper：对关键动态 SQL 有结构验证；真实数据库联调仍需独立环境。
- 前端当前以 TypeScript 检查和 Vite 构建为主要自动验证，尚无完整组件测试套件。
- 测试范围和执行策略遵循 `AGENTS.md`，不能把历史测试结果当作当前运行结果。

## 10. 文档导航

- 全局工作规则：`AGENTS.md`
- 后端开发规范：`docs/development/BACKEND_CONVENTIONS.md`
- 前端开发规范：`docs/development/FRONTEND_CONVENTIONS.md`
- 客户 V1 规则：`docs/customer-management/CUSTOMER_V1_RULES.md`
- AI Agent 需求/数据/技术：`docs/ai-agent/`
- 架构与数据库概览：`docs/architecture.md`、`docs/database.md`
- 阶段性审查、升级和验收记录：`docs/historical/`

## 11. 已知限制

- 当前没有 Refresh Token、Token 黑名单或密码变更后的即时 Token 失效机制。
- 权限与客户数据初始化仍依赖手工 SQL，没有数据库迁移框架。
- 部分数据库表和前端菜单是后续模块占位，不代表对应 API 已实现。
- AI 对话 API、Tool Calling 编排、动态工具授权和 AI 分析前端尚未完成。
- 文档可能落后；开始具体任务时仍需检查直接相关源码和测试。
