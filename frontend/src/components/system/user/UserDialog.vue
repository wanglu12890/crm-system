<!-- 用于管理系统用户的对话框组件-新建用户or编辑 -->
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { User, UserDialogMode, UserFormData } from '@/types/user'

// 定义props，用于接收父组件传递的数据
// 这里定义了三个 props：modelValue、mode 和 user。
// 什么是 modelValue？它是一个布尔值，表示对话框是否可见。
// 什么是 mode？它是一个字符串，表示对话框的模式，可以是 'create' 或 'edit'。
// 什么是 user？它是一个对象，表示当前编辑的用户信息，如果是新建用户，则为 null。
// 这里定义了两个 emits：'update:modelValue' 和 'save'。
// 'update:modelValue' 用于通知父组件对话框的可见性发生了变化，'save' 用于通知父组件用户表单数据已经保存。
// 什么是父组件？父组件是指使用这个对话框组件的组件，它可以通过 props 向子组件传递数据，也可以通过 emits 接收子组件发送的事件。
const props = defineProps<{
  modelValue: boolean
  mode: UserDialogMode
  user: User | null
}>()

// 定义 emits，用于向父组件发送事件。
// update:modelValue 是一个自定义事件，用于通知父组件对话框的可见性发生了变化。
// save 是一个自定义事件，用于通知父组件用户表单数据已经保存。
const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  save: [data: UserFormData]
}>()

// 定义一个 ref 类型的变量 formRef，用于获取表单组件的实例。
const formRef = ref<FormInstance>()

// 定义一个函数 createEmptyForm，用于创建一个空的用户表单数据对象。
const createEmptyForm = (): UserFormData => ({
  username: '',
  name: '',
  password: '',
  role: '',
  phone: '',
  status: '正常'
})

// 定义一个响应式对象 form，用于存储用户表单数据。
const form = reactive<UserFormData>(createEmptyForm())

// 定义一个计算属性 visible，用于获取和设置对话框的可见性。
// 当 visible 被设置为 true 时，对话框会显示；当 visible 被设置为 false 时，对话框会隐藏。
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

// 定义一个计算属性 title，用于根据对话框的模式显示不同的标题。
// 如果是新建用户模式，则显示“新增用户”；如果是编辑用户模式，则显示“编辑用户”。
const title = computed(() => (props.mode === 'create' ? '新增用户' : '编辑用户'))

// 定义一个函数 validatePassword，用于验证密码的合法性。
// 如果是新建用户模式，则密码不能为空；如果密码不为空，则长度必须在 6 到 64 个字符之间。
const validatePassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (props.mode === 'create' && !value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value && (value.length < 6 || value.length > 64)) {
    callback(new Error('密码长度应为 6 到 64 个字符'))
    return
  }
  callback() // 验证通过，调用 callback() 表示验证成功
}

// 定义表单验证规则 rules，用于对用户表单数据进行验证。
const rules: FormRules<UserFormData> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 64, message: '用户名长度应为 2 到 64 个字符', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 监听 props.modelValue 的变化，当对话框打开时，初始化表单数据。
watch(
  () => props.modelValue, // 监听对话框的可见性变化
  (opened) => {  // 当对话框打开时，初始化表单数据
    if (!opened) return  // 如果对话框关闭，则不做任何操作
    // 如果是编辑模式并且有用户数据，则将表单数据设置为当前用户的数据；否则，创建一个空的表单数据对象。
    Object.assign(
      form,
      props.mode === 'edit' && props.user  
        ? {
            id: props.user.id,
            username: props.user.username,
            name: props.user.name,
            password: '',
            role: props.user.role,
            phone: props.user.phone,
            status: props.user.status
          }
        : createEmptyForm()
    )
    formRef.value?.clearValidate() // 清除表单验证状态
  }
)

// 定义一个函数 handleSave，用于处理保存按钮的点击事件。
const handleSave = async () => {
  // 如果表单组件实例不存在，则直接返回。是指当前组件还没有渲染完成，或者表单组件还没有挂载到 DOM 上，就调用了 handleSave 函数，这时 formRef.value 可能是 null。
  if (!formRef.value) return
  // 调用表单组件的 validate 方法进行表单验证，如果验证失败，则直接返回。validate 方法会根据 rules 中定义的验证规则，对表单数据进行验证，如果有不符合规则的字段，则会触发验证失败的回调函数，并返回 false。
  const valid = await formRef.value.validate().catch(() => false)
  // 如果验证失败，则直接返回。
  if (!valid) return
  // 如果验证通过，则触发 save 事件，将表单数据传递给父组件。此时的父组件是 User.vue，它会根据对话框的模式来处理新增或编辑用户的逻辑。
  emit('save', { ...form })
}

// 定义一个函数 handleClosed，用于处理对话框关闭事件。
const handleClosed = () => {
  formRef.value?.resetFields() // 重置表单字段
  Object.assign(form, createEmptyForm()) // 
}
</script>

<template>
  <!-- 
   用户对话框组件，包含一个表单，用于新建或编辑用户信息。
   组件使用了 Element Plus 的 el-dialog、el-form、el-form-item、el-input、el-select、el-option 等组件来构建对话框和表单。
   组件通过 v-model 绑定 visible 属性来控制对话框的显示和隐藏
   组件通过 title 属性来显示对话框的标题，根据 mode 的值来决定是显示“新增用户”还是“编辑用户”
   组件通过 form 属性来绑定表单数据对象，使用 reactive 创建响应式对象
   组件通过 rules 属性来绑定表单验证规则，使用 FormRules 类型定义验证规则
   组件通过 ref 获取表单组件的实例，用于调用 validate、resetFields、clearValidate 等方法
   组件通过 emits 触发 update:modelValue 和 save 事件，通知父组件对话框的可见性变化和用户表单数据保存
   组件通过 @closed 监听对话框关闭事件，调用 handleClosed 方法重置表单数据
   组件通过 #footer 插槽自定义对话框的底部按钮，包含取消和保存按钮
    -->
  <el-dialog
    v-model="visible"
    :title="title"
    width="min(520px, calc(100vw - 32px))"
    destroy-on-close
    append-to-body
    @closed="handleClosed"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="form.username" placeholder="请输入用户名" maxlength="64" />
      </el-form-item>

      <el-form-item label="真实姓名" prop="name">
        <el-input v-model.trim="form.name" placeholder="请输入真实姓名" maxlength="64" />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          :placeholder="mode === 'create' ? '请输入密码' : '留空表示不修改密码'"
          show-password
          maxlength="64"
          autocomplete="new-password"
        />
      </el-form-item>

      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" placeholder="请选择角色" class="user-dialog__select">
          <el-option label="管理员" value="管理员" />
          <el-option label="销售经理" value="销售经理" />
          <el-option label="销售人员" value="销售人员" />
        </el-select>
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model.trim="form.phone" placeholder="请输入手机号" maxlength="11" />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" class="user-dialog__select">
          <el-option label="正常" value="正常" />
          <el-option label="停用" value="停用" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <!-- 点击取消按钮，visible属性值变为false，对话框关闭 -->
      <el-button @click="visible = false">取消</el-button> 
      <!-- 点击保存按钮，type值为primary，表示主要按钮；同时触发保存事件 -->
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.user-dialog__select {
  width: 100%;
}
</style>
