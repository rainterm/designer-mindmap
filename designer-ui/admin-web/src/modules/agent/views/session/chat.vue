<template>
  <div class="page-container session-chat" :class="{ 'is-standalone': standalone }">
    <div class="chat-shell">
      <aside class="chat-side">
        <div class="side-top">
          <div class="side-agent-row">
            <div class="side-agent">
              <div class="side-avatar" :class="{ 'has-img': !!session?.agentAvatar }">
                <img v-if="session?.agentAvatar" :src="session.agentAvatar" alt="" class="avatar-img" />
                <template v-else>{{ initials(session?.agentName) }}</template>
              </div>
              <div class="side-agent-meta">
                <div class="side-name">{{ session?.agentName || '智能体' }}</div>
                <div class="side-hint">会话记录</div>
              </div>
            </div>
            <n-button quaternary class="back-btn" @click="goBack">返回</n-button>
          </div>
          <n-button
            v-perm="'agent:session:chat'"
            type="primary"
            secondary
            block
            :loading="creating"
            :disabled="sending || creating"
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
                v-perm="'agent:session:chat'"
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
          <div v-if="!visibleMessages.length && !sending && !loading" class="msg-empty">
            <div class="empty-title">开始对话</div>
            <div class="empty-desc">有什么想问的，直接发给我吧</div>
          </div>

          <div
            v-for="m in visibleMessages"
            :key="m.id"
            class="msg-row"
            :class="`role-${m.role}`"
          >
            <div class="msg-head">
              <div class="msg-avatar" :class="[m.role, { 'has-img': m.role === 'assistant' && !!session?.agentAvatar }]">
                <img
                  v-if="m.role === 'assistant' && session?.agentAvatar"
                  :src="session.agentAvatar"
                  alt=""
                  class="avatar-img"
                />
                <template v-else>{{ roleGlyph(m.role) }}</template>
              </div>
              <div class="msg-nick">{{ roleName(m.role) }}</div>
              <time class="msg-time">{{ m.createdAt }}</time>
            </div>
            <div class="msg-body">
              <div v-if="m.role === 'assistant'" class="msg-bubble msg-md-wrap">
                <AgentTraceTimeline
                  v-if="traceStepsOf(m).length"
                  :steps="traceStepsOf(m)"
                  :preview-prefix="m.id"
                />
                <template v-if="m.content">
                  <MdPreview
                    :id="`chat-md-${m.id}`"
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
                <div v-if="m.streaming" class="msg-loading">{{ streamStatus(m) }}</div>
              </div>
              <div v-else class="msg-bubble">
                <div v-if="m.attachments?.length" class="msg-files">
                  <template v-for="(f, i) in m.attachments" :key="`${m.id}-f-${i}`">
                    <a
                      v-if="isImageAttachment(f)"
                      class="msg-image"
                      :href="f.url"
                      target="_blank"
                      rel="noopener"
                      @click.stop
                    >
                      <img :src="f.url" :alt="f.name || '图片'" />
                    </a>
                    <a
                      v-else
                      class="msg-file"
                      :href="f.url"
                      target="_blank"
                      rel="noopener"
                      @click.stop
                    >
                      {{ f.name || '附件' }}
                    </a>
                  </template>
                </div>
                <div v-if="m.content">{{ m.content }}</div>
              </div>
            </div>
          </div>
        </div>

        <footer class="composer">
          <div class="composer-box" :class="{ disabled: sending || loading || !session }">
            <div v-if="pendingFiles.length" class="pending-files">
              <div
                v-for="(f, idx) in pendingFiles"
                :key="f.key"
                class="pending-file"
                :class="{ 'is-image': isImageAttachment(f) }"
              >
                <img v-if="isImageAttachment(f)" :src="f.url" :alt="f.name" class="pending-thumb" />
                <span v-else class="pending-name">{{ f.name }}</span>
                <button type="button" class="pending-del" :disabled="sending" @click="removePending(idx)">
                  ×
                </button>
              </div>
            </div>
            <n-input
              v-model:value="draft"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 8 }"
              placeholder="请输入内容"
              :disabled="sending || loading || !session"
              :bordered="false"
              class="composer-input"
              @keydown="onKeydown"
            />
            <div class="composer-bar">
              <div class="composer-bar-right">
                <n-upload
                  v-if="fileEnabled"
                  :show-file-list="false"
                  multiple
                  :disabled="sending || loading || uploading || !session"
                  :custom-request="handleUpload"
                >
                  <button
                    type="button"
                    class="composer-icon-btn"
                    :disabled="sending || loading || uploading || !session"
                    title="上传附件"
                  >
                    <n-spin v-if="uploading" :size="14" />
                    <n-icon v-else :component="AttachOutline" :size="18" />
                  </button>
                </n-upload>
                <button
                  v-perm="'agent:session:chat'"
                  type="button"
                  class="composer-icon-btn is-send"
                  :disabled="sending || (!draft.trim() && !pendingFiles.length) || !session"
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
import { ArrowUpOutline, AttachOutline, TrashOutline } from '@vicons/ionicons5'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { uploadFile } from '@/modules/system/api/file'
import {
  createSession,
  deleteSessionByIds,
  getSession,
  listMessages,
  listSessionsByAgent,
  sendMessageStream
} from '@/modules/agent/api'
import AgentTraceTimeline from '@/modules/agent/components/AgentTraceTimeline.vue'
import HtmlArtifactList from '@/modules/agent/components/HtmlArtifactList.vue'
import KnowledgeSourceFooter from '@/modules/agent/components/KnowledgeSourceFooter.vue'
import { isImageAttachment } from '@/modules/agent/utils/attachment'
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
const standalone = computed(() => !!route.meta?.standalone)

