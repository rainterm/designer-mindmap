<template>
  <div class="kb-detail-panel">
    <n-spin v-if="!kb" style="width: 100%; padding: 80px 0" />

    <template v-else>
      <PageTable :pager="docPager" :columns="docColumns">
        <template #toolbar-left>
          <n-space>
            <n-input
              v-model:value="docQuery.keyword"
              clearable
              placeholder="搜索标题 / 文件名"
              style="width: 200px"
              @keyup.enter="docPager.handleSearch()"
            />
            <n-select
              v-model:value="docQuery.status"
              :options="docStatusOptions"
              clearable
              placeholder="状态"
              style="width: 120px"
              @update:value="docPager.handleSearch()"
            />
          </n-space>
        </template>
        <template #toolbar-right>
          <n-space>
            <n-button
              v-if="checkedDocKeys.length"
              v-perm="'agent:knowledge:edit'"
              type="error"
              @click="handleBatchRemoveDocs"
            >
              删除选中 {{ checkedDocKeys.length }} 项
            </n-button>
            <n-button v-perm="'agent:knowledge:query'" @click="openRetrieveTest">
              检索测试
            </n-button>
            <n-button v-perm="'agent:knowledge:edit'" @click="openKbEdit">
              编辑配置
            </n-button>
            <n-button v-perm="'agent:knowledge:edit'" type="primary" @click="openDocCreate">
              上传原文件
            </n-button>
          </n-space>
        </template>
      </PageTable>
    </template>

    <Dialog
      v-model:show="kbFormVisible"
      title="编辑知识库配置"
      size="lg"
      :loading="kbSubmitting"
      @confirm="submitKb"
    >
      <n-form ref="kbFormRef" :model="kbForm" :rules="kbRules" label-placement="left" label-width="100">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="kbForm.name" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="kbForm.description" type="textarea" :rows="2" />
        </n-form-item>
        <n-form-item label="标签">
          <n-dynamic-tags v-model:value="kbForm.tags" />
        </n-form-item>
        <n-form-item label="Embedding 供应商" path="embeddingProviderId">
          <n-select
            v-model:value="kbForm.embeddingProviderId"
            :options="providerOptions"
            filterable
            placeholder="选择供应商"
            @update:value="onEmbeddingProviderChange"
          />
        </n-form-item>
        <n-form-item label="Embedding 模型" path="embeddingModel">
          <div style="width: 100%">
            <n-select
              v-model:value="kbForm.embeddingModel"
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
            <n-select v-model:value="kbForm.chunkStrategy" :options="chunkStrategyOptions" />
            <div style="margin-top: 4px; font-size: 12px; color: var(--n-text-color-3)">
              {{ chunkStrategyHint(kbForm.chunkStrategy) }}
            </div>
          </div>
        </n-form-item>
        <n-grid :cols="kbForm.chunkStrategy === 'fixed' ? 2 : 1" :x-gap="12">
          <n-gi>
            <n-form-item label="分块大小">
              <n-input-number v-model:value="kbForm.chunkSize" :min="64" :max="4096" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi v-if="kbForm.chunkStrategy === 'fixed'">
            <n-form-item label="分块重叠">
              <n-input-number v-model:value="kbForm.chunkOverlap" :min="0" :max="1024" style="width: 100%" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-form-item label="状态">
          <n-radio-group v-model:value="kbForm.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">停用</n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </Dialog>

    <Dialog
      v-model:show="docFormVisible"
      :title="docForm.id ? '编辑文档' : '添加文档'"
      size="lg"
      :loading="docSubmitting"
      @confirm="submitDoc"
    >
      <n-form ref="docFormRef" :model="docForm" :rules="docRules" label-placement="left" label-width="90">
        <n-form-item v-if="!docForm.id" label="来源" path="sourceType">
          <n-radio-group v-model:value="docForm.sourceType" @update:value="onSourceTypeChange">
            <n-radio-button value="upload">上传原文件</n-radio-button>
            <n-radio-button value="text">粘贴文本</n-radio-button>
            <n-radio-button value="url">网页 URL</n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="标题" path="title">
          <n-input v-model:value="docForm.title" placeholder="文档标题（可自动取文件名）" />
        </n-form-item>
        <template v-if="!docForm.id && docForm.sourceType === 'upload'">
          <n-form-item label="原文件" path="url">
            <div class="upload-field">
              <n-upload
                :show-file-list="false"
                :max="1"
                accept=".txt,.md,.markdown,.json,.csv,.log,.pdf,.doc,.docx,.html,.htm"
                :disabled="uploading"
                :custom-request="handleDocUpload"
              >
                <n-button :loading="uploading">选择并上传文件</n-button>
              </n-upload>
              <div v-if="docForm.url" class="upload-file-card">
                <div class="upload-file-meta">
                  <div class="upload-file-name" :title="docForm.fileName || '已上传文件'">
                    {{ docForm.fileName || '已上传文件' }}
                  </div>
                  <div class="upload-file-sub">
                    {{ (docForm.fileType || '-').toUpperCase() }} · {{ formatSize(docForm.fileSize) }}
                  </div>
                </div>
                <n-space :wrap="false">
                  <n-button text type="primary" tag="a" :href="docForm.url" target="_blank">
                    打开
                  </n-button>
                  <n-button text type="error" @click="clearUploadedFile">移除</n-button>
                </n-space>
              </div>
              <div class="field-hint">原文件会保存到文件库；文本类文件会自动抽取正文用于检索</div>
            </div>
          </n-form-item>
        </template>
        <n-form-item v-if="docForm.id && docForm.url" label="原文件">
          <div class="upload-file-card">
            <div class="upload-file-meta">
              <div class="upload-file-name">{{ docForm.fileName || '原文件' }}</div>
              <div class="upload-file-sub">
                {{ (docForm.fileType || '-').toUpperCase() }} · {{ formatSize(docForm.fileSize) }}
              </div>
            </div>
            <n-button text type="primary" tag="a" :href="docForm.url" target="_blank">
              下载 / 打开
            </n-button>
          </div>
        </n-form-item>
        <n-form-item v-if="docForm.sourceType === 'url'" label="URL" path="url">
          <n-input v-model:value="docForm.url" placeholder="https://" />
        </n-form-item>
        <n-form-item
          v-if="docForm.sourceType === 'text' || docForm.id || docForm.content"
          label="正文"
          path="content"
        >
          <n-input
            v-model:value="docForm.content"
            type="textarea"
            :rows="8"
            :placeholder="
              docForm.sourceType === 'upload'
                ? '文本类文件会自动填入；也可手动补充正文后重索引'
                : '粘贴文本内容'
            "
            class="mono"
          />
        </n-form-item>
      </n-form>
    </Dialog>

    <n-drawer v-model:show="previewVisible" :width="640" placement="right">
      <n-drawer-content :title="previewDoc?.title || '文档预览'" closable>
        <div v-if="previewDoc" class="preview-wrap">
          <div class="preview-meta">
            <n-tag :bordered="false">{{ sourceLabel(previewDoc.sourceType) }}</n-tag>
            <n-tag
              :bordered="false"
              :type="docStatusMap[previewDoc.status]?.type || 'default'"
            >
              {{ docStatusMap[previewDoc.status]?.label || previewDoc.status }}
            </n-tag>
            <span>{{ previewDoc.fileName || previewDoc.url || previewDoc.fileType }}</span>
          </div>
          <div v-if="previewDoc.url" class="preview-file">
            <span>原文件</span>
            <a :href="previewDoc.url" target="_blank" rel="noopener">{{ previewDoc.fileName || '下载 / 打开' }}</a>
          </div>
          <p v-if="previewDoc.errorMsg" class="preview-error">{{ previewDoc.errorMsg }}</p>
          <div class="preview-body">
            <MdPreview
              v-if="previewDoc.content"
              :id="`kb-doc-md-${previewDoc.id}`"
              :model-value="previewDoc.content"
              theme="light"
              preview-theme="default"
              class="kb-md"
            />
            <div v-else class="preview-empty">（无正文预览；可下载上方原文件）</div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>

    <n-drawer v-model:show="chunkVisible" :width="640" placement="right">
      <n-drawer-content :title="chunkDocTitle" closable>
        <n-spin :show="chunkLoading">
          <div v-if="!chunkLoading && !chunks.length" class="chunk-empty">
            暂无分块；请先完成索引后再查看
          </div>
          <div v-else class="chunk-list">
            <div v-for="c in chunks" :key="c.id ?? c.chunkIndex" class="chunk-item">
              <div class="chunk-item-head">
                <div class="chunk-item-meta">
                  <n-tag :bordered="false">#{{ c.chunkIndex }}</n-tag>
                  <span>{{ c.charCount ?? (c.content || '').length }} 字</span>
                </div>
                <n-button
                  v-if="userStore.hasPerm('agent:knowledge:edit')"
                  quaternary
                  type="primary"
                  @click="openChunkEdit(c)"
                >
                  编辑
                </n-button>
              </div>
              <div class="chunk-item-body">
                <MdPreview
                  v-if="c.content"
                  :id="`kb-chunk-md-${c.id ?? c.chunkIndex}`"
                  :model-value="c.content"
                  theme="light"
                  preview-theme="default"
                  class="kb-md"
                />
              </div>
            </div>
          </div>
        </n-spin>
      </n-drawer-content>
    </n-drawer>

    <Dialog
      v-model:show="chunkEditVisible"
      :title="`编辑分块 #${chunkEditForm.chunkIndex}`"
      size="lg"
      :loading="chunkEditSubmitting"
      @confirm="submitChunkEdit"
    >
      <n-form label-placement="left" label-width="72">
        <n-form-item label="内容" required>
          <n-input
            v-model:value="chunkEditForm.content"
            type="textarea"
            :rows="14"
            placeholder="分块文本"
            class="mono"
          />
        </n-form-item>
        <div class="field-hint">保存后将按当前 Embedding 模型重新向量化该分块，不影响其他分块与文档正文</div>
      </n-form>
    </Dialog>

    <Dialog
      v-model:show="retrieveVisible"
      title="检索测试"
      size="lg"
      :loading="retrieveSubmitting"
      confirm-text="检索"
      @confirm="runRetrieveTest"
      @after-leave="onRetrieveLeave"
    >
      <n-form label-placement="top">
        <n-form-item label="查询" required>
          <n-input
            v-model:value="retrieveForm.query"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            placeholder="输入测试问题，验证本库向量召回效果"
          />
        </n-form-item>
        <n-grid :cols="2" :x-gap="12">
          <n-gi>
            <n-form-item label="Rerank 供应商">
              <n-select
                v-model:value="retrieveForm.rerankProviderId"
                :options="providerOptions"
                placeholder="可选，不填则按向量分排序"
                clearable
                @update:value="onRetrieveRerankProviderChange"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="Rerank 模型">
              <n-select
                v-model:value="retrieveForm.rerankModel"
                :options="retrieveRerankModelOptions"
                placeholder="重排序模型"
                clearable
                filterable
                tag
                :disabled="!retrieveForm.rerankProviderId"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="最终 Top K">
              <n-input-number v-model:value="retrieveForm.topK" :min="1" :max="50" style="width: 100%" />
              <div class="field-hint">返回的分块数；开启 Rerank 时为重排后截断数</div>
            </n-form-item>
          </n-gi>
          <n-gi v-if="retrieveForm.rerankProviderId">
            <n-form-item label="召回候选数">
              <n-input-number
                v-model:value="retrieveForm.recallTopK"
                :min="1"
                :max="50"
                clearable
                placeholder="留空 = 自动"
                style="width: 100%"
              />
              <div class="field-hint">
                向量先召回的候选条数，再交给 Rerank。留空表示自动（约 最终 Top K × 3，当前约
                {{ retrieveAutoRecallTopK }}）
              </div>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="相似度阈值（向量召回）">
              <n-input-number
                v-model:value="retrieveForm.scoreThreshold"
                :min="0"
                :max="1"
                :step="0.01"
                style="width: 100%"
              />
              <div class="field-hint">仅过滤 Embedding 召回；低于该值的分块不会进入候选</div>
            </n-form-item>
          </n-gi>
        </n-grid>
      </n-form>

      <div v-if="retrieveResult" class="retrieve-result">
        <div class="retrieve-meta">
          命中 {{ retrieveResult.hitCount }} 条
          <span v-if="retrieveResult.durationMs != null"> · {{ retrieveResult.durationMs }}ms</span>
          <span v-if="retrieveResult.rerankEnabled">
            · 已启用 Rerank（召回 {{ retrieveResult.recallTopK }} → Top {{ retrieveResult.topK }}）
          </span>
        </div>
        <div v-if="!retrieveResult.hits?.length" class="chunk-empty">未命中分块，可降低阈值或检查索引是否就绪</div>
        <div v-else class="chunk-list">
          <div v-for="(h, i) in retrieveResult.hits" :key="`${h.docId}-${h.chunkIndex}-${i}`" class="chunk-item">
            <div class="chunk-item-head">
              <div class="chunk-item-meta">
                <span>#{{ i + 1 }}</span>
                <span>doc-{{ h.docId }}</span>
                <span>分块 {{ h.chunkIndex }}</span>
                <n-tag :bordered="false">score {{ formatScore(h.score) }}</n-tag>
              </div>
            </div>
            <div class="chunk-item-body">
              <MdPreview
                v-if="h.content"
                :id="`kb-retrieve-md-${h.docId}-${h.chunkIndex}-${i}`"
                :model-value="h.content"
                theme="light"
                preview-theme="default"
                class="kb-md"
              />
            </div>
          </div>
        </div>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, onMounted, onUnmounted, ref, watch } from 'vue'
