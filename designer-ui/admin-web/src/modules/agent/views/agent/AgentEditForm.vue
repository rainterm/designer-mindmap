<template>
  <n-spin :show="loading" class="agent-edit-spin">
    <n-form ref="formRef" :model="form" :rules="formRules" label-placement="top" class="agent-edit-form">
      <div v-if="form.id" class="ver-bar">
        <div class="ver-bar-left">
          <n-tag v-if="form.version" :bordered="false" type="success">v{{ form.version }}</n-tag>
          <n-tag v-if="form.hasUnpublished" :bordered="false" type="warning">草稿</n-tag>
          <span class="ver-bar-hint">保存仅写草稿，发布后才正式生效</span>
        </div>
        <n-space>
          <n-button
            v-perm="'agent:agent:history'"
            :disabled="historyLoading"
            @click="openHistory"
          >
            历史版本
          </n-button>
          <n-button
            v-perm="'agent:agent:publish'"
            type="primary"
            :loading="publishing"
            :disabled="!form.hasUnpublished"
            @click="handlePublish"
          >
            发布
          </n-button>
        </n-space>
      </div>
      <div class="edit-grid">
        <div class="panel-col">
          <n-card :bordered="false" title="基础信息" class="panel-card">
            <template #header-extra>
              <span class="panel-extra">名称与角色设定</span>
            </template>
            <div class="panel-body">
              <div class="id-row">
                <n-upload
                  v-model:file-list="avatarFileList"
                  class="avatar-upload"
                  list-type="image-card"
                  accept="image/*"
                  :max="1"
                  :custom-request="handleAvatarUpload"
                  :disabled="avatarUploading"
                />
                <div class="id-fields">
                  <n-form-item label="名称" path="name">
                    <n-input v-model:value="form.name" placeholder="智能体名称" maxlength="50" show-count />
                  </n-form-item>
                </div>
              </div>
              <n-form-item label="系统提示词">
                <n-input
                  v-model:value="form.systemPrompt"
                  type="textarea"
                  :autosize="{ minRows: 8, maxRows: 18 }"
                  placeholder="定义角色、边界、语气与输出格式，选填"
                  class="prompt-input"
                />
              </n-form-item>
              <n-form-item label="描述">
                <n-input
                  v-model:value="form.remark"
                  type="textarea"
                  :rows="2"
                  placeholder="展示给使用者的简介，选填"
                />
              </n-form-item>
              <div class="switch-row">
                <n-form-item label="启用" class="switch-item">
                  <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
                </n-form-item>
                <n-form-item class="switch-item">
                  <template #label>
                    <span class="label-with-tip">
                      附件上传
                      <n-tooltip :show-arrow="false" placement="top">
                        <template #trigger>
                          <n-icon class="label-tip" :component="HelpCircleOutline" :size="14" />
                        </template>
                        开启后对话时可上传附件
                      </n-tooltip>
                    </span>
                  </template>
                  <n-switch v-model:value="form.fileEnabled" :checked-value="1" :unchecked-value="0" />
                </n-form-item>
              </div>
            </div>
          </n-card>
        </div>

        <div class="panel-col">
          <n-card :bordered="false" title="模型绑定" class="panel-card">
            <template #header-extra>
              <span class="panel-extra">供应商与采样参数</span>
            </template>
            <n-form-item label="供应商" path="providerId">
              <n-select
                v-model:value="form.providerId"
                :options="providerOptions"
                placeholder="选择供应商"
                clearable
                @update:value="onProviderChange"
              />
            </n-form-item>
            <n-form-item label="模型" path="modelCode">
              <n-select
                v-model:value="form.modelCode"
                :options="modelOptions"
                placeholder="选择模型"
                clearable
                :disabled="!form.providerId"
              />
            </n-form-item>
            <div class="param-row">
              <div class="param-label">
                <span>Temperature</span>
                <b>{{ form.temperature }}</b>
              </div>
              <n-slider v-model:value="form.temperature" :min="0" :max="2" :step="0.1" />
            </div>
            <div class="param-row">
              <div class="param-label">
                <span>Top P</span>
                <b>{{ form.topP }}</b>
              </div>
              <n-slider v-model:value="form.topP" :min="0" :max="1" :step="0.05" />
            </div>
            <n-form-item label="Max Tokens">
              <n-input-number
                v-model:value="form.maxTokens"
                :min="1"
                :max="128000"
                clearable
                placeholder="不限制"
                style="width: 100%"
              />
              <div class="field-hint">留空则不限制，使用模型默认最大输出</div>
            </n-form-item>
            <n-form-item label="记忆轮数">
              <n-input-number
                v-model:value="form.memoryRounds"
                :min="0"
                :max="50"
                style="width: 100%"
              />
              <div class="field-hint">带入上下文的最近对话轮数（一轮=用户+助手）；0 表示不带历史</div>
            </n-form-item>
          </n-card>

          <n-card :bordered="false" title="能力扩展" class="panel-card">
            <template #header-extra>
              <span class="panel-extra">工具 / MCP / Skill / 工作流 / 知识库</span>
            </template>
            <n-form-item label="绑定工具">
              <n-select
                v-model:value="form.toolIds"
                :options="toolOptions"
                multiple
                filterable
                placeholder="选择内置工具"
                clearable
              />
            </n-form-item>
            <n-form-item label="绑定 MCP">
              <n-select
                v-model:value="form.mcpIds"
                :options="mcpOptions"
                multiple
                filterable
                placeholder="选择 MCP Server"
                clearable
              />
            </n-form-item>
            <n-form-item label="绑定 Skill">
              <n-select
                v-model:value="form.skillIds"
                :options="skillOptions"
                multiple
                filterable
                placeholder="选择 Skill"
                clearable
              />
            </n-form-item>
            <n-form-item label="绑定工作流">
              <n-select
                v-model:value="form.workflowIds"
                :options="workflowOptions"
                multiple
                filterable
                placeholder="选择工作流"
                clearable
              />
            </n-form-item>
            <n-form-item label="绑定知识库">
              <n-select
                v-model:value="form.knowledgeIds"
                :options="knowledgeOptions"
                multiple
                filterable
                placeholder="选择知识库"
                clearable
              />
            </n-form-item>
          </n-card>

          <n-card :bordered="false" title="知识检索 (RAG)" class="panel-card">
            <template #header-extra>
              <span class="panel-extra">对话时检索参数</span>
            </template>
            <n-form-item label="Embedding 供应商">
              <n-select
                v-model:value="form.embeddingProviderId"
                :options="providerOptions"
                placeholder="默认使用知识库索引模型"
                clearable
                @update:value="onEmbeddingProviderChange"
              />
            </n-form-item>
            <n-form-item label="Embedding 模型">
              <n-select
                v-model:value="form.embeddingModel"
                :options="embeddingModelOptions"
                placeholder="须与知识库入库模型一致"
                clearable
                filterable
                tag
                :disabled="!form.embeddingProviderId"
              />
              <div class="field-hint">留空则按各知识库自身 Embedding 查询；填写时建议与绑定知识库入库模型相同</div>
            </n-form-item>
            <n-form-item label="Rerank 供应商">
              <n-select
                v-model:value="form.rerankProviderId"
                :options="providerOptions"
                placeholder="可选，不填则按向量分排序"
                clearable
                @update:value="onRerankProviderChange"
              />
            </n-form-item>
            <n-form-item label="Rerank 模型">
              <n-select
                v-model:value="form.rerankModel"
                :options="rerankModelOptions"
                placeholder="重排序模型"
                clearable
                filterable
                tag
                :disabled="!form.rerankProviderId"
              />
            </n-form-item>
            <n-form-item label="最终 Top K">
              <n-input-number
                v-model:value="form.ragTopK"
                :min="1"
                :max="50"
                style="width: 100%"
              />
              <div class="field-hint">进入对话上下文的分块数；开启 Rerank 时为重排后截断数</div>
            </n-form-item>
            <n-form-item v-if="form.rerankProviderId" label="召回候选数">
              <n-input-number
                v-model:value="form.ragRecallTopK"
                :min="1"
                :max="50"
                clearable
                placeholder="留空 = 自动"
                style="width: 100%"
              />
              <div class="field-hint">
                向量先召回的候选条数，再交给 Rerank。留空表示自动（约 最终 Top K × 3，当前约
                {{ autoRecallTopK }}）
              </div>
            </n-form-item>
            <div class="param-row">
              <div class="param-label">
                <span>相似度阈值（向量召回）</span>
                <b>{{ Number(form.ragScoreThreshold).toFixed(2) }}</b>
              </div>
              <n-slider v-model:value="form.ragScoreThreshold" :min="0" :max="1" :step="0.01" />
              <div class="field-hint">仅过滤 Embedding 召回；低于该值的分块不会进入候选 / 上下文</div>
            </div>
          </n-card>
        </div>

        <div class="panel-col debug-col">
          <AgentDebugPanel
            :agent-id="form.id"
            :agent-name="form.name"
            :agent-avatar="form.avatar"
            :file-enabled="form.fileEnabled"
            :before-send="beforeDebugSend"
            :use-draft="true"
          />
        </div>
      </div>
    </n-form>

    <AgentHistoryDialog
      v-model:show="historyVisible"
      :agent-id="form.id"
      :current-version="form.version"
      @rolled-back="onRolledBack"
    />
  </n-spin>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { HelpCircleOutline } from '@vicons/ionicons5'
