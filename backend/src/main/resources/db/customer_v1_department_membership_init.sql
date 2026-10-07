-- Customer V1 department membership foundation (MySQL 8.0)
-- ONE-TIME DEVELOPMENT MIGRATION.
-- Execute manually in sections. Do not execute the ALTER section when dept_id already exists.
-- This migration never creates users, changes passwords, or changes user-role relations.

USE crm_system;
SET NAMES utf8mb4;

-- Step 1 / preflight: expected result is 0. If it is 1, STOP and skip the ALTER TABLE below.
SELECT COUNT(*) AS dept_id_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'sys_user'
  AND column_name = 'dept_id';

-- Run once only when the preceding count is 0.
ALTER TABLE sys_user
    ADD COLUMN dept_id BIGINT NULL COMMENT '所属部门，NULL表示未归属部门' AFTER email,
    ADD KEY idx_sys_user_dept (dept_id),
    ADD CONSTRAINT fk_sys_user_department
        FOREIGN KEY (dept_id) REFERENCES sys_department (id);

-- Step 2: stable Snowflake-style IDs are shared with database/init.sql.
-- An ID conflict or duplicate code other than the expected row must be investigated, not overwritten.
INSERT INTO sys_department (
    id, dept_code, dept_name, parent_id, status, sort, created_at, updated_at, deleted
)
SELECT 2206070000000000001, 'SALES_DEPT_01', '销售一部', NULL, 1, 10,
       CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_department WHERE dept_code = 'SALES_DEPT_01'
);

INSERT INTO sys_department (
    id, dept_code, dept_name, parent_id, status, sort, created_at, updated_at, deleted
)
SELECT 2206070000000000002, 'SALES_DEPT_02', '销售二部', NULL, 1, 20,
       CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_department WHERE dept_code = 'SALES_DEPT_02'
);

-- Both rows must match the stable IDs and be enabled before any user binding is allowed.
SET @eligible_sales_department_count := (
    SELECT COUNT(*)
    FROM sys_department
    WHERE (id, dept_code) IN (
        (2206070000000000001, 'SALES_DEPT_01'),
        (2206070000000000002, 'SALES_DEPT_02')
    )
      AND parent_id IS NULL
      AND status = 1
      AND deleted = 0
);

SELECT @eligible_sales_department_count AS eligible_sales_department_count;

-- Step 3 / mandatory preflight: this query must return exactly five expected rows.
-- Every row must show id_matched=1, status=1 and deleted=0.
-- STOP: if any user is missing or any value differs, do not execute the binding UPDATE statements.
SELECT
    expected.username AS expected_username,
    expected.user_id AS expected_user_id,
    actual.id AS actual_user_id,
    actual.username AS actual_username,
    actual.status,
    actual.deleted,
    actual.dept_id,
    (actual.id = expected.user_id AND actual.username = expected.username) AS id_matched
FROM (
    SELECT 2107447193143648258 AS user_id, 'sales_manager01' AS username
    UNION ALL SELECT 2107447512812527618, 'sales_staff01'
    UNION ALL SELECT 2107447685940813826, 'sales_staff02'
    UNION ALL SELECT 2107682836507561985, 'sales_manager02'
    UNION ALL SELECT 2107683040359124993, 'sales_staff03'
) AS expected
LEFT JOIN sys_user AS actual
       ON actual.id = expected.user_id
      AND actual.username = expected.username
ORDER BY expected.username;

-- The guard must return 5. It prevents partial binding when a required user is absent or unavailable.
SET @eligible_department_user_count := (
    SELECT COUNT(*)
    FROM sys_user
    WHERE (id, username) IN (
        (2107447193143648258, 'sales_manager01'),
        (2107447512812527618, 'sales_staff01'),
        (2107447685940813826, 'sales_staff02'),
        (2107682836507561985, 'sales_manager02'),
        (2107683040359124993, 'sales_staff03')
    )
      AND status = 1
      AND deleted = 0
);

SELECT @eligible_department_user_count AS eligible_department_user_count;

-- Existing assignments to another department are deliberately not overwritten.
UPDATE sys_user AS u
JOIN sys_department AS d ON d.dept_code = 'SALES_DEPT_01' AND d.deleted = 0
SET u.dept_id = d.id,
    u.updated_at = CURRENT_TIMESTAMP(3)
WHERE @eligible_department_user_count = 5
  AND @eligible_sales_department_count = 2
  AND d.id = 2206070000000000001
  AND d.status = 1
  AND (u.id, u.username) IN (
      (2107447193143648258, 'sales_manager01'),
      (2107447512812527618, 'sales_staff01'),
      (2107447685940813826, 'sales_staff02')
  )
  AND u.status = 1
  AND u.deleted = 0
  AND (u.dept_id IS NULL OR u.dept_id = d.id);

UPDATE sys_user AS u
JOIN sys_department AS d ON d.dept_code = 'SALES_DEPT_02' AND d.deleted = 0
SET u.dept_id = d.id,
    u.updated_at = CURRENT_TIMESTAMP(3)
WHERE @eligible_department_user_count = 5
  AND @eligible_sales_department_count = 2
  AND d.id = 2206070000000000002
  AND d.status = 1
  AND (u.id, u.username) IN (
      (2107682836507561985, 'sales_manager02'),
      (2107683040359124993, 'sales_staff03')
  )
  AND u.status = 1
  AND u.deleted = 0
  AND (u.dept_id IS NULL OR u.dept_id = d.id);

-- Step 4: only the intended business role changes data scope.
UPDATE sys_role
SET data_scope = 'DEPT',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE role_code = 'SALES_MANAGER'
  AND data_scope <> 'DEPT';

-- Verification 1: sys_user.dept_id, its index and FK must exist.
SELECT
    column_name,
    column_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'sys_user'
  AND column_name = 'dept_id';

SELECT
    constraint_name,
    column_name,
    referenced_table_name,
    referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND table_name = 'sys_user'
  AND column_name = 'dept_id';

SELECT index_name, non_unique, seq_in_index, column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'sys_user'
  AND column_name = 'dept_id'
ORDER BY index_name, seq_in_index;

-- Verification 2: both departments must be enabled and not deleted.
SELECT id, dept_code, dept_name, parent_id, status, sort, deleted
FROM sys_department
WHERE dept_code IN ('SALES_DEPT_01', 'SALES_DEPT_02')
ORDER BY sort, id;

-- Verification 3: all five users must resolve to the expected department code; no dept_id may be NULL.
SELECT u.id, u.username, u.status, u.deleted, u.dept_id, d.dept_code, d.dept_name
FROM sys_user AS u
LEFT JOIN sys_department AS d ON d.id = u.dept_id
WHERE u.username IN (
    'sales_manager01', 'sales_staff01', 'sales_staff02',
    'sales_manager02', 'sales_staff03'
)
ORDER BY d.sort, u.username;

SELECT COUNT(*) AS test_user_without_department_count
FROM sys_user
WHERE username IN (
    'sales_manager01', 'sales_staff01', 'sales_staff02',
    'sales_manager02', 'sales_staff03'
)
  AND dept_id IS NULL;

-- Verification 4: expected scopes are ALL, ALL, DEPT and SELF in this order.
SELECT role_code, data_scope, status, deleted
FROM sys_role
WHERE role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF')
ORDER BY FIELD(role_code, 'SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF');
