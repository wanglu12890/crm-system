<script setup lang="ts">
import { Right } from '@element-plus/icons-vue'
defineProps<{ disabled: boolean }>()
defineEmits<{ choose: [question: string] }>()
const questions = ['目前一共有多少客户？', '本月新增了多少客户？', '近30天客户等级分布怎么样？', '本人有哪些客户需要重点关注？', '给出本人需要重点关注客户的跟进建议。']
</script>

<template>
  <section class="welcome-panel">
    <div class="welcome-heading">
      <svg class="brain-icon" viewBox="0 0 32 32" fill="none" aria-hidden="true">
        <path d="M16 7c-1-5-7-4-7 0-4 0-5 5-3 7-4 3-2 8 2 8-1 5 6 7 8 3V7Zm0 0c1-5 7-4 7 0 4 0 5 5 3 7 4 3 2 8-2 8 1 5-6 7-8 3M9 7v5m-3 2h5m-3 8h4m11-15v5m3 2h-5m3 8h-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
      <h2>CRM AI 销售分析助手</h2>
      <p>通过自然语言查询客户统计、跟进关注状态与辅助建议</p>
    </div>
    <h3>试试这些问题</h3>
    <button v-for="question in questions" :key="question" class="question-card" type="button"
      :disabled="disabled" @click="$emit('choose', question)">
      <span>{{ question }}</span><el-icon><Right /></el-icon>
    </button>
  </section>
</template>

<style scoped>
.welcome-panel { padding: 24px 0 8px; }
.welcome-heading { text-align: center; margin: 8px 0 30px; }
.brain-icon { width: 34px; height: 34px; color: #303133; }
h2 { margin: 12px 0; font-size: 24px; color: #20252d; }
p { font-size: 14px; color: #606266; }
h3 { margin: 0 0 14px; color: #606266; font-weight: 400; font-size: 14px; }
.question-card { width: 100%; display: flex; justify-content: space-between; align-items: center; gap: 12px; min-height: 64px; margin-bottom: 14px; padding: 16px; border: 1px solid #e0e3e8; border-radius: 10px; background: #fff; color: #303133; font: inherit; text-align: left; cursor: pointer; }
.question-card:hover:not(:disabled) { border-color: #7b9bc2; background: #fafcff; }
.question-card:disabled { cursor: not-allowed; opacity: .65; }
@media (max-width: 600px) { h2 { font-size: 19px; } .welcome-heading { margin-bottom: 20px; } .question-card { padding: 12px; } }
</style>
