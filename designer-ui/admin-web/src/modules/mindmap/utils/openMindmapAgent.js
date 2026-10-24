import { createSession, getSession, listAgents } from '@/modules/agent/api'

const MINDMAP_AGENT_CODE = 'mindmap'

function storageKey(projectId) {
  // v2：丢弃早期「无 mm_project_save / 只出 HTML」污染过的会话缓存
  return `admin-mindmap:ai-session:v2:${projectId}`
}

/**
 * 打开导图助手会话：同一 projectId 复用同一会话；无 projectId（列表 AI 生成）则新建。
 * @param {number|string} [projectId]
 * @returns {Promise<{ sessionId: number|string, agent: object }>}
 */
export async function openMindmapAgentSession(projectId) {
  const res = await listAgents()
  const list = res.data || []
  const agent =
    list.find((a) => a.code === MINDMAP_AGENT_CODE && a.status === 1) ||
    list.find((a) => a.code === MINDMAP_AGENT_CODE)
  if (!agent?.id) {
    throw new Error('未找到可用的导图助手，请先在智能体管理中启用「导图助手」（code: mindmap）')
  }

  const pid = projectId != null && String(projectId).trim() !== '' ? String(projectId) : null
  if (pid) {
    const cachedId = localStorage.getItem(storageKey(pid))
    if (cachedId) {
      try {
        const sRes = await getSession(cachedId)
        const session = sRes.data
        if (session?.id && String(session.agentId) === String(agent.id)) {
          return { sessionId: session.id, agent }
        }
      } catch {
        /* 会话已删则重建 */
      }
      localStorage.removeItem(storageKey(pid))
    }
  }

  const createRes = await createSession({
    agentId: agent.id,
    title: pid ? `导图 #${pid}` : '新会话'
  })
  const sessionId = createRes.data?.id
  if (!sessionId) {
    throw new Error('创建对话失败')
  }
  if (pid) {
    localStorage.setItem(storageKey(pid), String(sessionId))
  }
  return { sessionId, agent }
}
