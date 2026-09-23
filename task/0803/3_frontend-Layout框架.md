当前已经完成 Login.vue。

请开始实现后台管理系统 Layout 框架，不开发业务功能。
要求：
1. 创建 src/layout 目录（如果已创建请忽略该要求）
2. 创建：
src/layout/Layout.vue
src/layout/Header.vue
src/layout/Sidebar.vue

3. Layout.vue作为登录后的后台主框架。
结构：
顶部 Header
左侧 Sidebar
中间 RouterView

4. Header.vue实现：
- 左侧显示Logo占位
- 显示系统名称：雨燕科技CRM
- 右侧显示当前用户：admin
- 退出登录按钮（暂时只打印日志）

5. Sidebar.vue实现菜单：
首页
系统管理
  - 用户管理
  - 角色管理
  - 权限管理
业务管理
  - 客户管理
  - 联系人管理
  - 商机管理
  - 合同管理
数据分析
AI分析

6. 配置 Vue Router 嵌套路由：
/admin 使用 Layout.vue
/admin/dashboard 显示首页
/admin/system/user 显示用户管理页面

7. 暂时不连接后端，使用静态数据。
完成后说明修改了哪些文件。