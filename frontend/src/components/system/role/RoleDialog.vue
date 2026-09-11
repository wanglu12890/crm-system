<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import axios from 'axios'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createRole } from '@/api/role'
import type { ApiProblemDetail } from '@/utils/request'
import type { Role, RoleDialogMode, RoleFormData } from '@/types/role'

const props = defineProps<{
  modelValue: boolean
  mode: RoleDialogMode
  role: Role | null
}>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  save: [data: RoleFormData]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const createEmptyForm = (): RoleFormData => ({
  roleName: '',
  roleCode: '',
  status: 1,
  remark: ''
})
const form = reactive<RoleFormData>(createEmptyForm())

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const title = computed(() => (props.mode === 'create' ? '新建角色' : '编辑角色'))

const rules: FormRules<RoleFormData> = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 64, message: '角色名称不能超过 64 个字符', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '请输入大写字母、数字或下划线', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

watch(
  () => props.modelValue,
  (opened) => {
    if (!opened) return
    Object.assign(
      form,
      props.mode === 'edit' && props.role
        ? {
            id: props.role.id,
            roleName: props.role.roleName,
            roleCode: props.role.roleCode,
            status: props.role.status,
            remark: props.role.remark || ''
          }
        : createEmptyForm()
    )
    formRef.value?.clearValidate()
  }
)

const handleSave = async () => {
  if (!formRef.value || submitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (props.mode === 'edit') {
    emit('save', { ...form })
    return
  }

  submitting.value = true
  try {
    await createRole({
      roleName: form.roleName,
      roleCode: form.roleCode,
      status: form.status,
      remark: form.remark
    })
    ElMessage.success('角色创建成功')
    visible.value = false
    emit('success')
  } catch (error: unknown) {
    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || '角色创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
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
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model.trim="form.roleName" placeholder="例如：销售经理" maxlength="64" />
      </el-form-item>

      <el-form-item label="角色编码" prop="roleCode">
        <el-input
          v-model.trim="form.roleCode"
          placeholder="例如：SALES_MANAGER"
          maxlength="64"
        />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input
          v-model.trim="form.remark"
          type="textarea"
          :rows="3"
          placeholder="角色用途说明"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>
