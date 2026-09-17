<script setup lang="ts">
// 导入 reactive 和 watch 函数，以及 UserSearchCriteria 类型
// reactive 用于创建响应式对象，watch 用于监听响应式数据的变化
// 什么是 type 类型？type 类型用于定义 TypeScript 中的类型别名
import { reactive, watch } from 'vue'
import type { UserSearchCriteria } from '@/types/user'
import type { Role } from '@/types/role'

const props = defineProps<{
  modelValue: UserSearchCriteria
  roles: Role[]
}>()

const emit = defineEmits<{
  search: [criteria: UserSearchCriteria]
  reset: []
}>()

const searchForm = reactive<UserSearchCriteria>({ ...props.modelValue })

watch(
  () => props.modelValue,
  (value) => Object.assign(searchForm, value),
  { deep: true }
)

const handleSearch = () => {
  emit('search', { ...searchForm })
}

const handleReset = () => {
  Object.assign(searchForm, { username: '', role: '', status: '' })
  emit('reset')
}
</script>

<template>
  <div class="search-panel">
    <el-form :model="searchForm" inline label-width="64px" @submit.prevent="handleSearch">
      <el-form-item label="用户名">
        <el-input
          v-model.trim="searchForm.username"
          placeholder="请输入用户名"
          clearable
          maxlength="64"
          @keyup.enter="handleSearch"
        />
      </el-form-item>

      <el-form-item label="角色">
        <el-select v-model="searchForm.role" placeholder="全部角色" clearable>
          <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
          <el-option label="正常" value="正常" />
          <el-option label="停用" value="停用" />
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
