<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserDialog from '@/components/system/user/UserDialog.vue'
import UserSearch from '@/components/system/user/UserSearch.vue'
import UserTable from '@/components/system/user/UserTable.vue'
import type {
  User,
  UserDialogMode,
  UserFormData,
  UserSearchCriteria
} from '@/types/user'

const users = ref<User[]>([
  {
    id: 1,
    username: 'admin',
    name: '系统管理员',
    role: '管理员',
    phone: '13800000000',
    status: '正常',
    createTime: '2026-08-03'
  },
  {
    id: 2,
    username: 'zhangsan',
    name: '张三',
    role: '销售人员',
    phone: '13900000000',
    status: '正常',
    createTime: '2026-08-03'
  },
  {
    id: 3,
    username: 'lisi',
    name: '李四',
    role: '销售经理',
    phone: '13700000000',
    status: '正常',
    createTime: '2026-08-02'
  },
  {
    id: 4,
    username: 'wangwu',
    name: '王五',
    role: '销售人员',
    phone: '13600000000',
    status: '停用',
    createTime: '2026-08-01'
  },
  {
    id: 5,
    username: 'zhaoliu',
    name: '赵六',
    role: '销售人员',
    phone: '13500000000',
    status: '正常',
    createTime: '2026-07-31'
  },
  {
    id: 6,
    username: 'sunqi',
    name: '孙七',
    role: '销售经理',
    phone: '13400000000',
    status: '正常',
    createTime: '2026-07-30'
  }
])

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
    const matchesRole = !searchCriteria.value.role || user.role === searchCriteria.value.role
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
    await ElMessageBox.confirm(`确定删除用户“${user.username}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    users.value = users.value.filter((item) => item.id !== user.id)

    const maxPage = Math.max(1, Math.ceil(filteredUsers.value.length / pageSize.value))
    currentPage.value = Math.min(currentPage.value, maxPage)
    ElMessage.success('删除成功')
  } catch {
    // 用户取消删除时保持当前数据不变。
  }
}

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const handleSave = (data: UserFormData) => {
  if (dialogMode.value === 'create') {
    const usernameExists = users.value.some((user) => user.username === data.username)
    if (usernameExists) {
      ElMessage.warning('用户名已存在')
      return
    }

    const nextId = users.value.reduce((maxId, user) => Math.max(maxId, user.id), 0) + 1
    users.value.unshift({
      id: nextId,
      username: data.username,
      name: data.name,
      role: data.role,
      phone: data.phone,
      status: data.status,
      createTime: formatDate(new Date())
    })
    currentPage.value = 1
    ElMessage.success('新增成功')
  } else if (data.id) {
    const index = users.value.findIndex((user) => user.id === data.id)
    if (index >= 0) {
      users.value[index] = {
        ...users.value[index],
        username: data.username,
        name: data.name,
        role: data.role,
        phone: data.phone,
        status: data.status
      }
      ElMessage.success('编辑成功')
    }
  }

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
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- 用户操作工具栏-新建用户 -->
    <div class="user-page__toolbar">
      <el-button type="primary" @click="handleCreate">新建用户</el-button>
    </div>

    <!-- 用户表格组件 -->
    <UserTable :users="paginatedUsers" @edit="handleEdit" @delete="handleDelete" />

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
      @save="handleSave"
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
