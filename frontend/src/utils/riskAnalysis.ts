import type { AiAnalysisRequest, RiskAnalysisResult, RiskListQuery, RiskRecord } from '@/types/aiAnalysis'
import { isCustomerId } from './aiAnalysisResponse'

export const riskScopeLabels = { SELF: '本人客户', DEPT: '本部门客户', CURRENT_AUTHORIZED: '当前授权范围' }
export function riskStatusLabel(record: RiskRecord): string {
  if (record.state === 'OBSERVING') return '观察中'
  if (record.state === 'DATA_REVIEW') return '数据待核验'
  if (record.state !== 'NORMAL') return '未知关注状态'
  return ({ HIGH: '高关注', MEDIUM: '中关注', LOW: '低关注' } as Record<string, string>)[record.riskLevel ?? ''] ?? '未知关注等级'
}
export function riskStatusType(record: RiskRecord): 'danger' | 'warning' | 'info' | 'success' {
  if (record.state !== 'NORMAL') return 'info'
  return record.riskLevel === 'HIGH' ? 'danger' : record.riskLevel === 'MEDIUM' ? 'warning' : record.riskLevel === 'LOW' ? 'success' : 'info'
}
const scopeQuestion = (scope: string) => ({ SELF: '本人范围内', DEPT: '本部门范围内', CURRENT_AUTHORIZED: '当前可查看范围内' } as Record<string, string>)[scope]
export function listQuery(result: RiskAnalysisResult): RiskListQuery {
  if (result.page === null || !('queryMode' in result.normalizedQuery)) throw new Error('该结果不是可分页的风险清单。')
  return result.normalizedQuery
}
export function buildReasonRequest(result: RiskAnalysisResult, customerId: string): AiAnalysisRequest {
  if (!isCustomerId(customerId) || !result.records.some(record => record.customerId === customerId))
    throw new Error('请从当前结果中明确选择客户。')
  return { question: `查看${scopeQuestion(result.normalizedQuery.scopeIntent)}已选择客户的关注原因。`, customerId }
}
export function buildSuggestionsRequest(result: RiskAnalysisResult): AiAnalysisRequest {
  const q = listQuery(result)
  const levels = q.customerLevels?.map(level => level === 'UNRATED' ? '未评级' : `${level}级`).join('、')
  const condition = q.queryMode === 'ALL_STATES' ? '全部关注状态' : '系统规则判定需要重点关注'
  return {
    question: `请给出${scopeQuestion(q.scopeIntent)}${levels ? `客户等级仅限${levels}的` : ''}${condition}的客户跟进建议。`,
    page: q.page, size: q.size, expectedResultFingerprint: result.resultFingerprint
  }
}
export function buildPageRequest(result: RiskAnalysisResult, original: AiAnalysisRequest, page: number, size: number): AiAnalysisRequest {
  listQuery(result)
  if (!Number.isInteger(page) || page < 1 || !Number.isInteger(size) || size < 1 || size > 50)
    throw new Error('页码或每页数量不合法。')
  // Reclassify the original question. A previous page's fingerprint must never be sent for the new page.
  return { question: original.question, page, size }
}
export function sameRiskQuery(expected: RiskAnalysisResult['normalizedQuery'], actual: RiskAnalysisResult['normalizedQuery']): boolean {
  if (expected.scopeIntent !== actual.scopeIntent) return false
  if ('customerId' in expected) return 'customerId' in actual && expected.customerId === actual.customerId
  return 'queryMode' in actual && expected.queryMode === actual.queryMode && expected.page === actual.page && expected.size === actual.size
    && JSON.stringify([...(expected.customerLevels ?? [])].sort()) === JSON.stringify([...(actual.customerLevels ?? [])].sort())
}
