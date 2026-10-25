# CRM AI V2 S2-B2 实施报告

日期：2026-10-24。范围：统一聊天风险 Tool 与确定性查询闭环；不实施 B3 建议生成或前端页面。

## 1. 基线与实际文件

执行前已运行 `git status --short`、`git diff --check`，阅读任务、后端规范、PRD、D1、S1/S2-A/B1 报告及当前实现。工作区原有 B1 未提交修改，全部保留；以下列出本阶段相对 B1 的变化，不把原 B1 文件重复计为新增成果。

生产代码路径相对 `backend/src/main/java/com/company/crm/`：

| 变化 | 文件与职责 |
| --- | --- |
| 新增 | `service/AiRiskAnalysisService.java`、`service/impl/AiRiskAnalysisServiceImpl.java`：风险专属授权、规范查询、受控模型工具选择、手动执行和结果一致性校验 |
| 新增 | `ai/tool/CustomerRiskTool.java`、`ai/tool/CustomerRiskToolFactory.java`：绑定用户、查询、预期上下文；请求级单次执行 |
| 新增 | `service/CustomerRiskResponseAssembler.java`：复用原 S2-A VO、事实说明、分页、指纹和建议状态映射 |
| 修改 | `service/impl/CustomerRiskAnalysisServiceImpl.java`：保留原查询入口，改用共享组装器 |
| 修改 | `service/CustomerRiskService.java`、`service/impl/CustomerRiskServiceImpl.java`：新增带预期上下文的兼容重载，在 Mapper 前重新授权并比较 |
| 修改 | `service/CustomerRiskAccessPolicy.java`：按字段比较用户、角色、部门、最大/请求/有效范围；不依赖 DataScopeContext 对象引用相等 |
| 修改 | `dto/ai/AiAnalysisRequest.java`：兼容保存显式交互字段，V1 忽略扩展字段，风险分支严格验证白名单 |
| 修改 | `dto/ai/internal/AiAnalysisPlan.java`、`service/AiIntentRouter.java`：风险语义候选增加模式与客户等级条件，不增加分类轮次 |
| 修改 | `controller/AiAnalysisController.java`、`service/AiAnalysisService.java`、`service/impl/AiAnalysisServiceImpl.java`：传递交互参数，开关开启后分发风险服务 |
| 修改 | `exception/CustomerRiskException.java`、`exception/CustomerRiskExceptionHandler.java`：新增上下文变化、查询不支持和客户选择缺失语义，原 code/status 不变 |

资源新增：`backend/src/main/resources/prompts/customer-risk-tool.st`。

测试新增：`controller/AiRiskAnalysisControllerTest.java`、`ai/tool/CustomerRiskToolTest.java`、`service/AiRiskIntentRouterTest.java`（均相对 `backend/src/test/java/com/company/crm/`）。适配原 `AiAnalysisControllerTest`、`AiAnalysisRoutingControllerTest`、`AiCustomerRiskControllerTest`、`AiAnalysisServiceTest`、`QwenAnalysisLiveIntegrationTest` 的方法签名及 Spring 装配。Live 测试只参与编译，未实际调用。

最终目录沿用 `controller/`、`service/impl/`、`dto/ai/internal/`、`dto/risk/`、`ai/tool/`、`vo/ai/` 和 `resources/prompts/`；未创建 `ai/tool/risk/` 或新的自然语言 Controller。

## 2. 调用链与阶段边界

`POST /api/ai/analysis` → 原 Controller 公共双权限 → 统一分类 → 门面分派：

- STATISTICS：原独立统计编排，保持成功两次模型调用与原 JSON。
- RISK 且开关关闭：保持 B1 的 422 / AI_RISK_CHAT_DISABLED，无风险授权读取、工具或核心查询。
- RISK 且开关开启：AiRiskAnalysisService → AccessPolicy → 规范化 CustomerRiskRequest/Query → 请求级 Tool Factory → 一次风险 Tool Calling → Java 严格校验 → Tool → CustomerRiskService → 一次授权聚合 → 共享 Assembler → RiskAnalysisVO。
- UNSUPPORTED：保留 B1 拒绝，不回退统计。

V2 正常成功只有统一分类和风险工具选择两次模型调用；不重新分类，不把工具结果交给模型续写，不生成建议。模型等待在数据库事务之外。

## 3. 候选、查询计划和交互参数

风险分类协议严格为五个字段：