const session = ref(null)
const sessionList = ref([])
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const creating = ref(false)
const deletingId = ref(null)
const loading = ref(false)
const listLoading = ref(false)
const uploading = ref(false)
const pendingFiles = ref([])
const listRef = ref(null)

const currentId = computed(() => route.query.id)
const visibleMessages = computed(() => messages.value.filter((m) => m.role !== 'system'))
const fileEnabled = computed(() => session.value?.fileEnabled === 1)

const TEXT_EXTS = new Set([
  'txt',
  'md',
  'markdown',
  'json',
  'csv',
  'log',
  'xml',
  'yml',
  'yaml',
  'js',
  'ts',
  'jsx',
  'tsx',
  'vue',
  'java',
  'py',
  'go',
  'sql',
  'html',
  'css',
  'scss'
])

function roleGlyph(role) {
  if (role === 'user') return '你'
  if (role === 'assistant') return initials(session.value?.agentName)
  return '系'
}

function roleName(role) {
  if (role === 'user') return '我'
  if (role === 'assistant') return session.value?.agentName || '智能体'
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

function initials(name) {
  return String(name || 'A').trim().slice(0, 1)
}

function formatTime(v) {
  if (!v) return ''
  const s = String(v)
  return s.length >= 16 ? s.slice(5, 16) : s
}

function goBack() {
  if (standalone.value) {
    window.close()
    setTimeout(() => {
      if (!window.closed) router.push('/agent/store')
    }, 120)
    return
  }
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/agent/store')
}

async function scrollBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function loadSessionList(agentId) {
  if (!agentId) return
  listLoading.value = true
  try {
    const res = await listSessionsByAgent(agentId)
    sessionList.value = res.data || []
  } finally {
    listLoading.value = false
  }
}

async function loadSession(id) {
  if (!id) {
    message.error('缺少会话 ID')
    goBack()
    return
  }
  loading.value = true
  try {
    const [sRes, mRes] = await Promise.all([getSession(id), listMessages(id)])
    if (!sRes.data) {
      message.error('会话不存在')
      goBack()
      return
    }
    session.value = sRes.data
    messages.value = (mRes.data || []).map((m) => ({
      ...m,
      traceSteps: buildTraceStepsFromMessage(m)
    }))
    pendingFiles.value = []
    await loadSessionList(sRes.data.agentId)
    await scrollBottom()
  } finally {
    loading.value = false
  }
}

function removePending(idx) {
  pendingFiles.value.splice(idx, 1)
}

function isTextFile(file) {
  const name = String(file?.name || '')
  const ext = name.includes('.') ? name.split('.').pop().toLowerCase() : ''
  if (TEXT_EXTS.has(ext)) return true
  const type = String(file?.type || '')
  return type.startsWith('text/') || type === 'application/json'
}

async function readTextContent(file) {
  if (!isTextFile(file) || file.size > 200 * 1024) return ''
  try {
    return await file.text()
  } catch {
    return ''
  }
}

async function readImageBase64(file) {
  if (!String(file?.type || '').startsWith('image/') || file.size > 2 * 1024 * 1024) return ''
  try {
    const buf = await file.arrayBuffer()
    let binary = ''
    const bytes = new Uint8Array(buf)
    for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i])
    return btoa(binary)
  } catch {
    return ''
  }
}

