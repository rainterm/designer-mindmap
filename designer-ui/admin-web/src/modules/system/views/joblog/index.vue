<template>
  <div class="page-container">
    <SearchBar :pager="pager" @reset="clearRouteQuery">
      <template #fields>
        <n-form-item v-if="query.jobId" label="任务ID">
          <n-tag :bordered="false" type="info" closable @close="clearJobIdFilter">
            {{ query.jobId }}
          </n-tag>
        </n-form-item>
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
          <n-button
            v-perm="'system:joblog:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除
          </n-button>
          <n-button v-perm="'system:joblog:remove'" type="warning" @click="handleClear">
            清空日志
          </n-button>
        </n-space>
      </template>
    </PageTable>

    <DetailDrawer v-model:show="detailVisible" title="调度日志详情" size="lg">
      <n-descriptions :column="1" label-placement="left" bordered>
        <n-descriptions-item label="任务名称">{{ detail.jobName }}</n-descriptions-item>
        <n-descriptions-item label="任务组">{{ detail.jobGroup }}</n-descriptions-item>
        <n-descriptions-item label="调用目标">{{ detail.invokeTarget }}</n-descriptions-item>
        <n-descriptions-item label="耗时">{{ detail.costTime ?? 0 }} ms</n-descriptions-item>
        <n-descriptions-item label="执行时间">{{ detail.createTime }}</n-descriptions-item>
        <n-descriptions-item label="状态">
          <n-tag :type="detail.status === 1 ? 'success' : 'error'" :bordered="false">
            {{ statusLabel(detail.status) }}
          </n-tag>
        </n-descriptions-item>
      </n-descriptions>
      <n-divider title-placement="left">日志信息</n-divider>
      <n-code :code="detail.jobMessage || '—'" word-wrap />
      <template v-if="detail.exceptionInfo">
        <n-divider title-placement="left">异常信息</n-divider>
        <n-alert type="error">{{ detail.exceptionInfo }}</n-alert>
      </template>
    </DetailDrawer>
  </div>
</template>

<script setup>
import { computed, h, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NTag, useMessage } from 'naive-ui'
import { EyeOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import { clearJobLogs, deleteJobLogs, pageJobLogs } from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useDict } from '@/composables/useDict'
import { DICT_OPER_RESULT } from '@/constants/dict'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { locked, busy, run } = useActionLock()

const pager = usePagination((q) => pageJobLogs(q), {
  defaults: {
    jobId: null,
    jobName: '',
    jobGroup: '',
    status: null
  }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteJobLogs, {
  confirm: (n) => `确定删除选中的 ${n} 条日志吗？`
})
const { run: handleClear } = useConfirmAction(() => clearJobLogs(), {
  title: '清空日志',
  content: '将清空全部调度日志且不可恢复，确定继续吗？',
  positiveText: '确定清空',
  successMessage: '清空成功',
  onSuccess: () => loadData()
})

applyRouteQuery()

/** 执行结果文案取自字典：详情、列表与筛选项同源 */
const { options: statusOptions, labelOf: statusLabel } = useDict(DICT_OPER_RESULT, {
  numeric: true
})

const detailVisible = ref(false)
const detail = reactive({})

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '任务名称', key: 'jobName', width: 140, ellipsis: { tooltip: true } },
  { title: '任务组', key: 'jobGroup', width: 100 },
  { title: '调用目标', key: 'invokeTarget', width: 220, ellipsis: { tooltip: true } },
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
  { title: '执行时间', key: 'createTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 100,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          hActionBtn({
            tip: '详情',
            type: 'primary',
            icon: EyeOutline,
            disabled: busy(row.id),
            onClick: () => openDetail(row)
          }),
          userStore.hasPerm('system:joblog:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: '确定删除这条日志吗？',
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

function openDetail(row) {
  Object.assign(detail, row)
  detailVisible.value = true
}

function applyRouteQuery() {
  const raw = route.query.jobId
  if (raw != null && String(raw).trim() !== '') {
    const id = Number(raw)
    query.jobId = Number.isFinite(id) ? id : null
  } else {
    query.jobId = null
  }
  if (route.query.jobName) {
    query.jobName = String(route.query.jobName)
  }
}

function clearRouteQuery() {
  if (route.query.jobId || route.query.jobName) {
    router.replace({ path: '/system/joblog', query: {} })
  }
}

function clearJobIdFilter() {
  query.jobId = null
  query.jobName = ''
  clearRouteQuery()
  pager.handleSearch()
}

function handleDelete(row) {
  return deleteJobLogs([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}

watch(
  () => route.query.jobId,
  () => {
    applyRouteQuery()
    loadData()
  }
)
</script>
