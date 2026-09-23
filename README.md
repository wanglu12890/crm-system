# crm-system

企业 B 端 CRM 客户管理系统，采用前后端分离、模块化单体架构。

## 技术栈

- 后端：Java 17、Spring Boot 3.3.5、Maven、Spring Security、JWT（JJWT 0.12.6）、MyBatis-Plus 3.5.7、MySQL 8、Lombok
- 前端：Vue 3、Vite 5、TypeScript、Vue Router、Pinia、Axios、Element Plus
- 工程方式：RESTful API、环境变量配置、RBAC 权限模型、统一响应与异常协议、逻辑删除和审计字段

## 目录结构

```text
crm-system/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/company/crm/
│       │   │   ├── common/       # 统一响应、分页、常量、基类
│       │   │   ├── config/       # 框架和基础设施配置
│       │   │   ├── controller/   # HTTP 协议入口
│       │   │   ├── dto/          # 输入与服务参数对象
│       │   │   ├── entity/       # 数据库实体
│       │   │   ├── exception/    # 异常与错误码治理
│       │   │   ├── mapper/       # 数据访问接口
│       │   │   ├── security/     # JWT、认证和授权
│       │   │   ├── service/      # 业务接口
│       │   │   │   └── impl/     # 业务实现
│       │   │   ├── utils/        # 无状态通用工具
│       │   │   └── vo/           # API 输出视图对象
│       │   └── resources/
│       │       └── mapper/        # MyBatis XML
│       └── test/                  # 分层测试与集成测试
├── frontend/
│   ├── src/
│   │   ├── api/                   # 分模块 API 方法
│   │   ├── assets/                # 静态资源
│   │   ├── components/            # 公共组件
│   │   ├── layouts/               # 页面布局
│   │   ├── router/                # 路由与守卫
│   │   ├── stores/                # Pinia 状态
│   │   ├── types/                 # TypeScript 类型
│   │   ├── utils/                 # Axios 与通用工具
│   │   └── views/                 # 业务页面
│   ├── package.json
│   └── vite.config.ts
├── database/
│   └── init.sql                   # MySQL 8 初始化 DDL
└── docs/
    ├── architecture.md            # 分层、模块和完整数据流
    ├── api-spec.md                # REST API 与交互规范
    └── database.md                # 字段设计与初始化说明
```

详细目录设计、分层职责、DTO/VO 区别、数据流和九大业务模块见 [架构设计](docs/architecture.md)，接口清单见 [API 规范](docs/api-spec.md)。

## 核心模块

用户认证、客户、联系人、线索、商机、跟进记录、合同、数据统计和系统管理。权限模型采用 `用户 -> 角色 -> 权限` 的 RBAC 关系；功能权限由 Spring Security 执行，客户等业务数据还需按负责人、部门或自定义范围过滤。

## 数据库设计

`database/init.sql` 初始化脚本包含：

- `sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`
- `customer`、`contact`、`clue`、`business`、`follow_record`、`contract`

主要关系：用户与角色、角色与权限均为多对多；客户一对多联系人和商机；商机可关联联系人；合同归属客户并可关联商机；跟进记录通过 `target_type + target_id` 关联客户、线索或商机。多态关系无法使用普通外键，必须在 Service 层验证目标存在性，并建立组合索引。

## Maven 配置

`backend/pom.xml` 以 `spring-boot-starter-parent:3.3.5` 管理依赖，编译目标为 Java 17。核心依赖包括 Web、Validation、Security、MyBatis-Plus Spring Boot 3 Starter、MySQL Connector/J、JJWT API/实现/Jackson、Lombok，以及后端测试组件。

MyBatis-Plus 已配置驼峰映射、雪花主键、全局逻辑删除和 Mapper XML 路径。开发环境开启 SQL 控制台日志，生产配置关闭；正式项目建议接入结构化 SQL 慢查询监控，禁止生产输出完整 SQL 参数。

Spring Security 与 JWT 当前仅完成依赖和配置项预留。业务阶段需要实现 `SecurityFilterChain`、JWT 过滤器、认证失败/无权限处理器、`UserDetailsService`、密码编码器以及刷新令牌撤销策略；不得使用默认生成密码投入运行。

## Vue 配置

