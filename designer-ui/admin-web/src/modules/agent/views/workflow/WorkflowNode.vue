<template>
  <div
    class="wf-node"
    :class="{ selected, 'is-condition': data.nodeType === 'condition' }"
    :style="{ '--node-color': meta.color, minHeight: nodeMinHeight }"
  >
    <Handle
      v-if="data.nodeType !== 'start'"
      type="target"
      :position="Position.Left"
      class="wf-handle"
    />
    <div class="wf-node-body">
      <div class="wf-node-type">{{ meta.label }}</div>
      <div class="wf-node-name">{{ data.name || meta.label }}</div>
      <div v-if="summary && data.nodeType !== 'condition'" class="wf-node-config">{{ summary }}</div>
      <div v-if="data.outputVar" class="wf-node-var">→ {{ '{' + data.outputVar + '}' }}</div>
    </div>
    <div v-if="data.nodeType === 'condition'" class="wf-branch-col">
      <div
        v-for="(b, i) in conditionHandles"
        :key="'lb-' + b.id"
        class="wf-branch-item"
      >
        {{ branchTitle(b, i) }}
      </div>
    </div>
    <template v-if="data.nodeType === 'condition'">
      <Handle
        v-for="(b, i) in conditionHandles"
        :id="b.id"
        :key="b.id"
        type="source"
        :position="Position.Right"
        :style="{ top: handleTop(i) }"
        class="wf-handle"
      />
    </template>
    <Handle
      v-else-if="data.nodeType !== 'end'"
      type="source"
      :position="Position.Right"
      class="wf-handle"
    />
  </div>
</template>

<script setup>
import { computed, inject } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { conditionBranches, configSummary, nodeMeta } from './nodeMeta'

const BRANCH_ROW = 22
const BRANCH_PAD_TOP = 10

const props = defineProps({
  data: { type: Object, required: true },
  selected: { type: Boolean, default: false }
})

const workflowLookups = inject('workflowLookups', null)

const meta = computed(() => nodeMeta(props.data.nodeType))
const summary = computed(() =>
  configSummary(props.data.nodeType, props.data.config, workflowLookups || {})
)
const conditionHandles = computed(() => conditionBranches(props.data.config))
const nodeMinHeight = computed(() => {
  if (props.data.nodeType !== 'condition') return undefined
  return `${Math.max(64, BRANCH_PAD_TOP * 2 + conditionHandles.value.length * BRANCH_ROW)}px`
})

function branchTitle(b, i) {
  if (b.title === '否则') return '否则'
  if (i === 0) return '如果'
  const n = conditionHandles.value.filter((x) => x.title === '否则如果').length
  if (n <= 1) return '否则如果'
  return `否则如果${i}`
}

function handleTop(i) {
  return `${BRANCH_PAD_TOP + i * BRANCH_ROW + BRANCH_ROW / 2}px`
}
</script>

<style scoped>
.wf-node {
  min-width: 148px;
  max-width: 220px;
  border-radius: 8px;
  background: var(--color-bg, #fff);
  border: 1px solid color-mix(in srgb, var(--node-color) 35%, #d0d5dd);
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.06);
  display: flex;
  /* 勿 overflow:hidden，否则侧边 Handle 命中区被裁切，拉线难对准 */
  overflow: visible;
  position: relative;
}
.wf-node.is-condition {
  min-width: 196px;
  max-width: 260px;
  align-items: stretch;
}
.wf-node.selected {
  border-color: var(--node-color);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--node-color) 28%, transparent);
}
.wf-node-body {
  padding: 8px 10px;
  min-width: 0;
  flex: 1;
}
.wf-node.is-condition .wf-node-body {
  padding-right: 8px;
}
.wf-node-type {
  font-size: 11px;
  font-weight: 600;
  color: var(--node-color);
  letter-spacing: 0.02em;
}
.wf-node-name {
  margin-top: 2px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text, #1f2329);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wf-node-config {
  margin-top: 4px;
  font-size: 11px;
  color: var(--color-text-muted, #8a94a6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wf-node-var {
  margin-top: 4px;
  font-size: 11px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: var(--node-color);
}
.wf-branch-col {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding: 10px 16px 10px 8px;
  border-left: 1px solid color-mix(in srgb, var(--node-color) 18%, #e5e7eb);
}
.wf-branch-item {
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  font-size: 11px;
  line-height: 1;
  color: var(--color-text-muted, #8a94a6);
  white-space: nowrap;
  padding-right: 2px;
}
.wf-handle {
  width: 12px !important;
  height: 12px !important;
  background: var(--node-color) !important;
  border: 2px solid #fff !important;
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--node-color) 40%, transparent);
}
/* 扩大透明命中区，视觉仍是小圆点 */
.wf-handle::after {
  content: '';
  position: absolute;
  inset: -10px;
  border-radius: 50%;
}
</style>
