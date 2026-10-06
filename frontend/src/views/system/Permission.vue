<script setup lang="ts">
import { ref, watch } from 'vue'
import PermissionList from '@/views/system/permission/PermissionList.vue'
import PermissionOverview from '@/views/system/permission/PermissionOverview.vue'

type PermissionTab = 'list' | 'overview'

const activeTab = ref<PermissionTab>('list')
const overviewLoaded = ref(false)

watch(activeTab, (tab) => {
  if (tab === 'overview') overviewLoaded.value = true
})
</script>

<template>
  <section class="permission-page">
    <header class="permission-page__header">
      <h1>权限管理</h1>
      <p>查看系统权限定义及角色权限配置情况</p>
    </header>

    <el-tabs v-model="activeTab" class="permission-page__tabs">
      <el-tab-pane label="权限列表" name="list">
        <PermissionList />
      </el-tab-pane>
      <el-tab-pane label="权限总览" name="overview">
        <PermissionOverview v-if="overviewLoaded" />
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<style scoped>
.permission-page {
  padding: 24px;
  border: 1px solid #eaecf0;
  border-radius: 10px;
  background: #fff;
}

.permission-page__header {
  margin-bottom: 16px;
}

.permission-page__header h1 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  font-weight: 600;
}

.permission-page__header p {
  margin: 7px 0 0;
  color: #667085;
  font-size: 14px;
}

.permission-page__tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .permission-page {
    padding: 16px;
  }
}
</style>
