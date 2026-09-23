请对当前 CRM 项目进行一次只读的 Spring Security 认证与 RBAC 授权现状审查。

【任务目标】
确认现有用户—角色—权限数据是否已经真正参与后端接口授权，并为下一阶段最小范围的权限控制开发提供依据。

本次仅审查，不实现功能。

【严格约束】
1. 不修改、创建或删除任何项目文件。
2. 不修改数据库数据、表结构或角色权限配置。
3. 不执行会产生业务数据变更的接口请求。
4. 不安装依赖、不执行 Git 提交、不进行无关重构。
5. 优先阅读现有代码，不要根据类名或开发记录推测实现。
6. 所有结论必须给出对应文件路径、方法名和关键代码位置。
7. 无法确认的内容明确标记为“待验证”，不要直接认定已实现。

【一、检查 Spring Security 配置】

定位 SecurityConfig 及相关安全配置。

检查：
- 是否启用 @EnableMethodSecurity？
- 当前 SecurityFilterChain 如何配置？
- 哪些接口 permitAll？
- 其他接口是否只要求 authenticated？
- 是否存在 requestMatchers 级别的权限控制？
- 是否配置 AuthenticationEntryPoint 和 AccessDeniedHandler？
- 当前未认证、无权限分别如何处理？
- 是否存在 SUPER_ADMIN 特殊授权规则？

【二、检查用户权限加载链路】

定位：
SecurityUser
CustomUserDetailsService
相关 UserMapper、RoleMapper、PermissionMapper 及 Service。

检查：
- 用户登录时如何查询角色？
- 是否完整查询多个有效角色？
- 如何查询角色对应的权限？
- 是否对权限编码去重？
- 是否过滤禁用用户、角色和权限？
- SecurityUser.getAuthorities() 实际返回什么？
- 是否将 permission_code 转换为 SimpleGrantedAuthority？
- 是否存在 ROLE_ 前缀或其他权限命名规则？
- 是否存在只读取第一个角色的问题？

请列出真实调用链，不要只描述理论流程。

【三、检查 JWT 请求认证链路】

定位：
JwtAuthenticationFilter
JwtService
AuthServiceImpl
相关认证代码。

检查：
- JWT 中保存哪些用户信息？
- 每次请求是否重新查询数据库中的用户和权限？
- 还是直接使用 JWT 中的角色或权限快照？
- Authentication 如何创建？
- Authentication 中的 authorities 从哪里获得？
- 是否正确写入 SecurityContext？
- JWT 无效、过期时如何处理？
- 管理员修改角色权限后，已登录用户的后续请求何时使用新权限？

如果无法通过静态代码确认实际生效时机，请明确说明需要怎样验证。

【四、检查已实现 Controller 的授权情况】

检查：
AuthController
UserController
RoleController
PermissionController

列出实际存在的接口，包括：
HTTP 方法
完整请求路径
Controller 方法
当前认证要求
当前权限要求
是否存在 @PreAuthorize
是否存在其他授权方式

重点核对：
GET /api/users
POST /api/users
GET /api/roles
POST /api/roles
GET /api/roles/{id}/permission-ids
PUT /api/roles/{id}/permissions
GET /api/permissions

以上路径仅为参考，以项目真实代码为准。

不要为了匹配参考路径修改现有接口。

【五、检查权限编码与数据库定义】

只读检查现有数据库初始化脚本、实体及权限相关代码。

确认：
- sys_permission 的实际字段及权限编码。
- MENU、BUTTON、API 的实际使用方式。
- user:list、user:create、role:list、role:create、role:assign_permission 是否存在于初始化数据或其他可确认的数据来源。
- 权限树查询是否有独立权限编码。
- SUPER_ADMIN 是否通过数据库配置获得全部权限。
- 是否存在硬编码的角色授权或权限绕过逻辑。

如果初始化脚本无法代表当前真实数据库状态，请明确区分“脚本定义”和“实际数据库状态待验证”。

【六、检查前端权限数据】

定位：
/auth/me 对应的 API 调用
用户信息存储模块
认证工具
路由守卫
用户管理页面
角色管理页面

检查：
- 登录后何时调用 /auth/me？
- roles 和 permissions 保存在哪里？
- 页面刷新后是否重新获取？
- 是否已经存在 hasPermission 或类似工具？
- 是否存在基于角色名称硬编码的菜单或按钮控制？
- 后端返回 401、403 时，Axios 如何处理？

本次只审查前端，不修改页面。

【七、输出审查报告】

请在 Codex 回复中直接输出报告，不创建文件。

报告结构：

1. 总体结论
   用“已实现 / 部分实现 / 未实现 / 待验证”说明当前授权状态。

2. 实际认证与授权调用链
   从登录到 JWT 请求，再到 Controller。

3. 关键文件职责表
   文件路径、核心方法、当前职责、发现的问题。

4. 已实现接口授权现状表
   实际路径、HTTP 方法、认证要求、权限要求、证据位置。

5. 多角色权限聚合与权限变更生效机制
   区分代码可确认的事实与待运行验证的行为。

6. 401 与 403 当前处理情况。

7. 前端权限状态管理现状。

8. 最小修改建议
   仅列出下一阶段建议修改的文件及原因，不编写实现代码。

9. 待确认事项
   特别列出需要真实数据库、Postman 或浏览器验证的内容。

【完成标准】
我需要根据这份报告，能够准确判断：
现有 RBAC 数据在哪一步进入 Spring Security；
目前哪些接口真正执行了权限检查；
下一步应该修改哪些文件；
以及哪些现有实现必须保留。