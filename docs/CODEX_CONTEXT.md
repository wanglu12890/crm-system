# CRM Codex Development Context

## 1. Purpose

本文档是 `crm-system` 的项目级长期开发上下文，用于让新的 Codex 会话快速掌握当前系统的稳定事实、权限边界和开发约束。它不是 README、接口大全、字段字典或变更日志。

事实可信度按以下顺序判断：

1. 当前工作树中的生产代码；
2. 当前自动化测试；
3. 数据库结构与 SQL；
4. 应用配置和前端实现；
5. README、既有 docs 和代码注释。

本文档可能滞后。开始任何任务时，先读本文档，再检查任务相关的实际代码；两者冲突时以代码和测试为准。不得把无法从仓库确认的设计意图写成永久规则。

## 2. Project Overview

- 项目名称：`crm-system`，企业内部 B 端 CRM。
- 采用前后端分离架构：Spring Boot REST API + Vue SPA。
- System Management V1（认证、用户、角色、权限）已完成当前计划范围。
- Customer Management V1 已完成受 JWT 与 `customer:list` 保护的客户列表后端查询；联系人、跟进仍没有业务 Controller/Service/API 实现。
- 线索、商机、合同、数据分析和 AI 分析目前仅有数据库表或前端占位入口，没有业务 Controller/Service/API 实现。
- 后端 context path 为 `/api`；Controller 路径不得再次包含 `/api`。

## 3. Technology Stack

### Backend

- Java 17、Maven、Spring Boot 3.3.x。
- Spring Web、Spring Validation、Spring Security 6 风格配置。
- MyBatis-Plus 3.5.x，Mapper 当前主要使用注解 SQL 和 BaseMapper。
- MySQL 8，HikariCP。
- JJWT 0.12.x，HMAC JWT access token。
- Lombok、SLF4J。
- Jakarta Validation 用于请求 DTO 校验。

### Frontend

- Vue 3、Composition API、`<script setup lang="ts">`。
- Vite 5、TypeScript、Vue Router 4、Pinia 2。
- Axios 统一请求实例、Element Plus 及其图标库。
- 当前没有前端单元测试或 lint script。

### Database

- MySQL 8，字符集 `utf8mb4`。
- 主键主要为 MyBatis-Plus `ASSIGN_ID` 生成的 BIGINT。
- `database/init.sql` 负责建库建表；开发数据和权限数据位于 `backend/src/main/resources/db/` 的手工 SQL。
- 当前没有 Flyway 或 Liquibase。

### Testing

- JUnit 5、Mockito、AssertJ。
- Controller 使用 `@WebMvcTest` + MockMvc + Spring Security Test。
- Service 和安全组件主要使用 Mockito 单元测试。
- JWT 过滤器使用测试 Controller 验证完整过滤链。

## 4. Repository Structure

```text
crm-system/
├── backend/                     Spring Boot 应用与后端测试
│   └── src/main/java/com/company/crm/
│       ├── controller/          HTTP 接口与方法级授权
│       ├── service/impl/        业务、事务、对象级授权
│       ├── mapper/              MyBatis-Plus 与注解 SQL
│       ├── entity/              数据库实体
│       ├── dto/                 请求模型与校验
│       ├── vo/                  对外响应模型
│       ├── security/            JWT、UserDetails、401/403
│       ├── config/              Security、MP、初始化配置
│       └── exception/           模块异常与 ProblemDetail
├── frontend/                    Vue SPA
│   └── src/
│       ├── api/                 按资源组织的 API
│       ├── components/system/   用户/角色子组件
│       ├── layouts/             Header、Sidebar、Layout
│       ├── router/              静态路由与认证/权限守卫
│       ├── stores/              Pinia auth store
│       ├── types/               业务 TypeScript 类型
│       ├── utils/               Axios 与 token 工具
│       └── views/               页面入口
├── database/init.sql            MySQL schema
├── docs/                        架构与长期上下文
└── task/                        历史任务要求，不是运行时源代码
```

