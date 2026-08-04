<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
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

      <el-sub-menu index="system">
        <template #title>系统管理</template>
        <el-menu-item index="/admin/system/user">用户管理</el-menu-item>
        <el-menu-item index="/admin/system/role">角色管理</el-menu-item>
        <el-menu-item index="/admin/system/permission">权限管理</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="business">
        <template #title>业务管理</template>
        <el-menu-item index="/admin/business/customer">客户管理</el-menu-item>
        <el-menu-item index="/admin/business/contact">联系人管理</el-menu-item>
        <el-menu-item index="/admin/business/opportunity">商机管理</el-menu-item>
        <el-menu-item index="/admin/business/contract">合同管理</el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/admin/analytics">数据分析</el-menu-item>
      <el-menu-item index="/admin/ai-analysis">AI分析</el-menu-item>
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
