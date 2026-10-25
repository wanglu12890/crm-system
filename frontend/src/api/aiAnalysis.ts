import request from '@/utils/request'
import type { AiAnalysisRequest } from '@/types/aiAnalysis'

export const analyzeCustomers = (data: AiAnalysisRequest, signal?: AbortSignal) =>
  request.post<unknown>('/ai/analysis', data, {
    // Keep the existing bounded UI deadline; SUGGESTIONS can use three model stages.
    timeout: 60_000,
    signal
  })
