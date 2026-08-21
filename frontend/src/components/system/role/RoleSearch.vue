<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { RoleSearchCriteria } from '@/types/role'

const props = defineProps<{ modelValue: RoleSearchCriteria }>()

const emit = defineEmits<{
  search: [criteria: RoleSearchCriteria]
  reset: []
}>()

const searchForm = reactive<RoleSearchCriteria>({ ...props.modelValue })

watch(
  () => props.modelValue,
  (value) => Object.assign(searchForm, value),
  { deep: true }
)

const handleSearch = () => emit('search', { ...searchForm })

const handleReset = () => {
  Object.assign(searchForm, { roleName: '', roleCode: '', status: '' })
  emit('reset')
}
</script>

<template>
  <div class="search-panel">
    <el-form :model="searchForm" inline label-width="72px" @submit.prevent="handleSearch">
      <el-form-item label="角色名称">
        <el-input
          v-model.trim="searchForm.roleName"
          placeholder="请输入角色名称"
          clearable
          maxlength="64"
          @keyup.enter="handleSearch"
        />
      </el-form-item>

      <el-form-item label="角色编码">
        <el-input
          v-model.trim="searchForm.roleCode"
          placeholder="请输入角色编码"
          clearable
          maxlength="64"
          @keyup.enter="handleSearch"
        />
      </el-form-item>

      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>

      <el-form-item class="search-panel__actions">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.search-panel {
  padding: 18px 20px 0;
  border: 1px solid #e5eaf2;
  border-radius: 8px;
  background: #f8fafc;
}

.search-panel :deep(.el-form-item) {
  margin-right: 24px;
  margin-bottom: 18px;
}

.search-panel :deep(.el-input),
.search-panel :deep(.el-select) {
  width: 200px;
}

.search-panel__actions {
  margin-right: 0 !important;
}

@media (max-width: 768px) {
  .search-panel :deep(.el-form),
  .search-panel :deep(.el-form-item) {
    display: flex;
    width: 100%;
    margin-right: 0;
  }

  .search-panel :deep(.el-form-item__content),
  .search-panel :deep(.el-input),
  .search-panel :deep(.el-select) {
    width: 100%;
  }
}
</style>
