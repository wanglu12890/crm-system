<script setup lang="ts">
import { computed } from 'vue'
import type { Role } from '@/types/role'
import { useAuthStore } from '@/stores/auth'

defineProps<{ roles: Role[] }>()

const authStore = useAuthStore()

// 当前登录用户是否为超级管理员
const isSuperAdmin = computed(() => authStore.hasRole('SUPER_ADMIN'))

// 是否可编辑角色（仅超级管理员可编辑）
const canEditRole = computed(() => isSuperAdmin.value)

// 是否可分配权限（与行无关）
const canAssignPermission = computed(() =>
  isSuperAdmin.value && authStore.hasPermission('role:assign_permission')
)

// 行级：目标角色不是 SUPER_ADMIN，且当前用户有能力分配权限
const canConfigurePermission = (role: Role): boolean =>
  canAssignPermission.value && role.roleCode !== 'SUPER_ADMIN'

// 当前行没有任何可执行操作时，显示只读状态
const hasRoleOperationPermission = (role: Role): boolean =>
  canEditRole.value || canConfigurePermission(role)

const emit = defineEmits<{
  edit: [role: Role]
  configurePermission: [role: Role]
}>()
</script>

<template>
  <el-table :data="roles" row-key="id" stripe border empty-text="暂无角色数据">
    <el-table-column prop="roleName" label="角色名称" min-width="120" align="center" />
    <el-table-column prop="roleCode" label="角色编码" min-width="120" align="center" />
    <el-table-column label="状态" width="100" align="center">
      <template #default="{ row }: { row: Role }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="permissionCount" label="权限数量" width="110" align="center" />
    <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
    <el-table-column label="操作" width="190" fixed="right" align="center">
      <template #default="{ row }: { row: Role }">
        <template v-if="hasRoleOperationPermission(row)">
          <el-button v-if="canEditRole" link type="primary" @click="emit('edit', row)">
            编辑
          </el-button>
          <el-button
            v-if="canConfigurePermission(row)"
            link
            type="primary"
            @click="emit('configurePermission', row)"
          >
            配置权限
          </el-button>
        </template>
        <span v-else class="role-table__read-only">仅查看</span>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.role-table__read-only {
  color: #98a2b3;
  font-size: 14px;
  letter-spacing: 0.5px;
}
</style>
