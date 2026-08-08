<script setup lang="ts">
// 导入 RouterView 组件，用于在布局中显示路由视图；
// 什么是 RouterView？RouterView 是 Vue Router 提供的一个组件，用于在布局中显示当前路由匹配的组件。它会根据路由配置动态渲染对应的组件，从而实现页面的切换和导航。
import { RouterView } from 'vue-router'  

import AppHeader from './Header.vue'
import AppSidebar from './Sidebar.vue'
</script>

<template>
  <div class="admin-layout">
    <!-- 顶部导航栏，就是导入的 Header组件 -->
    <AppHeader />
    <!-- 页面的body部分，包含侧边栏和主要内容 -->
    <div class="admin-layout__body">
      <!-- 侧边栏，就是导入的 Sidebar组件 -->
      <AppSidebar />
      <main class="admin-layout__content">
        <!-- RouterView 组件用于显示当前路由匹配的组件，例如：User.Vue -->
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-width: 320px;
  min-height: 100vh;
  min-height: 100dvh;
  flex-direction: column;
  overflow: hidden;
  background: #f4f7fb;
}

.admin-layout__body {
  display: flex;
  min-height: 0;
  flex: 1;
}

.admin-layout__content {
  min-width: 0;
  padding: 24px;
  flex: 1;
  overflow: auto;
}

@media (max-width: 768px) {
  .admin-layout__content {
    padding: 16px;
  }
}
</style>
