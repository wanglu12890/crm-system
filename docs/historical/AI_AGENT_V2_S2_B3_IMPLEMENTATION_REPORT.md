# CRM AI V2 S2-B3 实施报告

日期：2026-10-25。范围：统一聊天 SUGGESTIONS 的受约束 AI 建议；停止于 B3，不进入 B4。

## 1. 审查与兼容决策

执行前已运行 git status --short 和 git diff --check。保留工作区原有 B1/B2 未提交修改；以下文件清单按 B3 相对 B2 的增量列出，不把旧阶段成果重复计入。

核对任务、后端规范、PRD、D1、S1/S2-A/B1/B2 报告及实际查询、Tool、VO、指纹、异常和 Spring AI 1.1.8 调用方式。S1 报告原有部分文字损坏、项目背景中 V2 未开始的描述过时，按生产代码和测试确认事实，不修改历史报告。

D1 的动作白名单加 Java 模板方案比任务示例的自由文本 suggestion 更严格，故采用 D1：Qwen 选择辅助动作和事实引用，Java 渲染实际建议。不接受模型自由编写身份、风险数字或文案。原 advice.items 为 List<Object>，此前为空；只在自然语言建议成功时填入新的类型化项目，根 VO 和 S2-A JSON 均不需扩字段。

## 2. 实际新增和修改文件

生产路径相对 backend/src/main/java/com/company/crm/：

| 新增文件 | 职责 |
| --- | --- |
| service/RiskAdviceService.java、service/impl/RiskAdviceServiceImpl.java | 最小输入、单次建议调用、严格输出解析及资源限制 |
| dto/risk/RiskAdviceInput.java | 当前页匿名事实投影 |
| dto/risk/RiskAdviceResult.java | 动作枚举、已验证结果及最终渲染项目 |
| dto/risk/CustomerRiskVisibility.java | 仅供授权复核的 ID/负责人/部门/状态投影 |
| service/CustomerRiskAdviceAccessVerifier.java | 独立短事务内重新授权与批量对象复核 |
| config/RiskAdviceProperties.java | 安全容量、时间和并发默认值，非法配置使建议不可用而不污染 V1 |
| ai/config/RiskAdviceChatClientFactory.java | 仅 ai profile 的独立同步 Qwen 传输，无重试 |
| exception/RiskAdviceUnavailableException.java | 建议阶段受控降级码，不保存模型原始输出 |

修改：service/AiRiskAnalysisService.java、service/impl/AiRiskAnalysisServiceImpl.java、service/impl/AiAnalysisServiceImpl.java，传递单调时钟起点并编排建议和前后检查；mapper/CustomerRiskMapper.java 与 resources/mapper/CustomerRiskMapper.xml 增加当前页批量复核；resources/application.yml 增加公共建议预算默认值。未修改 S1 风险聚合算法或阈值。

新增 resources/prompts/customer-risk-advice.st。更新 docs/ai-agent/AI_AGENT_V2_ARCHITECTURE_DESIGN.md 和本报告。

测试新增（相对 backend/src/test/java/com/company/crm/）：service/RiskAdviceServiceTest.java、controller/AiRiskAdviceControllerTest.java、ai/config/RiskAdviceChatClientFactoryTest.java。修改 mapper/CustomerRiskMapperTest.java、controller/AiRiskAnalysisControllerTest.java 与 AiAnalysisRoutingControllerTest.java，补充 SQL 结构和新依赖/方法签名回归。

沿用 service/impl、dto/risk、ai/config、resources/prompts 目录；没有新 Controller、自然语言入口或 Agent 框架。

## 3. 最终执行链

统一入口公共双权限 → AiIntentRouter → 风险开关 → AiRiskAnalysisService → V2 三权限/单销售角色授权 → 已验证查询与请求级零参数 Tool → 受控 Tool Calling → CustomerRiskService 单次风险聚合 → 确定性 Assembler（含预期指纹验证）。

- LIST/REASON：直接返回原结果，只有分类和 Tool Calling 两次模型调用。
- 空页 SUGGESTIONS：复核最终权限，无对象 SQL 或建议模型，返回原 SUCCESS/NOT_APPLICABLE。
- 非空 SUGGESTIONS：建议前上下文与当前页对象复核 → 最小事实 → 单次 RiskAdviceService → 严格校验 → Java 模板 → 返回前再次复核 → 不可变复制 VO。

