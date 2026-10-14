# CRM AI Agent V1 技术栈与架构基线

更新：2026-10-14。状态：S1/S2后端已实现，查询范围语义修复及单次C1真实回归通过；S3前端与端到端验收待开发。

## 1. 技术栈与边界

| 组件 | 当前实现 |
|---|---|
| Java / Spring Boot | Java17 / Boot3.5.16 |
| Spring AI | 1.1.8 BOM及OpenAI模型Starter |
| 模型 | 百炼OpenAI兼容API，qwen-plus，temperature=0.2 |
| 认证与授权 | Spring Security、JWT、方法级双权限 |
| 数据查询 | MyBatis-Plus3.5.7、现有CustomerMapper、MySQL8 |
| 测试 | JUnit5、Mockito、AssertJ、MockMvc |
| 前端 | 现有Vue3/TypeScript，AI页面为S3规划 |

沿用Java单体，不引入LangChain4j、Python服务、通用Agent框架、RAG、向量库或多Agent。V1仅提供客户总量、A/B/C/D/未评级分布和四种创建时间窗口；无任意SQL、业务写操作、自定义日期、明细、公海统计、其他人员/部门、合同/商机/销售额或主动缩小组织范围。

## 2. 实际调用链

```text
POST /api/ai/analysis
  -> JWT恢复SecurityUser、Controller校验question和双权限
  -> AiAnalysisService：Qwen结构化分类（无Tool）
  -> Java协议/枚举校验
  -> DataScopeResolver.resolve(authenticatedUserId, "customer:list")
  -> 请求组织范围与可信有效范围匹配，不满足即422
  -> CustomerStatisticsToolFactory创建请求独立Tool
  -> Qwen Tool Calling（禁止自动执行）
  -> Java校验唯一工具名、调用次数及timeRange
  -> CustomerStatisticsTool.getCustomerStatistics(timeRange)
  -> CustomerStatisticsServiceImpl：再次解析可信权限、计算时间窗口
  -> CustomerMapper.selectCustomerStatistics（现有共享可见性SQL）
  -> Java校验返回上下文、确定性生成回答
  -> AiAnalysisVO直接返回200
```

Controller方法授权为 `hasAuthority('ai:analysis') and hasAuthority('customer:list')`，身份只来自认证principal。模型不接收身份参数，不靠执行线程重新读取SecurityContext恢复用户；Tool在创建时不可变绑定可信用户ID。前端可见性不能替代后端授权。

## 3. 结构化分类与范围匹配

第一次模型响应严格为JSON对象：

```json
{
  "intent": "CUSTOMER_TOTAL",
  "timeRange": "ALL",
  "queryScopeIntent": "ALL"
}
```

| 字段 | 允许值 | 含义 |
|---|---|---|
| intent | CUSTOMER_TOTAL、CUSTOMER_GRADE_DISTRIBUTION、UNSUPPORTED | 统计需求，UNSUPPORTED为拒绝标记 |
| timeRange | ALL、CURRENT_MONTH、LAST_MONTH、RECENT_30_DAYS | 创建时间窗口；未指定时间默认ALL |
| queryScopeIntent | CURRENT_AUTHORIZED、SELF、DEPT、ALL、UNSUPPORTED | 用户要求的组织范围，绝非实际授权 |

字段必须恰好三个且均为字符串，拒绝额外字段、非法枚举、重复JSON键和尾随内容。不从模型自然语言文本反向解析统计数字。

queryScopeIntent=ALL表示全公司；timeRange=ALL只表示全部时间。本人/我的客户识别为SELF，本部门/我们部门为DEPT，当前可查看或未指定组织范围为CURRENT_AUTHORIZED，其他指定人员/部门为UNSUPPORTED。自称管理员不能改变可信权限，仍须识别文本中要求的全公司范围。

