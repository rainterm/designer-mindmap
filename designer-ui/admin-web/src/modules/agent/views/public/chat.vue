<template>
  <div class="page-container session-chat is-standalone">
    <div v-if="fatal" class="fatal-wrap">
      <div class="fatal-title">无法打开</div>
      <div class="fatal-desc">{{ fatal }}</div>
    </div>

    <div v-else class="chat-shell">
      <aside class="chat-side">
        <div class="side-top">
          <div class="side-agent-row">
            <div class="side-agent">
              <div class="side-avatar" :class="{ 'has-img': !!agent?.avatar }">
                <img v-if="agent?.avatar" :src="agent.avatar" alt="" class="avatar-img" />
                <template v-else>{{ initials(agent?.name) }}</template>
              </div>
              <div class="side-agent-meta">
                <div class="side-name">{{ agent?.name || '智能体' }}</div>
                <div class="side-hint">会话记录</div>
              </div>
            </div>
          </div>
          <n-button
            type="primary"
            secondary
            block
            :loading="creating"
            :disabled="sending || creating || booting"
            @click="handleNewSession"
          >
            新建会话
          </n-button>
        </div>

        <div class="session-list">
          <n-spin :show="listLoading">
            <div v-if="!sessionList.length && !listLoading" class="session-empty">暂无会话</div>
            <div
              v-for="item in sessionList"
              :key="item.id"
              class="session-item"
              :class="{ active: String(item.id) === String(currentId) }"
              @click="switchSession(item.id)"
            >
              <div class="session-main">
                <div class="session-title">{{ item.title || '新会话' }}</div>
                <div class="session-meta">
                  {{ formatTime(item.lastMessageAt || item.createTime) }}
                </div>
              </div>
              <span v-if="item.messageCount" class="session-count">{{ item.messageCount }} 条</span>
              <n-button
                quaternary
                type="error"
                class="session-del"
                :loading="deletingId === item.id"
                :disabled="sending || creating || !!deletingId"
                @click.stop="confirmDeleteSession(item)"
              >
                <template #icon>
                  <n-icon :component="TrashOutline" />
                </template>
              </n-button>
            </div>
          </n-spin>
        </div>
      </aside>

      <section class="chat-panel">
        <div ref="listRef" class="msg-list">
          <div v-if="!visibleMessages.length && !sending && !loading && !booting" class="msg-empty">
            <div class="empty-title">开始对话</div>
            <div class="empty-desc">有什么想问的，直接发给我吧</div>
          </div>

          <div
            v-for="m in visibleMessages"
            :key="m.id || m._key"
            class="msg-row"
            :class="`role-${m.role}`"
          >
            <div class="msg-head">
              <div
                class="msg-avatar"
                :class="[m.role, { 'has-img': m.role === 'assistant' && !!agent?.avatar }]"
              >
                <img
                  v-if="m.role === 'assistant' && agent?.avatar"
                  :src="agent.avatar"
                  alt=""
                  class="avatar-img"
                />
                <template v-else>{{ roleGlyph(m.role) }}</template>
              </div>
              <div class="msg-nick">{{ roleName(m.role) }}</div>
              <time v-if="m.createdAt" class="msg-time">{{ m.createdAt }}</time>
            </div>
            <div class="msg-body">
              <div v-if="m.role === 'assistant'" class="msg-bubble msg-md-wrap">
                <AgentTraceTimeline
                  v-if="traceStepsOf(m).length"
                  :steps="traceStepsOf(m)"
                  :preview-prefix="m.id || m._key"
                />
                <template v-if="m.content">
                  <MdPreview
                    :id="`public-md-${m.id || m._key}`"
                    :model-value="m.content"
                    theme="light"
                    preview-theme="default"
                    class="msg-md"
                  />
                  <span v-if="m.streaming" class="stream-caret" />
                  <HtmlArtifactList v-if="!m.streaming" :content="m.content" />
                  <KnowledgeSourceFooter
                    v-if="!m.streaming"
                    :sources="knowledgeSourcesOf(m)"
                  />
                </template>
                <MindmapToolLink v-if="!m.streaming" :tools="m.tools" />
                <div v-if="m.streaming" class="msg-loading">{{ streamStatus(m) }}</div>
              </div>
              <div v-else class="msg-bubble">
                <div v-if="m.content">{{ m.content }}</div>
              </div>
            </div>
          </div>
        </div>

        <footer class="composer">
          <div class="composer-box" :class="{ disabled: sending || loading || booting || !currentId }">
            <n-input
              v-model:value="draft"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 8 }"
              placeholder="请输入内容"
              :disabled="sending || loading || booting || !currentId"
              :bordered="false"
              class="composer-input"
              @keydown="onKeydown"
            />
            <div class="composer-bar">
              <div class="composer-bar-right">
                <button
                  type="button"
                  class="composer-icon-btn is-send"
                  :disabled="sending || !draft.trim() || !currentId"
                  title="发送"
                  @click="handleSend"
                >
                  <n-spin v-if="sending" :size="14" />
                  <n-icon v-else :component="ArrowUpOutline" :size="18" />
                </button>
              </div>
            </div>
          </div>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDialog, useMessage } from 'naive-ui'
