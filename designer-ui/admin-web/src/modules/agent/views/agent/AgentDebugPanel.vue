<template>
  <div class="debug-panel">
    <div class="debug-head">
      <div>
        <div class="debug-title">调试对话</div>
        <div class="debug-sub">{{ hint }}</div>
      </div>
      <n-button
        v-if="agentId"
        quaternary
        :disabled="sending || preparing"
        @click="resetChat"
      >
        清空
      </n-button>
    </div>

    <div v-if="!agentId" class="debug-empty">
      <div class="empty-title">保存后即可调试</div>
      <div class="empty-desc">先完善左侧配置并保存，右侧可直接试聊</div>
    </div>

    <template v-else>
      <div ref="listRef" class="debug-msgs">
        <div v-if="!visibleMessages.length && !sending" class="debug-empty soft">
          <div class="empty-title">开始试聊</div>
          <div class="empty-desc">调试使用当前草稿配置，保存后即可验证，无需发布</div>
        </div>

        <div
          v-for="m in visibleMessages"
          :key="m.id"
          class="msg-row"
          :class="`role-${m.role}`"
        >
          <div class="msg-head">
            <div class="msg-avatar" :class="[m.role, { 'has-img': m.role === 'assistant' && !!agentAvatar }]">
              <img
                v-if="m.role === 'assistant' && agentAvatar"
                :src="agentAvatar"
                alt=""
                class="avatar-img"
              />
              <template v-else>{{ roleGlyph(m.role) }}</template>
            </div>
            <div class="msg-nick">{{ roleName(m.role) }}</div>
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
                  :id="`debug-md-${m.id}`"
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

      <div class="debug-composer" :class="{ disabled: sending || preparing }">
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
          :autosize="{ minRows: 2, maxRows: 5 }"
          :placeholder="fileEnabled ? '输入内容或上传附件试聊…' : '输入内容试聊…'"
          :disabled="sending || preparing"
          :bordered="false"
          @keydown="onKeydown"
        />
        <div class="composer-bar">
          <div class="composer-bar-right">
            <n-upload
              v-if="fileEnabled"
              :show-file-list="false"
              multiple
              :disabled="sending || preparing || uploading"
              :custom-request="handleUpload"
            >
              <button
                type="button"
                class="icon-btn"
                :disabled="sending || preparing || uploading"
                title="上传附件"
              >
                <n-spin v-if="uploading" :size="14" />
                <n-icon v-else :component="AttachOutline" :size="18" />
              </button>
            </n-upload>
            <button
              type="button"
              class="send-btn"
              :disabled="sending || preparing || (!draft.trim() && !pendingFiles.length)"
              title="发送"
              @click="handleSend"
            >
              <n-spin v-if="sending || preparing" :size="14" />
              <n-icon v-else :component="ArrowUpOutline" :size="18" />
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { ArrowUpOutline, AttachOutline } from '@vicons/ionicons5'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { uploadFile } from '@/modules/system/api/file'
import { createSession, getSession, sendMessageStream } from '@/modules/agent/api'
import AgentTraceTimeline from '@/modules/agent/components/AgentTraceTimeline.vue'
import HtmlArtifactList from '@/modules/agent/components/HtmlArtifactList.vue'
import KnowledgeSourceFooter from '@/modules/agent/components/KnowledgeSourceFooter.vue'
import {
  appendThinkingDelta,
  buildTraceStepsFromMessage,
  extractKnowledgeSources,
  finishTraceSteps,
  stopThinkingRunning,
  upsertToolStep
} from '@/modules/agent/utils/traceSteps'
import { isImageAttachment } from '@/modules/agent/utils/attachment'

const props = defineProps({
  agentId: { type: [Number, String], default: null },
  agentName: { type: String, default: '' },
  agentAvatar: { type: String, default: '' },
  /** 与已保存配置一致；需开启附件上传并保存后调试才能带文件 */
  fileEnabled: { type: [Number, Boolean], default: 0 },
  /** 发送前钩子：父组件用它先自动保存未保存的草稿；返回 false 则中断发送 */
  beforeSend: { type: Function, default: null },
  /** true 时调试对话使用草稿配置（保存即生效，无需发布） */
  useDraft: { type: Boolean, default: false }
})

const message = useMessage()
const session = ref(null)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const preparing = ref(false)
const uploading = ref(false)
const pendingFiles = ref([])
const listRef = ref(null)

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

const visibleMessages = computed(() => messages.value.filter((m) => m.role !== 'system'))
const fileEnabled = computed(
  () => props.fileEnabled === 1 || props.fileEnabled === true || session.value?.fileEnabled === 1
)

const hint = computed(() => {
  if (!props.agentId) return '需先保存智能体'
  return props.agentName ? `与「${props.agentName}」对话` : '基于已保存配置试聊'
})

function roleGlyph(role) {
  if (role === 'user') return '你'
  return String(props.agentName || 'A').trim().slice(0, 1) || 'A'
}