- Vite：`@` 映射到 `src`，开发服务器为 5173，`/api` 代理到后端 8080。
- 路由：静态路由只保留登录/错误页；登录后根据后端权限生成动态业务路由，页面组件必须懒加载。
- Pinia：按 `auth`、`permission`、`dictionary` 等领域拆 Store；表格筛选和弹窗等页面局部状态不放入全局 Store。
- Axios：`src/utils/request.ts` 统一 baseURL、超时和 JSON 协议，后续在拦截器加入 Token、单次刷新并发控制、错误提示和 traceId 上报。
- API：每个模块在 `src/api` 中定义方法，并在 `src/types` 定义请求与响应类型，页面禁止直接调用 Axios。

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20 LTS+ 与 npm 10+
- MySQL 8.0+

## 数据库初始化

1. 使用管理员账号执行 `database/init.sql`，创建 `crm_system` 数据库和表，具体命令见 [数据库说明](docs/database.md)。
2. 创建最小权限应用账号，仅授予该库所需的 DML 权限；生产环境由 DBA/迁移工具执行 DDL。
3. 设置环境变量 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和至少 32 字节随机 `JWT_SECRET`。
4. 后续迭代建议引入 Flyway，将每次变更保存为不可修改的版本迁移，禁止启动时自动改表。

PowerShell 示例（请替换真实值）：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/crm_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false'
$env:DB_USERNAME='crm_app'
$env:DB_PASSWORD='your-password'
$env:JWT_SECRET='your-random-secret-at-least-32-bytes'
```

## 启动方法

后端：

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端默认地址为 `http://localhost:8080/api`。数据库必须已初始化且连接变量有效。

前端：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址为 `http://localhost:5173`，开发代理会将 `/api` 转发到 `http://localhost:8080`。

## 开发规范

- 包与模块：业务类按模块+分层组织；Controller 不访问 Mapper，Service 负责事务与数据权限。
- API：资源使用复数名词，HTTP 方法表达动作；DTO/VO/Entity 禁止混用；所有请求校验并返回统一错误协议。
- 数据：金额禁止使用浮点数；删除默认逻辑删除；唯一性必须由数据库约束兜底；更新关键数据使用乐观锁。
- 安全：密码仅存 BCrypt 哈希；最小权限；Token/密码/敏感个人信息禁止写日志；所有列表查询执行数据权限过滤。
- 代码：Java 类名 PascalCase，方法/变量 camelCase，常量 UPPER_SNAKE_CASE；TypeScript 开启 strict，避免 `any`。
- 事务：事务放在 Service 公共方法；不在长事务内调用外部服务；线索转化等跨表操作必须原子化。
- Git：分支建议 `feature/*`、`fix/*`；提交信息遵循 Conventional Commits；合并前必须通过编译、类型检查、测试和代码评审。
- 测试：业务规则做单元测试，Mapper 做数据库集成测试，认证和核心销售链路做 API 集成测试。
- 配置：环境差异通过 profile 和环境变量管理；密钥、密码、生产配置不得提交仓库。

## AI-Assisted Development Workflow

在项目开发过程中，我将 ChatGPT 与 Codex 作为主要的 AI 编程工具，并形成了“AI 实现 + 人工评审 + 测试验证 + 迭代修正”的开发流程：

需求定义 → 任务拆解 → Prompt 约束 → Codex 分析/实现 → Code Review → 功能/接口测试 → 问题反馈 → Prompt 迭代

当开发验证中发现需求设计或实现逻辑存在问题时，则重新梳理业务规则和技术约束，调整 Prompt 后交由 Codex 迭代实现，并再次进行代码审查与测试验证。

### Example：角色创建权限控制

在实现角色创建权限控制时，我首先根据已有角色体系定义业务规则，并通过 Prompt 向 Codex 明确修改范围、权限约束及验收目标。

初版规则要求 SYSTEM_ADMIN 不能创建同级角色，实现时通过判断待创建角色的 `roleCode` 是否为 `SYSTEM_ADMIN` 进行限制。

在代码审查和功能验证过程中，我发现 `roleCode` 本质上是可自定义的业务标识。例如，将同级角色设置为其他 `roleCode`，即可绕过这一判断，说明原有规则依赖角色编码进行权限控制并不可靠。

进一步重新梳理角色体系后，我发现问题不仅在代码实现，也来自前期权限模型对角色层级考虑不足。因此重新定义业务规则：角色创建属于系统级权限，仅允许 `SUPER_ADMIN` 执行。随后修改 Prompt 中的业务约束，由 Codex 调整实现，并再次完成代码检查与功能验证。

相关 Prompt：

- [实现角色创建范围授权](task/0919/3_实现角色创建范围授权.md)
- [角色创建范围授权逻辑修改](task/0919/4_角色创建范围授权逻辑修改.md)