<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const backToDashboard = () => router.push('/admin/dashboard')
</script>

<template>
  <main class="forbidden-page">
    <section class="forbidden-card">
      <div class="forbidden-code">403</div>
      <h1>无权访问该页面</h1>
      <p>当前账号没有访问该功能的权限。</p>
      <el-button type="primary" @click="backToDashboard">返回首页</el-button>
    </section>
  </main>
</template>

<style scoped>
.forbidden-page {
  display: flex;
  min-height: 100vh;
  padding: 24px;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.forbidden-card {
  width: 100%;
  max-width: 460px;
  padding: 48px 32px;
  border-radius: 12px;
  text-align: center;
  background: #fff;
  box-shadow: 0 8px 28px rgb(15 23 42 / 8%);
}

.forbidden-code {
  color: #2563eb;
  font-size: 72px;
  font-weight: 700;
  line-height: 1;
}

h1 {
  margin: 20px 0 10px;
  color: #1f2937;
  font-size: 24px;
}

p {
  margin: 0 0 28px;
  color: #6b7280;
  font-size: 14px;
}
</style>
