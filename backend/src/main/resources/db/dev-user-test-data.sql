-- CRM development-only user list test data (MySQL 8.0)
-- Prerequisite: database/init.sql has been executed and DataInitializer has created admin.
-- Development password for the five test users: 123456
-- BCrypt strength: 10; no plaintext password is stored below.

USE crm_system;

SET NAMES utf8mb4;
START TRANSACTION;

-- DataInitializer owns admin's password, status and SUPER_ADMIN relation.
-- This script only supplies fields needed by the user-list fixture.
UPDATE sys_user
   SET real_name = '系统管理员',
       mobile = '13800000000',
       created_at = '2026-08-03 00:00:00.000'
 WHERE username = 'admin'
   AND deleted = 0;

-- Insert missing development roles. IDs follow the current non-auto-increment BIGINT design.
INSERT INTO sys_role (
    id, role_code, role_name, data_scope, status, remark,
    created_by, updated_by, created_at, updated_at, deleted, version
)
SELECT next_id, 'SALES_MANAGER', '销售经理', 'DEPT', 1, '开发测试数据角色',
       NULL, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0
  FROM (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM sys_role) AS role_id_source
 WHERE NOT EXISTS (
       SELECT 1 FROM sys_role WHERE role_code = 'SALES_MANAGER'
 );

INSERT INTO sys_role (
    id, role_code, role_name, data_scope, status, remark,
    created_by, updated_by, created_at, updated_at, deleted, version
)
SELECT next_id, 'SALES_STAFF', '销售人员', 'SELF', 1, '开发测试数据角色',
       NULL, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0
  FROM (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM sys_role) AS role_id_source
 WHERE NOT EXISTS (
       SELECT 1 FROM sys_role WHERE role_code = 'SALES_STAFF'
 );

-- Insert each missing test user with a generated BIGINT ID.
-- Existing test usernames are updated below, making the fixture repeatable.
INSERT INTO sys_user (
    id, username, password_hash, real_name, mobile, email, status,
    last_login_at, created_by, updated_by, created_at, updated_at, deleted, version
)
SELECT next_id, 'zhangsan', '$2a$10$KobnEtJFHioC.XCTgjlrz..VceVxy2Q0/JWY/tk7CYW6Wk/eijcw2',
       '张三', '13900000000', NULL, 1, NULL, NULL, NULL,
       '2026-08-03 00:00:00.000', '2026-08-03 00:00:00.000', 0, 0
  FROM (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM sys_user) AS user_id_source
 WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'zhangsan');

INSERT INTO sys_user (
    id, username, password_hash, real_name, mobile, email, status,
    last_login_at, created_by, updated_by, created_at, updated_at, deleted, version
)
SELECT next_id, 'lisi', '$2a$10$KobnEtJFHioC.XCTgjlrz..VceVxy2Q0/JWY/tk7CYW6Wk/eijcw2',
       '李四', '13700000000', NULL, 1, NULL, NULL, NULL,
       '2026-08-02 00:00:00.000', '2026-08-02 00:00:00.000', 0, 0
  FROM (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM sys_user) AS user_id_source
 WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'lisi');

INSERT INTO sys_user (
    id, username, password_hash, real_name, mobile, email, status,
    last_login_at, created_by, updated_by, created_at, updated_at, deleted, version
)
SELECT next_id, 'wangwu', '$2a$10$KobnEtJFHioC.XCTgjlrz..VceVxy2Q0/JWY/tk7CYW6Wk/eijcw2',
       '王五', '13600000000', NULL, 0, NULL, NULL, NULL,
       '2026-08-01 00:00:00.000', '2026-08-01 00:00:00.000', 0, 0
  FROM (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM sys_user) AS user_id_source
 WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'wangwu');

INSERT INTO sys_user (
    id, username, password_hash, real_name, mobile, email, status,
    last_login_at, created_by, updated_by, created_at, updated_at, deleted, version
)
SELECT next_id, 'zhaoliu', '$2a$10$KobnEtJFHioC.XCTgjlrz..VceVxy2Q0/JWY/tk7CYW6Wk/eijcw2',
       '赵六', '13500000000', NULL, 1, NULL, NULL, NULL,
       '2026-07-31 00:00:00.000', '2026-07-31 00:00:00.000', 0, 0
  FROM (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM sys_user) AS user_id_source
 WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'zhaoliu');

INSERT INTO sys_user (
    id, username, password_hash, real_name, mobile, email, status,
    last_login_at, created_by, updated_by, created_at, updated_at, deleted, version
)
SELECT next_id, 'sunqi', '$2a$10$KobnEtJFHioC.XCTgjlrz..VceVxy2Q0/JWY/tk7CYW6Wk/eijcw2',
       '孙七', '13400000000', NULL, 1, NULL, NULL, NULL,
       '2026-07-30 00:00:00.000', '2026-07-30 00:00:00.000', 0, 0
  FROM (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM sys_user) AS user_id_source
 WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'sunqi');

-- Reapply deterministic fixture values when a development test user already exists.
-- This intentionally resets only these five development accounts to password 123456.
UPDATE sys_user
   SET password_hash = '$2a$10$KobnEtJFHioC.XCTgjlrz..VceVxy2Q0/JWY/tk7CYW6Wk/eijcw2',
       real_name = CASE username
           WHEN 'zhangsan' THEN '张三' WHEN 'lisi' THEN '李四' WHEN 'wangwu' THEN '王五'
           WHEN 'zhaoliu' THEN '赵六' WHEN 'sunqi' THEN '孙七' END,
       mobile = CASE username
           WHEN 'zhangsan' THEN '13900000000' WHEN 'lisi' THEN '13700000000'
           WHEN 'wangwu' THEN '13600000000' WHEN 'zhaoliu' THEN '13500000000'
           WHEN 'sunqi' THEN '13400000000' END,
       status = CASE WHEN username = 'wangwu' THEN 0 ELSE 1 END,
       created_at = CASE username
           WHEN 'zhangsan' THEN '2026-08-03 00:00:00.000'
           WHEN 'lisi' THEN '2026-08-02 00:00:00.000'
           WHEN 'wangwu' THEN '2026-08-01 00:00:00.000'
           WHEN 'zhaoliu' THEN '2026-07-31 00:00:00.000'
           WHEN 'sunqi' THEN '2026-07-30 00:00:00.000' END,
       deleted = 0
 WHERE username IN ('zhangsan', 'lisi', 'wangwu', 'zhaoliu', 'sunqi');

-- Add required role relations without deleting any existing roles or permissions.
INSERT IGNORE INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP(3)
  FROM sys_user AS u
  JOIN sys_role AS r ON r.role_code = 'SALES_STAFF'
 WHERE u.username IN ('zhangsan', 'wangwu', 'zhaoliu');

INSERT IGNORE INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP(3)
  FROM sys_user AS u
  JOIN sys_role AS r ON r.role_code = 'SALES_MANAGER'
 WHERE u.username IN ('lisi', 'sunqi');

COMMIT;

-- Verification query
SELECT
    u.id,
    u.username,
    u.real_name,
    u.mobile,
    u.status,
    u.deleted,
    u.created_at,
    r.role_code,
    r.role_name
FROM sys_user AS u
LEFT JOIN sys_user_role AS ur ON ur.user_id = u.id
LEFT JOIN sys_role AS r ON r.id = ur.role_id
WHERE u.username IN ('admin', 'zhangsan', 'lisi', 'wangwu', 'zhaoliu', 'sunqi')
ORDER BY u.created_at DESC, u.username, r.role_code;
