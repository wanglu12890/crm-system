-- CRM System - MySQL 8.0 initialization
-- Execute with: mysql -h localhost -P 3306 -u root -p < database/init.sql

CREATE DATABASE IF NOT EXISTS crm_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE crm_system;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL COMMENT '雪花主键',
  username VARCHAR(64) NOT NULL COMMENT '登录名',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
  real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
  mobile VARCHAR(32) NULL COMMENT '手机号',
  email VARCHAR(128) NULL COMMENT '邮箱',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  last_login_at DATETIME(3) NULL COMMENT '最后登录时间',
  created_by BIGINT NULL COMMENT '创建人',
  updated_by BIGINT NULL COMMENT '更新人',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_mobile (mobile),
  KEY idx_sys_user_status (status, deleted)
) ENGINE=InnoDB COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT 'ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  remark VARCHAR(500) NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (role_code),
  KEY idx_sys_role_status (status, deleted)
) ENGINE=InnoDB COMMENT='系统角色';

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父权限 0为根节点',
  permission_code VARCHAR(128) NOT NULL COMMENT '权限标识',
  permission_name VARCHAR(64) NOT NULL,
  permission_type VARCHAR(16) NOT NULL COMMENT 'MENU/BUTTON/API',
  route_path VARCHAR(255) NULL,
  component_path VARCHAR(255) NULL,
  http_method VARCHAR(16) NULL,
  api_path VARCHAR(255) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_permission_code (permission_code),
  KEY idx_sys_permission_parent (parent_id, sort_order)
) ENGINE=InnoDB COMMENT='菜单与操作权限';

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, role_id),
  KEY idx_sys_user_role_role (role_id),
  CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS sys_role_permission (
  id BIGINT NOT NULL COMMENT '雪花主键',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_permission (role_id, permission_id),
  KEY idx_sys_role_permission_role (role_id),
  KEY idx_sys_role_permission_permission (permission_id),
  CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS customer (
  id BIGINT NOT NULL,
  customer_no VARCHAR(32) NOT NULL COMMENT '客户编号',
  customer_name VARCHAR(200) NOT NULL,
  customer_type VARCHAR(32) NOT NULL DEFAULT 'ENTERPRISE' COMMENT 'ENTERPRISE/INDIVIDUAL',
  customer_level VARCHAR(16) NULL COMMENT 'A/B/C/D',
  industry VARCHAR(64) NULL,
  source VARCHAR(64) NULL COMMENT '客户来源',
  phone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  province VARCHAR(64) NULL,
  city VARCHAR(64) NULL,
  address VARCHAR(500) NULL,
  owner_id BIGINT NOT NULL COMMENT '负责人',
  status VARCHAR(32) NOT NULL DEFAULT 'POTENTIAL' COMMENT 'POTENTIAL/ACTIVE/INACTIVE',
  remark VARCHAR(1000) NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_customer_no (customer_no),
  KEY idx_customer_name (customer_name),
  KEY idx_customer_owner_status (owner_id, status, deleted),
  CONSTRAINT fk_customer_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id)
) ENGINE=InnoDB COMMENT='客户';

CREATE TABLE IF NOT EXISTS contact (
  id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  contact_name VARCHAR(64) NOT NULL,
  gender TINYINT NULL COMMENT '0未知 1男 2女',
  position VARCHAR(64) NULL,
  mobile VARCHAR(32) NULL,
  telephone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '是否主联系人',
  decision_role VARCHAR(32) NULL COMMENT 'DECISION/INFLUENCER/USER/OTHER',
  birthday DATE NULL,
  remark VARCHAR(500) NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_contact_customer (customer_id, deleted),
  KEY idx_contact_mobile (mobile),
  CONSTRAINT fk_contact_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
) ENGINE=InnoDB COMMENT='客户联系人';

