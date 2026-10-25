# CRM AI Agent V2 S2-B5 单客户 AI 跟进建议实施报告

日期：2026-10-25。结论：代码及离线验证完成；使用真实 Spring Boot/MySQL/Qwen 的销售经理接口联调已通过，含两名客户及非首页客户建议。销售人员账号、真实浏览器及故障/并发变更矩阵尚未完整验收，不以局部通过代表全部通过。

## 1. 需求与执行前审查

任务依据：`task/1025/4-CRM-AI-V2-S2-B5-单客户AI跟进建议.md`。执行前 git status --short 为空、git diff --check 通过，B4 已提交，基线为 `576e2f5`。任务说明 B4 已验收；旧 B4 报告保留当时的离线结果及限制，本轮不重写历史记录。

已核对前后端规范、V2 PRD、架构、S2-A/B1/B2/B3/B4 报告及实际 Controller/DTO/Router/Tool/Service/Evaluator/Mapper/Assembler/VO 与前端实现。现有 REASON 已支持在授权 SQL 内按显式 ID 查询；原 SUGGESTIONS 只接受清单。实现前已说明文件范围和兼容方案。

产品决策：高/中关注卡片提供单客户建议；低关注、观察中、数据待核验不生成建议。批量按钮保持隐藏，后端批量协议与直接自然语言批量响应兼容。不修复20/50条批量建议失败，不写客户或跟进数据。

## 2. 请求协议

唯一入口仍为 `POST /api/ai/analysis`，Controller 和公共双权限不变。可信 Router 决定 REASON/SUGGESTIONS，ID 只来自 HTTP 显式选择，不由模型生成。

```json
{"question":"请为本人范围内我选中的单个客户生成跟进建议。","customerId":"2300000000000000001"}
```

以上 ID 为合成示例。请求只包含 question/customerId，不卡在列表第一页，不携带来源整页指纹、客户事实、角色或可信范围。

| 可信意图与字段 | 处理 |
| --- | --- |
| LIST + customerId | 422，保持原约束 |
| REASON + customerId | 原协议不变，必须显式ID |
| SUGGESTIONS + customerId | B5单客户，拒绝显式page/size及未知字段 |
| SUGGESTIONS + 无customerId | 保持B3批量协议，不因缺少ID拒绝 |
| 单客户 + 非默认模式/客户等级筛选 | 422，不静默忽略冲突条件 |

ID 必须是正整数规范十进制字符串，无前导零，且不超过 Long.MAX_VALUE。null、数字类型、空串、负数、非数字、溢出均拒绝。自然语言中可识别的显式 ID 与选择冲突时拒绝。AiAnalysisRequest 不自动补分页字段；单客户明确只接受 ID 和可选预期单客户指纹，故无需默认分页兼容例外。

## 3. 实际修改文件

| 文件 | 职责 |
| --- | --- |
| backend/src/main/java/com/company/crm/service/impl/AiRiskAnalysisServiceImpl.java | 单客户协议、复用定向查询、适用性、模型后事实复核、安全耗时日志 |
| backend/src/test/java/com/company/crm/controller/AiRiskAdviceControllerTest.java | 新增45项单客户合同、权限、事实、建议及兼容测试 |
| frontend/src/utils/riskAnalysis.ts | 适用状态及最小单客户请求构造 |
| frontend/src/utils/aiAnalysisResponse.ts | SUGGESTIONS单客户规范查询/page=null/唯一记录校验 |
| frontend/src/types/aiAnalysis.ts | 新增single-suggestions关联操作类型，复用已有查询联合类型 |
| frontend/src/components/ai-analysis/useAiAnalysis.ts | 单客户预期ID/意图/范围核验、按客户去重、并发、显式重试 |
| frontend/src/components/ai-analysis/RiskAnalysisResult.vue | 高/中关注卡片按钮及逐客户进行中状态 |
| frontend/src/components/ai-analysis/RiskAdvicePanel.vue | 不适用状态显示后端安全业务说明 |
| frontend/src/views/analysis/AiAnalysis.vue | 单客户事件、关联消息标注及建议降级显式重试 |
| frontend/tests/ai-risk-analysis.test.mjs | 新增9项单客户交互/响应/错误/并发测试，保留原32项 |
| docs/ai-agent/AI_AGENT_V2_ARCHITECTURE_DESIGN.md | 当前协议和查询次数说明 |
| docs/ai-agent/AI_AGENT_V2_S2_B5_IMPLEMENTATION_REPORT.md | 本报告，新增 |

