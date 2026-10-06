<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import axios from 'axios'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { resetUserPassword } from '@/api/user'
import type { User } from '@/types/user'
import type { ApiProblemDetail } from '@/utils/request'

const props = defineProps<{
  modelValue: boolean
  user: User | null
}>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
}>()

interface ResetPasswordForm {
  newPassword: string
  confirmPassword: string
}

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<ResetPasswordForm>({ newPassword: '', confirmPassword: '' })

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules<ResetPasswordForm> = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度应为 6 到 64 个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const resetForm = () => {
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  (opened) => {
    if (opened) resetForm()
  }
)

const handleSubmit = async () => {
  if (!props.user || !formRef.value || submitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await resetUserPassword(props.user.id, { newPassword: form.newPassword })
    ElMessage.success('密码重置成功')
    visible.value = false
  } catch (error: unknown) {
    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || '密码重置失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="重置密码"
    width="min(480px, calc(100vw - 32px))"
    destroy-on-close
    append-to-body
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <el-form-item label="用户">
        <span>{{ user?.name }}（{{ user?.username }}）</span>
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          maxlength="64"
          autocomplete="new-password"
          placeholder="请输入 6 到 64 位新密码"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          maxlength="64"
          autocomplete="new-password"
          placeholder="请再次输入新密码"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="submitting" @click="handleSubmit">
        确认重置
      </el-button>
    </template>
  </el-dialog>
</template>
