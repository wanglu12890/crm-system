<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getPermissionList } from '@/api/permission'
import type {
  PermissionListItem,
  PermissionListQuery,
  PermissionStatus
} from '@/types/permission'
import type { ApiProblemDetail } from '@/utils/request'

interface PermissionQueryForm {
  keyword: string
  moduleId: string
  status: '' | PermissionStatus
}

interface ModuleOption {
  id: string
  name: string
}

const loading = ref(false)
const permissionList = ref<PermissionListItem[]>([])
const moduleOptions = ref<ModuleOption[]>([])
const queryForm = reactive<PermissionQueryForm>({ keyword: '', moduleId: '', status: '' })

const buildModuleOptions = (permissions: PermissionListItem[]): ModuleOption[] => {
  const modules = new Map<string, ModuleOption>()
  permissions.forEach((permission) => {
    if (permission.moduleId && permission.moduleName && !modules.has(permission.moduleId)) {
      modules.set(permission.moduleId, { id: permission.moduleId, name: permission.moduleName })
    }
  })
  return [...modules.values()]
}

const showLoadError = (error: unknown) => {
  const detail = axios.isAxiosError<ApiProblemDetail>(error)
    ? error.response?.data?.detail
    : undefined
  ElMessage.error(detail || '权限列表加载失败，请稍后重试')
}

const loadInitialPermissionList = async () => {
  loading.value = true
  try {
    const response = await getPermissionList()
    permissionList.value = response.data
    moduleOptions.value = buildModuleOptions(response.data)
  } catch (error: unknown) {
    permissionList.value = []
    moduleOptions.value = []
    showLoadError(error)
  } finally {
    loading.value = false
  }
}

const buildQueryParams = (): PermissionListQuery => {
  const params: PermissionListQuery = {}
  const keyword = queryForm.keyword.trim()
  if (keyword) params.keyword = keyword
  if (queryForm.moduleId) params.moduleId = queryForm.moduleId
  if (queryForm.status !== '') params.status = queryForm.status
  return params
}

const loadFilteredPermissions = async () => {
  loading.value = true
  try {
    const response = await getPermissionList(buildQueryParams())
    permissionList.value = response.data
  } catch (error: unknown) {
    permissionList.value = []
    showLoadError(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadFilteredPermissions()
const handleReset = async () => {
  Object.assign(queryForm, { keyword: '', moduleId: '', status: '' })
  await loadFilteredPermissions()
}

const permissionTypeLabel = (type: string): string => {
  const labels: Record<string, string> = { MENU: '菜单', BUTTON: '按钮', API: '接口' }
  return labels[type] || type
}

const permissionTypeTag = (type: string): 'primary' | 'success' | 'info' => {
  if (type === 'MENU') return 'primary'
  if (type === 'API') return 'success'
  return 'info'
}

onMounted(loadInitialPermissionList)
</script>

<template>
  <div class="permission-list">
    <div class="search-panel">
      <el-form :model="queryForm" inline label-width="120px" @submit.prevent="handleSearch">
        <el-form-item label="权限名称/权限码">
          <el-input
            v-model="queryForm.keyword"
            placeholder="请输入权限名称或权限码"
            clearable
            maxlength="128"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="所属模块">
          <el-select v-model="queryForm.moduleId" placeholder="全部模块" clearable>
            <el-option
              v-for="module in moduleOptions"
              :key="module.id"
              :label="module.name"
              :value="module.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-panel__actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="section-title">权限列表</div>
    <el-table
      v-loading="loading"
      :data="permissionList"
      row-key="id"
      stripe
      border
      empty-text="暂无权限数据"
    >
      <el-table-column prop="permissionName" label="权限名称" min-width="130" />
      <el-table-column label="权限码" min-width="180">
        <template #default="{ row }: { row: PermissionListItem }">
          <code class="permission-code">{{ row.permissionCode }}</code>
        </template>
      </el-table-column>
      <el-table-column label="所属模块" min-width="120">
        <template #default="{ row }: { row: PermissionListItem }">
          {{ row.moduleName || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="权限类型" width="100" align="center">
        <template #default="{ row }: { row: PermissionListItem }">
          <el-tag :type="permissionTypeTag(row.permissionType)" effect="plain">
            {{ permissionTypeLabel(row.permissionType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }: { row: PermissionListItem }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="路由 / API" min-width="220" show-overflow-tooltip>
        <template #default="{ row }: { row: PermissionListItem }">
          <div v-if="row.permissionType === 'API' && (row.httpMethod || row.apiPath)" class="path-cell">
            <el-tag v-if="row.httpMethod" size="small" effect="plain">{{ row.httpMethod }}</el-tag>
            <span>{{ row.apiPath || '—' }}</span>
          </div>
          <span v-else-if="row.permissionType === 'MENU' && row.routePath">{{ row.routePath }}</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>
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
  width: 220px;
}

.search-panel__actions {
  margin-right: 0 !important;
}

.section-title {
  margin: 20px 0 14px;
  color: #344054;
  font-size: 16px;
  font-weight: 600;
}

.permission-code {
  padding: 2px 6px;
  border-radius: 4px;
  color: #344054;
  background: #f2f4f7;
  font-size: 13px;
}

.path-cell {
  display: flex;
  align-items: center;
  gap: 8px;
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
