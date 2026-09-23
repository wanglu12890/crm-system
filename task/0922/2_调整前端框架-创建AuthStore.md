请根据前端权限控制分析报告，实现第一阶段：建立统一 Auth Store。

目标：
将当前 Header.vue 中局部保存的用户信息迁移到 Pinia，实现全局用户权限状态管理。

严格限制：
本次只处理认证状态管理，不实现菜单、按钮、路由权限控制。

不要修改：
- Sidebar.vue
- Role.vue
- User.vue
- router
- 权限判断逻辑

====================
实现要求
====================

1. 新增：
src/stores/auth.ts

使用 Pinia defineStore。

Store需要管理：

currentUser


类型包含：

id
username
realName
roles:string[]
permissions:string[]


提供：

loadCurrentUser()

调用已有：

GET /api/auth/me


成功后保存用户信息。


提供：

clearUser()


清理当前用户状态。


提供：

hasRole(roleCode:string)


提供：

hasPermission(permissionCode:string)


例如：

hasPermission('role:create')


====================
2. 修改 auth API 类型
====================

检查：

src/api/auth.ts


补充 CurrentUser 类型：

增加：

permissions:string[]


保持已有接口不变。


====================
3. 修改 Header.vue
====================

当前 Header.vue:

- 自己调用 getCurrentUser()
- 自己维护 currentUser ref


修改为：

使用 authStore。


页面挂载时：

调用：

authStore.loadCurrentUser()


显示用户信息时：

读取：

authStore.currentUser


不要重复保存一份 currentUser。


====================
4. 验证
====================

完成后输出：

1. 新增和修改文件。
2. Auth Store 数据流程。
3. Header.vue 修改前后区别。
4. 是否仍存在重复用户状态。
5. 编译测试结果。

不要实现菜单和按钮权限控制。
不要执行 Git commit。