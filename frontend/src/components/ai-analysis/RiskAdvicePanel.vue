<script setup lang="ts">
import type { RiskAdvice, RiskRecord } from '@/types/aiAnalysis'
defineProps<{ advice: RiskAdvice; records: RiskRecord[]; compact?: boolean }>()
</script>
<template>
  <section class="advice-panel" aria-label="跟进建议">
    <h3>AI 辅助跟进建议</h3>
    <template v-if="advice.status === 'AVAILABLE'">
      <ol v-if="compact" class="compact-advice"><li v-for="item in advice.items" :key="item.customerId">{{ item.suggestion }}</li></ol>
      <p class="notice">{{ compact ? 'AI 辅助建议仅供参考，请人工核实后决定是否执行。' : '建议由 AI 选择动作、系统模板生成，仅供参考，请人工核实后决定是否执行。' }}</p>
      <template v-if="!compact"><article v-for="item in advice.items" :key="item.customerId" class="advice-item">
        <strong>{{ records.find(record => record.customerId === item.customerId)?.customerName || '客户' }} · {{ item.customerId }}</strong>
        <p>{{ item.suggestion }}</p>
        <p class="facts">事实依据：{{ records.find(record => record.customerId === item.customerId)?.reasons.map(reason => reason.message).join('；') || '当前关注状态，无额外风险命中原因' }}</p>
      </article></template>
    </template>
    <el-alert v-else-if="advice.status === 'UNAVAILABLE'" title="风险分析已完成，AI 跟进建议暂不可用。可稍后重新查询。" type="warning" :closable="false" show-icon />
    <p v-else-if="advice.status === 'NOT_APPLICABLE'" class="notice">{{ advice.message || '本页没有适用客户，无需生成跟进建议。' }}</p>
    <p v-else class="notice">本次未请求 AI 跟进建议。</p>
  </section>
</template>
<style scoped>
.advice-panel { margin-top: 18px; } h3 { font-size: 15px; margin: 0 0 10px; }
.notice, .facts { font-size: 12px; color: #606266; line-height: 1.7; }
.advice-item { border-left: 3px solid #8ca7c8; padding: 8px 12px; margin-top: 12px; background: #f8fafc; }
.advice-item strong { font-size: 13px; overflow-wrap: anywhere; } .advice-item p { white-space: pre-wrap; line-height: 1.8; font-size: 13px; }
.compact-advice { padding-left: 22px; margin: 8px 0; } .compact-advice li { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; margin: 6px 0; }
</style>
