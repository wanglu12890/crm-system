# CRM AI Sales Analysis Agent V1 — 第一阶段需求与接口契约基线

- 状态：S1、S2-A/S2-B、C1、S3-A已完成；S3-B核心真实联调通过，阶段性验收PARTIAL
- 更新日期：2026-10-14
- 建议项目路径：`docs/ai-agent/AI_AGENT_V1_REQUIREMENTS.md`
- 范围：第一阶段最小闭环；不代表整个 V1 的所有能力已实现

## 1. 背景与目标

现有 CRM 采用 Java 17、Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL 8，以及 Vue3、TypeScript、Vite、Element Plus、Pinia。系统已有客户列表查询、RBAC、部门及数据范围控制，模拟业务数据已导入并验收。

已实现端到端核心链路：前端自然语言提问 → JWT/双权限 → Qwen结构化分类 → Java查询范围校验 → Qwen Tool Calling → Java受控统计工具 → S1/MySQL数据权限统计 → Java确定性回答 → AiAnalysisVO → 前端展示。关键统计数字不由LLM解释或改写。核心真实联调通过，仍有账号范围差异和缺权限真实账号验收待收尾，不能描述为全部验收完成。

验收核心：真实工具调用、正确的数据隔离、与 SQL 基准结果一致。合成演示数据不得表述为真实市场规律。

## 2. 用户与权限模型

### 2.1 三层权限

1. **入口权限**：已实现 `ai:analysis`，正式接口还必须同时具有 `customer:list`。前端菜单、正式路由及旧路由重定向也要求双权限，后端独立承担最终授权。
2. **业务工具权限**：第一阶段客户统计工具要求 `customer:list`；仅有 `ai:analysis` 不意味着可以查询客户统计。
3. **数据范围**：复用现有 Customer List 已实现的有效数据范围逻辑；不能由 LLM 或前端决定。

### 2.2 数据范围计算

- 只考虑当前用户**已启用且实际授予 `customer:list`** 的有效角色。
- 常规角色配置为 `SUPER_ADMIN` / `SYSTEM_ADMIN`：ALL，`SALES_MANAGER`：DEPT，`SALES_STAFF`：SELF；实际以 `DataScopeResolver` 查询有效角色所授范围为准，不能依赖模型自称或硬编码角色名判断。
- 多角色取最大有效范围：ALL > DEPT > SELF；不支持用户主动切换角色来改变授权范围。
- DEPT 仅包含当前部门，不自动包含子部门；DEPT 用户缺少部门时明确拒绝访问。
- SELF 仅包含 `customer.owner_id = 当前登录用户ID`；不包含 `owner_id IS NULL` 的公海客户。
- ALL 是只读分析可见范围，不赋予客户编辑、认领或新增跟进权限。
- 时间条件可筛选创建窗口；V1不支持主动缩小组织范围或查询指定其他人员/部门。显式要求本人、本部门、全公司时必须与当前有效范围完全一致，不能静默转换。
- 所有聚合统计同样受数据权限限制；不因只返回总数就放开授权。

### 2.3 公海及越权处理

- 第一阶段不实现专门的公海统计工具；不要将普通客户统计与公海统计混淆。
- SELF 用户询问“公海有多少客户”或跨人员统计，应明确说明当前功能/权限无法提供，**不得返回 0 伪装为实际数量**。
- 普通客户总量统计的口径必须显式说明是否仅包含 `owner_id IS NOT NULL`；第一阶段建议与 Customer List 普通客户列表一致，默认排除公海。ALL 也不代表默认把公海并入普通客户统计。
- 对于不支持的范围或问题，不应偷偷改成其他范围回答；应提示限制，必要时建议改问授权范围内的问题。
- 前端的菜单/快捷问题隐藏只是体验优化，后端必须独立执行权限验证。

## 3. 第一阶段功能范围

### 3.1 已实现后端能力

1. **客户总量**：统计当前可访问的普通客户总数。
2. **客户等级分布**：按 A/B/C/D/未评级返回数量，不额外计算占比。
3. **时间筛选**：`ALL`、`CURRENT_MONTH`、`LAST_MONTH`、`RECENT_30_DAYS`。未指定时间默认ALL，ALL仅表示全部时间，不表示全公司。

正式入口为 `POST /api/ai/analysis`，统计意图为 `CUSTOMER_TOTAL` 或 `CUSTOMER_GRADE_DISTRIBUTION`；统计口径复用S1。真实模型测试使用合成统计Service，不构成真实MySQL独立核验。

### 3.2 显式查询范围与匹配

第一次分类的 `queryScopeIntent` 仅表示请求语义，不能作为实际权限：

| 请求表达 | queryScopeIntent | 当前V1允许条件 |
|---|---|---|
| 当前可查看的客户；未指定组织范围 | CURRENT_AUTHORIZED | 使用实际ALL/DEPT/SELF |
| 我有多少客户；我的客户 | SELF | 实际SELF |
| 我们部门；本部门 | DEPT | 实际DEPT |
| 全公司 | ALL | 实际ALL |
| 其他指定人员/部门或不支持范围 | UNSUPPORTED | 拒绝 |