async function handleUpload({ file, onFinish, onError }) {
  const raw = file?.file
  if (!raw) {
    onError?.()
    return
  }
  if (pendingFiles.value.length >= 10) {
    message.warning('最多上传 10 个附件')
    onError?.()
    return
  }
  uploading.value = true
  try {
    const res = await uploadFile(raw, 'agent-chat')
    const data = res.data || {}
    const [textContent, base64] = await Promise.all([readTextContent(raw), readImageBase64(raw)])
    pendingFiles.value.push({
      key: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      name: data.name || raw.name,
      url: data.url,
      contentType: raw.type || '',
      size: data.fileSize ?? raw.size,
      textContent: textContent || undefined,
      base64: base64 || undefined
    })
    onFinish?.()
  } catch (e) {
    message.error(e?.message || '上传失败')
    onError?.()
  } finally {
    uploading.value = false
  }
}

function switchSession(id) {
  if (String(id) === String(currentId.value) || sending.value || deletingId.value) return
  router.replace({ path: '/agent/session/chat', query: { id } })
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
    await deleteSessionByIds([item.id])
    message.success('已删除')
    const agentId = session.value?.agentId || item.agentId
    const wasCurrent = String(item.id) === String(currentId.value)
    await loadSessionList(agentId)
    if (!wasCurrent) return
    const next = sessionList.value[0]
    if (next?.id) {
      router.replace({ path: '/agent/session/chat', query: { id: next.id } })
    } else if (agentId) {
      const res = await createSession({
        agentId,
        title: '新会话'
      })
      if (res.data?.id) {
        await loadSessionList(agentId)
        router.replace({ path: '/agent/session/chat', query: { id: res.data.id } })
      } else {
        goBack()
      }
    } else {
      goBack()
    }
  } catch (e) {
    message.error(e?.message || '删除失败')
  } finally {
    deletingId.value = null
  }
}

