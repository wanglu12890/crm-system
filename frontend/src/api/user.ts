import request from '@/utils/request'
import type { CreateUserRequest, UpdateUserRequest, User } from '@/types/user'

export const getUserList = () => {
  return request.get<Array<Omit<User, 'roleIds' | 'roleCodes'> & {
    roleIds: string | string[]
    roleCodes: string | string[]
  }>>('/users').then(
    ({ data }) => ({
      data: data.map((user) => ({
        ...user,
        roleIds: Array.isArray(user.roleIds)
          ? user.roleIds
          : user.roleIds
              .split(',')
              .map((roleId) => roleId.trim())
              .filter(Boolean),
        roleCodes: Array.isArray(user.roleCodes)
          ? user.roleCodes
          : user.roleCodes
              .split(',')
              .map((roleCode) => roleCode.trim())
              .filter(Boolean)
      }))
    })
  )
}

export const createUser = (data: CreateUserRequest) => {
  return request.post<string>('/users', data)
}

export const updateUser = (userId: string, data: UpdateUserRequest) => {
  return request.put<void>(`/users/${userId}`, data)
}
