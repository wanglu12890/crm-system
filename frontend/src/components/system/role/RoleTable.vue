<script setup lang="ts">
import type { Role } from '@/types/role'

defineProps<{ roles: Role[] }>()

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
        <el-button link type="primary" @click="emit('edit', row)">编辑</el-button>
        <el-button link type="primary" @click="emit('configurePermission', row)">
          配置权限
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