正常非空建议最多三次模型调用，失败不重试分类、Tool 或建议模型，不重新执行风险聚合。

## 4. 最小事实与输出协议

输入每客户包含 alias（r1…r50）、customerLevel、state、riskLevel、history、elapsedDays、thresholdDays、reasonKeys；全部来自 Java 当前页可信结果。

不发送真实客户 ID、名称、编号、联系方式、地址、跟进正文、备注、全部历史、用户原问题、身份、角色、部门或授权上下文。别名映射由 Java 保持，模型不能决定它与客户 ID 的对应关系。建议模型不注册任何工具。

严格输出示例：

```json
{"items":[{"alias":"r1","actions":["VERIFY_CURRENT_NEEDS"],"reasonKeys":["NEVER_FOLLOWED"]}]}
```

每个当前页客户恰好一项，输出别名集合与输入完全相同。单项只接受 alias/actions/reasonKeys；每客户选择 1–3 个不同动作，允许 VERIFY_CURRENT_NEEDS、CONFIRM_CONTACT_PREFERENCE、DISCUSS_NEXT_STEP、VERIFY_DATA。DATA_REVIEW 只允许 VERIFY_DATA，其他状态不能选择它；原因集合必须恰好为该客户的已有原因。

独立校验单对象、重复键、尾随内容、Markdown、额外/缺失字段、错误类型、null、非法枚举、未知/重复/缺失客户、跨页别名、超数量和超长度。动作及引用单值最长 64 字符，整响应最长 24000 字符。任何非法项使整页建议不可用，不能部分遗漏后声称成功。

Java 按可信页内 alias 映射真实 ID，保持原页顺序，生成 customerId/actions/reasonKeys/suggestion 项目。建议文案来自枚举模板，最多三段简短、需人工确认的核实/联系/下一步动作；不执行写操作。模型不能通过建议字段改变原风险事实、原因、指纹、分析时间或排序。

严格保证的是结构、集合对应、动作和事实引用白名单，以及模板没有接受模型自由文本。模型选择的动作是否最适合实际业务、原始风险事实是否符合真实世界，不能仅由此形式化证明；提示词与最小投影降低风险，响应明确提示人工核实。没有使用正则过滤后宣称彻底消除幻觉。

## 5. 成功、降级与不可降级异常

成功：HTTP 200 / SUCCESS，advice.status=AVAILABLE，source=QWEN_ACTIONS_JAVA_TEMPLATE，items 为 Java 渲染建议。保留原 answer、records、原因、等级、分析上下文、分页、规范查询和指纹。

建议阶段模型不可用、超时、协议/内容不合法或内部调用异常：在最终授权仍有效时，HTTP 200 / PARTIAL / advice.UNAVAILABLE，items=[]，只返回稳定原因码：RISK_ADVICE_UNAVAILABLE、RISK_ADVICE_INPUT_LIMIT、RISK_ADVICE_BUSY、RISK_ADVICE_TIMEOUT、RISK_ADVICE_NOT_READY。不向客户端返回原始模型响应或底层错误。

权限、角色、部门、实际范围变化：403 / RISK_ACCESS_CONTEXT_CHANGED。当前页缺失、归属/状态变化或预期指纹过期：409 / RISK_RESULT_STALE。批量查询基础设施失败整体拒绝，不返回客户详情，不能被建议失败降级吞掉。模型失败后仍执行最终授权复核。

## 6. 建议前后权限及批量对象复核

复核组件的两个公共方法均为 REQUIRES_NEW / REPEATABLE_READ / readOnly，模型调用不处于事务中。前后分别重新读取当前有效用户、单销售角色、三权限及数据范围，并与 B2 绑定上下文逐字段比较。

每次只对当前页至多 50 个 ID 执行一次 selectVisibleCustomers，复用原 customerVisibilityConditions 及有效负责人、POTENTIAL/ACTIVE 条件，排除公海/删除/不适用客户；非法 ALL/null 范围或空/超限 ID 集合 SQL fail-closed。查询只投影 ID、负责人、负责人部门和状态，不关联 follow_record，不读敏感正文、不重新计算风险。

