<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>

<template>
  <section class="page-card">
    <h1>{{ title }}</h1>
    <p>该模块将在后续业务阶段实现。</p>
  </section>
</template>

<style scoped>
.page-card {
  min-height: 160px;
  padding: 24px;
  border: 1px solid #eaecf0;
  border-radius: 10px;
  background: #fff;
}

h1 {
  margin: 0 0 10px;
  color: #172033;
  font-size: 22px;
}

p {
  margin: 0;
  color: #667085;
}
</style>
