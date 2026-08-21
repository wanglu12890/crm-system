<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Role } from '@/types/role'

interface PermissionNode {
  id: string
  label: string
  children?: PermissionNode[]
}

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

// TODO: 后续由 GET /permissions 或角色权限接口动态加载。
const permissionTree: PermissionNode[] = [
  {
    id: 'system',
    label: '系统管理',
    children: [
      {
        id: 'user-management',
        label: '用户管理',
        children: [
          { id: 'user:list', label: '查看用户' },
          { id: 'user:create', label: '新建用户' },
          { id: 'user:update', label: '编辑用户' },
          { id: 'user:delete', label: '删除用户' },
          { id: 'user:assign_role', label: '分配用户角色' }
        ]
      },
      {
        id: 'role-management',
        label: '角色管理',
        children: [
          { id: 'role:list', label: '查看角色' },
          { id: 'role:create', label: '新建角色' },
          { id: 'role:update', label: '编辑角色' },
          { id: 'role:delete', label: '删除角色' },
          { id: 'role:assign_permission', label: '配置角色权限' }
        ]
      }
    ]
  }
]

watch(
  () => props.modelValue,
  (opened) => {
    if (!opened) return
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
    <div class="permission-dialog__tree">
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="{ label: 'label', children: 'children' }"
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
