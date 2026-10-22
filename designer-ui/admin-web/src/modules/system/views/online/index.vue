<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="登录账号">
          <n-input v-model:value="query.username" placeholder="登录账号" clearable />
        </n-form-item>
        <n-form-item label="登录IP">
          <n-input v-model:value="query.ip" placeholder="登录IP" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns" :row-key="(row) => row.tokenId">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:online:forceLogout'" type="error" :disabled="!checkedRowKeys.length" @click="handleBatchForce"> 批量强退 </n-button>
        </n-space>
      </template>
    </PageTable>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useMessage } from 'naive-ui'
import { ExitOutline } from '@vicons/ionicons5'
import { hActionConfirm } from '@/utils/actionBtn'
import { forceLogoutOnline, forceLogoutOnlineBatch, pageOnlineUsers } from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const pager = usePagination((q) => pageOnlineUsers(q), {
  defaults: { username: '', ip: '' }
})
const { query, loadData, checkedRowKeys } = pager

const { handleBatchDelete: handleBatchForce } = useBatchDelete(pager, forceLogoutOnlineBatch, {
  title: '批量强退',
  confirm: (n) => `确定强退选中的 ${n} 个会话吗？`,
  positiveText: '确定',
  successMessage: '已强退'
})

function formatTimeout(sec) {
  if (sec == null || sec < 0) return '永久'
  if (sec < 60) return `${sec}秒`
  if (sec < 3600) return `${Math.floor(sec / 60)}分钟`
  if (sec < 86400) return `${Math.floor(sec / 3600)}小时`
  return `${Math.floor(sec / 86400)}天`
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: '账号', key: 'username', width: 120 },
  { title: '昵称', key: 'nickname', width: 120 },
  { title: 'IP', key: 'ip', width: 140 },
  { title: '浏览器', key: 'browser', width: 100 },
  { title: '系统', key: 'os', width: 100 },
  { title: '登录时间', key: 'loginTime', width: 190 },
  {
    title: '剩余有效期',
    key: 'timeout',
    width: 120,
    render: (row) => formatTimeout(row.timeout)
  },
  {
    title: 'Token',
    key: 'tokenId',
    width: 180,
    ellipsis: { tooltip: true }
  },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 56,
    fixed: 'right',
    render: (row) =>
      userStore.hasPerm('system:online:forceLogout')
        ? hActionConfirm({
            tip: '强退',
            icon: ExitOutline,
            confirm: `确定强退用户「${row.username}」吗？`,
            loading: locked(row.tokenId, 'force'),
            disabled: busy(row.tokenId),
            onPositiveClick: () => run(row.tokenId, 'force', () => handleForce(row))
          })
        : null
  }
])

function handleForce(row) {
  return forceLogoutOnline(row.tokenId).then(() => {
    message.success('已强退')
    loadData()
  })
}
</script>
