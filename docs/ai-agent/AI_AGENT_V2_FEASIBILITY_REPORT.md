# CRM AI 智能分析 V2-R1：数据可行性审查报告

- 日期：2026-10-18；状态：**R1数据审查及三轮需求评审完成；V2.0需求已确认，待设计实现**，不是已实现架构或已验收功能。
- 配套：[V2.0正式PRD](AI_AGENT_V2_REQUIREMENTS.md)。本轮只维护两份指定文档，不修改V1/V1.1、源码、配置、数据或权限，不调用Qwen，也不重新连接数据库或重跑SQL。第4节原始统计、时间与SQL完整保留。
- 结论：现有表和数据**支持客户级跟进间隔、从未跟进及等级滞后指标计算**；计划时间可计算但逾期正式判定已排除出V2.0，“新分配后未跟进”不支持可靠计算。角色、三权限及风险阈值已确认，安全读取能力、模型建议质量与性能仍待设计实现和验证，不能视为端到端V2已具备。

## 1. 审查范围与证据

原R1已读取CODEX_CONTEXT、V1需求/数据设计/技术基线、V1.1版本记录、客户V1规则、database及后端开发规范。事实区分为源码/数据库已确认、历史候选指标、三轮评审已确认需求及D1工程事项，不能互相替代。本轮冻结依据为[正式PRD维护任务](../../task/1018/1-CRM-AI-Agent-V2-R1-正式PRD.md)，原始候选统计不因规则确认而改写。

| 编号 | 实际证据 | 用途 |
|---|---|---|
| C1 | [Customer.java](../../backend/src/main/java/com/company/crm/entity/Customer.java)，17–42行 | 实体字段、逻辑删除；没有最后跟进时间 |
| C2 | [CustomerController.java](../../backend/src/main/java/com/company/crm/controller/CustomerController.java)，28–39行 | 当前仅列表GET和创建POST；未发现详情/编辑接口 |
| C3 | [CustomerServiceImpl.java](../../backend/src/main/java/com/company/crm/service/impl/CustomerServiceImpl.java)，49–59、64–93、114–127行 | 列表数据范围、创建时owner由后端决定；无分配历史 |
| C4 | [CustomerMapper.xml](../../backend/src/main/resources/mapper/CustomerMapper.xml)，8–24、60–74行 | 普通客户、公海/删除过滤；按owner及其部门限定范围 |
| C5 | [DataScopeResolver.java](../../backend/src/main/java/com/company/crm/security/datascope/DataScopeResolver.java)，22–42行 | customer:list授权范围、最大范围优先级及缺部门拒绝 |
| C6 | [CustomerAnalysisScopePolicy.java](../../backend/src/main/java/com/company/crm/service/CustomerAnalysisScopePolicy.java)，22–51行 | 显式角色/范围矩阵；CURRENT_AUTHORIZED保留多角色语义 |
| C7 | [database/init.sql](../../database/init.sql)，113–140、228–249行；本次information_schema.columns | 客户/跟进真实结构一致，follow_at与next_follow_at独立存在 |
| C8 | [客户V1规则](../customer-management/CUSTOMER_V1_RULES.md)，第9、14–16节 | 状态、跟进人/创建人、业务时间及计划语义；部分仍是计划实现 |
| C9 | [Customer.vue](../../frontend/src/views/business/Customer.vue)，showPendingFeature/handleView/handleEdit | 客户详情和编辑仍为“功能待实现”提示 |
| C10 | [生成器](../../scripts/seed-ai-demo/generate.py)，188–216行；[数据设计](AI_AGENT_V1_DATA_DESIGN.md) | 合成跟进、固定基准日与计划生成，不能当真实经营分布 |

定向检索backend/src/main、Mapper XML及相关测试，**未发现FollowRecord实体、Mapper、Service、Controller，也未发现联系人业务后端链路或客户对象级详情授权实现**。存在表、权限码和菜单不代表模块已完成。任务背景“已有联系人/跟进基础模块”需据此收窄为“已有数据基础及业务规则”。database文档关于sys_role_permission尚未建立的文字也已落后于init.sql及现有权限Mapper；本阶段只记录，不改旧文档。

本次未执行测试、构建、HTTP登录或模型调用。参考现有CustomerServiceImplTest、CustomerMapperSqlTest、CustomerAnalysisScopePolicyTest判断已覆盖的V1边界，不把其测试等同于V2明细授权。

