<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="名称">
          <n-input v-model:value="query.name" placeholder="名称" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'agent:knowledge:add'" type="primary" @click="openCreate">
            新建知识库
          </n-button>
          <n-button
            v-perm="'agent:knowledge:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除
          </n-button>
        </n-space>
      </template>
    </PageTable>

    <Dialog
      v-model:show="detailVisible"
      :title="detailTitle"
      size="full"
      :fullscreenable="false"
      :draggable="false"
      :show-footer="false"
      @after-leave="onDetailLeave"
    >
      <KnowledgeDetailPanel
        v-if="detailVisible && detailId"
        :knowledge-id="detailId"
        @title-change="detailTitle = $event"
        @close="detailVisible = false"
        @changed="loadData"
      />
    </Dialog>

    <Dialog
      v-model:show="formVisible"
      :title="isEdit ? '编辑知识库' : '新建知识库'"
      size="lg"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="form.name" placeholder="知识库名称" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="form.description" type="textarea" :rows="2" placeholder="用途说明" />
        </n-form-item>
        <n-form-item label="标签">
          <n-dynamic-tags v-model:value="form.tags" />
        </n-form-item>
        <n-form-item label="Embedding 供应商" path="embeddingProviderId">
          <n-select
            v-model:value="form.embeddingProviderId"
            :options="providerOptions"
            filterable
            placeholder="选择供应商"
            @update:value="onEmbeddingProviderChange"
          />
        </n-form-item>
        <n-form-item label="Embedding 模型" path="embeddingModel">
          <div style="width: 100%">
            <n-select
              v-model:value="form.embeddingModel"
              :options="embeddingModelOptions"
              filterable
              tag
              placeholder="text-embedding-v4 等文本向量模型"
            />
            <div style="margin-top: 4px; font-size: 12px; color: var(--n-text-color-3)">
              仅支持 OpenAI 兼容文本向量；勿选 qwen*-vl-embedding / tongyi-embedding-vision
            </div>
          </div>
        </n-form-item>
        <n-form-item label="分块策略" path="chunkStrategy">
          <div style="width: 100%">
            <n-select v-model:value="form.chunkStrategy" :options="chunkStrategyOptions" />
            <div style="margin-top: 4px; font-size: 12px; color: var(--n-text-color-3)">
              {{ chunkStrategyHint(form.chunkStrategy) }}
            </div>
          </div>
        </n-form-item>
        <n-grid :cols="form.chunkStrategy === 'fixed' ? 2 : 1" :x-gap="12">
          <n-gi>
            <n-form-item label="分块大小">
              <n-input-number v-model:value="form.chunkSize" :min="64" :max="4096" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi v-if="form.chunkStrategy === 'fixed'">
            <n-form-item label="分块重叠">
              <n-input-number v-model:value="form.chunkOverlap" :min="0" :max="1024" style="width: 100%" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">停用</n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { NTag, NSpace, useMessage } from 'naive-ui'
import { CreateOutline, DocumentsOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  createKnowledge,
  deleteKnowledges,
  listProviders,
  pageKnowledges,
  updateKnowledge
} from '@/modules/agent/api'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useUserStore } from '@/store/user'
import KnowledgeDetailPanel from './KnowledgeDetailPanel.vue'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const detailVisible = ref(false)
const detailId = ref(null)
const detailTitle = ref('知识库详情')

const chunkStrategyOptions = [
  { label: '按章节+递归', value: 'section_recursive' },
  { label: '固定窗口', value: 'fixed' }
]
function chunkStrategyHint(v) {
  return v === 'fixed'
    ? '按字符滑窗切分，适合无标题结构的纯文本'
    : '先按章节切分，超长再递归切；每个切片保留章节目录前缀'
}
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]
const FALLBACK_EMBEDDING = [
  { label: 'text-embedding-v4（阿里云）', value: 'text-embedding-v4' },
  { label: 'text-embedding-v3（阿里云）', value: 'text-embedding-v3' },
  { label: 'text-embedding-3-small', value: 'text-embedding-3-small' },
  { label: 'text-embedding-3-large', value: 'text-embedding-3-large' },
  { label: 'bge-m3', value: 'bge-m3' }
]
const VL_EMBED_RE = /vl-embedding|embedding-vision|tongyi-embedding-vision|multimodal-embedding/i
function isTextEmbeddingModel(m) {
  const code = m?.modelCode || ''
  if (VL_EMBED_RE.test(code)) return false
  return !m.modelType || m.modelType === 'embedding'
}
const providers = ref([])
const providerOptions = computed(() =>
  providers.value.map((p) => ({ label: p.name, value: p.id }))
)
const embeddingModelOptions = computed(() => {
  const p = providers.value.find((x) => x.id === form.embeddingProviderId)
  const models = (p?.models || []).filter((m) => m.status !== 0)
  const embeds = models.filter(isTextEmbeddingModel)
  if (!embeds.length) return FALLBACK_EMBEDDING
  return embeds.map((m) => ({
    label: m.displayName || m.modelCode,
    value: m.modelCode
  }))
})

