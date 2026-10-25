<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { AiAnalysisRequest, RiskAnalysisResult } from '@/types/aiAnalysis'
import { formatAnalysisTime } from '@/utils/aiAnalysis'
import { canSuggestCustomer, riskScopeLabels, riskStatusLabel, riskStatusType } from '@/utils/riskAnalysis'
import RiskAdvicePanel from './RiskAdvicePanel.vue'
const props = defineProps<{ result: RiskAnalysisResult; request?: AiAnalysisRequest; canAct: boolean; busy: boolean; actionError?: string; pendingCustomers?: string[] }>()
defineEmits<{ page: [page: number, size: number]; singleSuggestions: [customerId: string] }>()
const expanded = ref<string[]>([])
const detailsExpanded = ref(false)
const singleRecord = computed(() => {
  const { result, request } = props
  return result.intent === 'RISK_SUGGESTIONS' && request?.customerId && result.page === null
    && 'customerId' in result.normalizedQuery && result.normalizedQuery.customerId === request.customerId
    && result.records.length === 1 && result.records[0].customerId === request.customerId ? result.records[0] : undefined
})
const singleReason = computed(() => {
  const record = singleRecord.value
  if (!record) return ''
  if (record.reasons.length) return [...new Set(record.reasons.map(reason => reason.message))].join('；')
  if (record.state === 'DATA_REVIEW') return '客户数据待核验，暂不评定关注等级。'
  if (record.state === 'OBSERVING') return '客户仍处于观察期，不等同于高关注。'
  return '当前未返回额外风险命中原因，请结合关注状态核实。'
})
watch(() => [props.result, props.request], () => { expanded.value = []; detailsExpanded.value = false })
watch(() => props.canAct, value => { if (!value) { expanded.value = []; detailsExpanded.value = false } })
function toggleEvidence(customerId: string) {
  if (!props.canAct) return
  expanded.value = expanded.value.includes(customerId)
    ? expanded.value.filter(id => id !== customerId) : [...expanded.value, customerId]
}
</script>
<template>
  <section v-if="singleRecord" class="single-result" aria-label="单客户跟进建议">
    <header class="single-heading"><h3>{{ singleRecord.customerName || '未提供客户名称' }}</h3>
      <el-tag :type="riskStatusType(singleRecord)">{{ riskStatusLabel(singleRecord) }}</el-tag></header>
    <p class="single-meta">客户等级：{{ singleRecord.customerLevel || '未评级' }} · 状态：{{ singleRecord.customerStatus === 'ACTIVE' ? '活跃' : singleRecord.customerStatus === 'POTENTIAL' ? '潜在' : '未知状态' }} ·
      {{ singleRecord.historyStatus === 'NEVER_FOLLOWED' ? '从未有效跟进，距建档' : '距最近有效跟进' }}：{{ singleRecord.elapsedDays == null ? '待核验' : singleRecord.elapsedDays + ' 天' }}</p>
    <p class="single-reason">判定依据：{{ singleReason }}</p>
    <p v-if="singleRecord.dataQuality.status !== 'CLEAN'" class="quality">包含需核验的数据，请先确认相关业务记录。</p>
    <el-alert v-if="!['SUCCESS','PARTIAL'].includes(result.status)" title="返回了暂不支持的分析状态，请谨慎核实并重新查询。" type="warning" :closable="false" />
    <RiskAdvicePanel :advice="result.advice" :records="result.records" compact />
    <el-button class="details-toggle" size="small" :disabled="!canAct" :aria-expanded="detailsExpanded"
      @click="detailsExpanded = !detailsExpanded">{{ detailsExpanded ? '收起分析详情' : '分析详情' }}</el-button>
    <section v-if="canAct && detailsExpanded" class="context single-details" aria-label="分析详情">
      <p>客户编号：{{ singleRecord.customerNo || '未提供' }} · ID：{{ singleRecord.customerId }}</p>
      <p>数据范围：{{ riskScopeLabels[result.analysisContext.effectiveScope] }} · 最大授权范围：{{ riskScopeLabels[result.analysisContext.authorizedScope] }}</p>
      <p>分析时间：{{ formatAnalysisTime(result.analysisContext.analysisTime, result.analysisContext.timezone) }}（{{ result.analysisContext.timezone }}）</p>
      <p>规则版本：{{ result.analysisContext.ruleVersion }}</p>
      <p>查询口径：当前授权范围内有效已分配的潜在/活跃客户；关注状态不是客户流失预测。</p>
      <p>最近有效跟进时间：{{ singleRecord.lastValidFollowAt ? formatAnalysisTime(singleRecord.lastValidFollowAt, result.analysisContext.timezone) : singleRecord.historyStatus === 'NEVER_FOLLOWED' ? '从未有效跟进' : '未提供，待核验' }}</p>
      <p>当前规则阈值：{{ singleRecord.thresholdDays == null ? '不适用' : singleRecord.thresholdDays + ' 天' }}<template v-if="singleRecord.excessDays != null">；超过阈值：{{ singleRecord.excessDays }} 天</template></p>
      <ul v-if="singleRecord.reasons.length"><li v-for="reason in singleRecord.reasons" :key="reason.code">{{ reason.code }}：观测 {{ reason.evidence.elapsedDays ?? '待核验' }} 天；阈值 {{ reason.evidence.thresholdDays ?? '不适用' }} 天；时间依据 {{ formatAnalysisTime(reason.evidence.anchorTime, result.analysisContext.timezone) }}；等级组 {{ reason.evidence.levelGroup || '未提供' }}</li></ul>
      <p v-if="singleRecord.dataQuality.status !== 'CLEAN'">数据质量状态：{{ singleRecord.dataQuality.status }}；标记：{{ singleRecord.dataQuality.codes.join('、') || '未提供' }}</p>
      <p>每次请求重新查询当前状态，不保证与来源清单为同一历史快照；结果失效时请重新查询。</p>
    </section>
    <el-alert v-if="actionError" :title="actionError" type="warning" :closable="false" />
  </section>
  <section v-else class="risk-result" aria-label="客户跟进关注分析">
    <p class="answer">{{ result.answer }}</p>
    <el-alert v-if="!['SUCCESS','PARTIAL'].includes(result.status)" title="返回了暂不支持的分析状态，请谨慎核实并重新查询。" type="warning" :closable="false" />
    <div class="context">
      <p>数据范围：{{ riskScopeLabels[result.analysisContext.effectiveScope] }} · 最大授权：{{ riskScopeLabels[result.analysisContext.authorizedScope] }}</p>
      <p>分析时间：{{ formatAnalysisTime(result.analysisContext.analysisTime, result.analysisContext.timezone) }}（{{ result.analysisContext.timezone }}）</p>
      <p v-if="'queryMode' in result.normalizedQuery">查询条件：{{ result.normalizedQuery.queryMode === 'PRIORITY' ? '高/中关注客户' : '全部适用客户关注状态' }}；客户等级：{{ result.normalizedQuery.customerLevels?.map(level => level === 'UNRATED' ? '未评级' : level + '级').join('、') || '不限' }}</p>
      <p>分析口径：当前授权范围内有效已分配的潜在/活跃客户；关注状态不是客户流失预测。</p>
      <details><summary>规则与结果说明</summary><p>规则版本：{{ result.analysisContext.ruleVersion }}</p><p>每次请求重新查询当前状态，不保证跨页为同一历史快照。</p></details>
    </div>
    <el-empty v-if="result.records.length === 0" :description="result.page && result.page.total > 0 ? '本页没有客户，其他页仍有匹配结果。' : '没有匹配当前条件的客户。'" :image-size="60" />
    <article v-for="record in result.records" :key="record.customerId" class="customer-card">
      <header><div><strong>{{ record.customerName || '未提供客户名称' }}</strong><small>客户编号：{{ record.customerNo || record.customerId }} · ID：{{ record.customerId }}</small></div>
        <el-tag :type="riskStatusType(record)">{{ riskStatusLabel(record) }}</el-tag></header>
      <p>客户等级：{{ record.customerLevel || '未评级' }}<span class="divider">|</span>客户状态：{{ record.customerStatus === 'ACTIVE' ? '活跃' : record.customerStatus === 'POTENTIAL' ? '潜在' : '未知状态' }}</p>
      <p v-if="record.historyStatus === 'NEVER_FOLLOWED'">无有效跟进历史；距建档：{{ record.elapsedDays === null ? '待核验' : record.elapsedDays + ' 天' }}。</p>
      <p v-else-if="record.historyStatus === 'INVALID_ONLY'">跟进时间数据待核验，暂不评定关注等级。</p>
      <p v-else>最近有效跟进：{{ formatAnalysisTime(record.lastValidFollowAt, result.analysisContext.timezone) }}；距分析时间 {{ record.elapsedDays === null ? '待核验' : record.elapsedDays + ' 天' }}。</p>
      <p v-if="record.state === 'OBSERVING'">客户仍处于观察期，不等同于高关注。</p>
      <p v-if="record.state === 'DATA_REVIEW'">请先核验数据，当前不能作为已确认的风险等级。</p>
      <p v-if="record.dataQuality.status !== 'CLEAN'" class="quality">包含需核验的数据，请先确认相关业务记录。</p>
      <el-button size="small" :disabled="!canAct" :aria-expanded="expanded.includes(record.customerId)"
        @click="toggleEvidence(record.customerId)">{{ expanded.includes(record.customerId) ? '收起判定依据' : '查看判定依据' }}</el-button>
      <el-button v-if="canSuggestCustomer(record)" size="small" :disabled="!canAct || pendingCustomers?.includes(record.customerId)"
        :loading="pendingCustomers?.includes(record.customerId)" @click="$emit('singleSuggestions', record.customerId)">获取跟进建议</el-button>
      <section v-if="canAct && expanded.includes(record.customerId)" class="evidence" aria-label="判定依据">
        <p>客户等级：{{ record.customerLevel || '未评级' }}；当前关注等级：{{ riskStatusLabel(record) }}</p>
        <p>最近有效跟进时间：{{ record.lastValidFollowAt ? formatAnalysisTime(record.lastValidFollowAt, result.analysisContext.timezone) : record.historyStatus === 'NEVER_FOLLOWED' ? '从未有效跟进' : '未提供，待核验' }}</p>
        <p>{{ record.anchorType === 'CUSTOMER_CREATED' ? '距建档至分析时间' : '距分析时间' }}：{{ record.elapsedDays == null ? '待核验' : record.elapsedDays + ' 天' }}；当前规则阈值：{{ record.thresholdDays == null ? '不适用' : record.thresholdDays + ' 天' }}</p>
        <p v-if="record.excessDays != null">超过阈值：{{ record.excessDays }} 天</p>
        <ul v-if="record.reasons.length"><li v-for="reason in record.reasons" :key="reason.code">{{ reason.message }}
          <small>原因证据：观测 {{ reason.evidence.elapsedDays ?? '待核验' }} 天；阈值 {{ reason.evidence.thresholdDays ?? '不适用' }} 天；时间依据 {{ formatAnalysisTime(reason.evidence.anchorTime, result.analysisContext.timezone) }}；等级组 {{ reason.evidence.levelGroup || '未提供' }}</small>
        </li></ul>
        <p v-else>未返回额外风险命中原因。</p>
        <p v-if="record.dataQuality.status !== 'CLEAN'">数据质量状态：{{ record.dataQuality.status || '未提供' }}；标记：{{ record.dataQuality.codes.join('、') || '未提供' }}</p>
      </section>
    </article>
    <div v-if="result.page" class="pagination">
      <p>匹配总数 {{ result.page.total }} 个，本页 {{ result.page.returned }} 个，第 {{ result.page.number }} 页。</p>
      <el-pagination :current-page="result.page.number" :page-size="result.page.size" :total="result.page.total"
        :page-sizes="[10,20,50]" layout="prev, pager, next, sizes" :pager-count="5" :disabled="!canAct || busy"
        @current-change="$emit('page', $event, result.page.size)" @size-change="$emit('page', 1, $event)" />
      <p class="notice">翻页会重新分析原问题和当前数据；如原问题请求建议，新页也会重新生成建议。</p>
    </div>
    <p v-if="!canAct" class="notice">风险交互需具备销售准入及 AI 分析、客户查询、跟进查询权限，最终以服务端校验为准。</p>
    <el-alert v-if="actionError" :title="actionError" type="warning" :closable="false" />
    <RiskAdvicePanel v-if="result.intent === 'RISK_SUGGESTIONS'" :advice="result.advice" :records="result.records" />
  </section>