import { ArrowUpOutline, TrashOutline } from '@vicons/ionicons5'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import {
  createPublicSession,
  deletePublicSessions,
  getPublicAgent,
  listPublicMessages,
  listPublicSessions,
  openPublicSession,
  sendPublicMessageStream
} from '@/modules/agent/api'
import AgentTraceTimeline from '@/modules/agent/components/AgentTraceTimeline.vue'
import HtmlArtifactList from '@/modules/agent/components/HtmlArtifactList.vue'
import MindmapToolLink from '@/modules/agent/components/MindmapToolLink.vue'
import KnowledgeSourceFooter from '@/modules/agent/components/KnowledgeSourceFooter.vue'
import {
  appendThinkingDelta,
  buildTraceStepsFromMessage,
  extractKnowledgeSources,
  finishTraceSteps,
  stopThinkingRunning,
  upsertToolStep
} from '@/modules/agent/utils/traceSteps'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const token = String(route.params.token || '')

const agent = ref(null)
const sessionList = ref([])
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const creating = ref(false)
const deletingId = ref(null)
const loading = ref(false)
const listLoading = ref(false)
const booting = ref(true)
const fatal = ref('')
const listRef = ref(null)
let streamAbort = null

const currentId = computed(() => route.query.id)
const visibleMessages = computed(() => messages.value.filter((m) => m.role !== 'system'))

function initials(name) {
  return String(name || 'A').trim().slice(0, 1)
}

function roleGlyph(role) {
  if (role === 'user') return '你'
  if (role === 'assistant') return initials(agent.value?.name)
  return '系'
}

function roleName(role) {
  if (role === 'user') return '我'
  if (role === 'assistant') return agent.value?.name || '智能体'
  return '系统'
}

function streamStatus(m) {
  if (m?.statusText) return m.statusText
  if (m?.content || m?.tools?.length || m?.thinking) return '继续生成中…'
  return '正在回复…'
}

function traceStepsOf(m) {
  return buildTraceStepsFromMessage(m)
}

function knowledgeSourcesOf(m) {
  return extractKnowledgeSources(m)
}

function formatTime(v) {
  if (!v) return ''
  const s = String(v)
  return s.length >= 16 ? s.slice(5, 16) : s
}

function nowText() {
  return new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}

async function scrollBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function loadSessionList() {
  listLoading.value = true
  try {
    const res = await listPublicSessions(token)
    sessionList.value = res.data || []
  } finally {
    listLoading.value = false
  }
}

async function loadMessages(sessionId) {
  if (!sessionId) {
    messages.value = []
    return
  }
  loading.value = true
  try {
    const res = await listPublicMessages(token, sessionId)
    messages.value = (res.data || []).filter((m) => m.role !== 'system')
    await scrollBottom()
  } finally {
    loading.value = false
  }
}

function switchSession(id) {
  if (String(id) === String(currentId.value) || sending.value || deletingId.value) return
  router.replace({ path: route.path, query: { id } })
}

