/**
 * 智能体模块 API（对接 designer-module-agent + Solon AI Harness）。
 */
import request from '@/api/request'
import { getToken } from '@/utils/auth'

// ---------------------- 供应商 ----------------------
export const pageProviders = (params) => request.get('/admin-api/agent/provider/page', { params })
export const listProviders = () => request.get('/admin-api/agent/provider/list')
export const getProvider = (id) => request.get(`/admin-api/agent/provider/${id}`)
export const createProvider = (data) => request.post('/admin-api/agent/provider/add', data)
export const updateProvider = (data) => request.put('/admin-api/agent/provider/update', data)
export const deleteProviders = (ids) =>
  request.delete('/admin-api/agent/provider/remove', { params: { ids: ids.join(',') } })
export const fetchProviderModels = (data) =>
  request.post('/admin-api/agent/provider/fetch-models', data, { timeout: 60000 })
export const listProviderVendors = () => request.get('/admin-api/agent/provider/vendors')

// ---------------------- 工具 ----------------------
export const pageTools = (params) => request.get('/admin-api/agent/tool/page', { params })
export const listTools = () => request.get('/admin-api/agent/tool/list')
export const createTool = (data) => request.post('/admin-api/agent/tool/add', data)
export const updateTool = (data) => request.put('/admin-api/agent/tool/update', data)
export const deleteTools = (ids) =>
  request.delete('/admin-api/agent/tool/remove', { params: { ids: ids.join(',') } })

// ---------------------- MCP ----------------------
export const pageMcps = (params) => request.get('/admin-api/agent/mcp/page', { params })
export const listMcps = () => request.get('/admin-api/agent/mcp/list')
export const createMcp = (data) => request.post('/admin-api/agent/mcp/add', data)
export const updateMcp = (data) => request.put('/admin-api/agent/mcp/update', data)
export const deleteMcps = (ids) =>
  request.delete('/admin-api/agent/mcp/remove', { params: { ids: ids.join(',') } })
export const probeMcp = (id) => request.post(`/admin-api/agent/mcp/${id}/probe`)
/** 按当前表单配置拉取工具清单（不落库） */
export const probeMcpTools = (data) => request.post('/admin-api/agent/mcp/probe-tools', data)

// ---------------------- Skill ----------------------
export const pageSkills = (params) => request.get('/admin-api/agent/skill/page', { params })
export const listSkills = () => request.get('/admin-api/agent/skill/list')
export const getSkill = (id) => request.get(`/admin-api/agent/skill/${id}`)
export const createSkill = (data) => request.post('/admin-api/agent/skill/add', data)
export const updateSkill = (data) => request.put('/admin-api/agent/skill/update', data)
export const deleteSkills = (ids) =>
  request.delete('/admin-api/agent/skill/remove', { params: { ids: ids.join(',') } })
export const importSkillPackage = (data) => request.post('/admin-api/agent/skill/import', data)

// ---------------------- 工作流 ----------------------
export const pageWorkflows = (params) => request.get('/admin-api/agent/workflow/page', { params })
export const listWorkflows = () => request.get('/admin-api/agent/workflow/list')
export const getWorkflow = (id) => request.get(`/admin-api/agent/workflow/${id}`)
export const createWorkflow = (data) => request.post('/admin-api/agent/workflow/add', data)
export const updateWorkflow = (data) => request.put('/admin-api/agent/workflow/update', data)
export const deleteWorkflows = (ids) =>
  request.delete('/admin-api/agent/workflow/remove', { params: { ids: ids.join(',') } })
export const runWorkflow = (id, data) =>
  request.post(`/admin-api/agent/workflow/${id}/run`, data, { timeout: 180000 })
/** 发布：把当前草稿写入历史快照，version+1 */
export const publishWorkflow = (id) => request.put(`/admin-api/agent/workflow/${id}/publish`)
/** 历史快照分页 */
export const pageWorkflowHistory = (id, params) =>
  request.get(`/admin-api/agent/workflow/${id}/history`, { params })
/** 历史快照详情（含完整 nodes/edges） */
export const getWorkflowVersion = (id, version) =>
  request.get(`/admin-api/agent/workflow/${id}/versions/${version}`)
/** 回滚到指定版本（覆盖主表 draft，不动 version） */
export const rollbackWorkflow = (id, version) =>
  request.post(`/admin-api/agent/workflow/${id}/rollback`, { version })

/**
 * 工作流试跑 SSE。callbacks: onStatus / onStep / onDelta / onThinking / onDone / onError
 * @returns {Promise<object|null>} done 事件 payload
 */
