# CRM AI Agent V2-D1：后端架构与接口设计

- 初版：2026-10-18；更新：2026-10-25。状态：S1/S2-A、B1统一入口、B2风险Tool及B3受约束建议已有实现；风险聊天仍默认关闭，B4前端与真实环境验收待实施。
- 业务依据：[V2.0正式PRD](AI_AGENT_V2_REQUIREMENTS.md)、[R1可行性报告](AI_AGENT_V2_FEASIBILITY_REPORT.md)、[D1任务](../../task/1018/2-CRM-AI-Agent-V2-D1.md)。PRD的角色、三权限、状态、阈值、排序和20/50分页已冻结，本设计不重新裁决业务规则。
- 当前实施依据见归档的[B1报告](../historical/AI_AGENT_V2_S2_B1_IMPLEMENTATION_REPORT.md)、[B2报告](../historical/AI_AGENT_V2_S2_B2_IMPLEMENTATION_REPORT.md)、[B3报告](../historical/AI_AGENT_V2_S2_B3_IMPLEMENTATION_REPORT.md)。报告保留各阶段当时的测试、决策和限制；本设计文档描述当前B1/B2/B3实现及B4待办，不把历史阶段的未就绪状态当作当前开启开关后的行为。

## 1. 设计目标与范围

在既有Java单体内增加安全只读的客户关注分析：后端固定时间与规则，按授权集合读取历史，Java判级、筛选、排序、计数后分页，Qwen仅生成当前页的辅助建议。覆盖关注清单、单客户原因、建议及全关注状态；排除历史回溯、计划逾期正式判定、预测概率、业务写入和完整跟进CRUD。

特别区分：`ALL_STATES`是适用客户的全部关注状态，绝非ALL组织范围，也不包含INACTIVE、公海或删除客户。不能把V1按created_at统计的时间窗口用于截断最后跟进历史。

## 2. 现有实现与复用边界

已定向核对项目上下文、V1需求/数据/版本记录、客户规则及后端规范，并读取以下生产代码、表结构和直接相关测试：

| 证据 | 当前事实 | V2决策 |
|---|---|---|
| [AiAnalysisController](../../backend/src/main/java/com/company/crm/controller/AiAnalysisController.java) | POST /api/ai/analysis，双权限，question≤1000，主体ID来自SecurityUser | 保持入口、授权和返回兼容；不加follow:list到V1 |
| [AiAnalysisService](../../backend/src/main/java/com/company/crm/service/AiAnalysisService.java) | B1统一入口契约；Router按业务分支严格分类；独立统计编排继续两阶段模型、手动最多一次Tool | 保留统计执行模式，不把风险意图加入旧统计Tool或统计VO字段 |
| [CustomerStatisticsTool](../../backend/src/main/java/com/company/crm/ai/tool/CustomerStatisticsTool.java)、[Factory](../../backend/src/main/java/com/company/crm/ai/tool/CustomerStatisticsToolFactory.java) | 请求独立、不可变绑定用户/请求范围；Schema仅timeRange；AtomicReference状态 | 复用隔离及次数限制方式；风险Tool另设，不修改旧Schema |
| [CustomerAnalysisScopePolicy](../../backend/src/main/java/com/company/crm/service/CustomerAnalysisScopePolicy.java) | 显式请求读有效角色；允许管理员ALL；CURRENT_AUTHORIZED跳过单角色限制 | 必须先加V2准入，不可直接作为完整门禁 |
| [DataScopeResolver](../../backend/src/main/java/com/company/crm/security/datascope/DataScopeResolver.java) | 按customer:list解析最大范围，ALL>DEPT>SELF，DEPT无部门拒绝 | 复用解析职责，不改V1规则 |
| [CustomerStatisticsService](../../backend/src/main/java/com/company/crm/service/CustomerStatisticsService.java)、[实现](../../backend/src/main/java/com/company/crm/service/impl/CustomerStatisticsServiceImpl.java) | 两参数兼容、三参数再校验范围；公共只读事务、Clock和聚合校验 | 风险另建Service；复用Clock、时间转换与防御思路，不依赖六个统计计数反推明细 |
| [CustomerMapper.xml](../../backend/src/main/resources/mapper/CustomerMapper.xml)、[CustomerMapper](../../backend/src/main/java/com/company/crm/mapper/CustomerMapper.java) | 列表/统计共享普通客户可见性，未排除INACTIVE | 复用共享过滤后再加V2状态条件；不改变原片段语义 |
| [Customer](../../backend/src/main/java/com/company/crm/entity/Customer.java)、[CustomerController](../../backend/src/main/java/com/company/crm/controller/CustomerController.java)及相关DTO/VO | 有建档/负责人，无分配历史；当前列表/创建，不是完整详情服务 | 新只读投影，不输出Entity或假定已有详情授权 |
| [SecurityConfig](../../backend/src/main/java/com/company/crm/config/SecurityConfig.java) | 已启用方法授权，非登录路由默认认证 | 新Controller加三权限；Service仍检查可信当前权限/角色 |
| [AiAnalysisVO](../../backend/src/main/java/com/company/crm/vo/ai/AiAnalysisVO.java)、[Advice](../../backend/src/main/java/com/company/crm/exception/AiAnalysisExceptionHandler.java) | V1直接VO，ProblemDetail含code；聊天Advice作用于统一Controller并显式映射风险异常 | 复用两类VO联合返回，S2-A独立Advice保持作用域及合同 |
| [init.sql](../../database/init.sql) customer / follow_record | follow_at独立于created_at；target_type+target_id多态；无计划完成/分配事件 | 仅最小聚合读取，不新建完整FollowRecord业务模块 |

同时审阅AiAnalysisServiceTest、CustomerAnalysisScopePolicyTest、CustomerStatisticsToolTest及Customer Mapper/Service相关用例；现有测试不能代替新风险明细授权和规则测试。未发现关键PRD冲突。该表记录初版D1的调查依据；当前S1/S2-A已实现，入口和目录以本次修订及当前源码为准。

## 3. 总体架构与调用链

自然语言统一使用 `AiAnalysisController` 的 `POST /ai/analysis`，默认 context-path=/api 后对外为 `/api/ai/analysis`。不创建 AiRiskAnalysisController 或 /api/ai/risk-analysis。保留 S2-A 的 CustomerRiskController（/api/customer-risks/analyses），供结构化操作及后续分页/明确对象交互使用。

```mermaid
flowchart TD
  CHAT[AI聊天窗口] --> ENTRY[AiAnalysisController 公共双权限]
  ENTRY --> APP[AiAnalysisServiceImpl]
  APP --> ROUTER[AiIntentRouter 无Tool分类]
  ROUTER --> STATS[AiStatisticsAnalysisServiceImpl]
  STATS --> ST[统计Tool 唯一注册]
  ST --> SS[CustomerStatisticsService]
  ROUTER --> GATE{RISK}
  GATE -->|开关关闭| CLOSED[受控关闭 不读取风险数据]
  GATE -->|开关开启| RISK[AiRiskAnalysisServiceImpl V2准入]
  RISK --> RT[唯一零参数风险Tool]
  RT --> CORE[CustomerRiskService 独立复核]
  STRUCT[CustomerRiskController 三权限] --> SA[CustomerRiskAnalysisService]
  SA --> CORE
  CORE --> RESULT[可信风险结果]
  RESULT --> ASSEMBLER[CustomerRiskResponseAssembler 确定性VO]
  ASSEMBLER -->|仅自然语言SUGGESTIONS| PRE[前置权限与批量对象复核]
  PRE --> ADVICE[RiskAdviceService 受控动作与Java模板]
  ADVICE --> POST[返回前权限与批量对象复核]
```