CREATE TABLE IF NOT EXISTS clue (
  id BIGINT NOT NULL,
  clue_no VARCHAR(32) NOT NULL,
  clue_name VARCHAR(100) NOT NULL,
  company_name VARCHAR(200) NULL,
  mobile VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  source VARCHAR(64) NULL,
  industry VARCHAR(64) NULL,
  owner_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/FOLLOWING/CONVERTED/INVALID',
  converted_customer_id BIGINT NULL,
  converted_at DATETIME(3) NULL,
  invalid_reason VARCHAR(500) NULL,
  remark VARCHAR(1000) NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_clue_no (clue_no),
  KEY idx_clue_owner_status (owner_id, status, deleted),
  KEY idx_clue_mobile (mobile),
  CONSTRAINT fk_clue_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id),
  CONSTRAINT fk_clue_customer FOREIGN KEY (converted_customer_id) REFERENCES customer (id)
) ENGINE=InnoDB COMMENT='销售线索';

CREATE TABLE IF NOT EXISTS business (
  id BIGINT NOT NULL,
  business_no VARCHAR(32) NOT NULL,
  business_name VARCHAR(200) NOT NULL,
  customer_id BIGINT NOT NULL,
  contact_id BIGINT NULL,
  owner_id BIGINT NOT NULL,
  stage VARCHAR(32) NOT NULL COMMENT 'DISCOVERY/PROPOSAL/NEGOTIATION/WON/LOST',
  amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  probability DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  expected_close_date DATE NULL,
  actual_close_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/WON/LOST',
  loss_reason VARCHAR(500) NULL,
  remark VARCHAR(1000) NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_business_no (business_no),
  KEY idx_business_customer (customer_id),
  KEY idx_business_owner_stage (owner_id, stage, deleted),
  CONSTRAINT fk_business_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
  CONSTRAINT fk_business_contact FOREIGN KEY (contact_id) REFERENCES contact (id),
  CONSTRAINT fk_business_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id),
  CONSTRAINT chk_business_probability CHECK (probability BETWEEN 0 AND 100),
  CONSTRAINT chk_business_amount CHECK (amount >= 0)
) ENGINE=InnoDB COMMENT='销售商机';

CREATE TABLE IF NOT EXISTS follow_record (
  id BIGINT NOT NULL,
  target_type VARCHAR(32) NOT NULL COMMENT 'CUSTOMER/CLUE/BUSINESS',
  target_id BIGINT NOT NULL COMMENT '业务对象ID，多态关联由Service校验',
  follow_type VARCHAR(32) NOT NULL COMMENT 'PHONE/VISIT/EMAIL/IM/OTHER',
  content TEXT NOT NULL,
  follow_at DATETIME(3) NOT NULL,
  next_follow_at DATETIME(3) NULL,
  owner_id BIGINT NOT NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_follow_record_target (target_type, target_id, follow_at),
  KEY idx_follow_record_next (owner_id, next_follow_at, deleted),
  CONSTRAINT fk_follow_record_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id)
) ENGINE=InnoDB COMMENT='跟进记录';

CREATE TABLE IF NOT EXISTS contract (
  id BIGINT NOT NULL,
  contract_no VARCHAR(64) NOT NULL,
  contract_name VARCHAR(200) NOT NULL,
  customer_id BIGINT NOT NULL,
  business_id BIGINT NULL,
  owner_id BIGINT NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  sign_date DATE NULL,
  start_date DATE NULL,
  end_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVING/ACTIVE/COMPLETED/VOID',
  file_url VARCHAR(500) NULL,
  remark VARCHAR(1000) NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted TINYINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract_no (contract_no),
  KEY idx_contract_customer_status (customer_id, status, deleted),
  KEY idx_contract_owner (owner_id),
  CONSTRAINT fk_contract_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
  CONSTRAINT fk_contract_business FOREIGN KEY (business_id) REFERENCES business (id),
  CONSTRAINT fk_contract_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id),
  CONSTRAINT chk_contract_amount CHECK (amount >= 0),
  CONSTRAINT chk_contract_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB COMMENT='销售合同';

SET FOREIGN_KEY_CHECKS = 1;
