-- Customer Management V1 permissions and role bindings (MySQL 8.0)
-- ONE-TIME DEVELOPMENT MIGRATION.
-- Review and execute manually. This script only adds missing permissions and
-- missing role-permission relations; it never deletes or replaces existing RBAC data.

USE crm_system;
SET NAMES utf8mb4;
START TRANSACTION;

-- Preflight: expected 4. STOP if any system role is missing or logically deleted.
SET @customer_v1_role_count := (
    SELECT COUNT(*)
    FROM sys_role
    WHERE role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF')
      AND deleted = 0
);

SELECT @customer_v1_role_count AS customer_v1_role_count;

-- Permission IDs are stable BIGINT values. Existing permission codes are reused.
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    route_path, component_path, http_method, api_path,
    sort_order, status, created_at, updated_at
)
SELECT 2206071000000000001, 0, 'customer-management', '客户管理', 'MENU',
       NULL, NULL, NULL, NULL, 50, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'customer-management'
);

INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    route_path, component_path, http_method, api_path,
    sort_order, status, created_at, updated_at
)
SELECT menu_fixture.id, root.id, menu_fixture.permission_code,
       menu_fixture.permission_name, 'MENU', menu_fixture.route_path,
       NULL, NULL, NULL, menu_fixture.sort_order, 1,
       CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
FROM sys_permission AS root
CROSS JOIN (
    SELECT 2206071000000000002 AS id, 'customer-management:customer-list' AS permission_code,
           '客户列表' AS permission_name, '/admin/business/customer-list' AS route_path, 10 AS sort_order
    UNION ALL SELECT 2206071000000000003, 'customer-management:public-pool',
                     '公海客户', '/admin/business/public-customer', 20
    UNION ALL SELECT 2206071000000000004, 'customer-management:contact',
                     '联系人管理', '/admin/business/contact', 30
    UNION ALL SELECT 2206071000000000005, 'customer-management:follow',
                     '跟进记录', '/admin/business/follow-up', 40
) AS menu_fixture
WHERE root.permission_code = 'customer-management'
  AND root.permission_type = 'MENU'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission AS existing_permission
      WHERE existing_permission.permission_code = menu_fixture.permission_code
  );

INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    route_path, component_path, http_method, api_path,
    sort_order, status, created_at, updated_at
)
SELECT operation_fixture.id, parent_menu.id, operation_fixture.permission_code,
       operation_fixture.permission_name, 'BUTTON', NULL, NULL, NULL, NULL,
       operation_fixture.sort_order, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
FROM (
    SELECT 2206071000000000011 AS id, 'customer-management:customer-list' AS parent_code,
           'customer:list' AS permission_code, '查看客户' AS permission_name, 10 AS sort_order
    UNION ALL SELECT 2206071000000000012, 'customer-management:customer-list',
                     'customer:create', '新建客户', 20
    UNION ALL SELECT 2206071000000000013, 'customer-management:customer-list',
                     'customer:update', '编辑客户', 30
    UNION ALL SELECT 2206071000000000014, 'customer-management:public-pool',
                     'customer_pool:list', '查看公海客户', 10
    UNION ALL SELECT 2206071000000000015, 'customer-management:public-pool',
                     'customer_pool:claim', '领取公海客户', 20
    UNION ALL SELECT 2206071000000000016, 'customer-management:contact',
                     'contact:list', '查看联系人', 10
    UNION ALL SELECT 2206071000000000017, 'customer-management:contact',
                     'contact:create', '新建联系人', 20
    UNION ALL SELECT 2206071000000000018, 'customer-management:contact',
                     'contact:update', '编辑联系人', 30
    UNION ALL SELECT 2206071000000000019, 'customer-management:contact',
                     'contact:delete', '删除联系人', 40
    UNION ALL SELECT 2206071000000000020, 'customer-management:follow',
                     'follow:list', '查看跟进记录', 10
    UNION ALL SELECT 2206071000000000021, 'customer-management:follow',
                     'follow:create', '新建跟进记录', 20
) AS operation_fixture
JOIN sys_permission AS parent_menu
  ON parent_menu.permission_code = operation_fixture.parent_code
 AND parent_menu.permission_type = 'MENU'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission AS existing_permission
    WHERE existing_permission.permission_code = operation_fixture.permission_code
);

-- Existing development role IDs are intentionally resolved by stable role_code.
-- The generated relation IDs follow the current MyBatis-Plus Snowflake shape.
SET @relation_id_base =
    (CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS UNSIGNED) - 1288834974657) << 22;