未修改 Controller、结构化DTO、数据库Schema/SQL、权限矩阵、S1阈值、建议Prompt/白名单/Java模板/预算配置及历史报告。

## 4. 实际调用链与单客户查询

统一 Controller → AiAnalysisServiceImpl → AiIntentRouter → 风险开关 → V2 AccessPolicy → 单客户验证 → 请求级零参数 CustomerRiskTool → CustomerRiskServiceImpl 的 REASON 底层定向查询 → Evaluator → Assembler → 建议前授权/可见性复核 → RiskAdviceService → 建议后授权/可见性复核 → 定向事实复核 → 返回。

内部 REASON 是共享查询操作，不是返回 REASON HTTP 响应后再转建议。输出 action/intent 始终为 SUGGESTIONS/RISK_SUGGESTIONS。Mapper 的 eligible CTE 在授权条件内附加 `c.id = ?`，跟进聚合只针对目标，不扫描列表找ID；无需新Mapper或整页查询。

Tool 本身仍只能执行一次。为满足 B5 模型等待后的事实有效性，单客户可生成建议时另有一次服务级按ID事实复核；不是第二次Tool调用，也不增加无关客户聚合。原批量仍一次核心聚合，两次对象复核。

## 5. 权限与对象边界

沿用数据库实时三权限、单销售角色、范围策略。经理默认DEPT、可显式SELF；销售人员SELF；管理员和多角色拒绝。合法ID不代表授权，对象查询继续过滤已分配、有效负责人、POTENTIAL/ACTIVE、未删除与当前范围。

初次不存在/不可见/公海/删除/停用等均遵循原404/RISK_CUSTOMER_NOT_FOUND，不泄露存在性。单客户可生成建议时，前后两次可见性投影检查保持B3行为；模型返回后重新执行定向查询并按原单客户指纹比较。权限/上下文变化403；对象消失、风险事实或规则变化409/RISK_RESULT_STALE。即使模型失败也执行这些复核，403/409不降级。

LOW、OBSERVING、DATA_REVIEW保留当前唯一记录，返回SUCCESS/NOT_APPLICABLE，无建议项、不调用建议模型；完成授权/可见性复核。数据质量异常不冒充高关注。

## 6. 建议生成和响应

复用B3匿名单客户RiskAdviceInput（alias=r1）、严格JSON、客户覆盖、动作与reasonKeys校验、Java模板和真实ID关联。模型输入无真实ID/姓名/联系方式/地址/用户问题或授权信息。正常最多分类、Tool选择、建议三次模型调用；不适用只前两次。无自动重试或客户写入。

成功响应沿用RiskAnalysisVO根结构：

```json
{
  "status":"SUCCESS",
  "intent":"RISK_SUGGESTIONS",
  "normalizedQuery":{"scopeIntent":"SELF","customerId":"2300000000000000001"},
  "page":null,
  "advice":{"status":"AVAILABLE","source":"QWEN_ACTIONS_JAVA_TEMPLATE","unavailableCode":null,"message":"辅助建议仅供参考，请人工核实后决定是否执行。","items":[{"customerId":"2300000000000000001","actions":["VERIFY_CURRENT_NEEDS"],"reasonKeys":["NEVER_FOLLOWED"],"suggestion":"建议先核实客户当前需求，再由销售人员决定联系安排。"}]}
}
```

这是合成的响应字段节选，完整响应还含answer、analysisContext、唯一records及resultFingerprint，不新增包装。模型失败仍为PARTIAL/UNAVAILABLE并保留该客户事实；不适用SUCCESS/NOT_APPLICABLE。输出身份不由模型选择。

## 7. 指纹语义

不改现有算法或批量合同。单客户使用已有ReasonQuery(scopeIntent/customerId)、单记录、total=1、规则版本和权限上下文生成摘要，故不同于整页ListQuery指纹；intent/advice不参与摘要。可复用同条件REASON返回的单客户指纹，但不是其响应作为模型输入。

