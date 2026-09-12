export type RoleStatus = 0 | 1

export interface Role {
  id: string
  roleName: string
  roleCode: string
  status: RoleStatus
  permissionCount: number
  remark?: string
}

export interface RoleSearchCriteria {
  roleName: string
  roleCode: string
  status: '' | RoleStatus
}

export interface RoleFormData {
  id?: string
  roleName: string
  roleCode: string
  status: RoleStatus
  remark: string
}

export type CreateRoleRequest = Omit<RoleFormData, 'id'>

export type RoleDialogMode = 'create' | 'edit'
