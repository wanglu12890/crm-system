-- CRM 用户密码重置权限（MySQL 8.0）
-- 前置条件：database/init.sql、用户管理菜单权限及 SUPER_ADMIN / SYSTEM_ADMIN 角色已初始化。
-- 本脚本可重复执行；当前开发数据库需手动执行一次，不由应用自动执行。

USE crm_system;

SET NAMES utf8mb4;
START TRANSACTION;

-- parent_id 通过真实 permission_code 查找，不根据编码前缀或环境中的自增顺序推断。
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    route_path, component_path, http_method, api_path,
    sort_order, status, created_at, updated_at
)
SELECT 2090358768733782032,
       user_menu.id,
       'user:reset_password',
       '重置用户密码',
       'BUTTON',
       NULL,
       NULL,
       'POST',
       '/users/{id}/reset-password',
       60,
       1,
       CURRENT_TIMESTAMP(3),
       CURRENT_TIMESTAMP(3)
  FROM sys_permission AS user_menu
 WHERE user_menu.permission_code = 'system:user'
   AND user_menu.permission_type = 'MENU'
   AND NOT EXISTS (
       SELECT 1
         FROM sys_permission AS existing_permission
        WHERE existing_permission.permission_code = 'user:reset_password'
   )
 LIMIT 1;

-- 为两个管理员角色补齐权限；普通业务角色不会被写入。
SET @relation_id_base =
    (CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS UNSIGNED) - 1288834974657) << 22;

INSERT INTO sys_role_permission (id, role_id, permission_id, create_time)
SELECT @relation_id_base + 1024 + ROW_NUMBER() OVER (ORDER BY role.id),
       role.id,
       permission.id,
       CURRENT_TIMESTAMP(3)
  FROM sys_role AS role
  JOIN sys_permission AS permission
    ON permission.permission_code = 'user:reset_password'
 WHERE role.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN')
   AND role.deleted = 0
   AND NOT EXISTS (
       SELECT 1
         FROM sys_role_permission AS existing_relation
        WHERE existing_relation.role_id = role.id
          AND existing_relation.permission_id = permission.id
   );

COMMIT;

-- 执行后验证
SELECT id, parent_id, permission_code, permission_name, permission_type, sort_order, status
  FROM sys_permission
 WHERE permission_code = 'user:reset_password';

SELECT role.role_code, permission.permission_code
  FROM sys_role AS role
  JOIN sys_role_permission AS relation ON relation.role_id = role.id
  JOIN sys_permission AS permission ON permission.id = relation.permission_id
 WHERE permission.permission_code = 'user:reset_password'
 ORDER BY role.role_code;
