# CRM AI Agent V2-S2-A 实施报告

日期：2026-10-23。范围：结构化风险分析 HTTP 接口、V1 VO 包迁移、必要 S1 适配及回归。S2-B 尚未实施。

## 1. 实施结果与文件

新增生产文件（相对 `backend/src/main/java/com/company/crm/`）：

- `controller/AiCustomerRiskController.java`：结构化 POST 入口、认证主体和三权限授权。
- `dto/ai/CustomerRiskRequest.java`：V2 局部严格反序列化及按 action 校验显式字段。
- `dto/risk/CustomerRiskAccessContext.java`：后端策略产生的不可变访问上下文。
- `service/CustomerRiskAnalysisService.java`、`service/impl/CustomerRiskAnalysisServiceImpl.java`：HTTP 应用协调、响应映射、指纹和建议未就绪处理；复用核心风险 Service，不调用模型。
- `vo/ai/RiskAnalysisVO.java`、`RiskRecordVO.java`、`RiskAnalysisContextVO.java`：独立 V2 响应契约，相关小结构采用嵌套 record。
- `exception/CustomerRiskExceptionHandler.java`：仅针对新 Controller 的 ProblemDetail 转换。

迁移 `vo/AiAnalysisVO.java` 至 `vo/ai/AiAnalysisVO.java`。更新以下引用：

- `controller/AiAnalysisController.java`。
- `service/AiAnalysisService.java`、`service/impl/AiAnalysisServiceImpl.java`。
- `src/test/java/com/company/crm/controller/AiAnalysisControllerTest.java`。
- `src/test/java/com/company/crm/service/AiAnalysisServiceTest.java`。
- `src/test/java/com/company/crm/ai/smoke/QwenAnalysisLiveIntegrationTest.java`。

修改 S1 文件：

- `service/CustomerRiskAccessPolicy.java`：新增 `resolveContext`，保留 `resolve` 兼容入口。
- `dto/risk/CustomerRiskPage.java`：携带可信访问上下文。
- `service/impl/CustomerRiskServiceImpl.java`：在原短只读事务内调用新上下文入口，将有效范围传给 Mapper。
- `exception/CustomerRiskException.java`：新增认证缺失、范围不支持和结果过期代码。
- `resources/mapper/CustomerRiskMapper.xml`：安全 guard 内复用普通客户公共可见性条件。

测试：新增 `controller/AiCustomerRiskControllerTest.java`；调整 `service/CustomerRiskServiceTest.java` 的上下文 mock；调整 `mapper/CustomerRiskMapperTest.java` 的跨 namespace XML 加载。

## 2. 最终结构与依赖

```text
controller/
  AiAnalysisController
  AiCustomerRiskController
dto/ai/
  AiAnalysisRequest
  CustomerRiskRequest
dto/risk/
  CustomerRiskAggregate / Query / Result / Page / AccessContext
vo/ai/
  AiAnalysisVO
  RiskAnalysisVO / RiskRecordVO / RiskAnalysisContextVO
service/
  AiAnalysisService / CustomerStatisticsService
  CustomerRiskService / CustomerRiskAnalysisService
  CustomerAnalysisScopePolicy / CustomerRiskAccessPolicy / CustomerRiskEvaluator
service/impl/
  AiAnalysisServiceImpl / CustomerStatisticsServiceImpl
  CustomerRiskServiceImpl / CustomerRiskAnalysisServiceImpl
ai/tool/
  CustomerStatisticsTool / CustomerStatisticsToolFactory
mapper/
  CustomerMapper / CustomerRiskMapper
config/
  CustomerRiskProperties
exception/
  AiAnalysisExceptionHandler / CustomerRiskException / CustomerRiskExceptionHandler
```

新调用链：Controller → CustomerRiskAnalysisService 接口 → Impl → CustomerRiskService 接口 → Impl → AccessPolicy / Evaluator / Mapper。

增加 HTTP 应用 Service 是为了让 Controller 仅处理协议和认证主体，避免把指纹、建议状态和大量 VO 映射放入 Controller，也避免让 S1 核心 Service 依赖 HTTP DTO/VO。两个 Service 均采用接口 + Impl。Policy/Evaluator 继续作为独立组件。

## 3. V1 迁移与兼容性

对 Git HEAD 中旧 VO 与新 VO 做逐行比较，替换 package 声明后内容完全相同（`V1_VO_ONLY_PACKAGE_CHANGE=True`）。V1 生产调用方仅修改 import。未修改 `AiAnalysisVO.from()`、Prompt、模型调用顺序、统计工具 Schema、双权限、SQL 或 JSON 字段。

