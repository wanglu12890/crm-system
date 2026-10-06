import type {
  PermissionListItem,
  PermissionListQuery,
  PermissionOverviewResponse,
  PermissionTreeNode
} from '@/types/permission'
import request from '@/utils/request'

export function getPermissionTree() {
  return request.get<PermissionTreeNode[]>('/permissions')
}

export function getPermissionList(params?: PermissionListQuery) {
  return request.get<PermissionListItem[]>('/permissions/list', { params })
}

export function getPermissionOverview() {
  return request.get<PermissionOverviewResponse>('/permissions/overview')
}