## 5. Backend Architecture

- Controller 负责路径、HTTP 状态、DTO 校验、响应 VO 和 `@PreAuthorize`；复杂业务规则不放在 Controller。
- Service 负责事务、业务校验、当前操作者解析及对象级授权；写操作通常在此完整校验后再落库。
- Mapper 负责实体 CRUD 和面向页面的联表/聚合 SQL；当前没有实际 XML Mapper。
- Entity 对应表结构，不直接作为列表接口响应。
- DTO 是输入边界并承载 Jakarta Validation；VO 隔离数据库模型与接口结构。
- Security 层把数据库用户转换为 `SecurityUser`，构建角色与权限 authorities。
- 异常响应使用 RFC 7807 `ProblemDetail`，但当前仍按 Auth/User/Role 分模块处理，并非完全统一的全局业务异常体系。
- 依赖注入普遍采用 Lombok `@RequiredArgsConstructor` 的构造注入。

## 6. Authentication

### Login Flow

```text
Login.vue
  → POST /api/auth/login
  → AuthenticationManager
  → DaoAuthenticationProvider
  → CustomUserDetailsService
  → PasswordEncoder.matches
  → JwtService.generateAccessToken
```

- 登录接口是唯一显式 `permitAll` 的业务接口。
- `AuthServiceImpl` 使用 Spring Security 标准认证流程，不手写密码比对。
- 登录成功后更新 `last_login_at`；更新失败则认证流程失败。
- 密码摘要使用 Spring 容器中的 BCrypt encoder。
- 前端将 `accessToken` 和服务端返回的 `expiresIn` 保存到 localStorage。

### JWT Request Authentication

- `JwtAuthenticationFilter` 是 `OncePerRequestFilter`，位于 `UsernamePasswordAuthenticationFilter` 之前。
- 请求头格式为 `Authorization: Bearer <token>`。
- JWT 当前包含 subject、`userId`、`username`、签发时间和过期时间。
- 签名密钥按 UTF-8 普通字符串转为 HMAC key，不按 Base64 解码；密钥由应用配置提供，不得写入本文档。
- Filter 从 token 解析 username 后，每次请求调用 `CustomUserDetailsService` 重新查询用户、启用角色和启用权限。
- 验证成功后以 `SecurityUser` 为 principal 写入 `SecurityContextHolder`。
- Token 非法、过期或签名错误由认证入口返回 401，不继续进入 Controller。
- Security 为 stateless；禁用 form login、HTTP Basic、默认 logout、request cache 和 CSRF。

### Current User `/auth/me`

- `GET /api/auth/me` 依赖 Filter 已恢复的 `SecurityContext`，不重复解析 JWT。
- `@AuthenticationPrincipal SecurityUser` 映射为 `CurrentUserVO`。
- 返回 `id`、`username`、`realName`、`roles`、`permissions`；不返回密码摘要、status、deleted 或原始 authority 对象。

### Password Encoding

- 用户创建、管理员重置密码均复用已注入的 BCrypt `PasswordEncoder`。
- 禁止在 Service 内自行创建 encoder。
- 明文密码只作为请求输入使用，不应进入日志、Pinia、localStorage 或 URL。

## 7. Authorization and RBAC

### RBAC Model

```text
sys_user
  → sys_user_role
  → sys_role
  → sys_role_permission
  → sys_permission
```

- 启用且未删除的角色贡献 `ROLE_<role_code>` authority。
- 启用权限贡献原始 `permission_code` authority，例如 `user:list`。
- `SUPER_ADMIN` 不会被代码隐式补齐所有权限；它仍需存在真实的 `sys_role_permission` 关系。
- 认证 authority 查询会过滤停用/逻辑删除角色和停用权限。
- 方法级授权由 `@EnableMethodSecurity` 开启，使用 `hasRole`、`hasAuthority` 及组合表达式。
- 方法权限是第一层，涉及目标用户或目标角色的边界还必须在 Service 强制执行。