## 2. 客户字段可用性

以下类型/可空性由本次真实库元数据核验，字段在C1及C7中对应；“否”表示数据库NOT NULL，不保证业务值有效。

| 字段 | 类型 | 可空 | 业务含义及V2用途 |
|---|---|---|---|
| id | BIGINT | 否 | 客户唯一标识，关联与稳定排序；对浏览器须防BIGINT精度丢失 |
| customer_no | VARCHAR(32) | 否 | 唯一编号，可作最小识别信息 |
| customer_name | VARCHAR(200) | 否 | 客户名称，可供获准的风险列表识别 |
| customer_type | VARCHAR(32) | 否 | ENTERPRISE/INDIVIDUAL；不是风险结论 |
| customer_level | VARCHAR(16) | 是 | A/B/C/D；NULL表示未评级，不能自动视为低价值或高风险 |
| industry | VARCHAR(64) | 是 | 行业，首版不据此推断风险 |
| source | VARCHAR(64) | 是 | 来源，不代表分配时间 |
| phone | VARCHAR(32) | 是 | 敏感联系信息，首版不传模型 |
| email | VARCHAR(128) | 是 | 同上，不为生成建议默认暴露 |
| province | VARCHAR(64) | 是 | 地域，不作为首版关注等级依据 |
| city | VARCHAR(64) | 是 | 同上 |
| address | VARCHAR(500) | 是 | 敏感详细地址，非首版必要输入 |
| owner_id | BIGINT | 是 | 当前负责人；NULL是公海，不是创建人；SELF唯一归属依据 |
| status | VARCHAR(32) | 否 | POTENTIAL潜在、ACTIVE已有有效关系、INACTIVE不再重点跟进/无长期有效业务；不是公海标记或流失事实 |
| remark | VARCHAR(1000) | 是 | 非结构化备注，不能充当风险标签或可信指令 |
| created_by | BIGINT | 否 | 创建操作者，不是客户当前负责人 |
| updated_by | BIGINT | 否 | 更新操作者，不提供归属历史 |
| created_at | DATETIME(3) | 否 | 客户建档时间，可算新建未首次跟进时长；不等于首次/最近分配时间 |
| updated_at | DATETIME(3) | 否 | 任意更新审计时间，不可替代最后跟进时间 |
| deleted | TINYINT | 否 | 0有效，逻辑删除必须排除 |
| version | INT | 否 | 乐观锁版本，非风险指标 |

未发现last_follow_at、assigned_at、领取时间、负责人变更历史、流失标签、成交概率或客户约定的标准联系频次。跟进时间应从follow_record计算，不缓存推测字段。

## 3. 跟进字段、关联与时间语义

FollowRecord目前没有生产Java模型/调用链；以下依据C7真实表及C8业务约定，不能写成已由Service强制实施。

| 字段 | 类型 | 可空 | 含义、用途及限制 |
|---|---|---|---|
| id | BIGINT | 否 | 跟进标识；同follow_at时用于稳定选择最新记录，不代表实际发生顺序 |
| target_type | VARCHAR(32) | 否 | CUSTOMER/CLUE/BUSINESS；必须先限定CUSTOMER |
| target_id | BIGINT | 否 | 在CUSTOMER语义下关联customer.id；**没有customer_id字段**，多态对象无客户外键兜底 |
| contact_id | BIGINT | 是 | 可选联系人；需校验其所属客户，不是目标客户ID |
| follow_type | VARCHAR(32) | 否 | PHONE/VISIT/EMAIL/IM/OTHER；目前无业务Service枚举校验 |
| content | TEXT | 否 | 自由跟进内容；不得将其指令当系统指令或默认全量交给模型 |
| follow_at | DATETIME(3) | 否 | **实际业务跟进时间**，最近跟进的计算依据 |
| next_follow_at | DATETIME(3) | 是 | 记录中的下次计划时间；NULL可能正常；没有完成/取消/替代状态 |
| owner_id | BIGINT | 否 | 实际跟进人员，与customer.owner_id语义不同 |
| created_by | BIGINT | 否 | 记录创建人，业务规则允许代录/导入语义，不能等同跟进人 |
| updated_by | BIGINT | 否 | 审计操作者 |
| created_at | DATETIME(3) | 否 | 录入时间，不得代替follow_at |
| updated_at | DATETIME(3) | 否 | 更新审计，不表示新的跟进行为 |
| deleted | TINYINT | 否 | 删除记录不参与风险指标 |
| version | INT | 否 | 乐观锁，不表示计划完成 |

