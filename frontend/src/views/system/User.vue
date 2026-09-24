<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserDialog from '@/components/system/user/UserDialog.vue'
import UserSearch from '@/components/system/user/UserSearch.vue'
import UserTable from '@/components/system/user/UserTable.vue'
import { getUserList } from '@/api/user'
import { getRoleList } from '@/api/role'
import { ApiProblemDetail } from '@/utils/request'
import type { Role } from '@/types/role'
import type {
  User,
  UserDialogMode,
  UserFormData,
  UserSearchCriteria
} from '@/types/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const canCreateUser = computed(
  () => authStore.hasPermission('user:create')
    && authStore.hasPermission('user:assign_role')
)

const users = ref<User[]>([])
const roles = ref<Role[]>([])
const loading = ref(false)
const loadUsers = async () => {
  loading.value = true
  try {
    const response = await getUserList()
    users.value = response.data
  } catch (error: unknown) {
    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || '用户列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
onMounted(loadUsers)

const loadRoles = async () => {
  try {
    const response = await getRoleList()
    roles.value = response.data
  } catch (error: unknown) {
    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    ElMessage.error(detail || '角色列表加载失败，请稍后重试')
    roles.value = []
  }
}
onMounted(loadRoles)

// 定义搜索条件的响应式对象，初始值为空
const searchCriteria = ref<UserSearchCriteria>({
  username: '',
  role: '',
  status: ''
})

// 定义分页和对话框相关的响应式数据，ref(1) 表示当前页码，ref(5) 表示每页显示的条数。
const currentPage = ref(1)
const pageSize = ref(5)

// 定义对话框的可见性、模式和正在编辑的用户对象，ref(false) 表示对话框初始为不可见
const dialogVisible = ref(false)

// 定义对话框模式，初始为 'create'，表示创建用户
const dialogMode = ref<UserDialogMode>('create')

// 定义正在编辑的用户对象，初始为 null，表示没有正在编辑的用户
const editingUser = ref<User | null>(null)

const filteredUsers = computed(() => {
  const keyword = searchCriteria.value.username.toLowerCase()
  return users.value.filter((user) => {
    const matchesUsername = !keyword || user.username.toLowerCase().includes(keyword)
    const matchesRole =
      !searchCriteria.value.role || user.roleIds.includes(searchCriteria.value.role)
    const matchesStatus =
      !searchCriteria.value.status || user.status === searchCriteria.value.status
    return matchesUsername && matchesRole && matchesStatus
  })
})

const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredUsers.value.slice(start, start + pageSize.value)
})

// criteria: UserSearchCriteria 是一个类型注解，表示 handleSearch 函数的参数 criteria 的类型为 UserSearchCriteria。这样可以确保传入的参数符合 UserSearchCriteria 接口的结构要求。
const handleSearch = (criteria: UserSearchCriteria) => {
  searchCriteria.value = criteria
  currentPage.value = 1
}

const handleReset = () => {
  searchCriteria.value = { username: '', role: '', status: '' }
  currentPage.value = 1
}

const handleCreate = () => {
  dialogMode.value = 'create'
  editingUser.value = null
  dialogVisible.value = true
}

const handleEdit = (user: User) => {
  dialogMode.value = 'edit'
  editingUser.value = { ...user }
  dialogVisible.value = true
}

const handleDelete = async (user: User) => {
  try {
    // await ElMessageBox.confirm(`确定删除用户“${user.username}”吗？`, '删除确认', {
    //   type: 'warning',
    //   confirmButtonText: '确定',
    //   cancelButtonText: '取消'
    // })
    // users.value = users.value.filter((item) => item.id !== user.id)

    // const maxPage = Math.max(1, Math.ceil(filteredUsers.value.length / pageSize.value))
    // currentPage.value = Math.min(currentPage.value, maxPage)
    // ElMessage.success('删除成功')
  } catch {
    // 用户取消删除时保持当前数据不变。
  }
}

const handleSuccess = async () => {
  await loadUsers() // 重新加载用户列表以获取最新数据
  dialogVisible.value = false
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}
</script>

<template>
  <section class="user-page">
    <header class="user-page__header">
      <div>
        <h1>用户管理</h1>
        <p>管理系统用户、角色与账号状态</p>
      </div>
    </header>

    <!-- 用户搜索组件 -->
    <UserSearch
      :model-value="searchCriteria"
      :roles="roles"
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- 用户操作工具栏-新建用户 -->
    <div v-if="canCreateUser" class="user-page__toolbar">
      <el-button type="primary" @click="handleCreate">新建用户</el-button>
    </div>

    <!-- 用户表格组件 -->
    <UserTable
      :users="paginatedUsers"
      :roles="roles"
      @edit="handleEdit"
      @delete="handleDelete"
    />

    <!-- 用户分页组件 -->
    <div class="user-page__pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[5, 10, 20]"
        :total="filteredUsers.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 用户对话框组件 -->
    <UserDialog
      v-model="dialogVisible"
      :mode="dialogMode"
      :user="editingUser"
      :roles="roles"
      @success="handleSuccess"     
    />
  </section>
</template>

<style scoped>
.user-page {
  padding: 24px;
  border: 1px solid #eaecf0;
  border-radius: 10px;
  background: #fff;
}

.user-page__header {
  margin-bottom: 22px;
}

.user-page__header h1 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  font-weight: 600;
}

.user-page__header p {
  margin: 7px 0 0;
  color: #667085;
  font-size: 14px;
}

.user-page__toolbar {
  display: flex;
  margin: 20px 0 14px;
  justify-content: flex-end;
}

.user-page__pagination {
  display: flex;
  margin-top: 20px;
  justify-content: flex-end;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .user-page {
    padding: 16px;
  }

  .user-page__pagination {
    justify-content: flex-start;
  }
}
</style>
