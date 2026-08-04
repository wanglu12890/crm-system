# RESTful API 规范

## 通用约定

- 基础路径：`/api`；建议版本化生产接口为 `/api/v1`，当前骨架上下文已配置 `/api`。
- Content-Type：`application/json;charset=UTF-8`。
- 认证：`Authorization: Bearer <access_token>`；登录与刷新接口除外。
- 时间：请求和响应使用 ISO 8601，例如 `2026-08-02T10:30:00+08:00`。
- 分页：`page=1&pageSize=20`，默认按稳定字段排序；`pageSize` 最大 100。
- 幂等：PUT/DELETE 应幂等；合同提交等命令型接口使用 `Idempotency-Key`。
- 状态码：200 查询/更新成功，201 创建成功，204 删除成功，400 参数错误，401 未认证，403 无权限，404 不存在，409 状态冲突，500 服务异常。

统一成功响应：

```json
{ "code": "SUCCESS", "message": "success", "data": {}, "traceId": "..." }
```

分页 `data`：

```json
{ "records": [], "page": 1, "pageSize": 20, "total": 0 }
```

错误响应：

```json
{ "code": "CUSTOMER_NOT_FOUND", "message": "客户不存在", "data": null, "traceId": "..." }
```

## 资源接口

为兼容题目给出的命名，登录使用 `/auth/login`；业务资源采用 REST 名词复数形式，不使用 `/list`、`/add`、`/update` 动词，以 HTTP 方法表达动作。

| 模块 | 方法与路径 | 职责 |
|---|---|---|
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
