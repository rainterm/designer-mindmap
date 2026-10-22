/** 工作流节点组件（左侧面板可拖拽） */

import { MarkerType } from '@vue-flow/core'
import { defaultStartConfig, normalizeStartConfig, parseStartInputs } from './startInputs'

export const NODE_TYPE_META = {
  start: { label: '开始', color: '#1f6f54', desc: '流程入口' },
  llm: { label: 'LLM', color: '#2a6f97', desc: '大模型推理' },
  knowledge: { label: '知识库', color: '#2f6b4f', desc: '检索知识库文档' },
  http: { label: 'HTTP', color: '#3a5f8a', desc: '发起 HTTP 请求' },
  code: { label: '代码', color: '#6a4c7a', desc: '执行 JavaScript / Python' },
  sql: { label: 'SQL', color: '#7a4a3a', desc: '只读 SQL 查询' },
  condition: { label: '条件', color: '#7a5c3a', desc: '分支判断' },
  output: { label: '输出', color: '#3d5a80', desc: '组装最终输出' },
  end: { label: '结束', color: '#555', desc: '流程出口' }
}

/** 左侧可拖拽类型（Tool/MCP/Skill 已并入 LLM 节点配置） */
export const PALETTE_TYPES = [
  'start',
  'llm',
  'knowledge',
  'http',
  'code',
  'sql',
  'condition',
  'output',
  'end'
]

export function nodeMeta(type) {
  return NODE_TYPE_META[type] || { label: type, color: '#666', desc: '' }
}

export function createNodeKey(type) {
  return `${type}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 6)}`
}

export function defaultNodeName(type) {
  return nodeMeta(type).label
}

export function defaultLlmConfig() {
  return {
    providerId: null,
    modelCode: '',
    temperature: 0.7,
    topP: 1,
    maxTokens: null,
    systemPrompt: '',
    userPrompt: '{query}',
    skillCodes: [],
    toolCodes: [],
    mcpCodes: [],
    /** 是否作为回答内容对外输出（否：仅供下游节点引用） */
    emitAnswer: false,
    /** 对外输出时是否流式推送 */
    stream: true,
    /** 是否输出模型思考过程 */
    emitThinking: false
  }
}

export function defaultNodeConfig(type) {
  switch (type) {
    case 'start':
      return defaultStartConfig()
    case 'llm':
      return JSON.stringify(defaultLlmConfig())
    case 'http':
      return JSON.stringify({
        method: 'GET',
        url: '',
        headerFields: [],
        bodyType: 'form',
        body: '',
        formFields: [],
        timeoutSec: 15
      })
    case 'knowledge':
      return JSON.stringify({ knowledgeId: null, query: '{query}', topK: 5, scoreThreshold: 0.35 })
    case 'code':
      return JSON.stringify({
        language: 'js',
        script:
          '// 变量：开始节点参数、上游 output（字符串）；JSON 上游另有 obj\n' +
          '// 可用 return 返回，或把结果写在最后一行\n' +
          'var text = output || query || "";\n' +
          'text;'
      })
    case 'sql':
      return JSON.stringify({ datasourceId: null, datasources: [], sql: 'SELECT 1 AS ok' })
    case 'output':
      return JSON.stringify({ type: 'text', template: '{output}' })
    case 'condition':
      return defaultConditionConfig()
    default:
      return ''
  }
}

export function defaultConditionConfig() {
  return JSON.stringify({
    cases: [{ id: 'if', label: '', match: '' }]
  })
}

function sanitizeBranchId(id) {
  const s = String(id || '').trim()
  if (/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(s) && s !== 'else') return s
  return ''
}

