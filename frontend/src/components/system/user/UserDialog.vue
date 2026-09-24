<!-- 用于管理系统用户的对话框组件-新建用户or编辑 -->
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { User, UserDialogMode, UserFormData } from '@/types/user'
import type { Role } from '@/types/role'
import { createUser } from '@/api/user'
import axios from 'axios'
import type { ApiProblemDetail } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  modelValue: boolean
  mode: UserDialogMode
  user: User | null
  roles: Role[]
}>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  success: []
}>()

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const createEmptyForm = (): UserFormData => ({
  username: '',
  name: '',
  password: '',
  roleIds: [],
  phone: '',
  status: '正常'
})


const form = reactive<UserFormData>(createEmptyForm())

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const title = computed(() => (props.mode === 'create' ? '新增用户' : '编辑用户'))

const assignableRoles = computed(() => {
  const enabledRoles = props.roles.filter((role) => role.status === 1)

  // 编辑接口和对象范围授权尚未实现，保持现有编辑占位流程不变。
  if (props.mode !== 'create') return enabledRoles

  if (authStore.hasRole('SUPER_ADMIN')) return enabledRoles

  if (authStore.hasRole('SYSTEM_ADMIN')) {
    return enabledRoles.filter(
      (role) => role.roleCode !== 'SUPER_ADMIN' && role.roleCode !== 'SYSTEM_ADMIN'
    )
  }

  return []
})

// 定义一个函数 validatePassword，用于验证密码的合法性。
// 如果是新建用户模式，则密码不能为空；如果密码不为空，则长度必须在 6 到 64 个字符之间。
const validatePassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (props.mode === 'create' && !value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value && (value.length < 6 || value.length > 64)) {
    callback(new Error('密码长度应为 6 到 64 个字符'))
    return
  }
  callback() // 验证通过，调用 callback() 表示验证成功
}

// 定义表单验证规则 rules，用于对用户表单数据进行验证。
const rules: FormRules<UserFormData> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 64, message: '用户名长度应为 2 到 64 个字符', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  roleIds: [
    { type: 'array', required: true, min: 1, message: '请至少选择一个角色', trigger: 'change' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 监听 props.modelValue 的变化，当对话框打开时，初始化表单数据。
watch(
  () => props.modelValue, // 监听对话框的可见性变化
  async (opened) => {  // 当对话框打开时，初始化表单数据
    if (!opened) return  // 如果对话框关闭，则不做任何操作
    // 如果是编辑模式并且有用户数据，则将表单数据设置为当前用户的数据；否则，创建一个空的表单数据对象。
    Object.assign(
      form,
      props.mode === 'edit' && props.user  
        ? {
            id: props.user.id,
            username: props.user.username,
            name: props.user.name,
            password: '',
            roleIds: [...props.user.roleIds],
            phone: props.user.phone,
            status: props.user.status
          }
        : createEmptyForm()
    )
    formRef.value?.clearValidate() // 清除表单验证状态
  }
)

// 定义一个函数 handleSave，用于处理保存按钮的点击事件。
const handleSave = async () => {
  if (!formRef.value || submitting.value) return
  // 调用表单的 validate 方法进行验证，如果验证失败，则直接返回。
  const valid = await formRef.value.validate().catch(() => false)
  // 如果验证失败，则直接返回。
  if (!valid) return

  if (props.mode === 'edit') {
    ElMessage.warning('编辑用户接口尚未实现')
    return
  }

  submitting.value = true

  try {
    
      await createUser({
      username: form.username,
      realName: form.name,
      password: form.password,
      roleIds: [...form.roleIds],
      phone: form.phone || undefined,
      status: form.status === '正常' ? 1 : 0
    })
    ElMessage.success('用户创建成功')
       
    visible.value = false
    emit('success') // 触发 success 事件，通知父组件刷新用户列表
  } catch (error: unknown) {
      const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || (props.mode === 'create' ? '用户创建失败，请稍后重试' : '用户信息更新失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

// 定义一个函数 handleClosed，用于处理对话框关闭事件。
const handleClosed = () => {
  formRef.value?.resetFields() // 重置表单字段
  Object.assign(form, createEmptyForm()) // 
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

      <el-form-item label="角色" prop="roleIds">
        <el-select
          v-model="form.roleIds"
          placeholder="请选择角色"
          class="user-dialog__select"
          multiple
          collapse-tags
          collapse-tags-tooltip
        >
          <el-option
            v-for="role in assignableRoles"
            :key="role.id"
            :label="role.roleName"
            :value="role.id"
          />
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
      <!-- 点击取消按钮，visible属性值变为false，对话框关闭 -->
      <el-button @click="visible = false">取消</el-button> 
      <!-- 点击保存按钮，type值为primary，表示主要按钮；同时触发保存事件 -->
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="submitting"
        @click="handleSave"
      >保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.user-dialog__select {
  width: 100%;
}
</style>
