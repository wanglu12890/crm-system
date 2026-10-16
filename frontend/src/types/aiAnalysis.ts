export type CustomerAnalysisIntent = 'CUSTOMER_TOTAL' | 'CUSTOMER_GRADE_DISTRIBUTION'
export type StatisticsTimeRange = 'ALL' | 'CURRENT_MONTH' | 'LAST_MONTH' | 'RECENT_30_DAYS'
export type AnalysisDataScope = 'ALL' | 'DEPT' | 'SELF'

export interface AiAnalysisRequest { question: string }
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
  result?: AiAnalysisResult
  error?: string
}

export interface AnalysisSession {
  id: number
  title: string
  draft: string
  exchanges: AnalysisExchange[]
}
