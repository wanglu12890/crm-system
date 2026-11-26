import axios from 'axios'
import type { ApiProblemDetail } from './request'
export const detailError = (error: unknown): string => {
  if (axios.isAxiosError<ApiProblemDetail>(error)) {
    if (error.response?.status === 403) return '无权限查看此内容'
    if (error.response?.status === 404) return '客户不存在或不可访问'
    if (error.response?.status === 401) return '登录已失效，请重新登录'
  }
  return '加载失败，请稍后重试'
}
// 与客户列表保持一致
export const detailDate = (value: string | null) => value ? value.replace('T', ' ').slice(0, 19) : '-'
