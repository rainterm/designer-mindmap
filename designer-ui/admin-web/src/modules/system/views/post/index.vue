<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="岗位名称">
          <n-input v-model:value="query.postName" placeholder="岗位名称" clearable />
        </n-form-item>
        <n-form-item label="岗位编码">
          <n-input v-model:value="query.postCode" placeholder="岗位编码" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:post:add'" type="primary" @click="openCreate">
            新增岗位</n-button>
          <n-button
            v-perm="'system:post:remove'"
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
      :title="isEdit ? '编辑岗位' : '新增岗位'"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="90">
        <n-form-item label="岗位名称" path="postName">
          <n-input v-model:value="form.postName" placeholder="如系长/科长/部长" />
        </n-form-item>
        <n-form-item label="岗位编码" path="postCode">
          <n-input v-model:value="form.postCode" placeholder="全局唯一，如 KE_ZHANG" />
        </n-form-item>
        <n-form-item label="职级" path="postLevel">
          <n-input-number v-model:value="form.postLevel" :min="0" :max="9999" style="width: 160px" />
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
  createPost,
  deletePosts,
  pagePosts,
  updatePost
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

const pager = usePagination((q) => pagePosts(q), {
  defaults: {
    postName: '',
    postCode: '',
    status: null
  }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deletePosts, {
  confirm: (n) => `确定删除选中的 ${n} 个岗位吗？`
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
    postName: '',
    postCode: '',
    postLevel: 0,
    sort: 0,
    status: 1,
    remark: ''
  },
  async onSubmit(payload) {
    if (payload.id) {
      await updatePost({ ...payload })
      message.success('修改成功')
    } else {
      await createPost({ ...payload })
      message.success('新增成功')
    }
    loadData()
  }
})

/** 状态筛选项取自字典 */
const { options: statusOptions } = useDict(DICT_NORMAL_DISABLE, { numeric: true })

const formRules = {
  postName: [{ required: true, message: '请输入岗位名称', trigger: ['input', 'blur'] }],
  postCode: [
    { required: true, message: '请输入岗位编码', trigger: ['input', 'blur'] },
    { pattern: /^[A-Za-z][A-Za-z0-9_]*$/, message: '仅支持字母、数字与下划线', trigger: ['input', 'blur'] }
  ]
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '岗位名称', key: 'postName', width: 140 },
  {
    title: '岗位编码',
    key: 'postCode',
    width: 140,
    render: (row) => h(NTag, { type: 'primary', bordered: false }, { default: () => row.postCode })
  },
  { title: '职级', key: 'postLevel', width: 80 },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NSwitch, {
        value: row.status === 1,
        loading: locked(row.id, 'status'),
        disabled: !userStore.hasPerm('system:post:edit') || busy(row.id),
        'onUpdate:value': (val) => run(row.id, 'status', () => handleStatusToggle(row, val ? 1 : 0))
      })
  },
  { title: '备注', key: 'remark', width: 180, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 110,
    fixed: 'right',
    render: (row) => {
      const canEdit = userStore.hasPerm('system:post:edit')
      const canRemove = userStore.hasPerm('system:post:remove')
      const rowBusy = busy(row.id)

      const editBtn = canEdit
        ? hActionBtn({
            tip: '编辑',
            icon: CreateOutline,
            disabled: rowBusy,
            onClick: () =>
              openEdit(row, (r) => ({
                id: r.id,
                postName: r.postName,
                postCode: r.postCode,
                postLevel: r.postLevel ?? 0,
                sort: r.sort ?? 0,
                status: r.status,
                remark: r.remark
              }))
          })
        : null

      const delBtn = canRemove
        ? hActionConfirm({
            tip: '删除',
            confirm: `确定删除岗位「${row.postName}」吗？`,
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
    await updatePost({ ...row, status })
    row.status = status
    message.success(status === 1 ? '已启用' : '已停用')
  } catch (e) {
    loadData()
  }
}

function handleDelete(row) {
  return deletePosts([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}
</script>
