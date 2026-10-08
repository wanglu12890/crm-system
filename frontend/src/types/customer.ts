export type CustomerType = 'ENTERPRISE' | 'INDIVIDUAL'
export type CustomerLevel = 'A' | 'B' | 'C' | 'D'
export type CustomerStatus = 'POTENTIAL' | 'ACTIVE' | 'INACTIVE'

export interface CustomerListItem {
  id: string
  customerNo: string
  customerName: string
  customerType: CustomerType
  customerLevel: CustomerLevel | null
  industry: string | null
  status: CustomerStatus
  ownerId: string | null
  ownerName: string | null
  updatedAt: string
}

export interface CustomerListQuery {
  page: number
  size: number
  keyword?: string
  customerLevel?: CustomerLevel
  status?: CustomerStatus
  customerType?: CustomerType
  ownerId?: string
}

export interface CustomerListPageResult {
  records: CustomerListItem[]
  total: number
  page: number
  size: number
  pages: number
}

export interface CustomerSearchCriteria {
  keyword: string
  customerType: '' | CustomerType
  customerLevel: '' | CustomerLevel
  status: '' | CustomerStatus
  ownerId: string
}

export const CUSTOMER_TYPE_LABELS: Record<CustomerType, string> = {
  ENTERPRISE: '企业客户',
  INDIVIDUAL: '个人客户'
}

export const CUSTOMER_LEVEL_LABELS: Record<CustomerLevel, string> = {
  A: 'A级',
  B: 'B级',
  C: 'C级',
  D: 'D级'
}

export const CUSTOMER_STATUS_LABELS: Record<CustomerStatus, string> = {
  POTENTIAL: '潜在客户',
  ACTIVE: '活跃客户',
  INACTIVE: '非活跃客户'
}
