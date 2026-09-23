请检查当前Spring Boot配置。

目标：将数据库和JWT配置调整为适合本地开发的方式。

要求：
1. 保留application.yml作为公共配置
2. 新增application-local.yml用于本地开发
3. 将数据库连接：
DB_URL
DB_USERNAME
DB_PASSWORD

迁移到local配置

4. JWT_SECRET也放入local配置
5. 确保启动Spring Boot时自动加载local环境
6. 不修改代码逻辑，只调整配置文件。

完成后说明启动方式。