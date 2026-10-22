<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="客户端编码">
          <n-input v-model:value="query.clientId" placeholder="客户端编码" clearable />
        </n-form-item>
        <n-form-item label="名称">
          <n-input v-model:value="query.clientName" placeholder="客户端名称" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button quaternary @click="guideVisible = true">接口调用说明</n-button>
          <n-button v-perm="'system:oauthclient:add'" type="primary" @click="openCreate">
            新增客户端</n-button>
          <n-button
            v-perm="'system:oauthclient:remove'"
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
      :title="isEdit ? '编辑 OAuth2 客户端' : '新增 OAuth2 客户端'"
      size="lg"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="120">
        <n-form-item label="客户端编码" path="clientId">
          <n-input v-model:value="form.clientId" placeholder="小写字母开头" :disabled="isEdit" />
        </n-form-item>
        <n-form-item label="客户端名称" path="clientName">
          <n-input v-model:value="form.clientName" placeholder="第三方系统名称" />
        </n-form-item>
        <n-form-item label="Token 有效期" path="accessTokenTtl">
          <n-input-number
            v-model:value="form.accessTokenTtl"
            :min="60"
            :max="86400 * 30"
            style="width: 200px"
          />
          <span class="form-tip">秒，默认 7200</span>
        </n-form-item>
        <n-form-item label="授权接口" path="scopeIds">
          <div class="scope-field">
            <div v-if="selectedScopeTags.length" class="scope-tags">
              <n-tag v-for="t in selectedScopeTags" :key="t.id" type="primary" closable @close="removeScopeTag(t.id)">
                {{ t.scopeCode }}
              </n-tag>
            </div>
            <n-text v-else depth="3">尚未选择开放接口</n-text>
            <n-button type="primary" secondary @click="openScopeDialog">
              选择 / 新增接口
            </n-button>
          </div>
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
        <n-alert v-if="!isEdit" type="info" :bordered="false">
          创建成功后将展示一次性 client_secret，请立即保存。
        </n-alert>
      </n-form>
    </Dialog>

    <Dialog
      v-model:show="scopeDialogVisible"
      title="选择开放接口"
      size="xl"
      confirm-text="确定"
      @confirm="confirmScopePick"
    >
      <div class="scope-dialog">
        <SearchBar :pager="scopePager">
          <template #fields>
            <n-form-item label="Scope">
              <n-input v-model:value="scopeQuery.scopeCode" placeholder="如 open.ping" clearable />
            </n-form-item>
            <n-form-item label="名称">
              <n-input v-model:value="scopeQuery.scopeName" placeholder="显示名称" clearable />
            </n-form-item>
            <n-form-item label="状态">
              <n-select
                v-model:value="scopeQuery.status"
                :options="statusOptions"
                placeholder="全部"
                clearable
              />
            </n-form-item>
          </template>
        </SearchBar>

        <PageTable :pager="scopePager" :columns="scopeColumns">
          <template #toolbar-right>
            <n-button
              v-if="userStore.hasPerm('system:oauthclient:add') || userStore.hasPerm('system:oauthclient:edit')"
              type="primary"
              @click="openScopeCreate"
            >
              新增接口
            </n-button>
          </template>
        </PageTable>
      </div>
    </Dialog>

    <Dialog
      v-model:show="scopeFormVisible"
      title="新增开放接口"
      size="md"
      :loading="scopeSubmitting"
      @confirm="handleScopeCreateSubmit"
    >
      <n-form
        ref="scopeFormRef"
        :model="scopeForm"
        :rules="scopeFormRules"
        label-placement="left"
        label-width="90"
      >
        <n-form-item label="Scope" path="scopeCode">
          <n-input v-model:value="scopeForm.scopeCode" placeholder="open.xxx" />
        </n-form-item>
        <n-form-item label="名称" path="scopeName">
          <n-input v-model:value="scopeForm.scopeName" placeholder="显示名称" />
        </n-form-item>
        <n-form-item label="路径模式" path="pathPattern">
          <n-input
            v-model:value="scopeForm.pathPattern"
            placeholder="/open-api/system/ping 或 /open-api/xxx/**"
          />
        </n-form-item>
        <n-form-item label="排序" path="sort">
          <n-input-number v-model:value="scopeForm.sort" :min="0" :max="9999" style="width: 160px" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="scopeForm.status">
            <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="scopeForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>

    <Dialog v-model:show="secretVisible" title="请保存 client_secret" confirm-text="已保存" @confirm="secretVisible = false">
      <n-alert type="warning" :bordered="false" style="margin-bottom: 12px">
        密钥仅显示一次，关闭后无法再次查看（可重置生成新密钥）。
      </n-alert>
      <n-descriptions label-placement="left" :column="1" bordered>
        <n-descriptions-item label="客户端编码">
          <n-text code>{{ secretInfo.clientId }}</n-text>
        </n-descriptions-item>
        <n-descriptions-item label="client_secret">
          <n-text code>{{ secretInfo.clientSecret }}</n-text>
        </n-descriptions-item>
      </n-descriptions>
      <template #footer>
        <n-space justify="end">
          <n-button @click="copySecret">复制密钥</n-button>
          <n-button type="primary" @click="secretVisible = false">已保存</n-button>
        </n-space>
      </template>
    </Dialog>

    <Dialog v-model:show="guideVisible" title="OAuth2 接口调用说明" size="lg" confirm-text="关闭" @confirm="guideVisible = false">
      <div class="oauth-guide">
        <n-alert type="info" :bordered="false">
          采用 OAuth2 <n-text strong>Client Credentials</n-text>（客户端凭证）模式：用客户端编码 +
          secret 换取 access_token，再调用 <n-text code>/open-api/**</n-text> 开放接口。
          后端端口不对外开放，令牌端点与开放接口统一经 nginx 的 <n-text code>/open-api</n-text> 前缀转发。
        </n-alert>

        <h4>1. 获取 access_token</h4>
        <p>
          <n-text code>POST {{ apiOrigin }}/open-api/oauth2/token</n-text>
          ，Content-Type:
          <n-text code>application/x-www-form-urlencoded</n-text>
        </p>
        <n-descriptions label-placement="left" :column="1" bordered class="guide-desc">
          <n-descriptions-item label="grant_type">固定为 client_credentials</n-descriptions-item>
          <n-descriptions-item label="client_id">客户端编码</n-descriptions-item>
          <n-descriptions-item label="client_secret">创建/重置时展示的密钥</n-descriptions-item>
          <n-descriptions-item label="scope">
            可选，空格分隔；不传则授予该客户端全部已授权 Scope
          </n-descriptions-item>
        </n-descriptions>
        <pre class="guide-code">{{ tokenCurl }}</pre>
        <n-space justify="end" class="guide-actions">
          <n-button @click="copyText(tokenCurl)">复制示例</n-button>
        </n-space>
        <p>成功响应示例：</p>
        <pre class="guide-code">{{ tokenResponseSample }}</pre>

        <h4>2. 调用开放接口</h4>
        <p>
          请求头携带
          <n-text code>Authorization: Bearer &lt;access_token&gt;</n-text>
          （也可不写 Bearer 前缀）。接口路径须已登记为开放 Scope，且客户端已勾选对应授权。
        </p>
        <pre class="guide-code">{{ pingCurl }}</pre>
        <n-space justify="end" class="guide-actions">
          <n-button @click="copyText(pingCurl)">复制示例</n-button>
        </n-space>

        <h4>3. Scope 说明</h4>
        <ul>
          <li>每条开放接口对应一个 Scope（标识 + 路径模式）。</li>
          <li>路径模式支持通配：<n-text code>*</n-text> 单层、<n-text code>**</n-text> 多层、<n-text code>{var}</n-text> 路径变量。（不支持 <n-text code>?</n-text> 单字符通配）</li>
          <li>多条同时命中时，取路径模式更长（更具体）的一条做权限校验。</li>
        </ul>

        <n-alert type="warning" :bordered="false">
          示例地址取自当前访问站点；生产环境请替换为实际对外域名或 API 网关地址
        </n-alert>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button type="primary" @click="guideVisible = false">关闭</n-button>
        </n-space>
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, reactive, ref, watch } from 'vue'
import { NSwitch, NTag, useMessage } from 'naive-ui'
import { CreateOutline, KeyOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  createOauthClient,
  createOauthScope,
  deleteOauthClients,
  pageOauthClients,
  pageOauthScopes,
  resetOauthClientSecret,
  updateOauthClient
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_NORMAL_DISABLE } from '@/constants/dict'
import { useUserStore } from '@/store/user'
import { apiBase } from '@/settings'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const guideVisible = ref(false)
/** 第三方调用入口：前端 nginx（开发态为 Vite dev server）；若配置了绝对 apiBase 则用其 origin */
const apiOrigin = computed(() => {
  const base = String(apiBase || '').trim()
  if (base.startsWith('http://') || base.startsWith('https://')) {
    try {
      return new URL(base).origin
    } catch {
      // fall through
    }
  }
  return typeof window !== 'undefined' ? window.location.origin : ''
})

