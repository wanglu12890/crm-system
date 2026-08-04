// 该文件定义了一个封装的 Axios 实例，用于发送 HTTP 请求，并提供了请求和响应的拦截器，以便在请求发送前和响应接收后进行统一处理。
import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

// 定义一个通用的 API 响应接口，包含响应码、消息、数据和可选的跟踪 ID 字段。该接口用于描述后端 API 返回的数据结构。
export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId?: string
}

// 创建一个 Axios 实例，设置基础 URL、请求超时时间和请求头。基础 URL 从环境变量中获取，如果未设置，则默认为 '/api'。请求超时时间为 15 秒，请求头指定内容类型为 JSON。
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

// 添加请求拦截器，在请求发送前对请求配置进行处理。可以在这里添加身份验证令牌、修改请求头或进行其他操作。
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // Token should be read from the auth store through a small token service later.
  return config
})

// 添加响应拦截器，在响应接收后对响应数据进行处理。可以在这里统一处理错误、刷新令牌或进行其他操作。
request.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<null>>) => {
    // Centralized refresh-token, 401/403 routing and UI notification are added with auth implementation.
    return Promise.reject(error)
  }
)

// 导出封装的 Axios 实例，以便在其他模块中使用该实例发送 HTTP 请求。
export default request