复核返回 ID 集合必须恰好覆盖当前页；再比较前后归属/状态，连同一部门内转交也拒绝旧结果。没有逐客户 N+1。授权 SQL 与风险聚合分开计数：非空建议单请求 1 次核心聚合 + 2 次对象批量复核；另外有必要权限元数据 SQL。空页只再次验证权限，不执行对象查询。

并发边界是两个独立短事务的采样时刻，不锁住客户直到模型完成，也不引入分布式锁。不能保证最后一次检查后直到 HTTP 送达期间权限永远不变化，不能发现每一次中途变化后又恢复的 ABA 情况。未进行真实 MySQL 并发验收。

## 7. 指纹与不可变附加

沿用 B2/S2-A Assembler 的原 SHA-256 算法，在建议模型之前校验 expectedResultFingerprint。建议既不参与重新判级，也不改指纹。最终构造新 RiskAnalysisVO，仅更换全局 status 与 advice，不修改原共享对象。

测试以同一固定 Clock、规范查询与数据比较 LIST 和建议成功响应的 records/page/analysisContext/resultFingerprint/normalizedQuery/answer，断言完全一致。S2-A 仍使用原确定性服务和组装器，没有接入 RiskAdviceService，非空结构化建议保持 PARTIAL/UNAVAILABLE/RISK_ADVICE_NOT_READY，零模型调用。

## 8. 时间、容量与并发预算

核对到前端 AI 接口为 60000ms（通用 Axios 为 15000ms），原分类和 Tool 同步客户端连接 5 秒、读取 20 秒；未发现已有统一 AI 并发/请求预算。不能据此承诺三次模型调用在 60 秒内完成。

本次默认配置：

| 项目 | 实际默认值与处理 |
| --- | --- |
| 输入客户 | 当前页，最多 50；不跨页或拆成多轮 |
| 输入容量 | Prompt 加输入最多 12000 UTF-8 字节，保守大小门槛，不冒充实际 tokenizer 计量 |
| 输出 | 4000 token 提供方限制，加 Java 24000 字符限制 |
| 建议连接/请求读取 | 共享同步 JDK HttpClient 连接 2 秒；每次读取超时 min(8 秒, 剩余预算减 3.5 秒) |
| 请求软预算 | 从统一分类前以 System.nanoTime 计算 55 秒；建议开始时扣除已耗时，预留 3.5 秒返回前复核 |
| 并发 | 单 JVM Semaphore 最多 4，不排队、不自动重试；忙时整页降级 |

Prompt 或配置非法、输入超限、剩余不足均不发送建议事实；单次无法覆盖整页时整体降级，不隐式截断。输出和动作长度检查也在 Java 执行。

没有建议线程池/Future 后台任务，不依赖取消一个仍在无限运行的 worker；独立建议模型显式 maxAttempts=1。基础 HTTP Client 被复用，建议请求专属构建只设置本次超时，不修改全局统计/Tool 客户端。

55 秒是可选建议阶段的软准入/剩余预算，不是整个 Servlet、既有数据库连接等待与权限 SQL 的硬中断上限。独立 HTTP 超时通过本地合成服务器的响应正文停滞用例验证；DNS、驱动等待、真实 Qwen 和生产数据库延迟仍需实测。前端 60 秒未修改，B4 需统一端到端预算和超时反馈；不能把本阶段测试当作生产 SLA。

## 9. 测试与真实环境情况

已通过生产编译和 B2/B1/S2-A 定向回归。建议定向覆盖正常三阶段、最小输入、动作/JSON/引用污染、覆盖与数量、权限前后变化、对象丢失或归属变化、空页、模型失败、批量 SQL 失败、旧指纹、S2-A 隔离、容量、剩余超时和并发 permit 释放。

完整离线命令：

```powershell
mvn -o -q -f backend/pom.xml '-Dtest=*,!QwenLiveIntegrationTest,!QwenAnalysisLiveIntegrationTest,!CustomerRiskMapperMySqlTest' package
```

首轮完整离线回归 674 项通过；补充显式别名映射、并发请求隔离和部分客户丢失用例后，以完成检查中的最终结果为准。关键测试：

