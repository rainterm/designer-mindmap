<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="操作模块">
          <n-input v-model:value="query.title" placeholder="如 用户管理" clearable />
        </n-form-item>
        <n-form-item label="操作人">
          <n-input v-model:value="query.operName" placeholder="登录账号" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns" />

    <DetailDrawer v-model:show="detailVisible" title="操作日志详情" size="lg">
      <n-descriptions :column="1" label-placement="left" bordered>
        <n-descriptions-item label="操作模块">{{ detail.title }}</n-descriptions-item>
        <n-descriptions-item label="业务类型">{{ detail.businessType }}</n-descriptions-item>
        <n-descriptions-item label="操作人">{{ detail.operName }}</n-descriptions-item>
        <n-descriptions-item label="请求地址">
          {{ detail.requestMethod }} {{ detail.operUrl }}
        </n-descriptions-item>
        <n-descriptions-item label="调用方法">{{ detail.method }}</n-descriptions-item>
        <n-descriptions-item label="操作 IP">{{ detail.operIp }}</n-descriptions-item>
        <n-descriptions-item label="耗时">{{ detail.costTime }} ms</n-descriptions-item>
        <n-descriptions-item label="操作时间">{{ detail.operTime }}</n-descriptions-item>
        <n-descriptions-item label="状态">
          <n-tag :type="detail.status === 1 ? 'success' : 'error'" :bordered="false">
            {{ statusLabel(detail.status) }}
          </n-tag>
        </n-descriptions-item>
      </n-descriptions>

      <n-divider title-placement="left" style="margin-top: 16px">请求参数</n-divider>
      <n-code :code="formatJson(detail.operParam)" language="json" word-wrap />

      <n-divider title-placement="left">返回结果</n-divider>
      <n-code :code="formatJson(detail.jsonResult)" language="json" word-wrap />

      <template v-if="detail.errorMsg">
        <n-divider title-placement="left">异常信息</n-divider>
        <n-alert type="error">{{ detail.errorMsg }}</n-alert>
      </template>
    </DetailDrawer>
  </div>
</template>

<script setup>
import { h, reactive, ref, computed } from 'vue'
import { NTag } from 'naive-ui'
import { EyeOutline } from '@vicons/ionicons5'
import { hActionBtn } from '@/utils/actionBtn'
import { pageOperLogs } from '@/modules/system/api/system'
import { usePagination } from '@/composables/usePagination'
import { useDict } from '@/composables/useDict'
import { DICT_OPER_RESULT } from '@/constants/dict'

const pager = usePagination((q) => pageOperLogs(q), {
  defaults: { title: '', operName: '', status: null }
})
const { query } = pager

/** 执行结果文案取自字典：详情、列表与筛选项同源 */
const { options: statusOptions, labelOf: statusLabel } = useDict(DICT_OPER_RESULT, {
  numeric: true
})

const detailVisible = ref(false)
const detail = reactive({})

const columns = computed(() => [
  { title: 'ID', key: 'id', width: 70 },
  { title: '操作模块', key: 'title', width: 120 },
  {
    title: '业务类型',
    key: 'businessType',
    width: 100,
    render: (row) =>
      h(NTag, { bordered: false, type: 'default' }, { default: () => row.businessType })
  },
  { title: '操作人', key: 'operName', width: 110 },
  { title: '请求地址', key: 'operUrl', width: 220, ellipsis: { tooltip: true } },
  { title: '操作 IP', key: 'operIp', width: 130 },
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
  { title: '操作时间', key: 'operTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 56,
    fixed: 'right',
    render: (row) =>
      hActionBtn({
        tip: '详情',
        icon: EyeOutline,
        onClick: () => openDetail(row)
      })
  }
])

function openDetail(row) {
  Object.assign(detail, row)
  detailVisible.value = true
}

/** 日志里的参数与结果都是 JSON 字符串，格式化后便于阅读 */
function formatJson(text) {
  if (!text) return '—'
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch (e) {
    return text
  }
}
</script>