前端按钮不上传整页指纹。API若传expectedResultFingerprint，必须是该单客户结果指纹；比较不匹配409，绝不静默忽略。测试证明正确单客户指纹通过、整页指纹拒绝且不调用建议模型。返回前事实复核同样用初查单客户指纹比较，分析时间本身不参与比较。

## 8. 前端交互与状态

只对当前NORMAL/HIGH或MEDIUM显示“获取跟进建议”。请求使用当前已校验记录的字符串ID，question保留来源范围语义，后端仍重新分类授权。新关联消息展示客户、当前事实及RiskAdvicePanel建议，不覆盖来源列表、不以名称匹配。返回必须是SUGGESTIONS、单客户规范查询、唯一记录且ID一致；错误ID/批量形状/错误意图不展示。

同一来源链同一客户进行中去重，不同客户可并发，乱序各自回填；进行中按钮按客户禁用。UNAVAILABLE和请求失败允许显式重试，不无限重试。403/404/409隐藏相关旧事实/建议；权限变化、卸载取消与迟到响应保护继续复用。判定依据仍本地展开，批量按钮仍隐藏。

## 9. 兼容性

V1统计响应/双权限/原测试不变。无customerId的批量SUGGESTIONS原流程及全部B3测试保留。REASON、LIST、Tool单次调用、S2-A严格结构化DTO和未接入建议状态不变。未新增Controller、页面、全局状态框架或业务写操作。

## 10. 离线验证

前端：`node --test --test-concurrency=1 tests/ai-analysis.test.mjs tests/ai-risk-analysis.test.mjs`，**41项通过、0失败、0跳过**；原32项保留。`npm.cmd run type-check`、`npm.cmd run build`通过，构建退出码0；已有大于500KB包体提示保留。Vite测试/构建使用获准权限运行，未改系统策略。

后端：先执行AiRiskAdviceControllerTest、AiRiskAnalysisControllerTest、AiAnalysisRoutingControllerTest、CustomerRiskControllerTest定向回归通过；补齐边界测试后执行：

```powershell
mvn -o -q -f backend/pom.xml '-Dtest=*,!QwenLiveIntegrationTest,!QwenAnalysisLiveIntegrationTest,!CustomerRiskMapperMySqlTest' package
```

最终**43个测试类、721项、0失败、0错误、0跳过**，Spring Boot打包通过。只计当前测试源码对应报告，排除遗留重命名前XML和外部集成类。AiRiskAdviceControllerTest共71项，其中原26项、新45项。日志位于系统临时目录crm-b5-backend-full.log、crm-b5-frontend.log、crm-b5-types.log、crm-b5-build.log，不提交。

新增覆盖单客户合法/非法ID、分页/事实/权限字段冲突、经理默认/DEPT/SELF与人员SELF、管理员/多角色/缺权限、定向ID查询与匿名输入、风险解除/特殊状态、对象缺失、模型期权限/归属/风险变化、无效JSON/动作/原因/别名/缺项/超时、模型失败后事实变化、指纹隔离。已有Mapper测试证明SQL的对象过滤与按ID授权条件；这些是离线证据，不等于真实删除/公海/并发数据库验收。

## 11. 真实联调

本轮已读取用户提供的`docs/development/TEST_ACCOUNT.md`。后续用户明确提供Windows用户级环境变量名CRM_SALES_MANAGER、CRM_SALES_MANAGER_SCREAT作为销售经理账号和密码来源。沙箱进程未继承这些变量，获准在用户级读取后仅在内存使用，不输出值或Token。真实登录及/auth/me核验确认单一SALES_MANAGER角色、ai:analysis/customer:list/follow:list三权限齐全。

使用既有本地测试数据库配置启动独立18085端口实例，profile=local,ai，仅该进程临时开启风险聊天，管理员初始化关闭；未修改配置文件、用户环境、账号权限或客户数据。启动过程中发现只启用ai未激活本地数据源配置，且JWT_EXPIRE未继承导致校验失败，后按项目脚本使用local,ai，并仅为临时进程指定jwt.expire=7200后成功。未把失败启动记为通过。联调完成已核验PID/启动时间并停止本轮临时实例，未停止其他服务。

