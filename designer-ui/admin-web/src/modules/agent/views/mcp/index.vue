<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="名称">
          <n-input v-model:value="query.name" placeholder="名称 / 编码" clearable />
        </n-form-item>
        <n-form-item label="传输">
          <n-select
            v-model:value="query.transport"
            :options="transportOptions"
            placeholder="全部"
            clearable
          />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'agent:mcp:add'" type="primary" @click="openCreate">
            新增 MCP
          </n-button>
          <n-button
            v-perm="'agent:mcp:remove'"
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
      :title="isEdit ? '编辑 MCP' : '新增 MCP'"
      size="lg"
      :loading="submitting"
      @confirm="handleDialogConfirm"
    >
      <template v-if="!isEdit">
        <n-tabs v-model:value="createMode" type="segment" class="create-tabs" animated>
          <n-tab-pane name="form" tab="表单配置">
            <n-form
              ref="formRef"
              :model="form"
              :rules="formRules"
              label-placement="left"
              label-width="100"
            >
              <n-form-item label="名称" path="name">
                <n-input v-model:value="form.name" placeholder="MCP 名称" />
              </n-form-item>
              <n-form-item label="编码" path="code">
                <n-input v-model:value="form.code" placeholder="如 filesystem" />
              </n-form-item>
              <n-form-item label="传输方式" path="transport">
                <n-select v-model:value="form.transport" :options="transportOptions" />
              </n-form-item>
              <template v-if="form.transport === 'stdio'">
                <n-form-item label="Command" path="command">
                  <n-input v-model:value="form.command" placeholder="如 npx / uvx / node" />
                </n-form-item>
                <n-form-item label="Args">
                  <n-input v-model:value="form.args" type="textarea" :rows="2" placeholder="命令参数" />
                </n-form-item>
              </template>
              <n-form-item v-else label="URL" path="url">
                <n-input v-model:value="form.url" placeholder="http(s)://... 或 sse 地址" />
              </n-form-item>
              <n-form-item label="环境变量">
                <n-input
                  v-model:value="form.envJson"
                  type="textarea"
                  :rows="3"
                  placeholder='JSON，如 { "API_KEY": "" }'
                  class="mono"
                />
              </n-form-item>
              <n-form-item label="工具清单">
                <div class="tool-list">
                  <div class="tool-toolbar">
                    <span class="tool-hint">从 MCP Server 自动拉取，无需手填</span>
                    <n-button
                      type="primary"
                      secondary
                      :loading="pullingTools"
                      @click="handlePullTools"
                    >
                      拉取工具
                    </n-button>
                  </div>
                  <div v-if="!form.tools?.length" class="tool-empty">
                    尚未拉取，请先配置连接后点击「拉取工具」
                  </div>
                  <div v-for="(t, idx) in form.tools" :key="`${t.name}-${idx}`" class="tool-row">
                    <n-tag :bordered="false" type="info">{{ t.name }}</n-tag>
                    <span class="tool-desc" :title="t.description">{{ t.description || '—' }}</span>
                  </div>
                </div>
              </n-form-item>
              <n-form-item label="备注">
                <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="选填" />
              </n-form-item>
              <n-form-item label="状态" path="status">
                <n-radio-group v-model:value="form.status">
                  <n-radio :value="1">启用</n-radio>
                  <n-radio :value="0">停用</n-radio>
                </n-radio-group>
              </n-form-item>
            </n-form>
          </n-tab-pane>
          <n-tab-pane name="json" tab="JSON 导入">
            <div class="json-import">
              <n-input
                v-model:value="importJson"
                type="textarea"
                :rows="16"
                class="mono"
                :placeholder="jsonPlaceholder"
              />
              <p class="json-hint">
                支持标准 <code>mcpServers</code> 配置，可一次导入多个；也可粘贴单个 server 对象。
                粘贴后会自动解析并填入表单；在本页点确定可批量创建。
              </p>
            </div>
          </n-tab-pane>
        </n-tabs>
      </template>

      <n-form
        v-else
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-placement="left"
        label-width="100"
      >
        <n-form-item label="名称" path="name">
          <n-input v-model:value="form.name" placeholder="MCP 名称" />
        </n-form-item>
        <n-form-item label="编码" path="code">
          <n-input v-model:value="form.code" placeholder="如 filesystem" />
        </n-form-item>
        <n-form-item label="传输方式" path="transport">
          <n-select v-model:value="form.transport" :options="transportOptions" />
        </n-form-item>
        <template v-if="form.transport === 'stdio'">
          <n-form-item label="Command" path="command">
            <n-input v-model:value="form.command" placeholder="如 npx / uvx / node" />
          </n-form-item>
          <n-form-item label="Args">
            <n-input v-model:value="form.args" type="textarea" :rows="2" placeholder="命令参数" />
          </n-form-item>
        </template>
        <n-form-item v-else label="URL" path="url">
          <n-input v-model:value="form.url" placeholder="http(s)://... 或 sse 地址" />
        </n-form-item>
        <n-form-item label="环境变量">
          <n-input
            v-model:value="form.envJson"
            type="textarea"
            :rows="3"
            placeholder='JSON，如 { "API_KEY": "" }'
            class="mono"
          />
        </n-form-item>
        <n-form-item label="工具清单">
          <div class="tool-list">
            <div class="tool-toolbar">
              <span class="tool-hint">从 MCP Server 自动拉取，无需手填</span>
              <n-button
                type="primary"
                secondary
                :loading="pullingTools"
                @click="handlePullTools"
              >
                拉取工具
              </n-button>
            </div>
            <div v-if="!form.tools?.length" class="tool-empty">尚未拉取，请先配置连接后点击「拉取工具」</div>
            <div v-for="(t, idx) in form.tools" :key="`${t.name}-${idx}`" class="tool-row">
              <n-tag :bordered="false" type="info">{{ t.name }}</n-tag>
              <span class="tool-desc" :title="t.description">{{ t.description || '—' }}</span>
            </div>
          </div>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="选填" />
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
import { computed, h, onBeforeUnmount, ref, watch } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { CreateOutline, RefreshOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import { createMcp, deleteMcps, pageMcps, probeMcp, probeMcpTools, updateMcp } from '@/modules/agent/api'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()
const pullingTools = ref(false)
const createMode = ref('form')
const importJson = ref('')

const jsonPlaceholder = `{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
    },
    "exa": {
      "type": "streamable",
      "url": "https://mcp.exa.ai/mcp"
    }
  }
}`

const transportOptions = [
  { label: 'stdio', value: 'stdio' },
  { label: 'SSE', value: 'sse' },
  { label: 'HTTP (streamable)', value: 'http' }
]
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const pager = usePagination((q) => pageMcps(q), {
  defaults: { name: '', transport: null, status: null }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteMcps, {
  confirm: (n) => `确定删除选中的 ${n} 个 MCP 吗？`,
  positiveText: '删除'
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate: openCreateBase,
  openEdit,
  close: closeForm,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    name: '',
    code: '',
    transport: 'stdio',
    command: '',
    args: '',
    url: '',
    envJson: '',
    tools: [],
    status: 1,
    remark: ''
  },
  async onSubmit(payload) {
    let saved
    if (payload.id) {
      saved = await updateMcp({ ...payload })
      message.success('更新成功')
    } else {
      saved = await createMcp({ ...payload })
      message.success('新增成功')
    }
    const id = saved?.data?.id || payload.id
    if (id) {
      try {
        await probeMcp(id)
      } catch {
        // 保存成功但拉取失败时不阻断，可稍后手动拉取
        message.warning('已保存，但工具清单拉取失败，可稍后重试')
      }
    }
    loadData()
  }
})

function openCreate() {
  createMode.value = 'form'
  importJson.value = ''
  openCreateBase({ tools: [] })
}

function normalizeTransport(raw, url = '') {
  const t = String(raw || '')
    .trim()
    .toLowerCase()
    .replace(/-/g, '_')
  if (!t) return url ? 'http' : ''
  if (t === 'stdio') return 'stdio'
  if (t === 'sse') return 'sse'
  // streamable_http / streamable / http 等 → 表单里的 HTTP
  if (t === 'http' || t === 'streamable' || t === 'streamable_stateless' || t === 'streamable_http' || t.includes('streamable')) {
    return 'http'
  }
  if (url) return 'http'
  return t
}

function envToJson(env) {
  if (!env || typeof env !== 'object' || Array.isArray(env)) return ''
  const keys = Object.keys(env)
  if (!keys.length) return ''
  return JSON.stringify(env, null, 2)
}

function mapServerEntry(code, cfg = {}) {
  const transport =
    normalizeTransport(cfg.type || cfg.transport, cfg.url) || (cfg.url ? 'http' : 'stdio')
  const env = cfg.env && typeof cfg.env === 'object' ? cfg.env : cfg.headers
  const args = Array.isArray(cfg.args)
    ? cfg.args.map(String).join(' ')
    : cfg.args == null
      ? ''
      : String(cfg.args)
  return {
    name: String(cfg.name || code || '').trim() || code,
    code: String(code || cfg.code || '').trim(),
    transport,
    command: cfg.command == null ? '' : String(cfg.command),
    args,
    url: cfg.url == null ? '' : String(cfg.url),
    envJson: envToJson(env),
    tools: [],
    status: cfg.enabled === false || cfg.status === 0 ? 0 : 1,
    remark: cfg.remark == null ? '' : String(cfg.remark)
  }
}

/** 解析 mcpServers / 单个 server / 直接 map */
function parseMcpImportJson(text) {
  let raw
  try {
    raw = JSON.parse(text)
  } catch {
    throw new Error('JSON 格式无效')
  }
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    throw new Error('JSON 需为对象')
  }

  let servers = null
  if (raw.mcpServers && typeof raw.mcpServers === 'object' && !Array.isArray(raw.mcpServers)) {
    servers = raw.mcpServers
  } else if (raw.servers && typeof raw.servers === 'object' && !Array.isArray(raw.servers)) {
    servers = raw.servers
  } else if (raw.command || raw.url || raw.type || raw.transport) {
    const code = raw.code || raw.name || 'mcp'
    servers = { [code]: raw }
  } else {
    servers = raw
  }

  const list = Object.entries(servers).map(([code, cfg]) => {
    if (!cfg || typeof cfg !== 'object' || Array.isArray(cfg)) {
      throw new Error(`「${code}」配置无效`)
    }
    const item = mapServerEntry(code, cfg)
    if (!item.code) throw new Error('缺少 MCP 编码')
    if (item.transport === 'stdio' && !item.command) {
      throw new Error(`「${item.code}」缺少 command`)
    }
    if (item.transport !== 'stdio' && !item.url) {
      throw new Error(`「${item.code}」缺少 url`)
    }
    return item
  })
  if (!list.length) throw new Error('未解析到任何 MCP 配置')
  return list
}

