import type { AiAnalysisResult, AnalysisResult, RiskAnalysisResult } from '@/types/aiAnalysis'

type JsonRecord = Record<string, unknown>
const object = (v: unknown): v is JsonRecord => typeof v === 'object' && v !== null && !Array.isArray(v)
const text = (v: unknown): v is string => typeof v === 'string' && v.length > 0
const nullableText = (v: unknown) => v === null || typeof v === 'string'
const count = (v: unknown): v is number => typeof v === 'number' && Number.isSafeInteger(v) && v >= 0
const nullableCount = (v: unknown) => v === null || count(v)
const strings = (v: unknown): v is string[] => Array.isArray(v) && v.every(text)
const date = (v: unknown) => text(v) && Number.isFinite(Date.parse(v))
const nullableDate = (v: unknown) => v === null || date(v)
export const isCustomerId = (v: unknown): v is string => text(v) && /^[1-9][0-9]{0,18}$/.test(v) && BigInt(v) <= 9223372036854775807n
export const isRiskResult = (v: AnalysisResult): v is RiskAnalysisResult =>
  ['RISK_LIST', 'RISK_REASON', 'RISK_SUGGESTIONS'].includes(v.intent)
export class AnalysisResponseError extends Error {
  constructor() { super('分析响应格式异常，请重新查询。') }
}
const fail = (): never => { throw new AnalysisResponseError() }

function recordValid(v: unknown): boolean {
  if (!object(v) || !isCustomerId(v.customerId) || !nullableText(v.customerNo) || !nullableText(v.customerName)
    || !nullableText(v.customerLevel) || !text(v.customerStatus) || !text(v.state) || !nullableText(v.riskLevel)
    || !text(v.historyStatus) || !nullableDate(v.lastValidFollowAt) || !text(v.anchorType)
    || !nullableCount(v.elapsedDays) || !nullableCount(v.thresholdDays) || !nullableCount(v.excessDays)
    || !Array.isArray(v.reasons) || !object(v.dataQuality) || !text(v.dataQuality.status) || !strings(v.dataQuality.codes)) return false
  return v.reasons.every(r => object(r) && text(r.code) && text(r.message) && object(r.evidence)
    && nullableDate(r.evidence.anchorTime) && nullableCount(r.evidence.elapsedDays)
    && nullableCount(r.evidence.thresholdDays) && text(r.evidence.levelGroup))
}

export function parseAnalysisResponse(v: unknown): AnalysisResult {
  if (!object(v) || !text(v.intent) || !text(v.answer) || !object(v.analysisContext)) return fail()
  const c = v.analysisContext
  if (['CUSTOMER_TOTAL', 'CUSTOMER_GRADE_DISTRIBUTION'].includes(v.intent)) {
    if (!object(v.metrics) || !['totalCustomers','levelA','levelB','levelC','levelD','unrated'].every(k => count((v.metrics as JsonRecord)[k]))
      || !['SELF','DEPT','ALL'].includes(String(c.dataScope)) || !['ALL','CURRENT_MONTH','LAST_MONTH','RECENT_30_DAYS'].includes(String(c.timeRange))
      || !date(c.referenceTime) || !text(c.timezone)
      || (c.startTime !== undefined && !nullableDate(c.startTime)) || (c.endTime !== undefined && !nullableDate(c.endTime))
      || (c.timeRange !== 'ALL' && (!date(c.startTime) || !date(c.endTime)))) return fail()
    return { ...v, analysisContext: { ...c, startTime: c.startTime ?? null, endTime: c.endTime ?? null } } as unknown as AiAnalysisResult
  }
  if (!['RISK_LIST','RISK_REASON','RISK_SUGGESTIONS'].includes(v.intent) || !text(v.status)
    || !['SELF','DEPT'].includes(String(c.authorizedScope)) || !['SELF','DEPT'].includes(String(c.effectiveScope))
    || !['CURRENT_AUTHORIZED','SELF','DEPT'].includes(String(c.requestedScope)) || !date(c.analysisTime)
    || !text(c.timezone) || !text(c.ruleVersion) || !text(c.consistency) || !text(v.resultFingerprint)
    || !Array.isArray(v.records) || v.records.length > 50 || !v.records.every(recordValid)
    || !object(v.normalizedQuery) || !object(v.advice)) return fail()
  const records = v.records as JsonRecord[]
  const ids = records.map(r => r.customerId)
  if (new Set(ids).size !== ids.length) return fail()
  const q = v.normalizedQuery
  if (!['CURRENT_AUTHORIZED','SELF','DEPT'].includes(String(q.scopeIntent)) || q.scopeIntent !== c.requestedScope
    || (c.effectiveScope === 'DEPT' && c.authorizedScope !== 'DEPT')) return fail()
  if (v.intent === 'RISK_REASON' || (v.intent === 'RISK_SUGGESTIONS' && 'customerId' in q)) {
    if (v.page !== null || !isCustomerId(q.customerId) || records.length !== 1 || ids[0] !== q.customerId) return fail()
  } else {
    if (!object(v.page) || !count(v.page.number) || v.page.number < 1 || !count(v.page.size) || v.page.size < 1 || v.page.size > 50
      || !count(v.page.returned) || v.page.returned !== records.length || v.page.returned > v.page.size
      || !count(v.page.total) || !count(v.page.pages) || v.page.pages !== Math.ceil(v.page.total / v.page.size)
      || v.page.total < records.length || !['PRIORITY','ALL_STATES'].includes(String(q.queryMode))
      || q.page !== v.page.number || q.size !== v.page.size
      || !(q.customerLevels === null || (strings(q.customerLevels) && q.customerLevels.length > 0
        && q.customerLevels.every(l => ['A','B','C','D','UNRATED'].includes(l))))) return fail()
  }
  const a = v.advice
  const unavailable = { status: 'UNAVAILABLE', source: null, unavailableCode: 'CLIENT_UNSUPPORTED_ADVICE',
    message: '建议状态或内容暂不支持，已保留风险分析结果。', items: [] }
  const known = ['NOT_REQUESTED','AVAILABLE','UNAVAILABLE','NOT_APPLICABLE'].includes(String(a.status))
  if (!known) return { ...v, advice: unavailable } as unknown as RiskAnalysisResult
  if (!nullableText(a.source) || !nullableText(a.unavailableCode) || !nullableText(a.message) || !Array.isArray(a.items)) return fail()
  if (a.status === 'AVAILABLE') {
    const actions = ['VERIFY_CURRENT_NEEDS','CONFIRM_CONTACT_PREFERENCE','DISCUSS_NEXT_STEP','VERIFY_DATA']
    const valid = a.source === 'QWEN_ACTIONS_JAVA_TEMPLATE' && a.items.length === records.length && a.items.every(item =>
      object(item) && isCustomerId(item.customerId) && ids.includes(item.customerId) && text(item.suggestion) && item.suggestion.length <= 1000
      && strings(item.actions) && item.actions.length > 0 && item.actions.length <= 3 && item.actions.every(code => actions.includes(code))
      && strings(item.reasonKeys) && item.reasonKeys.every(code => {
        const record = records.find(r => r.customerId === item.customerId)
        return Array.isArray(record?.reasons) && record.reasons.some(r => object(r) && r.code === code)
      }))
    if (!valid || new Set(a.items.map(item => (item as JsonRecord).customerId)).size !== records.length)
      return { ...v, advice: unavailable } as unknown as RiskAnalysisResult
  } else if (a.items.length !== 0) return { ...v, advice: unavailable } as unknown as RiskAnalysisResult
  return v as unknown as RiskAnalysisResult
}