```text
customer.owner_id → sys_user.id → sys_user.dept_id
follow_record.target_type = CUSTOMER 且 target_id = customer.id
follow_record.owner_id → 实际跟进人
follow_record.contact_id → contact.id → contact.customer_id（可选）
```

一个客户可有多条跟进记录，数据库未限制跟进人必须等于当前负责人。当前库样本相同，不能据此建立未来强约束；经理代录、历史交接均可能不同。当前源码没有负责人变更入口/历史保留策略；数据结构可在owner变化后继续保留target_id，但不能证明实际交接流程已实现。评审已确认按“客户全部有效历史”计算，而非只取当前负责人自己写的记录。

评审已确认统一分析参考时间T（Asia/Shanghai），以follow_at≤T、follow_at≥customer.created_at的有效记录观察当前状态；未来/早于建档等异常不直接用于正常分级。最后跟进时间取客户全部有效历史MAX(follow_at)，不限跟进人员；同时间按id稳定选择不改变间隔计算。只有异常历史为数据待核验；有效加异常按有效历史分级并保留质量提示。下次计划不参与V2.0正式判定，不因同时间不同计划增加本版风险。

## 4. 真实数据库质量统计

本地测试库crm_system、MySQL 8.0.46；使用既有本地连接配置，在只读事务中执行SELECT/CTE，结束rollback/close；不登录、不写数据库。元数据读取和质量统计分为两次只读事务，质量统计统一T=`2026-10-18T20:27:23.756+08:00`，由UTC_TIMESTAMP转换到+08:00得到，不依赖服务器时区名称。

### 4.1 客户分布

| 指标 | 实测 |
|---|---:|
| customer总行数/有效数 | 327 / 327 |
| 已分配/公海 | 290 / 37 |
| 逻辑删除 | 0 |
| 空名称、非法非空等级、非法状态 | 各0 |
| 缺创建时间、未来创建时间 | 各0 |
| 已分配客户缺失/删除/停用负责人、负责人无部门 | 各0 |

| 等级 | 全部有效客户 | 已分配客户 |
|---|---:|---:|
| A | 53 | 49 |
| B | 96 | 82 |
| C | 110 | 101 |
| D | 49 | 43 |
| 未评级NULL | 19 | 15 |

| 状态 | 全部有效客户 | 已分配客户 |
|---|---:|---:|
| POTENTIAL | 159 | 143 |
| ACTIVE | 115 | 98 |
| INACTIVE | 53 | 49 |

两个销售部门（ID分别以0001、0002结尾）已分配客户151、139；按owner去重归属，不按多角色JOIN叠加计数。测试销售账号分布：sales_manager01 40、sales_manager02 51、sales_staff01 60、sales_staff02 51、sales_staff03 88。两个管理员测试账号无本人归属客户；不据此决定V2准入。

创建月份分布：2025-10至2026-10依次为30、17、24、30、27、18、23、24、19、40、44、28、3；最早2025-10-02 08:22:24，最晚2026-10-08 11:02:22.152。数量仅是本次测试库快照。

### 4.2 跟进数据与关联质量

| 指标 | 实测与解释 |
|---|---|
| 总行数/有效数/删除数 | 1,236 / 1,236 / 0 |
| 目标类型 | 全部CUSTOMER；不能据此省略生产查询的target_type条件 |
| 已分配客户有记录/无记录 | 285 / 5，共290 |
| 每客户记录数分桶 | 0条：5；1条：6；2–4条：218；5条及以上：61 |
| 关联客户不存在/已删除 | 各0 |
| 关联公海客户 | 2条；按普通客户范围排除，可能是历史关系，不能直接认定数据损坏或删除 |
| 缺follow_at/未来follow_at/早于客户建档 | 各0 |
| 业务时间不等于录入时间 | **36条**，直接证明不可使用created_at近似follow_at |
| 空内容/未知跟进方式/缺失、删除、停用跟进人 | 各0 |
| 跟进人不同于当前客户负责人 | 非空负责人比较为0；另有公海NULL情况，不可归为相同 |
| 跟进人与创建人不同 | 0，仅代表样本，不代表业务必须相同 |
| contact_id为空 | 390条，正常可选字段 |
| 联系人不存在/删除/关联到其他客户 | 各0 |
| next_follow_at为空 | 1,075条，不能当数据错误或未跟进 |
| next_follow_at早于本条follow_at | 0 |
| 业务跟进时间范围 | 2025-11-19 12:57:40至2026-10-05 17:00:00 |

