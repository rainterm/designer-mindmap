<template>
  <details
    v-if="tools?.length"
    class="tool-trace-group"
    :class="{ running: hasRunning }"
    :open="groupOpen"
  >
    <summary class="tool-group-summary" @click.prevent="groupOpen = !groupOpen">
      <span class="tool-group-status">{{ groupStatusText }}</span>
      <span class="tool-group-title">工具调用</span>
      <span class="tool-group-count">{{ tools.length }} 次</span>
      <span class="tool-group-chevron" :class="{ open: groupOpen }" />
    </summary>

    <div class="tool-trace-list">
      <details
        v-for="(t, idx) in tools"
        :key="t.callId || `${t.toolName}-${idx}`"
        class="tool-trace"
        :class="{ running: t.phase === 'start', error: !!t.error }"
        :open="openMap[itemKey(t, idx)]"
        @toggle="onItemToggle($event, t, idx)"
      >
        <summary class="tool-summary">
          <span class="tool-status">{{ statusText(t) }}</span>
          <span class="tool-name">{{ t.toolLabel || t.toolName || '工具' }}</span>
          <span v-if="argSummary(t)" class="tool-arg">{{ argSummary(t) }}</span>
          <span v-if="t.durationMs != null && t.phase === 'end'" class="tool-ms">{{ t.durationMs }}ms</span>
        </summary>

        <div class="tool-body">
          <div v-if="t.error" class="tool-error">{{ t.error }}</div>

          <div v-if="t.links?.length" class="tool-links">
            <div class="tool-section-title">检索到的网页</div>
            <a
              v-for="(url, i) in t.links"
              :key="`${t.callId || idx}-l-${i}`"
              class="tool-link"
              :href="url"
              target="_blank"
              rel="noopener"
              @click.stop
            >
              {{ url }}
            </a>
          </div>

          <div v-if="hasArgs(t)" class="tool-section">
            <div class="tool-section-title">参数</div>
            <pre class="tool-pre">{{ formatJson(t.args) }}</pre>
          </div>

          <div v-if="t.result" class="tool-section">
            <div class="tool-section-title">结果</div>
            <pre class="tool-pre">{{ t.result }}</pre>
          </div>
        </div>
      </details>
    </div>
  </details>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'

const props = defineProps({
  tools: { type: Array, default: () => [] }
})

const groupOpen = ref(true)
const openMap = reactive({})
const phaseMap = reactive({})
const userClosedGroup = ref(false)

const hasRunning = computed(() => (props.tools || []).some((t) => t?.phase === 'start'))
const hasError = computed(() => (props.tools || []).some((t) => !!t?.error))

const groupStatusText = computed(() => {
  if (hasRunning.value) return '执行中'
  if (hasError.value) return '有失败'
  return '已完成'
})

function itemKey(t, idx) {
  return t?.callId || `${t?.toolName || 'tool'}-${idx}`
}

function statusText(t) {
  if (t?.error) return '失败'
  if (t?.phase === 'start') return '执行中'
  return '已完成'
}

function hasArgs(t) {
  return t?.args && typeof t.args === 'object' && Object.keys(t.args).length > 0
}

function argSummary(t) {
  const args = t?.args || {}
  if (args.query) return String(args.query)
  if (args.url) return String(args.url)
  if (args.q) return String(args.q)
  const keys = Object.keys(args)
  if (!keys.length) return ''
  const first = args[keys[0]]
  const text = typeof first === 'string' ? first : JSON.stringify(first)
  return text.length > 48 ? `${text.slice(0, 48)}…` : text
}

function formatJson(v) {
  try {
    return JSON.stringify(v, null, 2)
  } catch {
    return String(v)
  }
}

function onItemToggle(e, t, idx) {
  openMap[itemKey(t, idx)] = !!e.target?.open
}

