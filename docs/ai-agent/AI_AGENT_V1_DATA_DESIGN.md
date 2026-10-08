# CRM AI Agent V1 模拟数据设计

本设计用于生成开发与演示用合成数据，支撑客户结构、销售部门对比、跟进风险、时间趋势和 ALL / DEPT / SELF 权限差异分析。完整冻结要求来源于 `task/1008/2_CRM-AI-Agent-v1-模拟数据设计方案.md`，实际可执行交付位于 `scripts/seed-ai-demo/`。

## 固定口径

- 数据集：`crm_ai_agent_v1`，随机种子 42。
- 分析基准时间：`2026-10-01T00:00:00`，时间窗口左闭右开。
- 新增 300 个 Customer、450 个 Contact、1,200 个 CUSTOMER FollowRecord。
- 复用现有五个销售账号与两个部门，不新增用户、角色或部门。
- 私有客户 270 个，公海客户 30 个；公海客户本批不生成跟进记录。
- Customer 不新增部门字段，通过 `owner_id -> sys_user.dept_id` 确定部门。
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
