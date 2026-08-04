# Frontend 前端现状检查报告

> 检查日期：2026-08-03  
> 检查范围：`frontend`（排除 `node_modules` 和 `dist`）  
> 本报告仅描述当前代码现状，不包含功能实现或文件改造。

## 1. Vue 3 项目入口

前端的 HTML 入口是：

```text
frontend/index.html
```

其中通过以下脚本加载 TypeScript 应用入口：

```html
<script type="module" src="/src/main.ts"></script>
```

Vue 应用入口文件是：

```text
frontend/src/main.ts
```

当前入口初始化流程为：

```text
index.html
  -> src/main.ts
     -> 创建 Vue 应用
     -> 注册 Pinia
     -> 注册 Vue Router
     -> 注册 Element Plus
     -> 挂载 App.vue 到 #app
```

根组件为 `frontend/src/App.vue`，当前只渲染 `<router-view />`。

## 2. 当前技术栈

### 2.1 Vue

- `package.json` 声明版本：`^3.5.12`
- 当前本地安装版本：`3.5.40`
- 使用方式：Vue 3 `createApp` API
- 根组件：`src/App.vue`

结论：Vue 3 已正确安装并接入入口文件。

### 2.2 Vite

- `package.json` 声明版本：`^5.4.10`
- 当前本地安装版本：`5.4.21`
- Vue 插件：`@vitejs/plugin-vue`，当前安装版本 `5.2.4`
- 配置文件：`frontend/vite.config.ts`
- 开发端口：`5173`
- 路径别名：`@` 指向 `frontend/src`
- 开发代理：`/api` 转发到 `VITE_API_PROXY_TARGET`
- 未配置环境变量时，代理目标默认为 `http://localhost:8080`

当前核心配置：

```text
@/*        -> src/*
/api       -> http://localhost:8080
dev port   -> 5173
```

环境文件：

- `.env.development`：配置 `/api` 和本地后端代理地址
- `.env.production`：配置生产 API 基础路径 `/api`

结论：Vite 开发服务器、Vue 插件、别名和后端代理均已配置。

### 2.3 TypeScript

- TypeScript 声明版本：`~5.6.3`
- 当前本地安装版本：`5.6.3`
- Vue 类型检查工具：`vue-tsc`，当前安装版本 `2.2.12`
- 应用配置：`tsconfig.app.json`
- Node/Vite 配置：`tsconfig.node.json`
- 环境声明：`src/env.d.ts`
- `strict: true`
- `noEmit: true`
- 编译目标：`ES2022`
- 模块解析：`Bundler`

构建前会依次执行 Vue 和 Node 配置的 TypeScript 类型检查，然后由 Vite 生成产物。

结论：TypeScript 已完整启用，并开启严格类型检查。

### 2.4 UI 组件库

- UI 库：Element Plus
- `package.json` 声明版本：`^2.8.8`
- 当前本地安装版本：`2.14.3`
- 图标库：`@element-plus/icons-vue`，当前安装版本 `2.3.2`

`src/main.ts` 已全局注册 Element Plus，并引入默认 CSS：

```ts
createApp(App).use(ElementPlus)
```

结论：Element Plus 已接入，但当前尚无页面或组件使用它。

### 2.5 Vue Router

- `package.json` 声明版本：`^4.4.5`
- 当前本地安装版本：`4.6.4`
- 配置文件：`src/router/index.ts`
- 路由模式：HTML5 History，即 `createWebHistory`
- 已在 `main.ts` 注册

当前路由表为：

```ts
const routes: RouteRecordRaw[] = []
```

当前未配置：

- 登录路由
- CRM 业务页面路由
- 404/403 路由
- 路由懒加载
- 登录校验守卫
- 动态权限路由
- 页面标题等路由元数据

结论：Vue Router 基础实例已创建，但尚未配置任何可访问页面。

### 2.6 Pinia

- `package.json` 声明版本：`^2.2.6`
- 当前本地安装版本：`2.3.1`
- 已在 `main.ts` 中通过 `createPinia()` 注册
- `src/stores` 目录存在

当前不存在任何 `defineStore` 实现，因此尚无：

- 用户认证状态
- Token 状态
- 当前用户信息
- 角色与权限状态
- 字典或系统配置状态

结论：Pinia 已完成框架注册，但状态管理业务代码尚未开始。

### 2.7 Axios

- `package.json` 声明版本：`^1.7.7`
- 当前本地安装版本：`1.19.0`
- 请求封装：`src/utils/request.ts`
- API 基础地址：`VITE_API_BASE_URL`，默认 `/api`
- 请求超时：15 秒
- 默认 Content-Type：`application/json;charset=UTF-8`
- 已定义通用 `ApiResponse<T>` 类型
- 已创建请求和响应拦截器框架

当前请求拦截器尚未附加 Token，响应拦截器尚未实现：

- Access Token 自动携带
- Refresh Token 刷新
- 并发刷新控制
- 401 登录跳转
- 403 权限提示
- Element Plus 统一错误提示
- 业务响应解包

结论：Axios 实例已有基础封装，但认证和统一异常处理仍是占位状态。

## 3. 当前 `src` 目录结构

```text
frontend/src/
├── api/
│   └── .gitkeep
├── assets/
│   └── .gitkeep
├── components/
│   └── .gitkeep
├── layouts/
│   └── .gitkeep
├── router/
│   └── index.ts
├── stores/
│   └── .gitkeep
├── types/
│   └── .gitkeep
├── utils/
│   ├── .gitkeep
│   └── request.ts
├── views/
│   └── .gitkeep
├── App.vue
├── env.d.ts
└── main.ts
```

`.gitkeep` 仅用于让 Git 保留空目录，不属于功能代码。

## 4. 指定项目能力检查

| 检查项 | 是否存在 | 当前状态 |
|---|---|---|
| `views` 目录 | 是 | 目录为空，只有 `.gitkeep`，没有页面组件 |
| `components` 目录 | 是 | 目录为空，只有 `.gitkeep`，没有公共组件 |
| `router` 目录 | 是 | 存在 `index.ts`，但路由数组为空 |
| API 请求封装 | 部分存在 | `utils/request.ts` 已创建 Axios 实例；`api` 目录为空，无模块 API |
| 登录相关代码 | 否 | 无登录页面、登录接口、认证 Store、Token 存储和路由守卫 |

## 5. 登录能力专项检查

在 `frontend/src` 中未发现可执行的登录或认证实现。仅在 `request.ts` 的注释中预留了 Token 和 401/403 处理方向。

目前缺少：

1. `views/login` 登录页面及表单校验。
2. `api/auth.ts` 登录、刷新、退出和当前用户接口。
3. `stores/auth.ts` 用户、Token、角色和权限状态。
4. Token 的安全存储与读取封装。
5. Axios 请求头 `Authorization: Bearer <token>` 注入。
6. Access Token 过期后的单次刷新机制。
7. 登录路由和全局前置守卫。
8. 未登录跳转、登录后重定向和退出清理。
9. 后端权限到前端路由/按钮权限的映射。

结论：当前前端是可构建的企业项目基础骨架，但尚未进入登录和 CRM 业务页面开发阶段。