watch(
  () => props.tools,
  (list) => {
    const tools = list || []
    for (let i = 0; i < tools.length; i++) {
      const t = tools[i]
      const key = itemKey(t, i)
      const prev = phaseMap[key]
      const phase = t?.phase
      if (phase === 'start') {
        openMap[key] = true
      } else if (!(key in openMap)) {
        openMap[key] = false
      } else if (prev === 'start' && phase === 'end' && !t?.error) {
        openMap[key] = false
      }
      phaseMap[key] = phase
    }
    if (hasRunning.value) {
      groupOpen.value = true
      userClosedGroup.value = false
    } else if (!userClosedGroup.value && tools.length) {
      groupOpen.value = false
    }
  },
  { deep: true, immediate: true }
)

watch(groupOpen, (open) => {
  if (!open && !hasRunning.value) userClosedGroup.value = true
  if (open) userClosedGroup.value = false
})
</script>

<style scoped>
.tool-trace-group {
  margin-bottom: 8px;
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  border-radius: 10px;
  background: color-mix(in srgb, var(--color-body) 40%, var(--color-surface));
  overflow: hidden;
}
.tool-trace-group.running {
  border-color: color-mix(in srgb, var(--color-primary) 35%, var(--color-border));
}
.tool-group-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  cursor: pointer;
  list-style: none;
  font-size: 12px;
  user-select: none;
}
.tool-group-summary::-webkit-details-marker {
  display: none;
}
.tool-group-status {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-text-muted) 14%, transparent);
  color: var(--color-text-secondary);
  font-size: 11px;
}
.tool-trace-group.running .tool-group-status {
  background: color-mix(in srgb, var(--color-primary) 14%, transparent);
  color: var(--color-primary);
}
.tool-group-title {
  font-weight: 600;
  color: var(--color-text);
}
.tool-group-count {
  color: var(--color-text-muted);
}
.tool-group-chevron {
  margin-left: auto;
  width: 6px;
  height: 6px;
  border-right: 1.5px solid var(--color-text-muted);
  border-bottom: 1.5px solid var(--color-text-muted);
  transform: rotate(-45deg);
  transition: transform 0.15s ease;
}
.tool-group-chevron.open {
  transform: rotate(45deg);
}
.tool-trace-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 8px 8px;
}
.tool-trace {
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-body) 55%, var(--color-surface));
  overflow: hidden;
}
.tool-trace.running {
  border-color: color-mix(in srgb, var(--color-primary) 35%, var(--color-border));
}
.tool-trace.error {
  border-color: color-mix(in srgb, #d03050 40%, var(--color-border));
}
.tool-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  cursor: pointer;
  list-style: none;
  font-size: 12px;
  user-select: none;
}
.tool-summary::-webkit-details-marker {
  display: none;
}
.tool-status {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-text-muted) 14%, transparent);
  color: var(--color-text-secondary);
  font-size: 11px;
}
.tool-trace.running .tool-status {
  background: color-mix(in srgb, var(--color-primary) 14%, transparent);
  color: var(--color-primary);
}
.tool-trace.error .tool-status {
  background: color-mix(in srgb, #d03050 12%, transparent);
  color: #d03050;
}
.tool-name {
  font-weight: 600;
  color: var(--color-text);
  flex-shrink: 0;
}
.tool-arg {
  min-width: 0;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tool-ms {
  margin-left: auto;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}
.tool-body {
  padding: 0 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.tool-section-title {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-bottom: 4px;
}
.tool-links {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tool-link {
  font-size: 12px;
  color: var(--color-primary);
  text-decoration: none;
  word-break: break-all;
  line-height: 1.4;
}
.tool-link:hover {
  text-decoration: underline;
}
.tool-error {
  font-size: 12px;
  color: #d03050;
}
.tool-pre {
  margin: 0;
  padding: 8px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-body) 80%, #000 2%);
  font-size: 11px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 180px;
  overflow: auto;
  color: var(--color-text-secondary);
}
</style>
