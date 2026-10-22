<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="部门名称">
          <n-input v-model:value="query.deptName" placeholder="部门名称" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable
      :key="tableKey"
      :pager="pager"
      :columns="columns"
      :default-expand-all="expandedAll"
    >
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:dept:add'" type="primary" @click="openCreate(null)">
            新增部门
          </n-button>
          <n-button secondary @click="toggleExpandAll">
            {{ expandedAll ? '折叠全部' : '展开全部' }}
          </n-button>
        </n-space>
      </template>
    </PageTable>

    <Dialog
      v-model:show="formVisible"
      :title="isEdit ? '编辑部门' : '新增部门'"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="90">
        <n-form-item label="上级部门" path="parentId">
          <n-tree-select
            v-model:value="form.parentId"
            :options="parentOptions"
            key-field="id"
            label-field="deptName"
            children-field="children"
            placeholder="不选表示顶级部门"
            clearable
            default-expand-all
          />
        </n-form-item>
        <n-form-item label="部门名称" path="deptName">
          <n-input v-model:value="form.deptName" placeholder="请输入部门名称" />
        </n-form-item>
        <n-form-item label="显示排序" path="sort">
          <n-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="状态">
          <n-radio-group v-model:value="form.status">
            <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import { createDept, deleteDept, getDeptOptions, getDeptTree, updateDept } from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_NORMAL_DISABLE } from '@/constants/dict'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const parentOptions = ref([])
const expandedAll = ref(true)
const tableKey = ref(0)

const pager = usePagination(
  async (q) => {
    const res = await getDeptTree(q)
    const list = res.data || []
    if (expandedAll.value) tableKey.value += 1
    return { records: list, total: list.length }
  },
  { paged: false, defaults: { deptName: '', status: null } }
)
const { query, loadData } = pager

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
    parentId: null,
    deptName: '',
    sort: 0,
    status: 1
  },
  async onSubmit(payload) {
    const data = { ...payload, parentId: payload.parentId || 0 }
    if (data.id) {
      await updateDept(data)
      message.success('修改成功')
    } else {
      await createDept(data)
      message.success('新增成功')
    }
    loadData()
  }
})

/** 状态文案取自字典：筛选下拉与列表标签同源 */
const { options: statusOptions, labelOf: statusLabel } = useDict(DICT_NORMAL_DISABLE, {
  numeric: true
})

const columns = computed(() => [
  { title: '部门名称', key: 'deptName', width: 200 },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NTag, { type: row.status === 1 ? 'success' : 'error', bordered: false }, {
        default: () => statusLabel(row.status)
      })
  },
  { title: '创建时间', key: 'createTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('system:dept:add') &&
            hActionBtn({
              tip: '新增',
              icon: AddOutline,
              disabled: busy(row.id),
              onClick: () => openCreate(row)
            }),
          userStore.hasPerm('system:dept:edit') &&
            hActionBtn({
              tip: '编辑',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () => openEdit(row)
            }),
          userStore.hasPerm('system:dept:remove') &&
            hActionConfirm({
              tip: '删除',
              confirm: `确定删除部门「${row.deptName}」吗？`,
              icon: TrashOutline,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

function toggleExpandAll() {
  expandedAll.value = !expandedAll.value
  tableKey.value += 1
}

async function loadParentOptions() {
  const res = await getDeptOptions()
  parentOptions.value = res.data || []
}

const formRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: ['input', 'blur'] }]
}

function openCreate(parent) {
  loadParentOptions()
  openFormCreate({
    id: null,
    parentId: parent?.id ?? null,
    deptName: '',
    sort: 0,
    status: 1
  })
}

function openEdit(row) {
  loadParentOptions()
  openFormEdit(row, (r) => ({
    id: r.id,
    parentId: r.parentId === 0 ? null : r.parentId,
    deptName: r.deptName,
    sort: r.sort ?? 0,
    status: r.status
  }))
}

function handleDelete(row) {
  return deleteDept(row.id).then(() => {
    message.success('删除成功')
    loadData()
  })
}
</script>