| 测试 | 数量 |
| --- | ---: |
| RiskAdviceServiceTest | 37 |
| AiRiskAdviceControllerTest | 26 |
| RiskAdviceChatClientFactoryTest | 1（本地合成 HTTP，非 Qwen） |
| CustomerRiskMapperTest | 7 |
| AiRiskAnalysisControllerTest / CustomerRiskToolTest | 60 / 4 |
| AiAnalysisServiceTest / ControllerTest / RoutingControllerTest | 83 / 11 / 38 |
| AiCustomerRiskControllerTest | 42 |
| S1 Policy / Evaluator / Service | 21 / 17 / 8 |

前端 node --test tests/ai-analysis.test.mjs：5 项通过。普通沙箱执行虽然 5 项通过，但 Vite 后台缓存更新仍报 EPERM，提升执行权限后原命令再跑 5 项通过、没有 Vite 缓存错误；没有修改前端源码。

首轮新增建议测试有 1 个范围基线不一致断言和 6 个 Mockito 覆盖桩时误触原 Answer 的异常，已将比较输入统一为 SELF、覆盖桩改用 doReturn/doThrow 后通过；没有删减安全断言。初次日志不作为通过证据。

真实 Qwen（包括 B2 可选冒烟）、MySQL 集成、容量/性能验收均未执行；不调用外部模型、不读写实际数据库。外部集成类明确排除，不计入上述通过数或跳过数。离线日志在系统临时目录 crm-b3-compile.log、crm-b3-initial.log、crm-b3-focused.log、crm-b3-full.log、crm-b3-final.log，未加入版本管理。

## 10. 最终验收与后续

| 验收问题 | 结论 |
| --- | --- |
| 基于 Java 可信事实生成 AI 建议 | 是，Qwen 选择动作，Java 模板生成最终建议 |
| 只在 SUGGESTIONS 生成 | 是，LIST/REASON 无建议调用，S2-A 不接入 |
| 当前页客户正确对应 | 是，严格别名集合并由 Java 映射 ID，不能只凭模型数组位置 |
| 防止改写等级/原因/指纹 | 是，模型只选择白名单动作，原可信响应字段不变 |
| 严格 JSON 和客户引用 | 是，独立 Java 校验并拒绝整页非法输出 |
| 建议前后授权和对象复核 | 是，短事务重新授权加当前页批量可见性/归属检查 |
| 建议失败降级、授权失败整体拒绝 | 是，模型失败后仍检查权限，绝不夹带旧客户结果 |
| 核心风险查询最多一次 | 是，建议无核心聚合依赖，只有额外授权复核 |
| V1 兼容 | 是，成功 JSON、双权限、两次模型及统计 Tool 保持 |
| S2-A 兼容 | 是，原合同与建议未就绪状态保持 |
| 调用次数/超时/容量 | 已限制；55 秒为软预算，真实端到端 SLA 未验收 |
| 风险聊天默认关闭 | 是，未自动开启开关 |
| 后端完整离线/打包 | 通过，最终数量及产物见下方完成检查 |
| 真实 Qwen/MySQL | 未执行，不声称真实联调通过 |
| 是否可进入 B4 | 人工验收后可进入前端集成；本次到 B3 停止 |

B4 需按新的 AVAILABLE 项目及来源渲染建议、提供人工核实提示，兼容 PARTIAL/UNAVAILABLE/NOT_APPLICABLE，处理分页、原因、指纹过期及整体拒绝，并核对前后端超时。单机限流不等于多实例配额；真实模型质量、容量和数据库并发需独立验证。

未修改数据库 Schema、阈值、角色权限矩阵或前端页面；未创建新自然语言入口、复杂 Agent 框架或自动写操作。工作区保留原 B1/B2 与本次 B3 的未提交改动，未暂存、commit 或 push。

## 11. 完成检查

最终完整离线 package 命令退出码 0：**676 项测试，0 failures、0 errors、0 skipped**。已生成 backend/target/crm-backend-0.1.0-SNAPSHOT.jar，包含生产编译、全部测试源码编译、离线回归和 Spring Boot 打包。

git diff --check、新增文件尾随空白、B3关键路径及新文档引用检查通过。Git 仅有原 Windows LF/CRLF 换行策略提示。最终工作区共 22 个已跟踪修改文件和 34 个未跟踪文件（逐文件计数，包含原 B1/B2 改动），无暂存、提交或推送；前端无源码改动。

B3代码与离线验收完成，等待人工验收；真实环境、性能及并发边界仍按第8–10节列为限制，不宣称已完成生产联调，不自动进入B4。
