<script setup lang="ts">
import type { CustomerContact } from '@/types/customer'
defineProps<{ contacts: CustomerContact[] }>()
const roles: Record<string, string> = { DECISION: '决策者', INFLUENCER: '影响者', USER: '使用者', OTHER: '其他' }
</script>
<template>
  <el-table :data="contacts" row-key="id" empty-text="暂无联系人" style="width: 100%">
    <el-table-column label="姓名" min-width="130"><template #default="{ row }">{{ row.contactName }} <el-tag v-if="row.isPrimary === 1" size="small">主联系人</el-tag></template></el-table-column>
    <el-table-column label="职务" min-width="100"><template #default="{ row }">{{ row.position || '-' }}</template></el-table-column>
    <el-table-column label="联系电话" min-width="150"><template #default="{ row }">{{ row.mobile || row.telephone || '-' }}</template></el-table-column>
    <el-table-column label="邮箱" min-width="180"><template #default="{ row }">{{ row.email || '-' }}</template></el-table-column>
    <el-table-column label="决策角色" min-width="100"><template #default="{ row }">{{ roles[row.decisionRole] || row.decisionRole || '-' }}</template></el-table-column>
  </el-table>
</template>
