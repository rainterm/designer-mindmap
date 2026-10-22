/** 开始节点输入参数 schema */

/** 系统内置参数，不可改名/删除 */
export const SYSTEM_START_INPUT_KEY = 'query'

export function systemStartInput() {
  return {
    key: SYSTEM_START_INPUT_KEY,
    label: '查询内容',
    required: true,
    defaultValue: '',
    system: true
  }
}

export function isSystemStartInput(item) {
  return item?.system === true || item?.key === SYSTEM_START_INPUT_KEY
}

export function defaultStartInputs() {
  return [systemStartInput()]
}

export function defaultStartConfig() {
  return JSON.stringify({ inputs: defaultStartInputs() })
}

/** 规范化开始节点 config，确保系统参数写入 */
export function normalizeStartConfig(config) {
  return JSON.stringify({ inputs: parseStartInputs(config) })
}

export function parseStartInputs(config) {
  const raw = (config || '').trim()
  let list = []
  if (raw.startsWith('{')) {
    try {
      const obj = JSON.parse(raw)
      if (Array.isArray(obj?.inputs)) {
        // 保留空 key 项（用户正在编辑时清空了 key），让用户继续输入
        list = obj.inputs.map((item, i) => normalizeInput(item, i))
      }
    } catch {
      list = []
    }
  }
  return ensureSystemInputs(list)
}

function normalizeInput(item, index) {
  const hasKey = item?.key != null
  const key = String(item?.key ?? '')
    .trim()
    .replace(/[^a-zA-Z0-9_]/g, '_')
  // 仅在原 key 缺失时填充默认值；空字符串允许保留，方便用户继续输入
  const normalizedKey = key || (hasKey ? '' : `arg${index + 1}`)
  if (normalizedKey === SYSTEM_START_INPUT_KEY) {
    return systemStartInput()
  }
  return {
    key: normalizedKey,
    label: String(item?.label ?? (normalizedKey || `参数${index + 1}`)),
    required: item?.required !== false,
    defaultValue: item?.defaultValue == null ? '' : String(item.defaultValue),
    system: false
  }
}

/** 保证系统参数 query 始终存在且排在最前。允许空 key 临时存在，以便用户继续输入。 */
export function ensureSystemInputs(list = []) {
  const rest = (list || []).filter((x) => x && x.key !== SYSTEM_START_INPUT_KEY)
  return [systemStartInput(), ...rest]
}

/** 从工作流 nodes 找到开始节点的输入定义 */
export function startInputsFromNodes(nodes = []) {
  const start = (nodes || []).find((n) => (n.type || n.data?.nodeType) === 'start')
  if (!start) return ensureSystemInputs([])
  const config = start.config ?? start.data?.config ?? ''
  return parseStartInputs(config)
}

export function emptyParamValues(inputs) {
  const vals = {}
  for (const p of inputs || []) {
    vals[p.key] = p.defaultValue ?? ''
  }
  return vals
}
