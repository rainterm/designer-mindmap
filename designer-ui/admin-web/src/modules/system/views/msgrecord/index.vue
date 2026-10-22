<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="通道">
          <n-select v-model:value="query.channelType" :options="channelOptions" placeholder="全部" clearable />
        </n-form-item>
        <n-form-item label="标题">
          <n-input v-model:value="query.title" placeholder="标题" clearable />
        </n-form-item>
        <n-form-item label="收件人">
          <n-input v-model:value="query.receivers" placeholder="邮箱/手机号" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:msgrecord:send'" type="primary" @click="openSend()">
            发送消息
          </n-button>
          <n-button
            v-perm="'system:msgrecord:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除
          </n-button>
          <n-button v-perm="'system:msgrecord:remove'" type="warning" @click="handleClear">
            清空记录
          </n-button>
        </n-space>
      </template>
    </PageTable>

    <DetailDrawer v-model:show="detailVisible" title="消息记录详情" size="lg">
      <n-descriptions :column="1" label-placement="left" bordered>
        <n-descriptions-item label="通道">{{ channelLabel(detail.channelType) }}</n-descriptions-item>
        <n-descriptions-item label="配置">{{ detail.configName }}</n-descriptions-item>
        <n-descriptions-item label="收件人">{{ detail.receivers }}</n-descriptions-item>
        <n-descriptions-item label="标题">{{ detail.title }}</n-descriptions-item>
        <n-descriptions-item label="发送人">{{ detail.sendBy || '—' }}</n-descriptions-item>
        <n-descriptions-item label="耗时">{{ detail.costTime ?? 0 }} ms</n-descriptions-item>
        <n-descriptions-item label="发送时间">{{ detail.sendTime }}</n-descriptions-item>
        <n-descriptions-item label="状态">
          <n-tag :type="detail.status === 1 ? 'success' : 'error'" :bordered="false">
            {{ statusLabel(detail.status) }}
          </n-tag>
        </n-descriptions-item>
      </n-descriptions>
      <n-divider title-placement="left">内容</n-divider>
      <n-code :code="detail.content || '—'" word-wrap />
      <template v-if="detail.errorMsg">
        <n-divider title-placement="left">错误信息</n-divider>
        <n-alert type="error">{{ detail.errorMsg }}</n-alert>
      </template>
    </DetailDrawer>

    <Dialog
      v-model:show="sendVisible"
      :title="sendForm.fromRecordId ? '再次发送' : '发送消息'"
      :loading="sending"
      confirm-text="发送"
      @confirm="handleSend"
    >
      <n-form ref="sendFormRef" :model="sendForm" :rules="sendRules" label-placement="left" label-width="90">
        <n-form-item label="通道配置" path="configId">
          <n-select
            v-model:value="sendForm.configId"
            :options="configOptions"
            placeholder="选择已启用的通道配置"
            filterable
          />
        </n-form-item>
        <n-form-item label="收件人" path="toText">
          <n-input v-model:value="sendForm.toText" placeholder="多个用逗号分隔" />
        </n-form-item>
        <n-form-item label="标题" path="title">
          <n-input v-model:value="sendForm.title" placeholder="消息标题" />
        </n-form-item>
        <n-form-item label="内容" path="content">
          <n-input v-model:value="sendForm.content" type="textarea" :rows="4" placeholder="消息内容" />
        </n-form-item>
        <n-form-item v-if="selectedChannelType === 'email'" label="HTML">
          <n-switch v-model:value="sendForm.html" />
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { EyeOutline, RefreshOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  clearMessageRecords,
  deleteMessageRecords,
  listMessageChannels,
  pageMessageConfigs,
  pageMessageRecords,
  sendMessage
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_OPER_RESULT } from '@/constants/dict'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const channelOptions = ref([])
const channelLabelMap = ref({})
const configOptions = ref([])
const configMetaMap = ref({})