### Current Key Permissions

权限码采用 `resource:action`：

- 用户：`user:list`、`user:create`、`user:update`、`user:delete`、`user:assign_role`、`user:reset_password`。
- 角色：`role:list`、`role:create`、`role:update`、`role:delete`、`role:assign_permission`。
- 权限管理：`permission:list`。
- Customer V1：`customer:list/create/update`、`customer_pool:list/claim`、`contact:list/create/update/delete`、`follow:list/create`。
- 菜单节点使用 `system:user`、`system:role`、`system:permission` 等编码，但菜单编码不等同于接口操作权限。
- 部分权限已存在于 SQL 数据但还没有对应业务接口，例如 delete 权限。

## 8. Role Boundaries

以下是当前代码行为，不代表未经确认的永久组织政策。

| 操作 | SUPER_ADMIN | SYSTEM_ADMIN | SALES_MANAGER / SALES_STAFF |
|---|---|---|---|
| 查看用户 | 需 `user:list` | 需 `user:list` | 业务规则不应授予 |
| 创建用户 | 需 create + assign_role | 可创建并只能分配非系统级角色 | 当前 Service 防御不完整，见 Known Issues |
| 编辑用户 | 可编辑；根用户另有保护 | 仅可编辑完整角色集中无系统级角色的用户 | Service 拒绝 |
| 重置密码 | 可重置非 SUPER_ADMIN 的其他用户 | 仅可重置非系统级其他用户 | Service 拒绝 |
| 创建角色 | 需角色身份 + `role:create`；不能创建 SUPER_ADMIN | 拒绝 | 拒绝 |
| 编辑角色 | 需角色身份 + `role:update` | 拒绝 | 拒绝 |
| 配置角色权限 | 需角色身份 + `role:assign_permission` | 拒绝 | 拒绝 |
| 权限管理页面/API | 需角色身份 + `permission:list` | 即使误授权限仍拒绝 | 拒绝 |

Current enforcement note:
GET /api/users currently relies on `user:list` method authority and does not
perform an additional administrator-role check in Service. Therefore, if a
business role is accidentally granted `user:list`, the current backend may
allow access. Treat this as current enforcement behavior, not intended role policy.

- 目标用户授权判断使用 `selectAllRoleCodesByUserId` 获取完整角色集合，而不是只检查第一角色。
- `SYSTEM_ADMIN` 的系统级目标定义为含 `SUPER_ADMIN` 或 `SYSTEM_ADMIN` 的用户。
- 当前代码明确将 `SUPER_ADMIN` 和 `SYSTEM_ADMIN` 视为系统级角色； `SALES_MANAGER` 和 `SALES_STAFF` 属于当前业务角色。
- 新增角色时，不得仅根据“不是 SUPER_ADMIN”就自动推断其安全级别； 新角色的系统级/业务级属性应在对应任务中明确确认。

## 9. Root Administrator Protection

- DataInitializer 确保存在一个初始化管理员用户、`SUPER_ADMIN` 角色及其关联。
- 当前根用户识别依赖“初始化用户名 + 完整角色集中含 SUPER_ADMIN”，数据库没有独立 root/system 标识。
- 根用户可以更新个人资料，但不能通过用户编辑接口改变角色集合或状态。
- 任何带 `SUPER_ADMIN` 角色的用户都不能通过管理员重置密码接口被重置，包括根用户。
- `SUPER_ADMIN` 角色本身：角色编码没有编辑入口；名称和状态不可修改，备注可以修改。
- `SUPER_ADMIN` 的权限配置不能通过角色权限 API 修改。
- 不要把数据库生成 ID 当作根身份规则。

## 10. User Management

### Implemented APIs

| API | 权限 | 说明 |
|---|---|---|
| `GET /api/users` | `user:list` | 返回未逻辑删除用户及完整角色集合 |
| `POST /api/users` | `user:create` + `user:assign_role` | 创建用户并绑定一个或多个角色 |
| `PUT /api/users/{id}` | `user:update` + `user:assign_role` | 修改资料、状态和角色 |
| `POST /api/users/{id}/reset-password` | `user:reset_password` | 管理员设置目标用户新密码 |