```json
{"businessType":"RISK","riskIntent":"LIST","queryScopeIntent":"CURRENT_AUTHORIZED","queryMode":"PRIORITY","customerLevels":null}
```

子意图 LIST/REASON/SUGGESTIONS 沿用 B1；模式为 PRIORITY/ALL_STATES；客户等级仅 A/B/C/D/UNRATED。null 表示不限等级，空数组、重复值、未知等级、非法类型、额外字段、重复 JSON 键和尾随内容均拒绝。V1 分类协议未修改。旧三字段风险模型响应不再视为完整 B2 计划。

模型候选不是授权计划。Java 先通过 V2 专属准入，再结合 HTTP 显式参数构造不可变 CustomerRiskQuery；复用 CustomerRiskRequest 携带 action 和可选指纹，不另造重复查询 DTO。

HTTP 保持 question 必填，风险分支扩展允许 customerId、page、size 和仅 SUGGESTIONS 使用的 expectedResultFingerprint。未知或可信控制字段（userId、role、deptId、effectiveScope 等）在风险分支拒绝；V1 继续忽略额外字段。重复交互字段及尾随内容被标记，在风险分支拒绝，未开启全局未知字段拒绝。

- page 默认 1，size 默认 20、最大 50；不接受字符串、浮点、null 或越界值，不静默裁剪。
- LIST/SUGGESTIONS 禁止 customerId。
- REASON 仅接受明确交互字段 customerId，必须为正十进制字符串、无前导零且不溢出 Long；不从名称或模型输出猜 ID。缺 ID 不查询，返回 422 / RISK_CUSTOMER_SELECTION_REQUIRED。明确文本 `customerId / 客户ID / 客户编号 / ID` 与交互 ID 冲突也拒绝。
- REASON 不接受清单分页、模式、等级或指纹条件；按 S2-A 默认内部模式构造查询，不过滤低关注或特殊状态原因。
- 不支持自定义天数、仅某一风险等级或特殊状态的单独筛选；提示词要求明确拒绝，不能将高/中关注 PRIORITY 冒充“仅高关注”。S2-A 没有这些独立筛选参数，本阶段不扩充冻结合同。客户等级 A/B 筛选与风险等级 HIGH/MEDIUM 严格区分。

## 4. 授权及查询前一致性

复用原 AccessPolicy 对当前有效用户、单销售角色、三项数据库权限和最大数据范围的验证。经理默认 DEPT，可显式 SELF；销售仅 SELF；管理员、多角色或配置不明拒绝。Controller 仍只要求公共双权限，风险服务单独要求 follow:list。

创建 Tool 前先 resolveContext。Factory 将可信用户、不可变查询和预期上下文绑定到新实例。Tool 执行时调用核心 Service 的三参数重载，进入原 REQUIRES_NEW / REPEATABLE_READ / readOnly 短事务，重新 resolveContext，再比较：用户 ID、销售角色、部门、authorizedScope、requestedScope 及 effectiveScope 的类型/用户/部门。

任何变化或重新授权失败均以 403 / RISK_ACCESS_CONTEXT_CHANGED 中止，比较发生于 selectCandidates 之前，不静默采用新范围。当前三项权限每次重新验证，不依靠旧 SecurityUser、自然语言或旧指纹授予权限。原两参数核心方法继续服务 S2-A，不要求额外模型上下文。

未授权请求不注册 Tool、不执行第二轮模型工具选择、不访问风险 Mapper；上下文变化可发生于模型等待期间，测试通过该时机改变权限/角色/部门/身份/范围验证 Mapper 零调用。实际数据库事务并发效果仍待真实 MySQL 验收。

## 5. Tool、Prompt 与查询次数

唯一允许工具名为 getCustomerRiskAnalysis，模型参数严格为空 JSON 对象 `{}`。用户、范围、客户 ID、分页、阈值、规则版本、SQL 均不能通过工具参数传入。Java 在执行前验证单个 function 调用、精确名称和严格空对象；多调用、错误工具、非 JSON、重复键、尾随内容、非空字段、仅自然语言响应均返回 502，不执行查询。

Tool 使用 AtomicReference 状态机 NOT_EXECUTED → RUNNING → SUCCEEDED/FAILED。并发只有一次可进入核心查询；第一次失败也不能重试。Factory 每请求创建新对象，不跨用户或请求共享可变状态。直接重复执行沿用既有工具重复异常 409 语义。

