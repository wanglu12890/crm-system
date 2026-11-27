import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import * as vue from 'vue'
import axios from 'axios'
const flush = () => new Promise(resolve => setImmediate(resolve))
function setup(fail = false) {
 const props = vue.reactive({ modelValue: false, customerId: '9223372036854775806' })
 const calls = [], events = []
 const script = fs.readFileSync(new URL('../src/components/business/customer/CustomerFormDialog.vue', import.meta.url), 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
 const context = { exports: {}, defineProps: () => props, defineEmits: () => (...args) => events.push(args),
  require: name => name === 'vue' ? vue : name === 'axios' ? { default: axios } : name === 'element-plus' ? { ElMessage: { success() {}, error() {} } } : name === '@/api/customer' ? {
   getCustomerDetail: async () => ({ data: { customerName: 'Current', customerType: 'ENTERPRISE', status: 'ACTIVE', version: 4 } }),
   updateCustomer: async (...args) => { calls.push(args); if(fail) throw { isAxiosError: true, response: { status: 409, data: {} } } },
   createCustomer: async () => { throw Error('must not create') }
  } : { detailError: () => 'error' }
 }
 const compiled = ts.transpileModule(script + '\n;globalThis.state = { form, formRef, submitting, conflict, handleSubmit };', { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
 vm.runInNewContext(compiled, context)
 props.modelValue = true
 return { props, calls, events, state: context.state }
}
test('edit loads latest version and submits whitelist, then closes and emits success', async () => {
 const { calls, events, state } = setup(); await flush()
 state.formRef.value = { validate: async () => true, clearValidate() {} }
 assert.equal(state.form.customerName, 'Current')
 state.form.customerName = 'Edited'
 await state.handleSubmit()
 assert.equal(calls[0][0], '9223372036854775806'); assert.equal(calls[0][1].version, 4)
 assert.equal(calls[0][1].customerName, 'Edited'); assert.equal('ownerId' in calls[0][1], false)
 assert.ok(events.some(e => e[0] === 'success')); assert.ok(events.some(e => e[0] === 'update:modelValue' && e[1] === false))
})
test('version conflict preserves input and does not emit success', async () => {
 const { events, state } = setup(true); await flush()
 state.formRef.value = { validate: async () => true, clearValidate() {} }
 state.form.customerName = 'Keep my changes'; await state.handleSubmit()
 assert.equal(state.form.customerName, 'Keep my changes'); assert.equal(state.conflict.value, true)
 assert.equal(events.some(e => e[0] === 'success'), false)
})

test('list edit entry selects customer and successful save refreshes detail and list', () => {
 const source = fs.readFileSync(new URL('../src/views/business/Customer.vue', import.meta.url), 'utf8')
 const handler = source.match(/const handleEdit = [\s\S]*?(?=\nonMounted)/)[0]
 const context = { editCustomerId: vue.ref(), createDialogVisible: vue.ref(false) }
 vm.runInNewContext(ts.transpileModule(handler + '\n;handleEdit({id:"9223372036854775806"})', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
 assert.equal(context.editCustomerId.value, '9223372036854775806'); assert.equal(context.createDialogVisible.value, true)
 assert.match(source, /detailRefreshKey.value\+\+/); assert.match(source, /await loadCustomers\(\)/)
 assert.match(source, /requestVersion !== listRequestVersion/)
})
