<script setup lang="ts">

import type { User } from '@/types/user'  
import type { Role } from '@/types/role'

// 定义props，用于接收父组件传递的数据
// 这里定义了一个props：users，它是一个User类型的数组。
const props = defineProps<{
  users: User[]
  roles: Role[]
  showEdit: boolean
  canEdit: (user: User) => boolean
  showResetPassword: boolean
  canResetPassword: (user: User) => boolean
}>()

const emit = defineEmits<{
  edit: [user: User]
  delete: [user: User]
  resetPassword: [user: User]
}>()
</script>

<template>
  <el-table :data="users" row-key="id" stripe border empty-text="暂无用户数据">
    <el-table-column prop="id" label="编号" min-width="150"  align="center" />
    <el-table-column prop="username" label="用户名" min-width="130" align="center" show-overflow-tooltip />
    <el-table-column prop="name" label="真实姓名" min-width="120" align="center" show-overflow-tooltip />
    <el-table-column label="角色" min-width="180" align="center" show-overflow-tooltip>
      <!-- 找到角色就显示角色名称，否则显示角色ID -->
      <template #default="{ row }: { row: User }">
        {{
          row.roleIds
            .map((roleId) => props.roles.find((role) => role.id === roleId)?.roleName || roleId)
            .join('、') || '未分配'
        }}
      </template>
    </el-table-column>
    <el-table-column prop="phone" label="手机号" min-width="140" align="center" />
    <el-table-column label="状态" width="100" align="center">
      <template #default="{ row }: { row: User }">
        <el-tag :type="row.status === '正常' ? 'success' : 'info'" effect="light">
          {{ row.status }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="createTime" label="创建时间" min-width="130" align="center" />
    <el-table-column label="操作" width="260" fixed="right" align="center">
      <template #default="{ row }: { row: User }">
        <el-tooltip
          v-if="showEdit"
          :content="props.canEdit(row) ? '' : '无权编辑系统级管理员'"
          :disabled="props.canEdit(row)"
          placement="top"
        >
          <span>
            <el-button
              link
              type="primary"
              :disabled="!props.canEdit(row)"
              @click="emit('edit', row)"
            >编辑</el-button>
          </span>
        </el-tooltip>
        <el-button
          v-if="showResetPassword && props.canResetPassword(row)"
          link
          type="warning"
          @click="emit('resetPassword', row)"
        >重置密码</el-button>
        <el-button link type="danger" @click="emit('delete', row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