- 用户名由数据库唯一约束兜底；逻辑删除记录仍占用唯一用户名。
- 创建时角色必须存在、启用且未删除。
- 编辑时可以保留目标用户已经拥有的停用角色，但不能新分配停用角色。
- 用户列表将角色 ID/编码聚合返回，供前端显示和对象级按钮控制。
- 当前没有用户删除接口，尽管 SQL 中存在 `user:delete` 权限。

### Password Reset

- 请求只包含 `newPassword`；确认密码仅是前端交互字段。
- 密码规则与创建用户一致：非空，6～64 字符；当前没有字母/数字复杂度规则。
- 禁止 self reset，提示使用尚未实现的“修改自己的密码”流程。
- 目标完整角色集中只要含 `SUPER_ADMIN` 即拒绝。
- `SUPER_ADMIN` 可重置 SYSTEM_ADMIN 和普通业务用户。
- `SYSTEM_ADMIN` 只能重置不含 SUPER_ADMIN/SYSTEM_ADMIN 的普通业务用户。
- 普通业务角色即使误获权限也在 Service 被拒绝。
- 停用但未删除的普通用户允许重置。
- 专用 Mapper SQL 仅更新 `password_hash`、`updated_by`、`updated_at`，不触碰资料和角色关系。
- 当前 V1 不会使目标用户已经签发的 JWT 立即失效。

## 11. Role Management

### Implemented APIs

| API | 权限/角色条件 | 说明 |
|---|---|---|
| `GET /api/roles` | `role:list` | 角色列表及关联权限数量 |
| `POST /api/roles` | SUPER_ADMIN + `role:create` | 创建非 SUPER_ADMIN 角色 |
| `PUT /api/roles/{id}` | SUPER_ADMIN + `role:update` | 编辑名称、状态、备注 |
| `GET /api/roles/{id}/permissions` | SUPER_ADMIN + `role:assign_permission` | 回显启用权限 ID |
| `PUT /api/roles/{id}/permissions` | SUPER_ADMIN + `role:assign_permission` | 覆盖保存角色权限 |

- `roleCode` 创建时转大写并由唯一约束保护；更新 DTO 不允许改 roleCode。
- 新角色 `dataScope` 当前固定为 `SELF`；数据范围尚未进入其他业务查询链路。
- 权限保存先去重并验证所有权限存在且启用，再整体替换关系；空数组表示清空。
- 对相同权限集合的保存会直接返回，避免无意义写入。
- 停用角色不会在登录/请求认证时贡献角色或权限，但 `sys_user_role` 和 `sys_role_permission` 关系不会因停用自动删除。
- 角色删除接口尚未实现。

## 12. Permission Management

权限管理入口的三个 API 均要求：

```text
ROLE_SUPER_ADMIN AND permission:list
```

因此 SYSTEM_ADMIN 即使被误授 `permission:list` 也无法访问。

### Permission Tree

- `GET /api/permissions`。
- 只查询 `status=1` 的权限，按 `parent_id`、`sort_order`、`id` 排序后组装树。
- `parentId=0` 是根节点；找不到父节点的孤儿会被忽略。
- `children` 初始化为空列表，不返回 null。
- 用于角色权限配置，不应与平铺权限列表混用。

### Permission List

- `GET /api/permissions/list`。
- 支持 `keyword`（名称/编码）、`moduleId`、`status` 筛选；当前无分页。
- 同时展示启用和停用权限。
- 模块是沿父链向上找到的“最近 MENU 祖先”，且父链必须最终到达根节点；不是通过 permission code 前缀推断。
- 孤儿或循环节点的模块为 null，但记录仍可出现在列表中。

### Permission Overview