async function handleNewSession() {
  if (!session.value?.agentId || creating.value || sending.value) return
  creating.value = true
  try {
    const res = await createSession({
      agentId: session.value.agentId,
      title: '新会话'
    })
    const created = res.data
    if (!created?.id) {
      message.error('创建会话失败')
      return
    }
    await loadSessionList(session.value.agentId)
    router.replace({ path: '/agent/session/chat', query: { id: created.id } })
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

function nowText() {
  return new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}

async function handleSend() {
  const text = draft.value.trim()
  const attachments = pendingFiles.value.map(
    ({ name, url, contentType, size, textContent, base64 }) => ({
      name,
      url,
      contentType,
      size,
      textContent,
      base64
    })
  )
  if ((!text && !attachments.length) || sending.value || !session.value) return
  sending.value = true
  draft.value = ''
  pendingFiles.value = []

  const userTmpId = `tmp-u-${Date.now()}`
  const assistantTmpId = `tmp-a-${Date.now()}`
  messages.value.push(
    {
      id: userTmpId,
      sessionId: session.value.id,
      role: 'user',
      content: text,
      attachments: attachments.map(({ name, url, contentType, size }) => ({
        name,
        url,
        contentType,
        size
      })),
      createdAt: nowText()
    },
    {
      id: assistantTmpId,
      sessionId: session.value.id,
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

  try {
    const done = await sendMessageStream(
      session.value.id,
      text,
      {
        onUser: (user) => {
          const idx = messages.value.findIndex((m) => m.id === userTmpId)
          if (idx >= 0 && user) messages.value.splice(idx, 1, user)
        },
        onDelta: async (delta) => {
          if (!delta) return
          patchAssistant((m) => {
            m.content = (m.content || '') + delta
            m.statusText = '正在生成…'
            m.traceSteps = stopThinkingRunning(m.traceSteps)
          })
          await scrollBottom()
        },
        onThinking: async (delta) => {
          if (!delta) return
          patchAssistant((m) => {
            m.thinking = (m.thinking || '') + delta
            m.traceSteps = appendThinkingDelta(m.traceSteps, delta)
            m.statusText = '正在思考…'
          })
          await scrollBottom()
        },
        onTool: async (tool) => {
          if (!tool) return
          patchAssistant((m) => {
            const list = [...(m.tools || [])]
            const i = list.findIndex((x) => x.callId && x.callId === tool.callId)
            if (i >= 0) list[i] = { ...list[i], ...tool }
            else list.push(tool)
            m.tools = list
            m.traceSteps = upsertToolStep(m.traceSteps, tool)
            const label = tool?.toolLabel || tool?.toolName || '工具'
            m.statusText = tool?.phase === 'start' ? `正在调用「${label}」…` : '工具已完成，继续生成…'
          })
          await scrollBottom()
        },
        onStatus: (status) => {
          if (!status) return
          patchAssistant((m) => {
            m.statusText = status
          })
        },
        onError: (msg) => {
          if (msg) message.error(msg)
        }
      },
      undefined,
      attachments
    )

    if (done?.user) {
      const uIdx = messages.value.findIndex((m) => m.id === userTmpId || m.id === done.user.id)
      if (uIdx >= 0) messages.value.splice(uIdx, 1, done.user)
    }
    if (done?.assistant) {
      const aIdx = messages.value.findIndex((m) => m.id === assistantTmpId)
      const prev = aIdx >= 0 ? messages.value[aIdx] : null
      const thinking = done.assistant.thinking || prev?.thinking || ''
      const tools = done.assistant.tools || prev?.tools || []
      const assistant = {
        ...done.assistant,
        thinking,
        tools,
        traceSteps: finishTraceSteps(prev?.traceSteps?.length
          ? prev.traceSteps
          : buildTraceStepsFromMessage({ thinking, tools })),
        streaming: false
      }
      if (aIdx >= 0) messages.value.splice(aIdx, 1, assistant)
      else messages.value.push(assistant)
    } else {
      patchAssistant((m) => {
        m.streaming = false
        m.traceSteps = finishTraceSteps(m.traceSteps)
        if (!m.content) m.content = '（无回复内容）'
      })
    }

    if (session.value) {
      const title = session.value.title || ''
      const isPlaceholder =
        !title || title === '新会话' || title.startsWith('调试 ·') || title.startsWith('对话 ·')
      if (isPlaceholder) {
        const firstUser = messages.value.find((m) => m.role === 'user' && typeof m.id === 'number')
        const src = (firstUser?.content || text).trim().replace(/\s+/g, ' ')
        session.value.title = src.slice(0, 24) || '新会话'
      }
      session.value.messageCount = visibleMessages.value.length
      await loadSessionList(session.value.agentId)
    }
    await scrollBottom()
  } catch (e) {
    patchAssistant((m) => {
      m.streaming = false
      m.content = m.content || e?.message || '对话失败'
    })
    message.error(e?.message || '对话失败')
  } finally {
    sending.value = false
  }
}

watch(
  () => route.query.id,
  (id, prev) => {
    if (id && String(id) !== String(prev)) loadSession(id)
  }
)

onMounted(() => loadSession(route.query.id))
</script>

<style scoped>
.page-container.session-chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--layout-nav-height, 44px) - var(--layout-tags-height, 36px));
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
  padding-bottom: 0;
}
.page-container.session-chat.is-standalone {
  height: 100vh;
  min-height: 100vh;
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
.back-btn {
  flex-shrink: 0;
  margin-left: auto;
  margin-right: -6px;
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
.msg-md :deep(pre) {
  margin: 8px 0;
  border-radius: 8px;
}
.msg-md :deep(code) {
  font-size: 13px;
}
.msg-md :deep(p) {
  margin: 0.4em 0;
}
.msg-md :deep(ul),
.msg-md :deep(ol) {
  margin: 0.4em 0;
  padding-left: 1.4em;
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
.composer-icon-btn.is-send :deep(.n-spin-body) {
  --n-color: #fff;
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
.pending-files {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}
.pending-file {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 2px 6px 2px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-body) 55%, var(--color-surface));
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  font-size: 12px;
}
.pending-file.is-image {
  padding: 4px;
  border-radius: 10px;
  gap: 2px;
}
.pending-thumb {
  display: block;
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 8px;
  background: var(--color-body);
}
.pending-name {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pending-del {
  border: 0;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0 2px;
}
.pending-del:hover:not(:disabled) {
  color: var(--color-error, #d03050);
}
.pending-del:disabled {
  cursor: not-allowed;
  opacity: 0.5;
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
.composer-bar-right :deep(.n-upload) {
  display: inline-flex;
  width: auto;
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
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}
.composer-icon-btn:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--color-primary) 40%, var(--color-border));
  color: var(--color-primary);
}
.composer-icon-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.composer-icon-btn.is-send {
  border-radius: 999px;
  border-color: transparent;
  background: var(--color-primary);
  color: #fff;
}
.composer-icon-btn.is-send:hover:not(:disabled) {
  background: color-mix(in srgb, var(--color-primary) 88%, #000);
  color: #fff;
}
.composer-icon-btn.is-send:disabled {
  opacity: 0.4;
}
.msg-files {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}
.msg-image {
  display: block;
  max-width: min(280px, 100%);
  border-radius: 10px;
  overflow: hidden;
  line-height: 0;
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
}
.msg-image img {
  display: block;
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  background: color-mix(in srgb, var(--color-body) 60%, transparent);
}
.msg-file {
  display: inline-flex;
  max-width: 100%;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-file:hover {
  text-decoration: underline;
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
