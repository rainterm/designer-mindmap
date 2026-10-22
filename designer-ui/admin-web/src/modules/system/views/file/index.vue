<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="文件名">
          <n-input v-model:value="query.originalName" placeholder="原始文件名" clearable />
        </n-form-item>
        <n-form-item label="业务目录">
          <n-input v-model:value="query.bizDir" placeholder="如 avatar" clearable />
        </n-form-item>
        <n-form-item label="存储类型">
          <n-select v-model:value="query.storageType" :options="storageOptions" placeholder="全部" clearable />
        </n-form-item>
        <n-form-item label="创建人">
          <n-input v-model:value="query.createBy" placeholder="账号" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-upload
            v-perm="'system:file:upload'"
            :show-file-list="false"
            :custom-request="handleUpload"
            :disabled="uploading"
          >
            <n-button type="primary" :loading="uploading">
              上传文件
            </n-button>
          </n-upload>
          <n-button
            v-perm="'system:file:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除
          </n-button>
        </n-space>
      </template>
    </PageTable>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { OpenOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import { deleteFiles, pageFiles, uploadFile } from '@/modules/system/api/file'
import { listStorageTypes } from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const uploading = ref(false)

const pager = usePagination((q) => pageFiles(q), {
  defaults: {
    originalName: '',
    bizDir: '',
    storageType: null,
    createBy: ''
  }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteFiles, {
  confirm: (n) => `确定删除选中的 ${n} 个文件吗？`
})

/**
 * 存储类型来自后端 Provider 注册表（新增 S3/OSS 等实现后前端自动出现），
 * 不在页面里写死「本地 / S3」。
 */
const storageTypes = ref([])
const storageOptions = computed(() =>
  storageTypes.value.map((t) => ({ label: t.label, value: t.type }))
)
const storageLabelMap = computed(() => {
  const map = {}
  storageTypes.value.forEach((t) => {
    map[t.type] = t.label
  })
  return map
})
const storageTypeLabel = (type) => storageLabelMap.value[type] || type

onMounted(async () => {
  const res = await listStorageTypes()
  storageTypes.value = res.data || []
})

function formatSize(size) {
  const n = Number(size) || 0
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(2)} MB`
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '原始文件名', key: 'originalName', width: 200, ellipsis: { tooltip: true } },
  { title: '业务目录', key: 'bizDir', width: 100 },
  {
    title: '存储',
    key: 'storageType',
    width: 80,
    render: (row) =>
      h(
        NTag,
        { bordered: false, type: row.storageType === 's3' ? 'warning' : 'info' },
        { default: () => storageTypeLabel(row.storageType) }
      )
  },
  { title: '大小', key: 'fileSize', width: 100, render: (row) => formatSize(row.fileSize) },
  { title: '扩展名', key: 'extension', width: 80 },
  { title: '创建人', key: 'createBy', width: 110 },
  { title: '访问地址', key: 'url', width: 260, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 190 },
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
            tip: '打开',
            icon: OpenOutline,
            disabled: busy(row.id),
            onClick: () => window.open(row.url, '_blank', 'noopener,noreferrer')
          }),
          userStore.hasPerm('system:file:remove') &&
            hActionConfirm({
              tip: '删除',
              confirm: '确定删除该文件吗？',
              icon: TrashOutline,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

async function handleUpload({ file, onFinish, onError }) {
  uploading.value = true
  try {
    await uploadFile(file.file, 'common')
    message.success('上传成功')
    onFinish()
    loadData()
  } catch (e) {
    onError()
  } finally {
    uploading.value = false
  }
}

function handleDelete(row) {
  return deleteFiles([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}
</script>
