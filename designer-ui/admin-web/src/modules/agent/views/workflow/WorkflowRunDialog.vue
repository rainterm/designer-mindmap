<template>
  <Dialog
    :show="show"
    :title="dialogTitle"
    size="full"
    :fullscreenable="false"
    :draggable="false"
    :show-footer="false"
    content-class="app-dialog--wf-run"
    @update:show="onShowUpdate"
  >
    <div class="wf-run">
      <aside class="wf-run-left">
        <div class="pane-title">输入参数</div>
        <p class="pane-hint">填写开始节点参数后执行；回答由输出节点或开启「是否输出」的 LLM 流式推送。</p>
        <n-form label-placement="top" class="param-form">
          <n-form-item
            v-for="p in inputDefs"
            :key="p.key"
            :label="p.label || p.key"
          >
            <n-input
              v-model:value="params[p.key]"
              type="textarea"
              :rows="3"
              :placeholder="p.required ? `{${p.key}}（必填）` : `{${p.key}}`"
            />
          </n-form-item>
        </n-form>
        <div class="pane-actions">
          <n-button
            type="primary"
            block
            :loading="submitting"
            :disabled="!workflowId"
            @click="submitRun"
          >
            执行
          </n-button>
        </div>
      </aside>

      <section class="wf-run-right">
        <n-tabs v-model:value="activeTab" type="line" animated class="run-tabs">
          <n-tab-pane name="output" tab="最终输出" display-directive="show">
            <div v-if="!answerText && !thinkingText && !submitting && !errorMsg" class="pane-empty">
              点击左侧「执行」后，回答内容将流式显示在这里
            </div>
            <div v-if="submitting && !answerText && !thinkingText" class="pane-empty">
              {{ statusText || '执行中…' }}
            </div>
            <n-alert v-if="errorMsg" type="error" :bordered="false" class="run-error">
              {{ errorMsg }}
            </n-alert>
            <ThinkingTrace
              v-if="thinkingText"
              :content="thinkingText"
              :running="submitting && thinkingRunning"
              :preview-id="`wf-run-thinking-${workflowId || 'tmp'}`"
            />
            <template v-if="answerText || (submitting && thinkingText)">
              <div v-if="statusText && submitting" class="stream-status">{{ statusText }}</div>
              <MdPreview
                v-if="answerText"
                :id="`wf-run-md-${workflowId || 'tmp'}`"
                :model-value="answerText"
                theme="light"
                preview-theme="default"
                class="result-md"
              />
              <span v-if="submitting && answerText" class="stream-caret" />
            </template>
          </n-tab-pane>
          <n-tab-pane name="steps" :tab="stepsTabLabel" display-directive="show">
            <div v-if="!steps.length && !submitting" class="pane-empty">执行后可查看各节点步骤</div>
            <div v-else-if="submitting && !steps.length" class="pane-empty">{{ statusText || '执行中…' }}</div>
            <n-timeline v-if="steps.length" class="run-timeline">
              <n-timeline-item
                v-for="(s, i) in steps"
                :key="i"
                :type="s.ok === false ? 'error' : stepTimelineType(s)"
              >
                <div class="step-card" :class="{ error: s.ok === false }">
                  <div class="step-head">
                    <span class="step-type" :data-type="s.nodeType || ''">{{ stepTypeLabel(s.nodeType) }}</span>
                    <span class="step-name">{{ s.nodeName || s.nodeKey || '节点' }}</span>
                    <span v-if="Number.isFinite(Number(s.durationMs))" class="step-cost">{{ formatDuration(s.durationMs) }}</span>
                  </div>
                  <div v-if="s.ok === false && s.detail" class="step-detail">{{ s.detail }}</div>
                  <n-collapse
                    v-if="hasStepPayload(s)"
                    class="step-collapse"
                    :default-expanded-names="[]"
                    display-directive="show"
                  >
                    <n-collapse-item title="详情" name="detail">
                      <div class="step-payload">
                        <section v-if="hasMap(s.input)" class="step-block">
                          <div class="step-block-h">输入变量</div>
                          <pre class="step-pre">{{ formatJson(s.input) }}</pre>
                        </section>
                        <section v-if="hasMap(s.params)" class="step-block">
                          <div class="step-block-h">过程参数</div>
                          <pre class="step-pre">{{ formatJson(s.params) }}</pre>
                        </section>
                        <section v-if="s.output != null && s.output !== ''" class="step-block">
                          <div class="step-block-h">输出</div>
                          <pre class="step-pre">{{ formatOutputJson(s.output) }}</pre>
                        </section>
                        <section v-if="s.thinking" class="step-block">
                          <div class="step-block-h">思考过程</div>
                          <pre class="step-pre thinking">{{ s.thinking }}</pre>
                        </section>
                      </div>
                    </n-collapse-item>
                  </n-collapse>
                </div>
              </n-timeline-item>
            </n-timeline>
          </n-tab-pane>
        </n-tabs>
      </section>
    </div>
  </Dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { runWorkflowStream } from '@/modules/agent/api'
