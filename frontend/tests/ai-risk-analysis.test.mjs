import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { readFileSync } from 'node:fs'
import { createServer, transformWithEsbuild } from 'vite'
import { parse, compileScript } from '@vue/compiler-sfc'
import vuePlugin from '@vitejs/plugin-vue'
import { createRenderer, createSSRApp, defineComponent, h, nextTick, ssrContextKey } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import axios from 'axios'
import { ID, record, risk, statistic } from './ai-risk-fixtures.mjs'

const server = await createServer({
  configFile: false, root: fileURLToPath(new URL('../', import.meta.url)),
  server: { middlewareMode: true }, plugins: [vuePlugin(), {
    name: 'risk-interaction-render',
    resolveId(id) { if (['risk-interaction-render','risk-advice-interaction-render'].includes(id)) return '\0' + id + '.ts' },
    async load(id) {
      if (!['\0risk-interaction-render.ts','\0risk-advice-interaction-render.ts'].includes(id)) return
      const file = id === '\0risk-interaction-render.ts' ? 'RiskAnalysisResult.vue' : 'RiskAdvicePanel.vue'
      const source = readFileSync(new URL('../src/components/ai-analysis/' + file, import.meta.url), 'utf8')
      const { descriptor } = parse(source)
      const script = compileScript(descriptor, { id: 'risk-interaction', inlineTemplate: true })
      return (await transformWithEsbuild(script.content.replace('./RiskAdvicePanel.vue', 'risk-advice-interaction-render'), 'risk-interaction.ts', { loader: 'ts' })).code
    }
  }],
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
const { default: request } = await server.ssrLoadModule('/src/utils/request.ts')
request.interceptors.request.use(config => {
  assert.equal(config.url, '/ai/analysis', 'All chat actions must use the unified endpoint, never structured S2-A')
  return config
})
const { useAuthStore } = await server.ssrLoadModule('/src/stores/auth.ts')
const { useAiAnalysis } = await server.ssrLoadModule('/src/components/ai-analysis/useAiAnalysis.ts')
const protocol = await server.ssrLoadModule('/src/utils/aiAnalysisResponse.ts')
const helpers = await server.ssrLoadModule('/src/utils/riskAnalysis.ts')
const errors = await server.ssrLoadModule('/src/utils/aiAnalysis.ts')
const { default: RiskComponent } = await server.ssrLoadModule('/src/components/ai-analysis/RiskAnalysisResult.vue')
const { default: InteractiveRiskComponent } = await server.ssrLoadModule('risk-interaction-render')
const renderer = createRenderer({
  createElement: tag => ({ tag, props: {}, children: [] }), createText: text => ({ text }), createComment: () => ({}),
  insert(node, parent, anchor) {
    if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
    node.parent = parent
    const index = anchor ? parent.children.indexOf(anchor) : -1
    if (index < 0) parent.children.push(node); else parent.children.splice(index, 0, node)
  },
  remove(node) { if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1) },
  setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text; node.children = [] },
  parentNode: node => node.parent, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1],
  patchProp: (node, key, before, value) => { node.props[key] = value }
})
function mounted(permissions = ['ai:analysis','customer:list','follow:list'], roles = ['SALES_STAFF']) {
  setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.currentUser = { id: 7, username: 'synthetic', realName: '合成用户', roles, permissions }
  auth.initialized = true
  let state
  const app = renderer.createApp(defineComponent({ setup() { state = useAiAnalysis(); return () => h('div') } }))
  app.provide(ssrContextKey, { modules: new Set() })
  app.mount({ children: [] })
  return { state, auth, close: () => app.unmount() }
}
const tick = () => new Promise(resolve => setImmediate(resolve))
const reply = data => config => Promise.resolve({ status: 200, statusText: 'OK', headers: {}, config, data })
const fail = (status, code) => config => Promise.reject(new axios.AxiosError('synthetic', undefined, config, undefined,
  { status, data: { code } }))
