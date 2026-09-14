<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { TreeInstance } from 'element-plus'
import type { Role } from '@/types/role'
import { getPermissionTree } from '@/api/permission';
import { getRolePermissionIds, updateRolePermissions } from '@/api/role'
import type { PermissionTreeNode} from '@/types/permission'
import axios from 'axios';
import { ApiProblemDetail } from '@/utils/request';
import { ElMessage } from 'element-plus';


const props = defineProps<{
  modelValue: boolean
  role: Role | null
}>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  success: []
}>()

const treeRef = ref<TreeInstance>()
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})


const permissionTree = ref<PermissionTreeNode[]>([])
const loading = ref(false)
const saving = ref(false)
const initialPermissionIds = ref<string[]>([])
const currentPermissionIds = ref<string[]>([])

const isSamePermissionSet = (left: string[], right: string[]) => {
  const leftSet = new Set(left)
  const rightSet = new Set(right)
  return leftSet.size === rightSet.size && [...leftSet].every((id) => rightSet.has(id))
}

const hasPermissionChanged = computed(
  () => !isSamePermissionSet(initialPermissionIds.value, currentPermissionIds.value)
)

const treeProps = {
  children: 'children',
  label: 'permissionName'
}

const loadPermissionState = async () => {
  permissionTree.value = []
  treeRef.value?.setCheckedKeys([])
  initialPermissionIds.value = []
  currentPermissionIds.value = []
  if (!props.role?.id) return
  loading.value = true

  try {
    const permissionTreeResponse = await getPermissionTree()
    permissionTree.value = permissionTreeResponse.data

    const rolePermissionResponse = await getRolePermissionIds(props.role.id)
    await nextTick()
    treeRef.value?.setCheckedKeys(rolePermissionResponse.data)
    await nextTick()
    const checkedIds = (treeRef.value?.getCheckedKeys() || []).map(String)
    initialPermissionIds.value = [...checkedIds]
    currentPermissionIds.value = [...checkedIds]
  } catch (error: unknown) {
    permissionTree.value = []

    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || '角色权限加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}



watch(
  () => [props.modelValue, props.role?.id] as const,
  async([opened]) => {
    if (!opened) {
      initialPermissionIds.value = []
      currentPermissionIds.value = []
      return
    }

    await loadPermissionState()
  }
)

const handlePermissionCheck = () => {
  currentPermissionIds.value = (treeRef.value?.getCheckedKeys() || []).map(String)
}

const handleSave = async () => {
  if (!props.role?.id || saving.value || !hasPermissionChanged.value) return
  const permissionIds = [...currentPermissionIds.value]
  saving.value = true
  try {
    await updateRolePermissions(props.role.id, { permissionIds })
    ElMessage.success('权限保存成功')
    visible.value = false
    emit('success')
  } catch (error: unknown) {
    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || '权限保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="配置权限"
    width="min(620px, calc(100vw - 32px))"
    destroy-on-close
    append-to-body
  >
    <div class="permission-dialog__summary">
      当前角色：<strong>{{ role?.roleName || '未选择角色' }}</strong>
    </div>
    <div 
      class="permission-dialog__tree"
      v-loading="loading"
    >
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="treeProps"
        @check="handlePermissionCheck"
      />
    </div>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="loading || !hasPermissionChanged || saving"
        @click="handleSave"
      >
        保存权限
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.permission-dialog__summary {
  margin-bottom: 14px;
  color: #475467;
  font-size: 14px;
}

.permission-dialog__tree {
  max-height: 420px;
  padding: 12px 14px;
  border: 1px solid #e5eaf2;
  border-radius: 8px;
  overflow-y: auto;
  background: #f8fafc;
}
</style>
