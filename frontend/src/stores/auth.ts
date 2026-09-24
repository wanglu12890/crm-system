import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, type CurrentUser } from '@/api/auth'

// 基于 Pinia Setup Store 的认证状态模块，负责保存当前用户、加载/清空用户信息，并提供基于角色和权限的便捷判断方法，常用于路由守卫和按钮级权限控制。
export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<CurrentUser | null>(null)

  const loadCurrentUser = async (): Promise<CurrentUser> => {
    try {
      const response = await getCurrentUser()
      currentUser.value = response.data
      return response.data
    } catch (error) {
      clearUser()
      throw error
    }
  }

  const clearUser = (): void => {
    currentUser.value = null
  }

  const hasRole = (roleCode: string): boolean =>
    currentUser.value?.roles.includes(roleCode) ?? false

  const hasPermission = (permissionCode: string): boolean =>
    currentUser.value?.permissions.includes(permissionCode) ?? false

  return {
    currentUser,
    loadCurrentUser,
    clearUser,
    hasRole,
    hasPermission
  }
})
