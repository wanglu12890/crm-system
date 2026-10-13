# CRM 后端开发规范（V1）

## 1. 适用范围与事实来源

本规范适用于 `backend` 下 Spring Boot 代码的新增、修改和审查。技术基线为 Java 17、Spring Boot 3.5.16、Spring Security、JWT、MyBatis-Plus 3.5.7、MySQL 8、Lombok 与 Jakarta Validation。

开始任务时先检查同模块或相似模块的当前实现。生产代码、测试和数据库结构优先于本文档；若现状与建议不同，先报告差异，再做任务范围内的最小调整。

详细业务规则不在本规范重复维护：客户规则见 `docs/customer-management/CUSTOMER_V1_RULES.md`，AI Agent 边界见 `docs/ai-agent/`。

## 2. 目录与分层

```text
controller/       HTTP 路径、校验、状态码、方法级授权
service/          业务接口
service/impl/     业务规则、事务、对象级授权、数据范围协调
mapper/           MyBatis-Plus Mapper 与定制查询
entity/           数据库实体
dto/              请求模型、查询模型、内部传输模型
vo/               对外响应模型
security/         认证、授权入口与数据范围
config/           框架配置和初始化
exception/        业务异常与 ProblemDetail 映射
```

当前项目普遍使用 `@RequiredArgsConstructor` 构造注入。新增代码继续采用构造注入，不使用字段注入。

## 3. Controller、Service、Mapper 边界

### Controller

- 使用 context path 内部路径，例如 `@RequestMapping("/customers")`，不得写成 `/api/customers`。
- 负责 `@Valid`、`@RequestBody`/`@ModelAttribute`、HTTP 状态、VO 返回和 `@PreAuthorize`。
- 保持薄层，不手动解析 JWT，不直接访问 Mapper，不承载对象级业务规则。
- 按当前接口风格直接返回 VO、列表或 `ResponseEntity`；不要为单个功能另造统一响应包装。

### Service

- 负责业务校验、事务边界、当前操作者、对象范围授权和多个 Mapper 的编排。
- 写操作及“校验后整体替换”操作使用 `@Transactional`；先完成授权与输入集合校验，再产生持久化副作用。
- 安全边界采用拒绝优先：身份、角色、权限或数据范围不明确时拒绝，不自动扩大权限。
- Service 返回 VO/结果 DTO 或明确的领域结果，不把 Entity 直接泄露给 Controller。

### Mapper

- 简单单表 CRUD 复用 `BaseMapper`；联表、聚合、数据范围与精确更新使用语义清晰的定制方法。
- 当前项目同时存在注解 SQL和 XML Mapper。新增 SQL 优先跟随目标 Mapper 已有风格；复杂动态 SQL、可复用条件片段优先 XML。
- Mapper 参数使用 `@Param` 明确命名，SQL 别名与 Java 属性保持可读映射。
- 禁止 Service 循环触发可合并的查询，避免明显 N+1。

## 4. DTO、VO 与 Entity

- Entity 精确映射表字段、主键、逻辑删除和时间字段，仅用于持久化边界。
- 请求 DTO 承载 Jakarta Validation；不要只依赖前端校验。
- 查询参数使用独立 Query DTO，避免 Controller 出现不断增长的散列参数。
- VO 只暴露接口所需字段，不返回密码摘要、deleted、内部 authority 对象等安全实现细节。
- 数据库 BIGINT ID 对 JavaScript 可能超出安全整数；遵循现有接口约定，将需要传给前端的雪花 ID 转为字符串。
- 内部聚合结果可使用专用 DTO，但需与 API VO 区分，避免把 Mapper 投影直接当成公共合同。

## 5. API 与错误响应

- REST 路径使用资源复数；GET 查询、POST 创建或动作、PUT 完整业务更新，具体风格与现有 User/Role/Customer 接口一致。
- 创建成功按现有接口返回 `201 Created`；查询成功返回 `200 OK`。
- 参数格式由 Validation 拒绝；业务冲突、未找到、禁止访问和认证失败使用正确的 4xx，不返回 500。
- 当前错误响应采用 RFC 7807 `ProblemDetail`，并附稳定 `code`。新增模块优先建立模块异常和对应 Advice，复用已有安全入口的 401/403 处理。
- 不把数据库异常、堆栈、SQL 或敏感字段原样返回前端。

## 6. 认证、RBAC 与数据范围