import { NProgress, NTag, NTooltip, useDialog, useMessage } from 'naive-ui'
import {
  CreateOutline,
  DownloadOutline,
  EyeOutline,
  RefreshOutline,
  TrashOutline
} from '@vicons/ionicons5'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import { uploadFile } from '@/modules/system/api/file'
import {
  createKnowledgeDoc,
  deleteKnowledgeDocs,
  getKnowledge,
  getKnowledgeDoc,
  listKnowledgeDocChunks,
  listProviders,
  pageKnowledgeDocs,
  reindexKnowledgeDocStream,
  retrieveTestKnowledge,
  updateKnowledge,
  updateKnowledgeDoc,
  updateKnowledgeDocChunk
} from '@/modules/agent/api'
import { useActionLock } from '@/composables/useActionLock'
import { useFormDialog } from '@/composables/useFormDialog'
import { usePagination } from '@/composables/usePagination'
import { useUserStore } from '@/store/user'

const props = defineProps({
  knowledgeId: { type: [String, Number], required: true }
})
const emit = defineEmits(['close', 'title-change', 'changed'])

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const kbId = computed(() => {
  const n = Number(props.knowledgeId)
  return Number.isFinite(n) && n > 0 ? n : null
})

const kb = ref(null)
const previewVisible = ref(false)
const previewDoc = ref(null)
const uploading = ref(false)
const chunkVisible = ref(false)
const chunkLoading = ref(false)
const chunkDoc = ref(null)
const chunks = ref([])
const chunkEditVisible = ref(false)
const chunkEditSubmitting = ref(false)
const chunkEditForm = ref({ chunkIndex: 0, content: '' })
const chunkDocTitle = computed(() =>
  chunkDoc.value ? `分块 · ${chunkDoc.value.title || chunkDoc.value.fileName || '文档'}` : '分块'
)

