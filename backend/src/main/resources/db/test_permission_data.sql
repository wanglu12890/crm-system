-- CRM development-only permission test data (MySQL 8.0)
-- Prerequisite: database/init.sql has been executed and SUPER_ADMIN exists.
-- IDs are generated in the same BIGINT Snowflake shape used by MyBatis-Plus ASSIGN_ID.

USE crm_system;

SET NAMES utf8mb4;
START TRANSACTION;

-- MyBatis-Plus Snowflake IDs use the Twitter epoch (2010-11-04 01:42:54.657 UTC).
-- The small sequence offset gives every row in this fixture a distinct ID.
SET @permission_id_base =
    (CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS UNSIGNED) - 1288834974657) << 22;

INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    route_path, component_path, http_method, api_path,
    sort_order, status, created_at, updated_at
)
SELECT @permission_id_base + fixture.seq,
       0,
       fixture.permission_code,
       fixture.permission_name,
       'BUTTON',
       NULL,
       NULL,
       NULL,
       NULL,
       fixture.sort_order,
       1,
       CURRENT_TIMESTAMP(3),
       CURRENT_TIMESTAMP(3)
  FROM (
        -- _utf8mb4 hex literals keep Chinese names intact in every CLI/pipe encoding.
        SELECT 1 AS seq, 'user:list' AS permission_code, _utf8mb4 0xE69FA5E79C8BE794A8E688B7 AS permission_name, 10 AS sort_order
        UNION ALL SELECT 2, 'user:create', _utf8mb4 0xE696B0E5BBBAE794A8E688B7, 20
        UNION ALL SELECT 3, 'user:update', _utf8mb4 0xE7BC96E8BE91E794A8E688B7, 30
        UNION ALL SELECT 4, 'user:delete', _utf8mb4 0xE588A0E999A4E794A8E688B7, 40
        UNION ALL SELECT 5, 'role:list', _utf8mb4 0xE69FA5E79C8BE8A792E889B2, 50
        UNION ALL SELECT 6, 'role:create', _utf8mb4 0xE696B0E5BBBAE8A792E889B2, 60
        UNION ALL SELECT 7, 'role:update', _utf8mb4 0xE7BC96E8BE91E8A792E889B2, 70
        UNION ALL SELECT 8, 'role:delete', _utf8mb4 0xE588A0E999A4E8A792E889B2, 80
        UNION ALL SELECT 9, 'role:assign_permission', _utf8mb4 0xE58886E9858DE8A792E889B2E69D83E99990, 90
       ) AS fixture
 WHERE NOT EXISTS (
       SELECT 1
         FROM sys_permission AS existing_permission
        WHERE existing_permission.permission_code = fixture.permission_code
 );

-- Resolve SUPER_ADMIN by role_code and add only missing role-permission relations.
SET @relation_id_base =
    (CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS UNSIGNED) - 1288834974657) << 22;

INSERT INTO sys_role_permission (id, role_id, permission_id, create_time)
SELECT @relation_id_base + 1024 +
       ROW_NUMBER() OVER (ORDER BY permission.id),
       role.id,
       permission.id,
       CURRENT_TIMESTAMP(3)
  FROM sys_role AS role
  JOIN sys_permission AS permission
    ON permission.permission_code IN (
       'user:list', 'user:create', 'user:update', 'user:delete',
       'role:list', 'role:create', 'role:update', 'role:delete',
       'role:assign_permission'
    )
 WHERE role.role_code = 'SUPER_ADMIN'
   AND NOT EXISTS (
       SELECT 1
         FROM sys_role_permission AS existing_relation
        WHERE existing_relation.role_id = role.id
          AND existing_relation.permission_id = permission.id
   );

COMMIT;

-- Verification queries
SELECT permission_code, permission_name
  FROM sys_permission
 ORDER BY permission_code;

SELECT role.role_code, permission.permission_code, permission.permission_name
  FROM sys_role AS role
  JOIN sys_role_permission AS relation ON relation.role_id = role.id
  JOIN sys_permission AS permission ON permission.id = relation.permission_id
 WHERE role.role_code = 'SUPER_ADMIN'
 ORDER BY permission.permission_code;
