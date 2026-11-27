# CRM 开发环境测试账号

## 一、使用说明

本文档用于 CRM 系统本地开发、接口联调及权限验收。

测试账号仅用于开发环境，不代表生产环境用户。

测试时应先验证实际角色绑定、功能权限和数据范围，不得仅根据角色名称判断权限。

## 二、测试账号

| 用户名 | 角色 | 所属部门 | 预期数据范围 | 测试用途 |
|---|---|---|---|---|
| sup_admin | SUPER_ADMIN | 待填写 | ALL | 全局客户统计、系统管理 |
| sys_admin | SYSTEM_ADMIN | 待填写 | ALL | 系统管理员业务权限 |
| sales_manager01 | SALES_MANAGER | 待填写 | DEPT | 部门客户统计与数据隔离 |
| sales_staff03 | SALES_STAFF | 待填写 | SELF | 本人客户统计与越权验证 |

数据库连接：MySQL80用户为 root，密码存在系统环境变量中，DB_PASSWORD，数据库 crm_system

测试账号也存在系统环境变量中
超级管理员
CRM_SUP_ADMIN
CRM_SUP_ADMIN_SCREAT
系统管理员
CRM_SYS_ADMIN
CRM_SYS_ADMIN_SCREAT
销售经理
CRM_SALES_MANAGER
CRM_SALES_MANAGER_SCREAT
销售员工
CRM_SALES_STAFF
CRM_SALES_STAFF_SCREAT

## 三、AI 智能分析权限

正式接口：

`POST /api/ai/analysis`

需要同时具有：

- `ai:analysis`
- `customer:list`

数据范围由后端 `DataScopeResolver` 解析。


## 四、凭证管理

本文档不保存明文密码、JWT 或 API Key。

登录凭证由本地受控配置或交互式输入提供。

测试报告不得输出完整认证凭证。