| 组件/位置 | 职责与当前阶段 |
|---|---|
| controller/AiAnalysisController | 已有统一自然语言入口，保留双权限；返回共同Java响应接口 |
| service/AiAnalysisService及impl | 统一协调、身份检查、路由与开关处理，传递总预算起点；不包含业务Tool细节 |
| service/AiIntentRouter | B1具体Spring组件；分类、严格协议解析，输出语义候选而非授权计划；无数据库、无Tool |
| service/AiStatisticsAnalysisService及impl | B1迁移V1范围策略、唯一统计Tool校验、执行和VO组装；不重复分类 |
| service/AiRiskAnalysisService及impl | 已实现B2内部风险编排，B3仅在SUGGESTIONS附加建议，保留单次核心查询 |
| service/RiskAdviceService及impl、CustomerRiskAdviceAccessVerifier | B3独立建议协议/预算；建议前后重新授权和当前页批量对象复核 |
| controller/CustomerRiskController、service/CustomerRiskAnalysisService及impl | 保留S2-A结构化入口和应用协调，不经过模型路由 |
| service/CustomerRiskAccessPolicy、CustomerRiskService及impl、CustomerRiskEvaluator | 保留S1可信准入、短只读事务、确定性规则与排序/分页 |
| ai/tool/CustomerStatisticsTool及Factory | 已有V1工具，Schema不变 |
| ai/tool/CustomerRiskTool及Factory | B2已实现；风险开关开启且授权通过后按请求注册，最多一次执行 |
| vo/ai/AiAnalysisResponse | B1轻量共同响应接口，无字段、无Jackson多态元数据 |
| vo/ai/AiAnalysisVO、RiskAnalysisVO及关联VO | 复用现有JSON，按intent判别；无payload外包装 |
| dto/ai、dto/ai/internal、dto/risk | 分别为HTTP请求、内部分类候选、S1内部风险数据 |

不因共享聊天窗口同时注册全部业务Tool。当前B2/B3风险读取及建议事实保护遵循下文安全要求，开关仍默认关闭。

## 4. 关键方案比较与推荐

| 决策 | 方案A | 方案B | 推荐及取舍 |
|---|---|---|---|
| AI入口 | 现有Service内部扩展分类并委托风险Service | 提取轻量路由组件，由统一Service分派独立统计/风险编排 | **采用B**：统一/api/ai/analysis，Controller双权限不变；只新增具体Router，不建设通用Agent框架；结构化风险入口保留 |
| 数据访问 | 在CustomerMapper增加风险查询，复用现有位置 | 专用CustomerRiskMapper，只读投影，引用原共享过滤并加状态条件 | **B**：隔离复杂聚合及测试，不污染列表/统计；跨XML共享片段需显式namespace引用与加载测试，不复制不同口径 |
| 判级位置 | SQL CASE完成判级、排序、计数分页；适合大数据，但规则需与Java解释同步 | SQL只做历史聚合，Java唯一判级/Comparator，全集合计算后分页 | **B**：当前阶段可测试性、版本一致性更好，符合Java确定性要求；必须容量保护，不能无界内存。A留为规模演进，经等价性测试再采用 |
| Tool数量 | 一个通用只读Risk Tool，查询计划完全服务器绑定 | 分清单Tool与原因Tool，每次仅注册对应一个 | **A**：零模型参数，一个执行状态，模式在Java计划里分支；B语义直观但重复包装/状态，初版收益有限 |
| 建议返回 | 与风险同步完成，一次HTTP但等待模型 | 先返回风险，按需独立请求当前页建议 | 当前自然语言SUGGESTIONS同步附加B3建议，LIST/REASON不自动生成；S2-A结构化SUGGESTIONS仍为未就绪。B4按现有接口组织按需交互，不把结构化入口当作已支持AI建议 |

不增加缓存集群、工作流引擎、RAG、多Agent、异步任务表或完整跟进CRUD。建议生成的分阶段返回不等于持久化快照，跨请求仍查询当前状态。

## 5. 角色权限与范围设计

### 5.1 公共权限与分支授权

1. 统一聊天Controller保留 `ai:analysis AND customer:list`；S2-A结构化Controller继续三权限。JWT主体是唯一可信身份来源。
2. 统一Router只接收用户问题，先做无Tool语义分类，不读取客户/跟进数据，不把用户ID、角色、部门或授权上下文交给模型。用户问题自身可能含敏感内容，不宣称无Tool就没有敏感输入；不记录完整问题/Prompt。
3. STATISTICS使用原CustomerAnalysisScopePolicy，保留管理员ALL和默认多角色既有语义。RISK不能因权限不足或未开放降级成统计。
4. **风险开关**：`crm.ai.risk.chat.enabled`默认false，关闭为422/AI_RISK_CHAT_DISABLED，无第二次模型调用或风险数据读取；当前开启后执行B2查询及按需B3建议。B1阶段开启仍返回503/AI_RISK_CHAT_NOT_READY的行为仅作为历史记录保留。开关不影响S2-A。
5. **B2**：风险分支先调用CustomerRiskAccessPolicy，重新读取启用未删除用户、恰好一个有效销售角色和三权限，再解析最大授权/请求范围。通过后才能绑定计划和注册风险Tool。管理员、多角色、未知角色和配置不符拒绝。
6. Tool调用核心Service时在短事务重新复核；预期用户/角色/部门/授权及有效范围必须在Mapper前比较，不一致拒绝。REASON仍在授权SQL内检查对象，建议发送前和返回前批量复核当前准入/页内对象。

CustomerRiskAccessContext只由后端产生；Router输出的AiAnalysisPlan仅是结构合法的语义候选，不能作为授权上下文。经理最大DEPT、允许SELF/DEPT；销售仅SELF。ALL和非法业务范围拒绝；不能把CURRENT_AUTHORIZED当作V2门禁旁路。

### 5.2 Mapper与对象边界

- 所有风险集合：共享客户可见性条件，加`status IN ('POTENTIAL','ACTIVE')`。SELF比较`customer.owner_id=authenticatedUserId`，DEPT比较**客户当前负责人**的dept_id；不按跟进人/创建人过滤历史。
- 共享片段允许V1的ALL，因此风险Mapper外层必须再限定scopeCode仅SELF/DEPT，其他值AND 1=0，不能把共享片段本身当作V2准入；参数化绑定所有值，禁止`${}`拼接SQL。
- 单客户原因在同一个授权客户查询中增加`c.id=:customerId`，不先selectById再把全对象交给模型或Controller。不存在、范围外、INACTIVE、公海、删除客户统一404，不泄露存在性；全局准入先失败仍403。
- 旧会话、客户端ID、页码、指纹均不是授权证据；业务服务自己生成SQL范围，不暴露任意ownerId/deptId、SQL或排序表达式。
- 模型前与返回前在短事务中复核当前准入，以及最多50个结果客户仍属于当前有效集合；变化则403/409，不返回旧结果或悄然删行改变总数。复核为批量ID可见性读取，不重新逐客户统计。
- 授权检查与网络发送之间仍存在不可消除的时间窗口：不持有跨Qwen长事务，也不能撤回授权后曾发送的最小结构化数据。这里保证每个读取/发送边界重检，不承诺请求开始后的全程全局锁定；测试记录此并发边界。

