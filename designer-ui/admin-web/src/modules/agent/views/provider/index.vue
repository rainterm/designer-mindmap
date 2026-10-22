<template>
  <div class="page-container provider-page">
    <div class="provider-layout">
      <aside class="vendor-aside">
        <n-card class="vendor-card" :bordered="false">
          <div class="vendor-head">
            <h3 class="vendor-title">协议</h3>
            <n-button text type="primary" @click="selectVendor(null)">全部</n-button>
          </div>
          <n-input
            v-model:value="vendorKeyword"
            class="vendor-search"
            placeholder="搜索协议"
            clearable
          />
          <div class="vendor-list">
            <button
              type="button"
              class="vendor-item"
              :class="{ active: !query.type }"
              @click="selectVendor(null)"
            >
              全部协议
            </button>
            <button
              v-for="v in filteredVendors"
              :key="v.code"
              type="button"
              class="vendor-item"
              :class="{ active: query.type === v.code }"
              @click="selectVendor(v.code)"
            >
              {{ v.label }}
            </button>
            <n-empty v-if="!filteredVendors.length" description="无匹配协议" class="vendor-empty" />
          </div>
        </n-card>
      </aside>

      <div class="provider-main">
        <SearchBar :pager="pager">
          <template #fields>
            <n-form-item label="名称">
              <n-input v-model:value="query.name" placeholder="供应商名称" clearable />
            </n-form-item>
            <n-form-item label="状态">
              <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
            </n-form-item>
          </template>
        </SearchBar>

        <PageTable :pager="pager" :columns="columns">
          <template #toolbar-right>
            <n-space>
              <n-button v-perm="'agent:provider:add'" type="primary" @click="openCreate">
                新增供应商
              </n-button>
              <n-button
                v-perm="'agent:provider:remove'"
                type="error"
                :disabled="!checkedRowKeys.length"
                @click="handleBatchDelete"
              >
                批量删除
              </n-button>
            </n-space>
          </template>
        </PageTable>
      </div>
    </div>

    <Dialog
      v-model:show="formVisible"
      :title="isEdit ? '编辑供应商' : '新增供应商'"
      size="lg"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="协议" path="type">
          <n-select
            v-model:value="form.type"
            :options="vendorOptions"
            placeholder="选择 Solon 方言"
            @update:value="onVendorChange"
          />
        </n-form-item>
        <n-form-item v-if="vendorHint" :show-label="false">
          <div class="vendor-hint">{{ vendorHint }}</div>
        </n-form-item>
        <n-form-item label="名称" path="name">
          <n-input v-model:value="form.name" placeholder="供应商名称" />
        </n-form-item>
        <n-form-item label="Base URL" path="baseUrl">
          <n-input v-model:value="form.baseUrl" placeholder="https://api.example.com/v1" />
        </n-form-item>
        <n-form-item label="API Key">
          <n-input
            v-model:value="form.apiKey"
            type="password"
            show-password-on="click"
            :placeholder="isEdit ? '留空表示不修改' : '选填'"
          />
        </n-form-item>
        <n-form-item label="模型列表">
          <div class="model-list">
            <div class="model-toolbar">
              <n-button
                type="primary"
                secondary
                :loading="fetchingModels"
                :disabled="!form.baseUrl"
                @click="handleFetchModels"
              >
                拉取模型
              </n-button>
              <span class="model-hint">按协议拉取远端模型；有元数据时自动填充类型与上下文</span>
            </div>
            <div v-for="(m, idx) in form.models" :key="idx" class="model-row">
              <n-input v-model:value="m.modelCode" placeholder="modelCode" />
              <n-input v-model:value="m.displayName" placeholder="显示名" />
              <n-select
                v-model:value="m.modelType"
                :options="modelTypeOptions"
                clearable
                placeholder="类型（接口未返回）"
              />
              <n-input-number
                v-model:value="m.contextWindow"
                :min="1"
                clearable
                placeholder="上下文（接口未返回可留空）"
              />
              <n-button quaternary type="error" @click="form.models.splice(idx, 1)">删除</n-button>
            </div>
            <n-button dashed block @click="addModel">
              添加模型
            </n-button>
          </div>
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">停用</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  createProvider,
  deleteProviders,
  fetchProviderModels,
  listProviderVendors,
  pageProviders,
  updateProvider
} from '@/modules/agent/api'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()
const fetchingModels = ref(false)
const vendors = ref([])
const vendorKeyword = ref('')