const pager = usePagination((q) => pageMessageRecords(q), {
  defaults: {
    channelType: null,
    title: '',
    receivers: '',
    status: null
  },
  immediate: false
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteMessageRecords, {
  confirm: (n) => `确定删除选中的 ${n} 条记录吗？`
})
const { run: handleClear } = useConfirmAction(() => clearMessageRecords(), {
  title: '清空记录',
  content: '将清空全部消息记录且不可恢复，确定继续吗？',
  positiveText: '确定清空',
  successMessage: '清空成功',
  onSuccess: () => loadData()
})

const {
  visible: sendVisible,
  submitting: sending,
  formRef: sendFormRef,
  form: sendForm,
  openCreate: openSendCreate,
  openEdit: openSendEdit,
  submit: handleSend
} = useFormDialog({
  defaults: {
    fromRecordId: null,
    configId: null,
    toText: '',
    title: '',
    content: '',
    html: false
  },
  async onSubmit(payload) {
    const to = payload.toText
      .split(/[,，;\s]+/)
      .map((s) => s.trim())
      .filter(Boolean)
    await sendMessage({
      configId: payload.configId,
      to,
      title: payload.title,
      content: payload.content,
      html: payload.html
    })
    message.success('发送成功')
    loadData()
  }
})

/** 发送结果文案取自字典：详情、列表与筛选项同源 */
const { options: statusOptions, labelOf: statusLabel } = useDict(DICT_OPER_RESULT, {
  numeric: true
})

const detailVisible = ref(false)
const detail = reactive({})

const sendRules = {
  configId: { type: 'number', required: true, message: '请选择通道配置', trigger: 'change' },
  toText: { required: true, message: '请输入收件人', trigger: 'blur' },
  title: { required: true, message: '请输入标题', trigger: 'blur' },
  content: { required: true, message: '请输入内容', trigger: 'blur' }
}

const selectedChannelType = computed(() => {
  const meta = configMetaMap.value[sendForm.configId]
  return meta?.channelType || ''
})

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  {
    title: '通道',
    key: 'channelType',
    width: 100,
    render: (row) =>
      h(NTag, { bordered: false, type: 'info' }, { default: () => channelLabel(row.channelType) })
  },
  { title: '配置', key: 'configName', width: 140, ellipsis: { tooltip: true } },
  { title: '收件人', key: 'receivers', width: 180, ellipsis: { tooltip: true } },
  { title: '标题', key: 'title', width: 160, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) =>
      h(
        NTag,
        { type: row.status === 1 ? 'success' : 'error', bordered: false },
        { default: () => statusLabel(row.status) }
      )
  },
  { title: '耗时', key: 'costTime', width: 90, render: (row) => `${row.costTime ?? 0} ms` },
  { title: '发送人', key: 'sendBy', width: 110 },
  { title: '发送时间', key: 'sendTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 140,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          hActionBtn({
            tip: '详情',
            icon: EyeOutline,
            disabled: busy(row.id),
            onClick: () => openDetail(row)
          }),
          userStore.hasPerm('system:msgrecord:send') &&
            hActionBtn({
              tip: '重发',
              type: 'warning',
              icon: RefreshOutline,
              loading: locked(row.id, 'send'),
              disabled: busy(row.id),
              onClick: () => run(row.id, 'send', () => openSend(row))
            }),
          userStore.hasPerm('system:msgrecord:remove') &&
            hActionConfirm({
              tip: '删除',
              confirm: '确定删除这条记录吗？',
              icon: TrashOutline,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

function channelLabel(type) {
  return channelLabelMap.value[type] || type || '—'
}

function openDetail(row) {
  Object.assign(detail, row)
  detailVisible.value = true
}

async function loadChannels() {
  try {
    const res = await listMessageChannels()
    const list = res.data || []
    channelOptions.value = list.map((t) => ({ label: t.label, value: t.type }))
    const map = {}
    list.forEach((t) => {
      map[t.type] = t.label
    })
    channelLabelMap.value = map
  } catch (e) {
    channelOptions.value = [{ label: '邮件', value: 'email' }]
    channelLabelMap.value = { email: '邮件' }
  }
}

async function loadConfigs() {
  const res = await pageMessageConfigs({ pageNum: 1, pageSize: 200, status: 1 })
  const list = res.data?.records || []
  const meta = {}
  configOptions.value = list.map((c) => {
    meta[c.id] = c
    const typeName = channelLabel(c.channelType)
    const def = c.isDefault === 1 ? '（默认）' : ''
    return { label: `${typeName} / ${c.configName}${def}`, value: c.id }
  })
  configMetaMap.value = meta
}

function handleDelete(row) {
  return deleteMessageRecords([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}

async function openSend(row) {
  await loadConfigs()
  if (!configOptions.value.length) {
    message.warning('请先在通道配置中新增并启用至少一个通道')
    return
  }
  if (row) {
    openSendEdit(row, (r) => ({
      fromRecordId: r.id,
      configId: r.configId || configOptions.value[0]?.value || null,
      toText: r.receivers || '',
      title: r.title || '',
      content: r.content || '',
      html: r.html === 1
    }))
  } else {
    openSendCreate({
      fromRecordId: null,
      configId: configOptions.value[0]?.value || null,
      toText: '',
      title: '',
      content: '',
      html: false
    })
  }
}

onMounted(async () => {
  await loadChannels()
  await loadData()
})
</script>