## 6. 安全只读数据访问

### 6.1 最小查询与聚合

推荐专用Mapper返回每个授权客户一行`CustomerRiskAggregate`，只取id/编号/名称/等级/状态/owner必要状态/created_at、totalHistoryCount、validHistoryCount、invalidHistoryCount、lastValidFollowAt及异常分类计数，不取content、备注、联系方式、next_follow_at。

概念SQL流程（不是本阶段交付的SQL文件）：

```text
eligible_customers = 普通客户共享过滤 + POTENTIAL/ACTIVE + 可选等级集合
                     + 可选已校验单客户ID
LEFT JOIN follow_record
  ON target_type='CUSTOMER' AND target_id=eligible.id AND deleted=0
GROUP BY 每个客户必要字段
totalHistoryCount = COUNT(f.id)
validHistoryCount = SUM(f.id非空 AND follow_at非空
                        AND created_at <= follow_at AND follow_at <= T)
lastValidFollowAt = MAX(CASE有效业务时间 THEN follow_at END)
invalidHistoryCount = totalHistoryCount - validHistoryCount
```

LEFT JOIN条件不能移到外层WHERE导致无跟进客户消失；空JOIN行不得算作一条异常记录。不能先按当前页客户取跟进再对全范围声称已排序/计数，也不能直接按follow_record.owner_id限制SELF。孤立/非CUSTOMER记录不进入授权客户JOIN，不能在请求中另查全库孤立明细。

列表一次批量聚合完成，原因查询同样方法但限定一个客户；不新增FollowRecord CRUD或把完整实体列表加载到Java。已有`idx_follow_record_target(target_type,target_id,follow_at)`可评估利用，是否增含deleted的索引留EXPLAIN/规模验证，本阶段不DDL。

### 6.2 时间与数据质量分流

- total=0：NEVER_FOLLOWED，建档年龄判定；历史逻辑删除不计入total。
- valid>0：FOLLOWED，按MAX有效follow_at判级；invalid>0则同时HAS_ANOMALIES，不丢有效结果。
- total>0且valid=0：INVALID_ONLY、DATA_REVIEW，riskLevel=null；不得套建档7/14天判高。
- 未来、NULL或早于建档的follow_at计异常且不参与MAX。时间全部解释为Asia/Shanghai业务DATETIME，绑定由同一个Clock生成的T；不按record.created_at或customer.updated_at替代。
- 客户建档未来/缺失、非法非空等级、失效负责人等阻止可靠判级时，保留授权边界内的数据质量标记，不默认为未评级或低关注；DATA_REVIEW在ALL_STATES可见，默认不混入。用户自身身份/部门配置失败属于拒绝，不是可绕过的质量状态。
- contact_id可空、历史跟进人与当前负责人不同不是时间风险异常；计划字段不读，不把计划缺失/过期计成风险。同follow_at无需窗口函数即可MAX；若后续需要最新记录内容才使用`ROW_NUMBER(...follow_at DESC,id DESC)`，不作为本版必需成本。

### 6.3 聚合、批量与容量取舍

SQL GROUP BY比先读取所有跟进原文再Java聚合更省数据且降低泄露面；窗口函数适合取整条最新记录，但本版只需要MAX/COUNT，建议不引入；按客户逐一查询不可接受。

SQL只聚合、Java判级意味着必须读齐本次授权筛选后的客户聚合集合。S1已实现后端`maxCandidates`容量保护（当前默认5000，容量性能仍待实测，不是业务分页上限）：授权候选CTE最多读取cap+1行并聚合；一旦超过cap，**整次查询受控拒绝**，不对截断集合返回匹配总数/风险页。不能LIMIT 20后再判级。页大小50不能代替总集合保护。独立单客户原因查询不扫描全范围。

单次SQL超时、输出行数/内存预算均受限；历史扫描仍可能很大，必须独立测量和超时控制，不能用随意截断历史改变最近跟进口径。后续扩展SQL判级时需保持Java参考实现的等价性，不能为性能修改业务阈值。

## 7. 风险计算与规则配置

### 7.1 内部输入/输出

| 当前数据结构 | 内容与可信来源 |
|---|---|
| AiAnalysisPlan / CustomerRiskRequest / CustomerRiskQuery | 分别承载模型语义候选、action/可选指纹、Java验证后的operation/mode/scope/客户等级筛选/客户ID/page/size；不能把候选当作授权计划，不接受用户的T、计算风险等级或SQL上下文 |
| CustomerRiskAccessContext / RuleSnapshot | 后端复核的用户、销售角色、部门、授权/请求/有效范围及规则快照；不暴露为可绑定HTTP DTO |
| CustomerRiskAggregate | Mapper最小投影，计数及有效时间必须一致，异常聚合拒绝假零 |
| CustomerRiskResult | 客户最小信息、state、riskLevel、history、时间锚点、elapsedDays、thresholdDays、excessDays、reasons、qualityHints；由Assembler映射到对外RiskRecordVO |
| CustomerRiskPage | 不可变排序后当前页、total、page/size、实际范围、规则版本、分析时间和可信访问上下文；不包含模型文本 |

核心公共方法为`analyze(trustedUserId, CustomerRiskQuery)`及带预期CustomerRiskAccessContext的兼容重载；核心Service通过Clock和CustomerRiskProperties.snapshot()固定T与规则，不允许HTTP/模型注入历史时间。Evaluator仅接收单客户聚合、T、RuleSnapshot。业务入口重新解析范围；Tool路径在Mapper查询前比较编排绑定的预期上下文。

### 7.2 确定性规则与排序

完整规则以PRD第5节为准：有效历史A级14/30，B/C/D/未评级30/60；无历史建档不足7天观察、7天中、14天高。`elapsedDays=floor(Duration(anchor,T)/24h)`，达到即命中。未来锚点走质量状态，不取绝对值或max(0)掩盖异常。

正常state=NORMAL，riskLevel=HIGH/MEDIUM/LOW；特殊state=OBSERVING或DATA_REVIEW且riskLevel=null。FOLLOWED锚点LAST_VALID_FOLLOW；NEVER_FOLLOWED锚点CUSTOMER_CREATED但lastValidFollowAt=null；INVALID_ONLY锚点NONE，间隔/阈值/超期均null。

超期指**超过当前关注等级起点**的完整天数，不是计划逾期。有效历史HIGH/MEDIUM起点取其组阈值，LOW起点0；无历史HIGH/MEDIUM起点14/7；特殊状态无excessDays。正常排序键为(HIGH/MEDIUM/LOW序、A/B/C/D/null序、excessDays降序、BIGINT id数值升序)，不按字符串ID字典序。ALL_STATES先正常排序结果，再OBSERVING，再DATA_REVIEW；两类特殊分区内按客户等级和ID升序，不赋予风险高低。PRIORITY只保留HIGH/MEDIUM。

客户去重后只记一次，风险最高规则获胜，不同真实原因保留；同一时间间隔事实仅一个规范原因，不把“长期未跟进”与“等级滞后”复制为两个独立风险。原因建议编码FOLLOW_GAP、NEVER_FOLLOWED，证据带阈值组/锚点/观测/当前等级起点；低关注与特殊状态用assessmentSummary解释，不伪造风险命中。

### 7.3 后端配置与追溯

CustomerRiskProperties已使用`@ConfigurationProperties`前缀`crm.ai.risk.rules`，实际字段为version、a-medium-days=14/a-high-days=30、other-medium-days=30/other-high-days=60、never-medium-days=7/never-high-days=14及max-candidates=5000。规则快照校验阈值为正、组内起点递增、版本非空及容量边界；不允许模型覆盖。

