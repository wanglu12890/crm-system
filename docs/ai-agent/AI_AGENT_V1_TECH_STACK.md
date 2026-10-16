# CRM AI Agent V1 / V1.1 技术栈与架构基线

更新：2026-10-16。状态：V1已完成；V1.1三阶段开发及真实联调验收完成，结论PASS；V2尚未开发。验收证据与覆盖限制见[版本记录](AI_AGENT_V1_1_CHANGELOG.md)。

## 1. 技术栈与边界

| 组件 | 当前实现 |
|---|---|
| Java / Spring Boot | Java17 / Boot3.5.16 |
| Spring AI | 1.1.8 BOM及OpenAI模型Starter |
| 模型 | 百炼OpenAI兼容API，qwen-plus，temperature=0.2 |
| 认证与授权 | Spring Security、JWT、方法级双权限 |
| 数据查询 | MyBatis-Plus3.5.7、现有CustomerMapper、MySQL8 |
| 测试 | JUnit5、Mockito、AssertJ、MockMvc |
| 前端 | Vue3/TypeScript，AI页面已接入正式接口，双权限菜单/路由 |

沿用Java单体，不引入LangChain4j、Python服务、通用Agent框架、RAG、向量库或多Agent。V1/V1.1仅提供客户总量、A/B/C/D/未评级分布和四种创建时间窗口；无任意SQL、业务写操作、自定义日期、明细、公海统计、指定其他人员/部门、合同/商机/销售额。V1.1新增受业务身份约束的经理DEPT→SELF，不支持任意范围降级。

## 2. 实际调用链

```text
POST /api/ai/analysis
  -> JWT恢复SecurityUser、Controller校验question和双权限
  -> AiAnalysisService：Qwen结构化分类（无Tool）
  -> Java协议/枚举校验
  -> CustomerAnalysisScopePolicy.resolve(authenticatedUserId, requestedScope)
     内部DataScopeResolver解析authorizedScope；显式请求读取可信有效角色
  -> 得到预期effectiveScope，不合法请求在Tool Calling前422
  -> CustomerStatisticsToolFactory绑定用户及validatedRequestedScope，创建请求独立Tool
  -> Qwen Tool Calling（禁止自动执行）
  -> Java校验唯一工具名、调用次数及timeRange
  -> CustomerStatisticsTool.getCustomerStatistics(timeRange)
  -> CustomerStatisticsService.getStatistics(authenticatedUserId, timeRange, validatedRequestedScope)
  -> CustomerStatisticsServiceImpl：再次调用同一策略复核，计算时间窗口
  -> CustomerMapper.selectCustomerStatistics（现有共享可见性SQL）
  -> Java检查结果范围等于预期effectiveScope、时间等于分类时间，确定性生成回答
  -> AiAnalysisVO直接返回200
```

Controller方法授权为 `hasAuthority('ai:analysis') and hasAuthority('customer:list')`，身份只来自认证principal。模型不接收身份参数，不靠执行线程重新读取SecurityContext恢复用户；Tool在创建时不可变绑定可信用户ID及已校验请求范围。前端可见性不能替代后端授权。

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

