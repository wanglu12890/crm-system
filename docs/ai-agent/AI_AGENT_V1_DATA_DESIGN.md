# CRM AI Agent V1 模拟数据设计

本设计用于生成开发与演示用合成数据，支撑客户结构、销售部门对比、跟进风险、时间趋势和 ALL / DEPT / SELF 权限差异分析。完整冻结要求来源于 `task/1008/2_CRM-AI-Agent-v1-模拟数据设计方案.md`，实际可执行交付位于 `scripts/seed-ai-demo/`。

版本说明（2026-10-16）：这是合成数据集设计，不是已实现AI能力清单。V1/V1.1已完成统计及范围优化；部门对比、跟进风险等只是数据可支持的后续分析方向，V2尚未开发。功能与权限规则以[需求基线](AI_AGENT_V1_REQUIREMENTS.md)为准，验收见[版本记录](AI_AGENT_V1_1_CHANGELOG.md)。

## 固定口径

- 数据集：`crm_ai_agent_v1`，随机种子 42。
- 分析基准时间：`2026-10-01T00:00:00`，时间窗口左闭右开。
- 上述时间及300条Customer等数量仅属于本批生成数据；运行时S1使用业务Clock确定referenceTime，按实际全库、请求范围及创建时间统计，不能把固定参考答案当作当前数据库结果。
- 新增 300 个 Customer、450 个 Contact、1,200 个 CUSTOMER FollowRecord。
- 复用现有五个销售账号与两个部门，不新增用户、角色或部门。
- 私有客户 270 个，公海客户 30 个；公海客户本批不生成跟进记录。
- Customer 不新增部门字段，通过 `owner_id -> sys_user.dept_id` 确定部门。
- V1.1经理SELF使用`customer.owner_id = 当前认证用户ID`，DEPT包含同部门负责人的客户；不按created_by统计，不为范围优化新增表、字段或另一套统计SQL。
- 数据包含固定等级、类型、状态、行业、来源、地域、活跃度及空值分布。
- 所有名称、地址和联系方式均为合成信息，邮箱仅使用示例域名。

## 安全与可复现性

- 生成器不连接数据库，不执行导入或删除。
- 使用独立固定 ID 段和清单文件，与现有 Customer V1 功能测试数据隔离。
- SQL 导入前要求人工核对用户、部门及碰撞查询；重复导入通过唯一约束明确失败。
- 校验失败时生成器不会输出可导入 SQL。
- 清理必须依据 manifest 精确 ID，按 FollowRecord、Contact、Customer 顺序人工执行。

## 交付物

运行 `python scripts/seed-ai-demo/generate.py` 后生成：

- `ai_demo_seed.sql`：手工审核和导入的 MySQL 8 脚本。
- `ai_demo_manifest.json`：全部实体 ID 和产物 SHA-256。
- `data_report.md`：确定性分布报告。
- `agent_reference_answers.json`：由生成数据直接计算的标准答案，不由 LLM 编写。

详细运行、校验和导入说明见 `scripts/seed-ai-demo/README.md`。