生成不可变RuleSnapshot；有效`ruleVersion=声明版本:规范配置SHA-256`，快照同时保留规范化阈值参数，不另设configHash字段。散列覆盖规则语义版本及会影响判定/排序的参数；同一声明标签下阈值变化也形成不同有效版本。本版不设计热更新或规则表。

每次核心查询取得同一个RuleSnapshot，判级、原因、超期、排序和计数使用该快照；建议只使用本次可信结果，不重新读取规则判级。配置非法则风险模块返回503/RISK_RULES_NOT_READY，不静默混默认值；采用模块边界校验而非启动时全局校验，S1相关离线测试已覆盖其与V1的隔离。

## 8. API与DTO/VO契约

### 8.1 推荐入口

| 方法/路径（新设计） | 用途 | 模型调用 | 权限 |
|---|---|---|---|
| POST /api/customer-risks/analyses | 结构化LIST、REASON、SUGGESTIONS；S2-A建议保持未就绪 | 全部为0，B3不自动接入建议 | 完全相同三权限+V2角色/范围 |
| POST /api/ai/analysis（统一） | V1统计；开启风险开关后B2查询/B3建议 | V1和风险LIST/REASON各2次；非空风险建议最多3次；关闭风险仅1次分类 | Controller原双权限；风险分支额外三权限/角色/范围 |

Controller内部路径省略/api前缀。保留两个已存在的只读入口，不创建持久化analysis资源。V1成功JSON保持AiAnalysisVO原结构；统一聊天Service/Controller通过AiAnalysisResponse返回实际AiAnalysisVO或RiskAnalysisVO，无外层data/payload包装；风险成功VO要求开关开启及V2授权通过。

### 8.2 客户端输入

| DTO（建议） | 白名单字段 | 规则 |
|---|---|---|
| AiAnalysisRequest | question；风险可选page、size、customerId、expectedResultFingerprint | 保留question非空白≤1000及V1额外字段忽略；风险分支严格白名单，REASON只允许字符串customerId，清单默认1/20，指纹只供SUGGESTIONS使用 |
| CustomerRiskRequest | action、scopeIntent、queryMode、customerLevels、page、size、customerId、expectedResultFingerprint | action=LIST/REASON/SUGGESTIONS；scope默认CURRENT_AUTHORIZED，仅SELF/DEPT显式；queryMode默认PRIORITY或ALL_STATES；等级集合只含A/B/C/D/UNRATED，空数组非法，null或省略表示不限等级 |

操作条件：LIST/SUGGESTIONS禁止customerId；REASON必须customerId且不接受queryMode/customerLevels/page/size/指纹，不因默认PRIORITY而屏蔽低关注或特殊状态原因。SUGGESTIONS指定规范清单条件重新计算当前页，不能上传上一页的客户数组、等级、原因或模型事实。expectedResultFingerprint仅允许SUGGESTIONS，可用于旧页提示；默认空，不作为权限凭据。

先按操作检查显式字段，再只给LIST/SUGGESTIONS应用分页/模式默认值；自然语言请求在分类完成后做同样检查，不把自动补出的1/20误当REASON非法输入，也不静默忽略客户端真传的额外条件。指纹为非空、至多128字符的比较值（可省略），服务器输出规范摘要；不把用户提交的任意字符串解析为快照或授权。

所有ID为正十进制**字符串**，转换时校验≤Long.MAX_VALUE，拒绝JSON数字、科学计数、溢出及前导零；page/size必须整数且page≥1，计算偏移用long并检查溢出，不接受客户端排序表达式。V2局部严格解析未知字段/重复键/错误类型，不全局改变旧DTO的解析行为。userId、role、deptId、effectiveScope、T、ruleVersion、SQL等字段一律禁止。

自然语言REASON只使用HTTP选中ID，不从模型生成客户ID、名字或“上一个客户”查找。缺选中对象返回422要求选择；显式文本与选择对象冲突/目标不明确时拒绝澄清，不能让模型猜另一个客户。自然语言分页由HTTP参数控制，不从文本提取任意offset；后续翻页用结构化入口和返回的normalizedQuery。

### 8.3 服务器输出

统一聊天入口按互斥intent返回现有AiAnalysisVO或RiskAnalysisVO；S2-A仍直接返回RiskAnalysisVO。AiAnalysisResponse只是Java类型，无额外序列化字段。V2 DTO/VO局部采用明确的null字段序列化约定，必填可空字段也输出null；不得依赖当前全局NON_NULL造成前端必填字段缺失，不修改全局配置。

| 结构 | 字段及约束 |
|---|---|
| RiskAnalysisVO | status=SUCCESS/PARTIAL、intent=RISK_LIST/RISK_REASON/RISK_SUGGESTIONS、answer（Java生成）、analysisContext、normalizedQuery、page、records、resultFingerprint、advice |
| analysisContext | authorizedScope、effectiveScope、requestedScope、analysisTime（+08:00 ISO）、timezone=Asia/Shanghai、ruleVersion、consistency=LIVE_REQUEST_SNAPSHOT；不返回操作者/部门ID |
| normalizedQuery | LIST/SUGGESTIONS为scopeIntent/queryMode/customerLevels/page/size；REASON为scopeIntent/customerId；由Java归一化，可作为下次请求语义，不是可信执行上下文 |
| page | number、size、returned、total、pages；total为本次筛选去重后的匹配客户数，returned为当前页数；REASON时page=null |
| records[] | customerId、customerNo、customerName、customerLevel（A/B/C/D/null）、customerStatus、state、riskLevel、historyStatus、lastValidFollowAt、anchorType、elapsedDays、thresholdDays、excessDays、reasons、dataQuality |
| reasons[] | code、message（Java）、evidence{anchorTime,elapsedDays,thresholdDays,levelGroup}；levelGroup=A/OTHER/NEVER，不能冒充另一个独立风险 |
| dataQuality | status=CLEAN/HAS_ANOMALIES、codes[]；不含原始跟进正文或跨客户ID |
| advice | status=NOT_REQUESTED/AVAILABLE/UNAVAILABLE/NOT_APPLICABLE、source=null/QWEN_ACTIONS_JAVA_TEMPLATE、unavailableCode（可空）、message（可空）、items[]；成功项为customerId、actions（动作码字符串数组）、reasonKeys和Java渲染的suggestion |

state=NORMAL才有riskLevel；OBSERVING/DATA_REVIEW为特殊状态，不把它们编码进riskLevel。全局status=PARTIAL仅表示可信分析成功、已请求的建议失败；未请求建议为SUCCESS/NOT_REQUESTED，空清单请求建议为SUCCESS/NOT_APPLICABLE。无匹配时records=[]、total=0、returned=0、pages=0；页码超出末页也返回空records但保留真实total，不伪装匹配总数为0。REASON成功仅一条，未找到则404而不是空清单。

### 8.4 请求示例

自然语言：

```json
{"question":"我个人有哪些客户需要优先跟进？","page":1,"size":20}
```

明确全关注状态的结构化分页：

```json
{"action":"LIST","scopeIntent":"SELF","queryMode":"ALL_STATES","page":1,"size":20}
```

选中客户的原因，不用模型提取ID：

```json
{"action":"REASON","scopeIntent":"SELF","customerId":"2300000000000000001"}
```

