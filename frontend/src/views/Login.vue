<script setup lang="ts">
import { reactive, ref } from 'vue' // 引入 Vue 的响应式 API，用于创建响应式对象和引用。
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'  // 引入 Element Plus 的消息提示组件和类型定义，用于表单验证和消息提示。
import axios from 'axios'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { setAuthToken } from '@/utils/auth'
import type { ApiProblemDetail } from '@/utils/request'

// 什么是响应式？ 响应式是 Vue 3 中的一个特性，它允许我们创建响应式数据，当数据发生变化时，相关的视图会自动更新。
// 什么是ref？ref 是 Vue 3 中的一个函数，用于创建一个响应式引用，它可以存储任何类型的值，并在值发生变化时触发视图更新。
// 什么是reactive？reactive 是 Vue 3 中的一个函数，用于创建一个响应式对象，它会递归地将对象的属性转换为响应式的，当属性发生变化时，相关的视图会自动更新。
// ref 和 reactive 的区别：ref 用于基本类型和对象的引用，而 reactive 用于对象的响应式处理。ref 返回一个包含 value 属性的对象，而 reactive 返回一个代理对象。

// 定义登录表单数据的接口，包含用户名和密码字段。
interface LoginForm { 
  username: string
  password: string
}

// 创建一个空的ref，用来存储表单组件实例，以便在提交表单时调用验证方法。
const formRef = ref<FormInstance>() 
const router = useRouter()

// 1.响应式数据。
const submitting = ref(false)  // 提交状态，防止重复提交。初始： false，表示未提交。

// 登录表单数据和验证规则
const form = reactive<LoginForm>({ // 对象响应式处理，包含用户名和密码字段。
  username: '',
  password: ''
})

// 定义表单验证规则，确保用户名和密码符合要求
const rules: FormRules<LoginForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 64, message: '用户名长度应为 2 到 64 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度应为 6 到 64 个字符', trigger: 'blur' }
  ]
}