const tokenCurl = computed(
  () => `curl -X POST '${apiOrigin.value}/open-api/oauth2/token' \\
  -H 'Content-Type: application/x-www-form-urlencoded' \\
  -d 'grant_type=client_credentials' \\
  -d 'client_id=你的客户端编码' \\
  -d 'client_secret=你的密钥' \\
  -d 'scope=open.ping'`
)

const tokenResponseSample = `{
  "access_token": "xxxxxxxx",
  "token_type": "Bearer",
  "expires_in": 7200,
  "scope": "open.ping"
}`

const pingCurl = computed(
  () => `curl '${apiOrigin.value}/open-api/system/ping' \\
  -H 'Authorization: Bearer <access_token>'`
)

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制')
  } catch {
    message.warning('复制失败，请手动选择文本')
  }
}

const scopeDialogVisible = ref(false)
/** id -> { id, scopeCode, scopeName }，跨页勾选展示用 */
const selectedScopeCache = reactive({})

const secretVisible = ref(false)
const secretInfo = reactive({ clientId: '', clientSecret: '' })

const pager = usePagination((q) => pageOauthClients(q), {
  defaults: { clientId: '', clientName: '', status: null }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteOauthClients, {
  confirm: (n) => `确定删除选中的 ${n} 个 OAuth2 客户端吗？`
})

const scopePager = usePagination((q) => pageOauthScopes(q), {
  defaults: { scopeCode: '', scopeName: '', status: 1 },
  immediate: false,
  pageSize: 10
})
const {
  query: scopeQuery,
  rows: scopeRows,
  checkedRowKeys: scopeCheckedKeys,
  loadData: loadScopeData
} = scopePager

watch(scopeRows, (rows) => {
  for (const s of rows || []) {
    selectedScopeCache[s.id] = {
      id: s.id,
      scopeCode: s.scopeCode,
      scopeName: s.scopeName
    }
  }
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate: openCreateDialog,
  openEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    clientId: '',
    clientName: '',
    accessTokenTtl: 7200,
    scopeIds: [],
    status: 1,
    remark: ''
  },
  async onSubmit(payload) {
    if (payload.id) {
      await updateOauthClient({ ...payload })
      message.success('修改成功')
    } else {
      const res = await createOauthClient({ ...payload })
      showSecret(res.data)
      message.success('创建成功')
    }
    loadData()
  }
})