已分配客户最新跟进时间范围为2025-12-28 04:47:26至2026-10-05 17:00:00。完整24小时的最近跟进间隔分布：0–6天0、7–13天2、14–29天173、30–59天49、60天及以上61；另外5个从未跟进，不与有历史的客户混在同一分桶。

### 4.3 计划字段语义风险

全部有效CUSTOMER记录有161条非空计划，其中160条时间已过去，涉及152个已分配客户。只看每个已分配客户的最新业务记录：52个有非空计划，其中51个时间已过去、1个尚未来到；另有**100个客户最新记录计划为空、历史记录却有计划**。

这些是三种不同口径，不能把160条历史计划说成160个客户违约，也不能把“时间过去”说成“任务未完成”。无任务完成/取消状态；最新NULL是取消、未安排还是未承接，尚无正式规则。生成器在最后生成的记录写计划，但follow_at随机生成，生成顺序未必等于时间顺序，是可能造成该现象的因素，不能据此推定所有实际数据来源。

### 4.4 候选阈值敏感性（不是业务定值）

按实际时间比较`anchor <= T - N DAY`，包括刚好等于边界；“高等级”在本统计中仅指A/B，是候选业务分组。

| N天 | 有跟进且间隔≥N | 从未跟进且建档≥N | 合计候选 | A/B有历史间隔≥N | A/B从未跟进建档≥N |
|---|---:|---:|---:|---:|---:|
| 7 | 285 | 5 | 290 | 127 | 4 |
| 14 | 283 | 3 | 286 | 126 | 2 |
| 30 | 110 | 3 | 113 | 52 | 2 |
| 60 | 61 | 3 | 64 | 37 | 2 |

不据此选阈值或目标风险占比。演示数据包含固定2026-10-01基准日及合成跟进，分析时间已为10月18日；样本老化和生成分布会放大候选数量。没有真实流失/成交标签，也无效果反馈，不能训练或宣称预测概率。

### 4.5 SQL依据与复核口径

以上为本次已执行聚合的结果，未读取或输出客户联系方式、跟进正文、密码或Token。以下保留主要查询形状；`:T`、`:N`表示客户端绑定参数，不是已执行的SET变量或生产查询设计。

```sql
-- Q1 客户基础与分布：另按customer_level/status/创建月份GROUP BY
SELECT COUNT(*) AS total, SUM(deleted=0) AS valid,
       SUM(deleted=0 AND owner_id IS NOT NULL) AS assigned,
       SUM(deleted=0 AND owner_id IS NULL) AS public_pool
FROM customer;

-- Q2 关联及时间质量；统计时没有把NULL负责人比较算成相同
SELECT COUNT(*) AS records, SUM(c.id IS NULL) AS orphan,
       SUM(c.deleted<>0) AS deleted_customer,
       SUM(c.owner_id IS NULL) AS public_target,
       SUM(f.follow_at IS NULL) AS missing_time,
       SUM(f.follow_at>:T) AS future_time,
       SUM(f.follow_at<c.created_at) AS before_creation,
       SUM(f.follow_at<>f.created_at) AS distinct_business_time,
       SUM(f.owner_id<>c.owner_id) AS actor_owner_difference
FROM follow_record f LEFT JOIN customer c ON c.id=f.target_id
WHERE f.deleted=0 AND f.target_type='CUSTOMER';

-- Q3 最新有效记录；质量统计中未加待确认的客户状态排除条件
WITH ranked AS (
  SELECT f.*, ROW_NUMBER() OVER (
    PARTITION BY f.target_id ORDER BY f.follow_at DESC,f.id DESC) AS rn
  FROM follow_record f JOIN customer c ON c.id=f.target_id
  WHERE f.deleted=0 AND f.target_type='CUSTOMER' AND c.deleted=0
    AND f.follow_at IS NOT NULL AND f.follow_at<=:T
    AND f.follow_at>=c.created_at
), last_follow AS (SELECT * FROM ranked WHERE rn=1)
SELECT COUNT(*) AS assigned, SUM(l.id IS NULL) AS never_followed,
       SUM(l.id IS NOT NULL AND l.follow_at<=TIMESTAMPADD(DAY,-:N,:T)) AS stale,
       SUM(l.id IS NULL AND c.created_at<=TIMESTAMPADD(DAY,-:N,:T)) AS never_old,
       SUM(l.next_follow_at IS NOT NULL) AS latest_has_plan,
       SUM(l.next_follow_at<:T) AS latest_plan_time_passed
FROM customer c LEFT JOIN last_follow l ON l.target_id=c.id
WHERE c.deleted=0 AND c.owner_id IS NOT NULL;
```

