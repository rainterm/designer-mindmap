<template>
  <div class="page-container mm-edit" :class="{ 'is-standalone': standalone }">
    <n-card :bordered="false" class="edit-topbar">
      <div class="topbar-inner">
        <div class="topbar-left">
          <n-input
            v-model:value="form.name"
            class="name-input"
            placeholder="导图名称"
            maxlength="100"
            @update:value="markDirty"
          />
          <n-select
            v-model:value="form.categoryId"
            class="category-select"
            :options="categoryOptions"
            placeholder="分类"
            clearable
            @update:value="onCategoryChange"
          />
          <span class="save-tip">{{ saveLabel }}</span>
        </div>
        <n-space align="center">
          <n-button
            :type="aiPanelVisible ? 'primary' : 'default'"
            secondary
            :loading="aiOpening"
            :disabled="!form.id || aiOpening"
            @click="handleAiExpand"
          >
            {{ aiPanelVisible ? '关闭 AI' : 'AI 扩展' }}
          </n-button>
          <n-button quaternary @click="goBack">返回</n-button>
          <n-dropdown
            trigger="click"
            :options="moreOptions"
            :disabled="!canvasReady"
            @select="onMoreSelect"
          >
            <n-button :disabled="!canvasReady" :loading="importing || exporting">更多</n-button>
          </n-dropdown>
          <n-button
            type="primary"
            :loading="saving"
            :disabled="(!dirty && !saving) || !canEdit"
            @click="handleSave"
          >
            保存
          </n-button>
        </n-space>
        <input
          ref="importFileRef"
          class="hidden-file"
          type="file"
          accept=".json,.smm,.md,.markdown,.xmind,.txt"
          @change="onImportFileChange"
        />
      </div>
    </n-card>
    <div class="mm-shell">
      <n-spin :show="loading" class="mm-spin">
        <div class="mm-workspace">
          <div class="mm-canvas-wrap">
            <MindMapCanvas
              v-if="form.id && !loading"
              :key="canvasKey"
              ref="canvasRef"
              :content="initialContent"
              :layout="form.layout"
              :theme="form.theme"
              @ready="onCanvasReady"
              @change="onCanvasChange"
              @node-active="onNodeActive"
            />
            <div v-if="showTip" class="canvas-tip">
              <span>选中节点后按 <b>Tab</b> 加分支、<b>Enter</b> 加同级，双击可改文字</span>
              <button type="button" class="tip-close" @click="dismissTip">知道了</button>
            </div>
          </div>
          <aside class="editor-side" :class="{ 'is-ai': aiPanelVisible }">
            <EasyPanel
              v-if="!aiPanelVisible"
              :has-selection="hasNodeSelection"
              :model="globalSettings"
              :node-style="nodeStyle"
              :note="nodeNote"
              @global-change="onGlobalSettingChange"
              @node-change="onNodeStyleChange"
              @note-change="onNodeNoteChange"
              @node-reset="onNodeStyleReset"
            />
            <div v-else class="ai-side-panel">
              <div class="ai-side-head">
                <span class="ai-side-title">AI 扩展</span>
              </div>
              <div class="ai-side-body">
                <iframe
                  v-if="aiChatUrl"
                  :src="aiChatUrl"
                  class="ai-chat-iframe"
                  title="导图助手"
                />
              </div>
            </div>
          </aside>
        </div>
      </n-spin>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import MindMapCanvas from '@/modules/mindmap/components/MindMapCanvas.vue'
import EasyPanel from '@/modules/mindmap/components/EasyPanel.vue'
import { getProject, listCategories, updateProject } from '@/modules/mindmap/api'
import { toSelectOptions } from '@/modules/mindmap/utils/categoryTree'
import { openMindmapAgentSession } from '@/modules/mindmap/utils/openMindmapAgent'
import { MM_PROJECT_SAVED_EVENT } from '@/modules/agent/utils/mindmapToolLink'
import { useUserStore } from '@/store/user'

const TIP_KEY = 'admin-mindmap:edit-tip-dismissed'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const standalone = computed(() => !!route.meta?.standalone)
const canEdit = computed(() => userStore.hasPerm('mindmap:project:edit'))