import {
  createAgent,
  getAgent,
  listKnowledges,
  listMcps,
  listProviders,
  listSkills,
  listTools,
  listWorkflows,
  publishAgent,
  updateAgent
} from '@/modules/agent/api'
import { uploadFile } from '@/modules/system/api/file'
import AgentDebugPanel from './AgentDebugPanel.vue'
import AgentHistoryDialog from './AgentHistoryDialog.vue'

const props = defineProps({
  /** 编辑时的智能体 ID；空则为新建 */
  agentId: { type: [Number, String], default: null }
})

const emit = defineEmits(['saved', 'missing'])

const message = useMessage()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const publishing = ref(false)
const historyVisible = ref(false)
const historyLoading = ref(false)
const avatarUploading = ref(false)
const avatarFileList = ref([])
const providers = ref([])
const toolOptions = ref([])
const mcpOptions = ref([])
const skillOptions = ref([])
const workflowOptions = ref([])
const knowledgeOptions = ref([])

const emptyForm = () => ({
  id: null,
  name: '',
  code: '',
  avatar: '',
  systemPrompt: '',
  providerId: null,
  modelCode: '',
  temperature: 0.7,
  topP: 1,
  maxTokens: null,
  memoryRounds: 10,
  toolIds: [],
  mcpIds: [],
  skillIds: [],
  workflowIds: [],
  knowledgeIds: [],
  embeddingProviderId: null,
  embeddingModel: '',
  rerankProviderId: null,
  rerankModel: '',
  ragTopK: 5,
  ragRecallTopK: null,
  ragScoreThreshold: 0.35,
  fileEnabled: 0,
  status: 1,
  remark: '',
  version: null,
  draftVersion: null,
  hasUnpublished: false
})

