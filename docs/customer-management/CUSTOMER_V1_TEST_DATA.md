# Customer Management V1 Test Data

## 1. Purpose

本文档说明 `database/test-data/customer_v1_test_data.sql` 中的固定开发测试数据。数据只用于 Customer Management V1 的前后端开发、Postman 验证和后续自动化测试，不包含真实个人或企业信息，也不代表正式客户编号生成算法。

SQL 当前仅生成，未自动导入数据库。

## 2. Required Existing Users

导入前必须确认下列三个用户真实存在、ID 与 username 一致、`status=1` 且 `deleted=0`：

| Role | Username | User ID |
|---|---|---:|
| SALES_MANAGER | sales_manager01 | 2107447193143648258 |
| SALES_STAFF | sales_staff01 | 2107447512812527618 |
| SALES_STAFF | sales_staff02 | 2107447685940813826 |

如果任何用户缺失、ID 不匹配、已停用或已删除，**STOP**，不要继续导入。脚本不会创建用户、角色或用户角色关系。

## 3. Dataset Summary

| Entity | ID range | Count | Notes |
|---|---|---:|---|
| Customer | 2206100000000000001–2206100000000000024 | 24 | manager、staff01、staff02、公海各 6 |
| Contact | 2206101000000000001–2206101000000000027 | 27 | 26 active + 1 deleted filter sample |
| FollowRecord | 2206102000000000001–2206102000000000036 | 36 | 全部为 CUSTOMER target、deleted=0 |

所有 Customer 使用 `TEST-CUST-V1-###` 编号。该编号仅用于稳定识别测试数据，不是未来 Customer Create API 的编号算法。

## 4. Test Scenario Matrix

| Key | Customer ID | customer_no | Customer | Owner | Purpose |
|---|---:|---|---|---|---|
| C01 | 2206100000000000001 | TEST-CUST-V1-001 | 星河制造测试有限公司 | manager01 | 无联系人、无跟进 |
| C02 | 2206100000000000002 | TEST-CUST-V1-002 | 启明科技测试有限公司 | manager01 | 1 联系人且 primary、1 跟进 |
| C03 | 2206100000000000003 | TEST-CUST-V1-003 | 安澜医疗测试中心 | manager01 | 2 联系人、1 primary、3 跟进 |
| C04 | 2206100000000000004 | TEST-CUST-V1-004 | 筑梦建筑测试集团 | manager01 | 3 联系人、1 primary、5 次有序跟进 |
| C05 | 2206100000000000005 | TEST-CUST-V1-005 | 共创零售测试中心 | manager01 | 2 联系人、无 primary；与 C17 同名 |
| C06 | 2206100000000000006 | TEST-CUST-V1-006 | 顾清和（测试客户） | manager01 | Individual、NULL 地址、单次跟进 |
| C07 | 2206100000000000007 | TEST-CUST-V1-007 | 远帆物流测试有限公司 | staff01 | SELF 允许访问基准 |
| C08 | 2206100000000000008 | TEST-CUST-V1-008 | 晨曦教育测试学院 | staff01 | 多联系人/多跟进；与 C14 共享电话 |
| C09 | 2206100000000000009 | TEST-CUST-V1-009 | 蓝湾金融测试服务有限公司 | staff01 | 2 联系人但无 primary |
| C10 | 2206100000000000010 | TEST-CUST-V1-010 | 林知夏（测试客户） | staff01 | Individual、3 次跟进 |
| C11 | 2206100000000000011 | TEST-CUST-V1-011 | 云阶信息测试工作室 | staff01 | 1 个 deleted Contact 样本 |
| C12 | 2206100000000000012 | TEST-CUST-V1-012 | 禾光农业测试合作社 | staff01 | 无联系人、无跟进 |
| C13 | 2206100000000000013 | TEST-CUST-V1-013 | 凌云软件测试有限公司 | staff02 | SELF 允许访问基准 |
| C14 | 2206100000000000014 | TEST-CUST-V1-014 | 新叶医疗测试有限公司 | staff02 | 与 C08 共享电话，跨用户边界 |
| C15 | 2206100000000000015 | TEST-CUST-V1-015 | 峰谷制造测试厂 | staff02 | 3 联系人、4 次跟进 |
| C16 | 2206100000000000016 | TEST-CUST-V1-016 | 周星遥（测试客户） | staff02 | Individual、2 次跟进 |
| C17 | 2206100000000000017 | TEST-CUST-V1-017 | 共创零售测试中心 | staff02 | 与 C05 同名但编号不同 |
| C18 | 2206100000000000018 | TEST-CUST-V1-018 | 北辰建筑测试设计院 | staff02 | 无跟进 |
| POOL-STAFF01 | 2206100000000000019 | TEST-CUST-V1-019 | 公海一号星港物流测试公司 | NULL | 专供 staff01 领取 |
| POOL-STAFF02 | 2206100000000000020 | TEST-CUST-V1-020 | 公海二号青禾教育测试中心 | NULL | 专供 staff02 领取 |
| POOL-CONCURRENCY | 2206100000000000021 | TEST-CUST-V1-021 | 公海并发领取专用测试客户 | NULL | staff01/staff02 同时领取；预期仅一方成功 |
| C22 | 2206100000000000022 | TEST-CUST-V1-022 | 公海历史跟进测试科技公司 | NULL | 有联系人及历史跟进 |
| C23 | 2206100000000000023 | TEST-CUST-V1-023 | 苏明月（公海测试客户） | NULL | Individual、无联系人/跟进 |
| C24 | 2206100000000000024 | TEST-CUST-V1-024 | 公海远景零售测试门店 | NULL | 有历史跟进、无具体联系人 |

