<template>
  <div v-if="steps.length" class="trace-timeline">
    <template v-for="(step, idx) in steps" :key="step.id || `${step.type}-${idx}`">
      <ThinkingTrace
        v-if="step.type === 'thinking' && step.content"
        :content="step.content"
        :running="!!step.running"
        :preview-id="`thinking-${previewPrefix}-${step.id || idx}`"
      />
      <details
        v-else-if="step.type === 'tool' && step.tool"
        class="tool-trace"
        :class="{ running: step.tool.phase === 'start', error: !!step.tool.error }"
        :open="toolOpen[toolKey(step, idx)]"
        @toggle="onToolToggle($event, step, idx)"
      >
        <summary class="tool-summary">
          <span class="tool-status">{{ toolStatus(step.tool) }}</span>
          <span class="tool-name">{{ step.tool.toolLabel || step.tool.toolName || '工具' }}</span>
          <span v-if="argSummary(step.tool)" class="tool-arg">{{ argSummary(step.tool) }}</span>
          <span
            v-if="step.tool.durationMs != null && step.tool.phase === 'end'"
            class="tool-ms"
          >{{ step.tool.durationMs }}ms</span>
        </summary>
        <div class="tool-body">
          <div v-if="step.tool.error" class="tool-error">{{ step.tool.error }}</div>
          <div v-if="step.tool.links?.length" class="tool-links">
            <div class="tool-section-title">检索到的网页</div>
            <a
              v-for="(url, i) in step.tool.links"
              :key="`${toolKey(step, idx)}-l-${i}`"
              class="tool-link"
              :href="url"
              target="_blank"
              rel="noopener"
              @click.stop
            >
              {{ url }}
            </a>
          </div>
          <div v-if="hasArgs(step.tool)" class="tool-section">
            <div class="tool-section-title">参数</div>
            <pre class="tool-pre">{{ formatJson(step.tool.args) }}</pre>
          </div>
          <div v-if="step.tool.result" class="tool-section">
            <div class="tool-section-title">结果</div>
            <pre class="tool-pre">{{ step.tool.result }}</pre>
          </div>
        </div>
      </details>
    </template>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import ThinkingTrace from '@/modules/agent/components/ThinkingTrace.vue'

const props = defineProps({
  /** @type {{ id?: string, type: 'thinking'|'tool', content?: string, running?: boolean, tool?: object }[]} */
  steps: { type: Array, default: () => [] },
  previewPrefix: { type: [String, Number], default: 'msg' }
})

const toolOpen = reactive({})
const phaseMap = reactive({})

function toolKey(step, idx) {
  return step?.tool?.callId || step?.id || `tool-${idx}`
}

function toolStatus(t) {
  if (t?.error) return '失败'
  if (t?.phase === 'start') return '执行中'
  return '已完成'
}

function hasArgs(t) {
  return t?.args && typeof t.args === 'object' && Object.keys(t.args).length > 0
}

function argSummary(t) {
  const args = t?.args || {}
  if (t?.toolName === 'knowledge_retrieve' || t?.toolLabel === '知识库检索') {
    if (args.hitCount != null) return `命中 ${args.hitCount} 条`
    if (args.query) return String(args.query)
  }
  if (args.query) return String(args.query)
  if (args.url) return String(args.url)
  if (args.name) return String(args.name)
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

function onToolToggle(e, step, idx) {
  toolOpen[toolKey(step, idx)] = !!e.target?.open
}

watch(
  () => props.steps,
  (list) => {
    const steps = list || []
    for (let i = 0; i < steps.length; i++) {
      const step = steps[i]
      if (step?.type !== 'tool' || !step.tool) continue
      const key = toolKey(step, i)
      const prev = phaseMap[key]
      const phase = step.tool.phase
      if (phase === 'start') {
        toolOpen[key] = true
      } else if (!(key in toolOpen)) {
        toolOpen[key] = false
      } else if (prev === 'start' && phase === 'end' && !step.tool.error) {
        toolOpen[key] = false
      }
      phaseMap[key] = phase
    }
  },
  { deep: true, immediate: true }
)
</script>

<style scoped>
.trace-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-bottom: 4px;
}
.tool-trace {
  margin-bottom: 8px;
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  border-radius: 10px;
  background: color-mix(in srgb, var(--color-body) 40%, var(--color-surface));
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
  padding: 8px 10px;
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
