export interface PermissionTreeNode {
  id: string
  parentId: string
  permissionCode: string
  permissionName: string
  permissionType: 'MENU' | 'BUTTON' | 'API'
  routePath?: string | null
  sortOrder: number
  children: PermissionTreeNode[]
}

export type PermissionStatus = 0 | 1

export interface PermissionListItem {
  id: string
  parentId: string
  permissionName: string
  permissionCode: string
  moduleId: string | null
  moduleName: string | null
  permissionType: string
  status: PermissionStatus
  routePath: string | null
  httpMethod: string | null
  apiPath: string | null
  sortOrder: number
}

export interface PermissionListQuery {
  keyword?: string
  moduleId?: string
  status?: PermissionStatus
}

export interface PermissionOverviewSummary {
  permissionCount: number
  roleCount: number
  moduleCount: number
}

export interface PermissionOverviewRole {
  id: string
  roleName: string
  roleCode: string
  status: PermissionStatus
}

export interface PermissionOverviewPermission {
  id: string
  parentId: string
  permissionName: string
  permissionCode: string
  moduleId: string | null
  moduleName: string | null
  permissionType: string
  status: PermissionStatus
  sortOrder: number
}

export interface PermissionOverviewResponse {
  summary: PermissionOverviewSummary
  roles: PermissionOverviewRole[]
  permissions: PermissionOverviewPermission[]
  rolePermissions: Record<string, string[]>
}
