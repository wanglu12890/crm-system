<script setup lang="ts">
import type { CustomerFollowRecord } from '@/types/customer'
import { detailDate } from '@/utils/customerDetail'
defineProps<{ records: CustomerFollowRecord[]; page: number; size: number; total: number }>()
defineEmits<{ pageChange: [page: number] }>()
const methods: Record<string, string> = { PHONE: '电话', VISIT: '拜访', EMAIL: '邮件', IM: '即时通讯', OTHER: '其他' }
</script>
<template>
  <el-empty v-if="!records.length" description="暂无跟进记录" />
  <el-timeline v-else>
    <el-timeline-item v-for="record in records" :key="record.id" :timestamp="detailDate(record.followAt)" placement="top">
      <strong>{{ methods[record.followType] || record.followType }} / {{ record.ownerName || '-' }}</strong>
      <p class="content">{{ record.content || '-' }}</p>
      <p v-if="record.nextFollowAt">下次跟进{{ detailDate(record.nextFollowAt) }}</p>
    </el-timeline-item>
  </el-timeline>
  <el-pagination :current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" small @current-change="$emit('pageChange', $event)" />
</template>
<style scoped>
.content { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
