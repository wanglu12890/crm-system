<script setup lang="ts">
import type { CustomerLevel, CustomerListItem, CustomerStatus, CustomerType } from '@/types/customer'
import { CUSTOMER_LEVEL_LABELS, CUSTOMER_STATUS_LABELS, CUSTOMER_TYPE_LABELS } from '@/types/customer'

defineProps<{
  customers: CustomerListItem[]
  loading: boolean
  showEdit: boolean
}>()

const emit = defineEmits<{
  view: [customer: CustomerListItem]
  edit: [customer: CustomerListItem]
}>()

const customerTypeLabel = (value: CustomerType) => CUSTOMER_TYPE_LABELS[value] || value
const customerLevelLabel = (value: CustomerLevel | null) => value ? CUSTOMER_LEVEL_LABELS[value] || value : '未评级'
const customerStatusLabel = (value: CustomerStatus) => CUSTOMER_STATUS_LABELS[value] || value
const customerStatusTagType = (value: CustomerStatus) => {
  if (value === 'ACTIVE') return 'success'
  if (value === 'INACTIVE') return 'info'
  return 'warning'
}
const formatDateTime = (value: string) => value ? value.replace('T', ' ').slice(0, 19) : '-'
</script>

<template>
  <el-table v-loading="loading" :data="customers" row-key="id" stripe border empty-text="暂无客户数据">
    <el-table-column prop="customerNo" label="客户编号" min-width="150" show-overflow-tooltip />
    <el-table-column prop="customerName" label="客户名称" min-width="180" show-overflow-tooltip>
      <template #default="{ row }: { row: CustomerListItem }">
        <el-button link type="primary" @click="emit('view', row)">{{ row.customerName }}</el-button>
      </template>
    </el-table-column>
    <el-table-column label="客户类型" width="110" align="center">
      <template #default="{ row }: { row: CustomerListItem }">{{ customerTypeLabel(row.customerType) }}</template>
    </el-table-column>
    <el-table-column label="客户等级" width="100" align="center">
      <template #default="{ row }: { row: CustomerListItem }">
        <el-tag v-if="row.customerLevel" effect="plain">{{ customerLevelLabel(row.customerLevel) }}</el-tag>
        <span v-else class="customer-table__empty-value">未评级</span>
      </template>
    </el-table-column>
    <el-table-column label="所属行业" min-width="130" show-overflow-tooltip>
      <template #default="{ row }: { row: CustomerListItem }">{{ row.industry || '-' }}</template>
    </el-table-column>
    <el-table-column label="客户状态" width="120" align="center">
      <template #default="{ row }: { row: CustomerListItem }">
        <el-tag :type="customerStatusTagType(row.status)" effect="light">{{ customerStatusLabel(row.status) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="负责人" min-width="120" show-overflow-tooltip>
      <template #default="{ row }: { row: CustomerListItem }">{{ row.ownerName || '-' }}</template>
    </el-table-column>
    <el-table-column label="更新时间" min-width="170">
      <template #default="{ row }: { row: CustomerListItem }">{{ formatDateTime(row.updatedAt) }}</template>
    </el-table-column>
    <el-table-column label="操作" width="140" fixed="right" align="center">
      <template #default="{ row }: { row: CustomerListItem }">
        <el-button link type="primary" @click="emit('view', row)">查看</el-button>
        <el-button v-if="showEdit" link type="primary" @click="emit('edit', row)">编辑</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.customer-table__empty-value {
  color: #98a2b3;
}
</style>
