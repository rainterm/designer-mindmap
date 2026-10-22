<template>
  <div v-if="files.length" class="html-files">
    <div v-for="(f, i) in files" :key="`${f.name}-${i}`" class="html-file">
      <div class="html-file-meta">
        <span class="html-file-icon">HTML</span>
        <div class="html-file-text">
          <div class="html-file-name">{{ f.name }}</div>
          <div class="html-file-sub">{{ sizeLabel(f.html) }}</div>
        </div>
      </div>
      <div class="html-file-actions">
        <button type="button" class="html-btn" @click="onPreview(f)">预览</button>
        <button type="button" class="html-btn primary" @click="onDownload(f)">下载</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useMessage } from 'naive-ui'
import {
  downloadHtmlFile,
  extractHtmlArtifacts,
  previewHtmlFile
} from '@/modules/agent/utils/htmlArtifact'

const props = defineProps({
  content: { type: String, default: '' }
})

const message = useMessage()
const files = computed(() => extractHtmlArtifacts(props.content))

function sizeLabel(html) {
  const n = new Blob([html || '']).size
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(2)} MB`
}

function onDownload(f) {
  downloadHtmlFile(f.name, f.html)
  message.success(`已下载 ${f.name}`)
}

function onPreview(f) {
  try {
    previewHtmlFile(f.html)
  } catch (e) {
    message.warning(e?.message || '预览失败')
  }
}
</script>

<style scoped>
.html-files {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}
.html-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  border-radius: 10px;
  background: color-mix(in srgb, var(--color-body) 45%, var(--color-surface));
}
.html-file-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.html-file-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #fff;
  background: linear-gradient(145deg, #e85d04, #f48c06);
}
.html-file-text {
  min-width: 0;
}
.html-file-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.html-file-sub {
  margin-top: 2px;
  font-size: 11px;
  color: var(--color-text-muted);
}
.html-file-actions {
  display: flex;
  flex-shrink: 0;
  gap: 6px;
}
.html-btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 7px;
  border: 1px solid color-mix(in srgb, var(--color-border) 90%, transparent);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 12px;
  cursor: pointer;
}
.html-btn:hover {
  border-color: color-mix(in srgb, var(--color-primary) 40%, var(--color-border));
}
.html-btn.primary {
  border-color: color-mix(in srgb, var(--color-primary) 45%, transparent);
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
  color: var(--color-primary);
  font-weight: 600;
}
</style>
