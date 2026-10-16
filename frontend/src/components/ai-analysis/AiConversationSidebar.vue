<script setup lang="ts">
import { ChatRound, Plus } from '@element-plus/icons-vue'
import type { AnalysisSession } from '@/types/aiAnalysis'
defineProps<{ sessions: AnalysisSession[]; activeId: number }>()
defineEmits<{ create: []; select: [id: number] }>()
</script>

<template>
  <aside class="conversation-sidebar" aria-label="分析会话">
    <button class="new-analysis" type="button" @click="$emit('create')">
      <el-icon :size="22"><Plus /></el-icon><span>新建分析</span>
    </button>
    <h2>最近的分析</h2>
    <nav class="session-list" aria-label="临时会话列表">
      <p v-if="sessions.length === 0" class="empty-history">暂无分析记录<br />提交问题后将在这里显示</p>
      <button v-for="session in sessions" :key="session.id" type="button"
        :class="['session-item', { active: session.id === activeId }]"
        :aria-current="session.id === activeId ? 'page' : undefined"
        :title="session.title" @click="$emit('select', session.id)">
        <el-icon><ChatRound /></el-icon><span>{{ session.title }}</span>
      </button>
    </nav>
    <p class="history-note">记录仅在当前页面保留，刷新后清空</p>
  </aside>
</template>

<style scoped>
.conversation-sidebar { display: flex; flex-direction: column; width: 26%; min-width: 190px; max-width: 280px; padding: 18px 12px 12px; border-right: 1px solid #eceef1; box-sizing: border-box; }
.new-analysis { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; min-height: 90px; border: 1px solid #dcdfe6; border-radius: 12px; background: #fff; color: #303133; cursor: pointer; font: inherit; }
h2 { margin: 20px 0 12px; color: #606266; font-size: 14px; font-weight: 400; }
.session-list { overflow-y: auto; flex: 1; min-height: 0; }
.session-item { display: flex; align-items: center; gap: 9px; width: 100%; margin-bottom: 8px; padding: 14px 10px; border: 0; border-radius: 7px; color: #303133; background: transparent; font: inherit; font-size: 14px; text-align: left; cursor: pointer; }
.session-item.active, .session-item:hover { background: #f3f5f7; }
.session-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty-history, .history-note { color: #909399; font-size: 12px; line-height: 1.8; }
.history-note { margin: 12px 0 0; }
@media (max-width: 900px) { .conversation-sidebar { width: 180px; min-width: 145px; } }
@media (max-width: 600px) { .conversation-sidebar { width: 120px; min-width: 100px; padding: 12px 8px; } .new-analysis { min-height: 72px; } .session-item { padding: 12px 4px; font-size: 12px; } }
</style>
