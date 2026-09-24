<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import RoleDialog from '@/components/system/role/RoleDialog.vue'
import RolePermissionDialog from '@/components/system/role/RolePermissionDialog.vue'
import RoleSearch from '@/components/system/role/RoleSearch.vue'
import RoleTable from '@/components/system/role/RoleTable.vue'
import { getRoleList } from '@/api/role'
import type {
  Role,
  RoleDialogMode,
  RoleFormData,
  RoleSearchCriteria
} from '@/types/role'
import axios from 'axios'
import { ApiProblemDetail } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const canCreateRole = computed(
  () => authStore.hasRole('SUPER_ADMIN') && authStore.hasPermission('role:create')
)

const roles = ref<Role[]>([])
const loading = ref(false)
const loadRoles = async () => {
  loading.value = true
  try{
    const response = await getRoleList()
    roles.value = response.data    
  }catch(error: unknown){
    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || '加载角色列表失败,请稍后重试')
    roles.value = []
  }finally{
    loading.value = false
  }
}
onMounted(loadRoles)

const searchCriteria = ref<RoleSearchCriteria>({
  roleName: '',
  roleCode: '',
  status: ''
})
const currentPage = ref(1)
const pageSize = ref(5)

const roleDialogVisible = ref(false)
const roleDialogMode = ref<RoleDialogMode>('create')
const editingRole = ref<Role | null>(null)
const permissionDialogVisible = ref(false)
const permissionRole = ref<Role | null>(null)

const filteredRoles = computed(() => {
  const name = searchCriteria.value.roleName.toLowerCase()
  const code = searchCriteria.value.roleCode.toLowerCase()
  return roles.value.filter((role) => {
    const matchesName = !name || role.roleName.toLowerCase().includes(name)
    const matchesCode = !code || role.roleCode.toLowerCase().includes(code)
    const matchesStatus =
      searchCriteria.value.status === '' || role.status === searchCriteria.value.status
    return matchesName && matchesCode && matchesStatus
  })
})

const paginatedRoles = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRoles.value.slice(start, start + pageSize.value)
})

const handleSearch = (criteria: RoleSearchCriteria) => {
  searchCriteria.value = criteria
  currentPage.value = 1
}

const handleReset = () => {
  searchCriteria.value = { roleName: '', roleCode: '', status: '' }
  currentPage.value = 1
}

const handleCreateRole = () => {
  roleDialogMode.value = 'create'
  editingRole.value = null
  roleDialogVisible.value = true
}

const handleEditRole = (role: Role) => {
  roleDialogMode.value = 'edit'
  editingRole.value = { ...role }
  roleDialogVisible.value = true
}

const handleSaveRole = (_data: RoleFormData) => {
  // TODO: 后续分别接入角色新增和编辑接口。
  ElMessage.info(
    roleDialogMode.value === 'create'
      ? '角色新增功能待后端接口完成后接入'
      : '角色编辑功能待后端接口完成后接入'
  )
  roleDialogVisible.value = false
}

const handleConfigurePermission = (role: Role) => {
  permissionRole.value = role
  permissionDialogVisible.value = true
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}
</script>

<template>
  <section class="role-page">
    <header class="role-page__header">
      <div>
        <h1>角色管理</h1>
        <p>管理系统角色、账号状态与功能权限</p>
      </div>
    </header>

    <RoleSearch
      :model-value="searchCriteria"
      @search="handleSearch"
      @reset="handleReset"
    />

    <div v-if="canCreateRole" class="role-page__toolbar">
      <el-button type="primary" @click="handleCreateRole">+ 新建角色</el-button>
    </div>

    <RoleTable
      :roles="paginatedRoles"
      @edit="handleEditRole"
      @configure-permission="handleConfigurePermission"
    />

    <div class="role-page__pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[5, 10, 20]"
        :total="filteredRoles.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
      />
    </div>

    <RoleDialog
      v-model="roleDialogVisible"
      :mode="roleDialogMode"
      :role="editingRole"
      @save="handleSaveRole"
      @success="loadRoles"
    />

    <RolePermissionDialog
      v-model="permissionDialogVisible"
      :role="permissionRole"
      @success="loadRoles"
    />
  </section>
</template>

<style scoped>
.role-page {
  padding: 24px;
  border: 1px solid #eaecf0;
  border-radius: 10px;
  background: #fff;
}

.role-page__header {
  margin-bottom: 22px;
}

.role-page__header h1 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  font-weight: 600;
}

.role-page__header p {
  margin: 7px 0 0;
  color: #667085;
  font-size: 14px;
}

.role-page__toolbar {
  display: flex;
  margin: 20px 0 14px;
  justify-content: flex-end;
}

.role-page__pagination {
  display: flex;
  margin-top: 20px;
  justify-content: flex-end;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .role-page {
    padding: 16px;
  }

  .role-page__pagination {
    justify-content: flex-start;
  }
}
</style>
