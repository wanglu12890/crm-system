import { computed, onBeforeUnmount, ref, watch } from 'vue'
import axios from 'axios'
import { analyzeCustomers } from '@/api/aiAnalysis'
import { useAuthStore } from '@/stores/auth'
import { AI_ANALYSIS_PERMISSIONS, analysisErrorMessage } from '@/utils/aiAnalysis'
import { isRiskResult, parseAnalysisResponse } from '@/utils/aiAnalysisResponse'
import { buildPageRequest, buildReasonRequest, buildSuggestionsRequest, buildSingleSuggestionsRequest, sameRiskQuery } from '@/utils/riskAnalysis'
import type { AiAnalysisRequest, AnalysisExchange, AnalysisSession, RiskAnalysisResult, RiskIntent } from '@/types/aiAnalysis'

type Operation = NonNullable<AnalysisExchange['operation']>
type Expected = { intent: RiskIntent; query: RiskAnalysisResult['normalizedQuery'] }
export function useAiAnalysis() {
  const auth = useAuthStore()
  const canAnalyze = computed(() => AI_ANALYSIS_PERMISSIONS.every(auth.hasPermission))
  // Presentation only: this never substitutes for the backend's current role, permissions and scope checks.
  const canUseRiskActions = computed(() => canAnalyze.value && auth.hasPermission('follow:list')
    && auth.currentUser?.roles.length === 1 && ['SALES_MANAGER','SALES_STAFF'].includes(auth.currentUser.roles[0]))
  let sequence = 0
  const createSession = (): AnalysisSession => ({ id: ++sequence, title: '新建分析', draft: '', exchanges: [] })
  const sessions = ref<AnalysisSession[]>([createSession()])
  const activeId = ref(sessions.value[0].id)
  const activeSession = computed(() => sessions.value.find(session => session.id === activeId.value)!)
  const recentSessions = computed(() => sessions.value.filter(session => session.exchanges.length > 0))
  const requestCount = ref(0)
  const pending = computed(() => requestCount.value > 0)
  const composerBusy = computed(() => activeSession.value.exchanges.some(e =>
    e.status === 'pending' && e.operation === 'question' && e.question === activeSession.value.draft.trim()))
  const controllers = new Map<number, AbortController>()
  const revisions = new Map<number, number>()
  let securityEpoch = 0
  let disposed = false

  function newAnalysis() {
    const empty = sessions.value.find(session => session.exchanges.length === 0)
    if (empty) activeId.value = empty.id
    else { const session = createSession(); sessions.value.unshift(session); activeId.value = session.id }
    activeSession.value.draft = ''
  }
  function operationPending(rootId: number): boolean {
    return sessions.value.some(s => s.exchanges.some(e => (e.rootId ?? e.id) === rootId && e.status === 'pending'))
  }
  function pendingCustomers(parent: AnalysisExchange): string[] {
    return sessions.value.flatMap(s => s.exchanges.filter(e => (e.rootId ?? e.id) === (parent.rootId ?? parent.id)
      && e.operation === 'single-suggestions' && e.status === 'pending').map(e => e.request?.customerId ?? ''))
  }
  function invalidateRoot(rootId: number, message: string) {
    revisions.set(rootId, (revisions.get(rootId) ?? 0) + 1)
    for (const session of sessions.value) for (const exchange of session.exchanges) {
      if ((exchange.rootId ?? exchange.id) !== rootId) continue
      exchange.result = undefined
      exchange.invalidated = true
      exchange.status = 'error'
      exchange.error = message
      controllers.get(exchange.id)?.abort()
    }
  }
  function invalidateRiskHistory(message: string) {
    securityEpoch++
    const roots = new Set<number>()
    for (const session of sessions.value) for (const e of session.exchanges)
      if ((e.result && isRiskResult(e.result)) || ['page','reason','suggestions','single-suggestions'].includes(e.operation ?? ''))
        roots.add(e.rootId ?? e.id)
    roots.forEach(root => invalidateRoot(root, message))
  }
  async function execute(session: AnalysisSession, request: AiAnalysisRequest, operation: Operation,
    parent?: AnalysisExchange, expected?: Expected): Promise<void> {
    if (disposed || !canAnalyze.value) return
    const rootId = parent?.rootId ?? parent?.id
    if (parent && (operation === 'single-suggestions'
      ? pendingCustomers(parent).includes(request.customerId!) : operationPending(rootId!))) return
    if (!parent && session.exchanges.some(e => e.status === 'pending' && e.question === request.question)) return
    const exchange: AnalysisExchange = { id: ++sequence, question: request.question, status: 'pending',
      operation, request: { ...request }, parentId: parent?.id, rootId }
    session.exchanges.push(exchange)
    const target = session.exchanges[session.exchanges.length - 1]
    const root = rootId ?? target.id
    const revision = revisions.get(root) ?? 0
    const epoch = securityEpoch
    const controller = new AbortController()
    controllers.set(target.id, controller)
    requestCount.value++
    const started = performance.now()
    if (session.exchanges.length === 1) session.title = request.question.length > 24 ? `${request.question.slice(0, 24)}…` : request.question
    try {
      const response = await analyzeCustomers(request, controller.signal)
      if (disposed || target.invalidated) return
      const result = parseAnalysisResponse(response.data)
      if (isRiskResult(result) && (epoch !== securityEpoch || revision !== (revisions.get(root) ?? 0))) {
        target.invalidated = true
        target.status = 'error'
        target.error = '访问状态或原结果已变化，请重新查询。'
        return
      }
      if (expected && (!isRiskResult(result) || result.intent !== expected.intent || !sameRiskQuery(expected.query, result.normalizedQuery)))
        throw new Error('本次结果与原查询条件不一致，请重新查询。')
      target.result = result
      target.status = 'success'
      if (operation === 'question' && session.draft.trim() === request.question) session.draft = ''
    } catch (error: unknown) {
      if (disposed || target.invalidated) return
      target.status = 'error'
      target.error = axios.isCancel(error) ? '请求已取消，可重新查询。'
        : error instanceof Error && error.message === '本次结果与原查询条件不一致，请重新查询。'
          ? error.message : analysisErrorMessage(error)
      if (axios.isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401 || status === 403) invalidateRiskHistory('访问权限或范围已变化，原风险结果已隐藏，请重新查询。')
        else if (status === 404 || (status === 409 && error.response?.data?.code === 'RISK_RESULT_STALE'))
          invalidateRoot(root, '客户或风险结果已失效，原详情已隐藏，请重新查询。')
      }
    } finally {
      if (!disposed) {
        target.elapsedMs = Math.round(performance.now() - started)
        requestCount.value--
      }
      controllers.delete(target.id)
    }
  }
  async function submitQuestion() {
    const target = activeSession.value
    const question = target.draft.trim()
    if (!question || target.draft.length > 1000) return
    await execute(target, { question }, 'question')
  }
  function sessionFor(parent: AnalysisExchange) { return sessions.value.find(s => s.exchanges.some(e => e.id === parent.id)) }
  async function riskAction(parent: AnalysisExchange, operation: 'page' | 'reason' | 'suggestions' | 'single-suggestions',
    args: { customerId?: string; page?: number; size?: number } = {}) {
    if (!canUseRiskActions.value || parent.invalidated || !parent.result || !isRiskResult(parent.result)) return
    const session = sessionFor(parent)
    if (!session) return
    try {
      parent.actionError = undefined
      const result = parent.result
      const request = operation === 'single-suggestions' ? buildSingleSuggestionsRequest(result, args.customerId ?? '')
        : operation === 'reason' ? buildReasonRequest(result, args.customerId ?? '')
        : operation === 'suggestions' ? buildSuggestionsRequest(result)
          : buildPageRequest(result, parent.request ?? { question: parent.question }, args.page ?? 1, args.size ?? 20)
      let expected: Expected
      if (operation === 'reason' || operation === 'single-suggestions') expected = {
        intent: operation === 'reason' ? 'RISK_REASON' : 'RISK_SUGGESTIONS',
        query: { scopeIntent: result.normalizedQuery.scopeIntent, customerId: request.customerId! } }
      else {
        if (!('queryMode' in result.normalizedQuery)) return
        expected = { intent: operation === 'suggestions' ? 'RISK_SUGGESTIONS' : result.intent,
          query: { ...result.normalizedQuery, page: request.page!, size: request.size! } }
      }
      await execute(session, request, operation, parent, expected)
    } catch (error: unknown) { parent.actionError = error instanceof Error ? error.message : '操作参数不合法。' }
  }
  async function retryExchange(exchange: AnalysisExchange) {
    if (exchange.invalidated || !exchange.request) return
    const session = sessionFor(exchange)
    if (exchange.operation === 'single-suggestions') {
      const parent = session?.exchanges.find(e => e.id === exchange.parentId)
      if (parent) await riskAction(parent, 'single-suggestions', { customerId: exchange.request.customerId })
      return
    }
    if (session) await execute(session, exchange.request, 'retry', exchange)
  }
  async function refreshExchange(exchange: AnalysisExchange) {
    const session = sessionFor(exchange)
    if (!session) return
    const root = session.exchanges.find(e => e.id === (exchange.rootId ?? exchange.id))
    if (root) await execute(session, { question: root.question }, 'question')
  }
  watch(() => [auth.currentUser?.id, canAnalyze.value, canUseRiskActions.value, JSON.stringify(auth.currentUser?.roles ?? [])] as const, (now, before) => {
    if (before[0] !== now[0]) {
      securityEpoch++
      controllers.forEach(controller => controller.abort())
      sessions.value = [createSession()]
      activeId.value = sessions.value[0].id
    } else if ((before[1] && !now[1]) || (before[2] && !now[2]) || before[3] !== now[3])
      invalidateRiskHistory('权限已变化，原风险结果已隐藏，请重新查询。')
  })
  onBeforeUnmount(() => { disposed = true; controllers.forEach(controller => controller.abort()); controllers.clear() })
  return { canAnalyze, canUseRiskActions, sessions, activeId, activeSession, recentSessions, pending, composerBusy,
    newAnalysis, submitQuestion, riskAction, retryExchange, refreshExchange, operationPending, pendingCustomers }
}
