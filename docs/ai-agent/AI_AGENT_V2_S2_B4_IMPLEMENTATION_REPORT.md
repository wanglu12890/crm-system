# CRM AI Agent V2 S2-B4 实施报告

日期：2026-10-25。范围：统一聊天前端集成及离线验收；真实环境联合验收未完成，不能将 Mock 结果视为真实 Qwen/MySQL 通过。

## 1. 执行前审查与实际修改

执行前 git status --short 为空，git diff --check 通过；Controller 已完成 CustomerRiskController 重命名，B1/B2/B3 报告已归档到 docs/historical/。已核对 PRD、当前架构、S2-A 与归档报告、前端规范、原页面/API/Axios/权限及后端真实 DTO/VO 和风险编排。

原 AI 页面经默认 /api 基址调用 POST /ai/analysis；V1 为 answer/intent/metrics/analysisContext，V2 为 status/intent/answer/analysisContext/normalizedQuery/page/records/resultFingerprint/advice。沿用后端字段，没有新增 JSON 包装或修改后端服务。

修改文件：

| 文件 | 变化 |
| --- | --- |
| frontend/src/types/aiAnalysis.ts | 统计/风险联合类型、风险记录/建议/查询类型、消息请求与关联状态 |
| frontend/src/api/aiAnalysis.ts | 保持统一 URL 与 60 秒超时，响应先以 unknown 接收，交给运行时校验 |
| frontend/src/utils/aiAnalysis.ts | 扩展错误码、404/过期反馈，保留统计复制等原能力 |
| frontend/src/views/analysis/AiAnalysis.vue | 按 intent 渲染两种结果，关联消息、独立错误/加载/重试及请求耗时 |
| frontend/src/components/ai-analysis/AiChatInput.vue | 请求期间可编辑另一问题，不重复发送进行中的同一问题 |
| frontend/src/components/ai-analysis/AiWelcomePanel.vue | 保留原示例并增加风险/建议示例，无前端模式选择 |
| docs/ai-agent/AI_AGENT_V2_ARCHITECTURE_DESIGN.md | 仅补充 B4 实际行为及待联调状态 |

新增文件：

| 文件 | 职责 |
| --- | --- |
| frontend/src/utils/aiAnalysisResponse.ts | 关键响应字段、字符串 ID、分页、建议客户引用与未知状态防护 |
| frontend/src/utils/riskAnalysis.ts | 中文状态映射、原因/建议/分页请求构造及规范条件比较 |
| frontend/src/components/ai-analysis/useAiAnalysis.ts | 页面局部组合式状态：消息/会话、请求绑定、取消、重复操作与结果失效管理 |
| frontend/src/components/ai-analysis/RiskAnalysisResult.vue | 风险卡片、范围/时间/原因、分页及关联操作 |
| frontend/src/components/ai-analysis/RiskAdvicePanel.vue | 建议按客户 ID 对应、来源与人工核实、降级及不适用状态 |
| frontend/tests/ai-risk-fixtures.mjs | 独立合成响应，不含真实客户或凭据 |
| frontend/tests/ai-risk-analysis.test.mjs | B4 协议、组件 SSR 与请求/并发状态离线测试 |
| docs/ai-agent/AI_AGENT_V2_S2_B4_IMPLEMENTATION_REPORT.md | 本报告 |

合计 7 个已有文件修改、8 个新增文件；其中前端 13 个文件、文档 2 个文件。未改 Java、数据库、配置、权限矩阵或历史报告；原 AiAnalysisResult、侧栏和认证 Store 继续复用。

## 2. 统一入口与类型适配

所有自然语言输入、分页和重试均调用 analyzeCustomers → 原 Axios 实例 → POST /api/ai/analysis。卡片“查看判定依据”仅展开已有证据，不请求接口。没有调用 /customer-risks/analyses，没有直接调用 Qwen，也没有新页面、Controller 或前端关键词意图分类。

根据后端 intent 区分 CUSTOMER_TOTAL/CUSTOMER_GRADE_DISTRIBUTION 和三个 RISK 意图；不根据 answer 或输入文本猜类型，不把 RiskAnalysisResult 强转为统计响应。新增类型与解析代码无 any。