通过统一聊天为当前页请求B3建议（queryMode与客户等级仍由问题分类，不由客户端传入可信计划）：

```json
{"question":"给出本人范围内需要重点关注客户的跟进建议","page":1,"size":20,"expectedResultFingerprint":"此前服务器返回的指纹"}
```

S2-A结构化建议请求仍合法，但只返回确定性结果及建议未就绪状态，不调用建议模型：

```json
{"action":"SUGGESTIONS","scopeIntent":"SELF","queryMode":"PRIORITY","page":1,"size":20,"expectedResultFingerprint":"此前服务器返回的指纹"}
```

### 8.5 成功与部分成功示例

下例为**虚构的单客户契约示例，不是R1数据库重算或验收结果**；有效规则版本的64个0是摘要占位。T为10月18日20:30，最后跟进9月16日20:30，完整32天，A级HIGH起点30天，excessDays=2。

```json
{
  "status": "SUCCESS",
  "intent": "RISK_LIST",
  "answer": "本人范围内匹配到1个高/中关注客户，本页返回1个。",
  "analysisContext": {
    "authorizedScope": "DEPT",
    "effectiveScope": "SELF",
    "requestedScope": "SELF",
    "analysisTime": "2026-10-18T20:30:00+08:00",
    "timezone": "Asia/Shanghai",
    "ruleVersion": "V2.0-1:0000000000000000000000000000000000000000000000000000000000000000",
    "consistency": "LIVE_REQUEST_SNAPSHOT"
  },
  "normalizedQuery": {"scopeIntent":"SELF","queryMode":"PRIORITY","customerLevels":null,"page":1,"size":20},
  "page": {"number":1,"size":20,"returned":1,"total":1,"pages":1},
  "records": [{
    "customerId": "2300000000000000001",
    "customerNo": "EXAMPLE-001",
    "customerName": "示例客户甲",
    "customerLevel": "A",
    "customerStatus": "ACTIVE",
    "state": "NORMAL",
    "riskLevel": "HIGH",
    "historyStatus": "FOLLOWED",
    "lastValidFollowAt": "2026-09-16T20:30:00+08:00",
    "anchorType": "LAST_VALID_FOLLOW",
    "elapsedDays": 32,
    "thresholdDays": 30,
    "excessDays": 2,
    "reasons": [{
      "code": "FOLLOW_GAP",
      "message": "A级客户距最近有效跟进32天，已达到高关注30天阈值。",
      "evidence": {"anchorTime":"2026-09-16T20:30:00+08:00","elapsedDays":32,"thresholdDays":30,"levelGroup":"A"}
    }],
    "dataQuality": {"status":"CLEAN","codes":[]}
  }],
  "resultFingerprint": "服务器生成的当前页条件与可信结果摘要",
  "advice": {"status":"NOT_REQUESTED","source":null,"unavailableCode":null,"message":null,"items":[]}
}
```

正常未跟进客户的lastValidFollowAt必须为null，anchorType=CUSTOMER_CREATED；观察中riskLevel/thresholdDays/excessDays为null但elapsedDays是建档年龄。INVALID_ONLY则state=DATA_REVIEW、riskLevel=null、anchorType=NONE、elapsedDays/thresholdDays/excessDays=null，不能写成“高风险无限天”。reasons为空，数据异常通过dataQuality及Java answer表达，不伪造风险原因。

已请求建议但失败时保留上述records/page/context，intent改为RISK_SUGGESTIONS，根status改为PARTIAL，仅建议片段替换为：

```json
{"status":"UNAVAILABLE","source":null,"unavailableCode":"RISK_ADVICE_UNAVAILABLE","message":"建议暂不可用，已保留关注分析结果。","items":[]}
```

正常AVAILABLE片段示例（同一受控页面映射，不含模型自造ID）：

```json
{"status":"AVAILABLE","source":"QWEN_ACTIONS_JAVA_TEMPLATE","unavailableCode":null,"message":"辅助建议仅供参考，请人工核实后决定是否执行。","items":[{"customerId":"2300000000000000001","actions":["VERIFY_CURRENT_NEEDS"],"reasonKeys":["FOLLOW_GAP"],"suggestion":"建议先核实客户当前需求，再由销售人员决定联系安排。"}]}
```

## 9. Spring AI Tool设计

推荐唯一`getCustomerRiskAnalysis`，建议方法无参数，模型可见Schema为`{"type":"object","properties":{},"additionalProperties":false}`，只接受空对象。action、查询模式、客户ID、范围、分页、预期AccessContext由Factory绑定的不可变计划决定；T和RuleSnapshot在Risk Service创建内部执行上下文时一次性取得，均不由模型填写。需要显式参数的备选方案也只能让模型回显已验证模式并要求完全相等，没有额外授权价值，故不推荐。

Factory每请求创建实例，绑定真实用户ID、ValidatedPlan及预期范围。用户/分页/ID等先按第8节验证；S1业务服务再验证，不把Factory当唯一安全边界。Tool自身使用NOT_EXECUTED→RUNNING→SUCCEEDED/FAILED状态，失败也不得再次调用。业务入口获取可信执行上下文，不能接受未核验的外部权限对象；同一个请求最多一个业务Tool、一次风险聚合，权限/对象复核的批量读取不计为第二次风险统计。

Java检查单一function调用、精确工具名、arguments严格空对象且无重复键/尾随内容。模型伪造customerId、userId、deptId、role、scope、page、SQL等均在执行业务前拒绝；B2实际实现对未知/缺失/多个调用和非法参数统一返回502/AI_MODEL_RESPONSE_INVALID，直接重复执行请求级Tool为既有409工具重复语义。手动执行后校验实际范围、客户ID/分页与绑定计划一致，所有记录/原因和建议使用业务执行上下文的同一T与规则版本，不能返回不同范围的成功结果。

Tool返回CustomerRiskPage给Java编排；关闭`internalToolExecutionEnabled`，不把完整Tool结果再次交给模型自由解释。建议阶段只发送新的最小投影。结构化接口直接调用同一Risk Service而不伪造一次模型/Tool请求；其安全验证与自然语言入口等价。

## 10. Qwen意图路由与建议编排

### 10.1 统一无Tool分类与业务分派（B1/B2当前协议）

| businessType | 精确模型字段 | Java语义 |
|---|---|---|
| STATISTICS | businessType、intent、timeRange、queryScopeIntent | intent为CUSTOMER_TOTAL/CUSTOMER_GRADE_DISTRIBUTION；时间为原四枚举；范围为原五枚举 |
| RISK | businessType、riskIntent、queryScopeIntent、queryMode、customerLevels | riskIntent=LIST/REASON/SUGGESTIONS；queryMode=PRIORITY/ALL_STATES；customerLevels为null或非空客户等级数组 |
| UNSUPPORTED | businessType、reason | reason=OUT_OF_SCOPE/MIXED/AMBIGUOUS；不执行任何业务 |

除customerLevels外字段均为字符串，严格拒绝缺失、未知字段、重复JSON键、尾随内容、非法枚举或分支字段混用。不接受用户ID、角色、部门、授权范围、风险等级、阈值、SQL或规则版本。模型失败503、协议错误502、不支持422，均不默认回退统计。统计与风险混合请求必须MIXED，不能只执行其中一部分。

统一分类由AiIntentRouter完成；STATISTICS结果直接交给独立统计编排，继续唯一统计Tool Calling和Java手动执行，成功仍恰好两次模型调用。原统计Tool Schema、范围复核、结果校验和AiAnalysisVO.from()保持不变。