- `GET /api/permissions/overview`。
- 返回 summary、未删除角色、全部权限及 `rolePermissions` 配置矩阵。
- 勾选只表示真实存储的 `sys_role_permission` 关系，不代表某用户此刻的有效运行时授权。
- 停用角色和停用权限的关系仍可显示；运行时认证会过滤它们。
- `SUPER_ADMIN` 不会在总览中自动补齐未存储的权限关系。
- 当前实现一次性加载全部角色、权限和关系，适合当前规模，尚无分页或虚拟化。

## 13. Permission Development Convention

新增受保护能力时必须同步评估：

1. 选择符合 `resource:action` 的稳定 permission code。
2. 在 `sys_permission` 中挂到真实 MENU 父节点，不按编码前缀猜 parent。
3. 明确该权限是否需要默认角色分配；如需要，则建立真实 `sys_role_permission`；如不需要，则不得擅自给角色自动授权。
4. Controller 增加恰当的 `@PreAuthorize`。
5. 涉及目标对象时在 Service 再做对象级授权。
6. 前端路由、菜单或按钮通过 auth store 控制可见性。
7. 补 Controller 的 401/403/校验测试和 Service 的边界角色测试。
8. 确认新权限自然进入权限树、权限列表和权限总览。
9. 当前无迁移框架；提供幂等手工 SQL，并明确执行前置条件，禁止自动清库或重建权限数据。

前端隐藏只改善 UX，后端始终是安全边界。

## 14. Frontend Architecture

### Auth State and Token

- `src/utils/auth.ts` 统一管理 token；业务组件不要直接操作 localStorage。
- Axios `baseURL` 默认 `/api`，API 文件只写 `/users`、`/roles` 等 context-path 内部路径。
- 请求拦截器自动添加 Bearer token。
- 非登录请求返回 401 时清除本地 token 并 replace 到 `/login`；没有 refresh-token 或自动重试。
- Pinia `auth` store 保存 `currentUser`、初始化状态，并提供 `hasRole`、`hasPermission`。
- `loadCurrentUser` 复用同一个进行中的 Promise，避免 Router/Header 并发请求 `/auth/me`。
- 刷新页面后 Router guard 会在进入受保护页面前重新加载当前用户。

### Router, Sidebar and Visibility

- `/admin` 及其子路由需要认证。
- 用户、角色、权限路由分别检查 `user:list`、`role:list`、`SUPER_ADMIN + permission:list`。
- 无 token 跳转登录；身份加载失败清 token；权限不足跳转 `/403`。
- Sidebar 对系统管理菜单进行相同的角色/权限过滤。
- 客户列表路由和菜单受 `customer:list` 控制并使用真实列表页面；其余客户、销售、分析入口仍为 Placeholder，尚未完成权限过滤。
- User/Role 页面进行按钮级和部分目标对象级可见性控制；后台 Service 仍重复强制执行边界。

### Component Responsibility

当前系统管理页面主要采用：

- Page：加载列表和依赖数据、搜索/分页状态、权限计算、打开 Dialog。
- Table：只展示并 emit 编辑、权限配置、重置等用户动作。
- Dialog：维护自己的表单、校验、提交状态和 API 调用，成功后通知父页面。
- API 文件按资源组织并统一使用 `request`，不直接新建 Axios 实例。
- TypeScript 业务模型集中在 `src/types`；认证相关类型当前位于 `src/api/auth.ts`。

## 15. Backend Development Conventions

- Controller 保持薄层，响应风格与同模块现有接口一致；当前没有统一成功响应包装。
- Service 写操作应使用事务，先完成权限与合法性校验，再执行任何写入；当前多数写流程已遵循，已知例外见下文。
- 对象级授权放 Service，不依赖 Controller 或前端隐藏。
- Mapper 优先避免 N+1；角色列表通过 LEFT JOIN + COUNT 一次查询权限数量。
- DTO 使用 Jakarta Validation，Service 仍负责数据库相关和授权相关校验。
- 使用构造注入，复用现有实体、Mapper、encoder 和认证 principal。
- 重要写操作与拒绝事件使用 SLF4J 记录操作者 ID、目标 ID、稳定业务标识和结果。
- 禁止记录明文密码、密码摘要、JWT、密钥或完整认证请求体。
- 不为局部功能另建重复的 Result、认证上下文或 JWT 工具体系。

