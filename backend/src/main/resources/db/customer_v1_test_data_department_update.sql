-- Customer V1 existing fixture department-owner synchronization (MySQL 8.0)
-- ONE-TIME DEVELOPMENT DATA UPDATE.
-- Review and execute manually only when the old Customer V1 fixture is already present.
-- This script changes only C01 and C12; it does not alter schema, departments, users or roles.

USE crm_system;
SET NAMES utf8mb4;

-- Preflight: expected result is 2. A different value means the fixture is absent,
-- already updated, or manually changed. STOP and investigate instead of overwriting it.
SET @eligible_old_customer_count := (
    SELECT COUNT(*)
    FROM customer
    WHERE deleted = 0
      AND (
          (id = 2206100000000000001
           AND customer_no = 'TEST-CUST-V1-001'
           AND owner_id = 2107447193143648258)
          OR
          (id = 2206100000000000012
           AND customer_no = 'TEST-CUST-V1-012'
           AND owner_id = 2107447512812527618)
      )
);

-- Expected result is 2: manager02 and staff03 must be enabled, undeleted and in SALES_DEPT_02.
SET @eligible_new_owner_count := (
    SELECT COUNT(*)
    FROM sys_user AS u
    JOIN sys_department AS d ON d.id = u.dept_id
    WHERE (u.id, u.username) IN (
        (2107682836507561985, 'sales_manager02'),
        (2107683040359124993, 'sales_staff03')
    )
      AND u.status = 1
      AND u.deleted = 0
      AND d.id = 2206070000000000002
      AND d.dept_code = 'SALES_DEPT_02'
      AND d.status = 1
      AND d.deleted = 0
);

SELECT
    @eligible_old_customer_count AS eligible_old_customer_count,
    @eligible_new_owner_count AS eligible_new_owner_count;

-- STOP unless both values above equal 2. Guards below prevent partial or unexpected overwrite.
START TRANSACTION;

UPDATE customer
SET owner_id = 2107682836507561985,
    remark = '销售二部经理 DEPT 基准；无联系人、无跟进场景',
    created_by = 2107682836507561985,
    updated_by = 2107682836507561985,
    updated_at = '2026-09-28 15:00:00.000'
WHERE @eligible_old_customer_count = 2
  AND @eligible_new_owner_count = 2
  AND id = 2206100000000000001
  AND customer_no = 'TEST-CUST-V1-001'
  AND owner_id = 2107447193143648258
  AND deleted = 0;

UPDATE customer
SET owner_id = 2107683040359124993,
    remark = '销售二部销售 SELF 基准；无联系人、无跟进',
    created_by = 2107683040359124993,
    updated_by = 2107683040359124993,
    updated_at = '2026-07-20 11:00:00.000'
WHERE @eligible_old_customer_count = 2
  AND @eligible_new_owner_count = 2
  AND id = 2206100000000000012
  AND customer_no = 'TEST-CUST-V1-012'
  AND owner_id = 2107447512812527618
  AND deleted = 0;

COMMIT;

-- Verification: expected owner mappings are C01 -> manager02 and C12 -> staff03.
SELECT
    c.id,
    c.customer_no,
    c.owner_id,
    u.username,
    d.dept_code,
    d.dept_name
FROM customer AS c
JOIN sys_user AS u ON u.id = c.owner_id
JOIN sys_department AS d ON d.id = u.dept_id
WHERE c.id IN (2206100000000000001, 2206100000000000012)
ORDER BY c.id;
