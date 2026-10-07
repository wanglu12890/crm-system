-- CRM System - MySQL 8.0 initialization
-- Execute with: mysql -h localhost -P 3306 -u root -p < database/init.sql

CREATE DATABASE IF NOT EXISTS crm_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE crm_system;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL COMMENT '雪花主键',
  username VARCHAR(64) NOT NULL COMMENT '登录名',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
  real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
  mobile VARCHAR(32) NULL COMMENT '手机号',
  email VARCHAR(128) NULL COMMENT '邮箱',
  dept_id BIGINT NULL COMMENT '所属部门，NULL表示未归属部门',
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
  KEY idx_sys_user_status (status, deleted),
  KEY idx_sys_user_dept (dept_id),
  CONSTRAINT fk_sys_user_department FOREIGN KEY (dept_id) REFERENCES sys_department (id)
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
  owner_id BIGINT NULL COMMENT '负责人，NULL表示公海客户',
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
  contact_id BIGINT NULL COMMENT '本次跟进涉及的联系人',
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
  CONSTRAINT fk_follow_record_contact FOREIGN KEY (contact_id) REFERENCES contact (id),
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

-- Customer V1 销售团队基础数据。用稳定业务编码定位部门，不依赖显示名称。
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

-- Fixed IDs make a fresh database self-contained. Existing environments keep
-- their current role rows because role_code is the stable lookup key.
INSERT INTO sys_role (
    id, role_code, role_name, data_scope, status, remark,
    created_by, updated_by, created_at, updated_at, deleted, version
)
SELECT role_fixture.id, role_fixture.role_code, role_fixture.role_name,
       role_fixture.data_scope, 1, '系统内置角色', NULL, NULL,
       CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0
FROM (
    SELECT 2206070900000000001 AS id, 'SUPER_ADMIN' AS role_code,
           '超级管理员' AS role_name, 'ALL' AS data_scope
    UNION ALL SELECT 2206070900000000002, 'SYSTEM_ADMIN', '系统管理员', 'ALL'
    UNION ALL SELECT 2206070900000000003, 'SALES_MANAGER', '销售经理', 'DEPT'
    UNION ALL SELECT 2206070900000000004, 'SALES_STAFF', '销售人员', 'SELF'
) AS role_fixture
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role AS existing_role
    WHERE existing_role.role_code = role_fixture.role_code
);

-- Customer Management V1 permission tree. MENU nodes organize the tree;
-- BUTTON nodes are the stable functional authorities used by method security.
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

-- Resolve role IDs by their stable role_code. MENU nodes are intentionally not
-- included in this functional permission matrix or its 9/9/11/11 counts.
INSERT INTO sys_role_permission (id, role_id, permission_id, create_time)
SELECT 2206071100000000000 + role_fixture.id_offset + permission_fixture.id_offset,
       role.id, permission.id, CURRENT_TIMESTAMP(3)
FROM (
    SELECT 'SUPER_ADMIN' AS role_code, 0 AS id_offset
    UNION ALL SELECT 'SYSTEM_ADMIN', 100
    UNION ALL SELECT 'SALES_MANAGER', 200
    UNION ALL SELECT 'SALES_STAFF', 300
) AS role_fixture
JOIN sys_role AS role
  ON role.role_code = role_fixture.role_code
 AND role.deleted = 0
JOIN (
    SELECT 'customer:list' AS permission_code, 1 AS id_offset
    UNION ALL SELECT 'customer:create', 2
    UNION ALL SELECT 'customer:update', 3
    UNION ALL SELECT 'customer_pool:list', 4
    UNION ALL SELECT 'customer_pool:claim', 5
    UNION ALL SELECT 'contact:list', 6
    UNION ALL SELECT 'contact:create', 7
    UNION ALL SELECT 'contact:update', 8
    UNION ALL SELECT 'contact:delete', 9
    UNION ALL SELECT 'follow:list', 10
    UNION ALL SELECT 'follow:create', 11
) AS permission_fixture
JOIN sys_permission AS permission
  ON permission.permission_code = permission_fixture.permission_code
WHERE (
        role_fixture.role_code IN ('SALES_MANAGER', 'SALES_STAFF')
        OR permission_fixture.permission_code NOT IN ('customer_pool:claim', 'follow:create')
      )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission AS existing_relation
      WHERE existing_relation.role_id = role.id
        AND existing_relation.permission_id = permission.id
  );

SET FOREIGN_KEY_CHECKS = 1;