async function send(state, data = risk(), question = '查看重点关注客户') {
  request.defaults.adapter = reply(data)
  state.activeSession.value.draft = question
  await state.submitQuestion()
  return state.activeSession.value.exchanges.at(-1)
}
async function html(result, request) {
  const app = createSSRApp({ render: () => h(RiskComponent, { result, request, canAct: true, busy: false }) })
  for (const name of ['el-tag','el-alert','el-empty','el-button','el-pagination'])
    app.component(name, defineComponent({ props: ['title','description'], setup(props, context) {
      return () => h('div', [props.title, props.description, context.slots.default?.()])
    } }))
  return renderToString(app)
}
test('union uses backend intent and accepts V1 omitted nullable ALL window', () => {
  assert.equal(protocol.parseAnalysisResponse(statistic).analysisContext.startTime, null)
  for (const intent of ['RISK_LIST','RISK_REASON','RISK_SUGGESTIONS']) {
    const result = protocol.parseAnalysisResponse(risk(intent))
    assert.equal(protocol.isRiskResult(result), true)
    assert.equal(result.intent, intent)
  }
})
test('malformed required fields, numeric IDs and mismatched object selection are rejected', () => {
  const mutations = [
    value => { delete value.records }, value => { value.records[0].customerId = 7 },
    value => { value.records[0].customerId = '01' }, value => { value.records[0].customerId = '9223372036854775808' },
    value => { value.records.push(value.records[0]); value.page.returned = 2; value.page.total = 2 },
    value => { value.page.returned = 10 }, value => { value.page.size = 51 },
    value => { value.analysisContext.effectiveScope = 'ALL' }, value => { value.resultFingerprint = null },
    value => { value.records[0].reasons[0].evidence = null }, value => { value.records[0].elapsedDays = '35' },
    value => { value.normalizedQuery.queryMode = 'FAKE' }, value => { value.page.pages = 10 }
  ]
  for (const mutate of mutations) {
    const value = risk(); mutate(value)
    assert.throws(() => protocol.parseAnalysisResponse(value))
  }
  const reason = risk('RISK_REASON'); reason.normalizedQuery.customerId = '9'
  assert.throws(() => protocol.parseAnalysisResponse(reason))
  assert.throws(() => protocol.parseAnalysisResponse({ answer: 'risk', intent: 'UNKNOWN' }))
})
test('unknown advice and unsafe cross-customer suggestions are unavailable, preserving facts', () => {
  for (const mutation of [
    v => { v.advice.status = 'FUTURE_STATUS' },
    v => { v.advice.items[0].customerId = '999' },
    v => { v.advice.items[0].actions = ['AUTOMATIC_WRITE'] },
    v => { v.advice.items[0].reasonKeys = ['MADE_UP'] },
    v => { v.advice.items.push(v.advice.items[0]) },
    v => { v.advice.items[0].suggestion = '' }
  ]) {
    const value = risk('RISK_SUGGESTIONS', [record()], 'AVAILABLE'); mutation(value)
    const parsed = protocol.parseAnalysisResponse(value)
    assert.equal(parsed.advice.status, 'UNAVAILABLE')
    assert.deepEqual(parsed.advice.items, [])
    assert.deepEqual(parsed.records, value.records)
  }
})
test('risk card renders high/medium and keeps special states distinct from prediction', async () => {
  const body = await html(risk('RISK_LIST', [record(ID), record('2300000000000000002','MEDIUM'),
    record('2300000000000000003','OBSERVING'), record('2300000000000000004','DATA_REVIEW')]))
  for (const label of ['高关注','中关注','观察中','数据待核验','客户等级','分析时间','本人客户','不是客户流失预测'])
    assert.ok(body.includes(label), label)
  assert.equal(helpers.riskStatusLabel({ ...record(), state: 'FUTURE' }), '未知关注状态')
  assert.equal(helpers.riskStatusType({ ...record(), riskLevel: 'FUTURE' }), 'info')
})
test('AVAILABLE, UNAVAILABLE and NOT_APPLICABLE render different advice states', async () => {
  const available = await html(risk('RISK_SUGGESTIONS', [record()], 'AVAILABLE'))
  assert.ok(available.includes('人工核实'))
  assert.ok(available.includes('系统模板生成'))
  assert.ok(available.includes('建议先核实当前需求'))
  assert.ok(available.includes(ID))
  const unavailable = await html(risk('RISK_SUGGESTIONS', [record()], 'UNAVAILABLE'))
  assert.ok(unavailable.includes('风险分析已完成'))
  assert.ok(unavailable.includes('高关注'))
  const empty = await html(risk('RISK_SUGGESTIONS', [], 'NOT_APPLICABLE'))
  assert.ok(empty.includes('没有匹配'))
  assert.ok(empty.includes('无需生成'))
  assert.ok(!empty.includes('暂不可用'))
})
test('V1 and V2 messages coexist with their own response data', async () => {
  const { state, close } = mounted()
  try {
    const one = await send(state, statistic, '客户总量')
    const two = await send(state)
    assert.equal(one.result.intent, 'CUSTOMER_TOTAL')
    assert.equal(two.result.intent, 'RISK_LIST')
    assert.equal(state.activeSession.value.exchanges.length, 2)
    assert.equal(typeof two.elapsedMs, 'number')
  } finally { close() }
})
test('reason sends a selected string ID only, keeps list, and always uses unified entry', async () => {
  const { state, close } = mounted()
  try {
    const parent = await send(state)
    const configs = []
    request.defaults.adapter = config => { configs.push(config); return reply(risk('RISK_REASON'))(config) }
    await state.riskAction(parent, 'reason', { customerId: ID })
    assert.equal(configs.length, 1)
    assert.equal(configs[0].url, '/ai/analysis')
    assert.ok(!configs[0].url.includes('customer-risks/analyses'))
    const body = JSON.parse(configs[0].data)
    assert.equal(body.customerId, ID)
    assert.deepEqual(Object.keys(body).sort(), ['customerId','question'])
    assert.equal(parent.result.intent, 'RISK_LIST')
    const child = state.activeSession.value.exchanges.at(-1)
    assert.equal(child.parentId, parent.id)
    assert.equal(child.result.intent, 'RISK_REASON')
    await state.riskAction(parent, 'reason', { customerId: '' })
    await state.riskAction(parent, 'reason', { customerId: '999' })
    assert.equal(configs.length, 1)
    assert.ok(parent.actionError)
  } finally { close() }
})
test('suggestions keep the exact server fingerprint and normalized scope/levels in natural language', async () => {
  const { state, close } = mounted()
  try {
    const data = risk(); data.normalizedQuery.customerLevels = ['A','UNRATED']
    const parent = await send(state, data)
    let config
    const answer = risk('RISK_SUGGESTIONS', [record()], 'AVAILABLE')
    answer.normalizedQuery.customerLevels = ['A','UNRATED']
    request.defaults.adapter = c => { config = c; return reply(answer)(c) }
    await state.riskAction(parent, 'suggestions')
    const body = JSON.parse(config.data)
    assert.equal(config.url, '/ai/analysis')
    assert.equal(body.expectedResultFingerprint, data.resultFingerprint)
    assert.ok(body.question.includes('当前可查看范围'))
    assert.ok(body.question.includes('A级'))
    assert.ok(body.question.includes('未评级'))
    assert.ok(!('queryMode' in body))
    assert.equal(state.activeSession.value.exchanges.at(-1).result.advice.status, 'AVAILABLE')
  } finally { close() }
})
test('pagination reuses original question and drops old-page fingerprint, preserving each message', async () => {
  const { state, close } = mounted()
  try {
    const data = risk(); data.page.total = 41; data.page.pages = 3
    const parent = await send(state, data, '本人重点关注清单')
    let config
    const answer = structuredClone(data)
    answer.page.number = 2; answer.normalizedQuery.page = 2
    request.defaults.adapter = c => { config = c; return reply(answer)(c) }
    await state.riskAction(parent, 'page', { page: 2, size: 20 })
    assert.deepEqual(JSON.parse(config.data), { question: '本人重点关注清单', page: 2, size: 20 })
    assert.equal(config.url, '/ai/analysis')
    assert.equal(parent.result.page.number, 1)
    assert.equal(state.activeSession.value.exchanges.at(-1).result.page.number, 2)
    assert.throws(() => helpers.buildPageRequest(data, { question: 'x' }, 1, 51))
  } finally { close() }
})
test('reclassification drift is an error instead of pretending the same query was paged', async () => {
  const { state, close } = mounted()
  try {
    const parent = await send(state)
    const answer = risk(); answer.normalizedQuery.page = 2; answer.page.number = 2
    answer.normalizedQuery.queryMode = 'ALL_STATES'
    request.defaults.adapter = reply(answer)
    await state.riskAction(parent, 'page', { page: 2, size: 20 })
    const child = state.activeSession.value.exchanges.at(-1)
    assert.equal(child.status, 'error')
    assert.ok(child.error.includes('条件不一致'))
    assert.equal(child.result, undefined)
  } finally { close() }
})
for (const [status, code] of [[403,'RISK_ACCESS_CONTEXT_CHANGED'], [404,'RISK_CUSTOMER_NOT_FOUND'], [409,'RISK_RESULT_STALE']]) {
  test(status + ' invalidates protected parent/child results but retains unrelated statistics', async () => {
    const { state, close } = mounted()
    try {
      const statistics = await send(state, statistic, '总量')
      const parent = await send(state)
      request.defaults.adapter = fail(status, code)
      await state.riskAction(parent, 'reason', { customerId: ID })
      assert.equal(parent.result, undefined)
      assert.equal(parent.invalidated, true)
      assert.equal(state.activeSession.value.exchanges.at(-1).result, undefined)
      assert.equal(statistics.result.intent, 'CUSTOMER_TOTAL')
      request.defaults.adapter = reply(risk())
      await state.refreshExchange(parent)
      assert.equal(state.activeSession.value.exchanges.at(-1).result.intent, 'RISK_LIST')
    } finally { close() }
  })
}
for (const status of [422,502,503]) {
  test(status + ' failure has feedback, no fabricated result and supports retry', async () => {
    const { state, close } = mounted()
    try {
      request.defaults.adapter = fail(status, status === 422 ? 'AI_RISK_CHAT_DISABLED' : undefined)
      state.activeSession.value.draft = '分析客户'
      await state.submitQuestion()
      const failed = state.activeSession.value.exchanges.at(-1)
      assert.equal(failed.status, 'error')
      assert.equal(failed.result, undefined)
      assert.ok(failed.error)
      request.defaults.adapter = reply(risk())
      await state.retryExchange(failed)
      assert.equal(state.activeSession.value.exchanges.at(-1).result.intent, 'RISK_LIST')
    } finally { close() }
  })
}
test('network and timeout errors are bounded and do not leak raw diagnostics', async () => {
  const { state, close } = mounted()
  try {
    for (const code of ['ERR_NETWORK','ECONNABORTED','ETIMEDOUT']) {
      request.defaults.adapter = config => Promise.reject(new axios.AxiosError('private stack',code,config))
      state.activeSession.value.draft = '客户分析' + code
      await state.submitQuestion()
      const row = state.activeSession.value.exchanges.at(-1)
      assert.equal(row.status, 'error')
      assert.ok(!row.error.includes('private'))
      assert.ok(code === 'ERR_NETWORK' ? row.error.includes('网络') : row.error.includes('超时'))
    }
    const error = new axios.AxiosError('x',undefined,undefined,undefined,{status:500,data:{detail:'SELECT password FROM sql'}})
    assert.ok(!errors.analysisErrorMessage(error).includes('password'))
  } finally { close() }
})
test('out-of-order requests and editing the draft cannot overwrite another message', async () => {
  const { state, close } = mounted()
  try {
    const jobs = []
    request.defaults.adapter = config => new Promise(resolve => jobs.push({ config, resolve }))
    state.activeSession.value.draft = '客户总量'
    const first = state.submitQuestion()
    await state.submitQuestion()
    await tick()
    state.activeSession.value.draft = '查看重点关注客户'
    const second = state.submitQuestion()
    await tick()
    assert.equal(jobs.length, 2)
    jobs[1].resolve({ status:200,headers:{},config:jobs[1].config,data:risk() })
    await second
    state.activeSession.value.draft = '保留新草稿'
    jobs[0].resolve({ status:200,headers:{},config:jobs[0].config,data:statistic })
    await first
    assert.equal(state.activeSession.value.exchanges[0].result.intent, 'CUSTOMER_TOTAL')
    assert.equal(state.activeSession.value.exchanges[1].result.intent, 'RISK_LIST')
    assert.equal(state.activeSession.value.draft, '保留新草稿')
  } finally { close() }
})
test('duplicate child actions run once and unmount aborts the pending request', async () => {
  const { state, close } = mounted()
  const parent = await send(state)
  let job
  let count = 0
  request.defaults.adapter = config => { count++; return new Promise(resolve => { job = {config,resolve} }) }
  const first = state.riskAction(parent, 'suggestions')
  await tick()
  await state.riskAction(parent, 'suggestions')
  assert.equal(count, 1)
  close()
  assert.equal(job.config.signal.aborted, true)
  job.resolve({status:200,headers:{},config:job.config,data:risk('RISK_SUGGESTIONS',[record()],'AVAILABLE')})
  await first
  assert.equal(state.activeSession.value.exchanges.at(-1).result, undefined)
})
test('a delayed risk response cannot refill details after a concurrent 403', async () => {
  const { state, close } = mounted()
  try {
    const parent = await send(state)
    let late
    request.defaults.adapter = config => new Promise(resolve => { late={config,resolve} })
    state.activeSession.value.draft = '另一份关注清单'
    const pending = state.submitQuestion()
    await tick()
    request.defaults.adapter = fail(403,'RISK_ACCESS_DENIED')
    await state.riskAction(parent, 'reason', { customerId: ID })
    late.resolve({status:200,headers:{},config:late.config,data:risk()})
    await pending
    assert.ok(state.activeSession.value.exchanges.every(row => row.result === undefined))
  } finally { close() }
})
test('risk action visibility combines permissions and a single sales role; V1 remains usable', async () => {
  for (const [permissions, roles] of [
    [['ai:analysis','customer:list'], ['SALES_STAFF']],
    [['ai:analysis','customer:list','follow:list'], ['SUPER_ADMIN']],
    [['ai:analysis','customer:list','follow:list'], ['SALES_STAFF','SALES_MANAGER']]
  ]) {
    const { state, close } = mounted(permissions, roles)
    try {
      assert.equal(state.canAnalyze.value, true)
      assert.equal(state.canUseRiskActions.value, false)
      const parent = await send(state)
      let calls = 0
      request.defaults.adapter = config => { calls++; return reply(risk())(config) }
      await state.riskAction(parent, 'suggestions')
      assert.equal(calls, 0)
      assert.equal((await send(state, statistic, '客户总量')).result.intent, 'CUSTOMER_TOTAL')
    } finally { close() }
  }
})
test('missing customer selection is a controlled 422, with no invented ID', async () => {
  const { state, close } = mounted()
  try {
    let config
    request.defaults.adapter = c => { config = c; return fail(422,'RISK_CUSTOMER_SELECTION_REQUIRED')(c) }
    state.activeSession.value.draft = '为什么这个客户需要关注'
    await state.submitQuestion()
    assert.deepEqual(JSON.parse(config.data), { question: '为什么这个客户需要关注' })
    const failed = state.activeSession.value.exchanges.at(-1)
    assert.ok(failed.error.includes('明确选择客户'))
    assert.equal(failed.result, undefined)
  } finally { close() }
})
test('permission or sales-role changes hide cached risk details; identity changes reset sessions', async () => {
  const { state, auth, close } = mounted()
  try {
    const parent = await send(state)
    auth.currentUser.roles = ['SALES_MANAGER']
    await tick()
    assert.equal(parent.result, undefined)
    assert.equal(parent.invalidated, true)
    const second = await send(state)
    auth.currentUser.permissions = ['ai:analysis','customer:list']
    await tick()
    assert.equal(second.result, undefined)
    auth.currentUser = { ...auth.currentUser, id: 8 }
    await tick()
    assert.equal(state.activeSession.value.exchanges.length, 0)
  } finally { close() }
})