let importParseTimer = null
watch(importJson, (text) => {
  if (importParseTimer) clearTimeout(importParseTimer)
  importParseTimer = setTimeout(() => {
    const raw = String(text || '').trim()
    if (!raw || isEdit.value || createMode.value !== 'json') return
    try {
      const list = parseMcpImportJson(raw)
      const first = list[0]
      Object.assign(form, { id: null, ...first, tools: [] })
      createMode.value = 'form'
      if (list.length > 1) {
        message.info(
          `已填入「${first.code}」（共 ${list.length} 个）。表单只保存当前这一个；要全部创建请回到 JSON 页点确定`
        )
      }
    } catch {
      // 输入过程中 JSON 未完成时忽略
    }
  }, 450)
})
onBeforeUnmount(() => {
  if (importParseTimer) clearTimeout(importParseTimer)
})

async function handleJsonImport() {
  let list
  try {
    list = parseMcpImportJson(importJson.value)
  } catch (e) {
    message.error(e?.message || '解析失败')
    return false
  }
  submitting.value = true
  let ok = 0
  const failed = []
  try {
    for (const item of list) {
      try {
        const saved = await createMcp({ ...item })
        ok += 1
        const id = saved?.data?.id
        if (id) {
          try {
            await probeMcp(id)
          } catch {
            // ignore probe failure
          }
        }
      } catch (e) {
        failed.push(`${item.code}: ${e?.message || '失败'}`)
      }
    }
    if (ok) {
      message.success(
        failed.length ? `成功导入 ${ok} 个，失败 ${failed.length} 个` : `成功导入 ${ok} 个 MCP`
      )
    }
    if (failed.length) {
      message.warning(failed.slice(0, 3).join('；') + (failed.length > 3 ? '…' : ''))
    }
    if (ok) {
      closeForm()
      loadData()
      return true
    }
    return false
  } finally {
    submitting.value = false
  }
}