风险工具只在当前请求 `.tools(tool)` 注册，保留 internalToolExecutionEnabled(false)，不修改全局 ChatClient。Prompt 从专用资源加载，通过原始 SystemMessage 传入，绕开模板渲染，因此字面量 `{}` 不会被 PromptTemplate 误解。真实 Spring AI 1.1.8 测试检查模型可见 schema 无参数、无自动执行、首轮无工具及实际加载的 Prompt 文本。

每个正常风险 HTTP 请求只有一次核心聚合查询。组装器只处理已查询的 CustomerRiskPage，不依赖 Mapper 或 CustomerRiskService。必要授权元数据读取与核心聚合次数分开统计。测试明确断言 LIST、REASON、SUGGESTIONS 各一次、无权/非法模型协议/缺 ID 零次。

## 6. LIST / REASON / SUGGESTIONS 与 S2-A 兼容

LIST 复用 S1 默认高/中关注、ALL_STATES 特殊状态、客户等级过滤、全局排序计数后分页、固定分析时间与规则快照。没有新阈值或等级算法。

REASON 通过授权 Mapper 的单客户 ID 条件读取；不可见、不存在、不适用统一 404 / RISK_CUSTOMER_NOT_FOUND。成功原因全部来自 Java，低关注、观察中或数据待核验不被默认 PRIORITY 排除。

SUGGESTIONS 完成同样确定性查询，非空页为 PARTIAL / advice.UNAVAILABLE / RISK_ADVICE_NOT_READY；空页 SUCCESS / NOT_APPLICABLE；无第三次模型调用和虚构建议。可选旧指纹不一致仍返回 409 / RISK_RESULT_STALE。

原 S2-A 映射与 fingerprint 方法在提取前后逐段比较完全相同；算法保留 risk-fingerprint-v1 固定元组、SHA-256、排序归一化及排除 T 本身的规则。测试对同一固定 Clock/权限/数据分别请求统一聊天和结构化接口，断言整个 JSON（包含指纹、null、时区、分页和建议状态）完全相同。原 S2-A 42 项合同测试继续运行。

## 7. 异常、日志及开关

复用 B1 共享 Advice，不新增第二个绑定聊天 Controller 的 Advice。原 S2-A 错误码和映射保持不变；新增 403 / RISK_ACCESS_CONTEXT_CHANGED、422 / RISK_QUERY_UNSUPPORTED、422 / RISK_CUSTOMER_SELECTION_REQUIRED。非法模型工具调用 502 / AI_MODEL_RESPONSE_INVALID；工具阶段模型失败 503 / AI_MODEL_UNAVAILABLE；不将 CustomerRiskException 包成普通 500。

风险入口和耗时使用安全日志；模型阶段只记异常类型，业务拒绝由共享 Advice 记录受控枚举，避免同一错误重复日志。不记录问题、Prompt、客户明细、联系方式、跟进正文或认证凭据。

crm.ai.risk.chat.enabled 仍默认 false，原环境变量 CRM_AI_RISK_CHAT_ENABLED 保留。关闭保持 B1 行为；开启才允许 B2 执行。不改变本地配置或自动对普通用户开放；不影响 V1 或 S2-A。

## 8. 实际验证记录

实际执行并通过：

```powershell
mvn -o -q -f backend/pom.xml '-Dtest=AiRiskAnalysisControllerTest,CustomerRiskToolTest,AiAnalysisServiceTest,AiAnalysisControllerTest,AiAnalysisRoutingControllerTest,AiCustomerRiskControllerTest,CustomerRiskServiceTest' test
mvn -o -q -f backend/pom.xml '-Dtest=AiRiskIntentRouterTest,AiRiskAnalysisControllerTest,CustomerRiskToolTest' test
mvn -o -q -f backend/pom.xml '-Dtest=*,!QwenLiveIntegrationTest,!QwenAnalysisLiveIntegrationTest,!CustomerRiskMapperMySqlTest' package
```

最终完整离线套件 **611 项，0 failures、0 errors、0 skipped**，完成生产编译、全部测试源码编译及 Spring Boot 打包，生成 `backend/target/crm-backend-0.1.0-SNAPSHOT.jar`。外部集成类被显式排除，不计入这 611 项或其跳过数。

