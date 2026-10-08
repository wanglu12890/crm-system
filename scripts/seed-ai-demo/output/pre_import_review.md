# CRM AI Agent V1 Pre-import Review

Review time: 2026-10-08 (Asia/Shanghai)  
Dataset: `crm_ai_agent_v1`  
Conclusion: **PASS — technically safe to proceed only after explicit user authorization**

No dataset row was inserted during this review.

## 1. Target database

| Item | Observed value |
|---|---|
| Spring profile | `local` (default from `application.yml`) |
| Database | MySQL 8.0.46 |
| Host | local development machine (redacted hostname) |
| Port | 3306 |
| Schema | `crm_system` |
| Environment conclusion | Local development database; no production endpoint or production profile is active |

Credentials and JWT secrets were not copied into this report or any command-line artifact.

## 2. Artifact integrity

| Check | Result |
|---|---|
| `validate.py` distributions and relationships | PASS |
| Configuration matches manifest | PASS |
| Manifest counts | Customer 300, Contact 450, FollowRecord 1,200 |
| SQL SHA-256 | `6061330abc1d60c1978a010c3622bea212b7e98adc1e69790ee2c24c2af0cd7a` |
| SQL SHA-256 matches manifest | PASS |
| Primary IDs/customer numbers unique in generated dataset | PASS |
| Contact/customer and follow/customer/contact relationships | PASS |
| Province/city mapping | PASS |
| Follow time boundary | PASS |
| Synthetic contact information only | PASS (`000-...`, `example.com`, `example.org`) |

The batch uses exact contiguous ID sets recorded in the manifest:

- Customer: `2308100000000000001`–`2308100000000000300`
- Contact: `2308101000000000001`–`2308101000000000450`
- FollowRecord: `2308102000000000001`–`2308102000000001200`

## 3. SQL safety review

`pre_import_check.py` result: **PASS**.

- 13 executable statements were identified.
- The only INSERT targets, in order, are `customer`, `contact`, `follow_record`.
- Each table is inserted once with ordinary `INSERT INTO`.
- No DROP, TRUNCATE, DELETE, UPDATE, ALTER, CREATE, REPLACE or INSERT IGNORE exists in the import SQL.
- No foreign-key disabling, stored procedure, dynamic SQL, external file operation or schema modification exists.
- The SQL uses `USE crm_system`, `SET NAMES utf8mb4`, read-only pre/verification SELECTs, one InnoDB transaction and COMMIT.
- It does not modify `sys_user`, `sys_role`, `sys_department` or their relationships.

Operational note: the SQL pre-checks are informational SELECTs rather than procedural abort guards. They have therefore been run and reviewed independently before considering the import command.

## 4. Current database baseline

| Table | Before import | Expected addition | Expected after import |
|---|---:|---:|---:|
| customer | 27 | 300 | 327 |
| contact | 27 | 450 | 477 |
| follow_record | 36 | 1,200 | 1,236 |

Existing ID ranges end below the new batch ranges. Range checks are supplementary only; exact batch identity is defined by the manifest.

## 5. Collision and repeated-import check

| Key set | Existing hits |
|---|---:|
| Customer manifest ID set | 0 |
| Generated `KH230810...` customer numbers | 0 |
| Contact manifest ID set | 0 |
| FollowRecord manifest ID set | 0 |

Conclusion: the batch is not already imported and no partial batch was detected. No existing row will be overwritten. A repeated import would fail primary/unique constraints rather than being silently ignored.

## 6. Dependency and schema checks

All five configured users exist with matching IDs, enabled/non-deleted status, department, role and data scope:

| User | Department | Role | Scope |
|---|---|---|---|
| sales_manager01 | SALES_DEPT_01 | SALES_MANAGER | DEPT |
| sales_staff01 | SALES_DEPT_01 | SALES_STAFF | SELF |
| sales_staff02 | SALES_DEPT_01 | SALES_STAFF | SELF |
| sales_manager02 | SALES_DEPT_02 | SALES_MANAGER | DEPT |
| sales_staff03 | SALES_DEPT_02 | SALES_STAFF | SELF |

The live tables are InnoDB. Required foreign keys exist for Customer owner, Contact customer, FollowRecord contact and FollowRecord owner. Actual table columns match the generated INSERT column lists.

## 7. Backup

Backup status: **PASS**

- File: `database/backups/crm_ai_agent_v1_pre_import_20261008.sql`
- Scope: consistent `--single-transaction` dump of the complete pre-import `customer`, `contact` and `follow_record` tables, including schema and data
- Size: 32,978 bytes
- SHA-256: `84CF31C936E002115969FE20CF12E573B7AB7032F3FD317E84C1DE5577C98FD3`
- Verification: file exists, is non-empty/readable and contains CREATE/INSERT sections for all three tables
- Repository handling: `database/backups/` is ignored by Git

## 8. Risks and decision

Remaining controlled risks:

1. Importing 1,950 rows is an intentional development-database write and still requires explicit user confirmation.
2. Pre-check SELECTs do not automatically abort the SQL file; the independently reviewed zero-collision and dependency results are the safety gate.
3. Integration and data-scope HTTP verification can only be performed after import; unimplemented public-pool/AI Agent APIs cannot be claimed as tested.

Final pre-import decision: **PASS**. The local development database is backed up and the batch can be imported as one transaction after explicit authorization. No import has been executed yet.
