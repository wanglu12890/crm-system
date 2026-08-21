START TRANSACTION;

-- =========================================================
-- 1. 新增 MENU 层级节点
-- =========================================================

-- 系统管理：根菜单
INSERT INTO sys_permission (
    id,
    parent_id,
    permission_code,
    permission_name,
    permission_type,
    route_path,
    component_path,
    http_method,
    api_path,
    sort_order,
    status
) VALUES (
    2090358768733782026,
    0,
    'system',
    '系统管理',
    'MENU',
    NULL,
    NULL,
    NULL,
    NULL,
    40,
    1
);

-- 用户管理
INSERT INTO sys_permission (
    id,
    parent_id,
    permission_code,
    permission_name,
    permission_type,
    route_path,
    component_path,
    http_method,
    api_path,
    sort_order,
    status
) VALUES (
    2090358768733782027,
    2090358768733782026,
    'system:user',
    '用户管理',
    'MENU',
    '/admin/system/user',
    NULL,
    NULL,
    NULL,
    10,
    1
);

-- 角色管理
INSERT INTO sys_permission (
    id,
    parent_id,
    permission_code,
    permission_name,
    permission_type,
    route_path,
    component_path,
    http_method,
    api_path,
    sort_order,
    status
) VALUES (
    2090358768733782029,
    2090358768733782026,
    'system:role',
    '角色管理',
    'MENU',
    '/admin/system/role',
    NULL,
    NULL,
    NULL,
    20,
    1
);

-- 权限管理
INSERT INTO sys_permission (
    id,
    parent_id,
    permission_code,
    permission_name,
    permission_type,
    route_path,
    component_path,
    http_method,
    api_path,
    sort_order,
    status
) VALUES (
    2090358768733782030,
    2090358768733782026,
    'system:permission',
    '权限管理',
    'MENU',
    '/admin/system/permission',
    NULL,
    NULL,
    NULL,
    30,
    1
);


-- =========================================================
-- 2. 新增缺失的 BUTTON 权限
-- =========================================================

-- 分配用户角色
INSERT INTO sys_permission (
    id,
    parent_id,
    permission_code,
    permission_name,
    permission_type,
    route_path,
    component_path,
    http_method,
    api_path,
    sort_order,
    status
) VALUES (
    2090358768733782028,
    2090358768733782027,
    'user:assign_role',
    '分配用户角色',
    'BUTTON',
    NULL,
    NULL,
    NULL,
    NULL,
    50,
    1
);

-- 查看权限
INSERT INTO sys_permission (
    id,
    parent_id,
    permission_code,
    permission_name,
    permission_type,
    route_path,
    component_path,
    http_method,
    api_path,
    sort_order,
    status
) VALUES (
    2090358768733782031,
    2090358768733782030,
    'permission:list',
    '查看权限',
    'BUTTON',
    NULL,
    NULL,
    NULL,
    NULL,
    10,
    1
);


-- =========================================================
-- 3. 调整原有 user:* 权限的 parent_id 和排序
-- =========================================================

UPDATE sys_permission
SET
    parent_id = 2090358768733782027,
    permission_name = '查看用户',
    permission_type = 'BUTTON',
    sort_order = 10
WHERE id = 2090358768733782017
  AND permission_code = 'user:list';

UPDATE sys_permission
SET
    parent_id = 2090358768733782027,
    permission_name = '新建用户',
    permission_type = 'BUTTON',
    sort_order = 20
WHERE id = 2090358768733782018
  AND permission_code = 'user:create';

UPDATE sys_permission
SET
    parent_id = 2090358768733782027,
    permission_name = '编辑用户',
    permission_type = 'BUTTON',
    sort_order = 30
WHERE id = 2090358768733782019
  AND permission_code = 'user:update';

UPDATE sys_permission
SET
    parent_id = 2090358768733782027,
    permission_name = '删除用户',
    permission_type = 'BUTTON',
    sort_order = 40
WHERE id = 2090358768733782020
  AND permission_code = 'user:delete';


-- =========================================================
-- 4. 调整原有 role:* 权限的 parent_id 和排序
-- =========================================================

UPDATE sys_permission
SET
    parent_id = 2090358768733782029,
    permission_name = '查看角色',
    permission_type = 'BUTTON',
    sort_order = 10
WHERE id = 2090358768733782021
  AND permission_code = 'role:list';

UPDATE sys_permission
SET
    parent_id = 2090358768733782029,
    permission_name = '新建角色',
    permission_type = 'BUTTON',
    sort_order = 20
WHERE id = 2090358768733782022
  AND permission_code = 'role:create';

UPDATE sys_permission
SET
    parent_id = 2090358768733782029,
    permission_name = '编辑角色',
    permission_type = 'BUTTON',
    sort_order = 30
WHERE id = 2090358768733782023
  AND permission_code = 'role:update';

UPDATE sys_permission
SET
    parent_id = 2090358768733782029,
    permission_name = '删除角色',
    permission_type = 'BUTTON',
    sort_order = 40
WHERE id = 2090358768733782024
  AND permission_code = 'role:delete';

UPDATE sys_permission
SET
    parent_id = 2090358768733782029,
    permission_name = '配置角色权限',
    permission_type = 'BUTTON',
    sort_order = 50
WHERE id = 2090358768733782025
  AND permission_code = 'role:assign_permission';


COMMIT;