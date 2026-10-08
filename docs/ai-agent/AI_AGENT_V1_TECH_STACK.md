# CRM AI Agent V1 技术栈与架构基线

> 文档状态：技术方案已选定，尚未实施与验收  
> 适用范围：CRM AI 销售分析助手 V1 第一阶段  
> 项目路径：`D:\MyProjectPractice\crm-system`  
> 建议保存：`docs/ai-agent/AI_AGENT_V1_TECH_STACK.md`

## 1. 目标与范围

在现有 Java 单体 CRM 中集成基于 Spring AI 的 AI 销售分析助手，支持自然语言提问，经 LLM Tool Calling 调用后端受控统计服务，从 MySQL 查询授权范围内的真实业务统计，并返回可核验的自然语言答案和结构化指标。

第一阶段仅实现：

1. 客户总量统计；
2. 客户等级分布统计（A/B/C/D/未评级）；
3. 简单创建时间筛选（本月、上月、最近 30 天）。

第一阶段必须验证工具调用、ALL/DEPT/SELF 数据权限与 SQL 统计正确性。不包含 RAG、向量数据库、MCP、多 Agent、任意 SQL、历史会话持久化、SSE 或图表。

## 2. 现有技术栈与目标技术栈

| 组件 | 审查时现状 | V1 目标 | 备注 |
|---|---|---|---|
| Java | 17 | 17 | 保持不变 |
| Spring Boot | 3.3.5 | 3.5.16 | 先独立升级、回归测试 |
| Spring AI | 未引入 | 1.1.8 | 使用 GA 版本与 BOM，升级完成后接入 |
| Spring Security / JWT | 已实现 | 复用现有 | 认证、方法权限与身份来源 |
| MyBatis-Plus | 3.5.7 | 优先保持，兼容性验证 | 不为 AI 任意升级 |
| MySQL | 8.x | 保持不变 | 业务数据与聚合 SQL |
| Maven | 3.9.16 | 保持不变 | 依赖构建 |
| Vue / TypeScript | Vue 3.5.12 / TS 5.6.x | 保持现有 | 前端工作区 |
| Element Plus | 2.8.x | 保持现有 | UI 组件 |
| Axios | 1.7.x | 保持现有 | AI 接口设置独立超时 |
| LLM 服务 | 未接入 | 阿里云百炼 | API Key 通过环境变量配置 |
| 模型 | 未接入 | Qwen-Plus（初选） | 以实际 Tool Calling 测试结果确定模型版本 |
| 模型接入 | 未接入 | Spring AI OpenAI 兼容接入 | 验证百炼兼容接口与工具调用能力 |

**状态说明**：目标版本是计划采用的基线，不表示项目已经完成升级、依赖解析或运行验收。实施时须重新核对官方兼容要求、可用依赖和 API 文档；如存在差异，应先更新本文件并获得确认。

官方参考入口：

- Spring AI: https://docs.spring.io/spring-ai/reference/
- Spring AI Tool Calling: https://docs.spring.io/spring-ai/reference/api/tools.html
- Spring AI Getting Started: https://docs.spring.io/spring-ai/reference/getting-started.html
- Spring Boot: https://spring.io/projects/spring-boot
- 阿里云百炼：以其官方 API 文档中的 OpenAI 兼容接口说明为准。

## 3. 技术选型原则

- 采用 **Spring AI**，不同时引入 LangChain4j。
- 采用现有 **Spring Boot 单体应用**，不新增 Python Agent 服务或微服务。
- 通过 **ChatClient + Tool Calling** 实现模型和业务工具交互。
- 仅向模型暴露显式注册、只读、参数白名单的客户统计工具。
- **LLM 不直接连接 MySQL，不执行任意 SQL，不决定权限范围**。
- 统计数据由 Java Service/Mapper 生成；LLM 只负责理解问题、选择工具和解释结果。
- 第一阶段普通 HTTP POST 即可，不引入 SSE。

## 4. 后端职责与调用链

