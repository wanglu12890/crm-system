export type CustomerAnalysisIntent = 'CUSTOMER_TOTAL' | 'CUSTOMER_GRADE_DISTRIBUTION'
export type StatisticsTimeRange = 'ALL' | 'CURRENT_MONTH' | 'LAST_MONTH' | 'RECENT_30_DAYS'
export type AnalysisDataScope = 'ALL' | 'DEPT' | 'SELF'

export interface AiAnalysisRequest {
  question: string
  customerId?: string
  page?: number
  size?: number
  expectedResultFingerprint?: string
}
export interface AiAnalysisResult {
  answer: string
  intent: CustomerAnalysisIntent
  metrics: {
    totalCustomers: number
    levelA: number
    levelB: number
    levelC: number
    levelD: number
    unrated: number
  }
  analysisContext: {
    dataScope: AnalysisDataScope
    timeRange: StatisticsTimeRange
    startTime: string | null
    endTime: string | null
    referenceTime: string
    timezone: string
  }
}

export interface AnalysisExchange {
  id: number
  question: string
  status: 'pending' | 'success' | 'error'
  result?: AnalysisResult
  error?: string
  request?: AiAnalysisRequest
  parentId?: number
  rootId?: number
  operation?: 'question' | 'page' | 'reason' | 'suggestions' | 'retry'
  invalidated?: boolean
  elapsedMs?: number
  actionError?: string
}

export interface AnalysisSession {
  id: number
  title: string
  draft: string
  exchanges: AnalysisExchange[]
}

export type RiskIntent = 'RISK_LIST' | 'RISK_REASON' | 'RISK_SUGGESTIONS'
export type RiskScopeIntent = 'CURRENT_AUTHORIZED' | 'SELF' | 'DEPT'
export interface RiskListQuery {
  scopeIntent: RiskScopeIntent
  queryMode: 'PRIORITY' | 'ALL_STATES'
  customerLevels: string[] | null
  page: number
  size: number
}
export interface RiskReasonQuery { scopeIntent: RiskScopeIntent; customerId: string }
export interface RiskRecord {
  customerId: string
  customerNo: string | null
  customerName: string | null
  customerLevel: string | null
  customerStatus: string
  state: string
  riskLevel: string | null
  historyStatus: string
  lastValidFollowAt: string | null
  anchorType: string
  elapsedDays: number | null
  thresholdDays: number | null
  excessDays: number | null
  reasons: { code: string; message: string; evidence: {
    anchorTime: string | null; elapsedDays: number | null; thresholdDays: number | null; levelGroup: string
  } }[]
  dataQuality: { status: string; codes: string[] }
}
export interface RiskAdviceItem {
  customerId: string
  actions: string[]
  reasonKeys: string[]
  suggestion: string
}
export interface RiskAdvice {
  status: 'NOT_REQUESTED' | 'AVAILABLE' | 'UNAVAILABLE' | 'NOT_APPLICABLE'
  source: string | null
  unavailableCode: string | null
  message: string | null
  items: RiskAdviceItem[]
}
export interface RiskAnalysisResult {
  status: string
  intent: RiskIntent
  answer: string
  analysisContext: {
    authorizedScope: 'SELF' | 'DEPT'; requestedScope: RiskScopeIntent; effectiveScope: 'SELF' | 'DEPT'
    analysisTime: string; timezone: string; ruleVersion: string; consistency: string
  }
  normalizedQuery: RiskListQuery | RiskReasonQuery
  page: { number: number; size: number; returned: number; total: number; pages: number } | null
  records: RiskRecord[]
  resultFingerprint: string
  advice: RiskAdvice
}
export type AnalysisResult = AiAnalysisResult | RiskAnalysisResult
