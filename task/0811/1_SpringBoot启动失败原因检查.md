请只读检查当前 Spring Boot 启动失败原因，不要先修改代码。

启动错误核心为：

io.jsonwebtoken.security.WeakKeyException:
The specified key byte array is 104 bits...
HMAC-SHA keys MUST have a size >= 256 bits.

请重点检查：

1. JwtService.java 中 JWT secret 是如何转换为 SecretKey 的；
2. JwtProperties.java 的配置绑定；
3. application.yml 和 application-local.yml 中当前 JWT secret 配置；
4. 当前 secret 为什么最终只有 104 bits；
5. 当前 JwtService 期望的是普通字符串还是 Base64 字符串；
6. 给出适合当前实现的最小修复方案。

要求：

* 不修改业务逻辑；
* 不重构 JwtService；
* 不修改 JWT Filter；
* 不执行 git commit；
* 不执行 git push；
* 优先只修改本地开发环境的 JWT secret 配置；
* 不把真实 JWT secret 提交到 Git。

最后告诉我：

1. 根因；
2. 具体应该修改哪个配置文件；
3. secret 应采用什么格式；
4. 给出一个安全的生成方式；
5. 修改后应该用什么命令重新启动验证。