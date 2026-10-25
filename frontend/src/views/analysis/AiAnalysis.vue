<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { ChatDotRound, Loading } from '@element-plus/icons-vue'
import { useAiAnalysis } from '@/components/ai-analysis/useAiAnalysis'
import { isRiskResult } from '@/utils/aiAnalysisResponse'
import AiConversationSidebar from '@/components/ai-analysis/AiConversationSidebar.vue'
import AiWelcomePanel from '@/components/ai-analysis/AiWelcomePanel.vue'
import AiChatInput from '@/components/ai-analysis/AiChatInput.vue'
import AiAnalysisResult from '@/components/ai-analysis/AiAnalysisResult.vue'
import RiskAnalysisResult from '@/components/ai-analysis/RiskAnalysisResult.vue'

const { canAnalyze, canUseRiskActions, sessions, activeId, activeSession, recentSessions, pending, composerBusy,
  newAnalysis, submitQuestion, riskAction, retryExchange, refreshExchange, operationPending, pendingCustomers } = useAiAnalysis()
const composer = ref<InstanceType<typeof AiChatInput>>()
const messages = ref<HTMLElement>()
async function chooseQuestion(question: string) {
  if (!canAnalyze.value) return
  activeSession.value.draft = question
  await nextTick()
  composer.value?.focus()
}
watch(() => [activeId.value, activeSession.value.exchanges.map(exchange => exchange.status).join(',')], async () => {
  await nextTick()
  if (messages.value) messages.value.scrollTop = messages.value.scrollHeight
})
</script>

<template>
  <section v-if="canAnalyze" class="ai-analysis-page">
    <header class="page-header"><el-icon><ChatDotRound /></el-icon><h1>AI 智能分析</h1></header>
    <div class="analysis-layout">
      <AiConversationSidebar :sessions="recentSessions" :active-id="activeId" @create="newAnalysis" @select="activeId = $event" />
      <main class="analysis-workspace">
        <div ref="messages" class="message-region" role="log" aria-label="分析消息" aria-live="polite" :aria-busy="pending">
          <AiWelcomePanel v-if="activeSession.exchanges.length === 0" :disabled="!canAnalyze" @choose="chooseQuestion" />
          <template v-else>
            <h2 class="session-title">{{ activeSession.title }}</h2>
            <article v-for="exchange in activeSession.exchanges" :key="exchange.id" :id="`analysis-message-${exchange.id}`" class="exchange">
              <p class="user-message">{{ exchange.question }}</p>
              <p v-if="exchange.parentId" class="related-message">关联消息 #{{ exchange.parentId }} · {{ exchange.operation === 'single-suggestions' ? '单客户跟进建议' : exchange.operation === 'reason' ? '客户原因' : exchange.operation === 'page' ? '重新查询分页' : exchange.operation === 'suggestions' ? '当前页建议' : '重新请求' }}</p>
              <div class="assistant-message">
                <div class="assistant-heading"><el-icon><ChatDotRound /></el-icon><span>CRM AI 销售分析助手</span></div>
                <p v-if="exchange.status === 'pending'" class="loading-state" role="status"><el-icon class="is-loading"><Loading /></el-icon>正在分析，请稍候…建议分析可能包含多个处理阶段，最长等待约 60 秒。</p>
                <el-alert v-else-if="exchange.status === 'error'" :title="exchange.error" type="error" :closable="false" show-icon class="error-state" />
                <RiskAnalysisResult v-else-if="exchange.result && isRiskResult(exchange.result)" :result="exchange.result" :request="exchange.request"
                  :can-act="canUseRiskActions" :busy="operationPending(exchange.rootId ?? exchange.id)" :action-error="exchange.actionError"
                  :pending-customers="pendingCustomers(exchange)" @single-suggestions="riskAction(exchange, 'single-suggestions', { customerId: $event })"
                  @page="(page, size) => riskAction(exchange, 'page', { page, size })" />
                <AiAnalysisResult v-else-if="exchange.result && !isRiskResult(exchange.result)" :question="exchange.question" :result="exchange.result" />
                <el-button v-if="exchange.status === 'error' || (exchange.operation === 'single-suggestions' && exchange.result && isRiskResult(exchange.result) && exchange.result.advice.status === 'UNAVAILABLE')" class="retry-button" size="small"
                  :disabled="operationPending(exchange.rootId ?? exchange.id)"
                  @click="exchange.invalidated ? refreshExchange(exchange) : retryExchange(exchange)">
                  {{ exchange.invalidated ? '重新查询原问题' : '重试' }}
                </el-button>
                <p v-if="exchange.elapsedMs !== undefined" class="request-time">本次请求用时 {{ (exchange.elapsedMs / 1000).toFixed(1) }} 秒</p>
              </div>
            </article>
          </template>
        </div>
        <div class="composer-region">
          <AiChatInput ref="composer" v-model="activeSession.draft" :busy="composerBusy" :disabled="!canAnalyze" @submit="submitQuestion" />
          <p class="reference-note">结果仅供业务参考；关注事实由后端规则判定，AI 辅助建议需人工核实。</p>
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
.related-message, .request-time { font-size: 12px; color: #909399; line-height: 1.6; }\n.retry-button { margin-top: 12px; }\n.error-state { margin-top: 16px; }
.composer-region { flex-shrink: 0; padding-top: 14px; background: #fff; }
.reference-note { color: #909399; font-size: 12px; text-align: center; line-height: 1.6; margin: 10px 0 12px; }
@media (max-width: 768px) { .ai-analysis-page { height: calc(100dvh - 100px); min-height: 500px; } .analysis-workspace { padding: 12px 10px 0; } .assistant-message { padding-left: 0; } .page-header { padding: 0 14px; } }
</style>
