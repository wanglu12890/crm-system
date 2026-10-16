# RESTful API 规范

状态说明（2026-10-16）：本文保留整体接口规划；具体已实现契约以Controller、DTO/VO和测试为准。AI V1/V1.1已完成并验收，真实契约见下文“AI智能分析”。V2尚未开发。

## 通用约定

- 基础路径：`/api`；建议版本化生产接口为 `/api/v1`，当前骨架上下文已配置 `/api`。
- Content-Type：`application/json;charset=UTF-8`。
- 认证：`Authorization: Bearer <access_token>`；当前登录公开，刷新接口属于规划。
- 时间：请求和响应使用 ISO 8601，例如 `2026-08-02T10:30:00+08:00`。
- 分页：`page=1&pageSize=20`，默认按稳定字段排序；`pageSize` 最大 100。
- 幂等：PUT/DELETE 应幂等；合同提交等命令型接口使用 `Idempotency-Key`。
- 状态码：200 查询/更新成功，201 创建成功，204 删除成功，400 参数错误，401 未认证，403 无权限，404 不存在，409 状态冲突，500 服务异常。

以下统一成功包装为规划示例，当前代码按模块直接返回VO/列表；不得用此包装解析AI响应：

```json
{ "code": "SUCCESS", "message": "success", "data": {}, "traceId": "..." }
```

分页 `data`：

```json
{ "records": [], "page": 1, "pageSize": 20, "total": 0 }
```

以下为规划的错误包装示例；当前AI及已有模块采用RFC 7807 ProblemDetail和稳定code，AI实际错误结构见后文：

```json
{ "code": "CUSTOMER_NOT_FOUND", "message": "客户不存在", "data": null, "traceId": "..." }
```

## 资源接口

为兼容题目给出的命名，登录使用 `/auth/login`；业务资源采用 REST 名词复数形式，不使用 `/list`、`/add`、`/update` 动词，以 HTTP 方法表达动作。

下表包含规划接口，不表示已全部实现；当前登录只返回Access Token，refresh/logout等规划能力未实现。AI正式入口单列，不能按规划路径推断功能存在。

| 模块 | 方法与路径 | 职责 |
|---|---|---|
| AI智能分析（已实现） | `POST /api/ai/analysis` | 客户总量/等级分布及范围、时间口径，直接返回AiAnalysisVO |
| 认证 | `POST /api/auth/login` | 用户名密码登录，返回 Access/Refresh Token |
| 认证 | `POST /api/auth/refresh` | 刷新访问令牌 |
| 认证 | `POST /api/auth/logout` | 撤销刷新令牌/会话 |
| 认证 | `GET /api/auth/me` | 当前用户、角色、权限 |
| 客户 | `GET /api/customers` | 分页查询客户，支持关键词/等级/负责人过滤 |
| 客户 | `GET /api/customers/{id}` | 客户详情 |
| 客户 | `POST /api/customers` | 创建客户，成功返回 201 |
| 客户 | `PUT /api/customers/{id}` | 全量更新客户 |
| 客户 | `PATCH /api/customers/{id}` | 局部更新或负责人调整 |
| 客户 | `DELETE /api/customers/{id}` | 逻辑删除客户，返回 204 |
| 联系人 | `GET /api/contacts` | 分页查询联系人，可按 customerId 筛选 |
| 联系人 | `POST /api/contacts` | 创建联系人 |
| 联系人 | `GET/PUT/DELETE /api/contacts/{id}` | 联系人详情、更新、删除 |
| 线索 | `GET/POST /api/leads` | 查询/创建线索 |
| 线索 | `GET/PUT/DELETE /api/leads/{id}` | 线索详情、更新、删除 |
| 线索 | `POST /api/leads/{id}/convert` | 原子转化为客户/联系人/商机 |
| 商机 | `GET/POST /api/opportunities` | 查询/创建商机 |
| 商机 | `GET/PUT/DELETE /api/opportunities/{id}` | 商机详情、更新、删除 |
| 商机 | `PATCH /api/opportunities/{id}/stage` | 变更销售阶段 |
| 跟进 | `GET/POST /api/follow-ups` | 按目标对象查询/创建跟进 |
| 跟进 | `GET/PUT/DELETE /api/follow-ups/{id}` | 跟进详情、更新、删除 |
| 合同 | `GET/POST /api/contracts` | 查询/创建合同 |
| 合同 | `GET/PUT/DELETE /api/contracts/{id}` | 合同详情、更新、删除 |
| 合同 | `POST /api/contracts/{id}/submit` | 提交审批 |
| 统计 | `GET /api/statistics/dashboard` | 仪表盘聚合数据 |
| 统计 | `GET /api/statistics/sales-funnel` | 销售漏斗 |
| 系统 | `GET/POST /api/system/users` | 用户查询/创建 |
| 系统 | `GET/PUT/DELETE /api/system/users/{id}` | 用户详情、更新、停用/删除 |
| 系统 | `GET/POST /api/system/roles` | 角色查询/创建 |
| 系统 | `PUT /api/system/roles/{id}/permissions` | 分配角色权限 |

前端的 `src/api/<module>.ts` 只暴露带类型的方法；`utils/request.ts` 统一配置 baseURL、超时、Token、重复请求与错误处理。页面调用 API 后将跨页面状态存入 Pinia，局部表格/表单状态保留在组件内。

## AI智能分析（V1 / V1.1已实现）

- 入口：`POST /api/ai/analysis`，必须同时具有`ai:analysis`、`customer:list`；身份来自JWT的SecurityUser。
- 请求：`{"question":"我个人有多少客户？"}`，question非空白且最多1000字符；没有用户ID、角色或范围输入字段。
- 成功：HTTP 200，直接返回`answer`、`intent`、`metrics`、`analysisContext`，不包裹data。intent只有`CUSTOMER_TOTAL`、`CUSTOMER_GRADE_DISTRIBUTION`。
- metrics包含`totalCustomers`、`levelA`、`levelB`、`levelC`、`levelD`、`unrated`六个计数；零值为合法成功结果。
- analysisContext包含实际执行`dataScope`（ALL/DEPT/SELF）、`timeRange`、`referenceTime`、`timezone`，有时间筛选时还含`startTime`、`endTime`。ALL时间内部边界为null，当前NON_NULL配置使JSON省略边界字段。
- V1.1经理查询本人返回SELF及本人文案；管理员ALL不等于具备本人查询资格。完整业务矩阵及时间定义只维护在[AI需求基线](ai-agent/AI_AGENT_V1_REQUIREMENTS.md)。Tool内部范围绑定不是新增HTTP请求参数，模型Tool参数仍只有timeRange。
- 错误：ProblemDetail包含`type`、`title`、`status`、`detail`、`instance`及稳定`code`。不合法范围返回422 `AI_ANALYSIS_UNSUPPORTED`，不返回metrics；双权限缺失403，未认证401，AI未启用503。完整错误码及成功示例见上述需求基线。

前端正式路由为`/admin/analytics/ai`，请求/响应类型位于`frontend/src/types/aiAnalysis.ts`。其时间边界目前声明为必填可空，与ALL实际省略字段存在类型差异，已记录为后续源码维护项，不在本文档任务中修改。