const retrieveVisible = ref(false)
const retrieveSubmitting = ref(false)
const retrieveResult = ref(null)
const retrieveForm = ref({
  query: '',
  topK: 5,
  recallTopK: null,
  scoreThreshold: 0.35,
  rerankProviderId: null,
  rerankModel: ''
})

function formatScore(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(3) : '—'
}

function openRetrieveTest() {
  retrieveForm.value = {
    query: '',
    topK: kb.value?.topK || 5,
    recallTopK: null,
    scoreThreshold: kb.value?.scoreThreshold ?? 0.35,
    rerankProviderId: null,
    rerankModel: ''
  }
  retrieveResult.value = null
  retrieveVisible.value = true
}

function onRetrieveLeave() {
  retrieveResult.value = null
  retrieveSubmitting.value = false
}

function onRetrieveRerankProviderChange() {
  retrieveForm.value.rerankModel = ''
}

const retrieveAutoRecallTopK = computed(() => {
  const k = Math.max(1, Math.min(50, Number(retrieveForm.value.topK) || 5))
  return Math.min(50, Math.max(k, k * 3))
})

async function runRetrieveTest() {
  const q = String(retrieveForm.value.query || '').trim()
  if (!q) {
    message.warning('请输入测试查询')
    return false
  }
  if (!kbId.value) return false
  if (retrieveForm.value.rerankProviderId && !String(retrieveForm.value.rerankModel || '').trim()) {
    message.warning('请选择 Rerank 模型，或不选供应商')
    return false
  }
  retrieveSubmitting.value = true
  try {
    const res = await retrieveTestKnowledge(kbId.value, {
      query: q,
      topK: retrieveForm.value.topK,
      recallTopK: retrieveForm.value.rerankProviderId && retrieveForm.value.recallTopK > 0
        ? retrieveForm.value.recallTopK
        : undefined,
      scoreThreshold: retrieveForm.value.scoreThreshold,
      rerankProviderId: retrieveForm.value.rerankProviderId || undefined,
      rerankModel: String(retrieveForm.value.rerankModel || '').trim() || undefined
    })
    retrieveResult.value = res.data || null
    return false
  } catch (e) {
    message.error(e?.message || '检索失败')
    return false
  } finally {
    retrieveSubmitting.value = false
  }
}