const vendorOptions = computed(() =>
  vendors.value.map((v) => ({ label: v.label, value: v.code }))
)

const filteredVendors = computed(() => {
  const kw = vendorKeyword.value.trim().toLowerCase()
  if (!kw) return vendors.value
  return vendors.value.filter(
    (v) =>
      String(v.label || '')
        .toLowerCase()
        .includes(kw) ||
      String(v.code || '')
        .toLowerCase()
        .includes(kw)
  )
})

const vendorHint = computed(() => {
  const v = vendors.value.find((x) => x.code === form.type)
  return v?.hint || ''
})

function selectVendor(code) {
  query.type = code || null
  handleSearch()
}

const modelTypeOptions = [
  { label: '对话', value: 'chat' },
  { label: '多模态', value: 'multimodal' },
  { label: 'Embedding', value: 'embedding' },
  { label: 'Rerank', value: 'rerank' },
  { label: '语音识别', value: 'speech' },
  { label: '语音合成', value: 'tts' },
  { label: '图像生成', value: 'image' },
  { label: '审核', value: 'moderation' }
]
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const pager = usePagination((q) => pageProviders(q), {
  defaults: { name: '', type: null, status: null }
})
const { query, loadData, handleSearch, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteProviders, {
  confirm: (n) => `确定删除选中的 ${n} 个供应商吗？`,
  positiveText: '删除'
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate: openCreateBase,
  openEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    name: '',
    type: 'openai',
    baseUrl: '',
    apiKey: '',
    models: [],
    status: 1,
    remark: ''
  },
  async onSubmit(payload) {
    if (payload.id) {
      await updateProvider({ ...payload })
      message.success('更新成功')
    } else {
      await createProvider({ ...payload })
      message.success('新增成功')
    }
    loadData()
  }
})

function vendorByCode(code) {
  return vendors.value.find((v) => v.code === code)
}

function applyVendorDefaults(code, { forceUrl = false, forceName = false } = {}) {
  const v = vendorByCode(code)
  if (!v) return
  if (forceName || !form.name?.trim()) {
    form.name = v.label
  }
  if (forceUrl || !form.baseUrl?.trim()) {
    form.baseUrl = v.defaultBaseUrl || ''
  }
}

function onVendorChange(code) {
  // 新建：切换协议覆盖默认 URL/名称；编辑：仅当 URL 为空时填入默认
  applyVendorDefaults(code, { forceUrl: !isEdit.value, forceName: !isEdit.value })
}

function openCreate() {
  const type = query.type || 'openai'
  openCreateBase({ models: [], type, name: '', baseUrl: '' })
  applyVendorDefaults(type, { forceUrl: true, forceName: true })
}

function addModel() {
  form.models.push({
    modelCode: '',
    displayName: '',
    modelType: null,
    contextWindow: null,
    status: 1
  })
}

function parseContextWindow(v) {
  const n = Number(v)
  return Number.isFinite(n) && n > 0 ? n : null
}

function normalizeModelType(v) {
  if (v == null || v === '') return null
  const t = String(v).trim().toLowerCase()
  return modelTypeOptions.some((o) => o.value === t) ? t : null
}

async function handleFetchModels() {
  if (!form.baseUrl?.trim()) {
    message.warning('请先填写 Base URL')
    return
  }
  if (form.baseUrl.includes('{WorkspaceId}') || form.baseUrl.includes('{workspaceId}')) {
    message.warning('请先将 Base URL 中的 {WorkspaceId} 替换为业务空间 ID')
    return
  }
  fetchingModels.value = true
  try {
    const res = await fetchProviderModels({
      id: form.id || undefined,
      type: form.type,
      baseUrl: form.baseUrl.trim(),
      apiKey: form.apiKey || undefined
    })
    const list = Array.isArray(res.data) ? res.data : []
    form.models = list.map((m) => ({
      modelCode: m.modelCode || '',
      displayName: m.displayName || m.modelCode || '',
      modelType: normalizeModelType(m.modelType),
      contextWindow: parseContextWindow(m.contextWindow),
      status: m.status == null ? 1 : Number(m.status)
    }))
    message.success(list.length ? `已拉取 ${list.length} 个模型` : '远端未返回模型')
  } finally {
    fetchingModels.value = false
  }
}