## 16. Database Conventions

### Core RBAC Tables

- `sys_user`：登录身份、BCrypt 摘要、状态、审计字段、逻辑删除和版本。
- `sys_role`：唯一 role_code、名称、data_scope、状态、备注、逻辑删除和版本。
- `sys_permission`：唯一 permission_code、树形 parent_id、MENU/BUTTON/API 类型、路由/API 元数据、排序和状态；当前无 deleted 字段。
- `sys_user_role`：用户与角色多对多，复合主键。
- `sys_role_permission`：角色与权限多对多，独立 BIGINT 主键并具有唯一关系约束。

### Status and Deletion

- user/role `status`: `1` 启用、`0` 停用。
- user/role `deleted`: `0` 有效、`1` 逻辑删除，由 MyBatis-Plus 全局配置及 `@TableLogic` 管理。
- permission 只有 status，没有逻辑删除字段。
- 停用是保留数据与关系但不产生有效认证能力；删除与停用不得混为一谈。

### ID and JavaScript Precision

- 后端实体主键使用 Long/BIGINT 和 `ASSIGN_ID`。
- 用户列表直接返回字符串 ID；角色和权限 VO 使用 `ToStringSerializer`。
- 前端 User/Role/Permission 的业务 ID 和 ID 数组使用 string，避免 JavaScript Number 丢失雪花 ID 精度。
- 新增列表/关联接口应延续字符串 ID 策略。
- `/auth/me` 当前仍将用户 ID 声明为后端 Long、前端 number；这是现存不一致，见 Known Issues。

### Customer V1 Schema Facts

- `sys_department` 已作为最小部门基础模型建立，包含稳定部门编码、nullable `parent_id` 层级、自引用 FK、启停状态、排序、时间和逻辑删除字段。
- `sys_user.dept_id → sys_department.id` 已建立；当前模型为一名用户最多归属一个部门，NULL 表示未归属。
- Customer V1 开发数据预置了 `SALES_DEPT_01`（销售一部）和 `SALES_DEPT_02`（销售二部），并为指定的 5 个销售测试账号规划了部门归属。
- `SALES_MANAGER.data_scope` 已切换为 `DEPT`；`SUPER_ADMIN` / `SYSTEM_ADMIN` 保持 `ALL`，`SALES_STAFF` 保持 `SELF`。
- Customer V1 的 11 个功能权限码已进入初始化数据；SUPER_ADMIN / SYSTEM_ADMIN 绑定 9 个（不含 `customer_pool:claim`、`follow:create`），SALES_MANAGER / SALES_STAFF 绑定全部 11 个。
- Customer 功能权限与 `sys_role.data_scope` 独立：前者决定能否使用功能，后者决定可访问的 Customer 范围。
- Customer 不冗余 `dept_id`；DEPT 归属通过 `customer.owner_id → sys_user.dept_id → sys_department.id` 推导。
- `customer.owner_id IS NULL` 的公海客户不属于具体 DEPT，由独立公海列表与领取规则处理。
- `sys_department.parent_id` 为未来组织层级和 DEPT_AND_CHILD 预留；当前没有 Department Entity、CRUD 或组织树 API。
- Customer List 已实现 ALL / DEPT / SELF 数据范围过滤；DEPT 通过 `customer.owner_id → sys_user.dept_id` 约束客户归属。
- `GET /api/customers` 仅返回 `deleted=0 AND owner_id IS NOT NULL` 的已分配客户，公海客户不进入普通客户列表。
- Customer List 的有效数据范围只取自实际授予 `customer:list` 的启用角色，优先级为 ALL > DEPT > SELF；DEPT 用户缺少部门时明确拒绝访问。
- `customer.owner_id` 可为空，仍通过 FK 关联 `sys_user.id`；NULL 表示公海客户，非 NULL 表示已分配客户。
- `follow_record.contact_id` 可为空，并通过 FK 关联 `contact.id`；它表示本次跟进可选涉及的具体联系人。
- `follow_record` 原有 `target_type + target_id` 多态目标结构保持不变。

