import request from '@/utils/request'
import type { User } from '@/types/user'

export const getUserList = () => {
  return request.get<User[]>('/users')
}