const form = reactive(emptyForm())

const formRules = {
  name: { required: true, message: '请输入名称', trigger: 'blur' },
  providerId: { required: true, type: 'number', message: '请选择供应商', trigger: 'change' },
  modelCode: { required: true, message: '请选择模型', trigger: 'change' }
}

const providerOptions = computed(() =>
  providers.value.map((p) => ({ label: p.name, value: p.id }))
)

const MODEL_TYPE_LABEL = {
  chat: '对话',
  multimodal: '多模态',
  embedding: 'Embedding',
  rerank: 'Rerank',
  speech: '语音识别',
  tts: '语音合成',
  image: '图像生成',
  moderation: '审核'
}

function isTextEmbeddingModel(m) {
  return m?.modelType === 'embedding'
}
function isRerankModel(m) {
  return m?.modelType === 'rerank'
}

const modelOptions = computed(() => {
  const p = providers.value.find((x) => x.id === form.providerId)
  return (p?.models || [])
    .filter((m) => m.status !== 0)
    .map((m) => {
      const name = m.displayName || m.modelCode
      const typeLabel = MODEL_TYPE_LABEL[m.modelType]
      return {
        label: typeLabel ? `[${typeLabel}] ${name}` : name,
        value: m.modelCode
      }
    })
})

const embeddingModelOptions = computed(() => {
  const p = providers.value.find((x) => x.id === form.embeddingProviderId)
  return (p?.models || [])
    .filter((m) => m.status !== 0 && isTextEmbeddingModel(m))
    .map((m) => ({
      label: m.displayName || m.modelCode,
      value: m.modelCode
    }))
})

const rerankModelOptions = computed(() => {
  const p = providers.value.find((x) => x.id === form.rerankProviderId)
  return (p?.models || [])
    .filter((m) => m.status !== 0 && isRerankModel(m))
    .map((m) => ({
      label: m.displayName || m.modelCode,
      value: m.modelCode
    }))
})

function onProviderChange() {
  form.modelCode = ''
}

function onEmbeddingProviderChange() {
  form.embeddingModel = ''
}

function onRerankProviderChange() {
  form.rerankModel = ''
}

const autoRecallTopK = computed(() => {
  const k = Math.max(1, Math.min(50, Number(form.ragTopK) || 5))
  return Math.min(50, Math.max(k, k * 3))
})