指定范围与实际范围不一致返回422 `AI_ANALYSIS_UNSUPPORTED`，在创建Tool及S1聚合查询前拒绝。ALL用户查询本人或本部门也拒绝：当前S1按有效范围统计，没有主动缩小能力。范围由后端解析，不能从模型输出的dataScope、用户自称管理员或Tool参数中取得。

S1执行时仍自行强制校验数据权限；若其返回范围与前置解析不同，不返回成功结果。回答明确本人、本部门或全公司实际范围，analysisContext始终来源于实际S1结果。

### 3.3 不在第一阶段

- 部门对比、销售人员排名、客户跟进风险（后续阶段）。
- 图表渲染、复杂多维交叉分析、RAG、向量数据库、多 Agent。
- 历史会话服务端持久化、跨设备恢复、复杂多轮记忆。
- 任何写操作、任意 SQL 执行、直接将数据库访问权限交给 LLM。
- 客户姓名、联系方式、地址等明细、公海专用统计、自定义日期、指定其他人员/部门、合同/销售额/商机/业绩指标及主动缩小组织范围。

## 4. 前端页面与 UI 基线

S3-A已按欢迎页及结果页原型实现以下布局，S3-B已进行真实浏览器核心核对。快捷问题采用未指定组织范围的客户总量、本月新增、近30天等级分布，避免ALL/DEPT用户的“我的客户”与实际范围不匹配。

### 4.1 菜单

`数据分析 → AI 智能分析`，与“销售报表 / 客户分析 / 业绩分析”并列。

### 4.2 原型结构（必须沿用已确认的聊天界面原型）

- 左侧约 25%–30%：**新建分析**、**最近的分析**区域；阶段一无真实历史记录时显示空状态，不伪造历史数据。
- 右侧主工作区：初始欢迎页显示脑形图标、标题“CRM AI 销售分析助手”、副标题及快捷提问。
- 欢迎页快捷提问在阶段一仅展示已实现能力，例如“统计我负责的客户等级分布”“我本月新增多少客户？”“最近30天新增客户有多少？”。不展示尚未实现的跟进风险、部门对比作为可用功能。
- 底部输入框：业务问题输入、发送按钮、权限范围提示；提交后切换为对话消息及分析结果。
- 分析结果：文字回答、可选指标摘要、统计范围与时间口径；第一阶段不要求图表。
- 视觉：简洁浅色、圆角卡片、适度留白，与现有 CRM Element Plus 风格协调；保留响应式和空状态/加载/错误状态。
- **原型是设计基准**：后续 Codex 开发必须参照当前会话已确认的截图/原型，不擅自更换为其他聊天页面布局；若 Codex 无法访问图片，应先将截图放入项目可访问的设计资料目录。

### 4.3 已实现 Vue 组件

- `views/analysis/AiAnalysis.vue`：页面容器、状态协调。
- `components/ai-analysis/AiWelcomePanel.vue`：欢迎页、快捷提问。
- `components/ai-analysis/AiChatInput.vue`：输入、提交。
- `components/ai-analysis/AiAnalysisResult.vue`：指标、等级条、口径与复制；消息列表由页面容器管理。
- `components/ai-analysis/AiConversationSidebar.vue`：新建分析、历史区域空状态。

第一阶段按实际复用需要逐步拆分，不要求一次创建所有组件。

## 5. 已实现 API 契约

`POST /api/ai/analysis`；请求由 Jakarta Validation 校验 question 非空、非空白、最多1000字符。身份只来自JWT认证主体，请求无需 userId 或 dataScope。

```json
{"question":"本月新增了多少客户？"}
```

成功返回200，直接返回 `AiAnalysisVO`：

```json
{
  "answer": "本人范围内，本月新增客户共 48 个。",
  "intent": "CUSTOMER_TOTAL",
  "metrics": {
    "totalCustomers": 48,
    "levelA": 10,
    "levelB": 15,
    "levelC": 12,
    "levelD": 8,
    "unrated": 3
  },
  "analysisContext": {
    "dataScope": "SELF",
    "timeRange": "CURRENT_MONTH",
    "startTime": "2026-10-01T00:00:00+08:00",
    "endTime": "2026-11-01T00:00:00+08:00",
    "referenceTime": "2026-10-14T12:00:00+08:00",
    "timezone": "Asia/Shanghai"
  }
}
```

数字仅为结构示例。metrics为对象，六个值为Java long；intent为两个允许统计枚举之一。analysisContext的dataScope是实际执行范围，timeRange是时间枚举；时间字段为ZonedDateTime，按项目Jackson配置输出带偏移的ISO时间字符串。ALL时间下startTime/endTime为null，referenceTime和timezone仍返回。不返回requestId、scopeLabel、toolCalls、charts或conversationId。