INSERT INTO sys_role_permission (id, role_id, permission_id, create_time)
SELECT @relation_id_base + 1024 +
       ROW_NUMBER() OVER (ORDER BY role.id, permission.id),
       role.id, permission.id, CURRENT_TIMESTAMP(3)
FROM sys_role AS role
JOIN sys_permission AS permission
  ON permission.permission_code IN (
      'customer:list', 'customer:create', 'customer:update',
      'customer_pool:list', 'customer_pool:claim',
      'contact:list', 'contact:create', 'contact:update', 'contact:delete',
      'follow:list', 'follow:create'
  )
WHERE role.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF')
  AND role.deleted = 0
  AND @customer_v1_role_count = 4
  AND (
      role.role_code IN ('SALES_MANAGER', 'SALES_STAFF')
      OR permission.permission_code NOT IN ('customer_pool:claim', 'follow:create')
  )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission AS existing_relation
      WHERE existing_relation.role_id = role.id
        AND existing_relation.permission_id = permission.id
  );

COMMIT;

-- Verification 1: expected 11 enabled BUTTON permissions.
SELECT id, parent_id, permission_code, permission_name, permission_type, sort_order, status
FROM sys_permission
WHERE permission_code IN (
    'customer:list', 'customer:create', 'customer:update',
    'customer_pool:list', 'customer_pool:claim',
    'contact:list', 'contact:create', 'contact:update', 'contact:delete',
    'follow:list', 'follow:create'
)
ORDER BY permission_code;

-- Verification 2: expected 0 rows.
SELECT permission_code, COUNT(*) AS duplicate_count
FROM sys_permission
WHERE permission_code IN (
    'customer:list', 'customer:create', 'customer:update',
    'customer_pool:list', 'customer_pool:claim',
    'contact:list', 'contact:create', 'contact:update', 'contact:delete',
    'follow:list', 'follow:create'
)
GROUP BY permission_code
HAVING COUNT(*) > 1;

-- Verification 3: inspect every stored Customer V1 operation relation.
SELECT role.role_code, permission.permission_code
FROM sys_role AS role
JOIN sys_role_permission AS relation ON relation.role_id = role.id
JOIN sys_permission AS permission ON permission.id = relation.permission_id
WHERE role.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF')
  AND permission.permission_code IN (
      'customer:list', 'customer:create', 'customer:update',
      'customer_pool:list', 'customer_pool:claim',
      'contact:list', 'contact:create', 'contact:update', 'contact:delete',
      'follow:list', 'follow:create'
  )
ORDER BY role.role_code, permission.permission_code;

-- Verification 4: expected SUPER_ADMIN=9, SYSTEM_ADMIN=9,
-- SALES_MANAGER=11 and SALES_STAFF=11. MENU nodes are excluded by the code list.
SELECT role.role_code, COUNT(DISTINCT permission.id) AS customer_permission_count
FROM sys_role AS role
LEFT JOIN sys_role_permission AS relation ON relation.role_id = role.id
LEFT JOIN sys_permission AS permission
       ON permission.id = relation.permission_id
      AND permission.permission_code IN (
          'customer:list', 'customer:create', 'customer:update',
          'customer_pool:list', 'customer_pool:claim',
          'contact:list', 'contact:create', 'contact:update', 'contact:delete',
          'follow:list', 'follow:create'
      )
WHERE role.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF')
GROUP BY role.id, role.role_code
ORDER BY FIELD(role.role_code, 'SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF');

-- Verification 5: expected no rows for governance roles and four rows for sales roles.
SELECT role.role_code, permission.permission_code
FROM sys_role AS role
JOIN sys_role_permission AS relation ON relation.role_id = role.id
JOIN sys_permission AS permission ON permission.id = relation.permission_id
WHERE role.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN')
  AND permission.permission_code IN ('customer_pool:claim', 'follow:create')
ORDER BY role.role_code, permission.permission_code;

SELECT role.role_code, permission.permission_code
FROM sys_role AS role
JOIN sys_role_permission AS relation ON relation.role_id = role.id
JOIN sys_permission AS permission ON permission.id = relation.permission_id
WHERE role.role_code IN ('SALES_MANAGER', 'SALES_STAFF')
  AND permission.permission_code IN ('customer_pool:claim', 'follow:create')
ORDER BY role.role_code, permission.permission_code;

-- Verification 6: data scope remains independently configured.
SELECT role_code, data_scope, status, deleted
FROM sys_role
WHERE role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF')
ORDER BY FIELD(role_code, 'SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF');