function nodes(root, predicate) {
  return [ ...(predicate(root) ? [root] : []), ...(root.children ?? []).flatMap(child => nodes(child, predicate)) ]
}
function textOf(root) { return [root.text ?? '', ...(root.children ?? []).map(textOf)].join('') }
function mountCards(state) {
  const root = { children: [] }
  const app = renderer.createApp(defineComponent({ setup() { return () => {
    const exchange = state.activeSession.value.exchanges.at(-1)
    return exchange?.result ? h(InteractiveRiskComponent, {
      result: exchange.result, request: exchange.request, canAct: state.canUseRiskActions.value, busy: false,
      pendingCustomers: state.pendingCustomers(exchange),
      onSingleSuggestions: customerId => state.riskAction(exchange, 'single-suggestions', { customerId })
    }) : h('p', '原风险结果已隐藏')
  } } }))
  app.provide(ssrContextKey, { modules: new Set() })
  for (const name of ['el-tag','el-alert','el-empty','el-pagination'])
    app.component(name, defineComponent({ setup(_, { slots }) { return () => h('div', slots.default?.()) } }))
  app.component('el-button', defineComponent({ props: ['disabled'], setup(props, { slots, attrs }) {
    return () => h('button', { ...attrs, disabled: props.disabled }, slots.default?.())
  } }))
  app.mount(root)
  return { root, close: () => app.unmount() }
}
const evidenceButtons = root => nodes(root, n => n.tag === 'button' && textOf(n).includes('判定依据'))
const evidencePanels = root => nodes(root, n => n.props?.['aria-label'] === '判定依据')

