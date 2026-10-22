<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="参数名称">
          <n-input v-model:value="query.configName" placeholder="参数名称" clearable />
        </n-form-item>
        <n-form-item label="参数键名">
          <n-input v-model:value="query.configKey" placeholder="参数键名" clearable />
        </n-form-item>
        <n-form-item label="内置">
          <n-select v-model:value="query.configType" :options="typeOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:config:add'" type="primary" @click="openCreate">
            新增参数
          </n-button>
          <n-button
            v-perm="'system:config:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除
          </n-button>
          <n-button v-perm="'system:config:edit'" secondary @click="handleRefreshCache">
            刷新缓存
          </n-button>
        </n-space>
      </template>
    </PageTable>

    <Dialog
      v-model:show="formVisible"
      :title="isEdit ? '编辑参数' : '新增参数'"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="90">
        <n-form-item label="参数名称" path="configName">
          <n-input v-model:value="form.configName" placeholder="如 账号登录验证码" />
        </n-form-item>
        <n-form-item label="参数键名" path="configKey">
          <n-input
            v-model:value="form.configKey"
            placeholder="如 sys.account.captchaEnabled"
            :disabled="form.configType === 'Y'"
          />
        </n-form-item>
        <n-form-item label="参数键值" path="configValue">
          <n-input v-model:value="form.configValue" type="textarea" :rows="3" placeholder="参数值" />
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
import { NTag, useMessage } from 'naive-ui'
import { CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  createConfig,
  deleteConfigs,
  pageConfigs,
  refreshConfigCache,
  updateConfig
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const pager = usePagination((q) => pageConfigs(q), {
  defaults: { configName: '', configKey: '', configType: null }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteConfigs, {
  confirm: (n) => `确定删除选中的 ${n} 条参数吗？（内置参数将被跳过失败）`
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
    configName: '',
    configKey: '',
    configValue: '',
    configType: 'N',
    remark: ''
  },
  async onSubmit(payload) {
    if (payload.id) {
      await updateConfig({ ...payload })
      message.success('修改成功')
    } else {
      await createConfig({ ...payload })
      message.success('新增成功')
    }
    loadData()
  }
})

const typeOptions = [
  { label: '是', value: 'Y' },
  { label: '否', value: 'N' }
]

const formRules = {
  configName: [{ required: true, message: '请输入参数名称', trigger: ['input', 'blur'] }],
  configKey: [{ required: true, message: '请输入参数键名', trigger: ['input', 'blur'] }],
  configValue: [{ required: true, message: '请输入参数键值', trigger: ['input', 'blur'] }]
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '参数名称', key: 'configName', width: 160, ellipsis: { tooltip: true } },
  { title: '参数键名', key: 'configKey', width: 220, ellipsis: { tooltip: true } },
  { title: '参数键值', key: 'configValue', width: 160, ellipsis: { tooltip: true } },
  {
    title: '内置',
    key: 'configType',
    width: 80,
    render: (row) =>
      h(NTag, { type: row.configType === 'Y' ? 'warning' : 'default', bordered: false }, {
        default: () => (row.configType === 'Y' ? '是' : '否')
      })
  },
  { title: '备注', key: 'remark', width: 160, ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 100,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('system:config:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () =>
                openEdit(row, (r) => ({
                  id: r.id,
                  configName: r.configName,
                  configKey: r.configKey,
                  configValue: r.configValue,
                  configType: r.configType,
                  remark: r.remark
                }))
            }),
          userStore.hasPerm('system:config:remove') &&
            row.configType !== 'Y' &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除参数「${row.configKey}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

function handleDelete(row) {
  return deleteConfigs([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}

async function handleRefreshCache() {
  await refreshConfigCache()
  message.success('缓存已刷新')
}
</script>
