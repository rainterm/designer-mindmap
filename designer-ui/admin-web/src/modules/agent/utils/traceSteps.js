/**
 * 对话轨迹步骤：按时间顺序交错记录 thinking / tool。
 */

export function createEmptyTraceSteps() {
  return []
}

/** 从工具轨迹中提取知识库文档来源（按 docId 去重） */
export function extractKnowledgeSources(m) {
  const tools = m?.tools || []
  const map = new Map()
  for (const t of tools) {
    if (!t || (t.toolName !== 'knowledge_retrieve' && t.toolLabel !== '知识库检索')) continue
    const list = t.args?.sources
    if (!Array.isArray(list)) continue
    for (const s of list) {
      if (!s || s.docId == null) continue
      const id = Number(s.docId)
      if (!Number.isFinite(id) || id <= 0 || map.has(id)) continue
      map.set(id, {
        knowledgeId: s.knowledgeId != null ? Number(s.knowledgeId) : null,
        docId: id,
        title: s.title || `文档 #${id}`,
        kbName: s.kbName || '',
        score: s.score
      })
    }
  }
  return [...map.values()]
}

/** 优先用流式轨迹；历史消息兜底为：工具在前、思考在后 */
export function buildTraceStepsFromMessage(m) {
  if (Array.isArray(m?.traceSteps) && m.traceSteps.length) {
    return m.traceSteps
  }
  const steps = []
  const tools = m?.tools || []
  for (let i = 0; i < tools.length; i++) {
    const t = tools[i]
    steps.push({
      id: `tool-${t.callId || i}`,
      type: 'tool',
      tool: t
    })
  }
  if (m?.thinking) {
    steps.push({
      id: 'thinking-hist',
      type: 'thinking',
      content: m.thinking,
      running: false
    })
  }
  return steps
}

export function appendThinkingDelta(steps, delta) {
  if (!delta) return steps
  const list = Array.isArray(steps) ? [...steps] : []
  const last = list[list.length - 1]
  if (last?.type === 'thinking') {
    list[list.length - 1] = {
      ...last,
      content: (last.content || '') + delta,
      running: true
    }
  } else {
    list.push({
      id: `thinking-${Date.now()}-${list.length}`,
      type: 'thinking',
      content: delta,
      running: true
    })
  }
  return list
}

export function stopThinkingRunning(steps) {
  const list = Array.isArray(steps) ? [...steps] : []
  for (let i = list.length - 1; i >= 0; i--) {
    if (list[i]?.type === 'thinking' && list[i].running) {
      list[i] = { ...list[i], running: false }
      break
    }
  }
  return list
}

export function upsertToolStep(steps, tool) {
  if (!tool) return steps
  let list = stopThinkingRunning(steps)
  const callId = tool.callId
  if (callId) {
    const i = list.findIndex((s) => s.type === 'tool' && s.tool?.callId === callId)
    if (i >= 0) {
      list[i] = {
        ...list[i],
        tool: { ...list[i].tool, ...tool }
      }
      return list
    }
  }
  list.push({
    id: `tool-${callId || Date.now()}-${list.length}`,
    type: 'tool',
    tool
  })
  return list
}

export function finishTraceSteps(steps) {
  return (steps || []).map((s) =>
    s.type === 'thinking' ? { ...s, running: false } : s
  )
}