answer由Java生成，metrics及analysisContext均来自实际Tool/S1结果。零客户是合法成功数据；拒绝和失败不以零值伪装。

错误沿用RFC 7807 ProblemDetail，含稳定code：

| HTTP | code | 含义 |
|---|---|---|
| 400 | AI_REQUEST_INVALID | question或请求格式非法 |
| 400 | AI_TOOL_ARGUMENT_INVALID | Tool参数非法或与分类时间不一致 |
| 401 | UNAUTHORIZED / AI_AUTHENTICATION_REQUIRED | 安全入口未认证 / 缺失可信主体 |
| 403 | FORBIDDEN | 缺少任一功能权限 |
| 403 | DATA_SCOPE_CONFIGURATION_ERROR | 无有效范围或DEPT缺部门 |
| 409 | AI_TOOL_REPEATED | 重复工具调用 |
| 422 | AI_ANALYSIS_UNSUPPORTED | 不支持需求或无法匹配指定查询范围 |
| 502 | AI_MODEL_RESPONSE_INVALID | 模型分类、工具协议或结果上下文校验失败 |
| 503 | AI_MODEL_UNAVAILABLE | 模型调用异常或超时 |
| 503 | AI_ANALYSIS_NOT_READY | 未启用AI profile，模型客户端不可用 |
| 500 | AI_STATISTICS_FAILED / AI_ANALYSIS_FAILED | S1聚合错误 / 其他安全封装的服务错误 |

## 6. 时间与统计口径

- 时区Asia/Shanghai，referenceTime由S1通过可注入Clock在统计时确定；测试可提供固定Clock，用户和模型不能指定它。
- CURRENT_MONTH：本月1日00:00至下月1日00:00；LAST_MONTH：上月1日00:00至本月1日00:00。
- RECENT_30_DAYS：referenceTime减30天至referenceTime；业务时区下为滚动30×24小时。
- ALL无创建时间筛选；其余按customer.created_at左闭右开过滤，不混用跟进时间。
- 普通客户排除deleted及owner_id为空的公海，与现有Customer List口径一致；不得把某批合成数据当作真实全库总量。

## 7. 已实现后端架构

HTTP → JWT/RBAC双权限 → Qwen结构化分类 → Java枚举与查询范围校验 → Qwen Tool Calling → CustomerStatisticsTool → S1 → Java回答 → AiAnalysisVO。

最多两次模型调用；不支持/范围不匹配通常在第一次分类后结束。分类无业务Tool，字段严格为intent、timeRange、queryScopeIntent，不接受模型提供实际授权dataScope。唯一Tool为getCustomerStatistics，Schema仅timeRange，每请求独立绑定可信身份；关闭框架内部工具执行，由Java先校验再最多执行一次统计。无模型解释阶段，不把统计结果再次发给模型改写。技术细节见[技术文档](AI_AGENT_V1_TECH_STACK.md)。

## 8. 验收状态与后续工作

已实现并由定向测试覆盖：正式API、双权限、范围匹配、请求隔离、Tool白名单/次数、S1统计边界及Java结果映射。2026-10-14已有11场景真实Qwen冒烟验证（合成S1），其中旧C1暴露组织范围误解释问题；本次增加范围语义协议和执行前校验，单次C1回归结果见技术文档。

阶段进度：S1统计、S2-A接口与安全基础、S2-B模型编排与Java回答、C1显式范围匹配、S3-A前端均已完成。S3-B已执行9项真实成功统计并全部与独立MySQL SQL对账一致，完成15项浏览器核心检查及真实401/422/503反馈；前端构建、5项离线测试、63项后端定向测试通过。这些是2026-10-14阶段记录，本次文档收尾未重复调用Qwen。

阶段性结论仍为PARTIAL：四个现有验收账号均有双权限，缺权限真实账号的403/菜单拒绝验收仍待补充；相应MockMvc和离线前端测试已通过，不能冒充真实缺权限验收。真实模型故障注入和剪贴板权限拒绝分支尚未现场验证。历史持久化、跨轮记忆、图表、requestId关联与耗时追踪仍为后续规划。

SYSTEM_ADMIN设计范围为ALL，但2026-10-14只读检查发现现有角色为SELF，sys_admin因此实际SELF。角色初始化只插入缺失角色，不覆盖已有配置；没有证据证明SELF是有意的新设计。恢复ALL方案待用户确认，影响该角色绑定的5个有效用户（包括一个当前DEPT的多角色用户），尚未执行数据库修改。前端和Java仍显示/执行实际范围，不硬编码角色名称绕过配置。

模型自然语言分类存在不确定性；Java校验只信任后端权限，但仍需后续覆盖更多同义范围表达。当前不支持主动缩小组织范围；需要该能力时另行设计，不修改当前S1或权限矩阵。