当前RISK在开关关闭时止于分类，不做第二次Tool Calling或风险读取；开启并通过V2授权后执行B2风险Tool，SUGGESTIONS再按B3条件附加建议。风险分支不调用统计Service或回退统计，也不通过CustomerRiskAnalysisService重复查询。UNSUPPORTED沿用原能力范围错误契约。

B2已扩展风险分类候选为恰好businessType/riskIntent/queryScopeIntent/queryMode/customerLevels五字段，customerLevels为null或非空A/B/C/D/UNRATED数组，其他字段为字符串。原因对象ID由HTTP明确选择，不能来自模型，缺失/歧义需澄清。统一前端由B4根据后端intent渲染，无需先判定V1/V2。

B2风险成功最多统一分类1次、风险Tool选择1次，B3请求建议可再1次；不添加额外V1分类，不开启无界循环或逐客户模型调用。

### 10.2 建议的事实保护与降级

不推荐直接让模型返回包含等级、日期、ID与长自由文本的完整风险VO，再试图用字符串检查证明没有幻觉。推荐首版使用**结构化建议动作选择+Java受控模板渲染**：Qwen负责基于可信指标组织建议，Java负责事实和最终响应。

输入仅当前页必要记录的局部别名r1…r50、关注状态、等级组、间隔/阈值及可信原因键；不发送真实客户ID、名称、联系方式、正文、地址、备注全文或完整历史。输出白名单为alias、actions[]、reasonKeys[]；actions取受控建议类型，例如VERIFY_CURRENT_NEEDS、CONFIRM_CONTACT_PREFERENCE、DISCUSS_NEXT_STEP、VERIFY_DATA。每客户最多3项，所有引用必须来自该页，reasonKeys必须是该客户已有键；DATA_REVIEW仅允许VERIFY_DATA，不伪造风险等级；观察中不能输出紧急逾期理由。

严格检查输出JSON、别名唯一性、动作/原因白名单、每项数量与整体覆盖；任一非法或缺失导致本次页建议整体UNAVAILABLE，保留风险结果。Java按原页顺序映射真实ID并渲染只描述待核实动作的文案；不接受模型提供的数字、日期、等级、排序、客户事实或原始自由文本。因此防止的是模型修改可信结果，并通过受控模板减少虚构业务事实，不宣称任意自然语言可被完美事实校验。

建议客户端缺失、超时、模型错误、协议异常、超输入预算均返回分析HTTP 200/PARTIAL+advice.UNAVAILABLE；空页无需模型，NOT_APPLICABLE。模型前/后权限失败不是可降级的模型错误，必须终止整个响应而非带旧客户返回。受控动作方案与需要多少模板/是否后续开放自由文案属于架构评审点，不改变“模型负责辅助建议、Java负责风险”的业务分工。

### 10.3 B3实际建议闭环与兼容边界

只有统一聊天RISK_SUGGESTIONS进入RiskAdviceService。LIST/REASON保留两次模型；非空建议请求最多三次；空页只复核当前权限，不调用建议模型或对象SQL。S2-A仍由确定性Assembler组装，建议保持PARTIAL/UNAVAILABLE/RISK_ADVICE_NOT_READY，不新增模型调用。

RiskAdviceInput仅投影当前页alias、客户等级、state/riskLevel/history、elapsedDays/thresholdDays和reasonKeys，不包含真实ID、姓名、原问题或授权元数据。提示词按资源加载，并通过原始消息传入。模型输出为单个对象`{"items":[{"alias":"r1","actions":["VERIFY_CURRENT_NEEDS"],"reasonKeys":["NEVER_FOLLOWED"]}]}`；严格拒绝额外字段、重复键、尾随内容、未知/重复/缺失客户、非法动作及原因。每个客户恰好一条、1到3个不同动作，原因集合必须与输入完全相同；DATA_REVIEW只允许VERIFY_DATA，其余状态禁止该动作。Java按可信输入顺序映射真实ID并渲染模板，不接受自由文本建议或模型事实。

不改变原advice根结构：成功为SUCCESS / AVAILABLE，source=QWEN_ACTIONS_JAVA_TEMPLATE；items包含customerId、actions、reasonKeys、suggestion。失败为PARTIAL / UNAVAILABLE，稳定代码RISK_ADVICE_UNAVAILABLE、INPUT_LIMIT、BUSY、TIMEOUT、NOT_READY（均带RISK_ADVICE_前缀）；空页保持NOT_APPLICABLE。不可变复制原VO，仅替换全局status与advice，原records、分页、T、指纹和原因不变。

CustomerRiskAdviceAccessVerifier在建议前后各开一个REQUIRES_NEW只读短事务，重新验证三权限和完整上下文；每次对当前页一次批量selectVisibleCustomers，读取ID/ownerId/负责人部门/status，复用原可见性与POTENTIAL/ACTIVE、负责人有效条件，不关联跟进表。可见性缺失或归属/状态前后变化409，权限/角色/部门/范围变化403；即使模型失败也执行返回前复核，绝不以PARTIAL夹带旧数据。只保证两次短快照采样时的检查，不能消除最终复核后直到HTTP送达期间的全部TOCTOU竞态。

建议独立同步HTTP客户端复用Qwen配置，连接2秒、动态读取超时至多8秒、无重试、输出至多4000 token、并发至多4个且不排队；没有后台Future任务。默认当前页至多50人、提示词加输入至多12000 UTF-8字节（保守容量门槛，非实测token数）、响应至多24000字符；超限整页降级，不拆批或静默遗漏。统一入口以单调时钟计算55秒软预算，建议开始时扣除已耗时和3.5秒最终复核预留，剩余不足则降级；并非对全部既有数据库/网络阶段的硬中断保证。风险聚合仍一次，非空建议多两次批量可见性查询；这些不是第二次聚合。

前端AI请求仍60秒，原分类与工具客户端读取各20秒。B3没有修改前端，也不宣称三次调用必在60秒完成；预算及本地合成HTTP超时测试不能替代真实Qwen/MySQL性能验收，B4需统一端到端超时与用户反馈。单机Semaphore不是分布式限流。

## 11. 分页、性能、一致性与异常

### 11.1 事务与分页一致性

风险读取公共方法使用短`@Transactional(readOnly=true, isolation=REPEATABLE_READ)`，在同一请求数据快照中完成准入/范围复核、一次授权聚合、Java判级/筛选/排序/计数，再按(page−1)×size取页。T在该次业务执行开始由Clock固定，不在每条记录上调用now；T是计算参考时间，不声称等于MySQL MVCC事务ID。

模型调用在事务外。结构化LIST/REASON不依赖模型是否启用；自然语言分类不可用时503，已经有风险结果的建议客户端不可用则部分成功。不得把网络等待放在只读事务里长期占连接。

跨HTTP请求是重新分析当前状态，不复用客户端上传的T或旧规则。Offset在数据变更时可能重复/漏过此前页的条目；返回`consistency=LIVE_REQUEST_SNAPSHOT`及本次T，明确不保证跨页历史快照。首版不引入持久化快照表、缓存会话或历史风险回放。后续若要求严格跨页一致，需要另行设计，不能声称仅凭稳定ORDER BY已经解决变更一致性。

