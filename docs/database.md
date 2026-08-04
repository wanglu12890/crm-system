# 数据库设计与初始化说明

## 1. 环境与连接

- 数据库：MySQL 8.0+
- 地址：`localhost:3306`
- Schema：`crm_system`
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_0900_ai_ci`
- 存储引擎：InnoDB

应用不保存数据库口令，连接配置由 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 三个环境变量注入。生产环境应使用独立的最小权限应用账号，不应让应用长期使用 root。

## 2. 公共字段规范

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | MyBatis-Plus 雪花主键，避免数据库自增成为扩展瓶颈 |
| `created_by` / `updated_by` | `BIGINT` | 操作用户 ID，由审计填充器维护 |
| `created_at` / `updated_at` | `DATETIME(3)` | 毫秒级审计时间，数据库提供默认值 |
| `deleted` | `TINYINT` | 0 正常、1 已删除，由 MyBatis-Plus 逻辑删除处理 |
| `version` | `INT` | 乐观锁版本，防止并发覆盖 |

金额统一使用 `DECIMAL(18,2)`，比例使用 `DECIMAL(5,2)`，禁止使用 FLOAT/DOUBLE。状态字段使用 `VARCHAR` 保存稳定业务编码，显示名称由字典转换。

## 3. 表字段设计

### `sys_user`

系统用户。`username` 唯一；只存 BCrypt `password_hash`，不存明文密码；`status` 控制登录资格。手机号和邮箱属于敏感信息，API 返回时应脱敏。

### `sys_role`

角色与数据范围。`role_code` 是程序鉴权使用的唯一稳定编码，`data_scope` 定义全部、部门、部门及下级、本人或自定义范围。

### `sys_permission`

菜单、按钮和 API 权限树。`parent_id` 构建层级；`permission_code` 用于 `@PreAuthorize`；路由、组件和 API 字段支持前后端动态权限。

### `sys_user_role`

用户和角色多对多中间表，联合主键防止重复分配。角色与权限的关联表建议在 RBAC 实现阶段增加 `sys_role_permission`；本次按指定基础表范围未创建。

### `customer`

客户主数据。`customer_no` 唯一；`owner_id` 指向负责人；等级、行业、来源、状态用于客户分层和统计。客户名称建立普通索引，企业级去重需结合名称、电话和统一社会信用代码策略后实现。

### `contact`

客户联系人，一名客户可以有多个联系人。`is_primary` 标识主联系人；主联系人唯一性因 MySQL 条件唯一索引限制，应由事务和业务校验保证。

### `clue`

销售线索。保存来源、负责人、跟进状态和转化结果。转化时需要在一个事务中创建或绑定客户，并写入 `converted_customer_id/converted_at`。

### `business`

销售商机。关联客户、可选联系人和负责人；阶段、成功概率、预计成交时间组成销售漏斗；金额和概率均有数据库检查约束。

### `follow_record`

统一跟进记录。通过 `target_type + target_id` 关联客户、线索或商机，组合索引支持时间线查询。该多态关联不能建立普通外键，Service 必须验证目标类型和目标记录存在。

### `contract`

销售合同。合同编号唯一，归属客户并可关联来源商机；金额、有效期和状态带检查约束。合同文件字段只保存受控对象存储地址，不保存本机文件路径。

## 4. 表关系

```text
sys_user --< sys_user_role >-- sys_role
sys_user --< customer --< contact
sys_user --< clue -------> customer（转化后）
customer --< business >-- contact（可选）
customer --< contract >-- business（可选）
customer/clue/business --< follow_record（多态关联）
```

外键用于保护主数据引用；业务删除统一使用逻辑删除。批量导入前不得通过永久关闭外键检查规避数据质量规则。

## 5. 初始化步骤

在仓库根目录执行：

```powershell
mysql -h localhost -P 3306 -u root -p < database/init.sql
```

`-p` 后不要追加密码，MySQL 会安全地提示输入。验证结果：

```powershell
mysql -h localhost -P 3306 -u root -p -e "USE crm_system; SHOW TABLES;"
```

在启动后端的同一个 PowerShell 会话中设置环境变量：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/crm_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='<输入本机数据库密码，不要提交到仓库>'
$env:JWT_SECRET='<至少32字节的随机密钥>'
```

然后启动：

```powershell
cd backend
mvn -s .mvn/settings.xml clean spring-boot:run
```

环境变量只对当前终端会话生效。团队开发可在 IDE Run Configuration 中配置；生产环境使用容器 Secret 或云密钥服务，禁止提交 `.env`、YAML 密码或带密码的命令行。

## 6. 后续迁移策略

`init.sql` 用于首次建库。进入持续开发后建议引入 Flyway，以 `V1__baseline.sql`、`V2__...sql` 管理增量变更；已经在环境执行过的迁移文件不得修改，禁止依赖 Hibernate/MyBatis 自动改表。