```text
Vue3 AI 智能分析页面
  -> AiAnalysisController（认证、ai:analysis、请求校验）
  -> AiAnalysisService（ChatClient、工具注册、回答编排）
  -> CustomerStatisticsTool（@Tool，业务参数白名单）
  -> CustomerStatisticsService（customer:list、DataScope、时间校验、统计口径）
  -> CustomerStatisticsMapper（COUNT / GROUP BY）
  -> MySQL
```

核心类的候选职责：

- `AiAnalysisController`：获取可信认证主体，校验请求并控制 `ai:analysis` 访问。
- `AiAnalysisService`：构造本次请求的分析上下文、调用 ChatClient、收集可信指标、返回响应。
- `CustomerStatisticsTool`：薄适配层，仅接受统计类型与时间筛选等白名单业务参数。
- `CustomerStatisticsService`：复用既有 `DataScopeResolver`，实施 `customer:list` 权限、参数和时间校验。
- `CustomerStatisticsMapper`：聚合 SQL，不分页加载明细再计数。
- `AnalysisContext`：请求级可信身份、授权范围、固定基准时间和结果收集机制；设计时保证并发隔离。

具体类数量、包名及注入方式应在实现时依据当前项目结构最小化确定，不必机械创建所有候选类。

## 5. 认证、授权与数据范围

Agent 页面与入口权限：`ai:analysis`。客户统计工具额外要求 `customer:list`。

现有 `DataScopeResolver` 是唯一有效范围解析入口：

- ALL：当前有效角色授予的全范围；
- DEPT：当前所属部门，**不包含下级部门**；无部门时拒绝；
- SELF：本人负责客户；
- 多角色有效范围按 ALL > DEPT > SELF；仅真正授予 `customer:list` 的有效角色参与计算。

第一阶段统计口径与当前 Customer List 的**普通客户列表**一致：

```sql
c.deleted = 0
AND c.owner_id IS NOT NULL
AND <由后端实施的 ALL / DEPT / SELF 条件>
```

因此默认排除公海客户；“无权查看公海”应明确拒绝，不得以 0 伪装权限不足。禁止模型参数传入 `userId`、`departmentId`、`role`、`dataScope` 或 SQL。

Controller 从 Spring Security 可信主体获取身份。Tool 不依赖在执行线程重新读取 `SecurityContextHolder`；使用请求级不可变上下文或等效安全机制，避免异步上下文丢失及单例 Tool 并发串号。

## 6. 时间与统计口径

- 业务时区：`Asia/Shanghai`（目标约定，实施时核实配置）。
- `referenceTime`：由后端可注入 `Clock` 在每次请求开始时确定一次；普通用户和 LLM 不可指定可信时钟。
- 本月：当月 1 日 00:00 至下月 1 日 00:00。
- 上月：上月 1 日 00:00 至当月 1 日 00:00。
- 最近 30 天：`referenceTime` 向前滚动 30×24 小时至 `referenceTime`。
- 未指定时间：不增加创建时间筛选。
- 实际过滤字段：`customer.created_at`，SQL 区间统一 `[startInclusive, endExclusive)`。
- 客户等级分布包括 A/B/C/D 及 NULL（未评级）。
- 模拟数据 `agent_reference_answers.json` 可辅助固定数据回归，但不得把模拟批次 300 条当作全库或授权范围总量。

## 7. 前端与响应契约

保留现有后台 Layout，正式页面采用已确认的 AI 销售分析助手原型：左侧“新建分析 / 最近分析”区域，右侧欢迎区或消息列表，底部输入框。参考项目内 UI 原型图片；不要重新设计为普通聊天页面。

第一阶段最近分析为空状态，不伪造历史会话；快捷问题仅展示已实现的客户总量、等级分布、简单时间筛选。现有 `/admin/analytics/ai` 作为正式路由候选；审查并处理重复占位路由 `/admin/ai-analysis`。前端菜单与路由检查 `ai:analysis`，后端独立鉴权。

