export interface PermissionTreeNode {
  id: number
  parentId: number
  permissionCode: string
  permissionName: string
  permissionType: 'MENU' | 'BUTTON' | 'API'
  routePath?: string | null
  sortOrder: number
  children: PermissionTreeNode[]
}