# CRM AI V2 S2-B1 实施报告

实施日期：2026-10-24。范围：统一聊天入口、语义分类、V1 统计编排迁移与风险分支关闭；止于 B1。

## 1. 实际修改文件与目录

后端生产代码（以下路径均相对 `backend/src/main/java/com/company/crm/`）：

| 文件 | 变化与职责 |
| --- | --- |
| `controller/AiAnalysisController.java` | 返回共同响应接口，保留原入口与双权限 |
| `service/AiAnalysisService.java`、`service/impl/AiAnalysisServiceImpl.java` | 共同响应签名；门面只做身份检查、路由与分发 |
| `service/AiIntentRouter.java`（新增） | 单次无工具分类、严格模型协议校验 |
| `service/AiStatisticsAnalysisService.java`、`service/impl/AiStatisticsAnalysisServiceImpl.java`（新增） | 承接原 V1 统计授权、工具选择、Java 手动执行与响应生成 |
| `dto/ai/internal/AiAnalysisPlan.java`（新增） | 类型化语义候选，不代表授权执行计划 |
| `vo/ai/AiAnalysisResponse.java`（新增） | 无字段标记接口 |
| `vo/ai/AiAnalysisVO.java`、`vo/ai/RiskAnalysisVO.java` | 实现共同接口，保留原字段 |
| `config/AiRiskChatProperties.java`（新增） | 仅聊天风险分支的开关 |
| `exception/AiRiskChatUnavailableException.java`（新增） | 区分关闭与尚未就绪 |
| `exception/AiAnalysisExceptionHandler.java` | 共享入口显式处理风险异常与开关异常 |
| `exception/CustomerRiskExceptionHandler.java` | 提取原风险异常到响应的静态映射，共享入口复用 |

配置：`backend/src/main/resources/application.yml` 新增默认关闭配置。

测试：修改 `service/AiAnalysisServiceTest.java`、`controller/AiAnalysisControllerTest.java`、`ai/smoke/QwenAnalysisLiveIntegrationTest.java`；新增 `controller/AiAnalysisRoutingControllerTest.java`、`config/AiRiskChatPropertiesTest.java`（均在 `backend/src/test/java/com/company/crm/`）。Live 测试只适配编译与装配，未执行外部调用。

文档：更新 `AI_AGENT_V2_ARCHITECTURE_DESIGN.md` 并新增本报告。原 S2-A 报告保持不变；未发现需要同步修改需求文档的直接冲突。

最终采用现有 `controller/`、`service/impl/`、`dto/ai/internal/`、`vo/ai/`、`config/`、`exception/` 目录，没有创建第二套聊天目录或独立风险自然语言 Controller。

## 2. 调用链与分类协议

`POST /api/ai/analysis` → `AiAnalysisController` → `AiAnalysisServiceImpl` → `AiIntentRouter` → `STATISTICS / RISK / UNSUPPORTED`。

Controller 继续要求 `ai:analysis` 与 `customer:list`，不追加 `follow:list`。身份来自认证上下文。HTTP 请求 DTO 仍只定义 question；额外请求字段不成为可信身份或执行参数。

分类模型只接收问题与分类规则，不注册工具，并设置 `internalToolExecutionEnabled=false`。每个合法路由请求只进行一次分类调用。严格接受以下三种互斥对象，所有值必须为字符串：

```json
{"businessType":"STATISTICS","intent":"CUSTOMER_TOTAL","timeRange":"ALL","queryScopeIntent":"CURRENT_AUTHORIZED"}
```

```json
{"businessType":"RISK","riskIntent":"LIST","queryScopeIntent":"CURRENT_AUTHORIZED"}
```

```json
{"businessType":"UNSUPPORTED","reason":"MIXED"}
```

统计意图沿用 `CUSTOMER_TOTAL / CUSTOMER_GRADE_DISTRIBUTION`；时间沿用 `ALL / CURRENT_MONTH / LAST_MONTH / RECENT_30_DAYS`；范围沿用 `CustomerQueryScopeIntent`。风险子意图为 `LIST / REASON / SUGGESTIONS`；拒绝原因为 `OUT_OF_SCOPE / MIXED / AMBIGUOUS`。

额外字段、交叉分支字段、身份和授权字段、未知枚举、错误类型、重复键、尾随 JSON、空响应均受控拒绝。路由器不查数据库、不判断最终权限、不生成 SQL、客户 ID、阈值或风险等级。混合及不明确请求不能回退到统计分支。分类语义由模型识别，后端严格验证结构与枚举；Mock 测试不构成真实模型语义准确率验证。

## 3. V1 迁移及兼容性

原服务的统计工具编排迁入 `AiStatisticsAnalysisServiceImpl`，原工具工厂、工具 schema、统计 Service、SQL 和规则未修改。新的统计分支直接消费类型化分类结果，再做实际数据范围解析、工具选择、工具参数一致性校验、Java 手动执行与结果生成。

普通 V1 成功请求仍为两次模型调用：统一分类一次、原统计工具选择一次。没有第三次分类或工具执行后的总结调用。越权与协议拒绝在相应边界停止。

