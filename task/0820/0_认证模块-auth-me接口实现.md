请在当前 CRM Spring Boot 后端项目中实现 `GET /api/auth/me` 接口。

项目现状：

- 技术栈：Spring Boot 3.x + Java 17 + Spring Security + JWT + MyBatis-Plus
- 后端 context path 为 `/api`
- JWT 登录接口 `POST /api/auth/login` 已完成
- `JwtAuthenticationFilter` 已完成，并已注册到 `SecurityFilterChain`
- JWT 过滤器会：

- 从 `Authorization: Bearer <JWT>` 读取 Token
- 验证 JWT
- 解析 username
- 调用 `CustomUserDetailsService.loadUserByUsername()`
- 得到 `SecurityUser`
- 构造已认证 `Authentication`
- 写入 `SecurityContextHolder`
- `/auth/login` 为 `permitAll`
- 其余接口默认要求 `authenticated`
- 当前 `SecurityUser` 已包含：

- `userId`
- `username`
- `password`
- `realName`
- `status`
- `deleted`
- `authorities`
- 当前 `CustomUserDetailsService` 中的 `authorities` 暂时为空：
  `List<SimpleGrantedAuthority> authorities = List.of();`
- 当前阶段尚未正式实现角色和权限查询，因此 `/auth/me` 中的 `roles` 可以先返回空列表，不要为了本任务额外开发角色权限功能。

实现目标：

新增：

`GET /api/auth/me`

该接口用于返回当前已经通过 JWT 认证的登录用户信息。

接口本身不要重复解析或验证 JWT，因为这些工作已经由 `JwtAuthenticationFilter` 完成。

实现要求：

1. 新增 `CurrentUserVO`

建议字段：

```java
Long id;
String username;
String realName;
List<String> roles;
```

其中：

- `id` 来自 `SecurityUser.userId`
- `username` 来自 `SecurityUser.username`
- `realName` 来自 `SecurityUser.realName`
- `roles` 当前阶段可以返回空列表

注意：

不要把以下内部安全字段返回给前端：

```text
password
status
deleted
authorities 原始对象
```

2. 在 `AuthController` 中新增：

```java
@GetMapping("/me")
```

优先使用 Spring Security 提供的：

```java
@AuthenticationPrincipal SecurityUser securityUser
```

直接获取当前认证用户。

不要使用：

```text
HttpServletRequest 手动读取 Authorization Header
JwtService 再次验证 Token
再次根据 username 查询数据库
SecurityContextHolder 手动解析 JWT
```

因为 JWT 验证和用户加载已经在过滤器阶段完成。

接口逻辑应保持简单：

```text
SecurityContext 中已有 Authentication
        ↓
@AuthenticationPrincipal 获取 SecurityUser
        ↓
转换为 CurrentUserVO
        ↓
返回 JSON
```

3. `/auth/me` 必须是受保护接口。

不要修改成：

```java
/auth/me -> permitAll
```

保持当前 SecurityConfig 的原则：

```text
POST /auth/login -> permitAll
其他接口 -> authenticated
```

4. 如果项目当前 VO 使用普通 class，请保持现有代码风格。

如果已有 Lombok，可以沿用 Lombok。

不要仅为了该接口引入新的依赖或大规模重构。

5. 补充必要测试。

至少验证：

场景一：

```text
GET /api/auth/me
不携带 JWT
```

预期：

```text
401 Unauthorized
```

场景二：

模拟已经认证的 `SecurityUser` 访问：

```text
GET /api/auth/me
```

预期返回类似：

```json
{
  "id": 1,
  "username": "admin",
  "realName": "超级管理员",
  "roles": []
}
```

确保：

- 不返回 password
- 不返回 passwordHash
- 不返回 status
- 不返回 deleted

6. 不修改前端代码。

7. 不实现角色权限查询。

8. 不实现 refresh token。

9. 不进行与 `/auth/me` 无关的重构。

10. 完成后进行编译和相关测试，确认没有破坏现有：

```text
POST /api/auth/login
GET /api/users
JWT Authentication
```

完成后输出一份简洁报告，包括：

1. 新增文件
2. 修改文件
3. `/auth/me` 的实际调用流程
4. `CurrentUserVO` 返回字段说明
5. 为什么 `/auth/me` 不需要再次验证 JWT
6. 测试结果
7. 如果发现当前代码结构存在问题，只报告问题，不要擅自扩大修改范围