const canvasRef = ref(null)
const canvasReady = ref(false)
const canvasKey = ref(0)
const loading = ref(true)
const saving = ref(false)
const importing = ref(false)
const exporting = ref(false)
const aiOpening = ref(false)
const aiPanelVisible = ref(false)
const aiChatUrl = ref('')
const dirty = ref(false)
const importFileRef = ref(null)
const showTip = ref(!localStorage.getItem(TIP_KEY))

const moreOptions = [
  { label: '全部展开', key: 'expand' },
  { label: '全部折叠', key: 'collapse' },
  { type: 'divider', key: 'd1' },
  { label: '导入文件', key: 'import' },
  {
    label: '导出',
    key: 'export',
    children: [
      { label: 'PNG 图片', key: 'png' },
      { label: 'SVG 矢量图', key: 'svg' },
      { label: 'JSON 数据', key: 'json' },
      { label: 'Markdown', key: 'md' },
      { label: 'XMind', key: 'xmind' }
    ]
  }
]

const lastSavedAt = ref(null)
const initialContent = ref('')
const hasNodeSelection = ref(false)
const nodeNote = ref('')
const nodeStyle = reactive({
  shape: 'rectangle',
  fillColor: '#ffffff',
  color: '#333333',
  fontSize: 14,
  fontWeight: 'normal',
  paddingX: 15,
  paddingY: 5,
  borderColor: '#549688',
  borderWidth: 1,
  borderRadius: 5,
  borderDasharray: 'none'
})
const globalSettings = reactive({
  layout: 'logicalStructure',
  theme: 'default',
  backgroundColor: '#fafafa',
  lineColor: '#549688',
  lineWidth: 1,
  lineStyle: 'straight',
  lineDasharray: 'none',
  rootFillColor: '#549688',
  rootFontSize: 16,
  rootPaddingX: 15,
  rootPaddingY: 5,
  secondFillColor: '#ffffff',
  secondBorderColor: '#549688',
  secondFontSize: 16,
  secondPaddingX: 15,
  secondPaddingY: 5,
  secondMarginX: 100,
  secondMarginY: 40,
  nodeFillColor: 'transparent',
  nodeColor: '#6a6d6c',
  nodeBorderColor: 'transparent',
  nodeBorderWidth: 0,
  nodeFontSize: 14,
  nodePaddingX: 15,
  nodePaddingY: 5,
  nodeMarginX: 50,
  nodeMarginY: 0
})

const form = reactive({
  id: null,
  categoryId: null,
  name: '未命名导图',
  layout: 'logicalStructure',
  theme: 'default',
  content: '',
  remark: ''
})
const categoryOptions = ref([])

const saveLabel = computed(() => {
  if (saving.value) return '保存中…'
  if (dirty.value) return '未保存'
  if (lastSavedAt.value) return `已保存 ${lastSavedAt.value}`
  return '自动保存'
})

let saveTimer = null

function dismissTip() {
  showTip.value = false
  localStorage.setItem(TIP_KEY, '1')
}

function goBack() {
  router.push('/mindmap/project')
}

async function handleAiExpand() {
  if (!form.id || aiOpening.value) return
  if (aiPanelVisible.value) {
    closeAiPanel()
    return
  }
  aiOpening.value = true
  try {
    const { sessionId } = await openMindmapAgentSession(form.id)
    const { href } = router.resolve({
      path: '/agent/session/chat',
      query: { id: sessionId, embed: '1', projectId: String(form.id) }
    })
    aiChatUrl.value = href
    aiPanelVisible.value = true
    nextTick(() => canvasRef.value?.resize?.())
  } catch (e) {
    message.error(e?.message || '打开 AI 对话失败')
  } finally {
    aiOpening.value = false
  }
}

async function refreshProjectFromServer() {
  if (!form.id) return
  try {
    const res = await getProject(form.id)
    const data = res.data || {}
    const nextContent = data.content || ''
    const contentChanged = nextContent !== form.content
    form.categoryId = data.categoryId || null
    form.name = data.name || form.name
    form.layout = data.layout || form.layout
    form.theme = data.theme || form.theme
    form.remark = data.remark || ''
    if (contentChanged) {
      form.content = nextContent
      initialContent.value = nextContent
      canvasReady.value = false
      canvasKey.value += 1
      dirty.value = false
    }
  } catch {
    /* 刷新失败不打断编辑 */
  }
}

