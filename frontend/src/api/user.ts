import request from '@/utils/request'
import type { CreateUserRequest, User } from '@/types/user'

export const getUserList = () => {
  return request.get<Array<Omit<User, 'roleIds'> & { roleIds: string | string[] }>>('/users').then(
    ({ data }) => ({
      data: data.map((user) => ({
        ...user,
        roleIds: Array.isArray(user.roleIds)
          ? user.roleIds
          : user.roleIds
              .split(',')
              .map((roleId) => roleId.trim())
              .filter(Boolean)
      }))
    })
  )
}

export const createUser = (data: CreateUserRequest) => {
  return request.post<string>('/users', data)
}
