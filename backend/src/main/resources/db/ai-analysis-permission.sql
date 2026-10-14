-- CRM AI Agent V1: ai:analysis permission and role bindings (MySQL 8.0)
-- Execute manually after reviewing the preconditions. The procedure provides
-- an atomic unit so that any failed precondition or write rolls back all DML.

USE crm_system;
SET NAMES utf8mb4;
DELIMITER $$

DROP PROCEDURE IF EXISTS crm_init_ai_analysis_permission$$
CREATE PROCEDURE crm_init_ai_analysis_permission()
BEGIN
    DECLARE ai_permission_id BIGINT DEFAULT 2214000000000000001;
    DECLARE ai_analysis_role_count INT DEFAULT 0;
    DECLARE permission_id_collision_count INT DEFAULT 0;
    DECLARE relation_id_collision_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

-- All four existing roles are required before relations are inserted.
SELECT COUNT(*)
      INTO ai_analysis_role_count
      FROM sys_role
     WHERE role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF')
       AND deleted = 0
;

SET @ai_analysis_role_count = ai_analysis_role_count;
IF @ai_analysis_role_count <> 4 THEN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ai:analysis initialization requires four active role records';
END IF;

SELECT COUNT(*)
  INTO permission_id_collision_count
  FROM sys_permission
 WHERE id = ai_permission_id
   AND permission_code <> 'ai:analysis'
   AND NOT EXISTS (
       SELECT 1 FROM sys_permission WHERE permission_code = 'ai:analysis'
   );

IF permission_id_collision_count > 0 THEN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ai:analysis permission ID conflicts with an existing permission';
END IF;

-- No analysis parent permission currently exists, so this API authority is a
-- root node. api_path excludes the application's /api context path.
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type,
    route_path, component_path, http_method, api_path,
    sort_order, status, created_at, updated_at
)
SELECT ai_permission_id, 0, 'ai:analysis', 'AI智能分析', 'API',
       NULL, NULL, 'POST', '/ai/analysis',
       10, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'ai:analysis'
);

-- Preserve status: rerunning initialization must not re-enable a permission
-- that an administrator deliberately disabled.
UPDATE sys_permission
   SET parent_id = 0,
       permission_name = 'AI智能分析',
       permission_type = 'API',
       route_path = NULL,
       component_path = NULL,
       http_method = 'POST',
       api_path = '/ai/analysis',
       sort_order = 10,
       updated_at = CURRENT_TIMESTAMP(3)
 WHERE permission_code = 'ai:analysis';

SELECT id INTO ai_permission_id
  FROM sys_permission
 WHERE permission_code = 'ai:analysis';

-- Stable relation IDs follow the deterministic IDs used by existing seed SQL.
-- A conflicting primary key fails the whole transaction before any relation is inserted.
SELECT COUNT(*)
  INTO relation_id_collision_count
  FROM (
      SELECT 'SUPER_ADMIN' AS role_code, 2214000100000000001 AS relation_id
      UNION ALL SELECT 'SYSTEM_ADMIN', 2214000100000000002
      UNION ALL SELECT 'SALES_MANAGER', 2214000100000000003
      UNION ALL SELECT 'SALES_STAFF', 2214000100000000004
  ) AS relation_fixture
  JOIN sys_role AS role
    ON role.role_code = relation_fixture.role_code
   AND role.deleted = 0
  JOIN sys_role_permission AS id_collision
    ON id_collision.id = relation_fixture.relation_id
 WHERE NOT EXISTS (
       SELECT 1
         FROM sys_role_permission AS existing_relation
        WHERE existing_relation.role_id = role.id
          AND existing_relation.permission_id = ai_permission_id
   )
   AND (id_collision.role_id <> role.id
        OR id_collision.permission_id <> ai_permission_id);

IF relation_id_collision_count > 0 THEN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'ai:analysis role-permission relation ID conflicts with existing data';
END IF;

INSERT INTO sys_role_permission (id, role_id, permission_id, create_time)
SELECT relation_fixture.relation_id,
       role.id, permission.id, CURRENT_TIMESTAMP(3)
  FROM (
      SELECT 'SUPER_ADMIN' AS role_code, 2214000100000000001 AS relation_id
      UNION ALL SELECT 'SYSTEM_ADMIN', 2214000100000000002
      UNION ALL SELECT 'SALES_MANAGER', 2214000100000000003
      UNION ALL SELECT 'SALES_STAFF', 2214000100000000004
  ) AS relation_fixture
  JOIN sys_role AS role
    ON role.role_code = relation_fixture.role_code
   AND role.deleted = 0
  JOIN sys_permission AS permission ON permission.permission_code = 'ai:analysis'
 WHERE @ai_analysis_role_count = 4
   AND NOT EXISTS (
       SELECT 1
         FROM sys_role_permission AS existing_relation
        WHERE existing_relation.role_id = role.id
          AND existing_relation.permission_id = permission.id
   );

COMMIT;
END$$

DELIMITER ;

CALL crm_init_ai_analysis_permission();
DROP PROCEDURE IF EXISTS crm_init_ai_analysis_permission;

-- Verification 1: expect one API permission. status may remain 0 when the
-- existing permission was deliberately disabled before this script was rerun.
SELECT id, parent_id, permission_code, permission_name, permission_type,
       http_method, api_path, sort_order, status
  FROM sys_permission
 WHERE permission_code = 'ai:analysis'
   AND permission_type = 'API'
   AND http_method = 'POST'
   AND api_path = '/ai/analysis';

-- Verification 2: expected four rows, one for every existing role.
SELECT role.role_code, permission.permission_code, permission.status
  FROM sys_role AS role
  JOIN sys_role_permission AS relation ON relation.role_id = role.id
  JOIN sys_permission AS permission ON permission.id = relation.permission_id
 WHERE role.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF')
   AND permission.permission_code = 'ai:analysis'
 ORDER BY FIELD(role.role_code, 'SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'SALES_STAFF');

-- Verification 3: expected no rows.
SELECT role_id, permission_id, COUNT(*) AS duplicate_count
  FROM sys_role_permission
 WHERE permission_id = (
       SELECT id FROM sys_permission WHERE permission_code = 'ai:analysis' LIMIT 1
 )
 GROUP BY role_id, permission_id
HAVING COUNT(*) > 1;