test('card evidence toggles locally and independently without requests or chat messages; new pages reset it', async () => {
  const { state, close } = mounted()
  let cards
  try {
    const parent = await send(state, risk('RISK_LIST', [record(), record('2300000000000000002','MEDIUM')]))
    let calls = 0
    request.defaults.adapter = config => { calls++; return reply(risk())(config) }
    cards = mountCards(state)
    assert.equal(evidenceButtons(cards.root).length, 2)
    assert.equal(evidencePanels(cards.root).length, 0)
    assert.ok(!textOf(cards.root).includes('为当前页获取跟进建议'))
    evidenceButtons(cards.root)[0].props.onClick(); await nextTick()
    assert.equal(evidencePanels(cards.root).length, 1)
    const details = textOf(evidencePanels(cards.root)[0])
    for (const label of ['客户等级','当前关注等级','最近有效跟进时间','35 天','30 天','超过阈值：5 天','原因证据'])
      assert.ok(details.includes(label), label)
    assert.equal(textOf(cards.root).split(parent.result.records[0].reasons[0].message).length - 1, 1)
    evidenceButtons(cards.root)[1].props.onClick(); await nextTick()
    assert.equal(evidencePanels(cards.root).length, 2)
    evidenceButtons(cards.root)[0].props.onClick(); await nextTick()
    assert.equal(evidencePanels(cards.root).length, 1)
    assert.equal(calls, 0)
    assert.equal(state.activeSession.value.exchanges.length, 1)
    parent.result = risk(); await nextTick()
    assert.equal(evidencePanels(cards.root).length, 0)
  } finally { cards?.close(); close() }
})

