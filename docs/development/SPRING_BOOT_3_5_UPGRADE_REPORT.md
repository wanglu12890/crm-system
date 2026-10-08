# Spring Boot 3.5.16 升级与 CRM 回归验证报告

> 执行日期：2026-10-08  
> 最终结论：**PARTIAL PASS**

## 1. 升级概况

| 项目 | 升级前 | 升级后 |
|---|---|---|
| Spring Boot Parent | 3.3.5 | 3.5.16 |
| Java | 17.0.19 | 17.0.19 |
| Maven | 3.9.16 | 3.9.16 |
| MyBatis-Plus | 3.5.7 | 3.5.7 |
| JJWT | 0.12.6 | 0.12.6 |

Spring 官方已发布 Spring Boot 3.5.16，并说明该版本最低要求 Java 17、Maven 3.6.3；当前环境满足要求。3.5.16 是 3.5.x 最后一个 OSS 版本，后续长期维护计划需要单独评估，不属于本次升级范围。

实际修改文件：

- `backend/pom.xml`：仅将 Spring Boot Parent 从 3.3.5 调整为 3.5.16。
- `docs/CODEX_CONTEXT.md`：同步当前后端技术基线。
- `docs/development/SPRING_BOOT_3_5_UPGRADE_REPORT.md`：记录升级与验证结果。

未修改前端、数据库结构、初始化数据、业务规则、安全规则或权限矩阵；未引入 Spring AI。

## 2. 依赖兼容性

升级后执行了 `mvn dependency:tree -Dscope=runtime`，解析成功且未发现并存的 Spring Boot、Spring Framework 或 Spring Security 冲突版本。关键解析结果如下：

| 依赖 | 升级后版本 | 说明 |
|---|---:|---|
| Spring Boot | 3.5.16 | 由 Parent 统一管理 |
| Spring Framework | 6.2.19 | 由 Boot BOM 管理 |
| Spring Security | 6.5.11 | 由 Boot BOM 管理 |
| Tomcat Embed | 10.1.55 | 由 Boot BOM 管理 |
| Jackson Databind | 2.21.4 | 由 Boot BOM 管理 |
| MySQL Connector/J | 9.7.0 | 运行时依赖，由 Boot BOM 管理 |
| HikariCP | 6.3.3 | 由 Boot BOM 管理 |
| Lombok | 1.18.46 | 未显式改版本，由 Boot BOM 管理 |
| MyBatis-Plus | 3.5.7 | 项目显式版本，保持不变 |
| MyBatis | 3.5.16 | MyBatis-Plus 传递依赖 |
| JJWT | 0.12.6 | 项目显式版本，保持不变 |

编译及测试证明当前代码可与上述组合共同加载。未发现需要升级 MyBatis-Plus、JJWT 或修改业务代码的直接兼容问题。

## 3. 代码变更说明

本次没有生产 Java 代码兼容性修改。原因是父版本变更后，94 个生产源文件和 17 个测试源文件均能使用 Java 17 完成干净编译，现有 214 个测试全部通过。

构建期间出现非阻断弃用提示：

- `SecurityConfig` 使用了 Spring Security 已弃用 API；现有行为和测试正常，本次未为消除警告而重构安全配置。
- Controller 测试使用的 `@MockBean` 在 Spring Boot 3.5 中已弃用并计划移除；当前测试仍可执行，建议在后续独立测试维护任务中迁移。

以上均为后续演进风险，不是本次升级阻断项。

## 4. 构建与测试结果

### 4.1 升级前基线（Spring Boot 3.3.5）

| 验证项 | 结果 |
|---|---|
| `mvn -version` | Maven 3.9.16 / Java 17.0.19 |
| `mvn clean test` | BUILD SUCCESS，214 通过，0 失败，0 错误，0 跳过，35.183 秒 |
| `mvn package -DskipTests` | BUILD SUCCESS，生成可执行 JAR |

首次在沙箱内执行 Maven 时，因本机 Maven 仓库被配置在 Maven 安装目录且沙箱账户无写权限而失败；改为经授权使用原本机 Maven 仓库后成功。该问题属于执行环境权限，不是项目或升级问题，且未修改全局 Maven 配置或镜像。

### 4.2 升级后（Spring Boot 3.5.16）

| 验证项 | 结果 |
|---|---|
| `mvn dependency:tree -Dscope=runtime` | BUILD SUCCESS |
| 干净生产代码编译 | 94 个源文件编译成功 |
| 干净测试编译 | 17 个测试源文件编译成功 |
| `mvn clean test` | BUILD SUCCESS，214 通过，0 失败，0 错误，0 跳过，38.208 秒 |
| `mvn clean package -DskipTests` | BUILD SUCCESS |
| `mvn clean package` | BUILD SUCCESS，214 个测试再次全部通过，22.955 秒 |
| 可执行 JAR | `backend/target/crm-backend-0.1.0-SNAPSHOT.jar`，34,494,101 bytes |