成功响应直接返回 VO，沿用项目既有模式；异常使用 RFC 7807 `ProblemDetail`：

```json
{
  "requestId": "example-request-id",
  "answer": "当前可访问的普通客户共 120 个。",
  "metrics": [
    { "key": "customer_total", "label": "客户总数", "value": 120 }
  ],
  "analysisContext": {
    "dataScope": "DEPT",
    "scopeLabel": "本部门",
    "referenceTime": "2026-10-08T16:00:00+08:00"
  }
}
```

**示例数值仅用于说明结构，不代表真实查询结果。** 实际时间筛选时可增加 `timeRange`。`metrics` 必须来自后端工具结果，不能从模型文本解析或接受模型自行生成。第一阶段不返回 `toolCalls`、`charts`、`conversationId`。

## 8. 模型接入与安全约束

- 模型服务商：阿里云百炼；模型：Qwen-Plus 初选。
- 采用百炼 OpenAI 兼容 API + Spring AI 的对应模型接入能力；正式编码前验证 endpoint、模型 ID、鉴权及 Tool Calling 行为。
- API Key 从环境变量或安全配置注入，禁止硬编码、提交仓库或写入日志。
- 提交给模型的工具结果应以聚合统计为主，避免发送客户姓名、联系方式、地址等个人或商业敏感明细。
- 模型输入视为不可信，限制工具、参数、执行次数及时间范围；工具端实施权限和参数校验。
- 记录 requestId、工具名称、执行状态、耗时、错误码和脱敏范围信息；不记录密钥、完整客户明细或无必要的敏感提示词。
- 对无工具调用、越权、空数据、模型超时及工具异常提供明确结果或受控降级，不得虚构统计数据。

## 9. 开发阶段与提交边界

**任务 A：升级 Spring Boot**

1. 单独检查 Boot 3.3.5 -> 3.5.16 的 Maven 依赖兼容性。
2. 只做必要的版本升级及兼容性修复。
3. 回归登录/JWT、`/auth/me`、RBAC、ALL/DEPT/SELF、用户/角色、客户列表和创建、异常处理、MyBatis 查询。
4. 测试通过后独立提交，建议：`chore(backend): upgrade Spring Boot to 3.5.16`。

**任务 B：引入 Spring AI 基础依赖**

1. 在 Boot 升级验收后引入 Spring AI 1.1.8 BOM 和模型 Starter。
2. 配置百炼兼容 API 的模型连接，密钥仅通过环境变量传入。
3. 独立验证模型调用和最小 Tool Calling；不要同时实现全部客户统计功能。
4. 独立提交，建议：`build(ai): integrate Spring AI and Qwen model configuration`。

**后续任务**：初始化 `ai:analysis` 权限；实现客户统计 Service/Mapper 与数据库核验；接入 Tool Calling；实现前端原型；完成端到端验收。

## 10. 验收底线

- Spring Boot 升级后原 CRM 核心功能无回归。
- Spring AI 与百炼模型能稳定完成工具调用。
- ALL/DEPT/SELF 统计严格遵守现有 `DataScopeResolver`。
- 客户总量、等级分布、时间筛选可用 SQL 独立核验。
- 权限不足返回拒绝，不得返回其他角色或公海客户数据。
- `metrics` 与实际工具查询一致，模型失败不产生伪造结果。
- 不执行未经明确授权的数据库写入、依赖升级或 Git 提交。

## 11. 相关项目文档

- `docs/ai-agent/AI_AGENT_V1_REQUIREMENTS.md`：业务需求、权限矩阵、前端原型及响应契约。
- `docs/ai-agent/AI_AGENT_V1_DATA_DESIGN.md`：模拟业务数据及统计参考。
- `docs/customer-management/CUSTOMER_V1_RULES.md`：Customer List 业务与数据范围规则。
- `docs/CODEX_CONTEXT.md`、`AGENTS.md`：项目上下文与 Codex 工作约束。

如文档与实际代码存在差异，先核查并更新设计基线，不得静默改变权限或统计口径。
