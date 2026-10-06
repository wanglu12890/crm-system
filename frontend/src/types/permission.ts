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