</template>
<style scoped>
.answer { white-space: pre-wrap; line-height: 1.8; } .context { font-size: 12px; padding: 12px; background: #f8fafc; border-radius: 8px; color: #606266; }
.context p { margin: 6px 0; line-height: 1.7; } .context details { overflow-wrap: anywhere; }
.customer-card { margin-top: 12px; padding: 14px; border: 1px solid #e0e3e8; border-radius: 10px; font-size: 13px; }
.customer-card header { display: flex; justify-content: space-between; align-items: start; gap: 12px; }
.customer-card header > div { min-width: 0; overflow-wrap: anywhere; }
.customer-card strong { font-size: 15px; } .customer-card p, li { line-height: 1.8; }
.customer-card small, li small { display: block; color: #909399; margin-top: 4px; overflow-wrap: anywhere; }
.customer-card ul { padding-left: 18px; } .divider { margin: 0 10px; color: #c0c4cc; }
.evidence { margin-top: 10px; padding: 10px 12px; background: #f8fafc; border-radius: 8px; }
.quality { color: #9b6d24; } .pagination { margin-top: 16px; font-size: 13px; } .notice { font-size: 12px; color: #909399; line-height: 1.7; }
.pagination :deep(.el-pagination) { flex-wrap: wrap; row-gap: 8px; }
.single-result { padding-top: 14px; font-size: 13px; line-height: 1.8; overflow-wrap: anywhere; }
.single-heading { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.single-heading h3 { margin: 0; font-size: 16px; }
.single-meta { color: #606266; margin: 8px 0; } .single-reason { margin: 8px 0; }
.details-toggle { margin-top: 12px; } .single-details { margin-top: 10px; }
.single-details ul { padding-left: 18px; }
</style>