function roleName(role) {
  if (role === 'user') return '我'
  return props.agentName || '智能体'
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

function nowText() {
  return new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
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

async function scrollBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function ensureSession() {
  if (session.value?.id) return session.value
  if (!props.agentId) return null
  preparing.value = true
  try {
    const res = await createSession({ agentId: props.agentId, title: '调试' })
    const id = res.data?.id
    if (!id) throw new Error('创建调试会话失败')
    const detail = await getSession(id)
    session.value = detail.data || res.data
    return session.value
  } finally {
    preparing.value = false
  }
}

async function resetChat() {
  session.value = null
  messages.value = []
  draft.value = ''
  pendingFiles.value = []
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
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
  if ((!text && !attachments.length) || sending.value || !props.agentId) return

  // 发送前钩子：父组件用它先自动保存未保存的草稿
  if (props.beforeSend) {
    let ok = false
    try {
      ok = (await props.beforeSend()) !== false
    } catch {
      ok = false
    }
    if (!ok) return
  }

  sending.value = true
  draft.value = ''
  pendingFiles.value = []

  try {
    const s = await ensureSession()
    if (!s?.id) throw new Error('调试会话未就绪')
    if (attachments.length && s.fileEnabled !== 1) {
      message.warning('请先开启「附件上传」并保存智能体')
      pendingFiles.value = attachments.map((a, i) => ({
        ...a,
        key: `restore-${Date.now()}-${i}`
      }))
      draft.value = text
      return
    }

    const userTmpId = `dbg-u-${Date.now()}`
    const assistantTmpId = `dbg-a-${Date.now()}`
    messages.value.push(
      {
        id: userTmpId,
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

    const done = await sendMessageStream(
      s.id,
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
      attachments,
      props.useDraft
    )

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
    await scrollBottom()
  } catch (e) {
    const last = messages.value[messages.value.length - 1]
    if (last?.role === 'assistant' && last.streaming) {
      last.streaming = false
      last.content = last.content || e?.message || '对话失败'
    }
    message.error(e?.message || '对话失败')
  } finally {
    sending.value = false
  }
}

watch(
  () => props.agentId,
  () => {
    resetChat()
  }
)
</script>

<style scoped>
.debug-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  overflow: hidden;
}
.debug-head {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
}
.debug-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}
.debug-sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.debug-msgs {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.debug-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 24px 16px;
  gap: 4px;
}
.debug-empty.soft {
  flex: 1;
  min-height: 120px;
}
.empty-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}
.empty-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.5;
}
.msg-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 100%;
}
.role-user {
  align-items: flex-end;
}
.msg-head {
  display: flex;
  align-items: center;
  gap: 6px;
}
.role-user .msg-head {
  flex-direction: row-reverse;
}
.msg-avatar {
  width: 24px;
  height: 24px;
  border-radius: 8px;
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
.msg-avatar.has-img {
  background: var(--color-body);
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.msg-nick {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text);
}
.msg-body {
  max-width: 100%;
}
.role-user .msg-body {
  display: flex;
  justify-content: flex-end;
}
.msg-bubble {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
  background: color-mix(in srgb, var(--color-body) 70%, var(--color-surface));
  color: var(--color-text);
}
.role-user .msg-bubble {
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-surface));
}
.msg-files {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}
.msg-image {
  display: block;
  max-width: min(240px, 100%);
  border-radius: 10px;
  overflow: hidden;
  line-height: 0;
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
}
.msg-image img {
  display: block;
  width: 100%;
  max-height: 280px;
  object-fit: contain;
  background: color-mix(in srgb, var(--color-body) 60%, transparent);
}
.msg-file {
  font-size: 12px;
  color: var(--color-primary);
  text-decoration: none;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-file:hover {
  text-decoration: underline;
}
.msg-md-wrap {
  white-space: normal;
  position: relative;
}
.msg-md {
  background: transparent !important;
}
.msg-md :deep(.md-editor-preview-wrapper) {
  padding: 0;
}
.msg-md :deep(.md-editor-preview) {
  font-size: 13px;
  line-height: 1.55;
}
.msg-loading {
  display: block;
  width: fit-content;
  max-width: 100%;
  margin-top: 6px;
  font-size: 12px;
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
@keyframes caret-blink {
  50% {
    opacity: 0;
  }
}
.debug-composer {
  flex-shrink: 0;
  margin: 0 10px 10px;
  padding: 8px 10px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-body);
}
.debug-composer.disabled {
  opacity: 0.7;
}
.debug-composer :deep(.n-input) {
  background: transparent !important;
}
.debug-composer :deep(.n-input__border),
.debug-composer :deep(.n-input__state-border) {
  display: none !important;
}
.debug-composer :deep(textarea) {
  font-size: 13px;
  padding: 0 !important;
}
.pending-files {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  margin-bottom: 6px;
}
.pending-file {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 2px 6px 2px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-surface) 70%, transparent);
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
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 8px;
  background: var(--color-body);
}
.pending-name {
  max-width: 140px;
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
.composer-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}
.composer-bar-right {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  margin-left: auto;
}
.composer-bar-right :deep(.n-upload) {
  display: inline-flex;
  width: auto;
}
.icon-btn,
.send-btn {
  box-sizing: border-box;
  width: 32px;
  height: 32px;
  min-width: 32px;
  border-radius: 999px;
  border: none;
  padding: 0;
  display: inline-grid;
  place-items: center;
  cursor: pointer;
}
.icon-btn {
  background: transparent;
  color: var(--color-text-muted);
}
.icon-btn:hover:not(:disabled) {
  color: var(--color-text);
  background: color-mix(in srgb, var(--color-surface) 80%, transparent);
}
.icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.send-btn {
  background: var(--color-primary);
  color: #fff;
}
.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.send-btn :deep(.n-spin-body) {
  --n-color: #fff;
}
</style>
