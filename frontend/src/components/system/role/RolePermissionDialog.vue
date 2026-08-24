<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Role } from '@/types/role'
import { getPermissionTree } from '@/api/permission';
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
  save: [permissionCodes: string[]]
}>()

const treeRef = ref<{ getCheckedKeys: () => Array<string | number> }>()
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})


const permissionTree = ref<PermissionTreeNode[]>([])
const loading = ref(false)

const treeProps = {
  children: 'children',
  label: 'permissionName'
}

const loadPermissionTree = async () => {
  loading.value = true

  try {
    const response = await getPermissionTree()
    permissionTree.value = response.data
  } catch (error: unknown) {
    permissionTree.value = []

    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || '权限树加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}



watch(
  () => props.modelValue,
  async(opened) => {
    if (!opened) return

    await loadPermissionTree()
    // TODO: 后续加载当前角色已有权限并通过 setCheckedKeys 回显。
  }
)

const handleSave = () => {
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  emit('save', checkedKeys.map(String).filter((key) => key.includes(':')))
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
      />
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存权限</el-button>
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
