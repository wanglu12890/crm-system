<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import axios from 'axios'
import { ChatDotRound, Loading } from '@element-plus/icons-vue'
import { analyzeCustomers } from '@/api/aiAnalysis'
import { useAuthStore } from '@/stores/auth'
import { AI_ANALYSIS_PERMISSIONS, analysisErrorMessage } from '@/utils/aiAnalysis'
import type { AnalysisExchange, AnalysisSession } from '@/types/aiAnalysis'
import AiConversationSidebar from '@/components/ai-analysis/AiConversationSidebar.vue'
import AiWelcomePanel from '@/components/ai-analysis/AiWelcomePanel.vue'
import AiChatInput from '@/components/ai-analysis/AiChatInput.vue'
import AiAnalysisResult from '@/components/ai-analysis/AiAnalysisResult.vue'

const auth = useAuthStore()
const canAnalyze = computed(() => AI_ANALYSIS_PERMISSIONS.every(auth.hasPermission))
let sequence = 0
const createSession = (): AnalysisSession => ({ id: ++sequence, title: '新建分析', draft: '', exchanges: [] })
const sessions = ref<AnalysisSession[]>([createSession()])
const activeId = ref(sessions.value[0].id)
const activeSession = computed(() => sessions.value.find(session => session.id === activeId.value)!)
const recentSessions = computed(() => sessions.value.filter(session => session.exchanges.length > 0))
const pending = ref(false)
const composer = ref<InstanceType<typeof AiChatInput>>()
const messages = ref<HTMLElement>()
let controller: AbortController | undefined
let disposed = false

function newAnalysis() {
  // Reuse an empty welcome session; no fabricated records or browser persistence.
  const empty = sessions.value.find(session => session.exchanges.length === 0)
  if (empty) activeId.value = empty.id
  else {
    const session = createSession()
    sessions.value.unshift(session)
    activeId.value = session.id
  }
  activeSession.value.draft = ''
}

async function chooseQuestion(question: string) {
  if (pending.value || !canAnalyze.value) return
  activeSession.value.draft = question
  await nextTick()
  composer.value?.focus()
}

async function submitQuestion() {
  if (pending.value || !canAnalyze.value) return
  const target = activeSession.value
  const question = target.draft.trim()
  if (!question || target.draft.length > 1000) return
  // Bind response and draft updates to the originating session, even if the user switches views.
  target.exchanges.push({ id: ++sequence, question, status: 'pending' })
  const exchange: AnalysisExchange = target.exchanges[target.exchanges.length - 1]
  if (target.exchanges.length === 1) target.title = question.length > 24 ? `${question.slice(0, 24)}…` : question
  pending.value = true
  controller = new AbortController()
  try {
    const response = await analyzeCustomers({ question }, controller.signal)
    if (disposed) return
    exchange.result = response.data
    exchange.status = 'success'
    target.draft = ''
  } catch (error: unknown) {
    if (disposed || axios.isCancel(error)) return
    exchange.status = 'error'
    exchange.error = analysisErrorMessage(error)
    // Keep the original draft for modification or retry; failures have no metric cards.
  } finally {
    if (!disposed) pending.value = false
    controller = undefined
  }
}

watch(() => [activeId.value, activeSession.value.exchanges.map(exchange => exchange.status).join(',')], async () => {
  await nextTick()
  if (messages.value) messages.value.scrollTop = messages.value.scrollHeight
})

onBeforeUnmount(() => { disposed = true; controller?.abort() })
</script>

<template>
  <section v-if="canAnalyze" class="ai-analysis-page">
    <header class="page-header"><el-icon><ChatDotRound /></el-icon><h1>AI 智能分析</h1></header>
    <div class="analysis-layout">
      <AiConversationSidebar :sessions="recentSessions" :active-id="activeId" @create="newAnalysis" @select="activeId = $event" />
      <main class="analysis-workspace">
        <div ref="messages" class="message-region" role="log" aria-label="分析消息" aria-live="polite" :aria-busy="pending">
          <AiWelcomePanel v-if="activeSession.exchanges.length === 0" :disabled="pending" @choose="chooseQuestion" />
          <template v-else>
            <h2 class="session-title">{{ activeSession.title }}</h2>
            <article v-for="exchange in activeSession.exchanges" :key="exchange.id" class="exchange">
              <p class="user-message">{{ exchange.question }}</p>
              <div class="assistant-message">
                <div class="assistant-heading"><el-icon><ChatDotRound /></el-icon><span>CRM AI 销售分析助手</span></div>
                <p v-if="exchange.status === 'pending'" class="loading-state" role="status"><el-icon class="is-loading"><Loading /></el-icon>正在分析，请稍候…</p>
                <el-alert v-else-if="exchange.status === 'error'" :title="exchange.error" type="error" :closable="false" show-icon class="error-state" />
                <AiAnalysisResult v-else-if="exchange.result" :question="exchange.question" :result="exchange.result" />
              </div>
            </article>
          </template>
        </div>
        <div class="composer-region">
          <AiChatInput ref="composer" v-model="activeSession.draft" :busy="pending" :disabled="!canAnalyze" @submit="submitQuestion" />
          <p class="reference-note">AI 结果仅供业务参考，统计数据以受控工具查询结果为准。</p>
        </div>
      </main>
    </div>
  </section>
  <el-alert v-else title="需要 AI 分析和客户查询权限才能使用此功能。" type="warning" :closable="false" show-icon />
</template>

<style scoped>
.ai-analysis-page { height: calc(100dvh - 120px); min-height: 520px; max-height: 1080px; display: flex; flex-direction: column; border: 1px solid #e0e3e8; border-radius: 16px; background: #fff; overflow: hidden; color: #303133; }
.page-header { display: flex; align-items: center; gap: 10px; flex-shrink: 0; min-height: 62px; padding: 0 20px; border-bottom: 1px solid #eceef1; }
.page-header h1 { font-size: 18px; margin: 0; font-weight: 600; }
.analysis-layout { display: flex; min-height: 0; flex: 1; }
.analysis-workspace { flex: 1; min-width: 0; min-height: 0; display: flex; flex-direction: column; padding: 18px 20px 0; }
.message-region { flex: 1; min-height: 0; overflow-y: auto; padding-right: 2px; }
.session-title { font-size: 18px; font-weight: 500; margin: 4px 0 24px; overflow-wrap: anywhere; }
.exchange { padding-bottom: 24px; margin-bottom: 20px; border-bottom: 1px solid #eceef1; }
.user-message { text-align: right; margin: 0 0 28px 32px; white-space: pre-wrap; overflow-wrap: anywhere; font-size: 15px; line-height: 1.7; }
.assistant-message { padding-left: 12px; }
.assistant-heading { display: flex; align-items: center; gap: 12px; font-size: 15px; }
.loading-state { display: flex; gap: 8px; align-items: center; margin: 20px 0; color: #606266; }
.error-state { margin-top: 16px; }
.composer-region { flex-shrink: 0; padding-top: 14px; background: #fff; }
.reference-note { color: #909399; font-size: 12px; text-align: center; line-height: 1.6; margin: 10px 0 12px; }
@media (max-width: 768px) { .ai-analysis-page { height: calc(100dvh - 100px); min-height: 500px; } .analysis-workspace { padding: 12px 10px 0; } .assistant-message { padding-left: 0; } .page-header { padding: 0 14px; } }
</style>
