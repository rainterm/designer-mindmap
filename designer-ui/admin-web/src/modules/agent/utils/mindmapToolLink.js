/** 导图工具名（含历史 create/update 别名） */
export const MINDMAP_TOOL_NAMES = new Set([
  'mm_project_save',
  'mm_project_create',
  'mm_project_update'
])

/** iframe → 父页：导图已落库，可刷新设计页 */
export const MM_PROJECT_SAVED_EVENT = 'designer:mm-project-saved'

/**
 * 从工具调用列表中提取可打开的导图链接。
 * @param {Array} tools ChatToolCallDTO 列表
 * @returns {{ projectId: number|string, name: string, editPath: string }[]}
 */
export function extractMindmapLinks(tools = []) {
  const out = []
  const seen = new Set()
  for (const t of tools || []) {
    const item = extractMindmapSave(t)
    if (!item) continue
    const key = String(item.projectId)
    if (seen.has(key)) continue
    seen.add(key)
    out.push(item)
  }
  return out
}

/**
 * 解析单次导图工具成功结果；未成功返回 null。
 * @param {object} tool ChatToolCallDTO
 */
export function extractMindmapSave(tool) {
  if (!tool || tool.phase === 'start' || tool.error) return null
  const name = String(tool.toolName || '').trim()
  if (!MINDMAP_TOOL_NAMES.has(name)) return null
  const parsed = parseToolResult(tool.result)
  const projectId = parsed?.projectId
  if (projectId == null || projectId === '') return null
  return {
    projectId,
    name: parsed.name || `导图 #${projectId}`,
    editPath: parsed.editPath || `/mindmap/project/edit?id=${projectId}`,
    toolName: name
  }
}

/** 嵌入对话通知父页面刷新设计画布 */
export function notifyParentMindmapSaved(payload) {
  if (!payload?.projectId || typeof window === 'undefined') return
  if (window.parent === window) return
  window.parent.postMessage(
    {
      type: MM_PROJECT_SAVED_EVENT,
      projectId: payload.projectId,
      toolName: payload.toolName || '',
      name: payload.name || ''
    },
    window.location.origin
  )
}

function parseToolResult(result) {
  if (result == null) return null
  if (typeof result === 'object') return result
  const text = String(result).trim()
  if (!text) return null
  try {
    const obj = JSON.parse(text)
    if (obj && obj.ok === false) return null
    return obj
  } catch {
    return null
  }
}
