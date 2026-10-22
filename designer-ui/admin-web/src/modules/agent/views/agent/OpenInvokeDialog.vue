<template>
  <Dialog
    :show="show"
    :title="dialogTitle"
    size="lg"
    confirm-text="关闭"
    @update:show="(v) => emit('update:show', v)"
    @confirm="emit('update:show', false)"
  >
    <div v-if="row" class="invoke-guide">
      <n-alert type="info" :bordered="false">
        采用 OAuth2 Client Credentials：先换 access_token，再调用
        <n-text code>/open-api/**</n-text>。请在「OAuth2 客户端」中授权对应 Scope。
      </n-alert>

      <h4>1. 获取 access_token</h4>
      <pre class="guide-code">{{ tokenCurl }}</pre>
      <n-space justify="end" class="guide-actions">
        <n-button size="small" @click="copyText(tokenCurl)">复制</n-button>
      </n-space>

      <h4>2. 所需 Scope</h4>
      <n-space>
        <n-tag v-for="s in scopes" :key="s" size="small" type="info" :bordered="false">{{ s }}</n-tag>
      </n-space>

      <template v-if="type === 'agent'">
        <h4>3. 打开会话</h4>
        <pre class="guide-code">{{ agentOpenCurl }}</pre>
        <n-space justify="end" class="guide-actions">
          <n-button size="small" @click="copyText(agentOpenCurl)">复制</n-button>
        </n-space>

        <h4>4. 发送消息（同步）</h4>
        <pre class="guide-code">{{ agentSendCurl }}</pre>
        <n-space justify="end" class="guide-actions">
          <n-button size="small" @click="copyText(agentSendCurl)">复制</n-button>
        </n-space>

        <h4>5. 流式对话（SSE）</h4>
        <pre class="guide-code">{{ agentStreamCurl }}</pre>
        <n-space justify="end" class="guide-actions">
          <n-button size="small" @click="copyText(agentStreamCurl)">复制</n-button>
        </n-space>

        <n-divider />
        <h4>公开访问链接</h4>
        <n-alert v-if="!canPublic" type="warning" :bordered="false" class="mb8">
          需先发布智能体（version &gt; 0）且保持启用，才能开启公开链接。
        </n-alert>
        <n-space align="center" class="mb8">
          <span>公开访问</span>
          <n-switch
            :value="publicOn"
            :disabled="!canPublic || !canManagePublic || publicBusy"
            :loading="publicBusy"
            @update:value="onPublicToggle"
          />
          <n-button
            v-if="publicOn"
            size="small"
            :disabled="!canManagePublic || publicBusy"
            @click="onRotate"
          >
            轮换链接
          </n-button>
        </n-space>
        <template v-if="publicOn && publicUrl">
          <p>访问地址：</p>
          <pre class="guide-code">{{ publicUrl }}</pre>
          <n-space justify="end" class="guide-actions">
            <n-button size="small" @click="copyText(publicUrl)">复制链接</n-button>
          </n-space>
          <p>iframe 嵌入：</p>
          <pre class="guide-code">{{ iframeSnippet }}</pre>
          <n-space justify="end" class="guide-actions">
            <n-button size="small" @click="copyText(iframeSnippet)">复制 iframe</n-button>
          </n-space>
        </template>
      </template>

      <template v-else>
        <h4>3. 入参说明（开始节点）</h4>
        <div class="wf-inputs-wrap">
          <div v-if="wfLoading" class="wf-inputs-loading">加载参数中…</div>
          <table v-else-if="wfInputs.length" class="wf-inputs-table">
            <thead>
              <tr>
                <th>参数名</th>
                <th>说明</th>
                <th>必填</th>
                <th>默认值</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in wfInputs" :key="p.key">
                <td><code>{{ p.key }}</code></td>
                <td>{{ p.label || p.key }}</td>
                <td>
                  <n-tag size="small" :bordered="false" :type="p.required ? 'error' : 'default'">
                    {{ p.required ? '是' : '否' }}
                  </n-tag>
                </td>
                <td>{{ p.defaultValue || '-' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="wf-inputs-loading">未配置开始节点参数</div>
        </div>
        <p class="hint">请求体 <n-text code>params</n-text> 的 key 与上表参数名一致；可选 <n-text code>agentId</n-text> 借用智能体模型。</p>

        <h4>4. 同步执行</h4>
        <pre class="guide-code">{{ workflowRunCurl }}</pre>
        <n-space justify="end" class="guide-actions">
          <n-button size="small" @click="copyText(workflowRunCurl)">复制</n-button>
        </n-space>

        <h4>5. 流式执行（SSE）</h4>
        <pre class="guide-code">{{ workflowStreamCurl }}</pre>
        <n-space justify="end" class="guide-actions">
          <n-button size="small" @click="copyText(workflowStreamCurl)">复制</n-button>
        </n-space>
      </template>

      <n-alert type="warning" :bordered="false" class="mt8">
        示例地址取自当前站点；生产请替换为对外域名。SSE 接口响应非 Result 包装。
      </n-alert>
    </div>
    <template #footer>
      <n-space justify="end">
        <n-button type="primary" @click="emit('update:show', false)">关闭</n-button>
      </n-space>
    </template>
  </Dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import {
  disableAgentPublic,
  enableAgentPublic,
  getWorkflow,
  rotateAgentPublic
} from '@/modules/agent/api'
import { emptyParamValues, startInputsFromNodes } from '@/modules/agent/views/workflow/startInputs'
import { useUserStore } from '@/store/user'

const props = defineProps({
  show: { type: Boolean, default: false },
  /** agent | workflow */
  type: { type: String, default: 'agent' },
  row: { type: Object, default: null }
})
const emit = defineEmits(['update:show', 'updated'])

