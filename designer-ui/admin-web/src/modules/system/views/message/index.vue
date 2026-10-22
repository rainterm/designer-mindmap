<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="配置名称">
          <n-input v-model:value="query.configName" placeholder="配置名称" clearable />
        </n-form-item>
        <n-form-item label="通道类型">
          <n-select v-model:value="query.channelType" :options="typeOptions" placeholder="全部" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:message:add'" type="primary" @click="openCreate">
            新增通道
          </n-button>
          <n-button
            v-perm="'system:message:remove'"
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
      :title="isEdit ? '编辑通道配置' : '新增通道配置'"
      size="lg"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="110">
        <n-form-item label="配置名称" path="configName">
          <n-input v-model:value="form.configName" placeholder="如 公司邮箱 / 阿里云短信" />
        </n-form-item>
        <n-form-item label="通道类型" path="channelType">
          <n-select
            v-model:value="form.channelType"
            :options="typeOptions"
            :disabled="!!form.id"
            placeholder="选择通道类型"
            @update:value="onChannelTypeChange"
          />
        </n-form-item>
        <n-form-item label="设为默认">
          <n-switch v-model:value="form.isDefault" :checked-value="1" :unchecked-value="0" />
          <span class="form-tip">同通道类型仅一条默认</span>
        </n-form-item>
        <n-form-item label="状态">
          <n-radio-group v-model:value="form.status">
            <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio>
          </n-radio-group>
        </n-form-item>

        <n-divider title-placement="left">通道参数</n-divider>
        <TableEmpty
          v-if="!activeFields.length"
          title="请先选择通道类型"
          description="选择类型后即可配置对应参数"
        />
        <template v-for="field in activeFields" :key="field.name">
          <n-form-item :label="field.label">
            <n-input-number
              v-if="field.inputType === 'number'"
              v-model:value="form.config[field.name]"
              style="width: 100%"
            />
            <n-switch
              v-else-if="field.inputType === 'switch'"
              v-model:value="form.config[field.name]"
              :checked-value="true"
              :unchecked-value="false"
            />
            <n-input
              v-else-if="field.inputType === 'textarea'"
              v-model:value="form.config[field.name]"
              type="textarea"
              :rows="2"
              :placeholder="field.placeholder"
            />
            <n-input
              v-else-if="field.inputType === 'password'"
              v-model:value="form.config[field.name]"
              type="password"
              show-password-on="click"
              :placeholder="form.id ? '留空表示不修改' : field.placeholder"
            />
            <n-input
              v-else
              v-model:value="form.config[field.name]"
              :placeholder="field.placeholder"
            />
            <template v-if="field.help" #feedback>{{ field.help }}</template>
          </n-form-item>
        </template>

        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>

    <Dialog
      v-model:show="sendVisible"
      title="发送测试"
      :loading="sending"
      confirm-text="发送"
      @confirm="handleSend"
    >
      <n-form ref="sendFormRef" :model="sendForm" :rules="sendRules" label-placement="left" label-width="90">
        <n-form-item label="通道">
          <n-tag :bordered="false" type="info">
            {{ typeLabelMap[sendForm.channelType] || sendForm.channelType }} / {{ sendForm.configName }}
          </n-tag>
        </n-form-item>
        <n-form-item label="收件人" path="toText">
          <n-input
            v-model:value="sendForm.toText"
            :placeholder="
              sendForm.channelType === 'inbox'
                ? '留空或 * = 全体用户；也可填登录账号，逗号分隔'
                : '多个用逗号分隔'
            "
          />
        </n-form-item>
        <n-form-item label="标题">
          <n-input v-model:value="sendForm.title" placeholder="测试消息" />
        </n-form-item>
        <n-form-item label="内容">
          <n-input v-model:value="sendForm.content" type="textarea" :rows="3" placeholder="这是一条测试消息" />
        </n-form-item>
        <n-form-item v-if="sendForm.channelType === 'email'" label="HTML">
          <n-switch v-model:value="sendForm.html" />
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { NButton, NSwitch, NTag, useMessage } from 'naive-ui'
import { CreateOutline, FlaskOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import TableEmpty from '@/components/TableEmpty.vue'
import {
  changeMessageConfigStatus,
  createMessageConfig,
  deleteMessageConfigs,
  getMessageConfig,
  listMessageChannels,
  pageMessageConfigs,
  sendMessage,
  setDefaultMessageConfig,
  updateMessageConfig
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_ENABLE_DISABLE } from '@/constants/dict'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const channelTypes = ref([])

const pager = usePagination((q) => pageMessageConfigs(q), {
  defaults: {
    configName: '',
    channelType: null,
    status: null
  },
  immediate: false
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteMessageConfigs, {
  confirm: (n) => `确定删除选中的 ${n} 条配置吗？`,
  filter: (ids, rows) => ids.filter((id) => rows.find((r) => r.id === id)?.isDefault !== 1),
  emptyWarning: '默认配置不可删除'
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate: openFormCreate,
  openEdit: openFormEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    configName: '',
    channelType: null,
    isDefault: 0,
    status: 1,
    config: {},
    remark: ''
  },
  async onSubmit(payload) {
    const data = {
      id: payload.id,
      configName: payload.configName,
      channelType: payload.channelType,
      isDefault: payload.isDefault,
      status: payload.status,
      remark: payload.remark,
      config: { ...payload.config }
    }
    if (payload.id) {
      await updateMessageConfig(data)
    } else {
      await createMessageConfig(data)
    }
    message.success('保存成功')
    loadData()
  }
})

const {
  visible: sendVisible,
  submitting: sending,
  formRef: sendFormRef,
  form: sendForm,
  openCreate: openSendForm,
  submit: handleSend
} = useFormDialog({
  defaults: {
    configId: null,
    configName: '',
    channelType: '',
    toText: '',
    title: '测试消息',
    content: '这是一条测试消息',
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
  }
})

/** 状态筛选项取自字典（通道类型由后端 Provider 注册表提供，见 channelTypes） */
const { options: statusOptions } = useDict(DICT_ENABLE_DISABLE, { numeric: true })

const typeOptions = computed(() =>
  channelTypes.value.map((t) => ({ label: t.label, value: t.type }))
)

const typeLabelMap = computed(() => {
  const map = {}
  channelTypes.value.forEach((t) => {
    map[t.type] = t.label
  })
  return map
})

const activeFields = computed(() => {
  const t = channelTypes.value.find((x) => x.type === form.channelType)
  return t?.fields || []
})

const formRules = {
  configName: { required: true, message: '请输入配置名称', trigger: 'blur' },
  channelType: { required: true, message: '请选择通道类型', trigger: 'change' }
}

const sendRules = computed(() => ({
  toText:
    sendForm.channelType === 'inbox'
      ? { required: false }
      : { required: true, message: '请输入收件人', trigger: 'blur' }
}))

const columns = computed(() => [
  { type: 'selection', width: 44, disabled: (row) => row.isDefault === 1 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '配置名称', key: 'configName', width: 160 },
  {
    title: '通道',
    key: 'channelType',
    width: 110,
    render: (row) =>
      h(NTag, { bordered: false, type: 'info' }, {
        default: () => typeLabelMap.value[row.channelType] || row.channelType
      })
  },
  {
    title: '默认',
    key: 'isDefault',
    width: 90,
    render: (row) =>
      row.isDefault === 1
        ? h(NTag, { type: 'success', bordered: false }, { default: () => '默认' })
        : userStore.hasPerm('system:message:edit')
          ? h(
              NButton,
              {
                text: true,
                type: 'primary',
                loading: locked(row.id, 'default'),
                disabled: busy(row.id),
                onClick: () => run(row.id, 'default', () => handleSetDefault(row))
              },
              { default: () => '设为默认' }
            )
          : '—'
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NSwitch, {
        value: row.status === 1,
        loading: locked(row.id, 'status'),
        disabled: busy(row.id) || !userStore.hasPerm('system:message:edit') || row.isDefault === 1,
        onUpdateValue: (v) => run(row.id, 'status', () => handleStatus(row, v ? 1 : 0))
      })
  },
  { title: '备注', key: 'remark', width: 160, ellipsis: { tooltip: true } },
  { title: '更新时间', key: 'updateTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('system:message:send') &&
            hActionBtn({
              tip: '测试',
              type: 'warning',
              icon: FlaskOutline,
              disabled: busy(row.id),
              onClick: () => openSend(row)
            }),
          userStore.hasPerm('system:message:edit') &&
            hActionBtn({
              tip: '编辑',
              icon: CreateOutline,
              loading: locked(row.id, 'edit'),
              disabled: busy(row.id),
              onClick: () => run(row.id, 'edit', () => openEdit(row))
            }),
          userStore.hasPerm('system:message:remove') &&
            row.isDefault !== 1 &&
            hActionConfirm({
              tip: '删除',
              confirm: '确定删除该配置吗？',
              icon: TrashOutline,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

function buildConfigDefaults(type) {
  const t = channelTypes.value.find((x) => x.type === type)
  const cfg = {}
  ;(t?.fields || []).forEach((f) => {
    if (f.inputType === 'switch') {
      const d = String(f.defaultValue ?? 'false')
      cfg[f.name] = d === 'true' || d === '1'
    } else if (f.inputType === 'number') {
      cfg[f.name] = f.defaultValue ? Number(f.defaultValue) : null
    } else {
      cfg[f.name] = f.defaultValue || ''
    }
  })
  return cfg
}

function onChannelTypeChange(type) {
  form.config = buildConfigDefaults(type)
}

async function loadTypes() {
  const res = await listMessageChannels()
  channelTypes.value = res.data || []
}

function openCreate() {
  const inboxType = channelTypes.value.find((t) => t.type === 'inbox')
  const defaultType = inboxType?.type || channelTypes.value[0]?.type || null
  openFormCreate({
    id: null,
    configName: defaultType === 'inbox' ? '站内通知' : '',
    channelType: defaultType,
    isDefault: 1,
    status: 1,
    remark: '',
    config: buildConfigDefaults(defaultType)
  })
}

async function openEdit(row) {
  const res = await getMessageConfig(row.id)
  const data = res.data
  openFormEdit(data, (d) => ({
    id: d.id,
    configName: d.configName,
    channelType: d.channelType,
    isDefault: d.isDefault,
    status: d.status,
    remark: d.remark || '',
    config: { ...(d.config || {}) }
  }))
}

function handleDelete(row) {
  return deleteMessageConfigs([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}

async function handleSetDefault(row) {
  await setDefaultMessageConfig(row.id)
  message.success('已设为默认')
  loadData()
}

async function handleStatus(row, status) {
  await changeMessageConfigStatus(row.id, status)
  message.success('状态已更新')
  loadData()
}

function openSend(row) {
  openSendForm({
    configId: row.id,
    configName: row.configName,
    channelType: row.channelType,
    toText: '',
    title: '测试消息',
    content: '这是一条测试消息',
    html: false
  })
}

onMounted(async () => {
  await loadTypes()
  await loadData()
})
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  color: var(--n-text-color-3);
  font-size: 12px;
}
</style>
