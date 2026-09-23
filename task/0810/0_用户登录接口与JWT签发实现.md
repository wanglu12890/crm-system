现在基于当前已有认证基础，实现用户登录接口。

当前已经完成：

- SysUser / SysUserMapper
- SecurityUser
- CustomUserDetailsService
- BCrypt PasswordEncoder
- JwtProperties
- JwtService
- admin初始化账号已存在并经过验证

当前暂时不要实现角色权限控制。


目标接口：

POST /auth/login

由于当前项目配置了 context-path=/api，
最终实际接口应为：

POST /api/auth/login


一、创建 LoginDTO

建议位置：

dto/auth/LoginDTO.java

字段：

username
password

要求：

- 使用 Jakarta Validation
- username不能为空
- password不能为空


二、创建 TokenVO

建议位置：

vo/auth/TokenVO.java

字段：

accessToken
expiresIn

如果当前 JwtService 已经实现 refreshToken，
可以同时保留：

refreshToken

否则本阶段不要为了返回 refreshToken 额外扩展复杂逻辑。


三、实现 AuthService

要求使用 Spring Security 标准认证流程：

AuthenticationManager
    ↓
CustomUserDetailsService
    ↓
PasswordEncoder

不要在 AuthService 中自己手写 BCrypt 密码比对。


登录流程：

1. 接收 username/password

2. 创建 UsernamePasswordAuthenticationToken

3. 调用 AuthenticationManager.authenticate()

4. 认证成功后从 Authentication 中获取 SecurityUser

5. 调用现有 JwtService 生成 Access Token

6. 返回 TokenVO

7. 登录成功后，如果 sys_user 已存在 last_login_at 字段，
   则更新最后登录时间；
   如果没有该字段，不修改数据库结构。


四、创建 AuthController

建议位置：

controller/AuthController.java

配置：

@RequestMapping("/auth")

实现：

POST /login

使用：

@Valid @RequestBody LoginDTO


返回项目现有统一响应结构。

如果当前项目没有统一 Result 类：

请先检查现有代码，不要自行创建重复的统一响应体系；
可以使用当前项目已有响应方式。


五、完善 Spring Security 基础配置

检查当前是否已有 SecurityFilterChain。

如果没有，则创建 SecurityConfig。

要求：

1. 使用无状态认证：

SessionCreationPolicy.STATELESS

2. 关闭默认 formLogin

3. 关闭 httpBasic

4. 根据当前前后端分离架构合理处理 CSRF

5. 匿名放行：

POST /auth/login

注意：
SecurityFilterChain 中匹配的是 context-path 内部路径，
不要错误写成 /api/auth/login，需结合当前 Spring Boot 配置确认。

6. 其他接口暂时要求 authenticated()

7. 配置 AuthenticationManager

8. 使用现有 PasswordEncoder

本阶段先不要创建 JWT Authentication Filter，
因为当前目标只是先验证登录接口能够成功生成JWT。


六、异常处理

至少正确处理：

- 用户不存在
- 密码错误
- 用户被停用
- 用户已逻辑删除

认证失败时不要返回500。

使用合理的401响应。


七、测试

请完成自动化测试或实际启动验证。

至少验证：

1. admin + 正确密码登录成功

2. 错误密码登录失败

3. 不存在的用户登录失败

4. /auth/login 可以匿名访问

5. 登录成功可以生成有效 JWT


完成后输出：

A. 新增/修改了哪些文件

B. 登录完整调用链

C. 实际测试结果

D. 当前还未实现的认证功能

不要实现：

- 角色权限
- @PreAuthorize
- 动态菜单
- Token黑名单
- Redis
- 退出登录