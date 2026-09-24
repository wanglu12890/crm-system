<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

interface MenuItem {
  title: string
  path: string
  permission?: string
}

const route = useRoute()
const authStore = useAuthStore()

const systemMenuItems: MenuItem[] = [
  { title: '用户管理', path: '/admin/system/user', permission: 'user:list' },
  { title: '角色管理', path: '/admin/system/role', permission: 'role:list' },
  { title: '权限管理', path: '/admin/system/permission', permission: 'permission:list' }
]

// 过滤出当前用户有权限访问的系统管理菜单项
const visibleSystemMenuItems = computed(() =>
  systemMenuItems.filter(
    (menuItem) => !menuItem.permission || authStore.hasPermission(menuItem.permission)
  )
)
</script>

<template>
  <aside class="sidebar">
    <el-menu
      :default-active="route.path"
      router
      class="sidebar__menu"
      background-color="#172033"
      text-color="#cbd5e1"
      active-text-color="#ffffff"
    >
      <el-menu-item index="/admin/dashboard">首页</el-menu-item>

      <el-sub-menu index="customer-management">
        <template #title>客户管理</template>
        <el-menu-item index="/admin/business/customer-list">客户列表</el-menu-item>
        <el-menu-item index="/admin/business/public-customer">公海客户</el-menu-item>
        <el-menu-item index="/admin/business/contact">联系人管理</el-menu-item>
        <el-menu-item index="/admin/business/follow-up">跟进记录</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="sales-management">
        <template #title>销售管理</template>
        <el-menu-item index="/admin/business/clue">线索管理</el-menu-item>
        <el-menu-item index="/admin/business/opportunity">商机管理</el-menu-item>
        <el-menu-item index="/admin/business/contract">合同管理</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="data-analysis">
        <template #title>数据分析</template>
        <el-menu-item index="/admin/analytics/report">销售报表</el-menu-item>
        <el-menu-item index="/admin/analytics/customer-analysis">客户分析</el-menu-item>
        <el-menu-item index="/admin/analytics/performance-analysis">业绩分析</el-menu-item>
        <el-menu-item index="/admin/analytics/ai">AI智能分析</el-menu-item>
      </el-sub-menu>

      <!-- 动态渲染有权限的菜单项 -->
      <el-sub-menu v-if="visibleSystemMenuItems.length > 0" index="system">
        <template #title>系统管理</template>
        <el-menu-item
          v-for="menuItem in visibleSystemMenuItems"
          :key="menuItem.path"
          :index="menuItem.path"
        >
          {{ menuItem.title }}
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 220px;
  min-height: 0;
  flex: 0 0 220px;
  overflow-x: hidden;
  overflow-y: auto;
  background: #172033;
}

.sidebar__menu {
  min-height: 100%;
  border-right: 0;
  padding: 12px 8px;
}

.sidebar__menu :deep(.el-menu-item),
.sidebar__menu :deep(.el-sub-menu__title) {
  height: 48px;
  margin-bottom: 4px;
  border-radius: 7px;
  line-height: 48px;
}

.sidebar__menu :deep(.el-menu-item.is-active) {
  background: #2563eb;
}

.sidebar__menu :deep(.el-menu-item:hover),
.sidebar__menu :deep(.el-sub-menu__title:hover) {
  background: rgb(255 255 255 / 8%);
}

@media (max-width: 768px) {
  .sidebar {
    width: 168px;
    flex-basis: 168px;
  }

  .sidebar__menu {
    padding-inline: 4px;
  }
}
</style>
