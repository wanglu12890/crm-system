// 导入 Vue Router 的核心函数和类型定义，用于创建路由实例和定义路由记录。
// createRouter：用于创建路由实例。
// createWebHistory：用于创建基于 HTML5 History API 的路由历史模式（路由路径不带 # 号），需要服务端配置支持。
// RouteRecordRaw：TypeScript 类型定义，约束路由配置的格式（path、name、component 等必须符合规范）。
// type 关键字表示这只是一个类型导入，编译后会被删除，不会增加打包体积
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 路由配置
const routes: RouteRecordRaw[] = [
    // 根路径路由，访问根路径时会重定向到登录页面
  {
    path: '/',
    redirect: '/login' // 重定向到登录页面，确保用户访问根路径时会被引导到登录界面
  },

  // 登录页面路由配置，包含路径、名称、组件和元信息
  {
    path: '/login', // 登录页面路由，给浏览器地址栏输入 /login 时会显示登录页面
    name: 'Login', // 登录页面路由名称，简单理解为路由的唯一标识符，程序内部可以通过路由名称来访问该路由，而不依赖于具体的路径
    component: () => import('@/views/Login.vue'), // 登录页面组件，使用动态导入（懒加载）方式加载组件，只有在访问该路由时才会加载组件，提高首屏加载速度
    meta: { title: '登录' } // 路由元信息，包含页面标题等自定义信息，可以在路由守卫中使用，用于动态设置页面标题或其他功能
  },

  // 管理后台路由配置，包含仪表板、用户管理、角色管理、权限管理、客户管理、联系人管理、商机管理、合同管理、数据分析和 AI 分析等子路由
  {
    path: '/admin', 
    component: () => import('@/layouts/Layout.vue'), 
    // 组件
    redirect: '/admin/dashboard', // 重定向到仪表板（默认），确保用户访问 /admin 时会被引导到仪表板页面
    // 子路由配置
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/User.vue'),
        meta: { title: '用户管理' }
      },
      // ……使用 map 方法动态生成路由记录，减少重复代码，提高可维护性
      // map 方法遍历一个包含路径、名称和标题的数组，生成对应的路由记录对象。
      // map 方法返回一个新的数组，包含每个子路由的路径、名称、组件和元信息。
      // 组件使用占位组件 Placeholder.vue，实际开发中可以替换为具体的页面组件。
      ...[
        ['system/role', 'SystemRole', '角色管理'],
        ['system/permission', 'SystemPermission', '权限管理'],
        ['business/customer-list', 'CustomerList', '客户列表'],
        ['business/public-customer', 'PublicCustomer', '公海客户'],
        ['business/contact', 'Contact', '联系人管理'],
        ['business/follow-up', 'FollowUp', '跟进记录'],
        ['business/clue', 'Clue', '线索管理'],
        ['business/opportunity', 'Opportunity', '商机管理'],
        ['business/contract', 'Contract', '合同管理'],
        ['analytics/report', 'SalesReport', '销售报表'],
        ['analytics/customer-analysis', 'CustomerAnalysis', '客户分析'],
        ['analytics/performance-analysis', 'PerformanceAnalysis', '业绩分析'],
        ['analytics/ai', 'AiIntelligence', 'AI智能分析'],
        ['analytics', 'Analytics', '数据分析'],
        ['ai-analysis', 'AiAnalysis', 'AI分析']
      ].map(([path, name, title]) => ({
        path,
        name,
        component: () => import('@/views/Placeholder.vue'),
        props: { title },
        meta: { title }
      }))
    ]
  },
    // 通配符路由，匹配所有未定义的路径，重定向到登录页面
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

// 创建路由实例，使用 HTML5 History 模式，并传入路由配置
export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})