async function closeAiPanel() {
  if (!aiPanelVisible.value) return
  aiPanelVisible.value = false
  aiChatUrl.value = ''
  await refreshProjectFromServer()
  nextTick(() => canvasRef.value?.resize?.())
}

function onMindmapSavedMessage(ev) {
  if (ev.origin !== window.location.origin) return
  const data = ev.data
  if (!data || data.type !== MM_PROJECT_SAVED_EVENT) return
  if (!form.id || String(data.projectId) !== String(form.id)) return
  refreshProjectFromServer()
}

function markDirty() {
  dirty.value = true
  scheduleSave()
}

function onCategoryChange() {
  markDirty()
}

function syncGlobalSettings() {
  const gs = canvasRef.value?.getGlobalSettings?.()
  if (!gs) return
  Object.assign(globalSettings, gs)
  if (gs.layout) form.layout = gs.layout
  if (gs.theme) form.theme = gs.theme
}

function onCanvasReady() {
  canvasReady.value = true
  nextTick(() => {
    canvasRef.value?.resize?.()
    syncGlobalSettings()
  })
}

function onCanvasChange(full) {
  if (!full) return
  form.content = JSON.stringify(full)
  if (full.layout) form.layout = full.layout
  if (full.theme?.template) form.theme = full.theme.template
  syncGlobalSettings()
  markDirty()
}

function onNodeActive(payload) {
  hasNodeSelection.value = (payload?.count || 0) > 0
  nodeNote.value = payload?.note || ''
  if (payload?.style) {
    Object.assign(nodeStyle, {
      shape: payload.style.shape || 'rectangle',
      fillColor: payload.style.fillColor || '#ffffff',
      color: payload.style.color || '#333333',
      fontSize: payload.style.fontSize || 14,
      fontWeight: payload.style.fontWeight || 'normal',
      paddingX: payload.style.paddingX ?? 15,
      paddingY: payload.style.paddingY ?? 5,
      borderColor: payload.style.borderColor || '#549688',
      borderWidth: payload.style.borderWidth ?? 1,
      borderRadius: payload.style.borderRadius ?? 5,
      borderDasharray: payload.style.borderDasharray || 'none'
    })
  }
}

function onNodeStyleChange(style) {
  Object.assign(nodeStyle, style)
  canvasRef.value?.setActiveNodeStyles?.(style)
}

function onNodeNoteChange(note) {
  nodeNote.value = note || ''
  canvasRef.value?.setActiveNodeNote?.(nodeNote.value)
}

function onNodeStyleReset() {
  canvasRef.value?.resetActiveNodeStyles?.()
}

function onGlobalSettingChange({ key, value }) {
  globalSettings[key] = value
  if (key === 'layout') form.layout = value
  if (key === 'theme') form.theme = value
  canvasRef.value?.applyGlobalSetting?.(key, value)
  if (key === 'theme') {
    nextTick(syncGlobalSettings)
  }
}

function scheduleSave() {
  if (!canEdit.value) return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    if (dirty.value) handleSave(true)
  }, 1200)
}

