<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import CustomerSearch from '@/components/business/customer/CustomerSearch.vue'
import CustomerTable from '@/components/business/customer/CustomerTable.vue'
import CustomerFormDialog from '@/components/business/customer/CustomerFormDialog.vue'
import { getCustomerList } from '@/api/customer'
import { getUserList } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { CustomerListItem, CustomerListQuery, CustomerSearchCriteria } from '@/types/customer'
import type { User } from '@/types/user'
import type { ApiProblemDetail } from '@/utils/request'

const DEFAULT_PAGE_SIZE = 10
const emptyCriteria = (): CustomerSearchCriteria => ({
  keyword: '',
  customerType: '',
  customerLevel: '',
  status: '',
  ownerId: ''
})

const authStore = useAuthStore()
const canCreateCustomer = computed(() => authStore.hasPermission('customer:create'))
const canUpdateCustomer = computed(() => authStore.hasPermission('customer:update'))
const canLoadOwners = computed(
  () => !authStore.hasRole('SALES_STAFF') && authStore.hasPermission('user:list')
)

const customers = ref<CustomerListItem[]>([])
const ownerOptions = ref<User[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(DEFAULT_PAGE_SIZE)
const total = ref(0)
const createDialogVisible = ref(false)
const searchCriteria = ref<CustomerSearchCriteria>(emptyCriteria())

const buildQuery = (): CustomerListQuery => ({
  page: page.value,
  size: size.value,
  keyword: searchCriteria.value.keyword || undefined,
  customerType: searchCriteria.value.customerType || undefined,
  customerLevel: searchCriteria.value.customerLevel || undefined,
  status: searchCriteria.value.status || undefined,
  ownerId: searchCriteria.value.ownerId || undefined
})

const getErrorDetail = (error: unknown): string | undefined =>
  axios.isAxiosError<ApiProblemDetail>(error) ? error.response?.data?.detail : undefined

const loadCustomers = async () => {
  loading.value = true
  try {
    const response = await getCustomerList(buildQuery())
    customers.value = response.data.records
    total.value = response.data.total
    page.value = response.data.page
    size.value = response.data.size
  } catch (error: unknown) {
    customers.value = []
    total.value = 0
    ElMessage.error(getErrorDetail(error) || '客户列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadOwnerOptions = async () => {
  if (!canLoadOwners.value) return
  try {
    const response = await getUserList()
    ownerOptions.value = response.data.filter(
      (user) => user.status === '正常'
        && user.roleCodes.some((code) => code === 'SALES_MANAGER' || code === 'SALES_STAFF')
    )
  } catch (error: unknown) {
    ownerOptions.value = []
    ElMessage.error(getErrorDetail(error) || '负责人候选加载失败，请稍后重试')
  }
}

const handleSearch = async (criteria: CustomerSearchCriteria) => {
  searchCriteria.value = criteria
  page.value = 1
  await loadCustomers()
}

const handleReset = async () => {
  searchCriteria.value = emptyCriteria()
  page.value = 1
  await loadCustomers()
}

const handleSizeChange = async (value: number) => {
  size.value = value
  page.value = 1
  await loadCustomers()
}

const handleCurrentChange = async (value: number) => {
  page.value = value
  await loadCustomers()
}

const handleCreate = () => {
  createDialogVisible.value = true
}

const handleCreateSuccess = async () => {
  await loadCustomers()
}

const showPendingFeature = (feature: string) => {
  ElMessage.info(`${feature}功能待实现`)
}

const handleView = (_customer: CustomerListItem) => showPendingFeature('客户详情')
const handleEdit = (_customer: CustomerListItem) => showPendingFeature('编辑客户')

onMounted(() => {
  void loadCustomers()
  void loadOwnerOptions()
})
</script>

<template>
  <section class="customer-page">
    <header class="customer-page__header">
      <div>
        <h1>客户列表</h1>
        <p>查询和管理当前数据范围内的客户资料</p>
      </div>
    </header>

    <CustomerSearch
      :model-value="searchCriteria"
      :owners="ownerOptions"
      :show-owner-filter="canLoadOwners"
      :loading="loading"
      @search="handleSearch"
      @reset="handleReset"
    />

    <div v-if="canCreateCustomer" class="customer-page__toolbar">
      <el-button type="primary" @click="handleCreate">新建客户</el-button>
    </div>

    <CustomerTable
      :customers="customers"
      :loading="loading"
      :show-edit="canUpdateCustomer"
      @view="handleView"
      @edit="handleEdit"
    />

    <div class="customer-page__pagination">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <CustomerFormDialog
      v-model="createDialogVisible"
      @success="handleCreateSuccess"
    />
  </section>
</template>

<style scoped>
.customer-page {
  padding: 24px;
  border: 1px solid #eaecf0;
  border-radius: 10px;
  background: #fff;
}

.customer-page__header {
  margin-bottom: 22px;
}

.customer-page__header h1 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  font-weight: 600;
}

.customer-page__header p {
  margin: 7px 0 0;
  color: #667085;
  font-size: 14px;
}

.customer-page__toolbar {
  display: flex;
  margin: 20px 0 14px;
  justify-content: flex-end;
}

.customer-page__pagination {
  display: flex;
  margin-top: 20px;
  justify-content: flex-end;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .customer-page {
    padding: 16px;
  }

  .customer-page__pagination {
    justify-content: flex-start;
  }
}
</style>