const {
  visible: scopeFormVisible,
  submitting: scopeSubmitting,
  formRef: scopeFormRef,
  form: scopeForm,
  openCreate: openScopeCreate,
  submit: handleScopeCreateSubmit
} = useFormDialog({
  defaults: {
    scopeCode: '',
    scopeName: '',
    pathPattern: '/open-api/',
    sort: 0,
    status: 1,
    remark: ''
  },
  async onSubmit(payload) {
    const res = await createOauthScope({ ...payload })
    message.success('接口已新增')
    const created = res.data
    if (created?.id != null) {
      selectedScopeCache[created.id] = {
        id: created.id,
        scopeCode: created.scopeCode,
        scopeName: created.scopeName
      }
      if (!scopeCheckedKeys.value.includes(created.id)) {
        scopeCheckedKeys.value = [...scopeCheckedKeys.value, created.id]
      }
    }
    await loadScopeData()
  }
})

/** 状态文案取自字典：筛选下拉与 Scope 列表标签同源 */
const { options: statusOptions, labelOf: statusLabel } = useDict(DICT_NORMAL_DISABLE, {
  numeric: true
})

const formRules = {
  clientId: [
    { required: true, message: '请输入客户端编码', trigger: ['input', 'blur'] },
    {
      pattern: /^[a-z][a-z0-9_-]{2,63}$/,
      message: '小写字母开头，3-64 位字母数字_-',
      trigger: ['input', 'blur']
    }
  ],
  clientName: [{ required: true, message: '请输入名称', trigger: ['input', 'blur'] }],
  accessTokenTtl: [{ required: true, type: 'number', message: '请设置有效期', trigger: ['change', 'blur'] }]
}

