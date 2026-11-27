import request from '@/utils/request'
import type {
  CreateCustomerRequest,
  CustomerCreateResult,
  CustomerListPageResult,
  CustomerListQuery
} from '@/types/customer'

export const getCustomerList = (params: CustomerListQuery) =>
  request.get<CustomerListPageResult>('/customers', { params })

export const createCustomer = (data: CreateCustomerRequest) =>
  request.post<CustomerCreateResult>('/customers', data)

export const getCustomerDetail = (id: string) =>
  request.get<import('@/types/customer').CustomerDetail>(`/customers/${encodeURIComponent(id)}`)
export const getCustomerContacts = (id: string) =>
  request.get<import('@/types/customer').CustomerContact[]>(`/customers/${encodeURIComponent(id)}/contacts`)
export const getCustomerFollowRecords = (id: string, page: number, size = 10) =>
  request.get<import('@/types/customer').CustomerFollowRecordPage>(`/customers/${encodeURIComponent(id)}/follow-records`, { params: { page, size } })

export const updateCustomer = (id: string, data: import('@/types/customer').UpdateCustomerRequest) =>
  request.put<void>(`/customers/${encodeURIComponent(id)}`, data)
