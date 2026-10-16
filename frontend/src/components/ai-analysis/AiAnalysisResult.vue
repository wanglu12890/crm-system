<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, InfoFilled } from '@element-plus/icons-vue'
import type { AiAnalysisResult } from '@/types/aiAnalysis'
import { formatAnalysisTime, resultCopyText, scopeLabels, statisticsCondition, timeLabels } from '@/utils/aiAnalysis'

const props = defineProps<{ question: string; result: AiAnalysisResult }>()
const context = computed(() => props.result.analysisContext)
const gradeRows = computed(() => [
  { label: 'A级', value: props.result.metrics.levelA }, { label: 'B级', value: props.result.metrics.levelB },
  { label: 'C级', value: props.result.metrics.levelC }, { label: 'D级', value: props.result.metrics.levelD },
  { label: '未评级', value: props.result.metrics.unrated }
])
const totalLabel = computed(() => context.value.timeRange === 'ALL'
  ? '客户总量' : `${timeLabels[context.value.timeRange]}新增客户`)
const percentage = (count: number) => props.result.metrics.totalCustomers === 0
  ? 0 : Math.min(100, Math.max(0, count / props.result.metrics.totalCustomers * 100))

async function copyResult() {
  try {
    await navigator.clipboard.writeText(resultCopyText(props.question, props.result))
    ElMessage.success('分析结果已复制')
  } catch { ElMessage.error('复制失败，请检查浏览器剪贴板权限或手动复制。') }
}
</script>

<template>
  <section class="analysis-result" :class="{ 'grade-focused': result.intent === 'CUSTOMER_GRADE_DISTRIBUTION' }">
    <p class="answer">{{ result.answer }}</p>
    <div class="metric-cards">
      <div class="metric-card total-card"><span>{{ totalLabel }}</span><strong>{{ result.metrics.totalCustomers }} <small>个</small></strong></div>
      <div class="metric-card"><span>统计范围</span><strong class="scope-name">{{ scopeLabels[context.dataScope] }}</strong><span>{{ context.dataScope }} 范围</span></div>
    </div>
    <section v-if="result.intent === 'CUSTOMER_GRADE_DISTRIBUTION'" class="grade-section">
      <h3>客户等级分布</h3>
      <div v-for="grade in gradeRows" :key="grade.label" class="grade-row">
        <span>{{ grade.label }}</span>
        <el-progress :percentage="percentage(grade.value)" :show-text="false" :stroke-width="9" color="#7c9bc0" />
        <span class="grade-count">{{ grade.value }} 个</span>
      </div>
    </section>
    <section class="statistics-context">
      <h3><el-icon><InfoFilled /></el-icon>统计口径</h3>
      <p>时间范围：{{ timeLabels[context.timeRange] }}<template v-if="context.timeRange === 'ALL'">（无创建时间筛选）</template></p>
      <p v-if="context.timeRange !== 'ALL'">起止时间：{{ formatAnalysisTime(context.startTime, context.timezone) }} 至 {{ formatAnalysisTime(context.endTime, context.timezone) }}（含起、不含止）</p>
      <p>数据范围：{{ scopeLabels[context.dataScope] }}</p>
      <p>数据条件：{{ statisticsCondition }}</p>
      <p>时间筛选基于客户创建时间，而非跟进时间。</p>
      <p>参考时间：{{ formatAnalysisTime(context.referenceTime, context.timezone) }}（{{ context.timezone }}）</p>
    </section>
    <footer class="result-footer">
      <span>统计结果由 CRM 后端受控查询生成</span>
      <el-button round size="small" :icon="CopyDocument" @click="copyResult">复制结果</el-button>
    </footer>
  </section>
</template>

<style scoped>
.answer { white-space: pre-wrap; overflow-wrap: anywhere; margin: 12px 0 18px; line-height: 1.8; color: #303133; }
.metric-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 14px; }
.metric-card { display: flex; flex-direction: column; gap: 9px; padding: 15px; border: 1px solid #e0e3e8; border-radius: 10px; color: #606266; font-size: 14px; }
.metric-card strong { font-size: 30px; color: #20252d; }
.metric-card small { font-size: 14px; font-weight: 400; color: #606266; }
.metric-card .scope-name { font-size: 19px; }
.total-card { background: #f8fafc; }
.grade-focused .total-card { background: #fff; }
.grade-section { border: 1px solid #e0e3e8; border-radius: 10px; padding: 16px; }
h3 { color: #303133; font-size: 17px; margin: 0 0 16px; }
.grade-row { display: grid; grid-template-columns: 48px minmax(0, 1fr) auto; align-items: center; gap: 10px; margin-top: 12px; font-size: 14px; }
.grade-count { min-width: 42px; text-align: right; }
.statistics-context { margin-top: 26px; }
.statistics-context h3 { display: flex; align-items: center; gap: 8px; }
.statistics-context p { margin: 8px 0; font-size: 13px; line-height: 1.7; overflow-wrap: anywhere; }
.result-footer { display: flex; justify-content: space-between; flex-wrap: wrap; align-items: center; gap: 12px; margin-top: 24px; color: #909399; font-size: 13px; }
@media (max-width: 700px) { .metric-cards { grid-template-columns: 1fr; } .grade-section { padding: 12px; } .grade-row { gap: 6px; font-size: 12px; } }
</style>