| 关键测试 | 项数 | 最终结果 |
| --- | ---: | --- |
| AiRiskAnalysisControllerTest | 60 | 通过：真实统一风险调用链、两次模型、权限/角色/上下文变化、严格 Tool、JSON/指纹对等、查询次数、原因与建议状态 |
| CustomerRiskToolTest | 4 | 通过：单次、失败不可重试、并发与请求隔离 |
| AiRiskIntentRouterTest | 12 | 通过：扩展候选协议、非法等级、伪造对象 ID 与重复键 |
| AiAnalysisServiceTest | 83 | 通过：V1 统计及权限/协议回归 |
| AiAnalysisControllerTest | 11 | 通过：V1 HTTP/JSON 与共享风险异常全部映射 |
| AiAnalysisRoutingControllerTest | 38 | 通过：B1 路由、关闭隔离、不支持/混合拒绝及 V1 两次模型 |
| AiCustomerRiskControllerTest | 42 | 通过：原 S2-A 合同与安全行为 |
| CustomerRiskAccessPolicyTest / EvaluatorTest / ServiceTest | 21 / 17 / 8 | 通过：S1 准入、判级与核心查询 |
| CustomerRiskMapperTest / PropertiesTest | 6 / 7 | 通过：SQL 结构、规则与配置 |
| CustomerStatisticsToolTest | 11 | 通过：原 V1 工具 |

其余原有后端离线测试均通过。测试日志保存在系统临时目录 `crm-b2-focused.log`、`crm-b2-routing.log`、`crm-b2-full.log`，未加入版本管理。

最终 `git diff --check`、新增文件尾随空白、文档所列关键路径和共享映射提取对比检查通过。Git 的 LF/CRLF 提示属于现有 Windows 换行策略，不是格式检查失败。

真实 Qwen 与 MySQL 集成测试未执行，不以 Mock 或 SQL 结构测试代替真实联调。前端未修改页面，现有离线测试 5 项通过；首次因沙箱禁止 Vite 更新缓存报 EPERM，提升执行权限后使用原命令通过。

调试过程真实记录：接口重载后测试 any() 匹配不明确及测试方法异常声明导致首轮编译失败，已修正；风险分类测试使用项目 NON_NULL Mapper 时漏掉必需 null 字段导致 44 项预期外 502，改为准确构造模型协议后定向回归通过；补充的 Router 测试在 thenReturn 内嵌套创建 ChatClient 触发 Mockito 未完成桩异常，已将客户端创建移到桩设置前，再行验证。未将这些失败轮次标记通过。

## 9. 已知限制、验收与 B3

自然语言分类准确率尚未通过真实 Qwen 验证，Prompt 不能替代结构和授权校验。明确客户 ID 的冲突检测只针对可识别 ID 语法；模糊名称无显式选择一律不能执行 REASON，不提供名称到数据库 ID 的自动搜索。未增加风险等级单独筛选或自定义阈值。

没有真实 MySQL 并发、执行计划、5000 候选容量或整体性能验收；跨页仍是 LIVE_REQUEST_SNAPSHOT，不提供历史快照。现有 S1 报告部分中文已损坏、项目背景文档的“V2 尚未开始”已过时，本次以实际源码和测试为准，不扩大修改历史文档。

B3 待办：最小结构化事实投影、受控建议生成与严格输出校验、模型前后权限和页内对象批量复核、模型预算/限流及建议失败降级。当前不发送客户事实给建议模型、不生成建议。后续前端风险 VO 渲染仍属 B4，因此开关继续默认关闭。

本次不修改数据库 Schema、风险规则、S2-A 合同、权限矩阵或前端页面，不执行 commit/push。工作区保留已有 B1 与本次 B2 的未提交文件。完成 B2 后停止，等待人工验收，不自动进入 B3。

| 最终验收问题 | 结论 |
| --- | --- |
| B2 是否满足验收条件 | 代码和离线自动化验收满足；真实 Qwen/MySQL、容量及性能验收另行执行 |
| V1 成功 JSON 是否不变 | 是 |
| V1 是否仍为两次模型调用 | 是，普通成功请求保持两次 |
| S2-A 原合同是否不变 | 是，原请求、响应、指纹、分页、错误和建议状态保留 |
| LIST 和 REASON 是否闭环 | 是，开关开启且授权及输入合法时，由真实核心 Service 执行 |
| SUGGESTIONS 是否仅确定性结果 | 是，无建议模型调用 |
| 未授权请求是否不执行风险查询 | 是，初次准入拒绝和 Tool 前上下文变化均零 Mapper 查询 |
| 单请求是否最多一次核心风险查询 | 是，单次状态机与组装无查询共同保证 |
| 风险聊天是否默认关闭 | 是，未更改默认值或环境配置 |
| 是否可以进入 B3 | 可在人工验收后进入；本次停止，不自动实施 |