/** 条件节点：如果 / 否则如果… / 否则 */
export function parseConditionConfig(config) {
  const raw = (config || '').trim()
  if (raw.startsWith('{')) {
    try {
      const parsed = JSON.parse(raw)
      if (parsed && Array.isArray(parsed.cases)) {
        const cases = parsed.cases
          .map((c, i) => ({
            id: sanitizeBranchId(c?.id) || `elif${i + 1}`,
            label: String(c?.label || '').trim(),
            match: String(c?.match ?? '')
          }))
          .filter((c) => c.id && c.id !== 'else' && c.id !== 'false')
        if (cases.length) {
          return { cases, elseId: 'else' }
        }
      }
    } catch {
      // fallthrough
    }
  }
  return {
    cases: [{ id: 'true', label: '', match: raw }],
    elseId: 'false'
  }
}

export function conditionBranches(config) {
  const parsed = parseConditionConfig(config)
  const cases = parsed.cases.map((c, i) => {
    const title = i === 0 ? '如果' : '否则如果'
    const m = (c.match || '').trim()
    return {
      id: c.id,
      match: c.match,
      title,
      label: m ? `${title}「${m.length > 8 ? m.slice(0, 8) + '…' : m}」` : title
    }
  })
  return [...cases, { id: parsed.elseId, match: '', title: '否则', label: '否则' }]
}

/** 节点是否支持输出变量（写入下游可引用） */
export function supportsOutputVar(type) {
  return !['start', 'end', 'condition', 'output'].includes(type)
}

function parseJsonConfig(config) {
  if (!config || !String(config).trim().startsWith('{')) return null
  try {
    return JSON.parse(config)
  } catch {
    return null
  }
}

/** 画布节点底部摘要（按类型） */
export function configSummary(type, config, lookups = {}) {
  const c = (config || '').trim()
  if (type === 'start') {
    const inputs = parseStartInputs(c)
    if (!inputs.length) return '入口'
    if (inputs.length === 1) return `参数 {${inputs[0].key}}`
    return `${inputs.length} 个输入参数`
  }
  if (type === 'end') return '出口'
  if (type === 'output') {
    const cfg = parseJsonConfig(c) || {}
    const tpl = (cfg.template != null ? String(cfg.template) : c || '{output}').trim()
    if (!tpl) return '空模板'
    return tpl.length > 28 ? tpl.slice(0, 28) + '…' : tpl
  }
  if (type === 'llm') {
    const cfg = parseJsonConfig(c)
    if (cfg) {
      const model = (cfg.modelCode || '').trim()
      const extras = []
      const sn = Array.isArray(cfg.skillCodes) ? cfg.skillCodes.length : 0
      const tn = Array.isArray(cfg.toolCodes) ? cfg.toolCodes.length : 0
      const mn = Array.isArray(cfg.mcpCodes) ? cfg.mcpCodes.length : 0
      if (sn) extras.push(`Skill×${sn}`)
      if (tn) extras.push(`工具×${tn}`)
      if (mn) extras.push(`MCP×${mn}`)
      if (cfg.emitAnswer) extras.push(cfg.stream === false ? '整段输出' : '流式输出')
      else extras.push('仅下游')
      if (cfg.emitThinking) extras.push('思考')
      const extra = extras.length ? ` · ${extras.join(' ')}` : ''
      if (model) return model + extra
      return extras.length ? extras.join(' ') : '未配置模型'
    }
    if (!c) return '未配置'
    const line = c.split('\n')[0]
    return line.length > 28 ? line.slice(0, 28) + '…' : line
  }
  if (type === 'condition') {
    const branches = conditionBranches(c)
    const ifs = branches.filter((b) => b.title !== '否则')
    const n = Math.max(0, ifs.length - 1)
    const first = (ifs[0]?.match || '').trim()
    if (!first && n === 0) return '如果 / 否则'
    if (n === 0) return `如果「${first.length > 12 ? first.slice(0, 12) + '…' : first}」`
    return `如果 + ${n} 个否则如果`
  }
  if (type === 'http') {
    const cfg = parseJsonConfig(c) || {}
    const method = cfg.method || 'GET'
    const url = (cfg.url || '').trim()
    if (!url) return `${method} 未填 URL`
    return `${method} ${url.length > 22 ? url.slice(0, 22) + '…' : url}`
  }
  if (type === 'knowledge') {
    const cfg = parseJsonConfig(c) || {}
    const id = cfg.knowledgeId
    if (!id) return '未选择知识库'
    const map = lookups.knowledge || {}
    return map[id] || map[String(id)] || `知识库 #${id}`
  }
  if (type === 'code') {
    const cfg = parseJsonConfig(c) || {}
    const lang = String(cfg.language || 'js').toLowerCase()
    const langTag = lang === 'python' || lang === 'py' ? 'Python' : 'JS'
    const script = (cfg.script || c || '').trim()
    if (!script) return `${langTag} · 未填写脚本`
    const commentPrefix = langTag === 'Python' ? '#' : '//'
    const line =
      script.split('\n').find((l) => l && !l.trim().startsWith(commentPrefix)) || script.split('\n')[0]
    const body = line.length > 22 ? line.slice(0, 22) + '…' : line
    return `${langTag} · ${body}`
  }
  if (type === 'sql') {
    const cfg = parseJsonConfig(c) || {}
    const sql = (cfg.sql || c || '').trim()
    if (!sql) return '未填写 SQL'
    return sql.length > 28 ? sql.slice(0, 28) + '…' : sql
  }
  return c || ''
}