`AiAnalysisResponse` 是空接口，没有 Jackson 多态注解、type 字段或包装对象。统计成功 JSON 继续为 `answer / intent / metrics / analysisContext`，风险 VO 原字段及 null 合同保留；真实 Controller 序列化回归验证没有新增 `businessType / @type / payload / userId`。

HTTP 路径、权限和 V1 成功 JSON 无变化。Java 接口返回值从 `AiAnalysisVO` 扩为 `AiAnalysisResponse`，属于内部源码类型变化，测试与已有直接调用处已适配。模型内部分类协议增加 businessType，旧三字段模型分类响应将被拒绝；这不是 HTTP 请求/响应协议变化。

## 4. 风险关闭与异常映射

配置 `crm.ai.risk.chat.enabled` 默认 false，可由 `CRM_AI_RISK_CHAT_ENABLED` 提供。它只控制统一聊天风险分支，不控制 S2-A 确定性风险接口。

| 分支或异常 | HTTP / code |
| --- | --- |
| 风险开关关闭 | 422 / `AI_RISK_CHAT_DISABLED` |
| 开关打开，但 B1 没有执行器 | 503 / `AI_RISK_CHAT_NOT_READY` |
| 不支持、混合或不明确 | 422 / `AI_ANALYSIS_UNSUPPORTED` |
| 模型协议错误 | 502 / `AI_MODEL_RESPONSE_INVALID` |
| 模型调用失败 | 503 / `AI_MODEL_UNAVAILABLE` |
| `CustomerRiskException` | 复用 S2-A 原有各 code 的状态码及 ProblemDetail 映射 |

关闭或尚未就绪的风险请求均只完成一次无工具分类，不解析统计授权范围，不调用统计 Service、风险核心 Service、风险应用 Service 或风险 Mapper，不触发第二次模型调用。没有注册风险 Tool。依赖零调用与模型次数由 Mock 验证，包含注入文本、风险原因、建议及清单请求。

共享聊天 Advice 显式处理风险异常，原风险 Advice 作用范围保持不变，不同时绑定到聊天 Controller。关键日志只记录分支、异常类型或错误枚举，不记录完整问题、Prompt、模型正文或客户明细。

## 5. 实际验证

已通过：

- 第一轮 V1 服务、Controller、统计 Tool 与 S2-A Controller 定向回归。
- 第二轮加入统一路由隔离和开关绑定的定向回归。
- 最终后端离线测试及打包：`mvn -o -q -f backend/pom.xml '-Dtest=*,!QwenLiveIntegrationTest,!QwenAnalysisLiveIntegrationTest,!CustomerRiskMapperMySqlTest' package`。535 项测试，0 失败、0 错误、0 跳过；包含 V1 服务 83 项、聊天 Controller 11 项、真实路由 Controller 38 项、开关 1 项、统计 Tool 11 项、S2-A Controller 42 项，以及 S1/S2-A 服务和项目其他离线测试。
- 统一路由测试覆盖无 follow:list 的统计成功、严格协议、伪造身份/规则字段、混合拒绝、风险零依赖调用、开关两种状态、模型故障、两次统计模型调用与 JSON 字段兼容。

未执行真实 Qwen 测试及 MySQL 集成测试，避免外部模型请求和数据库访问；不能据此声明真实模型或实际数据库联调通过。

前端离线回归：`node --test tests/ai-analysis.test.mjs`，5 项全部通过。首次运行因沙箱阻止 Vite 更新缓存而报 EPERM；尝试独立缓存也被文件写入限制阻止，随后在获准的提升权限执行中使用原测试命令成功完成，未修改前端源码或测试文件。

最终检查：`git diff --check` 通过；新增文件尾随空白检查通过；文档所列生产代码、测试路径存在。Git 仅提示现有 Windows LF/CRLF 转换规则，没有差异格式错误。后端生成 `backend/target/crm-backend-0.1.0-SNAPSHOT.jar`。

## 6. 边界、后续依赖与验收结论

V1 离线回归通过；S2-A Controller、应用服务与 S1 核心回归通过。没有修改数据库结构、风险规则、阈值、权限矩阵或 S2-A HTTP/JSON/错误合同，没有修改前端业务页面。

B1 不提供风险数据查询、风险 Tool、客户原因模型总结、跟进建议或前端风险卡片。即使打开开关也不会执行未实现的风险能力。

B2 需在既有门面下接入风险分支应用服务及受限工具，复用 S2-A 风险能力与可信身份、数据范围机制；在数据库查询前校验可信上下文，并在模型发送及返回前进行批量权限复核。风险对象选择、风险专属参数校验和类型化执行计划仍待 B2，不能直接把本阶段分类候选当作授权结果。后续响应组装及前端展示按 D1 的 B2/B3/B4 边界实施，不在本次提前创建。

主要限制：真实模型分类效果未联调；内部模型协议已调整，部署时需使用新的分类 Prompt；Java 直接调用方需接受共同响应类型。开关不是授权机制，未来风险执行路径仍必须实现完整后端权限校验。

结论：本次 B1 代码与后端离线验收要求已完成，可在人工验收后进入 B2；本次到 B1 停止。工作区保留本任务的未提交修改与新增文件，未执行暂存、commit 或 push。