test('expanded special-state and missing optional evidence use backend facts and safe defaults', async () => {
  const { state, close } = mounted()
  let cards
  try {
    const observing = record('2300000000000000002','OBSERVING')
    observing.historyStatus = 'NEVER_FOLLOWED'; observing.anchorType = 'CUSTOMER_CREATED'; observing.elapsedDays = 2
    const review = record('2300000000000000003','DATA_REVIEW')
    review.customerLevel = null; review.customerNo = null; review.customerName = null
    const never = record(); never.historyStatus = 'NEVER_FOLLOWED'; never.lastValidFollowAt = null
    never.anchorType = 'CUSTOMER_CREATED'
    await send(state, risk('RISK_LIST', [never, observing, review]))
    cards = mountCards(state)
    for (const button of evidenceButtons(cards.root)) button.props.onClick()
    await nextTick()
    const panels = evidencePanels(cards.root).map(textOf)
    assert.ok(panels[0].includes('从未有效跟进'))
    assert.ok(panels[1].includes('观察中'))
    assert.ok(panels[1].includes('距建档至分析时间：2 天'))
    assert.ok(panels[1].includes('当前规则阈值：不适用'))
    assert.ok(!panels[1].includes('超过阈值'))
    assert.ok(panels[2].includes('未评级'))
    assert.ok(panels[2].includes('未提供，待核验'))
    assert.ok(panels[2].includes('HAS_ANOMALIES'))
    assert.ok(panels[2].includes('ANOMALOUS_FOLLOW_TIME'))
    assert.ok(!textOf(cards.root).includes('undefined'))
  } finally { cards?.close(); close() }
})