## 17. Exception and Security Conventions

- 401：未认证、无效/过期 JWT、登录凭证错误或账号不可用。
- 403：Spring Security 方法权限拒绝，或 Service 对象级授权异常。
- 404：目标用户或角色不存在。
- 400：DTO validation、无效角色/权限关系等请求错误。
- 409：用户名或角色编码唯一冲突。
- 安全入口和拒绝处理器输出 `application/problem+json`，字段包括 title、status、detail、code。
- 模块业务异常也返回 ProblemDetail，但 title/code 由各模块 handler 决定，尚未完全标准化。
- 不向前端返回 password、passwordHash、deleted、原始 GrantedAuthority 或内部密钥。

## 18. Testing Strategy

### Existing Layers

- Controller：MockMvc 验证路径、JSON、validation、匿名 401、authority 403 和异常映射。
- Service：Mockito 验证 happy path、事务、对象级授权、完整角色集合、无效/停用数据及“拒绝时零写入”。
- Security：UserDetails 状态、JWT 签发/解析、过滤器认证闭环与错误 token。
- 当前没有独立 Mapper 集成测试、Testcontainers 或前端测试套件。

### Expectations for New Work

至少考虑：成功路径、输入边界、未认证、方法权限、对象级授权、not found、停用/删除数据、边界角色和拒绝后的无副作用。安全相关修改还要回归登录、JWT filter 和 `/auth/me`。

常用验证命令：

```powershell
cd backend
mvn.cmd test

cd ../frontend
npm.cmd run type-check
npm.cmd run build
```

不要声称存在 `npm test` 或 lint 命令；当前 package.json 未定义它们。

## 19. Current Module Status

| Module | Feature | Status | Notes |
|---|---|---|---|
| Authentication | login, access JWT, filter, `/auth/me` | Implemented | 无 refresh token、服务端 logout、token blacklist |
| User Management | list/create/edit/role binding/reset password | Implemented | 前端本地搜索分页；无删除接口 |
| Role Management | list/create/edit/permission read-save | Implemented | 创建/编辑/权限配置受 SUPER_ADMIN 边界保护；无删除接口 |
| Permission Management | tree/list/overview | Implemented | 只读管理视图，无权限 CRUD |
| Customer/Contact/Follow-up | customer list frontend/backend + rules/schema | Partially implemented | 客户列表已实现服务端分页、筛选及权限展示；无客户详情/写接口、联系人/跟进业务层和对应真实页面 |
| Clue/Opportunity/Contract | schema + menu placeholder | Not implemented | 无后端业务层和真实前端页面 |
| Analytics/AI | menu placeholder | Not implemented | 无真实数据或 API |

### Customer Management V1 Baseline

- Customer、Contact、FollowRecord 是下一阶段活动领域；完整冻结规则见 `docs/customer-management/CUSTOMER_V1_RULES.md`。
- `customer.owner_id IS NULL` 表示公海；非 NULL 表示已分配客户。
- V1 角色范围配置为：SUPER_ADMIN、SYSTEM_ADMIN 为 ALL，SALES_MANAGER 为 DEPT，SALES_STAFF 为 SELF；Customer List 已按实际授权角色的 `data_scope` 执行 ALL / DEPT / SELF 过滤。
- SUPER_ADMIN、SYSTEM_ADMIN 可查看但不领取公海，也不创建跟进记录；SALES_MANAGER、SALES_STAFF 可领取公海并创建跟进。
- Contact 访问继承所属 Customer；每个 Customer 最多一个主要 Contact。
- FollowRecord 可选关联 Contact；`owner_id` 表示实际跟进执行人，`created_by` 表示记录创建人。
- Customer V1 不提供客户删除；Contact 删除采用逻辑删除；FollowRecord V1 只提供列表和新增。
- 公海领取必须通过带 `owner_id IS NULL AND deleted=0` 条件的原子更新防止并发重复领取。
- Customer List 前端直接使用后端 `records/total/page/size/pages` 服务端分页，不对当前页数据再次执行前端业务筛选或数据范围过滤。
- 负责人候选暂时复用受 `user:list` 保护的用户列表；SALES_STAFF 隐藏该筛选，其他缺少 `user:list` 的角色也不发起无权请求。
- 除已完成的 Customer List 前后端查询及其数据范围外，其余 Customer 写操作、详情、公海、Contact 与 FollowRecord 能力仍是后续实现基线。