const docPager = usePagination(
  (q) =>
    pageKnowledgeDocs({
      ...q,
      knowledgeId: kbId.value
    }),
  {
    defaults: { keyword: '', status: null },
    pageSize: 10,
    immediate: false
  }
)
const {
  query: docQuery,
  rows: docs,
  loadData: reloadDocs,
  checkedRowKeys: checkedDocKeys
} = docPager

const TEXT_EXTS = new Set([
  'txt',
  'md',
  'markdown',
  'json',
  'csv',
  'log',
  'xml',
  'yml',
  'yaml',
  'html',
  'htm',
  'js',
  'ts',
  'java',
  'py',
  'go',
  'sql',
  'css'
])

const chunkStrategyOptions = [
  { label: '按章节+递归', value: 'section_recursive' },
  { label: '固定窗口', value: 'fixed' }
]
function chunkStrategyHint(v) {
  return v === 'fixed'
    ? '按字符滑窗切分，适合无标题结构的纯文本'
    : '先按章节切分，超长再递归切；每个切片保留章节目录前缀'
}
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
  const p = providers.value.find((x) => x.id === kbForm.embeddingProviderId)
  const models = (p?.models || []).filter((m) => m.status !== 0)
  const embeds = models.filter(isTextEmbeddingModel)
  if (!embeds.length) return FALLBACK_EMBEDDING
  return embeds.map((m) => ({
    label: m.displayName || m.modelCode,
    value: m.modelCode
  }))
})
function isRerankModel(m) {
  return m?.modelType === 'rerank'
}
const retrieveRerankModelOptions = computed(() => {
  const p = providers.value.find((x) => x.id === retrieveForm.value.rerankProviderId)
  return (p?.models || [])
    .filter((m) => m.status !== 0 && isRerankModel(m))
    .map((m) => ({
      label: m.displayName || m.modelCode,
      value: m.modelCode
    }))
})
function onEmbeddingProviderChange() {
  kbForm.embeddingModel = ''
}