test('permission invalidation unmounts expanded evidence and prevents further protected operations', async () => {
  const { state, auth, close } = mounted()
  let cards
  try {
    const parent = await send(state)
    cards = mountCards(state)
    evidenceButtons(cards.root)[0].props.onClick(); await nextTick()
    assert.equal(evidencePanels(cards.root).length, 1)
    auth.currentUser.permissions = ['ai:analysis','customer:list']
    await tick()
    assert.equal(parent.invalidated, true)
    assert.equal(evidencePanels(cards.root).length, 0)
    assert.equal(evidenceButtons(cards.root).length, 0)
    assert.ok(!textOf(cards.root).includes('原因证据'))
  } finally { cards?.close(); close() }
})

test('direct natural-language suggestions still use the unified entry and render advice without a batch button', async () => {
  const { state, close } = mounted()
  try {
    const exchange = await send(state, risk('RISK_SUGGESTIONS', [record()], 'AVAILABLE'), '请给出重点关注客户的跟进建议')
    assert.equal(exchange.result.intent, 'RISK_SUGGESTIONS')
    const body = await html(exchange.result)
    assert.ok(body.includes('建议先核实当前需求'))
    assert.ok(!body.includes('为当前页获取跟进建议'))
  } finally { close() }
})

function singleAdvice(id = ID, status = 'AVAILABLE') {
  const result = risk('RISK_SUGGESTIONS', [record(id)], status)
  result.normalizedQuery = { scopeIntent: 'CURRENT_AUTHORIZED', customerId: id }
  result.page = null
  return result
}
test('single-customer button sends only question and string ID, preserves list, and renders a related suggestion', async () => {
  const { state, close } = mounted()
  let cards
  try {
    const parent = await send(state)
    cards = mountCards(state)
    let config
    request.defaults.adapter = c => { config = c; return reply(singleAdvice())(c) }
    const button = nodes(cards.root, n => n.tag === 'button' && textOf(n) === '获取跟进建议')[0]
    assert.ok(button)
    button.props.onClick(); await tick()
    const body = JSON.parse(config.data)
    assert.equal(config.url, '/ai/analysis')
    assert.deepEqual(Object.keys(body).sort(), ['customerId','question'])
    assert.equal(body.customerId, ID)
    assert.ok(body.question.includes('单个客户生成跟进建议'))
    assert.ok(!('expectedResultFingerprint' in body))
    assert.equal(parent.result.intent, 'RISK_LIST')
    const child = state.activeSession.value.exchanges.at(-1)
    assert.equal(child.parentId, parent.id)
    assert.equal(child.result.records[0].customerId, ID)
    assert.equal(child.result.advice.status, 'AVAILABLE')
    assert.ok((await html(child.result)).includes('人工确认'))
  } finally { cards?.close(); close() }
})
test('single suggestions are hidden and refused for nonpriority and special-state customers', async () => {
  const { state, close } = mounted()
  try {
    const data = risk('RISK_LIST', [record(ID,'LOW'), record('2300000000000000002','OBSERVING'), record('2300000000000000003','DATA_REVIEW')])
    const parent = await send(state, data)
    assert.ok(!(await html(data)).includes('获取跟进建议'))
    let calls = 0
    request.defaults.adapter = c => { calls++; return reply(singleAdvice())(c) }
    for (const r of data.records) await state.riskAction(parent, 'single-suggestions', { customerId: r.customerId })
    assert.equal(calls, 0)
    assert.equal(state.activeSession.value.exchanges.length, 1)
  } finally { close() }
})
test('single suggestions reject wrong customer, wrong intent and batch-shaped responses', async () => {
  const { state, close } = mounted()
  try {
    const parent = await send(state)
    for (const data of [singleAdvice('2300000000000000002'), risk('RISK_REASON'), risk('RISK_SUGGESTIONS',[record()],'AVAILABLE')]) {
      request.defaults.adapter = reply(data)
      await state.riskAction(parent, 'single-suggestions', { customerId: ID })
      const child = state.activeSession.value.exchanges.at(-1)
      assert.equal(child.status, 'error')
      assert.equal(child.result, undefined)
    }
  } finally { close() }
})
test('single UNAVAILABLE supports explicit retry; NOT_APPLICABLE displays current reason without fabrication', async () => {
  const { state, close } = mounted()
  try {
    const parent = await send(state)
    request.defaults.adapter = reply(singleAdvice(ID,'UNAVAILABLE'))
    await state.riskAction(parent, 'single-suggestions', { customerId: ID })
    const child = state.activeSession.value.exchanges.at(-1)
    assert.equal(child.result.advice.status, 'UNAVAILABLE')
    request.defaults.adapter = reply(singleAdvice())
    await state.retryExchange(child)
    assert.equal(state.activeSession.value.exchanges.at(-1).result.advice.status, 'AVAILABLE')
    const cleared = singleAdvice(ID,'NOT_APPLICABLE')
    cleared.records = [record(ID,'LOW')]
    cleared.advice.message = '当前客户不属于高/中关注，无需生成跟进建议。'
    request.defaults.adapter = reply(cleared)
    await state.riskAction(parent, 'single-suggestions', { customerId: ID })
    const body = await html(state.activeSession.value.exchanges.at(-1).result)
    assert.ok(body.includes('当前客户不属于高/中关注'))
    assert.ok(!body.includes('建议先核实'))
  } finally { close() }
})
test('single requests deduplicate per customer and keep concurrent out-of-order suggestions separate', async () => {
  const { state, close } = mounted()
  try {
    const other = '2300000000000000002'
    const parent = await send(state, risk('RISK_LIST',[record(),record(other)]))
    const jobs = []
    request.defaults.adapter = config => new Promise(resolve => jobs.push({config,resolve}))
    const one = state.riskAction(parent,'single-suggestions',{customerId:ID})
    await tick()
    await state.riskAction(parent,'single-suggestions',{customerId:ID})
    const two = state.riskAction(parent,'single-suggestions',{customerId:other})
    await tick()
    assert.equal(jobs.length,2)
    assert.deepEqual(state.pendingCustomers(parent),[ID,other])
    jobs[1].resolve({status:200,headers:{},config:jobs[1].config,data:singleAdvice(other)})
    await two
    jobs[0].resolve({status:200,headers:{},config:jobs[0].config,data:singleAdvice(ID)})
    await one
    assert.equal(state.activeSession.value.exchanges[1].result.records[0].customerId,ID)
    assert.equal(state.activeSession.value.exchanges[2].result.records[0].customerId,other)
    assert.deepEqual(state.pendingCustomers(parent),[])
    assert.equal(parent.result.records.length,2)
  } finally { close() }
})
for (const [status, code] of [[403,'RISK_ACCESS_CONTEXT_CHANGED'],[404,'RISK_CUSTOMER_NOT_FOUND'],[409,'RISK_RESULT_STALE']]) {
  test('single '+status+' hides source facts and previous single advice',async () => {
    const { state, close } = mounted()
    try {
      const parent = await send(state)
      request.defaults.adapter = reply(singleAdvice())
      await state.riskAction(parent,'single-suggestions',{customerId:ID})
      request.defaults.adapter = fail(status,code)
      await state.riskAction(parent,'single-suggestions',{customerId:ID})
      assert.ok(state.activeSession.value.exchanges.every(e => !e.result && e.invalidated))
      await state.riskAction(parent,'single-suggestions',{customerId:ID})
      assert.equal(state.activeSession.value.exchanges.length,3)
    } finally { close() }
  })
}
test('single 422 and timeout remain safe errors and allow explicit retry',async () => {
  const { state, close } = mounted()
  try {
    const parent = await send(state)
    for (const adapter of [fail(422,'RISK_QUERY_UNSUPPORTED'), c => Promise.reject(new axios.AxiosError('private timeout','ETIMEDOUT',c))]) {
      request.defaults.adapter = adapter
      await state.riskAction(parent,'single-suggestions',{customerId:ID})
      const child = state.activeSession.value.exchanges.at(-1)
      assert.equal(child.status,'error')
      assert.ok(!child.error.includes('private'))
      request.defaults.adapter = reply(singleAdvice())
      await state.retryExchange(child)
      assert.equal(state.activeSession.value.exchanges.at(-1).result.advice.status,'AVAILABLE')
    }
  } finally { close() }
})

