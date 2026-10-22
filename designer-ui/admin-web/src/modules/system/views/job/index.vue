<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="任务名称">
          <n-input v-model:value="query.jobName" placeholder="任务名称" clearable />
        </n-form-item>
        <n-form-item label="任务组">
          <n-input v-model:value="query.jobGroup" placeholder="DEFAULT" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:job:add'" type="primary" @click="openCreate">
            新增任务
          </n-button>
          <n-button
            v-perm="'system:job:remove'"
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
      :title="isEdit ? '编辑任务' : '新增任务'"
      size="lg"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="110">
        <n-form-item label="任务名称" path="jobName">
          <n-input v-model:value="form.jobName" placeholder="如 清理临时文件" />
        </n-form-item>
        <n-form-item label="任务组" path="jobGroup">
          <n-input v-model:value="form.jobGroup" placeholder="DEFAULT" />
        </n-form-item>
        <n-form-item label="调用目标" path="invokeTarget">
          <n-input
            v-model:value="form.invokeTarget"
            placeholder="demoTask.noParams 或 demoTask.withParams('hi')"
          />
        </n-form-item>
        <n-form-item label="Cron" path="cronExpression">
          <n-input v-model:value="form.cronExpression" placeholder="0 0/10 * * * ?" />
        </n-form-item>
        <n-form-item label="misfire">
          <n-select v-model:value="form.misfirePolicy" :options="misfireOptions" />
        </n-form-item>
        <n-form-item label="允许并发">
          <n-switch v-model:value="form.concurrent" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
        <n-form-item label="状态">
          <n-radio-group v-model:value="form.status">
            <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
        <n-alert type="info" :bordered="false">
          调用目标为已注册 Bean 方法，如 demoTask.noParams；有参示例 demoTask.withParams('hello')
        </n-alert>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { NSwitch, NTag, useMessage } from 'naive-ui'
import { CreateOutline, DocumentTextOutline, PlayOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  changeJobStatus,
  createJob,
  deleteJobs,
  getJob,
  pageJobs,
  runJob,
  updateJob
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_JOB_MISFIRE_POLICY, DICT_JOB_STATUS } from '@/constants/dict'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const router = useRouter()
const { locked, busy, run } = useActionLock()

const pager = usePagination((q) => pageJobs(q), {
  defaults: {
    jobName: '',
    jobGroup: '',
    status: null
  }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteJobs, {
  confirm: (n) => `确定删除选中的 ${n} 个任务吗？`
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate,
  openEdit: openFormEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    jobName: '',
    jobGroup: 'DEFAULT',
    invokeTarget: 'demoTask.noParams',
    cronExpression: '0 0/10 * * * ?',
    misfirePolicy: 1,
    concurrent: 1,
    status: 0,
    remark: ''
  },
  async onSubmit(payload) {
    if (payload.id) {
      await updateJob({ ...payload })
    } else {
      await createJob({ ...payload })
    }
    message.success('保存成功')
    loadData()
  }
})

/** 状态与失火策略文案取自字典：筛选 / 表单下拉不再硬编码 */
const { options: statusOptions } = useDict(DICT_JOB_STATUS, { numeric: true })
const { options: misfireOptions } = useDict(DICT_JOB_MISFIRE_POLICY, { numeric: true })

const formRules = {
  jobName: { required: true, message: '请输入任务名称', trigger: 'blur' },
  invokeTarget: { required: true, message: '请输入调用目标', trigger: 'blur' },
  cronExpression: { required: true, message: '请输入 Cron 表达式', trigger: 'blur' }
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '任务名称', key: 'jobName', width: 140, ellipsis: { tooltip: true } },
  { title: '任务组', key: 'jobGroup', width: 100 },
  { title: '调用目标', key: 'invokeTarget', width: 220, ellipsis: { tooltip: true } },
  { title: 'Cron', key: 'cronExpression', width: 140, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NSwitch, {
        value: row.status === 1,
        loading: locked(row.id, 'status'),
        disabled: busy(row.id) || !userStore.hasPerm('system:job:edit'),
        'onUpdate:value': (v) => run(row.id, 'status', () => handleStatus(row, v ? 1 : 0))
      })
  },
  {
    title: '并发',
    key: 'concurrent',
    width: 80,
    render: (row) =>
      h(
        NTag,
        { bordered: false, type: row.concurrent === 1 ? 'success' : 'warning' },
        { default: () => (row.concurrent === 1 ? '允许' : '禁止') }
      )
  },
  { title: '更新时间', key: 'updateTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 180,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('system:joblog:list') &&
            hActionBtn({
              tip: '日志',
              type: 'info',
              icon: DocumentTextOutline,
              disabled: busy(row.id),
              onClick: () => openJobLogs(row)
            }),
          userStore.hasPerm('system:job:run') &&
            hActionConfirm({
              tip: '执行',
              type: 'warning',
              icon: PlayOutline,
              confirm: '确定立即执行一次该任务吗？',
              loading: locked(row.id, 'run'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'run', () => handleRun(row))
            }),
          userStore.hasPerm('system:job:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              loading: locked(row.id, 'edit'),
              disabled: busy(row.id),
              onClick: () => run(row.id, 'edit', () => openEdit(row))
            }),
          userStore.hasPerm('system:job:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: '确定删除该任务吗？',
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

async function openEdit(row) {
  const res = await getJob(row.id)
  const data = res.data
  openFormEdit(data, (d) => ({
    id: d.id,
    jobName: d.jobName,
    jobGroup: d.jobGroup || 'DEFAULT',
    invokeTarget: d.invokeTarget,
    cronExpression: d.cronExpression,
    misfirePolicy: d.misfirePolicy ?? 1,
    concurrent: d.concurrent ?? 1,
    status: d.status ?? 0,
    remark: d.remark || ''
  }))
}

function handleDelete(row) {
  return deleteJobs([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}

async function handleStatus(row, status) {
  await changeJobStatus(row.id, status)
  message.success(status === 1 ? '已恢复' : '已暂停')
  loadData()
}

async function handleRun(row) {
  await runJob(row.id)
  message.success('已触发执行')
}

function openJobLogs(row) {
  router.push({
    path: '/system/joblog',
    query: { jobId: String(row.id), jobName: row.jobName || '' }
  })
}
</script>
