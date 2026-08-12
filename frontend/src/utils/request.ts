// 该文件定义了一个封装的 Axios 实例，用于发送 HTTP 请求，并提供了请求和响应的拦截器，以便在请求发送前和响应接收后进行统一处理。
import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { getAccessToken, removeAuthToken } from '@/utils/auth'

// 定义一个通用的 API 响应接口，包含响应码、消息、数据和可选的跟踪 ID 字段。该接口用于描述后端 API 返回的数据结构。
export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId?: string
}

export interface ApiProblemDetail {
  title?: string
  status?: number
  detail?: string
  code?: string
}

// 创建一个 Axios 实例，设置基础 URL、请求超时时间和请求头。基础 URL 从环境变量中获取，如果未设置，则默认为 '/api'。请求超时时间为 15 秒，请求头指定内容类型为 JSON。
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

// 注册一个请求拦截器，在请求发送前对请求配置进行处理。可以在这里添加身份验证令牌、修改请求头或进行其他操作。
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // 1.获取存储的访问令牌，通过调用utils/auth.ts自定义函数getAccessToken()，通常从localStorage中读取
  const accessToken = getAccessToken()

  // 2.如果令牌存在，添加到请求头，即设置HTTP请求头的Authorization字段【前端发送 JWT 时，在 HTTP Authorization 请求头中使用 Bearer 认证方案。】
  // Bearer 是前端以后使用这个 JWT 请求后端接口时，加在 HTTP Authorization 请求头里的认证方案名称。
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// 添加响应拦截器，在响应接收后对响应数据进行处理。可以在这里统一处理错误、刷新令牌或进行其他操作。
// 自动检测 401 状态码，并在非登录场景下清除失效的认证信息，实现了认证状态的统一管理
request.interceptors.response.use(
  // 成功响应处理器（2xx 状态码），触发条件：后端返回 2xx 状态码（200、201、204 等）
  (response) => response,

  // 错误响应处理器，触发条件：任何非 2xx 的状态码（400、401、403、500 等）或网络错误
  (error: AxiosError<ApiProblemDetail>) => {
    // 判断是否为登录请求
    const isLoginRequest = error.config?.url === '/auth/login'

    // 如果HTTP 状态码为 401 Unauthorized（未认证/token 过期）并且不是登录请求本身，
    if (error.response?.status === 401 && !isLoginRequest) {
      removeAuthToken() // 则清除token
    }

    // 为什么需要为什么排除登录请求？因为如果是登录接口&返回 401，不需要清除token，用户还没登录成功，本地本来就没有有效 token，或者输入了错误密码，应该让用户看到"用户名或密码错误"的提示

    return Promise.reject(error)
  }
)

// 导出封装的 Axios 实例，以便在其他模块中使用该实例发送 HTTP 请求。
export default request