## 20. Known Issues and Deferred Work

### 20.1 Known Technical / Security Issues

- reset password 后 JWT 不立即失效
- /auth/me ID 类型不一致
- user create 的 Service defense 不完整
- DataInitializer bootstrap credential
- RoleServiceImpl.createRole transaction inconsistency

### 20.2 Deferred Features / Engineering Work

- user delete
- role delete
- permission CRUD
- permission list pagination
- permission overview scalability
- DB migration
- frontend automated tests
- customer/sales permission integration

## 21. Development Guardrails for Codex

1. 每个任务先读本文档，再读任务相关真实代码、测试和 SQL。
2. Context 不能替代代码；冲突时以当前代码和测试为准，并在必要时更新 Context。
3. 采用最小修改原则，不顺手修复或重构无关模块。
4. 不覆盖、reset 或删除用户已有的未提交改动。
5. 前端隐藏只用于 UX，所有安全规则必须由后端执行。
6. 方法级权限写在 Controller；目标对象授权和防御判断写在 Service。
7. 对多角色用户使用完整角色集合进行安全判断。
8. 新功能复用现有分层、统一 Axios、auth store、PasswordEncoder 和 ProblemDetail 风格。
9. 新权限必须同步考虑权限数据、角色关系、后端注解、前端可见性、三种权限视图和测试。
10. Long/BIGINT 业务 ID 对前端按 string 处理，除非全局策略被明确调整。
11. 写操作应事务化，并确保授权失败不会产生部分数据库写入。
12. 关键业务和安全拒绝记录必要日志，但绝不记录密码、JWT、secret 或数据库凭证。
13. 新功能至少补对应 Controller/Service 测试；完成后运行相关测试和前端 type-check/build。
14. 不自动执行会改写真实数据库的 SQL；提供明确、尽量幂等的手工执行说明。
15. 不自动 commit 或 push。

## 22. Context Maintenance Rules

只有任务改变以下长期事实时才更新本文档：

- 总体架构或技术栈；
- 认证/JWT 流程；
- RBAC、角色边界或对象级授权；
- API、数据库、ID 或异常约定；
- 模块实现状态或明确的安全限制；
- 开发、日志或测试约定。

不要因为变量改名、普通 DTO、局部样式、一行 SQL 或单个测试方法而追加记录。更新时应删除失效事实并压缩重复内容，保持 high-signal、low-noise；本文档不是任务执行日志或 Git changelog。

## Terminology

- Method-level authorization:
  Controller `@PreAuthorize` 对 authority / role 的入口控制。

- Object-level authorization:
  Service 根据当前操作者与目标对象角色/状态执行的二次业务授权。

- System-level role:
  当前指 SUPER_ADMIN、SYSTEM_ADMIN。

- Business role:
  当前指 SALES_MANAGER、SALES_STAFF。

- Permission configuration relation:
  `sys_role_permission` 中存储的角色-权限关系。

- Effective authority:
  当前用户经过启用角色、启用权限等过滤后实际进入 SecurityContext 的 authority。

- Disabled:
  数据保留，但当前不产生有效能力。

- Deleted:
  逻辑删除，不等同于 disabled。
