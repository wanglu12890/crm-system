import axios from 'axios'
import type { ApiProblemDetail } from '@/utils/request'
import type { AiAnalysisResult, AnalysisDataScope, StatisticsTimeRange } from '@/types/aiAnalysis'

export const AI_ANALYSIS_PERMISSIONS = ['ai:analysis', 'customer:list'] as const
export const scopeLabels: Record<AnalysisDataScope, string> = {
  ALL: '全部授权普通客户', DEPT: '本部门客户', SELF: '本人客户'
}
export const timeLabels: Record<StatisticsTimeRange, string> = {
  ALL: '全部时间', CURRENT_MONTH: '本月', LAST_MONTH: '上月', RECENT_30_DAYS: '近30天'
}
export const statisticsCondition = '仅统计未删除、已分配负责人的普通客户，不包含公海客户。'

export function formatAnalysisTime(value: string | null, timezone: string): string {
  if (!value) return '未提供'
  try {
    return new Intl.DateTimeFormat('zh-CN', {
      timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
    }).format(new Date(value))
  } catch { return value }
}

export function analysisErrorMessage(error: unknown): string {
  if (!axios.isAxiosError<ApiProblemDetail>(error)) return '分析失败，请稍后重试。'
  const status = error.response?.status
  if (error.response?.data?.detail) return error.response.data.detail
  const messages: Record<number, string> = {
    400: '输入或参数不合法，请修改问题后重试。', 401: '登录已失效，请重新登录。',
    403: '无权执行分析或账号数据范围配置有误。', 409: '统计工具调用异常，请重新提交完整问题。',
    422: '当前问题或指定查询范围暂不支持，请修改问题。',
    502: 'AI 模型响应异常，请稍后重试。', 503: 'AI 服务暂时不可用，请稍后重试。',
    500: '分析服务异常，请稍后重试。'
  }
  if (status) return messages[status] || '分析失败，请稍后重试。'
  return error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT'
    ? '分析请求超时，请稍后重试。' : '网络连接失败，请检查网络后重试。'
}

export function resultCopyText(question: string, result: AiAnalysisResult): string {
  const { metrics: m, analysisContext: c } = result
  const window = c.timeRange === 'ALL' ? '全部时间（无创建时间筛选）'
    : `${timeLabels[c.timeRange]}：${formatAnalysisTime(c.startTime, c.timezone)} 至 ${formatAnalysisTime(c.endTime, c.timezone)}（含起、不含止）`
  return [
    `问题：${question}`, `回答：${result.answer}`, `客户总量：${m.totalCustomers} 个`,
    `A 级：${m.levelA}；B 级：${m.levelB}；C 级：${m.levelC}；D 级：${m.levelD}；未评级：${m.unrated}`,
    `时间范围：${window}`, `数据范围：${scopeLabels[c.dataScope]}`,
    `统计口径：${statisticsCondition} 时间筛选基于客户创建时间。`,
    `参考时间：${formatAnalysisTime(c.referenceTime, c.timezone)}（${c.timezone}）`
  ].join('\n')
}