| 请求范围 | 实际有效范围 | 处理 |
|---|---|---|
| CURRENT_AUTHORIZED | ALL / DEPT / SELF | 使用实际范围 |
| SELF | SELF | 允许 |
| DEPT | DEPT | 允许 |
| ALL | ALL | 允许 |
| SELF / DEPT / ALL | 与请求不一致 | 422拒绝 |
| UNSUPPORTED | 任意 | 422拒绝 |

范围由现有DataScopeResolver读取真实有效角色/权限关系，优先级ALL > DEPT > SELF，仅授予customer:list的有效角色参与；DEPT缺部门或无可用范围拒绝。没有硬编码角色或根据模型dataScope授权。前置解析只查权限元数据，不提前调用S1统计；S1仍按既有规则强制过滤。若两次解析期间范围变化导致S1返回范围不同，拒绝成功响应，当前最多一次聚合统计不变。

ALL用户要求本人或本部门也拒绝，因为S1没有主动缩小组织范围能力。不能把范围优先级当作查询范围选择器。

## 4. 模型与Tool编排

支持且范围匹配时恰好两次模型调用：第一次分类，第二次工具选择；拒绝请求通常只有第一次。不进行模型解释调用，不将聚合结果再次发给模型。第二次仅传已校验的统计意图与时间，不携带原始用户的权限覆盖指令。

复用qwenChatClient，每HTTP请求创建独立CustomerStatisticsTool。模型只能调用getCustomerStatistics，Schema保持只有timeRange；禁止userId、deptId、dataScope、role、SQL等附加字段。

Spring AI1.1.8调用设置 `ToolCallingChatOptions.internalToolExecutionEnabled(false)`。Java检查单一响应、单一function调用、工具名及严格参数；时间必须与分类一致，然后显式执行Tool，不开启框架自动循环。Tool自身AtomicReference执行状态继续防止重复执行，最多一次S1聚合。多调用返回409，未调用或错误工具协议返回502。

## 5. S1口径、回答与VO

统计调用唯一来源为 `CustomerStatisticsService.getStatistics(authenticatedUserId, timeRange)`。复用现有可见性SQL，排除deleted和owner_id为空的公海；不修改统计口径、角色优先级或RBAC矩阵。

业务时区Asia/Shanghai，Clock由S1注入；referenceTime在实际统计阶段确定。时间过滤customer.created_at，左闭右开：本月从1日00:00至下月1日，上月从上月1日至本月1日，近30天从referenceTime减30天至referenceTime，ALL不加时间条件。

AiAnalysisVO包含：

- answer：Java字符串，明确“本人范围内”“本部门范围内”或“全公司范围内”；ALL时间称全部时间客户，其他创建窗口称新增客户。
- intent：CustomerAnalysisIntent两种枚举之一。
- metrics：对象，totalCustomers、levelA、levelB、levelC、levelD、unrated均为long。
- analysisContext：dataScope字符串、timeRange枚举、startTime/endTime/referenceTime为ZonedDateTime、timezone字符串；全部来源于实际S1。ALL时间下startTime/endTime为null。

不暴露用户ID、SQL、内部Prompt、Key、requestId、scopeLabel、toolCalls、charts或conversationId。成功JSON及HTTP错误契约见[需求文档](AI_AGENT_V1_REQUIREMENTS.md)。

## 6. 配置、超时与错误

AI默认关闭，仅ai profile配置模型：DASHSCOPE_API_KEY从环境注入，base-url为 `https://dashscope.aliyuncs.com/compatible-mode`，completions-path为 `/v1/chat/completions`。不在启动阶段调用模型。

AI profile的Boot RestClientCustomizer使用JDK HttpClient，连接超时5秒、读取超时20秒；Spring AI retry.max-attempts=1，禁止失败重试。最多两次请求，不无限循环。Customizer对该profile下Boot管理的RestClient Builder生效，未来其他消费者需评估这个超时配置。

