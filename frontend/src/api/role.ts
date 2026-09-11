import type { CreateRoleRequest, Role } from '@/types/role'
import request from '@/utils/request'

export function getRoleList() {
  return request.get<Role[]>('/roles')
}

export function createRole(data: CreateRoleRequest) {
  return request.post<number>('/roles', data)
}
