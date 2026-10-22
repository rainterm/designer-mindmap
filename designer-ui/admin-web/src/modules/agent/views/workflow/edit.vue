<template>
  <div class="page-container wf-edit" :class="{ 'is-standalone': standalone }">
    <n-card :bordered="false" class="edit-topbar">
      <div class="topbar-inner">
        <div class="topbar-left">
          <span class="topbar-title">{{ form.name || '未命名' }}</span>
          <n-tag v-if="form.version" :bordered="false" type="success" class="topbar-ver">
            v{{ form.version }}
          </n-tag>
          <n-tag v-if="hasUnpublished" :bordered="false" type="warning" class="topbar-ver">
            草稿
          </n-tag>
        </div>
        <n-space>
          <n-button @click="goBack">取消</n-button>
          <n-button
            v-if="form.id"
            v-perm="'agent:workflow:edit'"
            :disabled="form.status !== 1"
            @click="openRun"
          >
            试跑
          </n-button>
          <n-button
            v-perm="'agent:workflow:edit'"
            :loading="submitting"
            @click="handleSave"
          >
            保存草稿
          </n-button>
          <n-button
            v-if="form.id"
            v-perm="'agent:workflow:history'"
            @click="openHistory"
          >
            历史版本
          </n-button>
          <n-button
            v-if="form.id"
            v-perm="'agent:workflow:publish'"
            type="primary"
            :loading="publishing"
            :disabled="!hasUnpublished"
            @click="handlePublish"
          >
            发布
          </n-button>
        </n-space>
      </div>
    </n-card>

    <div class="wf-shell">
      <aside class="wf-palette">
        <div class="panel-title">组件</div>
        <p class="panel-hint">拖到画布添加节点</p>
        <div
          v-for="t in PALETTE_TYPES"
          :key="t"
          class="palette-item"
          draggable="true"
          @dragstart="onPaletteDragStart($event, t)"
        >
          <span class="palette-dot" :style="{ background: nodeMeta(t).color }" />
          <div>
            <div class="palette-name">{{ nodeMeta(t).label }}</div>
            <div class="palette-desc">{{ nodeMeta(t).desc }}</div>
          </div>
        </div>
      </aside>

      <div class="wf-canvas" @drop="onCanvasDrop" @dragover.prevent>
        <!-- canvasKey：回滚后整体重建画布，避免 VueFlow 节点残留 pointer-events:none 导致无法点击 -->
        <VueFlow
          :key="canvasKey"
          id="agent-workflow-editor"
          v-model:nodes="flowNodes"
          v-model:edges="flowEdges"
          :node-types="nodeTypes"
          :default-edge-options="defaultEdgeOptions"
          :connection-line-options="connectionLineOptions"
          :connection-radius="48"
          :delete-key-code="['Backspace', 'Delete']"
          :snap-to-grid="true"
          :snap-grid="[16, 16]"
          :min-zoom="0.25"
          :max-zoom="1.5"
          @node-click="onNodeClick"
          @edge-click="onEdgeClick"
          @selection-change="onSelectionChange"
          @pane-click="selectedNodeId = null"
          @connect="onConnect"
          @nodes-delete="onNodesDelete"
        >
          <Background :gap="16" pattern-color="#e5e7eb" />
          <Controls />
          <MiniMap pannable zoomable />
        </VueFlow>
      </div>

      <aside class="wf-aside">
        <template v-if="selectedNode">
          <div class="panel-title">节点属性</div>
          <n-form label-placement="top">
            <n-form-item label="类型">
              <n-tag :bordered="false" :color="{ color: nodeMeta(selectedNode.data.nodeType).color + '22', textColor: nodeMeta(selectedNode.data.nodeType).color }">
                {{ nodeMeta(selectedNode.data.nodeType).label }}
              </n-tag>
              <span class="type-desc">{{ nodeMeta(selectedNode.data.nodeType).desc }}</span>
            </n-form-item>
            <n-form-item label="名称">
              <n-input
                :value="selectedNode.data.name"
                placeholder="节点名称"
                @update:value="(v) => updateSelected({ name: v })"
              />
            </n-form-item>
            <NodeConfigFields
              :node-type="selectedNode.data.nodeType"
              :config="selectedNode.data.config || ''"
              :skill-options="skillOptions"
              :tool-options="toolOptions"
              :mcp-options="mcpOptions"
              :knowledge-options="knowledgeOptions"
              :providers="providers"
              @update:config="(v) => updateSelected({ config: v })"
            />
            <n-form-item v-if="supportsOutputVar(selectedNode.data.nodeType)" label="输出变量">
              <div style="width: 100%">
                <n-input
                  :value="selectedNode.data.outputVar || ''"
                  placeholder="如 llm_result，下游用 {变量名} 引用"
                  class="mono"
                  @update:value="(v) => updateSelected({ outputVar: sanitizeOutputVar(v) })"
                />
                <div class="field-hint">留空则只更新默认 output；填写后额外写入该变量</div>
              </div>
            </n-form-item>
            <n-button
              v-if="selectedNode.data.nodeType !== 'start'"
              block
              type="error"
              secondary
              @click="removeSelected"
            >
              删除节点
            </n-button>
          </n-form>
        </template>
        <div v-else class="empty-tip">点击画布节点可编辑属性；拖拽连线连接节点</div>
      </aside>
    </div>

    <WorkflowRunDialog
      v-model:show="runVisible"
      :workflow-id="form.id"
      :workflow-name="form.name"
      :input-defs="runInputDefs"
    />

    <WorkflowHistoryDialog
      v-model:show="historyVisible"
      :workflow-id="form.id"
      :current-version="form.version"
      @rolled-back="onRolledBack"
    />
  </div>