真实接口最终矩阵（page size=1用于明确区分第一页和第二页，不改变后端最大页大小）：

| 场景 | 结果 | HTTP往返耗时 |
| --- | --- | ---: |
| 经理默认关注LIST | 200/SUCCESS，实际DEPT，返回1条 | 2026ms |
| 第一名客户单客户建议 | 200/SUCCESS/AVAILABLE，page=null，唯一记录及唯一建议ID均等于选择ID | 3217ms |
| 同条件第二页LIST | 200/SUCCESS，取得不同客户 | 2413ms |
| 第二页客户单客户建议 | 200/SUCCESS/AVAILABLE，唯一记录/建议严格对应第二名客户，无需位于第一页 | 3051ms |
| 单客户建议携带page | 422，拒绝非法组合 | 1141ms |
| 已在DEPT可见的客户改用SELF建议请求 | 404，不泄露SELF范围外对象 | 1915ms |
| LIST携带customerId | 422，原约束保留 | 989ms |
| 不存在或不可见ID的单客户建议 | 404 | 1714ms |
| V1本人客户总量 | 200/CUSTOMER_TOTAL | 1893ms |
| 经理显式SELF关注LIST | 200/SUCCESS，实际SELF | 1725ms |

先前一轮两名客户也均AVAILABLE，往返3586ms和3330ms；最终表采用补充范围隔离后的完整一轮。每条建议均经过真实后端的别名、白名单动作、原因集合和Java模板验证，未将模型原文或客户名称/ID写入报告。脱敏证据存系统临时目录crm-b5-live-results.json、crm-b5-live.out.log；临时检查脚本已移除，无Token持久化。

尚未执行：销售人员账号登录（本轮只获经理凭证）、真实浏览器点击与页面验收、真实运行时撤权/客户变更竞态，以及真实模型失败的受控降级。未修改生产/测试客户来制造故障，未调整B3超时或白名单；上述故障边界仍只有离线测试证据。10条真实接口请求不是全部角色/浏览器矩阵。

## 12. 性能与已知限制

新增安全日志仅记录初次定向查询elapsedMs、single标记和定向事实复核elapsedMs；沿用B3建议elapsedMs及总请求totalElapsedMs。未记录ID/姓名/原始Prompt/模型响应/凭据。最终两条真实单客户请求的阶段耗时如下；不是Mock数据，也不是容量或生产SLA：

| 单客户样本 | 初查 | 建议模型阶段 | 定向事实复核 | 服务端总请求 | HTTP往返 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 第一页客户 | 7ms | 1249ms | 6ms | 3208ms | 3217ms |
| 第二页客户 | 6ms | 1141ms | 4ms | 3042ms | 3051ms |

初查日志包含进入核心服务后的权限查询/事务/评估，不等于裸SQL耗时；建议日志包含传输及解析。分类、Tool选择及每条权限SQL未分别计时，不可用上述数值推断其独立耗时。

连接2秒、建议动态读取至多8秒、55秒软预算、并发4及4000输出Token等B3保护不变，前端60秒不变。模型后定向事实复核增加必要的一次短查询，最终SQL/网络硬耗时、模型准确率、负载仍需真实验证。采样复核不能消除最后检查后到响应送达间的TOCTOU/ABA风险，不提供历史快照。来源列表可能已过时，单客户使用当前事实；接口允许可选单客户预期指纹，前端不把来源整页指纹误用为单客户指纹。

批量20/50条不稳定问题未处理。用户输入仍可能被Router拒绝或分类不一致，前端关联操作严格核对而非静默接受。

## 13. 最终结论与下一步

代码、离线回归、前端构建和后端打包通过；真实经理接口链路、两名客户/非首页建议、DEPT/SELF隔离及V1/LIST兼容已通过。完整B5验收仍需销售人员账号、真实浏览器及受控故障矩阵。最小下一步为使用销售人员测试凭证补齐角色与浏览器验收；不修改客户数据来伪造通过。

## 14. Git与范围检查