为“当前页建议”提供`resultFingerprint`：服务器对规范查询、操作者/范围归属、完整规则版本、已排序当前页可信记录及匹配总数做规范摘要，**不包含T本身**（T每次变化），也不把摘要当授权令牌。SUGGESTIONS重新取当前T并计算；若提交的expectedResultFingerprint不符，409要求刷新，不发送过期页给模型。若一致则对新快照结果生成建议并返回新T，页面应更新上下文。记录/分级/页序因时间跨阈值或数据变化会改变指纹。该机制检测可见结果变化，不提供旧数据读取，也不能代替三权限与对象校验。

### 11.2 当前资源控制与待测量边界

| 资源 | 当前实现 | 失败行为 |
|---|---|---|
| page/size | 默认1/20，size≤50；无动态sort、任意ID数组 | S2-A非法格式/边界400；自然语言风险查询计划非法422；不静默裁剪 |
| 授权客户聚合 | 默认maxCandidates=5000，cap+1检测，历史不截断；单原因只查目标客户 | 超容量503/RISK_CAPACITY_EXCEEDED，禁止返回不完整总量 |
| SQL | 参数化，一次核心聚合，建议前后各一次当前页批量复核；聚合SQL timeout=10秒，批量复核SQL timeout=3秒 | 查询失败整体拒绝，不用空页或PARTIAL替代；驱动及真实执行时限需实测 |
| 建议模型 | 默认最多50客户；Prompt加输入≤12000 UTF-8字节；提供方输出≤4000 token且Java校验≤24000字符，每客户1–3动作 | 超预算整页UNAVAILABLE，不隐式截断、拆批或重试 |
| 模型阶段 | 分类/Tool客户端连接5秒、读取20秒；建议独立连接2秒、读取至多8秒；风险建议最多3次模型，S2-A为0次 | 分类/Tool失败503；建议阶段失败降级，授权失败整体拒绝 |
| 总预算与并发 | 统一入口记录55秒软预算，建议按剩余预算准入；建议单JVM并发默认4、不排队；前端AI超时仍60秒 | 建议忙或预算不足返回200/PARTIAL/UNAVAILABLE，不是已实现HTTP 429或硬截止时间 |

以上为当前默认保护值，**不是生产性能验收或端到端硬SLA**。初版85秒/95秒预算和分布式/每用户限流建议并未实施；B4需基于当前同步调用与前端60秒限制核对交互和超时反馈，不能声称已自动扩展。客户端取消、数据库容量和真实模型耗时仍待独立验证，保持PRD的20/50及先算后分页不变。

### 11.3 ProblemDetail与部分成功

CustomerRiskExceptionHandler仅绑定CustomerRiskController。共享聊天Advice显式处理CustomerRiskException并复用同一状态/code映射，不能落入V1通用500；不要把两个有通用兜底的Advice同时绑定共享Controller。保留V1所有既有异常契约。B4应使用当前实际映射：

| HTTP | code | 情况与结果 |
|---|---|---|
| 400 | AI_REQUEST_INVALID / RISK_REQUEST_INVALID | 分别为统一聊天请求格式/校验及S2-A严格请求非法 |
| 401 | UNAUTHORIZED / AI_AUTHENTICATION_REQUIRED / RISK_AUTHENTICATION_REQUIRED | 安全入口未认证或相应服务缺少可信主体 |
| 403 | FORBIDDEN / RISK_ACCESS_DENIED | 公共权限或V2三权限、角色、用户状态及范围配置不符；风险策略内部Resolver失败转RISK_ACCESS_DENIED |
| 403 | RISK_ACCESS_CONTEXT_CHANGED | Tool前或模型前后权限、角色、部门/范围改变 |
| 404 | RISK_CUSTOMER_NOT_FOUND | 单客户不存在/不可见/不适用，统一说明 |
| 409 | RISK_RESULT_STALE | 当前页条件指纹不符，或页内对象在执行期间改变；无旧页返回 |
| 409 | AI_TOOL_REPEATED | 直接重复执行请求级Tool；风险多调用模型响应在执行前按502拒绝 |
| 422 | RISK_SCOPE_UNSUPPORTED | ALL、销售DEPT等语义范围不合法 |
| 422 | RISK_QUERY_UNSUPPORTED / RISK_CUSTOMER_SELECTION_REQUIRED | 历史/计划/写操作等不支持、对象需选择或歧义需澄清 |
| 422 | AI_ANALYSIS_UNSUPPORTED / AI_RISK_CHAT_DISABLED | 统一分类不支持、混合/不明确需求，或风险聊天开关关闭；不回退统计 |
| 502 | AI_MODEL_RESPONSE_INVALID | 分类或Tool协议非法（含非空参数、未知/多个工具），或可信结果上下文不一致 |
| 503 | AI_MODEL_UNAVAILABLE / AI_ANALYSIS_NOT_READY | 分类/Tool模型失败或客户端未就绪；AI_RISK_CHAT_NOT_READY仅保留B1历史异常映射 |
| 503 | RISK_RULES_NOT_READY / RISK_CAPACITY_EXCEEDED | 规则配置不可用或集合容量超过保护值 |
| 500 | RISK_QUERY_FAILED / RISK_ANALYSIS_FAILED / AI_ANALYSIS_FAILED | 风险领域数据错误、S2-A兜底或聊天兜底异常，不外露SQL |
| 200 | status=PARTIAL、advice.UNAVAILABLE | 核心成功、建议失败/限流/预算不足；保留真实records/total，不返回ProblemDetail代替整个结果 |

建议失败的unavailableCode取RISK_ADVICE_UNAVAILABLE、RISK_ADVICE_INPUT_LIMIT或RISK_ADVICE_NOT_READY等受控值，不返回模型原文错误。空结果不是异常，不使用206分段内容或把模型失败报为“没有关注客户”。

## 12. 安全边界与失效处理清单

1. V2三权限和单有效销售角色在任何风险明细查询前完成；只查权限元数据不算提前读取客户。直接调Service同样受保护。
2. 不接受模型/HTTP提供身份、部门、实际范围、分析时间、阈值、已计算等级；语义scope仍需后端校验。
3. SQL始终限定授权客户、POTENTIAL/ACTIVE、非删除/非公海；跟进关联带CUSTOMER和deleted=0，不按跟进人改变客户边界。非法ALL上下文在Mapper亦fail-closed。
4. 默认查询只含高/中，全状态不扩大生命周期或组织范围；特殊状态不伪装风险等级，默认空页不能称全体客户无风险。
5. 单客户、页内建议、前次结果ID都重新授权；请求指纹、模型alias、旧临时会话都不是可转授的能力。
6. 可信风险结果不可变，模型无机会覆盖等级/数字/日期/原因/排序；建议只附加经校验动作，不能写库或执行SQL。
7. 最小化模型及日志输入：不记录JWT、Key、密码、正文、完整请求或完整客户清单；只记录请求关联标识、动作、受控失败类型、数量、规则版本及耗时。新增请求关联标识属于后续实现设计，不冒充现有V1能力。
8. 业务异常与模型建议异常分开；权限撤销必须整体停止，不以PARTIAL夹带旧客户；无Qwen时结构化风险读取仍可用。
9. 不自动使用历史ALL管理员资格，不改变V1功能权限、SQL口径、现有统计Tool schema或response；回归中必须包含仅双权限无follow:list的V1成功路径。

## 13. 测试与验收设计

下表保留分层验收范围。B1/B2/B3已完成相应离线验证，最后一次后端离线回归为676项通过、打包成功，前端离线5项通过，详见[归档B3报告](../historical/AI_AGENT_V2_S2_B3_IMPLEMENTATION_REPORT.md)。这些是历史执行记录，本次文档整理未重跑测试；真实Qwen/MySQL、容量/性能及B4前端E2E仍未验收。