</template>

<script setup>
import { computed, markRaw, nextTick, onMounted, provide, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { VueFlow, useVueFlow, addEdge, MarkerType } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import {
  getWorkflow,
  listKnowledges,
  listMcps,
  listProviders,
  listSkills,
  listTools,
  publishWorkflow,
  updateWorkflow
} from '@/modules/agent/api'
import WorkflowNode from './WorkflowNode.vue'
import NodeConfigFields from './NodeConfigFields.vue'
import WorkflowRunDialog from './WorkflowRunDialog.vue'
import WorkflowHistoryDialog from './WorkflowHistoryDialog.vue'
import {
  PALETTE_TYPES,
  conditionBranches,
  createNodeKey,
  defaultNodeConfig,
  defaultNodeName,
  fromFlowGraph,
  nodeMeta,
  supportsOutputVar,
  toFlowEdges,
  toFlowNodes
} from './nodeMeta'
import { parseStartInputs, startInputsFromNodes } from './startInputs'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const standalone = computed(() => !!route.meta?.standalone)
const { screenToFlowCoordinate, fitView } = useVueFlow({ id: 'agent-workflow-editor' })

const submitting = ref(false)
const publishing = ref(false)
const selectedNodeId = ref(null)
const flowNodes = ref([])
const flowEdges = ref([])
/** 画布重建开关：回滚后 +1 强制重建 VueFlow，清理残留交互状态 */
const canvasKey = ref(0)

const runVisible = ref(false)
const historyVisible = ref(false)
const runInputDefs = ref([])
const providers = ref([])
const skillOptions = ref([])
const toolOptions = ref([])
const mcpOptions = ref([])
const knowledgeOptions = ref([])

/** 供画布节点显示名称摘要 */
const workflowLookups = reactive({ knowledge: {} })
provide('workflowLookups', workflowLookups)

const nodeTypes = {
  workflow: markRaw(WorkflowNode)
}

const defaultEdgeOptions = {
  type: 'default',
  markerEnd: MarkerType.ArrowClosed
}
const connectionLineOptions = {
  type: 'default',
  markerEnd: MarkerType.ArrowClosed
}

const form = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  status: 1,
  remark: '',
  version: null,
  draftVersion: null
})

/** 草稿版本号高于已发布版本号 → 存在未发布改动 */
const hasUnpublished = computed(() => {
  const d = Number(form.draftVersion || 0)
  const v = Number(form.version || 0)
  return d > v
})

const selectedNode = computed(() => flowNodes.value.find((n) => n.id === selectedNodeId.value) || null)

function goBack() {
  if (standalone.value) {
    window.close()
    // 非脚本打开的标签可能无法关闭，退回列表
    setTimeout(() => {
      if (!window.closed) router.push('/agent/workflow')
    }, 120)
    return
  }
  router.push('/agent/workflow')
}

async function loadNodeOptions() {
  const toOpts = (list) =>
    (list || []).map((x) => ({
      label: `${x.name} (${x.code})`,
      value: x.code
    }))
  const toKbOpts = (list) =>
    (list || []).map((x) => ({
      label: `${x.name} (${x.code})`,
      value: x.id
    }))
  const toKbMap = (list) => {
    const m = {}
    for (const x of list || []) {
      if (x?.id != null) m[x.id] = x.name || String(x.id)
    }
    return m
  }
  try {
    const [skills, tools, mcps, kbs, provs] = await Promise.all([
      listSkills().catch(() => ({ data: [] })),
      listTools().catch(() => ({ data: [] })),
      listMcps().catch(() => ({ data: [] })),
      listKnowledges().catch(() => ({ data: [] })),
      listProviders().catch(() => ({ data: [] }))
    ])
    skillOptions.value = toOpts(skills.data)
    toolOptions.value = toOpts(tools.data)
    mcpOptions.value = toOpts(mcps.data)
    knowledgeOptions.value = toKbOpts(kbs.data)
    providers.value = provs.data || []
    workflowLookups.knowledge = toKbMap(kbs.data)
  } catch {
    skillOptions.value = []
    toolOptions.value = []
    mcpOptions.value = []
    knowledgeOptions.value = []
    providers.value = []
  }
}

