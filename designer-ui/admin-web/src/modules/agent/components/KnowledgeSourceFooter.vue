<template>
  <div v-if="sources.length" class="kb-sources">
    <div class="kb-sources-label">文档来源</div>
    <div class="kb-sources-list">
      <button
        v-for="(s, i) in sources"
        :key="`${s.docId}-${i}`"
        type="button"
        class="kb-source-chip"
        :title="sourceTitle(s)"
        @click="openSource(s)"
      >
        <span class="kb-source-idx">{{ i + 1 }}</span>
        <span class="kb-source-name">{{ s.title || `文档 #${s.docId}` }}</span>
        <span v-if="s.kbName" class="kb-source-kb">{{ s.kbName }}</span>
      </button>
    </div>

    <n-drawer v-model:show="previewVisible" :width="720" placement="right">
      <n-drawer-content :title="previewDoc?.title || '文档来源'" closable>
        <n-spin :show="previewLoading">
          <div v-if="previewDoc" class="preview-wrap">
            <div class="preview-meta">
              <n-tag v-if="previewDoc.sourceType" :bordered="false">{{ sourceLabel(previewDoc.sourceType) }}</n-tag>
              <span v-if="previewDoc.fileName">{{ previewDoc.fileName }}</span>
            </div>
            <div v-if="hasSourceFile" class="preview-file">
              <img
                v-if="fileKind === 'image'"
                class="preview-image"
                :src="previewDoc.url"
                :alt="previewDoc.fileName || previewDoc.title || '源文件'"
              />
              <iframe
                v-else-if="fileKind === 'pdf'"
                class="preview-frame"
                :src="fileObjectUrl || previewDoc.url"
                title="源文件"
              />
              <a v-else class="preview-file-link" :href="previewDoc.url" target="_blank" rel="noopener">
                {{ previewDoc.fileName || '打开源文件' }}
              </a>
            </div>
            <div v-else-if="previewDoc.url" class="preview-file">
              <a class="preview-file-link" :href="previewDoc.url" target="_blank" rel="noopener">
                {{ previewDoc.url }}
              </a>
            </div>
            <div v-if="showText" class="preview-body">
              <MdPreview
                v-if="previewDoc.content"
                :id="`kb-source-md-${previewDoc.id}`"
                :model-value="previewDoc.content"
                theme="light"
                preview-theme="default"
                class="kb-source-md"
              />
              <div v-else class="preview-empty">（无正文预览）</div>
            </div>
          </div>
          <n-empty v-else-if="!previewLoading" description="文档不存在或已删除" />
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import request from '@/api/request'
import { getKnowledgeDoc } from '@/modules/agent/api'

const IMAGE_EXT = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'ico', 'avif'])
const SOURCE_LABEL = { upload: '上传', text: '文本', url: 'URL' }

defineProps({
  sources: { type: Array, default: () => [] }
})

const message = useMessage()
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewDoc = ref(null)
const fileObjectUrl = ref('')

const hasSourceFile = computed(() => {
  const doc = previewDoc.value
  if (!doc?.url || doc.sourceType === 'text') return false
  if (doc.sourceType === 'url' && !isStoredFile(doc.url)) return false
  return Boolean(doc.fileName) || doc.sourceType === 'upload' || isStoredFile(doc.url)
})

const fileKind = computed(() => {
  if (!hasSourceFile.value) return 'none'
  const ext = extOf(previewDoc.value)
  if (IMAGE_EXT.has(ext)) return 'image'
  if (ext === 'pdf') return 'pdf'
  return 'file'
})

const showText = computed(() => {
  if (!previewDoc.value) return false
  return fileKind.value !== 'image' && fileKind.value !== 'pdf'
})

function sourceLabel(type) {
  return SOURCE_LABEL[type] || type
}

function sourceTitle(s) {
  const parts = [s.title || `文档 #${s.docId}`]
  if (s.kbName) parts.push(s.kbName)
  if (s.score != null) parts.push(`score ${Number(s.score).toFixed(3)}`)
  return parts.join(' · ')
}

function isStoredFile(url) {
  return typeof url === 'string' && url.includes('/download/')
}

function extOf(doc) {
  const raw = String(doc?.fileType || '')
    .trim()
    .toLowerCase()
    .replace(/^\./, '')
  if (raw && !['bin', 'url', 'text', 'upload'].includes(raw)) return raw
  const name = String(doc?.fileName || doc?.url || '').split('?')[0]
  const i = name.lastIndexOf('.')
  return i >= 0 ? name.slice(i + 1).toLowerCase() : ''
}

