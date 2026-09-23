请检查当前Spring Boot后端项目。

目标：确认用户登录接口是否已经实现。

检查内容：
1. Controller层是否存在认证接口：
   - /auth/login
   - POST请求

2. 检查：
   - Controller文件位置
   - 请求参数格式
   - 返回结果格式

3. 检查Spring Security配置：
   - 是否允许登录接口匿名访问
   - JWT过滤器是否配置
   - 登录成功后返回Token逻辑

4. 如果接口不存在，请不要直接修改。
先输出当前认证模块实现情况和缺失内容。