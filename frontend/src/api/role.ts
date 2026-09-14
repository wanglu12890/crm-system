import type { CreateRoleRequest, Role, UpdateRolePermissionsRequest } from '@/types/role'
import request from '@/utils/request'

export function getRoleList() {
  return request.get<Role[]>('/roles')
}

export function createRole(data: CreateRoleRequest) {
  return request.post<number>('/roles', data)
}

export function getRolePermissionIds(roleId: string) {
  return request.get<string[]>(`/roles/${roleId}/permissions`)
}

export function updateRolePermissions(roleId: string, data: UpdateRolePermissionsRequest) {
  return request.put<void>(`/roles/${roleId}/permissions`, data)
}