function syncAvatarFileList(url) {
  if (!url) {
    avatarFileList.value = []
    return
  }
  avatarFileList.value = [
    {
      id: 'agent-avatar',
      name: 'icon',
      status: 'finished',
      url
    }
  ]
}

async function handleAvatarUpload({ file, onFinish, onError }) {
  const raw = file?.file
  if (!raw) {
    onError?.()
    return
  }
  if (!String(raw.type || '').startsWith('image/')) {
    message.error('请上传图片文件')
    onError?.()
    return
  }
  avatarUploading.value = true
  try {
    const res = await uploadFile(raw, 'agent-avatar')
    const url = res.data?.url || ''
    if (!url) throw new Error('上传失败：未返回地址')
    form.avatar = url
    file.url = url
    file.status = 'finished'
    syncAvatarFileList(url)
    message.success('图标已上传')
    onFinish?.()
  } catch (e) {
    message.error(e?.message || '上传失败')
    onError?.()
  } finally {
    avatarUploading.value = false
  }
}

watch(avatarFileList, (list) => {
  if (!list?.length) form.avatar = ''
})

function resetForm() {
  Object.assign(form, emptyForm())
  syncAvatarFileList('')
  formRef.value?.restoreValidation?.()
  markSaved()
}

async function loadOptions() {
  const [pRes, tRes, mRes, sRes, wRes, kRes] = await Promise.all([
    listProviders(),
    listTools(),
    listMcps(),
    listSkills(),
    listWorkflows(),
    listKnowledges()
  ])
  providers.value = pRes.data || []
  toolOptions.value = (tRes.data || []).map((t) => ({ label: t.name, value: t.id }))
  mcpOptions.value = (mRes.data || []).map((m) => ({
    label: `${m.name} (${m.transport})`,
    value: m.id
  }))
  skillOptions.value = (sRes.data || []).map((s) => ({ label: s.name, value: s.id }))
  workflowOptions.value = (wRes.data || []).map((w) => ({ label: w.name, value: w.id }))
  knowledgeOptions.value = (kRes.data || []).map((k) => ({ label: k.name, value: k.id }))
}

async function loadDetail(id) {
  if (!id) {
    resetForm()
    return
  }
  const res = await getAgent(id)
  const row = res.data
  if (!row) {
    message.error('智能体不存在')
    emit('missing')
    return
  }
  Object.assign(form, {
    id: row.id,
    name: row.name,
    code: row.code,
    avatar: row.avatar || '',
    systemPrompt: row.systemPrompt || '',
    providerId: row.providerId,
    modelCode: row.modelCode || '',
    temperature: row.temperature ?? 0.7,
    topP: row.topP ?? 1,
    maxTokens: row.maxTokens > 0 ? row.maxTokens : null,
    memoryRounds: row.memoryRounds == null ? 10 : row.memoryRounds,
    toolIds: [...(row.toolIds || [])],
    mcpIds: [...(row.mcpIds || [])],
    skillIds: [...(row.skillIds || [])],
    workflowIds: [...(row.workflowIds || [])],
    knowledgeIds: [...(row.knowledgeIds || [])],
    embeddingProviderId: row.embeddingProviderId ?? null,
    embeddingModel: row.embeddingModel || '',
    rerankProviderId: row.rerankProviderId ?? null,
    rerankModel: row.rerankModel || '',
    ragTopK: row.ragTopK > 0 ? row.ragTopK : 5,
    ragRecallTopK: row.ragRecallTopK > 0 ? row.ragRecallTopK : null,
    ragScoreThreshold: row.ragScoreThreshold ?? 0.35,
    fileEnabled: row.fileEnabled === 1 ? 1 : 0,
    status: row.status,
    remark: row.remark || '',
    version: row.version ?? null,
    draftVersion: row.draftVersion ?? null,
    hasUnpublished: row.hasUnpublished === true
  })
  syncAvatarFileList(form.avatar)
  markSaved()
}

async function init() {
  loading.value = true
  try {
    await loadOptions()
    await loadDetail(props.agentId)
  } finally {
    loading.value = false
  }
}

/** 同步后端返回的版本状态到表单 */
function syncVersionMeta(vo) {
  if (!vo) return
  form.version = vo.version ?? null
  form.draftVersion = vo.draftVersion ?? null
  form.hasUnpublished = vo.hasUnpublished === true
}

/** 已保存内容快照（脏检测用，不含版本号等元信息） */
const savedSnapshot = ref('')

