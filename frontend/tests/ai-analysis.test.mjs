import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'
import vuePlugin from '@vitejs/plugin-vue'
import { createRenderer, defineComponent, h, ssrContextKey } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import axios from 'axios'

// Existing toolchain, in-memory router and synthetic adapters; no browser/API/model calls.
const server = await createServer({
  configFile: false, root: fileURLToPath(new URL('../', import.meta.url)),
  server: { middlewareMode: true }, plugins: [vuePlugin()],
  resolve: { alias: [
    { find: /^vue-router$/, replacement: fileURLToPath(new URL('./memory-router.mjs', import.meta.url)) },
    { find: '@', replacement: fileURLToPath(new URL('../src', import.meta.url)) }
  ] }
})
after(() => server.close())
const storage = new Map([['crm_access_token', 'synthetic-test-token']])
globalThis.localStorage = {
  getItem: key => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value),
  removeItem: key => storage.delete(key)
}
const { useAuthStore } = await server.ssrLoadModule('/src/stores/auth.ts')
const { default: router } = await server.ssrLoadModule('/src/router/index.ts')
const { default: request } = await server.ssrLoadModule('/src/utils/request.ts')
const helpers = await server.ssrLoadModule('/src/utils/aiAnalysis.ts')
function authorize(permissions) {
  setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.currentUser = { id: 7, username: 'synthetic', realName: '测试用户', roles: ['SUPER_ADMIN'], permissions }
  auth.initialized = true
}
test('canonical and legacy routes require both permissions even for a SUPER_ADMIN role', async () => {
  for (const permissions of [[], ['ai:analysis'], ['customer:list'], ['ai:analysis', 'customer:list']]) {
    authorize(permissions)
    const expected = permissions.length === 2 ? '/admin/analytics/ai' : '/403'
    await router.push('/admin/analytics/ai')
    assert.equal(router.currentRoute.value.path, expected)
    await router.push('/admin/ai-analysis')
    assert.equal(router.currentRoute.value.path, expected)
  }
})
test('unauthenticated direct access uses the existing login redirect', async () => {
  await router.push('/admin/dashboard')
  storage.delete('crm_access_token')
  authorize(['ai:analysis', 'customer:list'])
  await router.push('/admin/analytics/ai')
  assert.equal(router.currentRoute.value.path, '/login')
  storage.set('crm_access_token', 'synthetic-test-token')
})
test('all required errors preserve the controlled ProblemDetail and have fallback feedback', () => {
  for (const status of [400, 401, 403, 409, 422, 500, 502, 503]) {
    const error = new axios.AxiosError('synthetic', undefined, undefined, undefined,
      { status, data: { detail: '后端受控拒绝信息' } })
    assert.equal(helpers.analysisErrorMessage(error), '后端受控拒绝信息')
    error.response.data = {}
    assert.ok(helpers.analysisErrorMessage(error).length > 0)
  }
})
const fixture = {
  answer: '本人范围内，全部时间客户共 0 个。', intent: 'CUSTOMER_TOTAL',
  metrics: { totalCustomers: 0, levelA: 0, levelB: 0, levelC: 0, levelD: 0, unrated: 0 },
  analysisContext: { dataScope: 'SELF', timeRange: 'ALL', startTime: null, endTime: null,
    referenceTime: '2026-10-14T12:00:00+08:00', timezone: 'Asia/Shanghai' }
}
test('copy text preserves zero counts, actual scope and unfiltered ALL time', () => {
  const text = helpers.resultCopyText('目前有多少客户？', fixture)
  for (const expected of [fixture.answer, '客户总量：0 个', '数据范围：本人客户', '全部时间（无创建时间筛选）', '不包含公海客户'])
    assert.ok(text.includes(expected))
  assert.ok(!text.includes('全公司'))
  assert.ok(!text.includes('null'))
})
const renderer = createRenderer({
  createElement: () => ({ children: [] }), createText: text => ({ text }), createComment: () => ({}),
  insert: (node, parent) => parent.children.push(node), remove: () => {},
  setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text },
  parentNode: () => null, nextSibling: () => null, patchProp: () => {}
})
test('duplicate submission, session switching and 422 cannot fabricate or misplace a result', async () => {
  authorize(['ai:analysis', 'customer:list'])
  const { default: Page } = await server.ssrLoadModule('/src/views/analysis/AiAnalysis.vue')
  let state
  const container = { children: [] }
  const Harness = defineComponent({ setup() {
    state = Page.setup({}, { expose() {} })
    return () => h('div')
  } })
  const app = renderer.createApp(Harness)
  app.provide(ssrContextKey, { modules: new Set() })
  app.mount(container)
  let resolveResponse, config
  let requests = 0
  request.defaults.adapter = input => {
    config = input
    requests++
    return new Promise(resolve => { resolveResponse = resolve })
  }
  try {
    state.activeSession.value.draft = '目前有多少客户？'
    const original = state.activeSession.value
    const inFlight = state.submitQuestion()
    await state.submitQuestion()
    await new Promise(resolve => setImmediate(resolve)) // Shared Axios async request interceptor.
    assert.equal(requests, 1)
    assert.equal(config.url, '/ai/analysis')
    assert.equal(config.timeout, 60_000)
    assert.deepEqual(JSON.parse(config.data), { question: '目前有多少客户？' })
    state.newAnalysis()
    const fresh = state.activeSession.value
    fresh.draft = '另一个完整问题'
    resolveResponse({ status: 200, statusText: 'OK', headers: {}, config, data: fixture })
    await inFlight
    assert.equal(fresh.exchanges.length, 0)
    assert.equal(fresh.draft, '另一个完整问题')
    assert.equal(original.exchanges[0].result.metrics.totalCustomers, 0)
    assert.equal(original.exchanges[0].question, '目前有多少客户？')
    assert.equal(original.draft, '')
    state.activeId.value = original.id
    original.draft = '全公司有多少客户？'
    request.defaults.adapter = input => Promise.reject(new axios.AxiosError('synthetic', undefined, input, undefined,
      { status: 422, data: { detail: '无法满足指定范围' } }))
    await state.submitQuestion()
    assert.equal(original.draft, '全公司有多少客户？')
    assert.equal(original.exchanges[1].status, 'error')
    assert.equal(original.exchanges[1].error, '无法满足指定范围')
    assert.equal(original.exchanges[1].result, undefined)
    assert.equal(storage.size, 1)
  } finally { app.unmount() }
})