async function handleDialogConfirm() {
  if (!isEdit.value && createMode.value === 'json') {
    await handleJsonImport()
    return
  }
  await handleSubmit()
}

async function handlePullTools() {
  if (form.transport === 'stdio' && !form.command) {
    message.warning('请先填写 Command')
    return
  }
  if (form.transport !== 'stdio' && !form.url) {
    message.warning('请先填写 URL')
    return
  }
  pullingTools.value = true
  try {
    const res = await probeMcpTools({
      id: form.id,
      name: form.name || 'draft',
      code: form.code || `draft_${Date.now()}`,
      transport: form.transport,
      command: form.command,
      args: form.args,
      url: form.url,
      envJson: form.envJson,
      status: form.status
    })
    form.tools = (res.data || []).map((t) => ({ ...t }))
    message.success(form.tools.length ? `已拉取 ${form.tools.length} 个工具` : '已连接，但未发现工具')
  } finally {
    pullingTools.value = false
  }
}

const formRules = {
  name: { required: true, message: '请输入名称', trigger: 'blur' },
  code: { required: true, message: '请输入编码', trigger: 'blur' },
  transport: { required: true, message: '请选择传输方式', trigger: 'change' },
  command: {
    required: true,
    message: '请输入 Command',
    trigger: 'blur',
    validator: (_r, v) => {
      if (form.transport !== 'stdio') return true
      return !!v
    }
  },
  url: {
    required: true,
    message: '请输入 URL',
    trigger: 'blur',
    validator: (_r, v) => {
      if (form.transport === 'stdio') return true
      return !!v
    }
  }
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '名称', key: 'name', width: 140 },
  { title: '编码', key: 'code', width: 120 },
  { title: '传输', key: 'transport', width: 90 },
  {
    title: '连接',
    key: 'endpoint',
    width: 200,
    ellipsis: { tooltip: true },
    render: (row) =>
      row.transport === 'stdio' ? `${row.command || ''} ${row.args || ''}`.trim() : row.url || '-'
  },
  { title: '工具数', key: 'toolCount', width: 80 },
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
    width: 140,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('agent:mcp:edit') &&
            hActionBtn({
              tip: '拉取工具',
              type: 'warning',
              icon: RefreshOutline,
              loading: locked(row.id, 'probe'),
              disabled: busy(row.id),
              onClick: () => run(row.id, 'probe', () => handleProbe(row))
            }),
          userStore.hasPerm('agent:mcp:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () =>
                openEdit(row, (r) => ({
                  id: r.id,
                  name: r.name,
                  code: r.code,
                  transport: r.transport,
                  command: r.command || '',
                  args: r.args || '',
                  url: r.url || '',
                  envJson: r.envJson || '',
                  tools: (r.tools || []).map((t) => ({ ...t })),
                  status: r.status,
                  remark: r.remark || ''
                }))
            }),
          userStore.hasPerm('agent:mcp:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除 MCP「${row.name}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

async function handleProbe(row) {
  const res = await probeMcp(row.id)
  const n = res.data?.toolCount ?? res.data?.tools?.length ?? 0
  message.success(n ? `已拉取 ${n} 个工具` : '已连接，但未发现工具')
  loadData()
}

async function handleDelete(row) {
  await deleteMcps([row.id])
  message.success('删除成功')
  loadData()
}
</script>

<style scoped>
.create-tabs {
  margin-bottom: 12px;
}
.json-import {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.json-hint {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}
.json-hint code {
  font-size: 14px;
}
.tool-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.tool-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.tool-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}
.tool-empty {
  font-size: 12px;
  color: var(--color-text-muted);
  padding: 8px 0;
}
.tool-row {
  display: grid;
  grid-template-columns: minmax(120px, 180px) 1fr;
  gap: 8px;
  align-items: center;
}
.tool-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mono :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}
</style>