async function loadProviders() {
  const res = await listProviders()
  providers.value = res.data || []
}
const docStatusOptions = [
  { label: '待处理', value: 'pending' },
  { label: '索引中', value: 'indexing' },
  { label: '已就绪', value: 'ready' },
  { label: '失败', value: 'failed' }
]
const docStatusMap = {
  pending: { label: '待处理', type: 'default' },
  indexing: { label: '索引中', type: 'warning' },
  ready: { label: '已就绪', type: 'success' },
  failed: { label: '失败', type: 'error' }
}

function sourceLabel(t) {
  return { upload: '上传', text: '文本', url: 'URL' }[t] || t
}
function formatSize(n) {
  if (!n) return '-'
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}

async function loadKb() {
  const id = kbId.value
  if (!id) {
    message.error('缺少知识库 ID')
    emit('close')
    return
  }
  const res = await getKnowledge(id)
  if (!res.data) {
    message.error('知识库不存在')
    emit('close')
    return
  }
  kb.value = res.data
  emit('title-change', res.data.name || '知识库详情')
}

async function refreshAll() {
  await loadKb()
  if (!kbId.value) return
  checkedDocKeys.value = []
  await reloadDocs()
}

const {
  visible: kbFormVisible,
  submitting: kbSubmitting,
  formRef: kbFormRef,
  form: kbForm,
  openEdit: openKbEditBase,
  submit: submitKb
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
    await updateKnowledge({ ...payload })
    message.success('配置已保存')
    await loadKb()
    emit('changed')
  }
})

const kbRules = {
  name: { required: true, message: '请输入名称', trigger: 'blur' },
  embeddingProviderId: {
    required: true,
    type: 'number',
    message: '请选择 Embedding 供应商',
    trigger: 'change'
  },
  embeddingModel: { required: true, message: '请选择 Embedding 模型', trigger: 'change' }
}

function openKbEdit() {
  if (!kb.value) return
  openKbEditBase(kb.value, (r) => ({
    id: r.id,
    name: r.name,
    description: r.description || '',
    tags: [...(r.tags || [])],
    embeddingProviderId: r.embeddingProviderId ?? null,
    embeddingModel: r.embeddingModel || '',
    chunkStrategy: r.chunkStrategy || 'section_recursive',
    chunkSize: r.chunkSize,
    chunkOverlap: r.chunkOverlap,
    topK: r.topK,
    scoreThreshold: r.scoreThreshold,
    status: r.status
  }))
}

const {
  visible: docFormVisible,
  submitting: docSubmitting,
  formRef: docFormRef,
  form: docForm,
  openCreate: openDocCreateBase,
  openEdit: openDocEditBase,
  submit: submitDoc
} = useFormDialog({
  defaults: {
    id: null,
    knowledgeId: null,
    title: '',
    sourceType: 'upload',
    fileName: '',
    fileType: '',
    fileSize: 0,
    url: '',
    content: ''
  },
  async onSubmit(payload) {
    let docId = payload.id
    const hasContent = !!(payload.content && String(payload.content).trim())
    if (payload.id) {
      await updateKnowledgeDoc({ ...payload })
      message.success('文档已更新')
    } else {
      const res = await createKnowledgeDoc({
        ...payload,
        knowledgeId: kbId.value
      })
      docId = res.data?.id
      message.success(payload.sourceType === 'upload' ? '原文件已入库' : '文档已添加')
    }
    await refreshAll()
    emit('changed')
    if (docId && hasContent) {
      await runIndexStream(docId)
      await loadKb()
      emit('changed')
    }
  }
})

const docRules = computed(() => {
  const rules = {
    sourceType: { required: true, message: '请选择来源', trigger: 'change' }
  }
  if (!docForm.id && docForm.sourceType === 'upload') {
    rules.url = { required: true, message: '请上传原文件', trigger: 'change' }
  }
  if (!docForm.id && docForm.sourceType === 'url') {
    rules.url = { required: true, message: '请输入 URL', trigger: 'blur' }
  }
  if (!docForm.id && docForm.sourceType === 'text') {
    rules.title = { required: true, message: '请输入标题', trigger: 'blur' }
    rules.content = { required: true, message: '请输入正文', trigger: 'blur' }
  }
  return rules
})