对关键上下文、真实字符串 ID、记录集合、分页/总数、原因证据和建议项目做运行时验证。V1 ALL 时间窗口允许原全局 NON_NULL 导致的字段省略，并在前端归一化为 null。风险 ID 保留字符串，拒绝数字、前导零及 Long 溢出。分页每页不超过 50，结果客户 ID 去重，REASON 必须对应所请求单对象的有效响应。

未知关注状态采用中性标签；未知或非法建议状态/客户引用/动作安全显示建议不可用，保留已验证风险记录。无法验证的基础响应作为协议错误显示，不渲染伪造指标或原始 JSON。

## 3. LIST 与风险卡片

显示客户名称/编号/字符串 ID、客户等级、关注等级或特殊状态、有效跟进时间/间隔、Java 原因和证据、分析时间、实际/最大范围、查询模式/客户等级条件、规则版本及分页。

HIGH/MEDIUM/LOW 使用高/中/低关注文案；OBSERVING 显示观察中，DATA_REVIEW 显示数据待核验，不用绿色低关注或红色高关注冒充未知/特殊状态。客户 A/B/C/D 与风险等级分开显示，不重新计算任何阈值，不称作客户流失预测。原因、建议均使用 Vue 文本渲染，没有 v-html。

空页区分无匹配和超出末页但 total 仍非零；不把分页列表再次做前端业务过滤或切页。每条风险消息持有自己的结果和页码，翻页生成关联消息，原列表保持原页和原时间。

## 4. 判定依据与 REASON 兼容

浏览器验收发现原“查看原因”重复请求 REASON 并新增内容重复的聊天消息。本次改为“查看判定依据”：在当前卡片本地展开/收起已有字段，不发送 HTTP、不调用 Qwen、不新增聊天消息，也不重新判级。

卡片主体保留状态和简短时间摘要，详细区域显示客户等级、关注状态、最近有效跟进、间隔、阈值、后端返回的超期天数、原因和证据，以及适用的数据质量状态。原因正文只在详细区域展示一次；从未跟进、观察中、异常时间及缺失可选字段使用真实字段和缺省提示，不推算新事实。每卡独立展开；结果替换时重置，分页新消息独立维护；权限失效后组件随 result 移除，不能保留受保护证据。

后端 REASON 意图和显式 customerId 合同未改；原请求构造及协议测试保留，但页面不再绑定卡片 REASON 事件。用户直接输入原因问题仍走统一入口，缺对象时继续显示 422/RISK_CUSTOMER_SELECTION_REQUIRED，不猜测 ID。

## 5. SUGGESTIONS

普通自然语言建议请求仍由 Router 识别，RiskAdvicePanel 继续兼容已有及直接自然语言 SUGGESTIONS 响应。根据本次任务提供的浏览器实测记录，批量建议 10 条成功，20/50 条出现 UNAVAILABLE；这是人工观察，不是本轮重新执行的结果，也不足以确定失败根因。

当前页“为当前页获取跟进建议”主动按钮及页面事件绑定已移除，批量稳定性列为已知限制。B3 后端批量实现、白名单、Java 模板、统一 API、原请求构造和测试均保留；不阻断用户直接输入建议问题。单客户建议计划在 S2-B5 实现，本轮不扩展协议或增加单客户入口。

- AVAILABLE：按 customerId 对应客户展示后端 suggestion 及可信原因，说明 AI 选择动作、系统模板生成，提示人工核实。不把建议绑定到数组下标，不提供自动写入或发送操作。
- UNAVAILABLE：保留真实风险卡片，提示风险分析完成、建议暂不可用，不将整次查询伪装失败或自动重试。
- NOT_APPLICABLE：正常显示空页和无需建议，不显示模型故障。

指纹仅在当前页 SUGGESTIONS 请求携带，使用原值，不参与前端授权。RISK_RESULT_STALE 整体提示重新查询并隐藏相关旧记录。客户端未知建议状态的安全降级是展示层保护，不声称后端模型产生了成功建议。