const scopeFormRules = {
  scopeCode: [
    { required: true, message: '请输入 Scope', trigger: ['input', 'blur'] },
    { pattern: /^[a-z][a-z0-9._-]*$/, message: '小写字母开头，可用 . _ -', trigger: ['input', 'blur'] }
  ],
  scopeName: [{ required: true, message: '请输入名称', trigger: ['input', 'blur'] }],
  pathPattern: [{ required: true, message: '请输入路径模式', trigger: ['input', 'blur'] }]
}

const selectedScopeTags = computed(() =>
  (form.scopeIds || [])
    .map((id) => selectedScopeCache[id])
    .filter(Boolean)
)

function seedScopeCache(scopeIds, scopeCodes) {
  const ids = scopeIds || []
  const codes = scopeCodes || []
  ids.forEach((id, i) => {
    if (selectedScopeCache[id]) return
    selectedScopeCache[id] = {
      id,
      scopeCode: codes[i] || String(id),
      scopeName: codes[i] || String(id)
    }
  })
}

function openCreate() {
  openCreateDialog()
}

function removeScopeTag(id) {
  form.scopeIds = (form.scopeIds || []).filter((x) => x !== id)
}

function openScopeDialog() {
  scopeCheckedKeys.value = [...(form.scopeIds || [])]
  scopeQuery.pageNum = 1
  scopeDialogVisible.value = true
  loadScopeData()
}

function confirmScopePick() {
  // 记住当前页勾选行
  for (const s of scopeRows.value || []) {
    if (scopeCheckedKeys.value.includes(s.id)) {
      selectedScopeCache[s.id] = {
        id: s.id,
        scopeCode: s.scopeCode,
        scopeName: s.scopeName
      }
    }
  }
  form.scopeIds = [...scopeCheckedKeys.value]
  scopeDialogVisible.value = false
}