V1/V1.1 定向回归通过，包含没有 `follow:list` 的 V1 双权限场景。Live 测试的引用已更新且随 testCompile 编译，但没有执行真实 Qwen 测试。

## 4. 风险 SQL 复用判断

公共语义相同：排除客户逻辑删除、公海；SELF 按当前 `c.owner_id`；DEPT 按当前负责人 `owner.dept_id`。因此复用 `com.company.crm.mapper.CustomerMapper.customerVisibilityConditions`。

V2 独有约束继续独立保留：

- `JOIN sys_user owner` 要求负责人 `deleted=0 AND status=1`。
- 仅 `POTENTIAL/ACTIVE`。
- 在 include 之前检查上下文非空，并且只允许有 userId 的 SELF 或有 deptId 的 DEPT。
- ALL、null、缺关键范围字段执行 `AND 1=0`，不会借公共片段的 ALL 分支扩大权限。

没有修改 V1 XML。Mapper 测试同时加载两份 XML，验证跨 namespace include、SELF/DEPT、ID/等级参数，以及 ALL/null/缺部门/缺用户的 fail-closed 分支。真实 MySQL 三项测试本次因环境开关未启用而跳过，不能把结构测试当作真实数据库对账。

## 5. 可信上下文与权限

`resolveContext` 在可信用户 ID 基础上重新读取有效用户、有效单角色、三权限和最大数据范围，返回 userId、role、departmentId、authorizedScope、requestedScope、effectiveScope。HTTP 无法提交该类型。

- 经理默认：DEPT / CURRENT_AUTHORIZED / DEPT。
- 经理本人：DEPT / SELF / SELF。
- 销售默认：SELF / CURRENT_AUTHORIZED / SELF。

核心 Service 仍在原 REQUIRES_NEW、REPEATABLE_READ、readOnly 事务中执行授权与查询，规则、T、过滤、排序、计数和分页未改变。访问上下文携带到内部结果，Controller 不自行查询权限或推断最大范围。内部用户/部门 ID 不进入响应。

新 Controller 使用 `ai:analysis AND customer:list AND follow:list`；Service 再核验有效角色和数据库权限，管理员、多角色及配置不符拒绝。合法身份请求 ALL 或销售请求 DEPT 返回范围不支持 422，未扩大 S1 允许集合。

## 6. HTTP DTO 与 VO

入口：`POST /api/customer-risks/analyses`，Controller 内部路径不重复 `/api`。

- LIST：默认 CURRENT_AUTHORIZED、PRIORITY、page=1、size=20，最大 50；保留先全量判级/筛选/排序/计数再分页。
- REASON：必须为正十进制字符串 ID，按同一授权 SQL 查询；低关注、观察中、数据待核验均可返回；不存在/不可见/不适用统一 404。规范查询仅输出 scopeIntent/customerId，page=null。
- SUGGESTIONS：重新计算当前页。非空页 PARTIAL/UNAVAILABLE，代码 RISK_ADVICE_NOT_READY；空页 SUCCESS/NOT_APPLICABLE。没有模型调用和虚假建议。

DTO 自定义反序列化只作用于该类型，不修改全局 ObjectMapper。先记录字段是否显式出现，再按操作校验，最后应用默认值。拒绝未知字段、重复键、尾随 JSON、错误类型、非法枚举、非整数/越界分页、空等级数组、非法等级、数字/前导零/科学计数/溢出 ID，以及各 action 禁止的显式字段（包括显式 null）。等级 null 或省略表示不限等级。

VO 局部 ALWAYS 序列化必填可空字段，时间转为 Asia/Shanghai 带偏移 ISO 格式；客户 ID 为字符串。状态/风险等级来自 S1，HTTP 层只映射和生成说明，不重新判级。观察中 thresholdDays/excessDays 输出 null，最近跟进时间不伪造为建档时间。原因包含可信锚点、间隔、阈值和等级组；DATA_REVIEW 通过数据质量字段解释。空页保留真实 total，未匹配为 0。

请求与根响应的诊断字符串均脱敏，错误不返回 SQL、客户内容、JWT 或底层数据库详情。

## 7. 指纹

SHA-256 输入为固定顺序元组：格式版本、可信用户、角色、部门、授权/有效范围、规范查询、规则版本、匹配总数、当前页有序可信 VO 事实。等级筛选排序归一化。LIST 与同条件 SUGGESTIONS 共用清单规范查询，不将 action 差异计入指纹。