| HTTP | code | 原因 |
|---|---|---|
| 400 | AI_REQUEST_INVALID | 请求格式/Validation |
| 400 | AI_TOOL_ARGUMENT_INVALID | 非法Tool参数或时间不一致 |
| 401 | UNAUTHORIZED / AI_AUTHENTICATION_REQUIRED | 安全入口未认证 / 可信主体无效 |
| 403 | FORBIDDEN / DATA_SCOPE_CONFIGURATION_ERROR | 双权限缺失 / 有效范围异常 |
| 409 | AI_TOOL_REPEATED | 重复调用 |
| 422 | AI_ANALYSIS_UNSUPPORTED | 不支持意图或范围无法精确匹配 |
| 502 | AI_MODEL_RESPONSE_INVALID | 分类、调用协议或可信结果上下文不一致 |
| 503 | AI_MODEL_UNAVAILABLE / AI_ANALYSIS_NOT_READY | 模型异常/超时 / 未启用客户端 |
| 500 | AI_STATISTICS_FAILED / AI_ANALYSIS_FAILED | 聚合异常 / 其他服务异常 |

错误为RFC7807 ProblemDetail，仅返回安全说明；模型异常不外露底层详情。AiAnalysisRequest.toString脱敏，AI及HTTP客户端日志保持安全级别，不打印密钥、JWT、完整Prompt或客户敏感数据。requestId、耗时追踪等为未来规划，不描述为当前能力。

## 7. 验证现状与执行方法

常规测试Mock ChatModel，真实ChatClient组装/解析；覆盖范围矩阵、协议白名单、权限拒绝、统计前拒绝、上下文变化、独立Tool及结果隔离。使用S1/DataScopeResolver相关测试保证安全边界，不运行全量回归。

本次定向Mock及直接相关回归共92项，0失败、0错误；包含并发用户请求隔离。执行命令：

```powershell
mvn -o -f backend/pom.xml '-Dtest=AiAnalysisServiceTest,AiAnalysisControllerTest,CustomerStatisticsToolTest,CustomerStatisticsServiceImplTest,DataScopeResolverTest' test
```

2026-10-14收尾修复前已有11场景真实qwen-plus合成数据冒烟验证；旧C1虽未越权，但把全公司需求变成SELF成功回答。本次增加queryScopeIntent并在Tool前严格匹配，单次真实C1返回CUSTOMER_TOTAL / timeRange=ALL / queryScopeIntent=ALL，后端可信SELF，统计前422拒绝；只有1次模型阶段、0次Tool、0次S1。未重复运行全部11个真实场景。

真实测试需要显式开关及已有环境Key，统计服务和范围解析均使用Mock，不连接数据库：

```powershell
$env:RUN_QWEN_ANALYSIS_INTEGRATION = 'true'
mvn -o -f backend/pom.xml '-Dtest=QwenAnalysisLiveIntegrationTest#shouldRejectC1BeforeStatistics' '-Dlogging.level.root=ERROR' '-Dlogging.level.org.springframework.ai=OFF' '-Dlogging.level.org.springframework.web=OFF' test
```

这里只是单次真实结果，不能证明所有同义表达都稳定正确。后续更多语言表达仍需定向验证；模型误分类为CURRENT_AUTHORIZED仍有语义风险，Java可信权限保护不能代替分类准确性验收。无网络或无Key时报告未执行，不拿Mock结果代替真实通过。

## 8. S3规划与相关文档

S3只按已确认UI原型接入：后台Layout、左侧新建/最近分析空态、右侧欢迎/消息、底部输入；快捷问题需与用户有效范围匹配，优先“当前可查看范围”。前端显示真实dataScope和时间，处理422/502/503、401/403及零值状态。AI前端、SSE、图表、历史持久化、多轮记忆均未在本次实现。

- [需求与API契约](AI_AGENT_V1_REQUIREMENTS.md)
- [合成数据设计](AI_AGENT_V1_DATA_DESIGN.md)
- [客户业务规则](../customer-management/CUSTOMER_V1_RULES.md)
- [后端规范](../development/BACKEND_CONVENTIONS.md)

代码与自动化测试优先于文档；后续范围缩小能力须单独设计，不修改当前S1来绕过语义校验。