升级前后自动化测试数量和结果一致，没有新增失败或跳过项。

## 5. CRM 功能回归矩阵

下表中的 PASS 仅代表相应自动化测试层级已经执行通过，不等同于真实 MySQL + HTTP 端到端验收。

| 模块 | 测试内容 | 结果 | 证据 |
|---|---|---|---|
| 认证 | 登录成功/失败及错误响应 | PASS | `AuthControllerTest`、`AuthServiceImplTest` |
| 认证 | JWT 生成、解析、篡改、过期 | PASS | `JwtServiceTest` |
| 认证 | JWT Filter 恢复身份及拒绝非法请求 | PASS | `JwtAuthenticationFilterTest` |
| 认证 | `/auth/me` 响应与未认证拒绝 | PASS | `AuthControllerTest` |
| 权限 | 用户、角色、权限接口方法级鉴权 | PASS | `UserControllerTest`、`RoleControllerTest`、`PermissionControllerTest` |
| 权限 | 角色创建/编辑/授权范围与超级管理员保护 | PASS | `RoleServiceImplTest` |
| 权限 | 用户创建/编辑/重置密码范围与无副作用拒绝 | PASS | `UserServiceImplTest` |
| 权限 | 权限树、列表与总览 | PASS | `PermissionServiceImplTest`、`PermissionControllerTest` |
| 数据范围 | ALL / DEPT / SELF 解析及优先级 | PASS | `DataScopeResolverTest` |
| 客户 | 列表分页、筛选参数与权限入口 | PASS | `CustomerControllerTest`、`CustomerServiceImplTest` |
| 客户 | 创建客户的角色归属、校验与失败无写入 | PASS | `CustomerCreationServiceTest` |
| MyBatis | Customer XML 中逻辑删除、公海排除及数据范围谓词 | PASS | `CustomerMapperSqlTest` |
| 数据库 | 真实 MySQL 连接、Mapper 执行、事务和分页 | NOT RUN | 无隔离测试库；为避免影响现有开发数据未启动写库验证 |
| HTTP | 升级后真实登录、查询、创建与越权请求 | NOT RUN | 无隔离运行环境，未执行真实 HTTP 端到端回归 |
| 前端 | 浏览器登录及核心页面烟测 | NOT RUN | 本次范围不修改前端，后端未启动真实联调环境 |

## 6. 启动与接口验证限制

没有启动连接当前开发 MySQL 的升级后应用。默认 `local` Profile 会连接开发库，并在启动时执行 `DataInitializer`；在没有隔离测试库的情况下，直接启动不能满足“不得影响现有业务数据”的验收约束。

自动化 MVC 测试已验证 Spring Security Filter Chain、Controller 映射、ProblemDetail 异常路径和方法级授权可以在 Spring Boot 3.5.16 测试上下文中初始化，但这不能替代真实数据源、完整应用上下文和网络层验证。因此最终结论不能标记为完全 PASS。

## 7. 兼容性风险

### P0（阻断）

- 无。

### P1（上线前必须补验）

- 尚未在隔离 MySQL 8 环境启动完整应用，MyBatis-Plus 3.5.7、MySQL Connector/J 9.7.0 与真实 schema 的运行时组合仍需验证。
- 尚未完成升级后真实 HTTP 认证、RBAC、DataScope、客户列表及客户创建的端到端回归。

### P2（后续维护）

- `SecurityConfig` 存在 Spring Security 弃用 API 警告。
- 测试中的 `@MockBean` 在 Spring Boot 3.5 已弃用，未来升级至 Spring Boot 4 前应迁移。
- Spring Boot 3.5.16 是 3.5.x 最后一个 OSS 版本，需要规划后续受支持版本路线，但不应与本次 Spring AI 接入混做一次变更。

## 8. 最终验收结论

**PARTIAL PASS**

Spring Boot 已从 3.3.5 最小升级到 3.5.16；Java 17、MyBatis-Plus 3.5.7 和 JJWT 0.12.6 保持不变。依赖解析、干净编译、214 个自动化测试和可执行 JAR 打包全部成功，没有发现由本次升级引入的阻断问题。

由于没有安全的隔离数据库环境，真实 MySQL 启动和 HTTP 端到端回归未执行。完成这些 P1 验证前，不宣称全部 CRM 验收通过。

## 9. 下一阶段准备情况

从构建基线看，项目已具备评估 Spring AI 1.1.x 接入的基础条件：Spring Boot 3.5.16、Java 17 与 Maven 3.9.16 均已就位，现有自动化测试保持全绿。

建议先在隔离 MySQL 8 环境完成本报告 P1 项，再以独立变更引入 Spring AI BOM 和模型 Starter。接入时不得混入本次遗留弃用清理、业务规则调整或权限放宽。

## 10. 参考资料

- Spring Boot 3.5.16 发布公告：https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/
- Spring Boot 3.5 系统要求：https://docs.spring.io/spring-boot/3.5/system-requirements.html