function confirmDeleteSession(item) {
  if (!item?.id || sending.value || deletingId.value) return
  dialog.warning({
    title: '删除会话',
    content: `确定删除「${item.title || '新会话'}」吗？删除后不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => handleDeleteSession(item)
  })
}

async function handleDeleteSession(item) {
  deletingId.value = item.id
  try {
    await deletePublicSessions(token, [item.id])
    message.success('已删除')
    const wasCurrent = String(item.id) === String(currentId.value)
    await loadSessionList()
    if (!wasCurrent) return
    const next = sessionList.value[0]
    if (next?.id) {
      router.replace({ path: route.path, query: { id: next.id } })
    } else {
      await handleNewSession()
    }
  } catch (e) {
    message.error(e?.message || '删除失败')
  } finally {
    deletingId.value = null
  }
}

async function handleNewSession() {
  if (creating.value || sending.value) return
  creating.value = true
  try {
    const res = await createPublicSession(token, { title: '新会话' })
    const created = res.data
    if (!created?.id) {
      message.error('创建会话失败')
      return
    }
    await loadSessionList()
    router.replace({ path: route.path, query: { id: created.id } })
  } catch (e) {
    message.error(e?.message || '创建会话失败')
  } finally {
    creating.value = false
  }
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || sending.value || !currentId.value) return
  draft.value = ''
  sending.value = true

  const userTmpId = `tmp-u-${Date.now()}`
  const assistantTmpId = `tmp-a-${Date.now()}`
  messages.value.push(
    {
      id: userTmpId,
      role: 'user',
      content,
      createdAt: nowText()
    },
    {
      id: assistantTmpId,
      role: 'assistant',
      content: '',
      thinking: '',
      tools: [],
      traceSteps: [],
      statusText: '正在回复…',
      createdAt: nowText(),
      streaming: true
    }
  )
  await scrollBottom()

  const patchAssistant = (fn) => {
    const idx = messages.value.findIndex((m) => m.id === assistantTmpId)
    if (idx < 0) return
    const next = { ...messages.value[idx] }
    fn(next)
    messages.value.splice(idx, 1, next)
  }

  streamAbort?.abort()
  streamAbort = new AbortController()
  try {
    await sendPublicMessageStream(
      token,
      currentId.value,
      content,
      {
        onUser: (user) => {
          const idx = messages.value.findIndex((m) => m.id === userTmpId)
          if (idx >= 0 && user) messages.value.splice(idx, 1, user)
        },
        onDelta: async (delta) => {
          patchAssistant((m) => {
            m.content = (m.content || '') + (delta || '')
            m.statusText = ''
          })
          await scrollBottom()
        },
        onTool: (tool) => {
          patchAssistant((m) => {
            m.tools = [...(m.tools || []), tool]
            m.traceSteps = upsertToolStep(m.traceSteps, tool)
          })
        },
        onThinking: (delta) => {
          patchAssistant((m) => {
            m.thinking = (m.thinking || '') + (delta || '')
            m.traceSteps = appendThinkingDelta(m.traceSteps, delta)
          })
        },
        onStatus: (status) => {
          patchAssistant((m) => {
            m.statusText = status || ''
          })
        },
        onDone: async (payload) => {
          patchAssistant((m) => {
            if (payload?.assistant) {
              Object.assign(m, payload.assistant, { streaming: false, statusText: '' })
            } else {
              m.streaming = false
              m.statusText = ''
            }
            m.traceSteps = finishTraceSteps(stopThinkingRunning(m.traceSteps || []))
          })
          if (payload?.user?.id) {
            const idx = messages.value.findIndex((m) => m.id === userTmpId)
            if (idx >= 0) messages.value.splice(idx, 1, payload.user)
          }
          await loadSessionList()
        },
        onError: (msg) => {
          patchAssistant((m) => {
            m.content = m.content || `对话失败：${msg}`
            m.streaming = false
            m.statusText = ''
            m.traceSteps = finishTraceSteps(stopThinkingRunning(m.traceSteps || []))
          })
          message.error(msg || '对话失败')
        }
      },
      streamAbort.signal
    )
  } catch (e) {
    if (e?.name !== 'AbortError') {
      patchAssistant((m) => {
        m.content = m.content || `对话失败：${e?.message || '未知错误'}`
        m.streaming = false
        m.statusText = ''
      })
      message.error(e?.message || '对话失败')
    }
  } finally {
    sending.value = false
    await scrollBottom()
  }
}

async function boot() {
  if (!token) {
    fatal.value = '链接无效'
    booting.value = false
    return
  }
  try {
    const info = await getPublicAgent(token)
    agent.value = info.data
    await loadSessionList()
    if (currentId.value) {
      const exists = sessionList.value.some((s) => String(s.id) === String(currentId.value))
      if (!exists) {
        const opened = await openPublicSession(token)
        await loadSessionList()
        router.replace({ path: route.path, query: { id: opened.data?.id } })
      }
    } else {
      const opened = await openPublicSession(token)
      await loadSessionList()
      if (opened.data?.id) {
        router.replace({ path: route.path, query: { id: opened.data.id } })
      }
    }
  } catch (e) {
    fatal.value = e?.message || '无法打开公开对话'
  } finally {
    booting.value = false
  }
}

watch(
  () => currentId.value,
  (id) => {
    if (!booting.value && !fatal.value) {
      loadMessages(id)
    }
  }
)

onMounted(async () => {
  await boot()
  if (currentId.value && !fatal.value) {
    await loadMessages(currentId.value)
  }
})
</script>

<style scoped>
.page-container.session-chat {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0;
  background: var(--color-body, #f5f5f5);
}
.page-container.session-chat.is-standalone {
  height: 100vh;
  padding: 0;
  background: var(--color-body, #f5f5f5);
}
.page-container.session-chat.is-standalone .chat-shell {
  border-radius: 0;
  border: none;
  box-shadow: none;
}
.chat-shell {
  flex: 1;
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr);
  border-radius: calc(var(--radius, 8px) + 2px);
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: 0 1px 2px color-mix(in srgb, var(--color-text) 4%, transparent);
}
.chat-side {
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: color-mix(in srgb, var(--color-body) 55%, var(--color-surface));
  border-right: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
}
.side-top {
  flex-shrink: 0;
  padding: 12px 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-bottom: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
}
.side-agent-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.side-agent {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}
.side-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  overflow: hidden;
  background: linear-gradient(
    145deg,
    var(--color-primary),
    color-mix(in srgb, var(--color-primary) 65%, #1a3d32)
  );
}
.side-avatar.has-img {
  background: var(--color-body);
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.side-agent-meta {
  min-width: 0;
}
.side-name {
  font-weight: 600;
  font-size: 14px;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.side-hint {
  margin-top: 2px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.session-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 8px;
}
.session-list :deep(.n-spin-container),
.session-list :deep(.n-spin-content) {
  min-height: 80px;
}
.session-empty {
  padding: 24px 8px;
  text-align: center;
  font-size: 12px;
  color: var(--color-text-muted);
}
.session-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  text-align: left;
  border: 1px solid transparent;
  background: transparent;
  border-radius: 10px;
  padding: 8px 6px 8px 10px;
  margin-bottom: 4px;
  cursor: pointer;
  color: inherit;
  box-sizing: border-box;
}
.session-item:hover {
  background: color-mix(in srgb, var(--color-surface) 85%, transparent);
}
.session-item.active {
  background: color-mix(in srgb, var(--color-primary) 10%, var(--color-surface));
  border-color: color-mix(in srgb, var(--color-primary) 28%, transparent);
}
.session-main {
  flex: 1;
  min-width: 0;
}
.session-count {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1;
  white-space: nowrap;
}
.session-del {
  flex-shrink: 0;
  opacity: 0.35;
}
.session-item:hover .session-del,
.session-item.active .session-del {
  opacity: 1;
}
.session-title {
  font-size: 13px;
  font-weight: 560;
  line-height: 1.35;
  color: var(--color-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}
.session-meta {
  margin-top: 4px;
  font-size: 11px;
  color: var(--color-text-muted);
}
.chat-panel {
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
}
.msg-list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--color-surface);
}
.msg-empty {
  margin: auto;
  text-align: center;
  padding: 24px;
}
.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
}
.empty-desc {
  font-size: 13px;
  color: var(--color-text-muted);
}
.msg-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: min(720px, 92%);
  align-items: stretch;
}
.msg-row.role-user {
  align-self: flex-end;
}
.msg-row.role-assistant,
.msg-row.role-system {
  align-self: flex-start;
}
.msg-head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.role-user .msg-head {
  flex-direction: row-reverse;
}
.msg-avatar {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  overflow: hidden;
  background: var(--color-text-muted);
}
.msg-avatar.user {
  background: var(--color-primary);
}
.msg-avatar.assistant {
  background: color-mix(in srgb, var(--color-text-secondary) 75%, #2c3a34);
}
.msg-avatar.has-img {
  background: var(--color-body);
}
.msg-nick {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.msg-time {
  font-size: 11px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.msg-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.role-user .msg-body {
  align-items: flex-end;
}
.msg-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.55;
  font-size: 14px;
  color: var(--color-text);
  background: color-mix(in srgb, var(--color-body) 70%, var(--color-surface));
}
.role-user .msg-bubble {
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-surface));
  border-top-right-radius: 4px;
}
.role-assistant .msg-bubble {
  border-top-left-radius: 4px;
}
.msg-md-wrap {
  white-space: normal;
  position: relative;
  min-width: min(280px, 100%);
}
.msg-md {
  background: transparent !important;
}
.msg-md :deep(.md-editor-preview-wrapper) {
  padding: 0;
}
.msg-md :deep(.md-editor-preview) {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text);
}
.msg-md :deep(.md-editor-preview > :first-child) {
  margin-top: 0;
}
.msg-md :deep(.md-editor-preview > :last-child) {
  margin-bottom: 0;
}
.msg-loading {
  display: block;
  width: fit-content;
  max-width: 100%;
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--color-text-muted);
}
.stream-caret {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: var(--color-primary);
  animation: caret-blink 0.9s steps(1) infinite;
}
.composer {
  flex-shrink: 0;
  padding: 12px 16px 16px;
  background: var(--color-surface);
  border-top: 1px solid color-mix(in srgb, var(--color-border) 55%, transparent);
}
.composer-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px 12px;
  border: 1px solid color-mix(in srgb, var(--color-border) 90%, #c8cdd3);
  border-radius: 20px;
  background: var(--color-surface);
  box-shadow: 0 2px 10px color-mix(in srgb, var(--color-text) 5%, transparent);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.composer-box:focus-within {
  border-color: color-mix(in srgb, var(--color-primary) 40%, var(--color-border));
  box-shadow: 0 2px 14px color-mix(in srgb, var(--color-primary) 10%, transparent);
}
.composer-box.disabled {
  opacity: 0.72;
}
.composer-input {
  width: 100%;
}
.composer-input :deep(.n-input) {
  background: transparent !important;
}
.composer-input :deep(.n-input__border),
.composer-input :deep(.n-input__state-border) {
  display: none !important;
}
.composer-input :deep(.n-input-wrapper) {
  padding: 0 !important;
}
.composer-input :deep(textarea) {
  font-size: 15px;
  line-height: 1.55;
  resize: none;
  padding: 0 !important;
  min-height: 48px;
  color: var(--color-text);
}
.composer-input :deep(textarea::placeholder) {
  color: var(--color-text-muted);
}
.composer-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  min-height: 36px;
}
.composer-bar-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-left: auto;
}
.composer-icon-btn {
  box-sizing: border-box;
  width: 34px;
  height: 34px;
  min-width: 34px;
  min-height: 34px;
  aspect-ratio: 1;
  flex-shrink: 0;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--color-border) 95%, #c5cad1);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  padding: 0;
  overflow: hidden;
}
.composer-icon-btn.is-send {
  border-color: transparent;
  background: var(--color-primary);
  color: #fff;
}
.composer-icon-btn.is-send:hover:not(:disabled) {
  background: color-mix(in srgb, var(--color-primary) 88%, #000);
}
.composer-icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.composer-icon-btn.is-send :deep(.n-spin-body) {
  --n-color: #fff;
}
.fatal-wrap {
  margin: auto;
  text-align: center;
  padding: 48px 24px;
}
.fatal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}
.fatal-desc {
  font-size: 13px;
  color: var(--color-error, #d03050);
}
@keyframes caret-blink {
  50% {
    opacity: 0;
  }
}
@media (max-width: 860px) {
  .chat-shell {
    grid-template-columns: 1fr;
  }
  .chat-side {
    display: none;
  }
}
</style>
