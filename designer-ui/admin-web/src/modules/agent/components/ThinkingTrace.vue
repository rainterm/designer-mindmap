<template>
  <details
    v-if="content"
    class="thinking-trace"
    :class="{ running }"
    :open="open"
  >
    <summary class="thinking-summary" @click.prevent="toggle">
      <span class="thinking-status">{{ running ? '思考中' : '已完成' }}</span>
      <span class="thinking-title">思考过程</span>
      <span class="thinking-chevron" :class="{ open }" />
    </summary>
    <div ref="bodyRef" class="thinking-body">
      <MdPreview
        :id="previewId"
        :model-value="content || ' '"
        theme="light"
        preview-theme="default"
        class="thinking-md"
      />
    </div>
  </details>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'

const props = defineProps({
  content: { type: String, default: '' },
  /** 仍在流式输出思考时为 true */
  running: { type: Boolean, default: false },
  previewId: { type: String, default: 'thinking-md' }
})

const open = ref(false)
const userTouched = ref(false)
const bodyRef = ref(null)

function toggle() {
  userTouched.value = true
  open.value = !open.value
}

async function scrollToBottom() {
  await nextTick()
  const el = bodyRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

watch(
  () => props.running,
  (isRunning) => {
    if (isRunning) {
      open.value = true
      userTouched.value = false
      scrollToBottom()
    } else if (!userTouched.value) {
      open.value = false
    }
  },
  { immediate: true }
)

watch(
  () => props.content,
  () => {
    if (props.running && props.content && !userTouched.value) {
      open.value = true
    }
    if (open.value && (props.running || !userTouched.value)) {
      scrollToBottom()
    }
  }
)
</script>

<style scoped>
.thinking-trace {
  margin-bottom: 8px;
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  border-radius: 10px;
  background: color-mix(in srgb, var(--color-body) 40%, var(--color-surface));
  overflow: hidden;
}
.thinking-trace.running {
  border-color: color-mix(in srgb, var(--color-primary) 35%, var(--color-border));
}
.thinking-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  cursor: pointer;
  list-style: none;
  font-size: 12px;
  user-select: none;
}
.thinking-summary::-webkit-details-marker {
  display: none;
}
.thinking-status {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-text-muted) 14%, transparent);
  color: var(--color-text-secondary);
  font-size: 11px;
}
.thinking-trace.running .thinking-status {
  background: color-mix(in srgb, var(--color-primary) 14%, transparent);
  color: var(--color-primary);
}
.thinking-title {
  font-weight: 600;
  color: var(--color-text);
}
.thinking-chevron {
  margin-left: auto;
  width: 6px;
  height: 6px;
  border-right: 1.5px solid var(--color-text-muted);
  border-bottom: 1.5px solid var(--color-text-muted);
  transform: rotate(-45deg);
  transition: transform 0.15s ease;
}
.thinking-chevron.open {
  transform: rotate(45deg);
}
.thinking-body {
  max-height: 220px;
  overflow: auto;
  padding: 0 10px 10px;
}
.thinking-md {
  background: transparent !important;
}
.thinking-md :deep(.md-editor-preview-wrapper) {
  padding: 0;
}
.thinking-md :deep(.md-editor-preview) {
  font-size: 12px;
  line-height: 1.55;
  color: var(--color-text-secondary);
}
.thinking-md :deep(.md-editor-preview > :first-child) {
  margin-top: 0;
}
.thinking-md :deep(.md-editor-preview > :last-child) {
  margin-bottom: 0;
}
</style>