- 登录使用 `AuthenticationManager` 标准链路；业务代码不得自行比较 BCrypt。
- Controller 不重复验证 JWT。认证后的 principal 使用 `SecurityUser`，由过滤器和 `CustomUserDetailsService` 恢复。
- 功能权限使用 `@PreAuthorize("hasAuthority('resource:action')")`；需要角色与权限组合时显式表达。
- 前端权限控制不是安全措施。目标用户、目标角色、根管理员保护等对象规则必须在 Service 再校验。
- 客户等数据查询通过 `DataScopeResolver` 解析 `ALL`、`DEPT`、`SELF`，并把 `DataScopeContext` 传给 Mapper。禁止前端传入或覆盖可信数据范围。
- 未识别范围、缺失部门或混乱角色组合应 fail closed；详细规则以现有 Service 和安全测试为准。
- 新权限需同步评估权限初始化 SQL、角色关联、Controller 授权、前端可见性和 401/403 测试。

## 7. 数据库与 SQL

- 所有表名、字段、状态值和默认值先从 `database/init.sql`、实体与实际 Mapper 确认，禁止猜测。
- 查询显式遵守 `deleted = 0`；联表时分别考虑主表和关联实体的启用/删除语义。
- 分页查询使用 MyBatis-Plus `Page/IPage`，筛选和数据范围在 SQL 端完成，不对当前页结果做全局业务过滤。
- 列表必须有稳定排序；统计空值使用 `COALESCE` 等方式返回明确的 0，不把 null 传播到业务层。
- 可复用的安全敏感 WHERE 条件抽为同一 XML SQL 片段，防止列表、统计和导出产生不同可见范围。
- 手工初始化脚本应幂等、不可覆盖真实凭据或权限关系，并与生产 schema 初始化分离。

## 8. 事务、并发与异常

- 事务放在 Service 公共方法，覆盖完整业务写入单元；不要在 Controller 开启事务。
- 唯一性先做友好校验，数据库唯一约束作为最终并发兜底，并将 `DuplicateKeyException` 转为稳定业务异常。
- 批量关系替换先校验全部目标记录，再删除旧关系和写入新关系；异常必须整体回滚。
- 更新使用明确字段或已有实体规则，避免无意覆盖密码、角色、审计字段或逻辑删除状态。
- 捕获 `DataAccessException` 时保留 cause 供服务端诊断，但对外只返回安全的业务信息。

## 9. 日志与敏感信息

- 使用 SLF4J 参数化日志，记录操作类型、稳定业务 ID、操作者 ID 和失败类型。
- 禁止记录明文密码、密码摘要、JWT、Authorization、Cookie、数据库凭据、AI API Key 或完整敏感请求对象。
- 不依赖 Lombok 自动生成的 `toString()` 输出认证 DTO、配置属性或安全对象；安全相关对象应避免包含敏感字段的字符串表示。
- 数据库异常日志不要拼接完整 SQL 参数；认证失败不要泄露“用户不存在”和“密码错误”的内部差异。
- 配置加载、MyBatis 与安全日志级别遵循 `application.yml` 的脱敏基线，不为排查方便提交 DEBUG/TRACE 敏感输出。

## 10. 测试规范

- Controller：使用 `@WebMvcTest`、MockMvc 与 Security Test，覆盖匿名 401、权限不足 403、成功合同、参数校验和关键安全字段不泄露。
- Service：使用 JUnit 5 + Mockito/AssertJ，覆盖正常路径、边界角色、对象权限、数据范围、事务前置校验和 Mapper 失败。
- Security：覆盖 JWT 有效/无效/过期、authority 构建、401/403 ProblemDetail 与敏感对象日志行为。
- Mapper：复杂动态 SQL 至少做结构测试；涉及真实 MySQL 特性的查询，在可用环境补充集成验证，不用 H2 结论替代 MySQL 事实。
- 修复缺陷时先补能复现问题的测试。安全、数据一致性和事务测试不能因实现简单而省略。
- 运行范围遵循 `AGENTS.md` 的分层策略；AI live test 只在显式环境条件满足时运行。

## 11. 配置与环境变量

- `application.yml` 只保存公共安全默认值；本地敏感覆盖文件不得进入构建产物或 Git。
- 数据库连接、JWT secret、初始化管理员密码、通义千问 API Key 等必须从环境变量注入。
- 新增 `@ConfigurationProperties` 时提供校验和安全默认行为，不给 secret 设置可用的硬编码默认值。
- 功能默认关闭时应在缺少外部配置的普通构建/测试中安全退化，不触发真实网络调用。
- 不在测试报告、异常消息或文档示例中写入真实本地凭据。

## 12. 现状与演进建议

以下是当前事实，不应在普通业务任务中顺手重构：

- 异常处理按模块拆分为多个 `@RestControllerAdvice`，尚未统一为全局业务异常层。
- Mapper 同时存在注解和 XML 两种风格；选择应以目标模块复杂度和既有实现为准。
- 数据库变更仍使用手工 SQL，没有迁移框架。
- 前端缺少完整组件测试，后端的真实 MySQL 集成覆盖有限。

若任务需要改变这些现状，应单独评估影响、迁移路径和回归范围。
