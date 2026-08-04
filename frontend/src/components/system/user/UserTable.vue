<script setup lang="ts">
import type { User } from '@/types/user'

defineProps<{
  users: User[]
}>()

const emit = defineEmits<{
  edit: [user: User]
  delete: [user: User]
}>()
</script>

<template>
  <el-table :data="users" row-key="id" stripe border empty-text="暂无用户数据">
    <el-table-column prop="id" label="编号" width="80" align="center" />
    <el-table-column prop="username" label="用户名" min-width="130" show-overflow-tooltip />
    <el-table-column prop="name" label="真实姓名" min-width="120" show-overflow-tooltip />
    <el-table-column prop="role" label="角色" min-width="110" />
    <el-table-column prop="phone" label="手机号" min-width="140" />
    <el-table-column label="状态" width="100" align="center">
      <template #default="{ row }: { row: User }">
        <el-tag :type="row.status === '正常' ? 'success' : 'info'" effect="light">
          {{ row.status }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="createTime" label="创建时间" min-width="130" />
    <el-table-column label="操作" width="150" fixed="right" align="center">
      <template #default="{ row }: { row: User }">
        <el-button link type="primary" @click="emit('edit', row)">编辑</el-button>
        <el-button link type="danger" @click="emit('delete', row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