function fileExt(name) {
  const n = String(name || '')
  const i = n.lastIndexOf('.')
  return i >= 0 ? n.slice(i + 1).toLowerCase() : ''
}

function isTextFile(file) {
  const ext = fileExt(file?.name)
  if (TEXT_EXTS.has(ext)) return true
  const type = String(file?.type || '')
  return type.startsWith('text/') || type === 'application/json'
}

async function readTextContent(file) {
  if (!isTextFile(file) || file.size > 2 * 1024 * 1024) return ''
  try {
    return await file.text()
  } catch {
    return ''
  }
}

function clearUploadedFile() {
  docForm.url = ''
  docForm.fileName = ''
  docForm.fileType = ''
  docForm.fileSize = 0
  if (!docForm.id) docForm.content = ''
}

function onSourceTypeChange() {
  if (!docForm.id) {
    clearUploadedFile()
    docForm.content = ''
  }
}

async function handleDocUpload({ file, onFinish, onError }) {
  const raw = file?.file
  if (!raw) {
    onError?.()
    return
  }
  uploading.value = true
  try {
    const res = await uploadFile(raw, 'agent-knowledge')
    const data = res.data || {}
    const url = data.url || ''
    if (!url) throw new Error('上传失败：未返回地址')
    const name = data.name || raw.name || '未命名文件'
    const ext = fileExt(name)
    const text = await readTextContent(raw)
    docForm.url = url
    docForm.fileName = name
    docForm.fileType = ext || 'bin'
    docForm.fileSize = data.fileSize ?? raw.size ?? 0
    if (!docForm.title?.trim()) docForm.title = name.replace(/\.[^.]+$/, '') || name
    if (text) docForm.content = text
    message.success('原文件已上传')
    onFinish?.()
  } catch (e) {
    message.error(e?.message || '上传失败')
    onError?.()
  } finally {
    uploading.value = false
  }
}

function openDocCreate() {
  openDocCreateBase({
    knowledgeId: kbId.value,
    sourceType: 'upload',
    title: '',
    fileName: '',
    fileType: '',
    fileSize: 0,
    url: '',
    content: ''
  })
}

function openOriginal(row) {
  if (!row?.url) {
    message.warning('该文档没有原文件')
    return
  }
  window.open(row.url, '_blank', 'noopener')
}

function openDocEdit(row) {
  openDocEditBase(row, (r) => ({
    id: r.id,
    knowledgeId: r.knowledgeId,
    title: r.title,
    sourceType: r.sourceType,
    fileName: r.fileName,
    fileType: r.fileType,
    fileSize: r.fileSize,
    url: r.url || '',
    content: r.content || ''
  }))
}

async function openPreview(row) {
  const res = await getKnowledgeDoc(row.id)
  previewDoc.value = res.data
  previewVisible.value = true
}

async function openChunks(row) {
  chunkDoc.value = row
  chunkVisible.value = true
  chunkLoading.value = true
  chunks.value = []
  try {
    const res = await listKnowledgeDocChunks(row.id)
    chunks.value = res.data || []
  } catch (e) {
    message.error(e?.message || '加载分块失败')
  } finally {
    chunkLoading.value = false
  }
}

function openChunkEdit(c) {
  chunkEditForm.value = {
    chunkIndex: c.chunkIndex,
    content: c.content || ''
  }
  chunkEditVisible.value = true
}

async function submitChunkEdit() {
  const docId = chunkDoc.value?.id
  const { chunkIndex, content } = chunkEditForm.value
  if (docId == null) return
  if (!content || !String(content).trim()) {
    message.warning('分块内容不能为空')
    return
  }
  chunkEditSubmitting.value = true
  try {
    const res = await updateKnowledgeDocChunk(docId, chunkIndex, {
      content: String(content).trim()
    })
    const updated = res.data
    const idx = chunks.value.findIndex((x) => x.chunkIndex === chunkIndex)
    if (idx >= 0) {
      chunks.value[idx] = { ...chunks.value[idx], ...updated }
    }
    chunkEditVisible.value = false
    message.success('分块已更新并重新向量化')
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    chunkEditSubmitting.value = false
  }
}

const indexAbortMap = new Map()

function patchDocRow(partial) {
  if (!partial?.id) return
  const idx = docs.value.findIndex((d) => d.id === partial.id)
  if (idx < 0) return
  const cur = docs.value[idx]
  docs.value[idx] = {
    ...cur,
    status: partial.status ?? cur.status,
    indexProgress: partial.indexProgress ?? cur.indexProgress,
    chunkCount: partial.chunkCount ?? cur.chunkCount,
    charCount: partial.charCount ?? cur.charCount,
    errorMsg: partial.errorMsg ?? cur.errorMsg,
    updateTime: partial.updateTime != null ? partial.updateTime : cur.updateTime
  }
}