function sanitizeOutputVar(v) {
  return String(v || '')
    .trim()
    .replace(/[^a-zA-Z0-9_]/g, '_')
}

function openRun() {
  if (!form.id) {
    message.warning('请先保存工作流')
    return
  }
  if (form.status !== 1) {
    message.warning('仅启用状态可试跑')
    return
  }
  const nodes = fromFlowGraph(flowNodes.value, flowEdges.value).nodes
  runInputDefs.value = startInputsFromNodes(nodes)
  runVisible.value = true
}

function loadGraph(nodes, edges) {
  flowNodes.value = toFlowNodes(nodes)
  flowEdges.value = toFlowEdges(edges)
  selectedNodeId.value = null
  nextTick(() => {
    try {
      // 节点少时限制放大，避免开始/结束占满画布
      fitView({ padding: 0.35, maxZoom: 1 })
    } catch {
      /* canvas 未就绪时忽略 */
    }
  })
}

function onPaletteDragStart(event, type) {
  event.dataTransfer.setData('application/workflow-node', type)
  event.dataTransfer.effectAllowed = 'move'
}

function onCanvasDrop(event) {
  const type = event.dataTransfer.getData('application/workflow-node')
  if (!type) return
  const position = screenToFlowCoordinate({ x: event.clientX, y: event.clientY })
  const key = createNodeKey(type)
  flowNodes.value = [
    ...flowNodes.value,
    {
      id: key,
      type: 'workflow',
      position,
      deletable: type !== 'start',
      data: {
        key,
        nodeType: type,
        name: defaultNodeName(type),
        config: defaultNodeConfig(type),
        outputVar: ''
      }
    }
  ]
  selectedNodeId.value = key
}

function onNodeClick({ node }) {
  selectedNodeId.value = node.id
}

function onEdgeClick() {
  selectedNodeId.value = null
}

function onSelectionChange({ nodes }) {
  selectedNodeId.value = nodes?.[0]?.id ?? null
}

function onNodesDelete(nodes) {
  if (nodes?.some((n) => n.id === selectedNodeId.value)) {
    selectedNodeId.value = null
  }
}

function onConnect(params) {
  const src = flowNodes.value.find((n) => n.id === params.source)
  let label = params.label
  if (src?.data?.nodeType === 'condition' && params.sourceHandle) {
    const b = conditionBranches(src.data.config).find((x) => x.id === params.sourceHandle)
    label = b?.title || params.sourceHandle
  }
  flowEdges.value = addEdge(
    { ...params, ...defaultEdgeOptions, id: `e_${Date.now()}`, label },
    flowEdges.value
  )
}

function updateSelected(patch) {
  const id = selectedNodeId.value
  if (!id) return
  const current = flowNodes.value.find((n) => n.id === id)
  flowNodes.value = flowNodes.value.map((n) =>
    n.id === id ? { ...n, data: { ...n.data, ...patch } } : n
  )
  if (current?.data?.nodeType === 'condition' && patch.config != null) {
    const ids = new Set(conditionBranches(patch.config).map((b) => b.id))
    flowEdges.value = flowEdges.value.filter(
      (e) => e.source !== id || !e.sourceHandle || ids.has(e.sourceHandle)
    )
  }
}

function removeSelected() {
  const id = selectedNodeId.value
  if (!id) return
  const node = flowNodes.value.find((n) => n.id === id)
  if (!node || node.data?.nodeType === 'start' || node.deletable === false) {
    message.warning('开始节点不可删除')
    return
  }
  flowNodes.value = flowNodes.value.filter((n) => n.id !== id)
  flowEdges.value = flowEdges.value.filter((e) => e.source !== id && e.target !== id)
  selectedNodeId.value = null
}

async function loadDetail() {
  const id = route.query.id
  if (!id) {
    message.error('请先在列表中新建工作流，再点「设计」')
    goBack()
    return
  }
  const res = await getWorkflow(id)
  const row = res.data
  if (!row) {
    message.error('工作流不存在')
    goBack()
    return
  }
  Object.assign(form, {
    id: row.id,
    name: row.name,
    code: row.code,
    description: row.description || '',
    status: row.status,
    remark: row.remark || '',
    version: row.version,
    draftVersion: row.draftVersion ?? row.version
  })
  loadGraph(row.nodes || [], row.edges || [])
}

