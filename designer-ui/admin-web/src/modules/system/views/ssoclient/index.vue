<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="客户端名称">
          <n-input v-model:value="query.clientName" placeholder="客户端名称" clearable />
        </n-form-item>
        <n-form-item label="客户端标识">
          <n-input v-model:value="query.clientKey" placeholder="如 app1" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:ssoclient:add'" type="primary" @click="openCreate">
            新增客户端</n-button>
          <n-button
            v-perm="'system:ssoclient:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除</n-button>
        </n-space>
      </template>
    </PageTable>

    <Dialog
      v-model:show="formVisible"
      :title="isEdit ? '编辑 SSO 客户端' : '新增 SSO 客户端'"
      :loading="submitting"
      style="width: 560px"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="客户端名称" path="clientName">
          <n-input v-model:value="form.clientName" placeholder="如外部系统" />
        </n-form-item>
        <n-form-item label="客户端标识" path="clientKey">
          <n-input v-model:value="form.clientKey" placeholder="全局唯一，如 app1" :disabled="isEdit" />
        </n-form-item>
        <n-form-item label="允许 Origin" path="origin">
          <n-input v-model:value="form.origin" placeholder="http://localhost:5176" />
        </n-form-item>
        <n-form-item label="打开地址" path="openUrl">
          <n-input v-model:value="form.openUrl" placeholder="一般与 Origin 相同" />
        </n-form-item>
        <n-form-item label="接入路径" path="entryPath">
          <n-input v-model:value="form.entryPath" placeholder="/sso" />
        </n-form-item>
        <n-form-item label="排序" path="sort">
          <n-input-number v-model:value="form.sort" :min="0" :max="9999" style="width: 160px" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio>
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
import { computed, h } from 'vue'
import { NSwitch, NTag, useMessage } from 'naive-ui'
import { CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  createSsoClient,
  deleteSsoClients,
  pageSsoClients,
  updateSsoClient
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_NORMAL_DISABLE } from '@/constants/dict'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const pager = usePagination((q) => pageSsoClients(q), {
  defaults: {
    clientName: '',
    clientKey: '',
    status: null
  }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteSsoClients, {
  confirm: (n) => `确定删除选中的 ${n} 个 SSO 客户端吗？`
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
    clientName: '',
    clientKey: '',
    origin: '',
    openUrl: '',
    entryPath: '/sso',
    sort: 0,
    status: 1,
    remark: ''
  },
  async onSubmit(payload) {
    if (payload.id) {
      await updateSsoClient({ ...payload })
      message.success('修改成功')
    } else {
      await createSsoClient({ ...payload })
      message.success('新增成功')
    }
    loadData()
  }
})

/** 状态筛选项取自字典 */
const { options: statusOptions } = useDict(DICT_NORMAL_DISABLE, { numeric: true })

const formRules = {
  clientName: [{ required: true, message: '请输入客户端名称', trigger: ['input', 'blur'] }],
  clientKey: [
    { required: true, message: '请输入客户端标识', trigger: ['input', 'blur'] },
    { pattern: /^[a-z][a-z0-9_-]*$/, message: '小写字母开头，仅字母数字_-', trigger: ['input', 'blur'] }
  ],
  origin: [{ required: true, message: '请输入允许回跳 Origin', trigger: ['input', 'blur'] }],
  openUrl: [{ required: true, message: '请输入打开地址', trigger: ['input', 'blur'] }]
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '名称', key: 'clientName', width: 120 },
  {
    title: '标识',
    key: 'clientKey',
    width: 120,
    render: (row) => h(NTag, { type: 'primary', bordered: false }, { default: () => row.clientKey })
  },
  { title: 'Origin', key: 'origin', width: 200, ellipsis: { tooltip: true } },
  { title: '打开地址', key: 'openUrl', width: 200, ellipsis: { tooltip: true } },
  { title: '接入路径', key: 'entryPath', width: 100 },
  { title: '排序', key: 'sort', width: 70 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NSwitch, {
        value: row.status === 1,
        loading: locked(row.id, 'status'),
        disabled: !userStore.hasPerm('system:ssoclient:edit') || busy(row.id),
        'onUpdate:value': (val) => run(row.id, 'status', () => handleStatusToggle(row, val ? 1 : 0))
      })
  },
  { title: '备注', key: 'remark', width: 160, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 110,
    fixed: 'right',
    render: (row) => {
      const canEdit = userStore.hasPerm('system:ssoclient:edit')
      const canRemove = userStore.hasPerm('system:ssoclient:remove')
      const rowBusy = busy(row.id)

      const editBtn = canEdit
        ? hActionBtn({
            tip: '编辑',
            icon: CreateOutline,
            disabled: rowBusy,
            onClick: () =>
              openEdit(row, (r) => ({
                id: r.id,
                clientName: r.clientName,
                clientKey: r.clientKey,
                origin: r.origin,
                openUrl: r.openUrl,
                entryPath: r.entryPath || '/sso',
                sort: r.sort ?? 0,
                status: r.status,
                remark: r.remark
              }))
          })
        : null

      const delBtn = canRemove
        ? hActionConfirm({
            tip: '删除',
            confirm: `确定删除客户端「${row.clientName}」吗？`,
            icon: TrashOutline,
            disabled: rowBusy,
            loading: locked(row.id, 'delete'),
            onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
          })
        : null

      return hActionGroup([editBtn, delBtn])
    }
  }
])

async function handleStatusToggle(row, status) {
  try {
    await updateSsoClient({ ...row, status })
    row.status = status
    message.success(status === 1 ? '已启用' : '已停用')
  } catch {
    loadData()
  }
}

function handleDelete(row) {
  return deleteSsoClients([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}
</script>