function normalizeNodeConfig(config, nodeType) {
  if (config == null || config === '') return defaultNodeConfig(nodeType)
  if (typeof config === 'object') {
    try {
      return JSON.stringify(config)
    } catch {
      return defaultNodeConfig(nodeType)
    }
  }
  return String(config)
}

export function toFlowNodes(nodes = []) {
  return nodes.map((n, i) => {
    const nodeType = n.type || 'llm'
    let config = normalizeNodeConfig(n.config, nodeType)
    if (nodeType === 'start') {
      config = normalizeStartConfig(config)
    }
    return {
      id: n.key || `n${i + 1}`,
      type: 'workflow',
      deletable: nodeType !== 'start',
      position: {
        x: Number.isFinite(n.x) ? n.x : 80 + (i % 4) * 220,
        y: Number.isFinite(n.y) ? n.y : 120 + Math.floor(i / 4) * 140
      },
      data: {
        key: n.key,
        nodeType,
        name: n.name || defaultNodeName(nodeType),
        config,
        outputVar: nodeType === 'output' ? '' : (typeof n.outputVar === 'string' ? n.outputVar : '')
      }
    }
  })
}

export function toFlowEdges(edges = []) {
  return edges.map((e, i) => ({
    id: e.id || `e${i + 1}`,
    source: e.source,
    target: e.target,
    sourceHandle: e.sourceHandle || undefined,
    targetHandle: e.targetHandle || undefined,
    label: e.label || undefined,
    animated: false,
    type: 'default',
    markerEnd: MarkerType.ArrowClosed
  }))
}

export function fromFlowGraph(flowNodes = [], flowEdges = []) {
  const nodes = flowNodes.map((n) => {
    const type = n.data?.nodeType || 'llm'
    let config = n.data?.config || ''
    if (type === 'start') {
      config = normalizeStartConfig(config)
    }
    return {
      key: n.id,
      type,
      name: n.data?.name || defaultNodeName(type),
      config,
      outputVar: type === 'output' ? '' : (typeof n.data?.outputVar === 'string' ? n.data.outputVar.trim() : ''),
      x: n.position?.x ?? 0,
      y: n.position?.y ?? 0
    }
  })
  const edges = flowEdges.map((e, i) => ({
    id: e.id || `e${i + 1}`,
    source: e.source,
    target: e.target,
    sourceHandle: e.sourceHandle || null,
    targetHandle: e.targetHandle || null,
    label: typeof e.label === 'string' ? e.label : e.label?.toString?.() || ''
  }))
  return { nodes, edges }
}

export function defaultWorkflowGraph() {
  return {
    nodes: [
      { key: 'start', type: 'start', name: '开始', config: defaultStartConfig(), x: 160, y: 160 },
      { key: 'end', type: 'end', name: '结束', config: '', x: 480, y: 160 }
    ],
    edges: [{ id: 'e1', source: 'start', target: 'end' }]
  }
}