async function runIndexStream(docId) {
  if (!docId) return
  const prev = indexAbortMap.get(docId)
  prev?.abort()
  const ac = new AbortController()
  indexAbortMap.set(docId, ac)
  patchDocRow({ id: docId, status: 'indexing', indexProgress: 0, errorMsg: '连接中…' })
  try {
    await reindexKnowledgeDocStream(
      docId,
      {
        onProgress: (data) => patchDocRow(data),
        onDone: (data) => {
          patchDocRow(data)
          if (data?.status === 'ready') {
            message.success('索引完成')
          } else if (data?.status === 'pending') {
            message.warning(data?.errorMsg || '正文为空，未能建索引')
          } else if (data?.status === 'failed') {
            message.error(data?.errorMsg || '索引失败')
          }
        },
        onError: (msg, data) => {
          if (data) patchDocRow(data)
          else patchDocRow({ id: docId, status: 'failed', indexProgress: 0, errorMsg: msg })
          message.error(msg || '索引失败')
        }
      },
      ac.signal
    )
  } catch (e) {
    if (e?.name === 'AbortError') return
    patchDocRow({
      id: docId,
      status: 'failed',
      indexProgress: 0,
      errorMsg: e?.message || '索引失败'
    })
    message.error(e?.message || '索引失败')
  } finally {
    if (indexAbortMap.get(docId) === ac) {
      indexAbortMap.delete(docId)
    }
  }
}

async function handleReindex(row) {
  await runIndexStream(row.id)
  await loadKb()
}

async function handleDeleteDoc(row) {
  await deleteKnowledgeDocs([row.id])
  message.success('已删除')
  await refreshAll()
  emit('changed')
}

