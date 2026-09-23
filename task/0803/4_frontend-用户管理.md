现在实现 CRM 系统「系统管理 → 用户管理」页面。

当前已经完成：src/views/system/User.vue
请按照企业级 Vue3 项目组件化方式开发，不要把所有代码写在 User.vue 中。

目标目录结构：
src
├── views
│   └── system
│       └── User.vue
│
└── components
    └── system
        └── user
            ├── UserSearch.vue
            ├── UserTable.vue
            └── UserDialog.vue


开发要求：
一、User.vue 页面
职责：作为用户管理页面入口，只负责：
1. 页面布局组合
2. mock 用户数据管理
3. 控制新增、编辑弹窗显示
4. 管理分页状态

页面结构：
用户管理标题
↓
UserSearch.vue
↓
新建用户按钮
↓
UserTable.vue
↓
分页组件

要求：使用 Vue3 Composition API
使用：<script setup lang="ts">
定义用户类型：
interface User {
 id:number
 username:string
 name:string
 role:string
 phone:string
 status:string
 createTime:string
}

初始化 mock 数据：
例如：
[
 {
   id:1,
   username:"admin",
   name:"系统管理员",
   role:"管理员",
   phone:"13800000000",
   status:"正常",
   createTime:"2026-08-03"
 },
 {
   id:2,
   username:"zhangsan",
   name:"张三",
   role:"销售人员",
   phone:"13900000000",
   status:"正常",
   createTime:"2026-08-03"
 }
]

暂时不要调用 API。
--------------------------------------------------
二、UserSearch.vue
位置：src/components/system/user/UserSearch.vue
功能：用户查询区域。
包含：
用户名输入框
角色选择框
状态选择框
查询按钮
重置按钮

使用 Element Plus：
el-form
el-input
el-select
el-button

当前查询只过滤前端 mock 数据。
通过 props / emits 与 User.vue 通信。
--------------------------------------------------
三、UserTable.vue
位置：src/components/system/user/UserTable.vue
功能：展示用户列表。
使用 Element Plus：
el-table
el-table-column
字段：
编号
用户名
真实姓名
角色
手机号
状态
创建时间
操作

操作列：
编辑按钮
删除按钮

点击编辑：emit 当前用户数据给 User.vue
点击删除：emit 删除事件

不要直接修改数据。
--------------------------------------------------
四、UserDialog.vue

位置：src/components/system/user/UserDialog.vue
功能：新增用户和编辑用户共用弹窗。

使用：
el-dialog
el-form
el-input
el-select

字段：
用户名
真实姓名
密码
角色
手机号
状态

支持两种模式：
新增：
标题：新增用户

编辑：
标题：编辑用户

点击保存：emit 用户数据给 User.vue
暂时修改 mock 数据。
--------------------------------------------------
五、分页
使用 Element Plus：el-pagination
实现：
当前页
每页数量
总数量

暂时实现前端分页。
--------------------------------------------------
六、代码规范要求
1. 所有组件使用：<script setup lang="ts">
2. 使用 TypeScript 类型定义。
3. 父子组件通信使用：props defineEmits
4. 不允许：
- Vue2 Options API
- 全部代码集中在 User.vue
- 直接写死大量 HTML
5. 保持后续接 Spring Boot API 的扩展性。

完成后请输出：
1. 新增和修改的文件列表。
2. 每个组件职责说明。
3. 当前访问路径。
4. 后续连接 Spring Boot 用户接口时需要修改的位置。