const message = useMessage()
const userStore = useUserStore()
const publicBusy = ref(false)
const localToken = ref('')
const localEnabled = ref(0)
const wfLoading = ref(false)
const wfInputs = ref([])

watch(
  () => props.row,
  (r) => {
    localToken.value = r?.publicToken || ''
    localEnabled.value = r?.publicEnabled === 1 ? 1 : 0
  },
  { immediate: true }
)

watch(
  () => [props.show, props.type, props.row?.id],
  ([visible, type, wid]) => {
    if (visible && type === 'workflow' && wid) {
      loadWorkflowInputs(wid)
    } else if (!visible) {
      wfInputs.value = []
    }
  },
  { immediate: true }
)

async function loadWorkflowInputs(wid) {
  wfLoading.value = true
  try {
    const res = await getWorkflow(wid)
    wfInputs.value = startInputsFromNodes(res?.data?.nodes || [])
  } catch {
    wfInputs.value = []
  } finally {
    wfLoading.value = false
  }
}

const wfParamsSample = computed(() => {
  const vals = emptyParamValues(wfInputs.value)
  // 示例：空默认值填占位，便于复制后改
  for (const p of wfInputs.value) {
    if (!String(vals[p.key] ?? '').trim()) {
      vals[p.key] = p.required ? `<${p.key}>` : ''
    }
  }
  return vals
})

const wfBodyJson = computed(() =>
  JSON.stringify({ params: wfParamsSample.value, agentId: null }, null, 2)
)

const dialogTitle = computed(() =>
  props.type === 'workflow' ? `调用说明 · ${props.row?.name || '工作流'}` : `调用说明 · ${props.row?.name || '智能体'}`
)
const apiOrigin = computed(() => window.location.origin)
const id = computed(() => props.row?.id)
const scopes = computed(() =>
  props.type === 'workflow' ? ['open.workflow'] : ['open.agent', 'open.agent.chat']
)
const canPublic = computed(
  () => props.type === 'agent' && props.row?.status === 1 && (props.row?.version || 0) > 0
)
const canManagePublic = computed(() => userStore.hasPerm('agent:agent:publish'))
const publicOn = computed(() => localEnabled.value === 1)
const publicUrl = computed(() =>
  localToken.value ? `${apiOrigin.value}/public/agent/${localToken.value}` : ''
)
const iframeSnippet = computed(
  () =>
    `<iframe src="${publicUrl.value}" width="100%" height="640" frameborder="0" allow="clipboard-write"></iframe>`
)

