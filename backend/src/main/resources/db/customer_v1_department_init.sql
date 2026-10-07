-- Customer Management V1 department foundation (MySQL 8.0)
-- ONE-TIME DEVELOPMENT MIGRATION.
-- Review and execute manually only after confirming sys_department does not already exist.
-- This script creates the department schema only; it does not seed departments or modify users/roles.

USE crm_system;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_department (
  id BIGINT NOT NULL COMMENT '雪花主键',
  dept_code VARCHAR(64) NOT NULL COMMENT '部门编码',
  dept_name VARCHAR(64) NOT NULL COMMENT '部门名称',
  parent_id BIGINT NULL COMMENT '上级部门，NULL为顶级部门',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  sort INT NOT NULL DEFAULT 0 COMMENT '显示排序',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_department_code (dept_code),
  KEY idx_sys_department_parent (parent_id),
  CONSTRAINT fk_sys_department_parent FOREIGN KEY (parent_id) REFERENCES sys_department (id)
) ENGINE=InnoDB COMMENT='部门';

-- Verification 1: inspect exact column definitions without SELECT *.
SELECT
    column_name,
    column_type,
    is_nullable,
    column_default,
    column_key,
    extra
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'sys_department'
ORDER BY ordinal_position;

-- Verification 2: inspect primary key, unique key, parent index and self-reference FK.
SELECT
    constraint_name,
    constraint_type
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name = 'sys_department'
ORDER BY constraint_type, constraint_name;

SELECT
    index_name,
    non_unique,
    seq_in_index,
    column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'sys_department'
ORDER BY index_name, seq_in_index;

SELECT
    constraint_name,
    column_name,
    referenced_table_name,
    referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND table_name = 'sys_department'
  AND referenced_table_name IS NOT NULL
ORDER BY constraint_name, ordinal_position;
