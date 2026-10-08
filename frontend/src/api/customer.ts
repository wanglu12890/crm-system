import request from '@/utils/request'
import type { CustomerListPageResult, CustomerListQuery } from '@/types/customer'

export const getCustomerList = (params: CustomerListQuery) =>
  request.get<CustomerListPageResult>('/customers', { params })
