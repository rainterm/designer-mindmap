<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="内容">
          <n-input v-model:value="query.title" placeholder="提问 / 回复关键词" clearable />
        </n-form-item>
        <n-form-item label="智能体">
          <n-select
            v-model:value="query.agentId"
            :options="agentOptions"
            placeholder="全部"
            clearable
          />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button
            v-perm="'agent:session:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除
          </n-button>
        </n-space>
      </template>
    </PageTable>

    <Dialog
      v-model:show="ctxVisible"
      :title="ctxTitle"
      size="xl"
      content-class="app-dialog--chat"
      :draggable="false"
      :show-footer="false"
      @after-leave="onCtxLeave"
    >
      <n-spin :show="ctxLoading" class="ctx-spin">
        <div class="ctx-chat">
          <div v-if="!ctxLoading && !ctxMessages.length" class="msg-empty">暂无消息</div>
          <div v-else class="msg-list">
            <div
              v-for="m in ctxMessages"
              :key="m.id"
              class="msg-row"
              :class="`role-${m.role}`"
            >
              <div class="msg-head">
                <div
                  class="msg-avatar"
                  :class="[m.role, { 'has-img': m.role === 'assistant' && !!ctxSession?.agentAvatar }]"
                >
                  <img
                    v-if="m.role === 'assistant' && ctxSession?.agentAvatar"
                    :src="ctxSession.agentAvatar"
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
                    :preview-prefix="m.id"
                  />
                  <MdPreview
                    v-if="m.content"
                    :id="`session-ctx-md-${m.id}`"
                    :model-value="m.content"
                    theme="light"
                    preview-theme="default"
                    class="msg-md"
                  />
                  <KnowledgeSourceFooter :sources="knowledgeSourcesOf(m)" />
                </div>
                <div v-else class="msg-bubble">
                  <div v-if="m.attachments?.length" class="msg-files">
                    <a
                      v-for="(f, i) in m.attachments"
                      :key="`${m.id}-f-${i}`"
                      class="msg-file"
                      :class="{ 'is-image': isImageAttachment(f) }"
                      :href="f.url"
                      target="_blank"
                      rel="noopener"
                    >
                      <img v-if="isImageAttachment(f)" :src="f.url" :alt="f.name || '图片'" />
                      <template v-else>{{ f.name || '附件' }}</template>
                    </a>
                  </div>
                  <div v-if="m.content">{{ m.content }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </n-spin>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { EyeOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  deleteSessions,
  getSession,
  listAgents,
  listMessages,
  pageSessions
} from '@/modules/agent/api'
import AgentTraceTimeline from '@/modules/agent/components/AgentTraceTimeline.vue'
import KnowledgeSourceFooter from '@/modules/agent/components/KnowledgeSourceFooter.vue'
import { buildTraceStepsFromMessage, extractKnowledgeSources } from '@/modules/agent/utils/traceSteps'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const agentOptions = ref([])
const ctxVisible = ref(false)
const ctxLoading = ref(false)
const ctxSession = ref(null)
const ctxMessages = ref([])
const ctxTitle = computed(() => {
  const s = ctxSession.value
  if (!s) return '查看上下文'
  const title = s.title || '会话'
  const agent = s.agentName ? ` · ${s.agentName}` : ''
  return `${title}${agent}`
})

const pager = usePagination((q) => pageSessions(q), {
  defaults: { title: '', agentId: null }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteSessions, {
  confirm: (n) => `确定删除选中的 ${n} 条聊天记录吗？`,
  positiveText: '删除'
})

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '智能体', key: 'agentName', width: 100 },
  {
    title: '用户提问',
    key: 'question',
    width: 180,
    ellipsis: { tooltip: true }
  },
  {
    title: '助手回复',
    key: 'answer',
    width: 220,
    ellipsis: { tooltip: true },
    render: (row) => row.answer || '—'
  },
  { title: '时间', key: 'createdAt', width: 120 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 80,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('agent:session:chat') &&
            hActionBtn({
              tip: '查看上下文',
              type: 'primary',
              icon: EyeOutline,
              disabled: busy(row.id),
              onClick: () => openContext(row)
            }),
          userStore.hasPerm('agent:session:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除这条聊天记录吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

function initials(name) {
  const s = String(name || '').trim()
  if (!s) return 'AI'
  return s.slice(0, 1).toUpperCase()
}

function roleGlyph(role) {
  if (role === 'user') return '我'
  if (role === 'assistant') return initials(ctxSession.value?.agentName)
  if (role === 'tool') return '工'
  return '系'
}

function roleName(role) {
  if (role === 'user') return '用户'
  if (role === 'assistant') return ctxSession.value?.agentName || '助手'
  if (role === 'tool') return '工具'
  return '系统'
}

function traceStepsOf(m) {
  if (m?.traceSteps?.length) return m.traceSteps
  return buildTraceStepsFromMessage(m)
}

function knowledgeSourcesOf(m) {
  return extractKnowledgeSources(m)
}

function isImageAttachment(f) {
  if (!f) return false
  if (String(f.mimeType || f.type || '').startsWith('image/')) return true
  const name = String(f.name || f.url || '')
  return /\.(png|jpe?g|gif|webp|bmp|svg)(\?|$)/i.test(name)
}

async function openContext(row) {
  const sessionId = row.sessionId
  if (!sessionId) {
    message.warning('缺少会话 ID')
    return
  }
  ctxVisible.value = true
  ctxLoading.value = true
  ctxSession.value = null
  ctxMessages.value = []
  try {
    const [sRes, mRes] = await Promise.all([getSession(sessionId), listMessages(sessionId)])
    if (!sRes.data) {
      message.error('会话不存在')
      ctxVisible.value = false
      return
    }
    ctxSession.value = sRes.data
    ctxMessages.value = (mRes.data || []).filter((m) => m.role !== 'system')
  } catch (e) {
    message.error(e?.message || '加载上下文失败')
    ctxVisible.value = false
  } finally {
    ctxLoading.value = false
  }
}

function onCtxLeave() {
  ctxSession.value = null
  ctxMessages.value = []
}

async function handleDelete(row) {
  await deleteSessions([row.id])
  message.success('删除成功')
  loadData()
}

onMounted(async () => {
  const res = await listAgents()
  agentOptions.value = (res.data || []).map((a) => ({ label: a.name, value: a.id }))
})
</script>

<style scoped>
.ctx-spin {
  flex: 1;
  min-height: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.ctx-spin :deep(.n-spin-container),
.ctx-spin :deep(.n-spin-content) {
  flex: 1;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.ctx-chat {
  flex: 1;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  overflow: hidden;
}
.msg-list {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.msg-empty {
  margin: auto;
  text-align: center;
  padding: 48px 24px;
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
.msg-row.role-system,
.msg-row.role-tool {
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
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
.msg-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}
.msg-file {
  font-size: 12px;
  color: var(--color-primary);
  text-decoration: none;
}
.msg-file.is-image {
  display: block;
  max-width: 220px;
  border-radius: 8px;
  overflow: hidden;
}
.msg-file.is-image img {
  display: block;
  width: 100%;
  height: auto;
}
.msg-file:hover {
  text-decoration: underline;
}
.msg-file.is-image:hover {
  text-decoration: none;
  opacity: 0.92;
}
</style>
