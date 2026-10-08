<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createCustomer } from '@/api/customer'
import {
  CUSTOMER_INDUSTRY_OPTIONS,
  CUSTOMER_LEVEL_OPTIONS,
  CUSTOMER_SOURCE_OPTIONS,
  CUSTOMER_STATUS_OPTIONS,
  CUSTOMER_TYPE_OPTIONS
} from '@/types/customer'
import type {
  CreateCustomerRequest,
  CustomerCreateResult,
  CustomerFormData,
  CustomerLevel,
  CustomerStatus,
  CustomerType
} from '@/types/customer'
import type { ApiProblemDetail } from '@/utils/request'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [result: CustomerCreateResult]
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const createInitialForm = (): CustomerFormData => ({
  customerName: '',
  customerType: 'ENTERPRISE',
  customerLevel: '',
  industry: '',
  source: '',
  phone: '',
  email: '',
  province: '',
  city: '',
  address: '',
  status: 'POTENTIAL',
  remark: ''
})

const form = reactive<CustomerFormData>(createInitialForm())

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const allowedTypes: CustomerType[] = ['ENTERPRISE', 'INDIVIDUAL']
const allowedLevels: CustomerLevel[] = ['A', 'B', 'C', 'D']
const allowedStatuses: CustomerStatus[] = ['POTENTIAL', 'ACTIVE', 'INACTIVE']

const rules: FormRules<CustomerFormData> = {
  customerName: [
    { required: true, message: '请输入客户名称', trigger: 'blur' },
    { max: 200, message: '客户名称不能超过 200 个字符', trigger: 'blur' }
  ],
  customerType: [
    { required: true, message: '请选择客户类型', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        allowedTypes.includes(value) ? callback() : callback(new Error('客户类型不合法'))
      },
      trigger: 'change'
    }
  ],
  customerLevel: [{
    validator: (_rule, value, callback) => {
      !value || allowedLevels.includes(value) ? callback() : callback(new Error('客户等级不合法'))
    },
    trigger: 'change'
  }],
  industry: [{ max: 64, message: '所属行业不能超过 64 个字符', trigger: 'blur' }],
  source: [{ max: 64, message: '客户来源不能超过 64 个字符', trigger: 'blur' }],
  phone: [{ max: 32, message: '联系电话不能超过 32 个字符', trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
    { max: 128, message: '邮箱不能超过 128 个字符', trigger: 'blur' }
  ],
  province: [{ max: 64, message: '省份不能超过 64 个字符', trigger: 'blur' }],
  city: [{ max: 64, message: '城市不能超过 64 个字符', trigger: 'blur' }],
  address: [{ max: 500, message: '详细地址不能超过 500 个字符', trigger: 'blur' }],
  status: [
    { required: true, message: '请选择客户状态', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        allowedStatuses.includes(value) ? callback() : callback(new Error('客户状态不合法'))
      },
      trigger: 'change'
    }
  ],
  remark: [{ max: 1000, message: '备注不能超过 1000 个字符', trigger: 'blur' }]
}

const optionalText = (value: string): string | undefined => value.trim() || undefined

const buildRequest = (): CreateCustomerRequest => ({
  customerName: form.customerName.trim(),
  customerType: form.customerType,
  customerLevel: form.customerLevel || undefined,
  industry: optionalText(form.industry),
  source: optionalText(form.source),
  phone: optionalText(form.phone),
  email: optionalText(form.email),
  province: optionalText(form.province),
  city: optionalText(form.city),
  address: optionalText(form.address),
  status: form.status,
  remark: optionalText(form.remark)
})

const getErrorDetail = (error: unknown): string | undefined =>
  axios.isAxiosError<ApiProblemDetail>(error) ? error.response?.data?.detail : undefined

const resetForm = () => {
  Object.assign(form, createInitialForm())
  void nextTick(() => formRef.value?.clearValidate())
}

const handleCancel = () => {
  if (!submitting.value) visible.value = false
}

const handleBeforeClose = (done: () => void) => {
  if (!submitting.value) done()
}

const handleSubmit = async () => {
  if (!formRef.value || submitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const response = await createCustomer(buildRequest())
    ElMessage.success('客户创建成功')
    visible.value = false
    emit('success', response.data)
  } catch (error: unknown) {
    ElMessage.error(getErrorDetail(error) || '客户创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="新建客户"
    width="min(760px, calc(100vw - 32px))"
    destroy-on-close
    :close-on-click-modal="!submitting"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    :before-close="handleBeforeClose"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <section class="form-section">
        <h3>基本信息</h3>
        <div class="form-grid">
          <el-form-item class="form-grid__full" label="客户名称" prop="customerName">
            <el-input v-model="form.customerName" maxlength="200" placeholder="请输入客户名称" />
          </el-form-item>
          <el-form-item label="客户类型" prop="customerType">
            <el-select v-model="form.customerType" placeholder="请选择客户类型">
              <el-option
                v-for="option in CUSTOMER_TYPE_OPTIONS"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="客户等级" prop="customerLevel">
            <el-select v-model="form.customerLevel" clearable placeholder="请选择客户等级">
              <el-option
                v-for="option in CUSTOMER_LEVEL_OPTIONS"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="所属行业" prop="industry">
            <el-select
              v-model="form.industry"
              clearable
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入所属行业"
            >
              <el-option v-for="item in CUSTOMER_INDUSTRY_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="客户来源" prop="source">
            <el-select
              v-model="form.source"
              clearable
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入客户来源"
            >
              <el-option v-for="item in CUSTOMER_SOURCE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="客户状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择客户状态">
              <el-option
                v-for="option in CUSTOMER_STATUS_OPTIONS"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <h3>联系信息</h3>
        <div class="form-grid">
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" maxlength="32" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" maxlength="128" placeholder="请输入邮箱" />
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <h3>地址与备注</h3>
        <div class="form-grid">
          <el-form-item label="省份" prop="province">
            <el-input v-model="form.province" maxlength="64" placeholder="请输入省份" />
          </el-form-item>
          <el-form-item label="城市" prop="city">
            <el-input v-model="form.city" maxlength="64" placeholder="请输入城市" />
          </el-form-item>
          <el-form-item class="form-grid__full" label="详细地址" prop="address">
            <el-input v-model="form.address" maxlength="500" placeholder="请输入详细地址" />
          </el-form-item>
          <el-form-item class="form-grid__full" label="备注" prop="remark">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="请输入备注"
            />
          </el-form-item>
        </div>
      </section>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-section + .form-section {
  margin-top: 18px;
}

.form-section h3 {
  margin: 0 0 16px;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary);
  color: #344054;
  font-size: 15px;
  font-weight: 600;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.form-grid__full {
  grid-column: 1 / -1;
}

.form-grid :deep(.el-select) {
  width: 100%;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid__full {
    grid-column: auto;
  }
}
</style>