export async function runWorkflowStream(id, data, callbacks = {}, signal) {
  const { onStatus, onStep, onDelta, onThinking, onDone, onError } = callbacks
  const base = import.meta.env.VITE_API_BASE || ''
  const res = await fetch(`${base}/admin-api/agent/workflow/${id}/run-stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: getToken() || ''
    },
    body: JSON.stringify(data || {}),
    signal
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `工作流试跑失败 (${res.status})`)
  }
  if (!res.body) {
    throw new Error('浏览器不支持流式响应')
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let donePayload = null

  const dispatch = (eventName, dataStr) => {
    let payload = dataStr
    try {
      payload = dataStr ? JSON.parse(dataStr) : null
    } catch {
      // keep raw
    }
    if (eventName === 'status') onStatus?.(payload?.status || '')
    else if (eventName === 'step') onStep?.(payload)
    else if (eventName === 'delta') onDelta?.(payload?.delta ?? '')
    else if (eventName === 'thinking') onThinking?.(payload?.delta ?? '')
    else if (eventName === 'done') {
      donePayload = payload
      onDone?.(payload)
    } else if (eventName === 'error') onError?.(payload?.msg || '执行失败')
  }

  const flushBlock = (block) => {
    if (!block.trim()) return
    let eventName = 'message'
    const dataLines = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
    }
    dispatch(eventName, dataLines.join('\n'))
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    buffer = buffer.replace(/\r\n/g, '\n')
    let idx
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      flushBlock(block)
    }
  }
  if (buffer.trim()) flushBlock(buffer)
  return donePayload
}

// ---------------------- 知识库 ----------------------
export const pageKnowledges = (params) => request.get('/admin-api/agent/knowledge/page', { params })
export const listKnowledges = () => request.get('/admin-api/agent/knowledge/list')
export const getKnowledge = (id) => request.get(`/admin-api/agent/knowledge/${id}`)
export const createKnowledge = (data) => request.post('/admin-api/agent/knowledge/add', data)
export const updateKnowledge = (data) => request.put('/admin-api/agent/knowledge/update', data)
export const deleteKnowledges = (ids) =>
  request.delete('/admin-api/agent/knowledge/remove', { params: { ids: ids.join(',') } })
export const retrieveTestKnowledge = (id, data) =>
  request.post(`/admin-api/agent/knowledge/${id}/retrieve-test`, data)
export const pageKnowledgeDocs = (params) =>
  request.get('/admin-api/agent/knowledge/doc/page', { params })
export const getKnowledgeDoc = (id) => request.get(`/admin-api/agent/knowledge/doc/${id}`)
export const createKnowledgeDoc = (data) => request.post('/admin-api/agent/knowledge/doc/add', data)
export const updateKnowledgeDoc = (data) => request.put('/admin-api/agent/knowledge/doc/update', data)
export const deleteKnowledgeDocs = (ids) =>
  request.delete('/admin-api/agent/knowledge/doc/remove', { params: { ids: ids.join(',') } })
export const reindexKnowledgeDoc = (id) =>
  request.post(`/admin-api/agent/knowledge/doc/${id}/reindex`)
export const listKnowledgeDocChunks = (id) =>
  request.get(`/admin-api/agent/knowledge/doc/${id}/chunks`)
export const updateKnowledgeDocChunk = (id, chunkIndex, data) =>
  request.put(`/admin-api/agent/knowledge/doc/${id}/chunks/${chunkIndex}`, data)

/**
 * SSE 重建索引。callbacks: onProgress / onDone / onError
 * @returns {Promise<object|null>} done 事件 payload
 */
export async function reindexKnowledgeDocStream(id, callbacks = {}, signal) {
  const { onProgress, onDone, onError } = callbacks
  const base = import.meta.env.VITE_API_BASE || ''
  const res = await fetch(`${base}/admin-api/agent/knowledge/doc/${id}/reindex-stream`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      Authorization: getToken() || ''
    },
    signal
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `索引失败 (${res.status})`)
  }
  if (!res.body) {
    throw new Error('浏览器不支持流式响应')
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let donePayload = null

  const dispatch = (eventName, dataStr) => {
    let data = dataStr
    try {
      data = dataStr ? JSON.parse(dataStr) : null
    } catch {
      // keep raw
    }
    if (eventName === 'progress') onProgress?.(data)
    else if (eventName === 'done') {
      donePayload = data
      onDone?.(data)
    } else if (eventName === 'error') onError?.(data?.msg || '索引失败', data)
  }

  const flushBlock = (block) => {
    if (!block.trim()) return
    let eventName = 'message'
    const dataLines = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
    }
    dispatch(eventName, dataLines.join('\n'))
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    buffer = buffer.replace(/\r\n/g, '\n')
    let idx
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      flushBlock(block)
    }
  }
  if (buffer.trim()) flushBlock(buffer)
  return donePayload
}

// ---------------------- 智能体 ----------------------
export const pageAgents = (params) => request.get('/admin-api/agent/agent/page', { params })
export const listAgents = () => request.get('/admin-api/agent/agent/list')
export const getAgent = (id) => request.get(`/admin-api/agent/agent/${id}`)
export const createAgent = (data) => request.post('/admin-api/agent/agent/add', data)
export const updateAgent = (data) => request.put('/admin-api/agent/agent/update', data)
export const deleteAgents = (ids) =>
  request.delete('/admin-api/agent/agent/remove', { params: { ids: ids.join(',') } })
/** 发布：把当前草稿固化为已发布配置，version+1，写历史快照 */
export const publishAgent = (id) => request.put(`/admin-api/agent/agent/${id}/publish`)
/** 历史快照分页 */
export const pageAgentHistory = (id, params) =>
  request.get(`/admin-api/agent/agent/${id}/history`, { params })
/** 历史快照详情（含完整配置） */
export const getAgentVersion = (id, version) =>
  request.get(`/admin-api/agent/agent/${id}/versions/${version}`)
/** 回滚到指定版本（覆盖主表草稿，不动 version） */
export const rollbackAgent = (id, version) =>
  request.post(`/admin-api/agent/agent/${id}/rollback`, { version })
/** 开启公开访问链接 */
export const enableAgentPublic = (id) => request.put(`/admin-api/agent/agent/${id}/public/enable`)
/** 关闭公开访问链接 */
export const disableAgentPublic = (id) => request.put(`/admin-api/agent/agent/${id}/public/disable`)
/** 轮换公开访问 token */
export const rotateAgentPublic = (id) => request.post(`/admin-api/agent/agent/${id}/public/rotate`)

// ---------------------- 公开访问（免登录） ----------------------
const PUBLIC_VISITOR_KEY = 'agent_public_vid'
const PUBLIC_VISITOR_HEADER = 'X-Public-Visitor'

function publicVisitorId() {
  try {
    let id = localStorage.getItem(PUBLIC_VISITOR_KEY)
    if (!id) {
      id = (crypto.randomUUID?.() || `${Date.now()}-${Math.random()}`).replace(/-/g, '').slice(0, 32)
      localStorage.setItem(PUBLIC_VISITOR_KEY, id)
    }
    return id
  } catch {
    return 'anon'
  }
}

function publicHeaders(extra = {}) {
  return {
    ...extra,
    [PUBLIC_VISITOR_HEADER]: publicVisitorId()
  }
}

export const getPublicAgent = (token) =>
  request.get(`/app-api/agent/public/${token}`, { headers: publicHeaders() })
export const listPublicSessions = (token) =>
  request.get(`/app-api/agent/public/${token}/sessions`, { headers: publicHeaders() })
export const openPublicSession = (token) =>
  request.post(`/app-api/agent/public/${token}/session`, null, { headers: publicHeaders() })
export const createPublicSession = (token, data) =>
  request.post(`/app-api/agent/public/${token}/sessions`, data || { title: '新会话' }, {
    headers: publicHeaders()
  })
export const deletePublicSessions = (token, ids) =>
  request.delete(`/app-api/agent/public/${token}/sessions`, {
    params: { ids: ids.join(',') },
    headers: publicHeaders()
  })
export const listPublicMessages = (token, sessionId) =>
  request.get(`/app-api/agent/public/${token}/sessions/${sessionId}/messages`, {
    headers: publicHeaders()
  })

/**
 * 公开访问 SSE 对话（不带后台登录态）。
 */
export async function sendPublicMessageStream(token, sessionId, content, callbacks = {}, signal) {
  const { onUser, onDelta, onTool, onThinking, onStatus, onDone, onError } = callbacks
  const base = import.meta.env.VITE_API_BASE || ''
  const res = await fetch(
    `${base}/app-api/agent/public/${encodeURIComponent(token)}/sessions/${sessionId}/send-stream`,
    {
      method: 'POST',
      headers: publicHeaders({
        'Content-Type': 'application/json',
        Accept: 'text/event-stream'
      }),
      body: JSON.stringify({ content, useDraft: false }),
      signal
    }
  )
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `流式对话失败 (${res.status})`)
  }
  if (!res.body) {
    throw new Error('浏览器不支持流式响应')
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let donePayload = null
  const dispatch = (eventName, dataStr) => {
    let data = dataStr
    try {
      data = dataStr ? JSON.parse(dataStr) : null
    } catch {
      // keep raw
    }
    if (eventName === 'user') onUser?.(data)
    else if (eventName === 'delta') onDelta?.(data?.delta ?? '')
    else if (eventName === 'tool') onTool?.(data)
    else if (eventName === 'thinking') onThinking?.(data?.delta ?? '')
    else if (eventName === 'status') onStatus?.(data?.status || '')
    else if (eventName === 'done') {
      donePayload = data
      onDone?.(data)
    } else if (eventName === 'error') onError?.(data?.msg || '对话失败')
  }
  const flushBlock = (block) => {
    if (!block.trim()) return
    let eventName = 'message'
    const dataLines = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
    }
    dispatch(eventName, dataLines.join('\n'))
  }
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    buffer = buffer.replace(/\r\n/g, '\n')
    let idx
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      flushBlock(block)
    }
  }
  if (buffer.trim()) flushBlock(buffer)
  return donePayload
}

// ---------------------- 智能体商店（使用者） ----------------------
export const pageStoreAgents = (params) => request.get('/admin-api/agent/store/page', { params })

// ---------------------- 会话 ----------------------
export const pageSessions = (params) => request.get('/admin-api/agent/session/page', { params })
export const listSessionsByAgent = (agentId) =>
  request.get('/admin-api/agent/session/list', { params: { agentId } })
/** 商店打开：复用最近非调试会话，没有才新建 */
export const openSessionByAgent = (agentId) =>
  request.get('/admin-api/agent/session/open', { params: { agentId } })
export const getSession = (id) => request.get(`/admin-api/agent/session/${id}`)
export const createSession = (data) => request.post('/admin-api/agent/session/add', data)
export const deleteSessions = (ids) =>
  request.delete('/admin-api/agent/session/remove', { params: { ids: ids.join(',') } })
/** 按会话 ID 删除整段会话（聊天侧栏） */
export const deleteSessionByIds = (ids) =>
  request.delete('/admin-api/agent/session/remove-by-ids', { params: { ids: ids.join(',') } })
export const listMessages = (sessionId) =>
  request.get(`/admin-api/agent/session/${sessionId}/messages`)
export const sendMessage = (sessionId, content) =>
  request.post(
    `/admin-api/agent/session/${sessionId}/send`,
    { content },
    { timeout: 120000 }
  )

/**
 * SSE 流式发送。callbacks: onUser / onDelta / onTool / onThinking / onStatus / onDone / onError
 * @returns {Promise<object|null>} done 事件 payload
 */
export async function sendMessageStream(
  sessionId,
  content,
  callbacks = {},
  signal,
  attachments = [],
  useDraft = false,
  extra = {}
) {
  const { onUser, onDelta, onTool, onThinking, onStatus, onDone, onError } = callbacks
  const base = import.meta.env.VITE_API_BASE || ''
  const res = await fetch(`${base}/admin-api/agent/session/${sessionId}/send-stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: getToken() || ''
    },
    body: JSON.stringify({ content, attachments, useDraft, ...extra }),
    signal
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `流式对话失败 (${res.status})`)
  }
  if (!res.body) {
    throw new Error('浏览器不支持流式响应')
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let donePayload = null

  const dispatch = (eventName, dataStr) => {
    let data = dataStr
    try {
      data = dataStr ? JSON.parse(dataStr) : null
    } catch {
      // keep raw
    }
    if (eventName === 'user') onUser?.(data)
    else if (eventName === 'delta') onDelta?.(data?.delta ?? '')
    else if (eventName === 'tool') onTool?.(data)
    else if (eventName === 'thinking') onThinking?.(data?.delta ?? '')
    else if (eventName === 'status') onStatus?.(data?.status || '')
    else if (eventName === 'done') {
      donePayload = data
      onDone?.(data)
    } else if (eventName === 'error') onError?.(data?.msg || '对话失败')
  }

  const flushBlock = (block) => {
    if (!block.trim()) return
    let eventName = 'message'
    const dataLines = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
    }
    dispatch(eventName, dataLines.join('\n'))
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    buffer = buffer.replace(/\r\n/g, '\n')
    let idx
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      flushBlock(block)
    }
  }
  if (buffer.trim()) flushBlock(buffer)
  return donePayload
}