test('compact single suggestions show customer, reason and original suggestion once with details initially hidden', async () => {
  const data = singleAdvice()
  data.answer = data.records[0].reasons[0].message
  const body = await html(data, { question: '选中客户建议', customerId: ID })
  assert.ok(body.includes('单客户跟进建议'))
  assert.ok(!body.includes('class="customer-card"'))
  assert.equal(body.split(data.records[0].customerName).length - 1, 1)
  assert.equal(body.split(data.records[0].reasons[0].message).length - 1, 1)
  assert.equal(body.split(data.advice.items[0].suggestion).length - 1, 1)
  assert.ok(body.includes('<ol'))
  assert.ok(body.includes('人工核实'))
  assert.ok(!body.includes(ID))
  assert.ok(!body.includes('SYNTHETIC-RULE'))
  assert.ok(!body.includes('获取跟进建议'))
})

test('compact layout needs matching explicit request and response, not just one record', async () => {
  for (const [data, request] of [
    [singleAdvice(), undefined], [singleAdvice(), {question:'建议',customerId:'999'}],
    [risk('RISK_SUGGESTIONS',[record()],'AVAILABLE'), {question:'建议',customerId:ID}],
    [risk('RISK_LIST'), {question:'清单',customerId:ID}],
    [risk('RISK_REASON'), {question:'原因',customerId:ID}]
  ]) {
    const body = await html(data, request)
    assert.ok(!body.includes('aria-label="单客户跟进建议"'))
    assert.ok(body.includes('class="customer-card"'))
  }
})

