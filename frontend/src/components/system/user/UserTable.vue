<script setup lang="ts">
// 导入User类型，用于定义props的类型
// User类型定义了用户对象的结构，包括id、username、name、role、phone、status和createTime等属性。通过导入User类型，我们可以确保传递给UserTable组件的users数组中的每个元素都符合User类型的定义，从而提高代码的可维护性和可读性。
// User类型的定义位于src/types/user.ts文件中，它是一个TypeScript接口，描述了用户对象的属性和类型。通过使用User类型，我们可以在编译时进行类型检查，避免运行时错误。
// 为什么要导入User类型？
// 因为在UserTable组件中，我们需要接收一个users的props，它是一个User类型的数组。为了确保类型安全，我们需要导入User类型来定义props的类型。
// 怎么理解props的类型？props是组件的属性，它们是从父组件传递过来的数据。通过定义props的类型，我们可以确保传递的数据符合预期的结构和类型，从而避免运行时错误。
import type { User } from '@/types/user'  

// 定义props，用于接收父组件传递的数据
// 这里定义了一个props：users，它是一个User类型的数组。
defineProps<{
  users: User[]
}>()

// 定义emits，并赋值给emit，用于向父组件发送事件。
// 为什么要定义emits？
// 因为在UserTable组件中，我们需要向父组件发送两个事件：edit和delete。当用户点击编辑或删除按钮时，我们需要通知父组件进行相应的操作。
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
