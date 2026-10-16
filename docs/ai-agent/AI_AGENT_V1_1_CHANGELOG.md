# CRM AI 智能分析 V1.1 变更与验收基线

更新：2026-10-16。V1统计闭环已完成；V1.1三阶段开发及S3真实联调验收完成，结论 **PASS**。V2尚未开始开发，下一步仅准备需求分析。

## 1. 背景、根因与范围

V1识别queryScopeIntent后要求显式请求与最大授权范围完全一致，经理有DEPT却不能询问本人SELF。原Tool只传userId和timeRange，S1总是执行最大授权，AI也要求结果等于最大授权；仅放开一个判断不能实现正确本人统计。

V1.1把Authorized Scope（最大授权）、Requested Scope（请求意图）和Effective Scope（实际执行）分开，新增经理合法DEPT授权内的SELF查询。SELF始终按当前认证用户的owner_id，不按创建人；不新增指标、指定人员/任意部门查询、多角色组合业务或V2能力。完整角色规则唯一主表见[需求第3.2节](AI_AGENT_V1_REQUIREMENTS.md#32-v11-查询范围与业务身份规则主表)，技术细节见[技术基线](AI_AGENT_V1_TECH_STACK.md)。

## 2. 技术变更与安全兼容

- 新增`backend/src/main/java/com/company/crm/service/CustomerAnalysisScopePolicy.java`，`resolve(Long authenticatedUserId, CustomerQueryScopeIntent requestedScope)`自行读取最大授权和有效角色，显式请求按业务矩阵校验，不信任模型或调用方构造的权限上下文。
- `CustomerStatisticsService`保留`getStatistics(Long authenticatedUserId, StatisticsTimeRange timeRange)`，新增第三参数`CustomerQueryScopeIntent requestedScope`重载。旧接口及CURRENT_AUTHORIZED保留最大授权与多角色语义；显式请求对角色混乱、授权配置不符和非法范围拒绝。
- `AiAnalysisService`复用同一策略，Tool Calling前确定预期Effective Scope；ToolFactory保留默认create方法，新增范围重载，Tool不可变绑定用户和validatedRequestedScope。
- Tool调用S1新接口，由S1再次独立验证；两个公共统计入口均为只读事务，共用executeStatistics。AI校验结果范围和时间，VO只使用实际S1结果生成回答。
- 模型Schema仍只有timeRange；每请求独立Tool、最多一次工具/S1执行。HTTP请求、响应结构和前端页面不变。
- 复用原DataScopeResolver、CustomerMapper XML及共享过滤，不修改SQL、角色配置或数据库结构。保留普通客户、公海排除、逻辑删除、等级聚合与四种创建时间窗口。

## 3. 三阶段结果与证据来源

以下是本次文档维护前已完成的运行记录，不表示本次重跑或重新访问数据库/Qwen。

| 阶段 | 已确认结果 | 证据来源 |
|---|---|---|
| S1 底层能力 | 策略及服务重载完成，126项定向回归通过 | 本会话S1交付报告；策略、S1及兼容性测试源码 |
| S2 AI/Tool接入 | 范围传递、结果校验完成，154项定向回归通过 | 本会话S2交付报告；AI/Tool测试及调用链用例 |
| S3 真实验收 | 22个API场景全部通过（18成功、4预期拒绝）；10次浏览器AI请求、15项界面检查通过；所有成功统计独立SQL对账一致 | 本会话前一阶段2026-10-16 S3中文验收报告 |
| S3 自动回归 | 后端154项、前端5项通过，当前代码打包成功 | 同上；后端本地Surefire报告及前端现有测试 |

后端测试数按AiAnalysisServiceTest 78、CustomerStatisticsToolTest 11、AiAnalysisControllerTest 10、CustomerAnalysisScopePolicyTest 24、CustomerStatisticsServiceImplTest 25、DataScopeResolverTest 6组成。本次只读核对了`backend/target/surefire-reports/TEST-*.xml`摘要；这些是可再生的本地产物，不是永久提交证据。

历史执行命令：

```powershell
mvn -o -f backend/pom.xml '-Dtest=AiAnalysisServiceTest,CustomerStatisticsToolTest,AiAnalysisControllerTest,CustomerAnalysisScopePolicyTest,CustomerStatisticsServiceImplTest,DataScopeResolverTest' package
# 以下在frontend目录执行
node --test tests/ai-analysis.test.mjs
```

真实验收使用当前构建的临时8081后端（local,ai）、5174前端代理及独立Edge，Qwen为真实调用、统计为真实MySQL。临时进程、驱动、浏览器目录及结果文件已在S3清理；没有另存永久逐请求原始日志，详细执行证据来源是上述会话报告，本文仅保留脱敏摘要。

## 4. 验收数据摘要

四类现有测试账号均启用、单有效角色、具备ai:analysis与customer:list；两个管理员实际ALL，经理DEPT，销售SELF。2026-10-14 SYSTEM_ADMIN为SELF、修复待确认的阶段描述不再代表本次实测环境，不推断历史修改者或操作过程。

| 实际范围 | 总量 | A | B | C | D | 未评级 | 本月 | 上月 | 近30天 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 经理DEPT | 151 | 25 | 49 | 46 | 20 | 11 | 0 | 14 | 4 |
| 经理SELF | 40 | 8 | 12 | 11 | 5 | 4 | 0 | 1 | 1 |
| 销售SELF | 88 | 16 | 21 | 35 | 14 | 2 | 2 | 6 | 3 |
| 两类管理员ALL | 290 | 49 | 82 | 101 | 43 | 15 | 2 | 26 | 9 |

以上是2026-10-16样本快照，不能硬编码为未来参考答案；近30天按各响应referenceTime及Asia/Shanghai窗口对账。经理部门含其他成员111个客户，本人40个是部门子集；两个管理员测试账号无本人归属记录。

核心经理团队/本人/等级/上月请求返回正确范围；全公司请求422。销售本人成功、部门拒绝；管理员全公司成功、本人拒绝。四类角色未指定范围使用最大授权。浏览器核验加载、重复提交限制、统计卡片、等级、复制及临时会话恢复，刷新清空符合V1限制。

S3中Vite缓存失效及临时脚本中文编码问题修复后重跑通过；无效脚本输入造成的422没有计入正式业务场景，也不作为模型错误结论。

## 5. 验收边界与已知限制

- S3最终结论PASS，覆盖关键业务、真实SQL、Qwen及浏览器，不表示所有输入和故障分支都已验证。真实缺权限账号、模型故障注入、剪贴板权限拒绝未现场覆盖，Mock和离线验证不能替代它们。
- 当时数据有37个公海客户，但无逻辑删除客户或精确月初边界样本；经理本人归属与创建人统计相同，不能单凭该样本区分owner_id与created_by，需结合SQL及单元测试。管理员有历史归属时拒绝SELF由策略测试覆盖，不冒充现场数据验证。
- 通用`AiUnsupportedAnalysisException`默认提示仍包含V1“暂不支持主动缩小到本人”；策略自定义拒绝提示已独立使用。旧文案是待处理源码问题，本阶段不修改。
- ALL时间的startTime/endTime内部为null，当前JSON省略字段；前端类型仍声明必填可空，存在类型契约差异，需后续源码维护确认。
- S3经用户批准执行正常登录，更新last_login_at；现有Mapper还同步更新updated_at，前置说明遗漏后已如实告知。未修改客户、角色或权限，也未回写恢复审计字段。以后只读验收需先核对登录SQL副作用。
- 同义表达仅抽样通过；未实现风险识别、跟进建议、持久化会话、多轮记忆、任意SQL、RAG、图表或requestId追踪。

## 6. 后续版本边界

现有V1/V1.1代码与文档可作为V2需求分析基线。V2客户风险识别与跟进建议尚未开发；指标、权限、工具和响应契约均须另行需求确认，本文不引入V2设计。历史归档保持原状，当前开发状态以[项目上下文](../CODEX_CONTEXT.md)为入口。