| 层级/场景 | 必测点 | 阶段与方法 |
|---|---|---|
| Evaluator边界 | A 14/30、其他30/60、无历史7/14分别在前1毫秒/恰好/后1毫秒；跨自然日未满24小时不提前命中 | S1固定Clock纯单元 |
| 数据状态 | 无记录、只有删除记录、只有未来/早于建档异常、有效+异常、NULL时间防御；未评级不套A级 | S1单元+聚合投影测试 |
| 排序与去重 | HIGH/MEDIUM/LOW→A/B/C/D/null→excess降序→数值ID升序；同事实原因去重、取最高；特殊分区独立 | S1单元，含不同长度数字ID与阈值超期0 |
| 规则配置 | 组缺失、负数/相等阈值、版本空、hash变化、请求中快照不变；风险未就绪不污染V1 | S1配置/上下文测试 |
| SQL与集合 | LEFT JOIN零记录、空行不算异常、MAX仅有效时间、不按follow owner、同ID异type、逻辑删除、公海/INACTIVE排除 | S1 Mapper结构+受控MySQL集成；H2不冒充MySQL |
| 范围和单对象 | 经理DEPT/SELF差异、销售SELF、客户端越界ID、不可见与不存在同404，聚合前已限定范围 | S1服务/Mapper，S2-A HTTP |
| 角色/功能权限 | 三权限分别缺失、两管理员、多角色含管理员/销售、未知角色、配置不符；默认也拒绝 | S1策略单元，S2-A MockMvc |
| 分页与容量 | default20/max50，先全局判级排序计数再取页；空页保留total、cap+1整次拒绝，无假总数 | S1单元/规模用例，S2-A协议 |
| Controller/VO | 严格类型/未知字段/超大ID、必填可空JSON、所有错误映射、默认与全状态分离 | S2-A MockMvc与合同测试 |
| Tool隔离 | Schema空参数；伪造userId/customerId/page/scope、错误名、多调用、失败重试、并发不同用户模式均安全 | S2-B Mock模型+真实Tool对象 |
| 意图路由 | V1两统计、新三风险意图、历史/计划/混合写入/ALL_STATES歧义、SELECTED缺ID；V1不要求follow:list | S2-B Mock，大量边界不付费 |
| 建议失败/污染 | 不认识alias、额外ID/等级、缺项、伪造reason、超动作数、超token、超时、未启用及限流均保留风险并标不可用 | S2-B Mock，不能只测模型正常文本 |
| 并发与一致性 | 两阶段授权/部门/owner变化、单次同T同规则、指纹不符409、跨页变更LIVE语义，取消不无限后台运行 | S2-A/B受控集成 |
| 旧版本回归 | AiAnalysisService/Tool/Controller、CustomerStatisticsService、ScopePolicy、DataScopeResolver旧测试，管理员ALL及多角色默认原语义 | 各阶段运行直接受影响用例，不删旧断言 |
| 真实SQL与模型 | 在正式状态过滤/阈值下重新独立对账；模型只少量代表性清单/原因/建议/拒绝请求 | S4显式授权环境，记录真实与Mock差异 |
| 前后端E2E | 经理DEPT/SELF、全状态特殊状态、三权限/管理员拒绝、页大小/总数、建议失败仍显示Java结果、指纹变化刷新 | B4前端接入后，S4浏览器验收 |

S4需准备授权测试环境的异常/交接/排序样本；本次不插入或修复数据，也不复用R1含INACTIVE的290总量当冻结规则答案。真实登录会更新审计字段，后续验收须核对授权和副作用；D1不登录、不访问数据库、不跑真实Qwen。

## 14. 阶段实施与退出条件

| 阶段 | 实施范围 | 退出条件与禁止提前扩展 |
|---|---|---|
| V2-S1 | 安全准入/范围策略、规则快照/配置、只读聚合Mapper、纯Evaluator/Comparator、风险Service及内部结果；最小对象读取 | 无模型和新HTTP入口也能独立正确判级、过滤、分页；三权限及对象边界单测/SQL验证；不开发完整Follow CRUD |
| V2-S2-A | 独立Controller、严格DTO/VO、模块Advice、结构化LIST/REASON及HTTP合同，建议状态契约 | MockMvc覆盖三权限/角色/404/分页及V1兼容；未接建议时明确NOT_READY/UNAVAILABLE，不假称建议可用；不改旧入口授权 |
| V2-S2-B1 | 统一自然语言入口、无Tool路由、V1统计编排迁移、共同响应接口、风险默认关闭、分支异常映射 | V1两次调用及JSON兼容；RISK/混合/协议错误均不触发业务；S2-A回归 |
| V2-S2-B2 | 内部风险编排、零参数风险Tool/Factory、可信计划、上下文与对象复核、复用映射/指纹 | 仅合法分支注册Tool，最多一次风险聚合；无独立聊天Controller |
| V2-S2-B3 | 已实现受约束Qwen建议、单机预算/并发保护、建议失败降级 | 合法核心结果保留；权限失败不可PARTIAL；S2-A仍保持建议未就绪，不自动接入模型 |
| V2-S2-B4 | 统一聊天前端按intent渲染统计/风险，分页、原因、建议、部分成功与指纹提示 | 前端不识别意图决定后端业务，不以隐藏按钮代替授权，联合端到端验收 |
| V2-S4（后续） | 授权环境真实MySQL/Qwen/浏览器与性能验收 | 独立SQL、边界数据、模型/界面证据、容量/超时达标后才称V2验收 |

S1/S2-A不能把后续模型未接入包装成全功能完成；结构化SUGGESTIONS若在S2-A暴露，仍须先计算可信结果后返回advice.UNAVAILABLE/RISK_ADVICE_NOT_READY，而非200假建议或丢弃核心结果。每阶段遵循最小回归，出现必须修改冻结业务规则时停止对应实现并报告。

## 15. 架构评审事项与结论

未发现需暂停整体设计的关键PRD矛盾。以下是技术评审点，不重新询问已冻结的角色、阈值、INACTIVE排除、排序优先级或分页默认值：

1. 已确认统一自然语言聊天入口，保留S2-A结构化入口；公共双权限、风险分支额外准入及已有VO联合返回按本修订实施。
2. 已采用专用只读Mapper、共享过滤外加V2限制、SQL聚合+Java全集合判级；maxCandidates默认保护与超容量拒绝已有实现，规模性能待验证。
3. 已采用一个零参数Tool、不可变查询、核心Service查询前上下文复核及建议前后批量对象复核；短事务之间仍有已记录的并发边界。
4. B3已采用结构化动作+Java模板，仅统一聊天SUGGESTIONS生成；建议质量与模板覆盖需人工/真实模型验证，自由文本不是当前协议。
5. 已采用每次查询不可变规则快照、版本摘要、风险模块NOT_READY隔离、ID数值升序及特殊状态后置；不新增规则热更新。
6. 当前为LIVE_REQUEST_SNAPSHOT与指纹重算，不提供跨页历史快照；未来若要求固定快照需独立设计。
7. B4应复用现有DTO/VO、实际错误码和null语义，核对预算与前端渲染；保护参数与离线回归均不代表真实环境性能结论。

B1/B2/B3代码已按阶段实施；风险聊天默认关闭，B3等待人工验收，B4另行实施。本次修订不改变冻结规则，也不重写S2-A历史验收事实；真实模型、数据库和性能验证仍需独立证据。
