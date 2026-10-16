import request from '@/utils/request'
import type { AiAnalysisRequest, AiAnalysisResult } from '@/types/aiAnalysis'

export const analyzeCustomers = (data: AiAnalysisRequest, signal?: AbortSignal) =>
  request.post<AiAnalysisResult>('/ai/analysis', data, {
    // Two bounded backend model calls; retain the shared auth and response interceptors.
    timeout: 60_000,
    signal
  })
