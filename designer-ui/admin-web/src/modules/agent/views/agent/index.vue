<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="名称">
          <n-input v-model:value="query.name" placeholder="名称 / 编码" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'agent:agent:add'" type="primary" @click="goEdit()">
            新建智能体
          </n-button>
          <n-button
            v-perm="'agent:agent:remove'"
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
      v-model:show="editVisible"
      :title="editTitle"
      size="full"
      :fullscreenable="false"
      :draggable="false"
      :loading="editSaving"
      confirm-text="保存草稿"
      @confirm="handleEditConfirm"
      @after-leave="onEditLeave"
    >
      <AgentEditForm
        v-if="editVisible"
        ref="editFormRef"
        :agent-id="editAgentId"
        @saved="onEditSaved"
        @missing="editVisible = false"
      />
    </Dialog>

    <OpenInvokeDialog
      v-model:show="invokeVisible"
      type="agent"
      :row="invokeRow"
      @updated="loadData"
    />
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { NAvatar, NTag, useMessage } from 'naive-ui'
import { CreateOutline, LinkOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import { deleteAgents, pageAgents } from '@/modules/agent/api'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useUserStore } from '@/store/user'
import AgentEditForm from './AgentEditForm.vue'
import OpenInvokeDialog from './OpenInvokeDialog.vue'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const editVisible = ref(false)
const editAgentId = ref(null)
const editFormRef = ref(null)
const editSaving = ref(false)
const editTitle = computed(() => (editAgentId.value ? '编辑智能体' : '新建智能体'))
const invokeVisible = ref(false)
const invokeRow = ref(null)

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const AVATAR_COLORS = ['#1f6f54', '#2a6f97', '#6b4f3a', '#5a4a7a', '#3d6b5a', '#7a5c3a']

const pager = usePagination((q) => pageAgents(q), {
  defaults: { name: '', status: null }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteAgents, {
  confirm: (n) => `确定删除选中的 ${n} 个智能体吗？`,
  positiveText: '删除'
})

function initials(name) {
  return String(name || '?').trim().slice(0, 1).toUpperCase()
}

/** 名称 + 版本/草稿标识 */
function renderName(row) {
  const tags = []
  if (row.version) {
    tags.push(h(NTag, { size: 'small', bordered: false, type: 'success' }, { default: () => `v${row.version}` }))
  }
  if (row.hasUnpublished) {
    tags.push(h(NTag, { size: 'small', bordered: false, type: 'warning' }, { default: () => '草稿' }))
  }
  if (!tags.length) return row.name
  return h('div', { style: 'display:flex;align-items:center;gap:6px;min-width:0' }, [
    h('span', { style: 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap' }, row.name),
    ...tags
  ])
}

function avatarColor(name) {
  let hval = 0
  for (const c of String(name || '')) hval = (hval * 31 + c.charCodeAt(0)) >>> 0
  return AVATAR_COLORS[hval % AVATAR_COLORS.length]
}

function renderAvatar(row) {
  if (row.avatar) {
    return h(NAvatar, { size: 32, src: row.avatar, style: 'border-radius: 8px' })
  }
  return h(
    NAvatar,
    {
      size: 32,
      style: {
        borderRadius: '8px',
        background: avatarColor(row.name),
        color: '#fff',
        fontWeight: 700,
        fontSize: '14px'
      }
    },
    { default: () => initials(row.name) }
  )
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  {
    title: '图标',
    key: 'avatar',
    width: 64,
    render: (row) => renderAvatar(row)
  },
  { title: '名称', key: 'name', width: 140, ellipsis: { tooltip: true }, render: (row) => renderName(row) },
  {
    title: '描述',
    key: 'remark',
    width: 160,
    ellipsis: { tooltip: true },
    render: (row) => row.remark || '-'
  },
  {
    title: '供应商',
    key: 'providerName',
    width: 120,
    ellipsis: { tooltip: true },
    render: (row) => row.providerName || '-'
  },
  {
    title: '模型',
    key: 'modelCode',
    width: 140,
    ellipsis: { tooltip: true },
    render: (row) => row.modelCode || '-'
  },
  {
    title: '能力',
    key: 'caps',
    width: 160,
    ellipsis: { tooltip: true },
    render: (row) => {
      const parts = []
      if ((row.toolIds || []).length) parts.push(`工具 ${row.toolIds.length}`)
      if ((row.mcpIds || []).length) parts.push(`MCP ${row.mcpIds.length}`)
      if ((row.skillIds || []).length) parts.push(`Skill ${row.skillIds.length}`)
      if ((row.workflowIds || []).length) parts.push(`工作流 ${row.workflowIds.length}`)
      if ((row.knowledgeIds || []).length) parts.push(`知识 ${row.knowledgeIds.length}`)
      return parts.length ? parts.join(' · ') : '-'
    }
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
    width: 132,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('agent:agent:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () => goEdit(row.id)
            }),
          hActionBtn({
            tip: '调用',
            type: 'primary',
            icon: LinkOutline,
            disabled: busy(row.id),
            onClick: () => openInvoke(row)
          }),
          userStore.hasPerm('agent:agent:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除智能体「${row.name}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

function openInvoke(row) {
  invokeRow.value = row
  invokeVisible.value = true
}

function goEdit(id) {
  editAgentId.value = id || null
  editVisible.value = true
}

async function handleEditConfirm() {
  editSaving.value = true
  try {
    await editFormRef.value?.save()
  } finally {
    editSaving.value = false
  }
}

function onEditSaved() {
  const id = editFormRef.value?.form?.id
  if (id) editAgentId.value = id
  loadData()
}

function onEditLeave() {
  editAgentId.value = null
  editFormRef.value?.resetForm?.()
}

async function handleDelete(row) {
  await deleteAgents([row.id])
  message.success('删除成功')
  loadData()
}
</script>