async function handleSave(silent = false) {
  if (!form.id || saving.value || !canEdit.value) return
  const json = canvasRef.value?.getContentJson?.()
  if (json) form.content = json
  const full = canvasRef.value?.getFullData?.()
  if (full?.layout) form.layout = full.layout
  if (full?.theme?.template) form.theme = full.theme.template
  saving.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name || '未命名导图',
      layout: form.layout,
      theme: form.theme,
      content: form.content,
      remark: form.remark
    }
    if (form.categoryId) {
      payload.categoryId = form.categoryId
    } else {
      payload.clearCategory = true
    }
    await updateProject(payload)
    dirty.value = false
    lastSavedAt.value = new Date().toLocaleTimeString()
    if (!silent) message.success('已保存')
  } catch (e) {
    if (!silent) message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function onMoreSelect(key) {
  if (key === 'expand') {
    canvasRef.value?.expandAll?.()
    return
  }
  if (key === 'collapse') {
    canvasRef.value?.collapseAll?.()
    return
  }
  if (key === 'import') {
    importFileRef.value?.click()
    return
  }
  if (['png', 'svg', 'json', 'md', 'xmind'].includes(key)) {
    handleExport(key)
  }
}

async function onImportFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  importing.value = true
  try {
    await canvasRef.value?.importFile?.(file)
    message.success('导入成功')
    nextTick(syncGlobalSettings)
  } catch (err) {
    message.error(err?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

async function handleExport(type) {
  if (!type) return
  exporting.value = true
  try {
    await canvasRef.value?.exportFile?.(type, form.name || '思维导图')
  } catch (e) {
    message.error(e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

async function loadCategories() {
  try {
    const res = await listCategories()
    categoryOptions.value = toSelectOptions(res.data?.categories || [])
  } catch {
    categoryOptions.value = []
  }
}

async function loadDetail() {
  const id = route.query.id
  if (!id) {
    message.warning('缺少项目 ID')
    goBack()
    return
  }
  loading.value = true
  try {
    const res = await getProject(id)
    const data = res.data || {}
    form.id = data.id
    form.categoryId = data.categoryId || null
    form.name = data.name || '未命名导图'
    form.layout = data.layout || 'logicalStructure'
    form.theme = data.theme || 'default'
    form.content = data.content || ''
    form.remark = data.remark || ''
    initialContent.value = form.content
    dirty.value = false
  } catch {
    goBack()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  window.addEventListener('message', onMindmapSavedMessage)
  await loadCategories()
  await loadDetail()
})

onBeforeUnmount(() => {
  window.removeEventListener('message', onMindmapSavedMessage)
  if (saveTimer) clearTimeout(saveTimer)
})
</script>

<style scoped>
.page-container.mm-edit {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
  padding: 8px;
  gap: 8px;
}
.page-container.mm-edit.is-standalone {
  min-height: 100vh;
  height: 100vh;
  padding: 12px;
  background: var(--color-body, #f2f5f3);
}
.edit-topbar {
  flex: none;
}
.topbar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.hidden-file {
  display: none;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.name-input {
  width: 360px;
  max-width: min(360px, 40vw);
  font-weight: 600;
}
.category-select {
  width: 160px;
}
.save-tip {
  flex: none;
  font-size: 12px;
  color: var(--color-text-muted, #87918b);
  white-space: nowrap;
}
.mm-shell {
  flex: 1;
  min-height: 0;
  border-radius: var(--radius, 6px);
  overflow: hidden;
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e2e8e4);
}
.mm-spin {
  height: 100%;
}
.mm-spin :deep(.n-spin-content) {
  height: 100%;
}
.mm-workspace {
  display: flex;
  height: 100%;
  min-height: 0;
  background: var(--color-surface, #fff);
}
.mm-canvas-wrap {
  position: relative;
  z-index: 0;
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: var(--color-body, #f2f5f3);
}
.canvas-tip {
  position: absolute;
  left: 50%;
  bottom: 16px;
  z-index: 2;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: calc(100% - 32px);
  padding: 10px 12px 10px 14px;
  border-radius: 999px;
  background: rgba(22, 26, 23, 0.86);
  color: #fff;
  font-size: 12px;
  line-height: 1.4;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
}
.canvas-tip b {
  font-weight: 700;
}
.tip-close {
  flex: none;
  border: 0;
  border-radius: 999px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}
.tip-close:hover {
  background: rgba(255, 255, 255, 0.24);
}
.editor-side {
  position: relative;
  z-index: 3;
  width: 240px;
  flex: none;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-left: 1px solid var(--color-border, #e2e8e4);
  background: var(--color-surface, #fff);
  box-shadow: -6px 0 16px color-mix(in srgb, var(--color-text, #161a17) 6%, transparent);
  transition: width 0.18s ease;
}
.editor-side.is-ai {
  width: min(420px, 42vw);
}
.ai-side-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--color-surface, #fff);
}
.ai-side-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  height: 44px;
  padding: 0 10px 0 14px;
  border-bottom: 1px solid var(--color-border, #e2e8e4);
}
.ai-side-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text, #161a17);
}
.ai-side-body {
  flex: 1;
  min-height: 0;
}
.ai-chat-iframe {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
  background: var(--color-surface, #fff);
}
</style>
