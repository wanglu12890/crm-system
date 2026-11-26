import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { reactive, ref, computed, watch } from 'vue'
const source = fs.readFileSync(new URL('../src/components/business/customer/CustomerDetailDrawer.vue', import.meta.url), 'utf8')
function setup(permissions = ['customer:list', 'contact:list', 'follow:list']) {
  const props = reactive({ modelValue: false, customerId: '' })
  const pending = { detail: [], contacts: [], follow: [] }
  const api = key => (...args) => new Promise((resolve, reject) => pending[key].push({ args, resolve, reject }))
  let script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  script = script.replace(/^import .*$/gm, '')
  script += '\n;globalThis.state = { detail, contacts, records, page, total, error, contactsError, followError, loadFollow };'
  const context = { ref, computed, watch, defineProps: () => props, defineEmits: () => () => {},
    useAuthStore: () => ({ hasPermission: p => permissions.includes(p) }),
    getCustomerDetail: api('detail'), getCustomerContacts: api('contacts'), getCustomerFollowRecords: api('follow'), detailError: () => 'error' }
  vm.runInNewContext(ts.transpileModule(script, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { props, pending, state: context.state }
}
const flush = async () => { await new Promise(resolve => setImmediate(resolve)) }
test('customer switch and close reject stale responses, preserving string IDs', async () => {
  const { props, pending, state } = setup()
  props.customerId = '9223372036854775806'; props.modelValue = true; await flush()
  assert.equal(pending.detail[0].args[0], '9223372036854775806')
  props.customerId = 'B'; await flush()
  pending.detail[1].resolve({ data: { id: 'B' } }); await flush()
  pending.detail[0].resolve({ data: { id: 'A' } }); await flush()
  assert.equal(state.detail.value.id, 'B')
  props.modelValue = false; await flush()
  pending.contacts[1].resolve({ data: [{ id: 'old' }] }); await flush()
  assert.equal(state.contacts.value.length, 0)
  assert.equal(state.detail.value, null)
})
test('permissions prevent unauthorized child requests', async () => {
  const { props, pending } = setup(['customer:list'])
  props.customerId = '1'; props.modelValue = true; await flush()
  assert.equal(pending.detail.length, 1)
  assert.equal(pending.contacts.length, 0); assert.equal(pending.follow.length, 0)
})
test('pagination rejects stale page and child failure stays independent', async () => {
  const { props, pending, state } = setup()
  props.customerId = '1'; props.modelValue = true; await flush()
  state.loadFollow(2)
  pending.follow[1].resolve({ data: { records: [{ id: 'page2' }], page: 2, size: 10, total: 20 } }); await flush()
  pending.follow[0].resolve({ data: { records: [{ id: 'page1' }], page: 1, size: 10, total: 20 } })
  pending.contacts[0].reject(new Error('failure')); await flush()
  assert.equal(state.page.value, 2); assert.equal(state.records.value[0].id, 'page2')
  assert.equal(state.contactsError.value, 'error')
})