完整角色规则只维护在[需求文档第3.2节](AI_AGENT_V1_REQUIREMENTS.md#32-v11-查询范围与业务身份规则主表)。Authorized Scope为最大授权，Requested Scope为请求意图，Effective Scope为实际SQL范围；经理DEPT→SELF后，前置预期及结果均为SELF，不能再与最大DEPT做相等比较。

`service/CustomerAnalysisScopePolicy.java`的`resolve(Long authenticatedUserId, CustomerQueryScopeIntent requestedScope)`自行调用DataScopeResolver和`SysUserMapper.selectRoleCodesByUserId`。最大授权仍按ALL > DEPT > SELF解析，仅customer:list有效授权参与；显式业务资格另查全部有效角色。策略校验单角色及角色/授权矩阵，不信任调用方构造的授权上下文，不通过范围大小自动允许缩小。

CURRENT_AUTHORIZED直接保留解析结果，不施加新单角色限制；显式请求对null、UNSUPPORTED、多有效角色、未知角色及配置不匹配拒绝。DEPT缺部门或无授权仍由原Resolver拒绝。管理员即使有ALL或历史归属记录，也没有显式SELF资格。

前置策略只查权限元数据，不提前统计；S1新入口独立再次验证，角色或授权改变时可在Mapper前拒绝；若实际结果范围不同，AI仍拒绝成功响应。最多一次聚合统计不变。

## 4. 模型与Tool编排

支持且范围匹配时恰好两次模型调用：第一次分类，第二次工具选择；拒绝请求通常只有第一次。不进行模型解释调用，不将聚合结果再次发给模型。第二次仅传已校验的统计意图与时间，不携带原始用户的权限覆盖指令。

复用qwenChatClient，每HTTP请求创建独立CustomerStatisticsTool。Factory保留`create(Long authenticatedUserId)`，委托CURRENT_AUTHORIZED；新增`create(Long authenticatedUserId, CustomerQueryScopeIntent validatedRequestedScope)`。用户与范围通过Java构造参数绑定，Tool不构造SQL权限上下文。模型只能调用getCustomerStatistics，Schema保持只有timeRange；禁止userId、deptId、dataScope、role、requestedScope、effectiveScope、SQL等附加字段。

Spring AI1.1.8调用设置 `ToolCallingChatOptions.internalToolExecutionEnabled(false)`。Java检查单一响应、单一function调用、工具名及严格参数；时间必须与分类一致，然后显式执行Tool，不开启框架自动循环。Tool自身AtomicReference执行状态继续防止重复执行，最多一次S1聚合。多调用返回409，未调用或错误工具协议返回502。

## 5. S1口径、回答与VO

`service/CustomerStatisticsService.java`保留兼容接口，并新增重载：

```java
CustomerStatisticsResult getStatistics(Long authenticatedUserId, StatisticsTimeRange timeRange);
CustomerStatisticsResult getStatistics(Long authenticatedUserId, StatisticsTimeRange timeRange,
                                      CustomerQueryScopeIntent requestedScope);
```

旧入口直接使用DataScopeResolver最大授权，不被新显式角色限制改变。AI Tool调用新入口；实现类调用统一策略，生成实际范围上下文，两个公共入口均声明`@Transactional(readOnly = true)`，共用私有`executeStatistics`，避免依赖同类方法调用触发事务代理。

复用现有CustomerMapper XML的`customerVisibilityConditions`和`selectCustomerStatistics`：SELF按`c.owner_id = dataScope.userId`，DEPT按负责人`owner.dept_id`；共同排除deleted和owner_id为空的公海。V1.1未改Mapper、DataScopeResolver、数据库结构或角色配置，未复制统计SQL。

业务时区Asia/Shanghai，Clock由S1注入；referenceTime在实际统计阶段确定。时间过滤customer.created_at，左闭右开：本月从1日00:00至下月1日，上月从上月1日至本月1日，近30天从referenceTime减30天至referenceTime，ALL不加时间条件。

AiAnalysisVO包含：

- answer：Java字符串，明确“本人范围内”“本部门范围内”或“全公司范围内”；ALL时间称全部时间客户，其他创建窗口称新增客户。
- intent：CustomerAnalysisIntent两种枚举之一。
- metrics：对象，totalCustomers、levelA、levelB、levelC、levelD、unrated均为long。
- analysisContext：dataScope为Effective Scope，timeRange枚举、startTime/endTime/referenceTime为ZonedDateTime、timezone字符串；全部来源于实际S1。ALL时间下startTime/endTime内部为null，Jackson NON_NULL使JSON省略这两个字段。

不暴露用户ID、SQL、内部Prompt、Key、requestId、scopeLabel、toolCalls、charts或conversationId。成功JSON及HTTP错误契约见[需求文档](AI_AGENT_V1_REQUIREMENTS.md)。

## 6. 配置、超时与错误

AI默认关闭，仅ai profile配置模型：DASHSCOPE_API_KEY从环境注入，base-url为 `https://dashscope.aliyuncs.com/compatible-mode`，completions-path为 `/v1/chat/completions`。不在启动阶段调用模型。

本地开发使用已有`scripts/start-local-ai.ps1 -Offline`，在8080启用local,ai并加载未跟踪本地配置；缺Key应停止启动，不把Key写入文档或版本库。ai profile未启用时保留503受控退化，不默认在所有部署启用AI。Vite代理使用VITE_API_PROXY_TARGET（默认localhost:8080）；临时验收端口与清理情况见版本记录。

AI profile的Boot RestClientCustomizer使用JDK HttpClient，连接超时5秒、读取超时20秒；Spring AI retry.max-attempts=1，禁止失败重试。最多两次请求，不无限循环。Customizer对该profile下Boot管理的RestClient Builder生效，未来其他消费者需评估这个超时配置。

| HTTP | code | 原因 |
|---|---|---|
| 400 | AI_REQUEST_INVALID | 请求格式/Validation |
| 400 | AI_TOOL_ARGUMENT_INVALID | 非法Tool参数或时间不一致 |
| 401 | UNAUTHORIZED / AI_AUTHENTICATION_REQUIRED | 安全入口未认证 / 可信主体无效 |
| 403 | FORBIDDEN / DATA_SCOPE_CONFIGURATION_ERROR | 双权限缺失 / 有效范围异常 |
| 409 | AI_TOOL_REPEATED | 重复调用 |
| 422 | AI_ANALYSIS_UNSUPPORTED | 不支持意图或请求不符合业务身份/授权范围策略 |
| 502 | AI_MODEL_RESPONSE_INVALID | 分类、调用协议或可信结果上下文不一致 |
| 503 | AI_MODEL_UNAVAILABLE / AI_ANALYSIS_NOT_READY | 模型异常/超时 / 未启用客户端 |
| 500 | AI_STATISTICS_FAILED / AI_ANALYSIS_FAILED | 聚合异常 / 其他服务异常 |

错误为RFC7807 ProblemDetail，仅返回安全说明；模型异常不外露底层详情。AiAnalysisRequest.toString脱敏，AI及HTTP客户端日志保持安全级别，不打印密钥、JWT、完整Prompt或客户敏感数据。requestId、耗时追踪等为未来规划，不描述为当前能力。

## 7. 验证现状与执行方法

常规测试Mock ChatModel，真实ChatClient组装/解析；覆盖范围矩阵、协议白名单、权限拒绝、统计前拒绝、上下文变化、独立Tool及结果隔离。使用S1/DataScopeResolver相关测试保证安全边界，不运行全量回归。

2026-10-16 S3定向回归154项，0失败、0错误；包含策略、原S1兼容、AI结果范围、并发隔离及真实S1配Mock Mapper的复核测试。历史执行命令（本次文档维护未重跑）：

```powershell
mvn -o -f backend/pom.xml '-Dtest=AiAnalysisServiceTest,CustomerStatisticsToolTest,AiAnalysisControllerTest,CustomerAnalysisScopePolicyTest,CustomerStatisticsServiceImplTest,DataScopeResolverTest' package
```

2026-10-14历史记录：收尾修复前已有11场景真实qwen-plus合成数据冒烟验证；旧C1虽未越权，但把全公司需求变成SELF成功回答。当时增加queryScopeIntent并在Tool前严格匹配，单次真实C1返回CUSTOMER_TOTAL / timeRange=ALL / queryScopeIntent=ALL，后端可信SELF，统计前422拒绝；只有1次模型阶段、0次Tool、0次S1。当时未重复运行全部11个真实场景；V1.1现已使用统一范围策略。

真实测试需要显式开关及已有环境Key，统计服务和范围解析均使用Mock，不连接数据库：

```powershell
$env:RUN_QWEN_ANALYSIS_INTEGRATION = 'true'
mvn -o -f backend/pom.xml '-Dtest=QwenAnalysisLiveIntegrationTest#shouldRejectC1BeforeStatistics' '-Dlogging.level.root=ERROR' '-Dlogging.level.org.springframework.ai=OFF' '-Dlogging.level.org.springframework.web=OFF' test
```

这里只是单次真实结果，不能证明所有同义表达都稳定正确。后续更多语言表达仍需定向验证；模型误分类为CURRENT_AUTHORIZED仍有语义风险，Java可信权限保护不能代替分类准确性验收。无网络或无Key时报告未执行，不拿Mock结果代替真实通过。

## 8. S3实现、阶段性验收与相关文档

S3-A已按确认原型接入：后台Layout、左侧新建/临时会话/空态、右侧欢迎或消息、底部输入。使用共享Axios `/ai/analysis`，仅该接口60秒超时；直接读取VO，双权限限制正式和旧路由；记录只在页面内存保留，不发送历史、身份或范围参数。前端显示实际dataScope、时间、后端answer，等级条仅按原始数量计算可视比例，支持复制、错误草稿保留和零值成功。SSE、图表库、历史持久化、多轮记忆仍未实现。

S3-B真实环境验收记录：9项成功统计全部与独立MySQL分组SQL一致，覆盖ALL/DEPT/SELF及全部时间、本月、近30天和上月；15项Edge浏览器核心检查通过，包含实际复制、会话切换、窄屏、零值和401/422/503。成功请求约1.8–2.3秒。前端构建、5项离线前端测试、63项后端Service/Controller定向测试通过；没有把合成模型冒烟代替真实数据库验收。

上述为2026-10-14历史阶段记录，不是当前待办。V1及V1.1现已完成；2026-10-16 S3实测两个管理员测试账号均为单有效角色、ALL，经理DEPT和销售SELF正确。22个真实API场景、10次浏览器AI请求及15项界面检查通过，所有成功响应与独立SQL一致；前端5项离线测试通过。详细结果和证据来源见[版本记录](AI_AGENT_V1_1_CHANGELOG.md)。

验收未涵盖所有自然语言、真实缺权限账号、真实模型故障注入、剪贴板拒绝及缺少真实样本的过滤边界；相应Mock验证不冒充现场验证。通用旧错误提示和前端时间字段类型差异仍是源码遗留，本次仅记录，不修改代码。V2仅准备需求分析，不预设风险分析设计。

- [需求与API契约](AI_AGENT_V1_REQUIREMENTS.md)
- [合成数据设计](AI_AGENT_V1_DATA_DESIGN.md)
- [客户业务规则](../customer-management/CUSTOMER_V1_RULES.md)
- [后端规范](../development/BACKEND_CONVENTIONS.md)

代码与自动化测试优先于文档；V1.1以外的范围变化须另行确认，不得绕过统一策略或S1复核。
