# CRM 开发环境测试账号

## 用途与连接

用于本地开发、客户详情联调及权限验收，不代表生产账号。数据库为本机 MySQL 8 的 `crm_system`，本地联调账号为 `root`，密码从 `DB_PASSWORD` 读取；应用连接仍以现有本地配置及 DB_URL、DB_USERNAME、DB_PASSWORD 为准。仅执行必要只读查询，不修改演示数据或授权来构造场景。

## 账号环境变量

| 测试角色 | 默认用户名 | 用户名变量 | 密码变量 | 预期范围 |
|---|---|---|---|---|
| SUPER_ADMIN | sup_admin | CRM_SUP_ADMIN | CRM_SUP_ADMIN_SCREAT | ALL |
| SYSTEM_ADMIN | sys_admin | CRM_SYS_ADMIN | CRM_SYS_ADMIN_SCREAT | ALL |
| SALES_MANAGER | sales_manager01 | CRM_SALES_MANAGER | CRM_SALES_MANAGER_SCREAT | DEPT |
| SALES_STAFF | sales_staff03 | CRM_SALES_STAFF | CRM_SALES_STAFF_SCREAT | SELF |

环境变量值优先于默认用户名。`SCREAT` 为现有变量名称，沿用其拼写，不自行改成 SECRET。本文不保存密码、JWT 或 API Key。

2026-11-27 联调时四个账号的实际范围分别确认为 ALL、ALL、DEPT、SELF；后续测试仍须核验有效角色、权限及所属部门，不依赖账号名称推断授权。DEPT 通过客户负责人用户的部门判断，SELF 通过当前负责人判断。

## 环境变量生效与排查

在 Windows 高级系统设置添加变量后，重启使用它的终端、IDE 或 Agent 应用；已运行的进程不会自动继承新值。必要时重启其父应用，而不仅是新建终端标签。

以下 PowerShell 只检查当前进程是否有非空值，不输出凭据：

```powershell
$names = 'DB_PASSWORD',
  'CRM_SUP_ADMIN', 'CRM_SUP_ADMIN_SCREAT',
  'CRM_SYS_ADMIN', 'CRM_SYS_ADMIN_SCREAT',
  'CRM_SALES_MANAGER', 'CRM_SALES_MANAGER_SCREAT',
  'CRM_SALES_STAFF', 'CRM_SALES_STAFF_SCREAT'
foreach ($name in $names) {
  $present = -not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))
  Write-Output "$name available=$present"
}
```

User/Machine 注册表可见性可能受执行身份或沙箱影响；当前进程可读取才是该测试进程能否使用凭据的直接证据。不要打印变量完整值或将密码放到命令行参数中。

## 接口与权限检查

后端默认 `http://localhost:8080/api`，前端默认 `http://localhost:5173`。确认服务正在运行；连接被拒绝应先检查服务及端口，不能直接归因于密码错误。

| 接口 | 功能权限 |
|---|---|
| GET /api/customers/{id} | customer:list |
| GET /api/customers/{id}/contacts | contact:list |
| GET /api/customers/{id}/follow-records | follow:list |
| POST /api/ai/analysis | ai:analysis 和 customer:list |

登录后通过 /api/auth/me 核验角色和权限；数据范围以 DataScopeResolver 和实际有效授权为准。客户子资源继承父客户可见性，公海不进入普通详情接口。

缺少单项权限、授权撤销或负责人变化等场景应使用隔离测试环境或自动化测试，禁止直接修改演示数据。报告仅记录角色、范围、状态码与脱敏断言，不输出 Token、密码或客户联系方式。
