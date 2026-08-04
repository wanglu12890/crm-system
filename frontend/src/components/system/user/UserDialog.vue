<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { User, UserDialogMode, UserFormData } from '@/types/user'

const props = defineProps<{
  modelValue: boolean
  mode: UserDialogMode
  user: User | null
}>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  save: [data: UserFormData]
}>()

const formRef = ref<FormInstance>()

const createEmptyForm = (): UserFormData => ({
  username: '',
  name: '',
  password: '',
  role: '',
  phone: '',
  status: '正常'
})

const form = reactive<UserFormData>(createEmptyForm())

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const title = computed(() => (props.mode === 'create' ? '新增用户' : '编辑用户'))

const validatePassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (props.mode === 'create' && !value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value && (value.length < 6 || value.length > 64)) {
    callback(new Error('密码长度应为 6 到 64 个字符'))
    return
  }
  callback()
}

const rules: FormRules<UserFormData> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 64, message: '用户名长度应为 2 到 64 个字符', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

watch(
  () => props.modelValue,
  (opened) => {
    if (!opened) return

    Object.assign(
      form,
      props.mode === 'edit' && props.user
        ? {
            id: props.user.id,
            username: props.user.username,
            name: props.user.name,
            password: '',
            role: props.user.role,
            phone: props.user.phone,
            status: props.user.status
          }
        : createEmptyForm()
    )
    formRef.value?.clearValidate()
  }
)

const handleSave = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  emit('save', { ...form })
}

const handleClosed = () => {
  formRef.value?.resetFields()
  Object.assign(form, createEmptyForm())
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="min(520px, calc(100vw - 32px))"
    destroy-on-close
    append-to-body
    @closed="handleClosed"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="form.username" placeholder="请输入用户名" maxlength="64" />
      </el-form-item>

      <el-form-item label="真实姓名" prop="name">
        <el-input v-model.trim="form.name" placeholder="请输入真实姓名" maxlength="64" />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          :placeholder="mode === 'create' ? '请输入密码' : '留空表示不修改密码'"
          show-password
          maxlength="64"
          autocomplete="new-password"
        />
      </el-form-item>

      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" placeholder="请选择角色" class="user-dialog__select">
          <el-option label="管理员" value="管理员" />
          <el-option label="销售经理" value="销售经理" />
          <el-option label="销售人员" value="销售人员" />
        </el-select>
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model.trim="form.phone" placeholder="请输入手机号" maxlength="11" />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" class="user-dialog__select">
          <el-option label="正常" value="正常" />
          <el-option label="停用" value="停用" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.user-dialog__select {
  width: 100%;
}
</style>