本轮11个已跟踪修改、1个新报告，未暂存、commit或push。git diff --check通过；数据库、规则、权限、配置、结构化接口及历史报告未改动。等待人工验收，不自动进入后续阶段。

## 15. B5 收尾：单客户建议页面去重与布局优化

日期：2026-10-25。任务`task/1025/5-CRM-AI-V2-S2-B5-收尾-单客户跟进建议页面去重与布局优化.md`提供新的真实浏览器观察：单客户建议已经生成成功，但answer、风险卡片、建议面板重复展示客户身份及风险原因，技术上下文占据过多空间。这是任务提供的人工浏览器观察；本轮没有重新执行浏览器验收，不与第11节本人执行的真实接口测试混淆，也不将其扩大为全部角色/故障矩阵通过。

执行前工作区已有11个已跟踪修改、1个新报告，git diff --check通过。保留全部B5未提交改动，本轮只增量修改5个文件：

| 文件 | 收尾变化 |
| --- | --- |
| frontend/src/components/ai-analysis/RiskAnalysisResult.vue | 同时核对原请求customerId、RISK_SUGGESTIONS意图、规范查询ID、唯一记录ID及page=null后使用紧凑布局；本地默认折叠分析详情 |
| frontend/src/components/ai-analysis/RiskAdvicePanel.vue | 新增仅展示建议原文和人工核实提示的compact展示模式；保留批量展示 |
| frontend/src/views/analysis/AiAnalysis.vue | 把已有exchange.request作为布局校验上下文传入，不改变HTTP请求 |
| frontend/tests/ai-risk-analysis.test.mjs | 新增4项布局/状态/折叠/失效测试，真实组件客户端模板交互测试覆盖建议面板 |
| docs/ai-agent/AI_AGENT_V2_S2_B5_IMPLEMENTATION_REPORT.md | 本节 |

单客户主区域依次为客户名称与关注标签、客户等级/状态/跟进间隔、唯一风险判定说明、建议原文列表和人工核实提示。不再渲染顶部answer和完整LIST卡片后追加另一套建议卡片；原始响应及风险事实仍保留在消息结果中。原因正文按完整文本去重，详细区域只展示其代码及证据，不重复同一句原因。建议不按标点拆分或改写，直接使用后端suggestion，一条项目只渲染一次；不使用v-html或将ID转换为Number。

“分析详情”默认不渲染内容，点击只改变组件内局部状态，不发请求或新增消息。展开后保留客户编号/字符串ID、实际与最大范围、分析时间/时区、规则版本、查询口径、有效跟进时间、阈值/超期/原因证据、数据质量状态与重新查询/快照限制。客户ID仅在展开详情出现；客户名只在主标题显示。结果/请求变化重置折叠状态，权限失效后沿用B4/B5移除受保护结果和建议。

从未跟进明确显示建档间隔，观察中及数据待核验保留显著解释，缺失时间不编造。AVAILABLE显示原文建议与人工提示；UNAVAILABLE保留当前事实及不可用提示，原显式重试能力不变；NOT_APPLICABLE保留当前事实及后端不适用说明。只有一条记录的批量SUGGESTIONS仍使用批量布局，LIST和REASON不进入紧凑分支；V1统计组件、请求与类型完全不改。

本轮实际验证：前端全套相关测试**45项通过、0失败、0跳过**（保留原41项，新增4项）；npm.cmd run type-check与npm.cmd run build通过，构建退出码0。测试验证去重、原文完整、默认隐藏ID/规则、展开收起无请求、特殊状态、三种建议状态、批量/LIST/REASON模式区分及权限失效。原V1五项测试继续通过。构建仍有已有主包超过500KB的提示，未扩大范围做依赖拆包。

本轮未改Java、API/JSON合同、Prompt、模板、阈值、数据库、权限矩阵或请求/响应校验器；第10节721项后端测试及第11节真实接口记录为前一轮证据，本轮未重跑或冒称新结果。最终git diff --check通过，Git仍为11个已跟踪修改、1个新报告，未暂存、commit或push。

剩余事项：优化后的真实浏览器视觉复核、销售人员账号及受控故障/并发变更矩阵尚未在本轮验证；批量20/50条稳定性问题仍保留。等待人工审查，不自动进入后续阶段。