function handleBatchRemoveDocs() {
  dialog.warning({
    title: '批量删除',
    content: `确定删除选中的 ${checkedDocKeys.value.length} 个文档吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteKnowledgeDocs(checkedDocKeys.value)
      message.success('已删除')
      await refreshAll()
      emit('changed')
    }
  })
}

const docColumns = computed(() => [
  { type: 'selection', width: 44 },
  {
    title: '文档',
    key: 'title',
    width: 160,
    ellipsis: { tooltip: true },
    render: (row) =>
      h('div', { style: 'min-width:0' }, [
        h(
          'div',
          {
            style: 'font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap',
            title: row.title
          },
          row.title
        ),
        h(
          'div',
          {
            style:
              'font-size:12px;color:var(--color-text-muted,#87918b);margin-top:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap',
            title: row.fileName || row.url || sourceLabel(row.sourceType)
          },
          row.fileName || row.url || sourceLabel(row.sourceType)
        )
      ])
  },
  {
    title: '来源',
    key: 'sourceType',
    width: 120,
    render: (row) => sourceLabel(row.sourceType)
  },
  {
    title: '类型',
    key: 'fileType',
    width: 120,
    render: (row) => (row.fileType || '-').toUpperCase()
  },
  {
    title: '大小',
    key: 'fileSize',
    width: 120,
    render: (row) => formatSize(row.fileSize)
  },
  {
    title: '分块',
    key: 'chunkCount',
    width: 120,
    render: (row) => {
      const n = Number(row.chunkCount) || 0
      const disabled = busy(row.id) || row.status === 'indexing'
      return h(
        'button',
        {
          type: 'button',
          class: 'chunk-count-link',
          disabled,
          title: n ? '查看分块' : '暂无分块',
          onClick: (e) => {
            e.stopPropagation()
            if (!disabled) openChunks(row)
          }
        },
        String(n)
      )
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: (row) => {
      const m = docStatusMap[row.status] || { label: row.status, type: 'default' }
      if (row.status === 'indexing') {
        const pct = Math.max(0, Math.min(100, Number(row.indexProgress) || 0))
        return h('div', [
          h(
            'div',
            { style: 'display:flex;align-items:center;justify-content:space-between;gap:6px;margin-bottom:4px' },
            [
              h(NTag, { type: 'warning', bordered: false, size: 'small' }, { default: () => '索引中' }),
              h('span', { style: 'font-size:12px;color:var(--color-text-muted)' }, `${pct}%`)
            ]
          ),
          h(NProgress, {
            type: 'line',
            percentage: pct,
            showIndicator: false,
            processing: true,
            height: 6,
            borderRadius: 3
          })
        ])
      }
      return h(NTag, { type: m.type, bordered: false }, { default: () => m.label })
    }
  },
  {
    title: '失败原因',
    key: 'errorMsg',
    width: 140,
    render: (row) => {
      if (row.status !== 'failed' || !row.errorMsg) {
        return h('span', { style: 'color:var(--color-text-muted)' }, '-')
      }
      return h(
        NTooltip,
        {
          contentStyle: {
            color: '#fff',
            maxWidth: '360px',
            whiteSpace: 'normal',
            wordBreak: 'break-word',
            lineHeight: '1.45'
          }
        },
        {
          trigger: () =>
            h(
              'span',
              {
                style:
                  'color:var(--color-error,#d03050);font-size:12px;display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap'
              },
              row.errorMsg
            ),
          default: () => row.errorMsg
        }
      )
    }
  },
  { title: '更新时间', key: 'updateTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 210,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          row.url &&
            hActionBtn({
              tip: '原文件',
              icon: DownloadOutline,
              disabled: busy(row.id),
              onClick: () => openOriginal(row)
            }),
          hActionBtn({
            tip: '预览',
            icon: EyeOutline,
            disabled: busy(row.id),
            onClick: () => openPreview(row)
          }),
          userStore.hasPerm('agent:knowledge:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () => openDocEdit(row)
            }),
          userStore.hasPerm('agent:knowledge:edit') &&
            row.status !== 'indexing' &&
            hActionBtn({
              tip: '重索引',
              type: 'warning',
              icon: RefreshOutline,
              loading: locked(row.id, 'reindex'),
              disabled: busy(row.id),
              onClick: () => run(row.id, 'reindex', () => handleReindex(row))
            }),
          userStore.hasPerm('agent:knowledge:edit') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除文档「${row.title}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDeleteDoc(row))
            })
        ].filter(Boolean)
      )
  }
])

onMounted(async () => {
  await loadProviders()
  await refreshAll()
})

watch(kbId, async (id, prev) => {
  if (id && id !== prev) {
    kb.value = null
    await refreshAll()
  }
})

onUnmounted(() => {
  for (const ac of indexAbortMap.values()) {
    ac.abort()
  }
  indexAbortMap.clear()
})
</script>

<style scoped>
.kb-detail-panel {
  min-height: 0;
}
.field-hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-text-muted);
}
.upload-field {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}
.upload-field .field-hint {
  margin-top: 0;
}
.upload-file-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body);
}
.upload-file-meta {
  min-width: 0;
  flex: 1;
}
.upload-file-name {
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-file-sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.mono :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}
.preview-file {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.preview-file a {
  color: var(--color-primary);
  text-decoration: none;
}
.preview-file a:hover {
  text-decoration: underline;
}
.preview-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: var(--color-text-muted);
}
.preview-error {
  margin: 0;
  padding: 8px 10px;
  border-radius: var(--radius);
  background: color-mix(in srgb, #d03050 10%, transparent);
  color: #d03050;
  font-size: 13px;
}
.preview-body {
  margin: 0;
  padding: 12px;
  border-radius: var(--radius);
  background: var(--color-body);
  max-height: calc(100vh - 180px);
  overflow: auto;
}
.preview-empty {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.55;
}
.kb-md {
  background: transparent !important;
}
.kb-md :deep(.md-editor-preview-wrapper) {
  padding: 0;
}
.kb-md :deep(.md-editor-preview) {
  font-size: 13px;
  line-height: 1.55;
  color: var(--color-text);
  word-break: break-word;
}
.kb-md :deep(.md-editor-preview > :first-child) {
  margin-top: 0;
}
.kb-md :deep(.md-editor-preview > :last-child) {
  margin-bottom: 0;
}
.kb-md :deep(pre) {
  margin: 8px 0;
  border-radius: var(--radius);
}
.kb-md :deep(code) {
  font-size: 12px;
}
.kb-md :deep(p) {
  margin: 0.4em 0;
}
.kb-md :deep(ul),
.kb-md :deep(ol) {
  margin: 0.4em 0;
  padding-left: 1.4em;
}
.kb-md :deep(h1),
.kb-md :deep(h2),
.kb-md :deep(h3),
.kb-md :deep(h4) {
  margin: 0.8em 0 0.4em;
  line-height: 1.35;
}
.kb-md :deep(img) {
  max-width: 100%;
}
:deep(.chunk-count-link) {
  padding: 0;
  border: 0;
  background: none;
  color: var(--color-primary);
  font: inherit;
  cursor: pointer;
}
:deep(.chunk-count-link:hover:not(:disabled)) {
  opacity: 0.85;
}
:deep(.chunk-count-link:disabled) {
  color: var(--color-text-muted);
  cursor: not-allowed;
}
.chunk-empty {
  padding: 48px 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
}
.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chunk-item {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body);
}
.chunk-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.chunk-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.chunk-item-body {
  margin: 0;
  max-height: 280px;
  overflow: auto;
}
.retrieve-result {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}
.retrieve-meta {
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>
