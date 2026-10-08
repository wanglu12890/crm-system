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
