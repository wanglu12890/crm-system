请实现JWT基础设施第一阶段。

背景：

当前已经完成：

1. sys_user数据库
2. SysUser Entity
3. SysUserMapper
4. CustomUserDetailsService
5. SecurityUser
6. PasswordEncoder


目标：

实现JWT基础支持。


要求：

新增：

1. JwtProperties.java

位置：

security目录


功能：

从application-local.yml读取：

jwt.secret

jwt.expire


不要硬编码密钥。


2. JwtService.java

功能：

① 根据用户信息生成accessToken

Token中保存：

userId
username


② 解析token获取username


③ 校验token是否有效


要求：

- 使用当前项目已有JJWT依赖
- 使用Spring Bean方式管理
- 不实现登录接口
- 不实现JWT过滤器


完成后：

说明新增文件和实现逻辑。