## 6. 分页与查询语义边界

分页严格复用来源请求的原 question，发送新 page/size，移除旧页指纹。默认及边界沿用后端 1/20、最大 50；不调用 S2-A，不虚构会话或服务端快照。

当前统一 API 每次仍重新执行自然语言分类和风险查询。对关联操作返回的 intent、scopeIntent、queryMode、客户等级、page/size（REASON 为客户 ID）与预期规范条件比较；不一致显示“本次结果与原查询条件不一致，请重新查询”，不把改变条件后的结果冒充同一清单翻页。

限制：前端检查可防止静默接受条件变化，但不能让模型分类变成确定性协议；无法保证跨页固定快照，不能将 normalizedQuery 作为授权执行计划上传。若来源是建议请求，翻页也会再次请求新页建议，UI 已说明。没有发现必须修改 B1～B3 合同才能继续实现的阻塞缺口。

## 7. 权限、错误及旧结果处理

页面路由/菜单继续只要求原公共双权限。风险按钮展示同时使用现有三权限检查和单销售角色信息，不因管理员菜单可见而认定其获得风险权限。前端不决定实际授权，所有操作仍由后端复核。

| 场景 | 实际处理 |
| --- | --- |
| 401/403 | 显示相应拒绝；隐藏缓存风险结果、标记失效并阻止迟到风险响应回填；保留不相关统计历史 |
| 404 | 客户不存在/不可见/不适用；隐藏相关风险消息链的旧详情 |
| 409/RISK_RESULT_STALE | 相关消息链失效，要求重新查询原问题，不继续使用旧客户选择 |
| 其他 409 | 显示冲突，不一概当作风险指纹过期 |
| 422 | 区分缺少客户选择、能力未开放及不支持条件 |
| 502/503 | 模型协议/服务不可用；错误消息无伪造响应，可显式重试 |
| 网络/超时 | 提供有限等待、错误反馈和显式重试，不无限请求 |

403 不当成 PARTIAL。失效消息不再保留可操作的 result；“重新查询原问题”创建新消息，不重放失效 customerId。权限/角色变化隐藏旧风险结果，登录主体变化重置会话；所有会话只存页面内存，不持久化风险数据。

稳定已知业务码映射到本地提示，非业务异常不直接显示 error.message；后端 detail 仅在长度受限、未包含明显技术/敏感诊断标记时作为既有兼容反馈，其他情况使用通用提示。未输出 SQL、Prompt、Token 或异常堆栈。

## 8. 并发、取消与超时

独立问题可并发；同一进行中问题和同一风险消息链的关联操作去重/串行保护。每个请求绑定发起时的 session/exchange，不用全局“最后一个响应”覆盖当前消息；旧请求成功也不能清空新编辑的草稿。

AbortController 管理每个请求，页面卸载取消；响应保存前检查卸载、失效标记以及风险安全代次，防止并发拒绝后旧风险响应再次填回。失败后恢复操作，重试为新关联消息，不改写历史结果。

AI Axios 超时仍为 60000ms，通用 Axios 仍 15000ms；没有为凑合三次调用增加超时。页面显示较长分析的加载反馈，每条完成请求记录 performance.now 测得的总耗时并显示秒数。浏览器取消不能保证服务器已停止数据库或模型调用，已有 B3 55 秒软预算也不是硬端到端 SLA。

真实联调未进行，因此没有提供实际分类/Tool/风险查询/建议/复核分段耗时或声称请求必在 60 秒完成。B3 日志已有建议耗时及风险链路总耗时，但不足以证明所有独立网络/SQL阶段耗时；后续应先采集，再决定最小超时或性能优化。

## 9. 初次集成前端验证（收尾结果见第13节）

最终命令：

```powershell
node --test --test-concurrency=1 tests/ai-analysis.test.mjs tests/ai-risk-analysis.test.mjs
npm.cmd run type-check
npm.cmd run build
```

前端离线 **28 项全部通过**（原 V1 5 项、B4 新增 23 项），类型检查与生产构建通过。测试使用原 Node test + Vite SSR + Vue renderer/SSR + Axios 合成 adapter，不新增框架，不调用真实接口/模型。

