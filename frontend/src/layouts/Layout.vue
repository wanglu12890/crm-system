<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from './Header.vue'
import AppSidebar from './Sidebar.vue'
</script>

<template>
  <div class="admin-layout">
    <AppHeader />
    <div class="admin-layout__body">
      <AppSidebar />
      <main class="admin-layout__content">
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
