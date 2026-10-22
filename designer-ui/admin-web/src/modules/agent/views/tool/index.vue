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

    <PageTable :pager="pager" :columns="columns" />

    <Dialog
      v-model:show="formVisible"
      title="工具详情"
      size="lg"
      :loading="submitting"
      confirm-text="保存状态"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" label-placement="left" label-width="90">
        <n-form-item label="类型">
          <n-tag :bordered="false">内置</n-tag>
        </n-form-item>
        <n-form-item label="名称">
          <n-input :value="form.name" disabled />
        </n-form-item>
        <n-form-item label="编码">
          <n-input :value="form.code" disabled />
        </n-form-item>
        <n-form-item label="描述">
          <n-input :value="form.description" type="textarea" :rows="2" disabled />
        </n-form-item>
        <n-form-item label="Schema">
          <n-input :value="form.schemaJson" type="textarea" :rows="8" disabled class="mono" />
        </n-form-item>
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
import { computed, h } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { EyeOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionGroup } from '@/utils/actionBtn'
import { pageTools, updateTool } from '@/modules/agent/api'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useFormDialog } from '@/composables/useFormDialog'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { busy } = useActionLock()

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const pager = usePagination((q) => pageTools(q), {
  defaults: { name: '', status: null }
})
const { query, loadData } = pager

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  openEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    name: '',
    code: '',
    description: '',
    schemaJson: '',
    status: 1
  },
  async onSubmit(payload) {
    const res = await updateTool({ id: payload.id, status: payload.status })
    if (res.code !== 200) {
      message.error(res.msg || '更新失败')
      throw new Error(res.msg || '更新失败')
    }
    message.success('已更新状态')
    loadData()
  }
})

const columns = computed(() => [
  { title: 'ID', key: 'id', width: 70 },
  { title: '名称', key: 'name', width: 160 },
  { title: '编码', key: 'code', width: 160 },
  {
    title: '类型',
    key: 'type',
    width: 90,
    render: () => h(NTag, { bordered: false }, { default: () => '内置' })
  },
  { title: '描述', key: 'description', width: 200, ellipsis: { tooltip: true } },
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
          userStore.hasPerm('agent:tool:edit') &&
            hActionBtn({
              tip: '查看 / 启停',
              type: 'primary',
              icon: EyeOutline,
              disabled: busy(row.id),
              onClick: () =>
                openEdit(row, (r) => ({
                  id: r.id,
                  name: r.name,
                  code: r.code,
                  description: r.description || '',
                  schemaJson: r.schemaJson || '',
                  status: r.status
                }))
            })
        ].filter(Boolean)
      )
  }
])
</script>

<style scoped>
.mono :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
</style>
