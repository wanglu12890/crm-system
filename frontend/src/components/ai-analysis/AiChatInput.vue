<script setup lang="ts">
import { computed, ref } from 'vue'
import { Top } from '@element-plus/icons-vue'
const props = defineProps<{ modelValue: string; busy: boolean; disabled: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: string]; submit: [] }>()
const input = ref<{ focus: () => void }>()
const canSubmit = computed(() => !props.busy && !props.disabled && props.modelValue.trim().length > 0 && props.modelValue.length <= 1000)
defineExpose({ focus: () => input.value?.focus() })
</script>

<template>
  <form class="chat-input" @submit.prevent="canSubmit && emit('submit')">
    <el-input ref="input" :model-value="modelValue" type="textarea" :rows="3" :maxlength="1000"
      :disabled="disabled" resize="none" aria-label="业务分析问题"
      placeholder="请输入需要分析的业务问题..." @update:model-value="emit('update:modelValue', $event)" />
    <div class="input-footer">
      <span>分析范围由账号权限决定</span>
      <span class="input-count">{{ modelValue.length }}/1000</span>
      <el-button native-type="submit" round :loading="busy" :disabled="!canSubmit">
        <el-icon v-if="!busy"><Top /></el-icon>{{ busy ? '分析中' : '发送' }}
      </el-button>
    </div>
    <p class="independent-note">每次分析相互独立，请输入完整问题。</p>
  </form>
</template>

<style scoped>
.chat-input { border: 1px solid #e0e3e8; border-radius: 11px; padding: 10px; background: #fff; }
.chat-input :deep(.el-textarea__inner) { box-shadow: none; padding: 8px 6px; font-size: 15px; }
.input-footer { display: flex; gap: 10px; align-items: center; padding-top: 8px; font-size: 12px; color: #909399; flex-wrap: wrap; }
.input-count { margin-left: auto; }
.input-footer .el-icon { margin-right: 4px; }
.independent-note { margin: 6px 0 0; font-size: 12px; color: #909399; }
</style>