其余已执行查询包括：information_schema.columns核对类型/可空性；客户按owner部门、账号、等级、状态、创建月分组；联系人和跟进人LEFT JOIN质量计数；每客户COUNT分桶；同一latest CTE的间隔分桶、A/B敏感性、最新NULL但历史存在计划的EXISTS计数；四个测试账号的有效角色/权限元数据查询。阈值及状态变化后应重新只读对账，不能把本报告数值硬编码为答案。

## 5. 指标可行性矩阵

| 指标 | 结论 | 理由与依赖 |
|---|---|---|
| 距最近跟进完整天数 | 支持 | follow_at及有效关联存在；完整24小时、达到阈值即命中，默认值见正式PRD |
| 是否从未跟进 | 支持 | 客户LEFT JOIN有效CUSTOMER历史；只有异常记录时必须是待核验，不是从未 |
| 新建客户未首次跟进时长 | 支持 | created_at到T；须称“新建/建档”，已确认7/14天分流且仅POTENTIAL/ACTIVE |
| 新分配/新领取客户未首次跟进 | 不支持可靠计算 | 无assigned_at、领取事件和交接历史；created_at/updated_at均不能代替 |
| 分等级客户跟进滞后 | 支持，待实现 | 等级和业务时间可用；已确认A级14/30天，B/C/D/未评级30/60天，不沿用原A/B敏感性分组作为规则 |
| 已计划跟进逾期 | 数据有条件支持；V2.0明确排除 | next_follow_at存在但缺完成/取消生命周期；历史统计不能升级为本版正式判定 |
| 客户流失/成交概率 | 不支持 | 无监督标签、验证集和预测能力；关注规则不等于概率 |
| 基于事实的通用跟进建议 | 有条件支持 | 可仅根据结构化指标给辅助动作；需限定模型事实、权限、输出校验及人工复核，未完成实测 |

## 6. 权限与安全复用分析

V1.1可复用的是**范围解析和销售角色的范围决策**，不是自动取得所有跟进明细权限。C6硬绑定customer:list，允许管理员ALL，且CURRENT_AUTHORIZED跳过单角色限制；直接照搬会把V2管理员和多角色准入决定隐式继承，故不能原样作为完整V2门禁。

本次真实四个测试账号均有ai:analysis、customer:list、follow:list；销售另有follow:create，管理员没有follow:create。权限码存在但Follow查询Service未实现，不能证明跟进正文已受对象范围保护。

评审已确认V2只允许单有效SALES_MANAGER/SALES_STAFF且同时具备ai:analysis、customer:list、follow:list；两个管理员、未知/多角色及角色授权不符均拒绝，默认请求也不例外。经理最大DEPT、默认DEPT且可显式SELF；销售最大及执行范围均SELF。先做V2准入再复用V1.1范围语义，不修改V1/V1.1规则；派生指标、客户清单、原因、分页总量与建议限定同一适用集合，并额外排除INACTIVE，不能直接复用原统计总量。

按customer.id查询原因或建议时必须重新检查该客户当前可见性；用户提交ID、分页游标、旧会话结果都不能当授权证据。历史跟进按获准客户全部有效历史聚合，不按跟进人的当前部门或当前负责人过滤。Qwen仅接收当前页必要结果的结构化指标，不默认提供正文、联系方式、详细地址、备注全文等非必要敏感信息；模型不得改变可信等级、日期、原因、排序及客户事实。