const tokenCurl = computed(
  () =>
    `curl -X POST '${apiOrigin.value}/open-api/oauth2/token' \\\n` +
    `  -H 'Content-Type: application/x-www-form-urlencoded' \\\n` +
    `  -d 'grant_type=client_credentials&client_id=YOUR_CLIENT_ID&client_secret=YOUR_SECRET&scope=${scopes.value.join('%20')}'`
)

const agentOpenCurl = computed(
  () =>
    `curl '${apiOrigin.value}/open-api/agent/session/open?agentId=${id.value}' \\\n` +
    `  -H 'Authorization: Bearer <access_token>'`
)
const agentSendCurl = computed(
  () =>
    `curl -X POST '${apiOrigin.value}/open-api/agent/session/{sessionId}/send' \\\n` +
    `  -H 'Authorization: Bearer <access_token>' \\\n` +
    `  -H 'Content-Type: application/json' \\\n` +
    `  -d '{"content":"你好"}'`
)
const agentStreamCurl = computed(
  () =>
    `curl -N -X POST '${apiOrigin.value}/open-api/agent/session/{sessionId}/send-stream' \\\n` +
    `  -H 'Authorization: Bearer <access_token>' \\\n` +
    `  -H 'Content-Type: application/json' \\\n` +
    `  -H 'Accept: text/event-stream' \\\n` +
    `  -d '{"content":"你好"}'`
)
const workflowRunCurl = computed(() => {
  const body = wfBodyJson.value.replace(/'/g, "'\\''")
  return (
    `curl -X POST '${apiOrigin.value}/open-api/agent/workflow/${id.value}/run' \\\n` +
    `  -H 'Authorization: Bearer <access_token>' \\\n` +
    `  -H 'Content-Type: application/json' \\\n` +
    `  -d '${body}'`
  )
})
const workflowStreamCurl = computed(() => {
  const body = wfBodyJson.value.replace(/'/g, "'\\''")
  return (
    `curl -N -X POST '${apiOrigin.value}/open-api/agent/workflow/${id.value}/run-stream' \\\n` +
    `  -H 'Authorization: Bearer <access_token>' \\\n` +
    `  -H 'Content-Type: application/json' \\\n` +
    `  -H 'Accept: text/event-stream' \\\n` +
    `  -d '${body}'`
  )
})

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制')
  } catch {
    message.error('复制失败')
  }
}

async function onPublicToggle(on) {
  if (!props.row?.id) return
  publicBusy.value = true
  try {
    if (on) {
      const res = await enableAgentPublic(props.row.id)
      applyPublic(res?.data)
      message.success('已开启公开访问')
    } else {
      const res = await disableAgentPublic(props.row.id)
      applyPublic(res?.data)
      message.success('已关闭公开访问')
    }
    emit('updated')
  } finally {
    publicBusy.value = false
  }
}

async function onRotate() {
  if (!props.row?.id) return
  publicBusy.value = true
  try {
    const res = await rotateAgentPublic(props.row.id)
    applyPublic(res?.data)
    message.success('链接已轮换，旧链接失效')
    emit('updated')
  } finally {
    publicBusy.value = false
  }
}

function applyPublic(data) {
  localEnabled.value = data?.publicEnabled === 1 ? 1 : 0
  localToken.value = data?.publicToken || ''
}
</script>

<style scoped>
.invoke-guide h4 {
  margin: 16px 0 8px;
  font-size: 14px;
}
.guide-code {
  margin: 0;
  padding: 12px;
  background: var(--n-color-embedded, #f5f5f5);
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.guide-actions {
  margin-top: 6px;
}
.hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--n-text-color-3);
}
.wf-inputs-wrap {
  width: 100%;
}
.wf-inputs-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.wf-inputs-table th,
.wf-inputs-table td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid var(--n-divider-color, #efeff5);
  vertical-align: middle;
}
.wf-inputs-table th {
  color: var(--n-text-color-3, #999);
  font-weight: 500;
  white-space: nowrap;
}
.wf-inputs-table code {
  font-size: 12px;
}
.wf-inputs-loading {
  padding: 8px 0;
  font-size: 13px;
  color: var(--n-text-color-3);
}
.mb8 {
  margin-bottom: 8px;
}
.mt8 {
  margin-top: 12px;
}
</style>