function snapshotOf() {
  const { id, version, draftVersion, hasUnpublished, ...config } = form
  return JSON.stringify(config)
}

function markSaved() {
  savedSnapshot.value = snapshotOf()
}

/** 表单是否有未保存改动 */
function isDirty() {
  return snapshotOf() !== savedSnapshot.value
}

async function save({ silent = false } = {}) {
  try {
    await formRef.value?.validate()
  } catch {
    return false
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (payload.id) {
      const res = await updateAgent(payload)
      syncVersionMeta(res.data)
      if (!silent) message.success('草稿已保存（发布后才正式生效）')
    } else {
      const res = await createAgent(payload)
      form.id = res.data.id
      syncVersionMeta(res.data)
      message.success('新增成功')
    }
    markSaved()
    emit('saved', { ...form })
    return true
  } finally {
    submitting.value = false
  }
}

/** 发布：先保存当前草稿，再固化发布 */
async function handlePublish() {
  publishing.value = true
  try {
    const saved = await save({ silent: true })
    if (!saved) return
    const res = await publishAgent(form.id)
    syncVersionMeta(res.data)
    message.success(`已发布为 v${res.data?.version ?? ''}`)
    emit('saved', { ...form })
  } catch (e) {
    message.error(e?.msg || e?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

function openHistory() {
  historyLoading.value = true
  historyVisible.value = true
  // 弹窗自身会拉取列表；这里只做一次性禁用防抖动
  setTimeout(() => {
    historyLoading.value = false
  }, 300)
}

/** 历史版本回滚后：重新加载草稿内容与版本状态 */
async function onRolledBack(version) {
  message.success(`草稿已回滚为 v${version} 的配置`)
  await loadDetail(form.id)
  emit('saved', { ...form })
}

/**
 * 调试面板发送前钩子：有未保存改动时自动保存草稿，失败（校验不过/接口异常）则中断发送。
 * 调试会话以 useDraft 运行草稿配置，此处保存保证最新改动即时生效且草稿不丢。
 */
async function beforeDebugSend() {
  if (!form.id) {
    message.warning('请先保存智能体后再调试')
    return false
  }
  if (!isDirty()) return true
  const ok = await save({ silent: true })
  if (!ok) return false
  message.info('检测到未保存改动，已自动保存草稿')
  return true
}

watch(
  () => props.agentId,
  () => {
    init()
  },
  { immediate: true }
)

defineExpose({
  save,
  resetForm,
  submitting,
  form
})
</script>

<style scoped>
.agent-edit-spin {
  height: calc(100vh - 132px);
  min-height: 480px;
  display: flex;
  flex-direction: column;
}
.agent-edit-spin :deep(.n-spin-container),
.agent-edit-spin :deep(.n-spin-content) {
  flex: 1;
  min-height: 0;
  height: 100%;
  display: flex !important;
  flex-direction: column !important;
}
.agent-edit-form {
  flex: 1;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.edit-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  align-items: stretch;
}
.ver-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 80%, transparent);
  background: color-mix(in srgb, var(--color-fill-secondary, #f5f6f8) 50%, transparent);
}
.ver-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ver-bar-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}
.panel-col {
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.debug-col {
  overflow: hidden;
}
.panel-card {
  flex-shrink: 0;
}
.panel-extra {
  font-size: 12px;
  color: var(--color-text-muted);
  font-weight: 400;
}
.field-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-text-muted);
}
.panel-body {
  padding: 0;
}
.id-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.avatar-upload {
  flex-shrink: 0;
  width: 64px;
}
.avatar-upload :deep(.n-upload-file),
.avatar-upload :deep(.n-upload-trigger.n-upload-trigger--image-card) {
  width: 64px;
  height: 64px;
}
.id-fields {
  flex: 1;
  min-width: 0;
}
.switch-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}
.switch-item {
  margin-bottom: 0;
}
.param-row {
  margin-bottom: 16px;
}
.param-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.param-label b {
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}
.label-with-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.label-tip {
  color: var(--color-text-muted);
  cursor: help;
  vertical-align: middle;
}
.label-tip:hover {
  color: var(--color-primary);
}
.prompt-input :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.55;
}
@media (max-width: 1200px) {
  .edit-grid {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: auto minmax(360px, 1fr);
  }
  .debug-col {
    grid-column: 1 / -1;
    min-height: 360px;
  }
}
@media (max-width: 800px) {
  .edit-grid {
    grid-template-columns: 1fr;
    grid-template-rows: none;
  }
  .debug-col {
    grid-column: auto;
    min-height: 320px;
  }
}
</style>
