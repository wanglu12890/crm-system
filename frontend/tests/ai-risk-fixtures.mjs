export const statistic = {
  answer: '本人范围内，全部时间客户共 0 个。', intent: 'CUSTOMER_TOTAL',
  metrics: { totalCustomers: 0, levelA: 0, levelB: 0, levelC: 0, levelD: 0, unrated: 0 },
  analysisContext: { dataScope: 'SELF', timeRange: 'ALL', referenceTime: '2026-10-25T12:00:00+08:00', timezone: 'Asia/Shanghai' }
}
export const ID = '2300000000000000001'
export function record(id = ID, kind = 'HIGH') {
  const special = ['OBSERVING','DATA_REVIEW'].includes(kind)
  return {
    customerId: id, customerNo: 'SYN-1', customerName: '合成客户', customerLevel: 'A', customerStatus: 'ACTIVE',
    state: special ? kind : 'NORMAL', riskLevel: special ? null : kind,
    historyStatus: kind === 'DATA_REVIEW' ? 'INVALID_ONLY' : 'FOLLOWED',
    lastValidFollowAt: special ? null : '2026-09-20T12:00:00+08:00', anchorType: special ? 'NONE' : 'LAST_VALID_FOLLOW',
    elapsedDays: special ? null : 35, thresholdDays: special ? null : 30, excessDays: special ? null : 5,
    reasons: special ? [] : [{ code: 'FOLLOW_GAP', message: '距最近有效跟进35天，已达到关注阈值。',
      evidence: { anchorTime: '2026-09-20T12:00:00+08:00', elapsedDays: 35, thresholdDays: 30, levelGroup: 'A' } }],
    dataQuality: { status: kind === 'DATA_REVIEW' ? 'HAS_ANOMALIES' : 'CLEAN', codes: kind === 'DATA_REVIEW' ? ['ANOMALOUS_FOLLOW_TIME'] : [] }
  }
}
export function risk(intent = 'RISK_LIST', records = [record()], advice = 'NOT_REQUESTED') {
  return {
    status: advice === 'UNAVAILABLE' ? 'PARTIAL' : 'SUCCESS', intent, answer: '授权范围内客户关注分析已完成。',
    analysisContext: { authorizedScope: 'SELF', requestedScope: 'CURRENT_AUTHORIZED', effectiveScope: 'SELF',
      analysisTime: '2026-10-25T12:00:00+08:00', timezone: 'Asia/Shanghai', ruleVersion: 'SYNTHETIC-RULE',
      consistency: 'LIVE_REQUEST_SNAPSHOT' },
    normalizedQuery: intent === 'RISK_REASON' ? { scopeIntent: 'CURRENT_AUTHORIZED', customerId: records[0]?.customerId ?? ID }
      : { scopeIntent: 'CURRENT_AUTHORIZED', queryMode: 'PRIORITY', customerLevels: null, page: 1, size: 20 },
    page: intent === 'RISK_REASON' ? null : { number: 1, size: 20, returned: records.length, total: records.length, pages: records.length ? 1 : 0 },
    records, resultFingerprint: 'synthetic-fingerprint-keep-exact',
    advice: { status: advice, source: advice === 'AVAILABLE' ? 'QWEN_ACTIONS_JAVA_TEMPLATE' : null,
      unavailableCode: advice === 'UNAVAILABLE' ? 'RISK_ADVICE_UNAVAILABLE' : null, message: null,
      items: advice === 'AVAILABLE' ? records.map(r => ({ customerId: r.customerId, actions: ['VERIFY_CURRENT_NEEDS'],
        reasonKeys: r.reasons.map(reason => reason.code), suggestion: '建议先核实当前需求，人工确认后再安排联系。' })) : [] }
  }
}
