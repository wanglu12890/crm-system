<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getPermissionOverview } from '@/api/permission'
import type {
  PermissionOverviewPermission,
  PermissionOverviewResponse
} from '@/types/permission'
import type { ApiProblemDetail } from '@/utils/request'

const emptyOverview = (): PermissionOverviewResponse => ({
  summary: { permissionCount: 0, roleCount: 0, moduleCount: 0 },
  roles: [],
  permissions: [],
  rolePermissions: {}
})

const loading = ref(false)
const overview = ref<PermissionOverviewResponse>(emptyOverview())
const rolePermissionSets = ref<Record<string, Set<string>>>({})

const buildRolePermissionSets = (
  rolePermissions: Record<string, string[]>
): Record<string, Set<string>> => {
  const result: Record<string, Set<string>> = {}
  Object.entries(rolePermissions).forEach(([roleId, permissionIds]) => {
    result[roleId] = new Set(permissionIds)
  })
  return result
}

const loadOverview = async () => {
  loading.value = true
  try {
    const response = await getPermissionOverview()
    overview.value = response.data
    rolePermissionSets.value = buildRolePermissionSets(response.data.rolePermissions)
  } catch (error: unknown) {
    overview.value = emptyOverview()
    rolePermissionSets.value = {}
    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || '权限总览加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const hasRolePermission = (roleId: string, permissionId: string): boolean =>
  rolePermissionSets.value[roleId]?.has(permissionId) ?? false

const permissionTypeLabel = (type: string): string => {
  const labels: Record<string, string> = { MENU: '菜单', BUTTON: '按钮', API: '接口' }
  return labels[type] || type
}

onMounted(loadOverview)
</script>

<template>
  <div v-loading="loading" class="permission-overview">
    <div class="section-title">权限总览</div>

    <div class="summary-grid">
      <div class="summary-item">
        <span>权限总数</span>
        <strong>{{ overview.summary.permissionCount }}</strong>
      </div>
      <div class="summary-item">
        <span>角色总数</span>
        <strong>{{ overview.summary.roleCount }}</strong>
      </div>
      <div class="summary-item">
        <span>模块总数</span>
        <strong>{{ overview.summary.moduleCount }}</strong>
      </div>
    </div>

    <el-alert
      class="overview-note"
      type="info"
      :closable="false"
      show-icon
      title="勾选表示角色已配置该权限；停用角色或停用权限的配置关系仍会保留，但当前不产生有效授权。角色权限调整请前往角色管理。"
    />

    <el-table
      :data="overview.permissions"
      row-key="id"
      stripe
      border
      empty-text="暂无权限数据"
      class="overview-table"
    >
      <el-table-column label="所属模块" min-width="120" fixed="left">
        <template #default="{ row }: { row: PermissionOverviewPermission }">
          {{ row.moduleName || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="权限名称" min-width="150" fixed="left">
        <template #default="{ row }: { row: PermissionOverviewPermission }">
          <div class="permission-name">
            <span>{{ row.permissionName }}</span>
            <el-tag v-if="row.status === 0" size="small" type="info">停用</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="权限码" min-width="190" fixed="left">
        <template #default="{ row }: { row: PermissionOverviewPermission }">
          <code class="permission-code">{{ row.permissionCode }}</code>
        </template>
      </el-table-column>
      <el-table-column label="权限类型" width="100" align="center">
        <template #default="{ row }: { row: PermissionOverviewPermission }">
          {{ permissionTypeLabel(row.permissionType) }}
        </template>
      </el-table-column>

      <el-table-column
        v-for="role in overview.roles"
        :key="role.id"
        :min-width="140"
        align="center"
      >
        <template #header>
          <el-tooltip :content="role.roleCode" placement="top">
            <div class="role-header">
              <span>{{ role.roleName }}</span>
              <el-tag v-if="role.status === 0" size="small" type="info">停用</el-tag>
            </div>
          </el-tooltip>
        </template>
        <template #default="{ row }: { row: PermissionOverviewPermission }">
          <span
            v-if="hasRolePermission(role.id, row.id)"
            class="permission-check"
            aria-label="已配置"
          >✓</span>
          <span v-else class="permission-empty">—</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.permission-overview {
  min-height: 240px;
}

.section-title {
  margin-bottom: 14px;
  color: #344054;
  font-size: 16px;
  font-weight: 600;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-item {
  display: flex;
  min-height: 76px;
  padding: 14px 18px;
  border: 1px solid #e5eaf2;
  border-radius: 8px;
  flex-direction: column;
  justify-content: center;
  background: #f8fafc;
}

.summary-item span {
  color: #667085;
  font-size: 13px;
}

.summary-item strong {
  margin-top: 5px;
  color: #172033;
  font-size: 24px;
  font-weight: 600;
}

.overview-note {
  margin-bottom: 16px;
}

.overview-table {
  width: 100%;
}

.permission-name,
.role-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.role-header {
  justify-content: center;
  line-height: 1.3;
}

.permission-code {
  padding: 2px 6px;
  border-radius: 4px;
  color: #344054;
  background: #f2f4f7;
  font-size: 13px;
}

.permission-check {
  color: #16a34a;
  font-size: 18px;
  font-weight: 700;
}

.permission-empty {
  color: #98a2b3;
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