const scopeColumns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  {
    title: 'Scope',
    key: 'scopeCode',
    width: 160,
    render: (row) => h(NTag, { type: 'primary', bordered: false }, { default: () => row.scopeCode })
  },
  { title: '名称', key: 'scopeName', width: 140 },
  { title: '路径模式', key: 'pathPattern', width: 220, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) =>
      h(
        NTag,
        { type: row.status === 1 ? 'success' : 'default', bordered: false },
        { default: () => statusLabel(row.status) }
      )
  }
])

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  {
    title: '客户端编码',
    key: 'clientId',
    width: 160,
    render: (row) => h(NTag, { type: 'info', bordered: false }, { default: () => row.clientId })
  },
  { title: '名称', key: 'clientName', width: 140 },
  {
    title: '授权接口',
    key: 'scopeCodes',
    width: 200,
    render: (row) => (row.scopeCodes || []).join(' ') || '-'
  },
  { title: 'TTL(秒)', key: 'accessTokenTtl', width: 90 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NSwitch, {
        value: row.status === 1,
        loading: locked(row.id, 'status'),
        disabled: !userStore.hasPerm('system:oauthclient:edit') || busy(row.id),
        'onUpdate:value': (val) => run(row.id, 'status', () => handleStatusToggle(row, val ? 1 : 0))
      })
  },
  { title: '备注', key: 'remark', width: 140, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 150,
    fixed: 'right',
    render: (row) => {
      const canEdit = userStore.hasPerm('system:oauthclient:edit')
      const canReset = userStore.hasPerm('system:oauthclient:resetSecret')
      const canRemove = userStore.hasPerm('system:oauthclient:remove')
      const rowBusy = busy(row.id)
      const editBtn = canEdit
        ? hActionBtn({
            tip: '编辑',
            icon: CreateOutline,
            disabled: rowBusy,
            onClick: () => {
              seedScopeCache(row.scopeIds, row.scopeCodes)
              openEdit(row, (r) => ({
                id: r.id,
                clientId: r.clientId,
                clientName: r.clientName,
                accessTokenTtl: r.accessTokenTtl ?? 7200,
                scopeIds: [...(r.scopeIds || [])],
                status: r.status,
                remark: r.remark
              }))
            }
          })
        : null
      const resetBtn = canReset
        ? hActionConfirm({
            tip: '重置密钥',
            confirm: `确定重置「${row.clientId}」的密钥吗？旧密钥将立即失效。`,
            icon: KeyOutline,
            disabled: rowBusy,
            loading: locked(row.id, 'reset'),
            onPositiveClick: () => run(row.id, 'reset', () => handleResetSecret(row))
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
      return hActionGroup([editBtn, resetBtn, delBtn])
    }
  }
])

function showSecret(data) {
  secretInfo.clientId = data?.clientId || ''
  secretInfo.clientSecret = data?.clientSecret || ''
  secretVisible.value = true
}

async function copySecret() {
  try {
    await navigator.clipboard.writeText(secretInfo.clientSecret)
    message.success('已复制 client_secret')
  } catch {
    message.warning('复制失败，请手动选择文本')
  }
}

async function handleStatusToggle(row, status) {
  try {
    await updateOauthClient({
      id: row.id,
      clientId: row.clientId,
      clientName: row.clientName,
      accessTokenTtl: row.accessTokenTtl,
      scopeIds: row.scopeIds || [],
      status,
      remark: row.remark
    })
    row.status = status
    message.success(status === 1 ? '已启用' : '已停用')
  } catch {
    loadData()
  }
}

async function handleResetSecret(row) {
  const res = await resetOauthClientSecret(row.id)
  showSecret(res.data)
  message.success('密钥已重置')
}

function handleDelete(row) {
  return deleteOauthClients([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}
</script>

<style scoped>
.form-tip {
  margin-left: 8px;
  color: var(--color-text-muted);
  font-size: 12px;
}
.scope-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.scope-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.scope-dialog {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.scope-dialog :deep(.n-card) {
  box-shadow: none;
}
.oauth-guide h4 {
  margin: 16px 0 8px;
  font-size: 15px;
}
.oauth-guide p,
.oauth-guide ul {
  margin: 0 0 8px;
  color: var(--color-text-secondary, #666);
  line-height: 1.6;
}
.oauth-guide ul {
  padding-left: 1.2em;
}
.guide-desc {
  margin: 8px 0 12px;
}
.guide-code {
  margin: 0;
  padding: 12px;
  border-radius: 6px;
  background: var(--color-body, #f5f5f5);
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.guide-actions {
  margin: 8px 0 12px;
}
</style>
