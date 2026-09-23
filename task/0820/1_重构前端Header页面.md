请在当前 CRM Vue3 + Vite + TypeScript 前端项目中，重构 `Header.vue`，并接入后端已经完成的：

```text
GET /api/auth/me
```

目标：实现第一版后台 Header 用户区域。

当前项目条件：

- 前端：Vue3 + Vite + TypeScript + Element Plus
- 后端 context path：`/api`
- 前端统一 Axios 实例位于 `src/utils/request.ts`
- `request.ts` 已实现：

- `baseURL = /api`
- 如果 localStorage 中存在 JWT，则自动添加：
  `Authorization: Bearer <JWT>`
- 登录成功后 Token 已保存到 localStorage
- 后端 `/auth/me` 已完成，并且是受保护接口
- `/auth/me` 会直接根据当前认证用户返回类似：

```json
{
  "id": 1,
  "username": "admin",
  "realName": "系统管理员",
  "roles": []
}
```

当前阶段角色权限尚未正式实现，所以 `roles` 可以存在但暂时不用于 Header 展示。

请按以下要求实现。

一、Header 页面目标布局

第一版 Header 结构：

```text
[系统Logo] [系统名称]                    [默认头像] 系统管理员 ▼
```

用户点击右侧头像或用户名后，打开下拉菜单：

```text
admin
----------------
个人信息
修改密码
退出登录
```

其中：

- `系统管理员` 使用 `/auth/me` 返回的 `realName`
- `admin` 使用 `/auth/me` 返回的 `username`
- 如果 `realName` 为空，则使用 `username` 作为主显示名称
- 当前头像先使用 Element Plus 默认用户图标或简单圆形默认头像，不开发真实头像上传功能

二、接入 `/auth/me`

如果 `src/api/auth.ts` 已存在，请在其中新增当前用户接口，不要新建重复 API 文件。

新增类似：

```ts
export interface CurrentUser {
  id: number
  username: string
  realName: string
  roles: string[]
}

export const getCurrentUser = () => {
  return request.get<CurrentUser>('/auth/me')
}
```

请根据当前项目 Axios 封装的实际返回类型进行适配，不要机械照抄。

不要直接使用原生 `axios`，必须继续使用项目现有统一 `request` 实例。

实际请求应为：

```text
GET /api/auth/me
```

`Authorization: Bearer <JWT>` 由 `request.ts` 自动添加。

三、Header.vue 状态管理

在 `Header.vue` 中增加当前用户状态，例如：

```ts
const currentUser = ref<CurrentUser | null>(null)
```

组件加载后调用：

```text
getCurrentUser()
```

成功后：

```text
currentUser.value = 当前用户数据
```

然后页面显示：

```text
currentUser.realName
currentUser.username
```

不要把用户名和姓名继续写死。

四、Header 用户区域展示

右侧用户区域建议使用 Element Plus：

```text
el-dropdown
el-avatar
el-dropdown-menu
el-dropdown-item
```

大致交互：

```text
[头像] 系统管理员 ▼
        ↓
      admin
      --------
      个人信息
      修改密码
      退出登录
```

要求：

1. 用户区域样式保持简洁，符合 B 端 CRM 后台风格
2. 头像、姓名、下拉箭头处于同一行
3. 鼠标悬停有合理反馈
4. 不要做复杂动画
5. 不要引入新的 UI 依赖

五、菜单功能

当前阶段：

`个人信息`

- 保留菜单项
- 点击后可以使用 `ElMessage.info('个人信息功能暂未实现')`

`修改密码`

- 保留菜单项
- 点击后可以使用 `ElMessage.info('修改密码功能暂未实现')`

`退出登录`

- 必须真正实现

退出登录时应：

```text
清除本地 JWT
→ 清除本地过期时间信息
→ 跳转到 /login
```

项目已经有 `src/utils/auth.ts`，其中已有 Token 管理方法。

优先复用现有方法。

如果已有类似：

```ts
removeAuthToken()
clearAuthToken()
```

直接复用。

不要在 Header.vue 中重复写：

```ts
localStorage.removeItem(...)
```

除非当前工具文件确实没有对应方法。

六、系统 Logo 和系统名称

Header 左侧显示：

```text
[系统Logo] [系统名称]
```

请优先检查项目当前已有：

- Logo 图片
- SVG
- 系统名称
- Layout 相关配置

如果项目已经存在 Logo，请复用，不要创建新的 Logo 文件。

系统名称使用当前 CRM 项目已有名称。

如果当前 Header 已经有系统 Logo 和名称，则保留原有设计，只优化布局，不要重复实现。

七、401 情况

`/auth/me` 如果因为 Token 过期、非法等情况返回 401，项目的 `request.ts` 如果已经有统一 401 处理逻辑，则继续复用现有逻辑。

不要在 `Header.vue` 再重复实现一整套 JWT 失效处理。

如果当前 `request.ts` 尚未统一处理 401，则：

- 只做最小必要处理
- 清除 Token
- 跳转 `/login`
- 不要大规模重构请求模块

八、不要做的事情

本任务不要：

- 修改后端代码
- 实现角色权限功能
- 实现动态菜单
- 实现真实头像上传
- 实现个人中心页面
- 实现修改密码接口
- 引入 Pinia，除非当前项目本来就在使用
- 大规模重构 Layout
- 改动无关页面
- 重写 request.ts

九、重点检查现有代码兼容性

修改前先检查：

```text
Header.vue
src/api/auth.ts
src/utils/auth.ts
src/utils/request.ts
router/index.ts
```

确认已有函数和类型后再修改，优先复用现有代码。

不要创建重复的：

```text
getAccessToken
removeToken
axios instance
CurrentUser 类型
```

如果项目中已经存在对应能力，应直接复用。

十、完成后验证

至少完成以下验证：

1. 用户正常登录后进入后台
2. Header 自动调用 `/api/auth/me`
3. 请求自动携带 Bearer JWT
4. Header 正确显示：

- realName
- username
5. 刷新后台页面后仍然能重新获取当前用户并显示
6. 点击“个人信息”提示暂未实现
7. 点击“修改密码”提示暂未实现
8. 点击“退出登录”：

- Token 被清除
- 跳转到 `/login`
9. Token 失效时不能继续保持假登录状态
10. TypeScript 编译通过

完成后输出：

1. 修改文件列表
2. Header.vue 主要修改内容
3. `/auth/me` 前端调用流程
4. 退出登录流程
5. 是否复用了现有 auth.ts / request.ts / auth 工具
6. 测试结果
7. 如果发现当前代码存在问题，只报告问题，不要扩大修改范围