覆盖联合响应、未知/非法建议、真实字符串 ID、四种关注/特殊状态、空页、三种建议状态、来源与人工提示、显式原因 ID、分页保留原问题/不带旧指纹、查询漂移、403/404/409失效、422/502/503、网络超时、重试、连续消息、乱序响应、重复操作、页面卸载、权限/角色/主体变化。测试拦截器对所有聊天请求断言 URL 恰好 /ai/analysis，禁止通过结构化入口完成聊天。

合成 SSR/请求状态测试不是浏览器真实页面与后端端到端验收，也没有以合成测试耗时替代真实业务延迟。

过程记录：初次 npm 命令被 PowerShell npm.ps1 执行策略阻止，改用 npm.cmd 后类型检查通过。普通沙箱中的 Vite 测试缓存及构建配置临时文件写入报 EPERM，使用获准的提升权限原命令完成验证；未修改系统执行策略或项目配置。生产构建仍有主包大于 500KB 的体积提示，构建成功，本阶段未扩大到全局依赖拆包重构。

## 10. 后端回归

已执行直接相关回归：

```powershell
mvn -o -q -f backend/pom.xml '-Dtest=AiAnalysisServiceTest,AiAnalysisControllerTest,AiAnalysisRoutingControllerTest,CustomerRiskControllerTest,AiRiskAnalysisControllerTest,AiRiskAdviceControllerTest,CustomerRiskToolTest' test
```

该轮 **264 项通过**，覆盖 V1、B1 路由、已重命名结构化 Controller、风险查询、建议及 Tool。随后执行完整离线套件并打包：

```powershell
mvn -o -q -f backend/pom.xml '-Dtest=*,!QwenLiveIntegrationTest,!QwenAnalysisLiveIntegrationTest,!CustomerRiskMapperMySqlTest' package
```

本轮 **43 个测试类、676 项，0 failures、0 errors、0 skipped**，Spring Boot 打包成功。计数仅使用当前实际测试源码对应报告，排除历史遗留的 AiCustomerRiskControllerTest XML 及外部集成类，避免把旧报告叠加进本轮。B4 未修改任何后端源码、配置或测试。

## 11. 初次集成真实环境前置检查与未执行项

以下保留初次集成时的检查记录；后续任务提供的浏览器观察见第5、13节，不将其等同于完整真实验收。检查结果只记录配置是否存在，不打印任何值：

- DASHSCOPE_API_KEY 已配置；这只能证明环境变量存在，不能证明当前服务连接或真实风险建议联调成功。
- 专用测试库 CRM_RISK_TEST_JDBC_URL/DB_USER/DB_PASSWORD、RUN_CUSTOMER_RISK_MYSQL 未配置，未将已有开发库擅自视为隔离测试库。
- 测试账号文档存在，但尚未在指定测试库核验有效销售角色、权限及样本。风险聊天默认配置仍关闭，本进程未提供临时启用参数。

没有启动或修改运行环境，没有自动开启风险聊天，没有连接实际数据库或发出真实 Qwen 请求。真实 Qwen/MySQL 联调、销售经理 DEPT/SELF、销售人员 SELF、管理员/缺权限/对象越权真实验证、浏览器联调及端到端耗时均 **未执行**。

原因是受控联合测试条件尚不完整，不以 Mock、676 项离线测试或“已有 Key”冒充真实通过。当前结果也不代表生产容量验收。最小下一步是提供专用测试数据库与授权账号/样本，在受控实例临时启用风险开关后，执行任务第16节矩阵并记录真实耗时；默认配置应继续 false。

## 12. 最终验收结论

