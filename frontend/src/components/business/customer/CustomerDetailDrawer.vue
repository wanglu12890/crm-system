<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getCustomerDetail, getCustomerContacts, getCustomerFollowRecords } from '@/api/customer'
import type { CustomerDetail, CustomerContact, CustomerFollowRecord } from '@/types/customer'
import { CUSTOMER_TYPE_LABELS, CUSTOMER_LEVEL_LABELS, CUSTOMER_STATUS_LABELS } from '@/types/customer'
import { useAuthStore } from '@/stores/auth'
import { detailError, detailDate } from '@/utils/customerDetail'
import CustomerContactList from './CustomerContactList.vue'
import CustomerFollowRecordList from './CustomerFollowRecordList.vue'
const props = defineProps<{ modelValue: boolean; customerId: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const auth = useAuthStore()
const canDetail = computed(() => auth.hasPermission('customer:list'))
const canContacts = computed(() => auth.hasPermission('contact:list'))
const canFollow = computed(() => auth.hasPermission('follow:list'))
const detail = ref<CustomerDetail | null>(null)
const contacts = ref<CustomerContact[]>([])
const records = ref<CustomerFollowRecord[]>([])
const loading = ref(false), contactsLoading = ref(false), followLoading = ref(false)
const error = ref(''), contactsError = ref(''), followError = ref('')
const page = ref(1), size = ref(10), total = ref(0)
let generation = 0, detailRequest = 0, contactRequest = 0, followRequest = 0
const active = (version: number) => version === generation && props.modelValue
async function loadDetail() {
  const version = generation, request = ++detailRequest
  loading.value = true; error.value = ''; detail.value = null
  try {
    const response = await getCustomerDetail(props.customerId)
    if (active(version) && request === detailRequest) detail.value = response.data
  } catch (e) { if (active(version) && request === detailRequest) error.value = detailError(e) }
  finally { if (active(version) && request === detailRequest) loading.value = false }
}
async function loadContacts() {
  const version = generation, request = ++contactRequest
  contactsLoading.value = true; contactsError.value = ''; contacts.value = []
  try {
    const response = await getCustomerContacts(props.customerId)
    if (active(version) && request === contactRequest) contacts.value = response.data
  } catch (e) { if (active(version) && request === contactRequest) contactsError.value = detailError(e) }
  finally { if (active(version) && request === contactRequest) contactsLoading.value = false }
}
async function loadFollow(nextPage = page.value) {
  const version = generation, request = ++followRequest
  page.value = nextPage; followLoading.value = true; followError.value = ''; records.value = []
  try {
    const response = await getCustomerFollowRecords(props.customerId, nextPage, size.value)
    if (active(version) && request === followRequest) {
      records.value = response.data.records; total.value = response.data.total
      page.value = response.data.page; size.value = response.data.size
    }
  } catch (e) { if (active(version) && request === followRequest) followError.value = detailError(e) }
  finally { if (active(version) && request === followRequest) followLoading.value = false }
}
watch(() => [props.modelValue, props.customerId, canDetail.value, canContacts.value, canFollow.value], () => {
  generation++
  detail.value = null; contacts.value = []; records.value = []
  error.value = ''; contactsError.value = ''; followError.value = ''
  loading.value = false; contactsLoading.value = false; followLoading.value = false
  page.value = 1; size.value = 10; total.value = 0
  if (!props.modelValue || !props.customerId) return
  if (canDetail.value) void loadDetail()
  if (canDetail.value && canContacts.value) void loadContacts()
  if (canDetail.value && canFollow.value) void loadFollow(1)
}, { immediate: true, flush: 'sync' })
const fields = computed(() => {
  const d = detail.value
  if (!d) return []
  return [
    ['客户名称', d.customerName], ['客户编号', d.customerNo],
    ['类型', CUSTOMER_TYPE_LABELS[d.customerType] || d.customerType],
    ['等级', d.customerLevel ? CUSTOMER_LEVEL_LABELS[d.customerLevel] : null],
    ['状态', CUSTOMER_STATUS_LABELS[d.status] || d.status], ['行业', d.industry],
    ['来源', d.source], ['负责人', d.ownerName], ['联系电话', d.phone], ['邮箱', d.email],
    ['省份', d.province], ['城市', d.city], ['地址', d.address], ['备注', d.remark],
    ['创建时间', detailDate(d.createdAt)], ['更新时间', detailDate(d.updatedAt)]
  ]
})
</script>
<template>
  <el-drawer :model-value="modelValue" title="客户详情" size="min(920px, 100vw)" @update:model-value="emit('update:modelValue', $event)">
    <div v-if="modelValue" class="detail">
      <section>
        <h3>基础信息</h3>
        <el-alert v-if="!canDetail" title="无权限查看客户详情" type="warning" :closable="false" />
        <el-skeleton v-else-if="loading" :rows="6" animated />
        <div v-else-if="error"><el-alert :title="error" type="error" :closable="false" /><el-button @click="loadDetail">重试</el-button></div>
        <el-descriptions v-else-if="detail" :column="1" border>
          <el-descriptions-item v-for="[label, value] in fields" :key="label || ''" :label="label || ''"><span class="value">{{ value || '-' }}</span></el-descriptions-item>
        </el-descriptions>
      </section>
      <section>
        <h3>联系人</h3>
        <el-alert v-if="!canDetail || !canContacts" title="无权限查看联系人" type="warning" :closable="false" />
        <el-skeleton v-else-if="contactsLoading" :rows="3" animated />
        <div v-else-if="contactsError"><el-alert :title="contactsError" type="error" :closable="false" /><el-button @click="loadContacts">重试</el-button></div>
        <CustomerContactList v-else :contacts="contacts" />
      </section>
      <section>
        <h3>跟进记录</h3>
        <el-alert v-if="!canDetail || !canFollow" title="无权限查看跟进记录" type="warning" :closable="false" />
        <el-skeleton v-else-if="followLoading" :rows="4" animated />
        <div v-else-if="followError"><el-alert :title="followError" type="error" :closable="false" /><el-button @click="loadFollow()">重试</el-button></div>
        <CustomerFollowRecordList v-else :records="records" :page="page" :size="size" :total="total" @page-change="loadFollow" />
      </section>
    </div>
  </el-drawer>
</template>
<style scoped>
.detail { min-width: 0; }
section { margin-bottom: 28px; }
h3 { margin: 0 0 16px; }
.value { white-space: pre-wrap; overflow-wrap: anywhere; }
.el-button { margin-top: 10px; }
</style>
