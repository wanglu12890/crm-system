import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, type CurrentUser } from '@/api/auth'

// 基于 Pinia Setup Store 的认证状态模块，负责保存当前用户、加载/清空用户信息，并提供基于角色和权限的便捷判断方法，常用于路由守卫和按钮级权限控制。
export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<CurrentUser | null>(null)
  const initialized = ref(false)
  let loadingPromise: Promise<CurrentUser> | null = null

  const clearUser = (): void => {
    currentUser.value = null
    initialized.value = false
  }

  const loadCurrentUser = async (): Promise<CurrentUser> => {
    // 路由、Header 等多个入口同时初始化时，共用同一个请求，避免重复调用 /auth/me。
    if (loadingPromise) return loadingPromise

    loadingPromise = (async () => {
      try {
        const response = await getCurrentUser()
        currentUser.value = response.data
        return response.data
      } catch (error) {
        currentUser.value = null
        throw error
      } finally {
        initialized.value = true
        loadingPromise = null
      }
    })()

    return loadingPromise
  }

  const hasRole = (roleCode: string): boolean =>
    currentUser.value?.roles.includes(roleCode) ?? false

  const hasPermission = (permissionCode: string): boolean =>
    currentUser.value?.permissions.includes(permissionCode) ?? false

  return {
    currentUser,
    initialized,
    loadCurrentUser,
    clearUser,
    hasRole,
    hasPermission
  }
})