// 登录处理函数，验证表单并调用后端认证接口
const handleLogin = async () => {
  // 通过.value访问实际的DOM元素或组件实例。
  if (!formRef.value || submitting.value) return // 如果表单引用不存在或正在提交，则直接返回，防止重复提交。

  // 调用Element Plus的表单验证方法。
  const valid = await formRef.value.validate().catch(() => false) // 调用表单验证方法，如果验证失败则返回 false。
  if (!valid) return // 如果表单验证不通过，则直接返回。

  // 数据变化
  submitting.value = true // 设置提交状态为 true，表示正在提交。
  try {
    const response = await login({
      username: form.username,
      password: form.password
    })
    setAuthToken(response.data)
    await router.replace('/admin/dashboard')
  } catch (error: unknown) {
    const detail = axios.isAxiosError<ApiProblemDetail>(error)
      ? error.response?.data?.detail
      : undefined
    const message = detail || (axios.isAxiosError(error) && !error.response
      ? '无法连接服务器，请确认后端服务已启动'
      : '登录失败，请稍后重试')
    ElMessage.error(message)
  } finally {
    // 4.在 finally 块中，无论请求成功还是失败，都会将提交状态重置为 false，确保按钮恢复可点击状态。
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <div class="login-page__decoration login-page__decoration--top" aria-hidden="true" />
    <div class="login-page__decoration login-page__decoration--bottom" aria-hidden="true" />

    <section class="login-card" aria-labelledby="login-title">
      <header class="login-card__header">
        <!-- <div class="brand-mark" aria-hidden="true">CRM</div> -->
        <div class="brand-logo-wrapper" aria-hidden="true">
          <img
            src="@/assets/images/yuyan_crm_logo.svg"
            alt="雨燕科技CRM系统"
          />
        </div>
        <h1 id="login-title">雨燕科技CRM系统</h1>
        <p>企业客户关系管理平台</p>
      </header>

      <!-- ref绑定：Vue 会自动把组件实例赋值给 formRef.value -->
      <!-- 使用Element Plus组件库构建的表单，包含表单容器、两个表单项和一个提交按钮 -->
       <!-- 
        属性=“值” 的含义：
        1. ref="formRef"：【模板引用】绑定表单组件实例到 formRef，用于调用表单方法（如 validate()）在 script 中通过 formRef.value 访问。
        2. :model="form"：【数据绑定】绑定表单数据对象 form（带：是动态绑定），实现双向数据绑定。表单数据对象，在 script 中定义表单所有输入框的值都存储在这个对象里。
        3. :rules="rules"：【验证规则】绑定表单验证规则 rules，用于验证用户名和密码的输入。
        4. label-position="top"：【标签位置】设置表单标签的位置为顶部（top），其他选项包括 left（左侧）和 right（右侧）。
        5. size="large"：【组件大小】设置表单组件的大小为大号。
        6. class="login-form"：【样式类】为表单添加自定义样式类。
        7. @keyup.enter="handleLogin"：【事件监听】监听回车键事件（键盘事件），在表单内按回车键触发登录处理函数 handleLogin。
       -->
      <el-form
        ref="formRef"  
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <!-- 
        1. el-form-item：Element Plus 表单项组件，用于包裹输入框和标签。
        2. label="用户名"：设置表单项的标签为“用户名”。
        3. prop="username"：指定表单项对应的字段名，用于表单验证和数据绑定。
        4. el-input：Element Plus 输入框组件，用于接收用户输入。
        5. v-model.trim="form.username"：使用 v-model 指令实现双向数据绑定，并在输入时去除首尾空格，绑定到 form 对象的 username 字段。
        6. placeholder="请输入用户名"：设置输入框的占位符文本。
        7. autocomplete="username"：启用浏览器的自动完成功能，提示用户名。
        8. clearable：允许用户清除输入框内容。
        9. maxlength="64"：限制输入框的最大字符长度为 64 个字符。
        10. el-form-item 和 el-input 的组合实现了一个完整的表单项，用户可以输入用户名，并且在提交表单时会
       -->
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model.trim="form.username"
            placeholder="请输入用户名"
            autocomplete="username"
            clearable
            maxlength="64"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            show-password
            maxlength="64"
          />
        </el-form-item>

        <!-- 6. 提交按钮。页面自动反映数据变化 -->
         <!-- 
        1. el-button：Element Plus 按钮组件，用于提交表单。
        2. type="primary"：设置按钮的类型为主要按钮，通常用于提交操作。
        3. class="login-form__submit"：为按钮添加自定义样式类。
        4. :loading="submitting"：绑定按钮的加载状态到 submitting 响应式变量，当提交表单时显示加载状态，防止重复提交。
        5. @click="handleLogin"：监听按钮的点击事件，触发登录处理函数 handleLogin。
        6. 按钮文本为“登录”，用户点击后会触发表单验证和提交逻辑。
         -->
        <el-button
          type="primary"
          class="login-form__submit"
          :loading="submitting"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <p class="login-card__help">账号由管理员创建，如需开通请联系管理员</p>
    </section>

    <footer class="login-footer">© 2026 雨燕科技</footer>
  </main>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  margin: 0;
  min-width: 320px;
  color: #1d2939;
  font-family:
    Inter, "PingFang SC", "Microsoft YaHei", system-ui, -apple-system,
    BlinkMacSystemFont, "Segoe UI", sans-serif;
  background: #f3f6fb;
}

.login-page {
  position: relative;
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 48px 24px 72px;
  overflow: hidden;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(135deg, rgb(238 245 255 / 92%), rgb(247 249 252 / 96%)),
    radial-gradient(circle at 50% 0%, #dbeafe 0, transparent 55%);
}

.login-page::before {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgb(37 99 235 / 3%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(37 99 235 / 3%) 1px, transparent 1px);
  background-size: 32px 32px;
  content: "";
  mask-image: linear-gradient(to bottom, black, transparent 80%);
  pointer-events: none;
}

.login-page__decoration {
  position: absolute;
  width: min(36vw, 520px);
  aspect-ratio: 1;
  border-radius: 50%;
  filter: blur(4px);
  opacity: 0.42;
  pointer-events: none;
}

.login-page__decoration--top {
  top: -24%;
  right: -10%;
  background: linear-gradient(145deg, #bfdbfe, #e0e7ff);
}

.login-page__decoration--bottom {
  bottom: -30%;
  left: -12%;
  background: linear-gradient(145deg, #dbeafe, #cffafe);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 440px;
  padding: 42px 44px 34px;
  border: 1px solid rgb(255 255 255 / 82%);
  border-radius: 16px;
  background: rgb(255 255 255 / 96%);
  box-shadow:
    0 24px 64px rgb(15 23 42 / 10%),
    0 4px 16px rgb(15 23 42 / 5%);
  backdrop-filter: blur(12px);
}

.login-card__header {
  margin-bottom: 32px;
  text-align: center;
}

/* .brand-mark {
  display: inline-flex;
  width: 56px;
  height: 56px;
  margin-bottom: 18px;
  border-radius: 14px;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.06em;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  box-shadow: 0 10px 22px rgb(37 99 235 / 25%);
} */

.brand-logo-wrapper {
  display: inline-flex;
  width: 60px;
  height: 60px;
  /* margin-bottom: 18px; */
  border-radius: 14px;
  align-items: center;
  justify-content: center;
  /* background: linear-gradient(135deg, #2563eb, #1d4ed8); */
  /* box-shadow: 0 10px 22px rgb(37 99 235 / 25%); */
  padding: 2px; /* 给logo留些内边距 */
}

.brand-logo-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.login-card__header h1 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  font-weight: 650;
  letter-spacing: 0.02em;
  line-height: 1.4;
}

.login-card__header p {
  margin: 8px 0 0;
  color: #7a8699;
  font-size: 14px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #344054;
  font-weight: 500;
  line-height: 1.3;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d9e0e9 inset;
  transition: box-shadow 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #aeb9c8 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2563eb inset;
}

.login-form__submit {
  width: 100%;
  height: 44px;
  margin-top: 6px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  background: #2563eb;
  box-shadow: 0 8px 18px rgb(37 99 235 / 18%);
}

.login-form__submit:hover,
.login-form__submit:focus {
  background: #1d4ed8;
}

.login-card__help {
  margin: 24px 0 0;
  color: #8a94a6;
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}

.login-footer {
  position: absolute;
  z-index: 1;
  bottom: 24px;
  color: #98a2b3;
  font-size: 12px;
}

@media (max-width: 600px) {
  .login-page {
    padding: 24px 16px 64px;
  }

  .login-card {
    padding: 34px 24px 28px;
    border-radius: 14px;
  }

  .login-card__header {
    margin-bottom: 28px;
  }

  .login-card__header h1 {
    font-size: 22px;
  }

  .login-page__decoration {
    width: 72vw;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-form :deep(.el-input__wrapper) {
    transition: none;
  }
}
</style>