function openHistory() {
  if (!form.id) return
  historyVisible.value = true
}

/** 发布：version+1，历史快照生成 */
async function handlePublish() {
  if (!form.id) {
    message.warning('请先在列表中新建工作流')
    return
  }
  if (!hasUnpublished.value) {
    message.warning('当前没有未发布的改动')
    return
  }
  publishing.value = true
  try {
    const res = await publishWorkflow(form.id)
    const vo = res.data || {}
    form.version = vo.version ?? form.version
    form.draftVersion = vo.draftVersion ?? form.draftVersion
    message.success(`已发布为 v${form.version}`)
  } finally {
    publishing.value = false
  }
}

/** 回滚后重新拉详情（draft 内容已被快照覆盖；提示用户保存） */
async function onRolledBack(targetVersion) {
  message.info(`已应用 v${targetVersion} 的内容到草稿，请保存以生效`)
  // 回滚导致节点数组整体替换，VueFlow 可能残留 pointer-events:none；重建画布保证可交互
  canvasKey.value++
  await loadDetail()
}

async function handleSave() {
  if (!form.id) {
    message.warning('请先在列表中新建工作流')
    return
  }
  if (!flowNodes.value.length) {
    message.warning('请至少添加一个节点')
    return
  }
  // 开始节点参数名校验，避免保存后被后端兜底重命名
  const startNode = flowNodes.value.find((n) => n.data?.nodeType === 'start')
  if (startNode) {
    const inputs = parseStartInputs(startNode.data?.config || '')
    const missing = inputs.filter((p) => !p.key && !p.system)
    if (missing.length) {
      message.warning('开始节点存在未填写的参数名，请检查')
      return
    }
  }
  const graph = fromFlowGraph(flowNodes.value, flowEdges.value)
  submitting.value = true
  try {
    const res = await updateWorkflow({
      id: form.id,
      name: form.name,
      code: form.code,
      description: form.description,
      status: form.status,
      remark: form.remark,
      nodes: graph.nodes,
      edges: graph.edges
    })
    // 保存仅递增草稿版本号；已发布版本号不动
    form.draftVersion = res.data?.draftVersion ?? form.draftVersion
    message.success(hasUnpublished.value ? '已保存为草稿（尚未发布）' : '已保存为草稿')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadNodeOptions()
  await loadDetail()
})
</script>

<style scoped>
.wf-edit.is-standalone {
  min-height: 100vh;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.wf-edit.is-standalone .wf-shell {
  flex: 1;
  min-height: 0;
  height: auto;
}
.edit-topbar {
  margin-bottom: 6px;
  flex-shrink: 0;
}
.topbar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.topbar-title {
  font-size: 15px;
  font-weight: 600;
}
.topbar-ver {
  font-size: 12px;
  color: var(--color-text-muted);
}
.wf-shell {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) 320px;
  gap: 6px;
  height: calc(100vh - 180px);
  min-height: 520px;
}
.wf-palette,
.wf-aside {
  background: var(--color-bg, #fff);
  border-radius: var(--radius, 8px);
  padding: 14px 16px;
  overflow: auto;
}
.wf-aside :deep(.n-form-item) {
  margin-bottom: 14px;
}
.wf-aside :deep(.n-form-item-label) {
  padding-bottom: 6px !important;
}
.wf-aside :deep(.n-form-item-feedback-wrapper) {
  min-height: 0 !important;
}
.wf-canvas {
  position: relative;
  border-radius: var(--radius, 8px);
  overflow: hidden;
  background: #f7f8fa;
  border: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 80%, transparent);
}
.wf-canvas :deep(.vue-flow) {
  width: 100%;
  height: 100%;
}
.panel-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}
.panel-title.mt {
  margin-top: 16px;
}
.panel-hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.palette-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 8px;
  border-radius: 8px;
  border: 1px dashed color-mix(in srgb, var(--color-border, #d0d5dd) 90%, transparent);
  margin-bottom: 8px;
  cursor: grab;
  user-select: none;
  background: color-mix(in srgb, var(--color-fill-secondary, #f5f6f8) 70%, transparent);
}
.palette-item:active {
  cursor: grabbing;
}
.palette-item:hover {
  border-style: solid;
}
.palette-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}
.palette-name {
  font-size: 13px;
  font-weight: 600;
}
.palette-desc {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 2px;
}
.empty-tip {
  margin-top: 12px;
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.5;
}
.type-desc {
  margin-left: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.mono :deep(textarea),
.mono :deep(input) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
.field-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-text-muted, #8a94a6);
}
@media (max-width: 1100px) {
  .wf-shell {
    grid-template-columns: 1fr;
    height: auto;
  }
  .wf-canvas {
    height: 480px;
  }
}
</style>