## 7. 可复用能力与实施缺口

可复用：JWT主体、双权限入口经验、DataScopeResolver、销售范围矩阵、普通客户共享过滤、Clock/Asia-Shanghai、严格模型输入/工具白名单、Java确定性结果和ProblemDetail、前端临时会话及结构化展示经验。V1工具只有汇总统计，不能把现有六项计数当作风险客户明细；V2必需新的读取/指标能力，但R1不命名具体意图枚举、类、API或工具签名。

后续设计依赖：跟进安全只读能力及对象授权、原因/建议的最小化接口契约、已确认角色/三权限的强制落点、后端阈值配置与规则版本一致性、全量可信筛选排序计数后分页、当前页建议组织与事实保护、性能测量和异常样本。客户详情仍未实现，不假设已有跳转；分配历史与计划生命周期属于排除指标的数据缺口，不是本版必须补齐的完整CRUD。数据字段足够不等于模块已实现。

## 8. 需求基线与D1入口

V2.0业务范围、风险默认值与准入已按三轮评审冻结，详细主表见[正式PRD第3–5节](AI_AGENT_V2_REQUIREMENTS.md#3-用户角色与使用场景)。当前可以进入V2-D1技术设计，不再把已确认的阈值或权限重复列为需求待确认。

当前小规模、含合成数据的历史样本能证明可计算性，不能证明默认阈值的真实经营效果或性能SLA；阈值是人工确认的业务基线，不是从敏感性分布自动学出的参数。D1处理的接口、Tool、配置结构和性能验证事项见[PRD第11节](AI_AGENT_V2_REQUIREMENTS.md#11-已确认决策与d1设计事项)，本次不提前冻结架构或编码。

## 9. V2-R1需求评审结论

本节为2026-10-18三轮人工评审后的正式结论，与第4节原始数据审查分开记录：

- **角色/权限**：仅单有效销售经理和销售人员，且同时有ai:analysis、customer:list、follow:list；管理员、多角色、未知或角色配置不符拒绝。经理默认DEPT、可显式SELF；销售仅SELF，不改变V1/V1.1。
- **适用集合**：deleted=0、owner_id非空、status为POTENTIAL/ACTIVE且处于实际授权范围；INACTIVE、公海、删除客户排除。全状态查询也遵守该集合。
- **已有历史默认值**：A级不足14天低、14至不足30天中、30天及以上高；B/C/D/未评级不足30天低、30至不足60天中、60天及以上高。
- **从未跟进默认值**：以建档时间起算，不足7天观察中、7至不足14天中、14天及以上高。观察中和数据待核验不是正常风险等级。
- **时间/异常**：全部有效CUSTOMER历史、不限跟进人员，以follow_at和统一后端T、Asia/Shanghai、完整24小时计算，达到即命中。仅异常历史不正常分级；有效+异常按有效历史分级并提示质量。
- **去重/排序**：同客户只计一次、取最高等级、保留不同真实原因，不重复同事实；正常结果按高/中/低、A/B/C/D/未评级、超过当前等级起点的天数降序、ID稳定排序，超期不得负值或无限大。
- **查询/分页**：默认只返回高/中关注；明确全状态可展示低关注和特殊状态，不冒充优先风险。默认20、最大50；先可信筛选、去重、排序和计数，再分页。
- **规则配置**：默认阈值由后端配置管理并记录规则版本；同T、数据快照和规则版本确定，单次原因/排序/建议不可混用不同配置，结构与实现留D1。
- **建议边界**：模型只使用当前页必要结构化指标，不默认传正文、联系方式、地址、备注全文；不能改Java结果。失败保留可信结果并提示建议暂不可用，不伪造或把查询变成空结果，禁止无界逐客户模型调用。
- **排除范围**：历史风险回溯、计划逾期正式判定、流失概率预测、新分配风险、自动写入/分配、完整跟进CRUD、RAG/向量库/多Agent及持久化对话不纳入V2.0。

第4节327/290等统计与敏感性表包含INACTIVE及原A/B候选分组，保留原值和时间，不是冻结规则下的匹配总量、关注等级分布或已通过的V2验收。后续设计实现后须按正式适用集合、默认阈值和授权范围重新独立对账，本轮没有执行此类数据库或功能验证。
