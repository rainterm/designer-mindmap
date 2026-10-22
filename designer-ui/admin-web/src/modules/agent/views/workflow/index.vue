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
          <n-button v-perm="'agent:workflow:add'" type="primary" @click="openForm()">
            新增工作流
          </n-button>
          <n-button
            v-perm="'agent:workflow:remove'"
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
      v-model:show="formVisible"
      :title="form.id ? '编辑工作流' : '新建工作流'"
      :loading="formSubmitting"
      @confirm="submitForm"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="88">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="form.name" placeholder="工作流名称" maxlength="100" />
        </n-form-item>
        <n-form-item label="状态">
          <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="form.description" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>

    <OpenInvokeDialog v-model:show="invokeVisible" type="workflow" :row="invokeRow" />
  </div>
</template>

<script setup>
import { computed, h, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NTag, useMessage } from 'naive-ui'
import {
  CreateOutline,
  GitNetworkOutline,
  LinkOutline,
  TrashOutline
} from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  createWorkflow,
  deleteWorkflows,
  pageWorkflows,
  updateWorkflow
} from '@/modules/agent/api'
import { defaultWorkflowGraph } from './nodeMeta'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useUserStore } from '@/store/user'
import OpenInvokeDialog from '../agent/OpenInvokeDialog.vue'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const pager = usePagination((q) => pageWorkflows(q), {
  defaults: { name: '', status: null }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteWorkflows, {
  confirm: (n) => `确定删除选中的 ${n} 个工作流吗？`,
  positiveText: '删除'
})

const formVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref(null)
const invokeVisible = ref(false)
const invokeRow = ref(null)
const form = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  status: 1
})
const formRules = {
  name: { required: true, message: '请输入名称', trigger: 'blur' }
}

function emptyForm() {
  form.id = null
  form.name = ''
  form.code = ''
  form.description = ''
  form.status = 1
}

function openForm(row) {
  if (row) {
    form.id = row.id
    form.name = row.name || ''
    form.code = row.code || ''
    form.description = row.description || ''
    form.status = row.status ?? 1
  } else {
    emptyForm()
  }
  formVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  formSubmitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      code: form.id ? form.code : `wf_${Date.now().toString(36)}`,
      description: form.description || '',
      status: form.status
    }
    if (form.id) {
      await updateWorkflow({ id: form.id, ...payload })
      message.success('保存成功')
    } else {
      const graph = defaultWorkflowGraph()
      await createWorkflow({ ...payload, nodes: graph.nodes, edges: graph.edges })
      message.success('已创建，可在列表中点「设计」编排节点')
    }
    formVisible.value = false
    loadData()
  } finally {
    formSubmitting.value = false
  }
}

function goDesign(id) {
  const { href } = router.resolve({ path: '/agent/workflow/edit', query: { id } })
  window.open(href, '_blank', 'noopener,noreferrer')
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '名称', key: 'name', width: 160 },
  { title: '节点数', key: 'nodeCount', width: 80 },
  { title: '版本', key: 'version', width: 70 },
  {
    title: '描述',
    key: 'description',
    width: 180,
    ellipsis: { tooltip: true }
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
  { title: '更新时间', key: 'updateTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 172,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('agent:workflow:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () => openForm(row)
            }),
          userStore.hasPerm('agent:workflow:edit') &&
            hActionBtn({
              tip: '设计',
              type: 'primary',
              icon: GitNetworkOutline,
              disabled: busy(row.id),
              onClick: () => goDesign(row.id)
            }),
          hActionBtn({
            tip: '调用',
            type: 'primary',
            icon: LinkOutline,
            disabled: busy(row.id),
            onClick: () => openInvoke(row)
          }),
          userStore.hasPerm('agent:workflow:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除工作流「${row.name}」吗？`,
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

async function handleDelete(row) {
  await deleteWorkflows([row.id])
  message.success('删除成功')
  loadData()
}
</script>