import ThinkingTrace from '@/modules/agent/components/ThinkingTrace.vue'
import { emptyParamValues } from './startInputs'

const props = defineProps({
  show: { type: Boolean, default: false },
  workflowId: { type: [Number, String], default: null },
  workflowName: { type: String, default: '' },
  inputDefs: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:show'])

const message = useMessage()
const submitting = ref(false)
const answerText = ref('')
const thinkingText = ref('')
const thinkingRunning = ref(false)
const steps = ref([])
const errorMsg = ref('')
const statusText = ref('')
const activeTab = ref('output')
const params = reactive({})
let abortCtrl = null

const dialogTitle = computed(() =>
  props.workflowName ? `试跑 · ${props.workflowName}` : '试跑工作流'
)

const stepsTabLabel = computed(() => {
  const n = steps.value.length
  return n ? `步骤 (${n})` : '步骤'
})

watch(
  () => props.show,
  (visible) => {
    if (!visible) {
      abortCtrl?.abort()
      abortCtrl = null
      return
    }
    answerText.value = ''
    thinkingText.value = ''
    thinkingRunning.value = false
    steps.value = []
    errorMsg.value = ''
    statusText.value = ''
    activeTab.value = 'output'
    resetParams()
  }
)

watch(
  () => props.inputDefs,
  () => {
    if (props.show) resetParams()
  },
  { deep: true }
)

function resetParams() {
  const next = emptyParamValues(props.inputDefs)
  for (const k of Object.keys(params)) {
    delete params[k]
  }
  Object.assign(params, next)
}

function onShowUpdate(v) {
  emit('update:show', v)
}

const TYPE_LABELS = {
  start: '开始',
  end: '结束',
  llm: 'LLM',
  http: 'HTTP',
  knowledge: '知识库',
  code: '代码',
  sql: 'SQL',
  output: '输出',
  condition: '条件'
}

function stepTypeLabel(type) {
  const t = (type || '').toLowerCase()
  return TYPE_LABELS[t] || (t || '节点')
}

function stepTimelineType(s) {
  const t = (s.nodeType || '').toLowerCase()
  if (t === 'start') return 'info'
  if (t === 'end') return 'success'
  if (t === 'condition') return 'warning'
  return 'default'
}

function formatDuration(ms) {
  const n = Number(ms)
  if (!Number.isFinite(n)) return ''
  if (n < 1000) return `${n}ms`
  return `${(n / 1000).toFixed(n < 10000 ? 2 : 1)}s`
}

function hasMap(obj) {
  return obj && typeof obj === 'object' && !Array.isArray(obj) && Object.keys(obj).length > 0
}

function hasStepPayload(s) {
  return hasMap(s.input) || hasMap(s.params) || (s.output != null && s.output !== '') || !!s.thinking
}

function formatJson(obj) {
  try {
    return JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}

/** 输出按 JSON 美化展示（对象/数组解析后格式化，否则作为 JSON 字符串） */
function formatOutputJson(output) {
  if (output == null) return 'null'
  if (typeof output !== 'string') return formatJson(output)
  const t = output.trim()
  if ((t.startsWith('{') && t.endsWith('}')) || (t.startsWith('[') && t.endsWith(']'))) {
    try {
      return JSON.stringify(JSON.parse(t), null, 2)
    } catch {
      // fallthrough
    }
  }
  return JSON.stringify(output, null, 2)
}

async function submitRun() {
  if (!props.workflowId) return
  for (const p of props.inputDefs || []) {
    if (p.required && !String(params[p.key] ?? '').trim()) {
      message.warning(`请填写必填参数：${p.label || p.key}`)
      return
    }
  }
  abortCtrl?.abort()
  abortCtrl = new AbortController()
  submitting.value = true
  answerText.value = ''
  thinkingText.value = ''
  thinkingRunning.value = false
  steps.value = []
  errorMsg.value = ''
  statusText.value = '准备执行…'
  activeTab.value = 'output'
  try {
    const done = await runWorkflowStream(
      props.workflowId,
      { params: { ...params } },
      {
        onStatus: (s) => {
          statusText.value = s || ''
        },
        onStep: (step) => {
          if (step) {
            steps.value = [...steps.value, step]
            thinkingRunning.value = false
          }
        },
        onDelta: (delta) => {
          if (delta) {
            thinkingRunning.value = false
            answerText.value += delta
          }
        },
        onThinking: (delta) => {
          if (delta) {
            thinkingRunning.value = true
            thinkingText.value += delta
          }
        },
        onError: (msg) => {
          errorMsg.value = msg || '执行失败'
        }
      },
      abortCtrl.signal
    )
    if (done) {
      if (done.output != null && !answerText.value) {
        answerText.value = done.output
      }
      if (Array.isArray(done.steps) && done.steps.length && !steps.value.length) {
        steps.value = done.steps
      }
    }
    if (!errorMsg.value) {
      message.success('执行完成')
    }
  } catch (e) {
    if (e?.name !== 'AbortError') {
      errorMsg.value = e?.message || e?.msg || '执行失败'
    }
  } finally {
    submitting.value = false
    thinkingRunning.value = false
    statusText.value = ''
    abortCtrl = null
  }
}
</script>

<style scoped>
.wf-run {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  height: 100%;
  min-height: 0;
}
.wf-run-left,
.wf-run-right {
  min-height: 0;
  overflow: auto;
  padding: 16px 20px 20px;
}
.wf-run-left {
  border-right: 1px solid var(--color-border, #e5e7eb);
  display: flex;
  flex-direction: column;
}
.wf-run-right {
  display: flex;
  flex-direction: column;
}
.run-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.run-tabs :deep(.n-tabs-pane-wrapper) {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.pane-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}
.pane-hint {
  margin: 0 0 14px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-muted, #8a94a6);
}
.param-form {
  flex: 1;
}
.pane-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 80%, transparent);
}
.pane-empty {
  margin-top: 24px;
  font-size: 13px;
  color: var(--color-text-muted, #8a94a6);
}
.run-error {
  margin: 12px 0;
}
.run-timeline {
  padding: 4px 0 0 2px;
}
.run-timeline :deep(.n-timeline-item-content__title) {
  display: none;
}
.step-card {
  margin: 0 0 4px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 85%, transparent);
  background: color-mix(in srgb, var(--color-fill-secondary, #f8fafc) 45%, #fff);
}
.step-card.error {
  border-color: color-mix(in srgb, #e5484d 45%, var(--color-border, #e5e7eb));
  background: color-mix(in srgb, #e5484d 6%, #fff);
}
.step-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.step-type {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #334155;
  background: #e2e8f0;
}
.step-type[data-type='start'] {
  color: #0f4c81;
  background: #dbeafe;
}
.step-type[data-type='end'] {
  color: #166534;
  background: #dcfce7;
}
.step-type[data-type='llm'] {
  color: #6b21a8;
  background: #f3e8ff;
}
.step-type[data-type='condition'] {
  color: #9a3412;
  background: #ffedd5;
}
.step-type[data-type='knowledge'] {
  color: #075985;
  background: #e0f2fe;
}
.step-type[data-type='http'] {
  color: #115e59;
  background: #ccfbf1;
}
.step-type[data-type='output'] {
  color: #1e3a5f;
  background: #e2e8f0;
}
.step-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text, #1f2329);
}
.step-cost {
  margin-left: auto;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-muted, #8a94a6);
  background: color-mix(in srgb, var(--color-fill-secondary, #f1f5f9) 90%, transparent);
  border-radius: 999px;
  padding: 2px 8px;
}
.step-detail {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-muted, #64748b);
  white-space: pre-wrap;
  word-break: break-word;
}
.step-collapse {
  margin-top: 8px;
}
.step-collapse :deep(.n-collapse-item) {
  margin: 0;
}
.step-collapse :deep(.n-collapse-item__header) {
  padding: 6px 0;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}
.step-collapse :deep(.n-collapse-item__content-inner) {
  padding: 0 0 8px;
}
.step-payload {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.step-block-h {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}
.step-pre {
  margin: 0;
  padding: 10px 12px;
  max-height: 280px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.55;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  white-space: pre-wrap;
  word-break: break-word;
  color: #0f172a;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 70%, transparent);
}
.step-pre.thinking {
  background: #faf5ff;
  color: #581c87;
}
.stream-status {
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--color-text-muted, #8a94a6);
}
.stream-caret {
  display: inline-block;
  width: 8px;
  height: 1.1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: var(--color-primary, #2a6f97);
  animation: blink 1s step-end infinite;
}
@keyframes blink {
  50% {
    opacity: 0;
  }
}
.result-md {
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 90%, transparent);
  background: color-mix(in srgb, var(--color-fill-secondary, #f5f6f8) 35%, transparent);
  padding: 4px 12px;
}
.result-md :deep(.md-editor-preview-wrapper) {
  padding: 0;
}
.result-md :deep(.md-editor-preview) {
  font-size: 14px;
  line-height: 1.65;
  color: var(--color-text, #1f2329);
}
.result-md :deep(.md-editor-preview > :first-child) {
  margin-top: 0;
}
.result-md :deep(.md-editor-preview > :last-child) {
  margin-bottom: 0;
}
@media (max-width: 900px) {
  .wf-run {
    grid-template-columns: 1fr;
  }
  .wf-run-left {
    border-right: none;
    border-bottom: 1px solid var(--color-border, #e5e7eb);
    max-height: 42vh;
  }
}
</style>

<style>
.app-dialog--wf-run.n-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.app-dialog--wf-run .n-card__content {
  flex: 1;
  min-height: 0;
  overflow: hidden !important;
  padding: 0 !important;
  display: flex;
  flex-direction: column;
}
</style>