function onEmbeddingProviderChange() {
  form.embeddingModel = ''
}

async function loadProviders() {
  const res = await listProviders()
  providers.value = res.data || []
}

onMounted(loadProviders)

const indexStatusMap = {
  ready: { label: '已就绪', type: 'success' },
  indexing: { label: '索引中', type: 'warning' },
  failed: { label: '有失败', type: 'error' },
  empty: { label: '空库', type: 'default' }
}

const pager = usePagination((q) => pageKnowledges(q), {
  defaults: { name: '', status: null }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteKnowledges, {
  confirm: (n) => `确定删除选中的 ${n} 个知识库吗？文档将一并删除。`,
  positiveText: '删除'
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate,
  openEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    name: '',
    description: '',
    tags: [],
    embeddingProviderId: null,
    embeddingModel: '',
    chunkStrategy: 'section_recursive',
    chunkSize: 512,
    chunkOverlap: 64,
    topK: 5,
    scoreThreshold: 0.35,
    status: 1
  },
  async onSubmit(payload) {
    if (payload.id) {
      await updateKnowledge({ ...payload })
      message.success('更新成功')
    } else {
      const res = await createKnowledge({ ...payload })
      message.success('新建成功')
      loadData()
      if (res.data?.id) openDetail(res.data.id, res.data.name)
      return
    }
    loadData()
  }
})

const formRules = {
  name: { required: true, message: '请输入名称', trigger: 'blur' },
  embeddingProviderId: {
    required: true,
    type: 'number',
    message: '请选择 Embedding 供应商',
    trigger: 'change'
  },
  embeddingModel: { required: true, message: '请选择 Embedding 模型', trigger: 'change' }
}

const columns = computed(() => [
  { type: 'selection', width: 40 },
  { title: 'ID', key: 'id', width: 64 },
  {
    title: '知识库',
    key: 'name',
    width: 180,
    ellipsis: { tooltip: true }
  },
  {
    title: '标签',
    key: 'tags',
    width: 120,
    ellipsis: { tooltip: true },
    render: (row) =>
      h(
        NSpace,
        { size: 4, wrap: false },
        {
          default: () =>
            (row.tags || []).slice(0, 3).map((t) =>
              h(NTag, { bordered: false }, { default: () => t })
            )
        }
      )
  },
  {
    title: '文档',
    key: 'docCount',
    width: 100,
    align: 'right',
    render: (row) => row.docCount || 0
  },
  {
    title: '索引',
    key: 'indexStatus',
    width: 100,
    render: (row) => {
      const m = indexStatusMap[row.indexStatus] || indexStatusMap.empty
      return h(NTag, { type: m.type, bordered: false }, { default: () => m.label })
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) =>
      h(NTag, { type: row.status === 1 ? 'success' : 'default', bordered: false }, {
        default: () => (row.status === 1 ? '启用' : '停用')
      })
  },
  { title: '更新时间', key: 'updateTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 130,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('agent:knowledge:query') &&
            hActionBtn({
              tip: '详情',
              type: 'primary',
              icon: DocumentsOutline,
              disabled: busy(row.id),
              onClick: () => goDetail(row.id)
            }),
          userStore.hasPerm('agent:knowledge:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'info',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () =>
                openEdit(row, (r) => ({
                  id: r.id,
                  name: r.name,
                  description: r.description || '',
                  tags: [...(r.tags || [])],
                  embeddingProviderId: r.embeddingProviderId ?? null,
                  embeddingModel: r.embeddingModel || '',
                  chunkStrategy: r.chunkStrategy || 'section_recursive',
                  chunkSize: r.chunkSize ?? 512,
                  chunkOverlap: r.chunkOverlap ?? 64,
                  topK: r.topK ?? 5,
                  scoreThreshold: r.scoreThreshold ?? 0.35,
                  status: r.status
                }))
            }),
          userStore.hasPerm('agent:knowledge:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除知识库「${row.name}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

function openDetail(id, name) {
  detailId.value = id
  detailTitle.value = name || '知识库详情'
  detailVisible.value = true
}

function goDetail(id) {
  openDetail(id)
}

function onDetailLeave() {
  detailId.value = null
  detailTitle.value = '知识库详情'
  loadData()
}

async function handleDelete(row) {
  await deleteKnowledges([row.id])
  message.success('删除成功')
  loadData()
}
</script>