const formRules = {
  name: { required: true, message: '请输入名称', trigger: 'blur' },
  type: { required: true, message: '请选择协议', trigger: 'change' }
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '名称', key: 'name', width: 140 },
  {
    title: '协议',
    key: 'type',
    width: 140,
    render: (row) => vendorOptions.value.find((o) => o.value === row.type)?.label || row.type
  },
  { title: 'Base URL', key: 'baseUrl', width: 180, ellipsis: { tooltip: true } },
  { title: 'API Key', key: 'apiKeyMasked', width: 120 },
  {
    title: '模型数',
    key: 'models',
    width: 80,
    render: (row) => (row.models || []).length
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NTag, { type: row.status === 1 ? 'success' : 'default', bordered: false }, {
        default: () => (row.status === 1 ? '启用' : '停用')
      })
  },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 100,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('agent:provider:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () =>
                openEdit(row, (r) => ({
                  id: r.id,
                  name: r.name,
                  type: r.type || 'openai',
                  baseUrl: r.baseUrl || '',
                  apiKey: '',
                  models: (r.models || []).map((m) => ({ ...m })),
                  status: r.status,
                  remark: r.remark || ''
                }))
            }),
          userStore.hasPerm('agent:provider:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除供应商「${row.name}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

async function handleDelete(row) {
  await deleteProviders([row.id])
  message.success('删除成功')
  loadData()
}

async function loadVendors() {
  try {
    const res = await listProviderVendors()
    vendors.value = Array.isArray(res.data) ? res.data : []
  } catch {
    vendors.value = [
      {
        code: 'openai',
        label: 'OpenAI 兼容',
        defaultBaseUrl: 'https://api.openai.com/v1',
        hint: 'DeepSeek / Kimi / 智谱 / 硅基 / 火山 / 百炼 compatible-mode 等均选此项'
      },
      {
        code: 'ollama',
        label: 'Ollama',
        defaultBaseUrl: 'http://127.0.0.1:11434',
        hint: ''
      }
    ]
  }
}

onMounted(loadVendors)
</script>

<style scoped>
.provider-layout {
  display: flex;
  align-items: stretch;
  gap: 6px;
  min-height: calc(100vh - var(--layout-nav-height, 44px) - var(--layout-tags-height, 36px) - 28px);
}
.vendor-aside {
  flex: 0 0 220px;
  width: 220px;
  min-width: 180px;
  display: flex;
}
.vendor-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: none !important;
}
.vendor-card :deep(.n-card__content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px 10px !important;
}
.vendor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.vendor-title {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
  color: var(--color-text);
  letter-spacing: -0.01em;
}
.vendor-search {
  margin-bottom: 10px;
}
.vendor-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.vendor-item {
  display: block;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
  line-height: 1.4;
}
.vendor-item:hover {
  background: color-mix(in srgb, var(--color-body) 70%, transparent);
}
.vendor-item.active {
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
  color: var(--color-primary);
  font-weight: 600;
}
.vendor-empty {
  margin-top: 24px;
}
.provider-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.vendor-hint {
  margin: -8px 0 4px 100px;
  font-size: 12px;
  line-height: 1.45;
  color: var(--n-text-color-3);
}
.model-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.model-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.model-hint {
  font-size: 12px;
  color: var(--n-text-color-3);
}
.model-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 110px 120px 36px;
  gap: 8px;
  align-items: center;
}
@media (max-width: 960px) {
  .provider-layout {
    flex-direction: column;
  }
  .vendor-aside {
    flex: none;
    width: 100%;
  }
  .vendor-card {
    max-height: 260px;
  }
}
</style>
