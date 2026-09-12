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