不要在普通浏览测试中修改 POOL-STAFF01、POOL-STAFF02 和 POOL-CONCURRENCY。领取测试后应使用精确 ID 将整套数据清理并重新导入，而不是随意手工恢复 owner。

## 5. Authorization Test Scenarios

以下均为未来预期行为，不是当前已实现 API 的测试结果：

| ID | Actor | Target | Expected | Purpose |
|---|---|---|---|---|
| AUTH-01 | sales_staff01 | C07/C08 等 staff01 Customer | Allowed | SELF 正向 |
| AUTH-02 | sales_staff01 | C13/C14 等 staff02 Customer | Forbidden | 跨销售访问 |
| AUTH-03 | sales_staff02 | C07/C08 等 staff01 Customer | Forbidden | 反向跨销售访问 |
| AUTH-04 | sales_manager01 | C07 与 C13 | Allowed | Manager ALL |
| AUTH-05 | sales_staff01 | C14 的 Contact 2206101000000000017 | Forbidden | 不能通过 Contact ID 绕过 Customer SELF |
| AUTH-06 | sales_staff01 | C14 的 FollowRecord 2206102000000000025 | Forbidden | 不能通过 FollowRecord ID 绕过 Customer SELF |
| AUTH-07 | sales_staff01 | 公海列表 | Allowed with planned `customer_pool:list` | 公海独立权限 |
| AUTH-08 | sales_staff01 | POOL-STAFF01 | Allowed with planned `customer_pool:claim` | staff 领取 |
| AUTH-09 | sales_manager01 | POOL-CONCURRENCY 或其他公海客户 | Allowed with planned permission | manager 领取 |

SUPER_ADMIN 和 SYSTEM_ADMIN 的真实 ID 未在任务中提供，因此测试数据没有虚构其审计 ID。其查看/禁止领取规则应在业务自动化测试中通过认证 principal 模拟。

## 6. Coverage

- Status：POTENTIAL、ACTIVE、INACTIVE 在四个 owner 组中均有覆盖。
- Level：A、B、C、D、NULL 均有覆盖。
- Type：ENTERPRISE 为主，INDIVIDUAL 共 4 条。
- Industry：制造业、信息技术、金融、教育、医疗、零售、建筑、物流、其他。
- Source：官网咨询、电话营销、客户推荐、线下活动、线上推广、销售开发、其他。
- Duplicate rules：C05/C17 同名；C08/C14 共享联系电话，但 customer_no 始终唯一。
- NULL fields：覆盖 phone、email、level、省市地址和 remark 等空值。
- Contact：覆盖 0、1、2、3 个联系人；覆盖无 primary 和一个 primary；永不超过一个有效 primary。
- Contact values：gender 0/1/2，decision_role 四种值，以及联系方式、生日、备注空值。
- Deleted filtering：Contact 2206101000000000015 是唯一 `deleted=1` 样本，不是 primary，不计入正常联系人统计。
- Follow count：覆盖 0、1、2、3、4、5 次跟进。
- Follow type：PHONE、VISIT、EMAIL、IM、OTHER 全覆盖。
- Follow contact：同时覆盖 `contact_id IS NULL` 和非空，所有非空联系人均属于目标 Customer。
- Follow time：覆盖较早记录、30–90 天、近 30 天和近 7 天；next_follow_at 同时覆盖 NULL、已过去时间和近期未来时间。

## 7. Public-pool Historical Follow Records

C22 和 C24 当前 `owner_id=NULL`，但保留少量历史 FollowRecord。其语义是客户过去由销售跟进，后来进入公海；FollowRecord.owner_id 是当时实际执行跟进的销售人员，不代表客户当前 owner。

其他公海领取专用记录没有新跟进，避免测试语义混乱。

## 8. Import and Cleanup

### Import

1. 先将上一任务提供的 schema migration 应用到当前开发数据库，确保 Customer owner 可空且 FollowRecord 已有 contact_id。
2. 打开 SQL，单独执行 Pre-check，核对三个用户。
3. 执行 collision 查询，首次导入应返回 0 行。
4. 确认无冲突后执行 Customer、Contact、FollowRecord 插入事务。
5. 执行文件末尾 Verification Queries。

命令行示例：

```powershell
cmd /c "mysql -h localhost -P 3306 -u root -p crm_system < database\test-data\customer_v1_test_data.sql"
```

由于命令行整文件执行不能代替人工核对，推荐先在 MySQL 客户端中分段执行 Pre-check。不要把密码写入命令或脚本。

### Cleanup and re-import

SQL 中的 Cleanup 默认被注释。需要重置时，人工确认固定 ID 范围后取消注释，按以下顺序执行：

1. FollowRecord
2. Contact
3. Customer

Cleanup 只匹配本数据集固定 ID 范围，Customer 还要求 `TEST-CUST-V1-%` 前缀；不会删除其他业务数据。清理后重新执行插入部分。

## 9. Important Limitations

- 数据集没有创建权限、用户、角色或用户角色关系。
- 数据集不验证尚未实现的 Customer API 或数据范围代码。
- 脚本不是数据库迁移工具，不保证在已存在同 ID/编号数据时自动覆盖。
- 公海领取会改变固定测试数据，执行领取类测试前后需要管理测试夹具状态。
- 数据仅面向 Customer、Contact、FollowRecord；不生成 Clue、Business、Contract。
