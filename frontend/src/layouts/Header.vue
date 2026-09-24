<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ArrowDown, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { removeAuthToken } from '@/utils/auth'

type UserMenuCommand = 'profile' | 'password' | 'logout'

const router = useRouter()
const authStore = useAuthStore()
const { currentUser } = storeToRefs(authStore)

const displayName = computed(() =>
  currentUser.value?.realName?.trim() || currentUser.value?.username || '用户'
)

const handleUserCommand = async (command: UserMenuCommand) => {
  if (command === 'profile') {
    ElMessage.info('个人信息功能暂未实现')
    return
  }

  if (command === 'password') {
    ElMessage.info('修改密码功能暂未实现')
    return
  }

  removeAuthToken()
  authStore.clearUser()
  await router.replace('/login')
}

</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <div class="brand-logo-wrapper">
        <img src="@/assets/images/yuyan_crm_logo.svg" alt="雨燕科技CRM系统" />
      </div>
      <span class="app-header__name">雨燕科技CRM系统</span>
    </div>

    <el-dropdown
      trigger="click"
      placement="bottom-end"
      @command="handleUserCommand"
    >
      <button class="user-trigger" type="button" :aria-label="`${displayName}用户菜单`">
        <el-avatar :size="34" :icon="UserFilled" class="user-trigger__avatar" />
        <span class="user-trigger__name">{{ displayName }}</span>
        <el-icon class="user-trigger__arrow"><ArrowDown /></el-icon>
      </button>

      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item disabled class="user-menu__username">
            {{ currentUser?.username || '未知用户' }}
          </el-dropdown-item>
          <el-dropdown-item divided command="profile">个人信息</el-dropdown-item>
          <el-dropdown-item command="password">修改密码</el-dropdown-item>
          <el-dropdown-item command="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid #e5eaf2;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgb(15 23 42 / 4%);
}

.app-header__brand {
  display: flex;
  min-width: 0;
  gap: 12px;
  align-items: center;
}

.app-header__name {
  overflow: hidden;
  color: #172033;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.brand-logo-wrapper {
  display: inline-flex;
  width: 54px;
  height: 54px;
  padding: 2px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
}

.brand-logo-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.user-trigger {
  display: inline-flex;
  min-height: 44px;
  padding: 5px 10px;
  border: 0;
  border-radius: 8px;
  align-items: center;
  gap: 9px;
  color: #344054;
  font: inherit;
  background: transparent;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-trigger:hover,
.user-trigger:focus-visible {
  background: #f2f4f7;
  outline: none;
}

.user-trigger__avatar {
  color: #fff;
  background: #2563eb;
}

.user-trigger__name {
  max-width: 160px;
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.user-trigger__arrow {
  color: #98a2b3;
  font-size: 12px;
}

:global(.user-menu__username.is-disabled) {
  color: #344054;
  font-weight: 600;
  cursor: default;
  opacity: 1;
}

@media (max-width: 640px) {
  .app-header {
    height: 56px;
    padding: 0 12px;
  }

  .app-header__name {
    max-width: 150px;
    font-size: 16px;
  }

  .brand-logo-wrapper {
    width: 46px;
    height: 46px;
  }

  .user-trigger__name {
    display: none;
  }
}
</style>
