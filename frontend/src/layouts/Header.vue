<script setup lang="ts">
const handleLogout = () => {
  console.log('退出登录')
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <!-- <div class="app-header__logo" aria-label="Logo 占位">CRM</div> -->
      <div class="brand-logo-wrapper" aria-hidden="true">
          <img
            src="@/assets/images/yuyan_crm_logo.svg"
            alt="雨燕科技CRM系统"
          />
        </div>
      <span class="app-header__name">雨燕科技CRM系统</span>
    </div>

    <div class="app-header__user">
      <span class="app-header__username">当前用户：admin</span>
      <el-button plain size="small" @click="handleLogout">退出登录</el-button>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid #e5eaf2;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgb(15 23 42 / 4%);
}

.app-header__brand,
.app-header__user {
  display: flex;
  align-items: center;
}

.app-header__brand {
  min-width: 0;
  gap: 12px;
}

.app-header__logo {
  display: inline-flex;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  border-radius: 9px;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
}

.app-header__name {
  overflow: hidden;
  color: #172033;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.app-header__user {
  flex: 0 0 auto;
  gap: 16px;
}

.app-header__username {
  color: #475467;
  font-size: 14px;
}

.brand-logo-wrapper {
  display: inline-flex;
  width: 60px;
  height: 60px;
  border-radius: 14px;
  align-items: center;
  justify-content: center;
  padding: 2px; /* 给logo留些内边距 */
}

.brand-logo-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

@media (max-width: 640px) {
  .app-header {
    height: 56px;
    padding: 0 12px;
  }

  .app-header__name {
    font-size: 16px;
  }

  .app-header__username {
    display: none;
  }
}
</style>