不使用 UUID，不将每次变化的 T 直接加入；只有影响当前事实的时间边界变化才通过 elapsedDays/状态等反映。相同条件与可信结果生成相同摘要。SUGGESTIONS 先独立授权并重新查询，再比较 expectedResultFingerprint；不一致返回 409/RISK_RESULT_STALE，不携带旧客户详情。指纹不充当权限凭据。

## 8. 异常映射

| HTTP | code |
|---|---|
| 400 | RISK_REQUEST_INVALID |
| 401 | UNAUTHORIZED（安全入口）/ RISK_AUTHENTICATION_REQUIRED |
| 403 | RISK_ACCESS_DENIED（或安全入口 FORBIDDEN） |
| 404 | RISK_CUSTOMER_NOT_FOUND |
| 409 | RISK_RESULT_STALE |
| 422 | RISK_SCOPE_UNSUPPORTED |
| 503 | RISK_RULES_NOT_READY / RISK_CAPACITY_EXCEEDED |
| 500 | RISK_QUERY_FAILED / RISK_ANALYSIS_FAILED |

Advice 仅绑定新 Controller，不改变 V1 Advice。查询失败不会返回伪装的空清单。

## 9. 实际验证

执行命令（离线依赖，不调用模型）：

```powershell
mvn -o -q -f backend/pom.xml '-Dtest=AiCustomerRiskControllerTest,CustomerRisk*Test,CustomerStatisticsServiceImplTest,CustomerAnalysisScopePolicyTest,DataScopeResolverTest,CustomerMapperSqlTest,AiAnalysisServiceTest,AiAnalysisControllerTest,CustomerStatisticsToolTest' package
```

最终命令退出码 0，完成生产编译、全部测试源码编译、定向测试及 Spring Boot 打包。共发现 261 项：258 通过，0 failures，0 errors，3 skipped。

| 测试类 | 项数 | 结果 |
|---|---:|---|
| AiCustomerRiskControllerTest | 42 | 通过 |
| CustomerRiskAccessPolicyTest | 21 | 通过 |
| CustomerRiskEvaluatorTest | 17 | 通过 |
| CustomerRiskPropertiesTest | 7 | 通过 |
| CustomerRiskMapperTest | 6 | 通过 |
| CustomerRiskMapperMySqlTest | 3 | 环境开关未启用，跳过 |
| CustomerRiskServiceTest | 8 | 通过 |
| CustomerMapperSqlTest | 2 | 通过 |
| AiAnalysisServiceTest | 79 | 通过 |
| AiAnalysisControllerTest | 10 | 通过 |
| CustomerStatisticsToolTest | 11 | 通过 |
| CustomerStatisticsServiceImplTest | 25 | 通过 |
| CustomerAnalysisScopePolicyTest | 24 | 通过 |
| DataScopeResolverTest | 6 | 通过 |

HTTP 测试使用真实 Controller、自定义解析、方法安全、应用 Service、核心 Service、AccessPolicy 和 Evaluator；仅数据库相关 Mapper/Resolver、认证基础依赖及 Clock 使用 Mock。覆盖权限撤销时旧指纹失效、经理范围区别、严格字段、分页、等级过滤、LOW/OBSERVING/DATA_REVIEW 原因、建议状态、字段 null、字符串大 ID、时区、T 小幅变化的稳定指纹、条件/归属/事实变化和安全错误映射。

首轮有一项新增测试错误：重复 `when(...).thenThrow(...)` 设置时触发上一个异常桩；改为 `doThrow` 后复测通过。该失败属于测试代码，未以失败结果报告通过。之后按规范查询 LIST/REASON 分别输出契约字段，再次执行完整上述命令通过。

## 10. 限制、检查与 S2-B 准备

- 本次没有连接真实 MySQL；尚未完成本阶段独立 SQL 数据对账、容量/执行计划或事务隔离的真实并发验收。
- 没有真实 Qwen 调用、前端变更、数据库结构/数据修改，也没有 commit/push。
- `git diff --check` 通过；新增/迁移文件及未跟踪文件已核对，V1 生产调用方只改 VO import。
- S1 核心逻辑及冻结阈值保留；新增 HTTP 应用层可以供后续 S2-B 复用映射/指纹思路，但尚未创建自然语言 Controller、Risk Tool/Factory 或建议模型服务。
- 未来 S2-B 仍需实现绑定计划、模型前后权限/页内对象复核、模型预算、严格协议和受控建议降级；当前不宣称具备这些模型能力。
- 跨 HTTP 请求为 LIVE_REQUEST_SNAPSHOT；没有持久化历史快照，不保证数据变化后的跨页快照一致性。

S2-A 代码与离线合同验证已完成，等待人工审查；真实数据库验证明确保留为未验证项，不进入 S2-B。