function revokeFileObject() {
  if (fileObjectUrl.value) {
    URL.revokeObjectURL(fileObjectUrl.value)
    fileObjectUrl.value = ''
  }
}

/** 私有下载带 sandbox CSP，浏览器内置 PDF 查看器会被拦住，改用 blob 预览。 */
async function loadPdfObject(url) {
  revokeFileObject()
  if (!isStoredFile(url)) return
  try {
    const data = await request.get(url, { responseType: 'blob', silent: true })
    if (!(data instanceof Blob) || data.size === 0 || String(data.type || '').includes('json')) return
    const pdf = data.type === 'application/pdf' ? data : new Blob([data], { type: 'application/pdf' })
    fileObjectUrl.value = URL.createObjectURL(pdf)
  } catch {
    fileObjectUrl.value = ''
  }
}

async function openSource(s) {
  if (!s?.docId) return
  previewVisible.value = true
  previewLoading.value = true
  previewDoc.value = null
  revokeFileObject()
  try {
    const res = await getKnowledgeDoc(s.docId)
    const doc = res.data || null
    previewDoc.value = doc
    if (doc && extOf(doc) === 'pdf' && doc.url) {
      await loadPdfObject(doc.url)
    }
  } catch (e) {
    message.error(e?.message || '加载文档失败')
    previewVisible.value = false
  } finally {
    previewLoading.value = false
  }
}

watch(previewVisible, (show) => {
  if (!show) revokeFileObject()
})

onBeforeUnmount(revokeFileObject)
</script>

<style scoped>
.kb-sources {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--color-border);
}
.kb-sources-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 8px;
}
.kb-sources-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.kb-source-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: 4px 10px 4px 6px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body, #f7f8f7);
  color: var(--color-text);
  font-size: 12px;
  line-height: 1.4;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.kb-source-chip:hover {
  border-color: color-mix(in srgb, var(--color-primary) 40%, var(--color-border));
  background: color-mix(in srgb, var(--color-primary) 6%, var(--color-surface, #fff));
}
.kb-source-idx {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  display: grid;
  place-items: center;
  background: color-mix(in srgb, var(--color-primary) 14%, transparent);
  color: var(--color-primary);
  font-weight: 600;
  font-size: 11px;
}
.kb-source-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}
.kb-source-kb {
  flex-shrink: 0;
  color: var(--color-text-muted);
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.preview-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
}
.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: var(--color-text-muted);
}
.preview-file-link {
  color: var(--color-primary);
  font-size: 13px;
  word-break: break-all;
}
.preview-image {
  display: block;
  max-width: 100%;
  max-height: calc(100vh - 180px);
  object-fit: contain;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body, #f7f8f7);
}
.preview-frame {
  width: 100%;
  height: calc(100vh - 180px);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: #fff;
}
.preview-body {
  margin: 0;
  padding: 12px;
  border-radius: var(--radius);
  background: var(--color-body, #f7f8f7);
  border: 1px solid var(--color-border);
  max-height: calc(100vh - 220px);
  overflow: auto;
}
.preview-empty {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.55;
}
.kb-source-md {
  background: transparent !important;
}
.kb-source-md :deep(.md-editor-preview-wrapper) {
  padding: 0;
}
.kb-source-md :deep(.md-editor-preview) {
  font-size: 13px;
  line-height: 1.55;
  color: var(--color-text);
  word-break: break-word;
}
.kb-source-md :deep(.md-editor-preview > :first-child) {
  margin-top: 0;
}
.kb-source-md :deep(.md-editor-preview > :last-child) {
  margin-bottom: 0;
}
.kb-source-md :deep(pre) {
  margin: 8px 0;
  border-radius: var(--radius);
}
.kb-source-md :deep(code) {
  font-size: 12px;
}
.kb-source-md :deep(p) {
  margin: 0.4em 0;
}
.kb-source-md :deep(ul),
.kb-source-md :deep(ol) {
  margin: 0.4em 0;
  padding-left: 1.4em;
}
.kb-source-md :deep(h1),
.kb-source-md :deep(h2),
.kb-source-md :deep(h3),
.kb-source-md :deep(h4) {
  margin: 0.8em 0 0.4em;
  line-height: 1.35;
}
.kb-source-md :deep(img) {
  max-width: 100%;
}
</style>
