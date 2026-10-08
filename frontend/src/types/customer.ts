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

/** 新建客户请求，仅包含后端允许前端提交的业务字段。 */
export interface CreateCustomerRequest {
  customerName: string
  customerType: CustomerType
  customerLevel?: CustomerLevel
  industry?: string
  source?: string
  phone?: string
  email?: string
  province?: string
  city?: string
  address?: string
  status: CustomerStatus
  remark?: string
}

export interface CustomerCreateResult {
  id: string
  customerNo: string
}

/** 表单允许用空字符串表示尚未选择的可选项。 */
export interface CustomerFormData {
  customerName: string
  customerType: CustomerType
  customerLevel: '' | CustomerLevel
  industry: string
  source: string
  phone: string
  email: string
  province: string
  city: string
  address: string
  status: CustomerStatus
  remark: string
}

export const CUSTOMER_TYPE_OPTIONS: Array<{ label: string; value: CustomerType }> = [
  { label: '企业客户', value: 'ENTERPRISE' },
  { label: '个人客户', value: 'INDIVIDUAL' }
]

export const CUSTOMER_LEVEL_OPTIONS: Array<{ label: string; value: CustomerLevel }> = [
  { label: 'A 级', value: 'A' },
  { label: 'B 级', value: 'B' },
  { label: 'C 级', value: 'C' },
  { label: 'D 级', value: 'D' }
]

export const CUSTOMER_STATUS_OPTIONS: Array<{ label: string; value: CustomerStatus }> = [
  { label: '潜在客户', value: 'POTENTIAL' },
  { label: '活跃客户', value: 'ACTIVE' },
  { label: '非活跃客户', value: 'INACTIVE' }
]

export const CUSTOMER_INDUSTRY_OPTIONS = [
  '信息技术',
  '制造业',
  '金融',
  '零售',
  '教育',
  '医疗',
  '建筑',
  '物流',
  '专业服务',
  '其他'
] as const

export const CUSTOMER_SOURCE_OPTIONS = [
  '官网咨询',
  '电话营销',
  '客户推荐',
  '线下活动',
  '线上推广',
  '销售开发',
  '其他'
] as const

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