test('compact analysis details toggle locally without duplicating reasons and disappear on permission invalidation', async () => {
  const { state, auth, close } = mounted()
  let cards
  try {
    const parent = await send(state)
    request.defaults.adapter = reply(singleAdvice())
    await state.riskAction(parent,'single-suggestions',{customerId:ID})
    cards = mountCards(state)
    let requests = 0
    request.defaults.adapter = c => { requests++; return reply(singleAdvice())(c) }
    const toggle = () => nodes(cards.root, n => n.tag === 'button' && textOf(n).includes('分析详情'))[0]
    const panels = () => nodes(cards.root,n => n.props?.['aria-label'] === '分析详情')
    assert.equal(panels().length,0)
    assert.equal(toggle().props['aria-expanded'],false)
    toggle().props.onClick(); await nextTick()
    assert.equal(panels().length,1)
    for (const label of [ID,'数据范围','最大授权范围','分析时间','SYNTHETIC-RULE','查询口径','阈值','历史快照'])
      assert.ok(textOf(panels()[0]).includes(label),label)
    assert.equal(textOf(cards.root).split(record().reasons[0].message).length - 1,1)
    toggle().props.onClick(); await nextTick()
    assert.equal(panels().length,0)
    assert.equal(requests,0)
    assert.equal(state.activeSession.value.exchanges.length,2)
    toggle().props.onClick(); await nextTick()
    auth.currentUser.permissions = ['ai:analysis','customer:list']
    await tick()
    assert.ok(!textOf(cards.root).includes('建议先核实'))
    assert.equal(panels().length,0)
    assert.ok(!textOf(cards.root).includes(ID))
  } finally { cards?.close(); close() }
})

test('compact unavailable and special not-applicable states retain facts without invented advice', async () => {
  for (const [kind,status] of [['HIGH','UNAVAILABLE'],['LOW','NOT_APPLICABLE'],['OBSERVING','NOT_APPLICABLE'],['DATA_REVIEW','NOT_APPLICABLE']]) {
    const data = singleAdvice(ID,status)
    data.records = [record(ID,kind)]
    if (kind === 'OBSERVING') {
      data.records[0].historyStatus = 'NEVER_FOLLOWED'
      data.records[0].anchorType = 'CUSTOMER_CREATED'
      data.records[0].elapsedDays = 2
    }
    const body = await html(data,{question:'建议',customerId:ID})
    assert.ok(body.includes('单客户跟进建议'))
    assert.equal(body.split('合成客户').length - 1,1)
    assert.ok(!body.includes('建议先核实当前需求'))
    if (status === 'UNAVAILABLE') assert.ok(body.includes('暂不可用'))
    if (kind === 'OBSERVING') { assert.ok(body.includes('观察期')); assert.ok(body.includes('从未有效跟进')); assert.ok(body.includes('2 天')) }
    if (kind === 'DATA_REVIEW') { assert.ok(body.includes('暂不评定')); assert.ok(body.includes('待核验')) }
  }
})
