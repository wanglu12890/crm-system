<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { CustomerSearchCriteria } from '@/types/customer'
import type { User } from '@/types/user'

const props = defineProps<{
  modelValue: CustomerSearchCriteria
  owners: User[]
  showOwnerFilter: boolean
  loading: boolean
}>()

const emit = defineEmits<{
  search: [criteria: CustomerSearchCriteria]
  reset: []
}>()

const emptyCriteria = (): CustomerSearchCriteria => ({
  keyword: '', customerType: '', customerLevel: '', status: '', ownerId: ''
})
const searchForm = reactive<CustomerSearchCriteria>({ ...props.modelValue })

watch(
  () => props.modelValue,
  (value) => Object.assign(searchForm, value),
  { deep: true }
)

const handleSearch = () => {
  emit('search', { ...searchForm, keyword: searchForm.keyword.trim() })
}

const handleReset = () => {
  Object.assign(searchForm, emptyCriteria())
  emit('reset')
}
</script>

<template>
  <div class="search-panel">
    <el-form :model="searchForm" inline label-width="76px" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="客户编号 / 客户名称" clearable maxlength="200" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="客户类型">
        <el-select v-model="searchForm.customerType" placeholder="全部类型" clearable>
          <el-option label="企业客户" value="ENTERPRISE" />
          <el-option label="个人客户" value="INDIVIDUAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户等级">
        <el-select v-model="searchForm.customerLevel" placeholder="全部等级" clearable>
          <el-option label="A级" value="A" />
          <el-option label="B级" value="B" />
          <el-option label="C级" value="C" />
          <el-option label="D级" value="D" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户状态">
        <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
          <el-option label="潜在客户" value="POTENTIAL" />
          <el-option label="活跃客户" value="ACTIVE" />
          <el-option label="非活跃客户" value="INACTIVE" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="showOwnerFilter" label="负责人">
        <el-select v-model="searchForm.ownerId" placeholder="全部负责人" clearable filterable>
          <el-option
            v-for="owner in owners"
            :key="owner.id"
            :label="`${owner.name || owner.username}（${owner.username}）`"
            :value="owner.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item class="search-panel__actions">
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button :disabled="loading" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
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
  width: 210px;
}

.search-panel__actions {
  margin-right: 0 !important;
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