| 验收问题 | 结论 |
| --- | --- |
| 聊天始终使用 AiAnalysisController | 是，所有操作复用 POST /api/ai/analysis |
| 不通过 CustomerRiskController 绕过路由 | 是，源码与所有聊天请求离线断言确认 |
| V1 统计兼容 | 是，原统计展示/复制/双权限/请求路径和原5项测试保留 |
| LIST 展示 | 已实现并离线验证，包含中/高关注、观察中、待核验与空页 |
| 卡片判定依据 / REASON | 卡片本地展开，不请求或新增消息；后端显式 customerId 合同保留 |
| SUGGESTIONS 三种状态 | 已实现并离线验证，不编造建议或执行写操作 |
| 分页 | 已实现重新查询及规范条件检查，不承诺固定快照 |
| 指纹过期 | 已处理，隐藏相关旧结果并要求重新查询 |
| 权限拒绝 | 已处理，前端按钮不是授权，403不降级PARTIAL |
| 防止旧响应覆盖新消息 | 会话/消息绑定、失效代次与取消测试通过 |
| 前端测试/类型/构建 | 收尾32项通过，类型检查和生产构建通过 |
| 后端回归 | 初次集成定向264项及完整676项通过，打包成功；收尾未重跑 |
| 真实 Qwen | 本轮未重新执行；任务提供批量10条成功、20/50条UNAVAILABLE观察，尚非完整验收 |
| 真实 MySQL | 未执行，缺专用测试库连接参数 |
| 真实端到端超时 | 尚未采集，未声称满足60秒或生产SLA |
| API 合同缺口 | 无阻塞性缺口；模型重新分类、跨页LIVE快照及超时为已记录限制 |

代码集成和离线验收完成；**B4真实环境端到端验收尚未完成**，等待人工审查及受控测试环境。未 commit/push，不自动推进后续阶段。工作区仅包含本次前端与当前文档改动，历史 B1/B2/B3 报告保留不动。

完成检查：最终前端28项测试、类型检查及生产构建通过；git diff --check、24个当前文档相对链接和新增文件尾随空白检查通过。Git为7个已跟踪修改、8个未跟踪文件，暂存区为空；backend、database及docs/historical没有改动。构建产物和临时日志均未加入版本管理。

## 13. B4 收尾：风险卡片交互优化

日期：2026-10-25。本次执行前工作区已有7个已跟踪修改、8个未跟踪文件，git diff --check通过；保留全部B4未提交工作。本节为收尾验证记录，上文28项/676项是此前执行记录。

本轮修改5个文件：RiskAnalysisResult.vue、AiAnalysis.vue、ai-risk-analysis.test.mjs、本报告及当前架构设计文档。卡片改为本地独立展开判定依据，移除重复REASON消息入口和当前页批量建议按钮；不改RiskAdvicePanel、API、组合式请求链、类型、后端或规则。现有REASON/批量建议请求构造与离线协议测试保留，不意味着正式页面仍提供相应按钮。

任务提供的浏览器观察为：原“查看原因”重复生成内容；批量建议10条可成功、20/50条出现UNAVAILABLE。本轮未调用真实Qwen或连接数据库，未重新验证这些观察，也不判断其根因。当前仅隐藏主动批量按钮，保留直接自然语言建议和历史响应兼容；单客户建议计划在S2-B5实施。

收尾执行相关前端全套测试：**32项通过，0失败、0跳过**（原28项保留，新增4项交互测试）。测试编译真实卡片客户端模板并用Vue renderer触发点击，验证独立展开/收起、0请求/0新增消息、新结果重置、特殊状态/缺失可选字段、权限失效移除证据、无批量按钮及直接自然语言建议兼容。现有V1、LIST、显式字符串ID的REASON协议测试继续通过。

npm.cmd run type-check及npm.cmd run build通过。普通沙箱测试缓存/构建临时文件写入遇到EPERM，获准后使用原命令提升权限重跑通过，未修改系统策略或配置；主包体积提示仍为已有已知限制。未修改后端，因此收尾未重跑后端测试，也未将此前676项报告伪称为本轮结果。

仍待完成：受控真实环境的完整销售角色/权限/范围/对象边界矩阵、真实MySQL独立对账、模型失败分类及20/50条批量稳定性、真实端到端耗时与浏览器回归。局部10条成功不代表B4全部真实验收通过；批量问题在后续专项处理，本轮不放宽白名单、不增加超时。
