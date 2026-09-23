请继续实现 CRM 系统认证模块的下一阶段：JWT 认证过滤器。

当前已完成：

1. 管理员初始化
2. 认证查询基础

   * SecurityUser
   * CustomUserDetailsService
   * SysUserMapper.selectByUsername()
3. 登录接口与 JWT 签发

   * POST /auth/login
   * AuthenticationManager 标准认证流程
   * BCrypt 密码校验
   * JwtService 生成 access token
   * SecurityConfig 已设置 STATELESS
   * /auth/login 匿名放行
   * 其他接口要求 authenticated()

当前尚未实现 JWT Filter，因此虽然登录能获得 JWT，但后续请求携带 JWT 时，Spring Security 还不能恢复登录身份。

本阶段目标：

实现完整流程：

前端请求
Authorization: Bearer <JWT>
↓
JWT Authentication Filter
↓
提取 Bearer Token
↓
JwtService 验证 Token
↓
解析用户身份
↓
加载 SecurityUser
↓
构造 Authentication
↓
写入 SecurityContextHolder
↓
继续执行过滤器链
↓
允许访问 authenticated() 接口

具体要求：

一、新增 JWT 认证过滤器

建议类名：

JwtAuthenticationFilter.java

位置：

security 包下。

继承：

OncePerRequestFilter

职责：

1. 从请求头读取：

Authorization: Bearer <token>

2. 如果没有 Authorization 请求头，或者不是 Bearer 格式：

   * 不报错
   * 不设置认证状态
   * 直接 filterChain.doFilter()
   * 后续由 Spring Security 判断该接口是否需要认证

3. 如果存在 Bearer Token：

   * 使用现有 JwtService 验证 JWT
   * 解析 JWT 中的用户标识
   * 加载当前用户
   * 构造已认证的 Authentication
   * 写入 SecurityContextHolder.getContext()

4. 如果当前 SecurityContext 已经存在 Authentication，不重复认证。

5. Token 非法、过期、签名错误时：

   * 不允许异常直接导致 500
   * 清理 SecurityContext
   * 最终返回规范的 401 JSON
   * 尽量复用当前认证异常处理机制
   * 不要在 Filter 中写大量重复 JSON 响应逻辑，如果确实需要 Filter 层专门处理，请保持职责清晰

二、优先复用现有 JwtService

先检查现有 JwtService.java。

不要重新实现 JWT 工具类。

确认现有 JwtService 是否已经支持：

* Token 签名验证
* Token 过期验证
* 从 Token 提取 username 或 userId
* claims 解析

如果现有方法不足，可以在 JwtService 中补充最小必要方法，但不要重复设计另一套 JWT 服务。

三、用户加载方式

优先使用现有：

CustomUserDetailsService

通过 JWT 中解析出的 username 加载：

SecurityUser

不要直接在 Filter 中重新写 SysUserMapper 查询逻辑。

期望：

JWT
↓
username
↓
CustomUserDetailsService.loadUserByUsername()
↓
SecurityUser

这样登录认证与 JWT 认证使用同一套用户状态检查逻辑。

如果现有 JWT 中存储的是 userId 而不是 username，请先分析现有 JwtService 的 claims 设计，再采用与当前代码最一致的实现，不要擅自大改 Token 结构。

四、构造 Spring Security Authentication

认证成功后构造：

UsernamePasswordAuthenticationToken

注意这里代表的是“已经认证完成”的 Authentication。

principal 应为：

SecurityUser

credentials 可为 null。

authorities 使用：

securityUser.getAuthorities()

然后写入：

SecurityContextHolder.getContext().setAuthentication(authentication)

如有必要设置请求详情：

WebAuthenticationDetailsSource

五、修改 SecurityConfig

将 JwtAuthenticationFilter 注册到 Spring Security Filter Chain。

位置应在：

UsernamePasswordAuthenticationFilter

之前。

类似逻辑：

addFilterBefore(
jwtAuthenticationFilter,
UsernamePasswordAuthenticationFilter.class
)

保留现有配置：

* STATELESS
* CSRF disabled
* formLogin disabled
* httpBasic disabled
* logout disabled
* /auth/login permitAll
* 其他接口 authenticated()

不要破坏现有登录功能。

六、认证失败处理

请区分：

A. 没有 Token

访问受保护接口：

最终由 RestAuthenticationEntryPoint 返回 401：

{
"title": "认证失败",
"status": 401,
"detail": "当前请求未通过身份认证",
"code": "UNAUTHORIZED"
}

B. Token 非法或过期

应返回 401，而不是 403 或 500。

可以根据当前项目结构新增合适的错误 code，例如：

TOKEN_INVALID

TOKEN_EXPIRED

但不要过度设计。

如果为了保持代码简单，统一返回 UNAUTHORIZED 也可以。

七、测试

请补充 JWT Filter 相关测试。

至少覆盖：

1. 不携带 JWT 访问受保护接口
   → 401

2. 携带有效 JWT
   → Spring Security 能识别用户
   → 请求通过认证层

3. Authorization 不是 Bearer 格式
   → 视为未认证
   → 访问受保护接口返回 401

4. Token 无效
   → 401
   → 不返回 500

5. Token 已过期
   → 401

6. /auth/login 不携带 Token
   → 仍可正常匿名访问

7. 登录成功后生成 JWT，再使用该 JWT 访问一个受保护测试接口
   → 能成功完成完整认证闭环

如果当前项目没有适合测试的业务接口，可以在测试范围内使用测试 Controller，不要为了测试增加无关正式业务接口。

八、验证

完成后执行现有 Maven 测试。

重点确认：

* JwtServiceTest 继续通过
* CustomUserDetailsServiceTest 继续通过
* AuthServiceImplTest 继续通过
* AuthControllerTest 继续通过
* 新增 JWT Filter 测试通过
* 项目可以正常编译

不要实现以下内容：

* Refresh Token
* 角色权限控制
* @PreAuthorize
* RBAC
* 前端 Token 存储
* 前后端联调
* 登出黑名单
* Redis
* Token 刷新机制

这些留到后续阶段。

完成后请输出报告：

1. 新增文件
2. 修改文件
3. JWT Filter 完整调用链
4. SecurityConfig 修改点
5. Token 校验失败时的处理方式
6. SecurityContext 是如何恢复的
7. 测试结果
8. 当前认证模块已经完成到什么程度
9. 下一阶段建议

特别要求：

* 优先复用现有代码
* 不大规模重构
* 不修改与认证无关的业务代码
* 不直接执行 git commit
* 不执行 git push
* 保持当前项目已有代码风格