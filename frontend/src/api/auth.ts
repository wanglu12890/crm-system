// 该文件定义了与身份验证相关的 API 请求，包括登录请求和响应的类型定义，以及登录函数的实现。
import request from '@/utils/request'

// 定义登录请求的接口，包含用户名和密码字段。
export interface LoginRequest {
  username: string
  password: string
}

// 定义登录响应的接口，包含访问令牌、刷新令牌和过期时间字段。
export interface LoginResponse {
  accessToken: string
  expiresIn: number
}

export interface CurrentUser {
  id: number
  username: string
  realName: string
  roles: string[]
}

// 定义登录函数，接受登录请求数据作为参数，并返回一个 Promise，表示异步的登录请求结果。该函数使用封装的 request 实例发送 POST 请求到 '/auth/login' 接口，并指定请求和响应的数据类型。
// 什么是 Promise？Promise 是 JavaScript 中的一种异步编程解决方案，它表示一个可能在未来某个时间点完成的操作，并允许我们在操作完成后处理结果或错误。Promise 有三种状态：pending（进行中）、fulfilled（已完成）和rejected（已拒绝）。通过 then() 和 catch() 方法，我们可以注册回调函数来处理操作的结果或错误。
export const login = (data: LoginRequest) =>
  request.post<LoginResponse>('/auth/login', data)

export const getCurrentUser = () =>
  request.get<CurrentUser>('/auth/me')
