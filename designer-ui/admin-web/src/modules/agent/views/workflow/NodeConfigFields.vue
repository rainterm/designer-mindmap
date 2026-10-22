<template>
  <!-- start：输入参数定义 -->
  <template v-if="nodeType === 'start'">
    <div class="inputs-block">
      <div class="inputs-head">
        <span class="inputs-title">输入参数</span>
        <n-button quaternary type="primary" @click="addInput">+ 添加</n-button>
      </div>
      <div v-for="(item, idx) in startInputs" :key="idx" class="input-row">
        <n-input
          :value="item.key"
          placeholder="参数名"
          class="mono cell-key"
          :disabled="isSystemStartInput(item)"
          @update:value="(v) => patchInput(idx, { key: sanitizeKey(v) })"
        />
        <n-input
          :value="item.label"
          placeholder="显示名"
          class="cell-label"
          :disabled="isSystemStartInput(item)"
          @update:value="(v) => patchInput(idx, { label: v })"
        />
        <n-input
          :value="item.defaultValue"
          placeholder="默认值（选填）"
          class="cell-default"
          :disabled="isSystemStartInput(item)"
          @update:value="(v) => patchInput(idx, { defaultValue: v })"
        />
        <div class="cell-actions">
          <template v-if="isSystemStartInput(item)">
            <n-tag :bordered="false" type="info">系统 · 必填</n-tag>
          </template>
          <template v-else>
            <n-checkbox
              :checked="item.required"
              @update:checked="(v) => patchInput(idx, { required: v })"
            >
              必填
            </n-checkbox>
            <n-button quaternary type="error" @click="removeInput(idx)">
              删除
            </n-button>
          </template>
        </div>
      </div>
      <p class="hint"><code>{query}</code> 为系统参数，不可修改；下游可用 <code>{参数名}</code> 引用</p>
    </div>
  </template>

  <!-- end：无需额外配置 -->
  <template v-else-if="nodeType === 'end'">
    <p class="hint">流程出口，无需额外参数。</p>
  </template>

  <!-- 输出：最终回答（流式） -->
  <template v-else-if="nodeType === 'output'">
    <n-form-item label="输出形式">
      <n-radio-group
        :value="jsonCfg.type ?? 'text'"
        @update:value="(v) => patchJson({ type: v })"
      >
        <n-radio value="text">文本</n-radio>
        <n-radio value="table">表格</n-radio>
      </n-radio-group>
    </n-form-item>
    <n-form-item label="输出模板">
      <n-input
        :value="jsonCfg.template ?? '{output}'"
        type="textarea"
        :rows="5"
        placeholder="如：{output} 或 回答：{output}"
        class="mono"
        @update:value="(v) => patchJson({ template: v })"
      />
    </n-form-item>
    <p class="hint">
      作为工作流<strong>回答内容</strong>流式输出。可用开始节点参数、<code>{output}</code>，
      以及 JSON 上游字段 <code>{obj.field}</code> / <code>{output.field}</code>。
      <span v-if="(jsonCfg.type ?? 'text') === 'table'">
        表格形式：将上游 <code>{output}</code>（JSON 数组，如 SQL 结果）渲染为 Markdown 表格；非数组时原样输出。
      </span>
    </p>
  </template>

  <!-- LLM：模型 + 参数 + 提示词 -->
  <template v-else-if="nodeType === 'llm'">
    <n-form-item label="供应商">
      <n-select
        :value="llmCfg.providerId"
        :options="providerOptions"
        placeholder="选择供应商"
        filterable
        clearable
        @update:value="onLlmProviderChange"
      />
    </n-form-item>
    <n-form-item label="模型">
      <n-select
        :value="llmCfg.modelCode || null"
        :options="llmModelOptions"
        placeholder="选择对话模型"
        filterable
        clearable
        tag
        :disabled="!llmCfg.providerId"
        @update:value="(v) => patchLlm({ modelCode: v || '' })"
      />
    </n-form-item>
    <div class="param-row">
      <div class="param-label">
        <span>Temperature</span>
        <b>{{ llmCfg.temperature }}</b>
      </div>
      <n-slider
        :value="llmCfg.temperature"
        :min="0"
        :max="2"
        :step="0.1"
        @update:value="(v) => patchLlm({ temperature: v })"
      />
    </div>
    <div class="param-row">
      <div class="param-label">
        <span>Top P</span>
        <b>{{ llmCfg.topP }}</b>
      </div>
      <n-slider
        :value="llmCfg.topP"
        :min="0"
        :max="1"
        :step="0.05"
        @update:value="(v) => patchLlm({ topP: v })"
      />
    </div>
    <n-form-item label="Max Tokens">
      <n-input-number
        :value="llmCfg.maxTokens"
        :min="1"
        :max="128000"
        clearable
        placeholder="不限制"
        style="width: 100%"
        @update:value="(v) => patchLlm({ maxTokens: v })"
      />
    </n-form-item>
    <n-form-item label="系统提示词">
      <n-input
        :value="llmCfg.systemPrompt"
        type="textarea"
        :rows="3"
        placeholder="角色与约束，选填。可用 {参数名} {output} {obj.field}"
        class="mono"
        @update:value="(v) => patchLlm({ systemPrompt: v })"
      />
    </n-form-item>
    <n-form-item label="输入语句">
      <n-input
        :value="llmCfg.userPrompt"
        type="textarea"
        :rows="4"
        placeholder="用户侧输入，如 {query} 或 {obj.field}"
        class="mono"
        @update:value="(v) => patchLlm({ userPrompt: v })"
      />
    </n-form-item>
    <n-form-item label="Skill">
      <n-select
        :value="llmCfg.skillCodes || []"
        :options="skillOptions"
        placeholder="选择 Skill（可多选）"
        multiple
        filterable
        clearable
        @update:value="(v) => patchLlm({ skillCodes: v || [] })"
      />
    </n-form-item>
    <n-form-item label="工具">
      <n-select
        :value="llmCfg.toolCodes || []"
        :options="toolOptions"
        placeholder="选择内置工具（可多选）"
        multiple
        filterable
        clearable
        @update:value="(v) => patchLlm({ toolCodes: v || [] })"
      />
    </n-form-item>
    <n-form-item label="MCP">
      <n-select
        :value="llmCfg.mcpCodes || []"
        :options="mcpOptions"
        placeholder="选择 MCP Server（可多选）"
        multiple
        filterable
        clearable
        @update:value="(v) => patchLlm({ mcpCodes: v || [] })"
      />
    </n-form-item>
    <n-form-item label="是否输出">
      <n-switch
        :value="!!llmCfg.emitAnswer"
        @update:value="(v) => patchLlm({ emitAnswer: !!v })"
      />
      <span class="switch-hint">开启后作为回答内容对外输出；关闭则只给下游节点用</span>
    </n-form-item>
    <n-form-item v-if="llmCfg.emitAnswer" label="流式输出">
      <n-switch
        :value="llmCfg.stream !== false"
        @update:value="(v) => patchLlm({ stream: !!v })"
      />
      <span class="switch-hint">开启后边生成边推送；关闭则整段返回</span>
    </n-form-item>
    <n-form-item label="输出思考过程">
      <n-switch
        :value="!!llmCfg.emitThinking"
        @update:value="(v) => patchLlm({ emitThinking: !!v })"
      />
      <span class="switch-hint">开启后推送模型推理/思考内容（需模型支持）</span>
    </n-form-item>
    <p class="hint">
      Skill / 工具 / MCP 绑定在本 LLM 调用上。可用开始节点参数、<code>{output}</code>，
      以及 JSON 上游字段 <code>{obj.field}</code>。
    </p>
  </template>

  <!-- 知识库检索 -->
  <template v-else-if="nodeType === 'knowledge'">
    <n-form-item label="知识库">
      <n-select
        :value="jsonCfg.knowledgeId ?? null"
        :options="knowledgeOptions"
        placeholder="选择知识库"
        filterable
        clearable
        @update:value="(v) => patchJson({ knowledgeId: v ?? null })"
      />
    </n-form-item>
    <n-form-item label="检索词">
      <n-input
        :value="jsonCfg.query ?? ''"
        placeholder="可用 {参数名}，留空则取最新文档"
        @update:value="(v) => patchJson({ query: v })"
      />
    </n-form-item>
    <n-form-item label="Top K">
      <n-input-number
        :value="jsonCfg.topK ?? 5"
        :min="1"
        :max="20"
        @update:value="(v) => patchJson({ topK: v ?? 5 })"
      />
    </n-form-item>
    <div class="param-row">
      <div class="param-label">
        <span>相似度阈值</span>
        <b>{{ Number(jsonCfg.scoreThreshold ?? 0.35).toFixed(2) }}</b>
      </div>
      <n-slider
        :value="Number(jsonCfg.scoreThreshold ?? 0.35)"
        :min="0"
        :max="1"
        :step="0.01"
        @update:value="(v) => patchJson({ scoreThreshold: v ?? 0.35 })"
      />
    </div>
    <p class="hint">低于阈值的分块会被过滤。结果写入 output，并注入后续 LLM 上下文。</p>
  </template>

  <!-- HTTP 请求 -->
  <template v-else-if="nodeType === 'http'">
    <n-form-item label="方法">
      <n-select
        :value="jsonCfg.method || 'GET'"
        :options="httpMethodOptions"
        @update:value="onHttpMethodChange"
      />
    </n-form-item>
    <n-form-item label="URL">
      <n-input
        :value="jsonCfg.url || ''"
        placeholder="https://api.example.com/path"
        class="mono"
        @update:value="(v) => patchJson({ url: v })"
      />
    </n-form-item>
    <div class="form-fields-block">
      <div class="inputs-head">
        <span class="inputs-title">请求头</span>
        <n-button quaternary type="primary" @click="addHttpHeaderField">+ 添加</n-button>
      </div>
      <div v-if="!httpHeaderFields.length" class="inputs-empty">暂无请求头（Content-Type 一般由 Body 类型自动设置）</div>
      <div v-for="(item, idx) in httpHeaderFields" :key="'h-' + idx" class="form-field-row">
        <n-input
          :value="item.key"
          placeholder="Header 名，如 Authorization"
          class="mono"
          @update:value="(v) => patchHttpHeaderField(idx, { key: v })"
        />
        <n-input
          :value="item.value"
          placeholder="值，可用 {query} {obj.field}"
          class="mono"
          @update:value="(v) => patchHttpHeaderField(idx, { value: v })"
        />
        <n-button quaternary type="error" @click="removeHttpHeaderField(idx)">删除</n-button>
      </div>
    </div>
    <n-form-item :label="httpIsQueryMethod ? '参数类型' : 'Body 类型'">
      <n-select
        :value="httpBodyType"
        :options="httpBodyTypeOptions"
        @update:value="onHttpBodyTypeChange"
      />
    </n-form-item>
    <n-form-item v-if="!httpIsQueryMethod && httpBodyType === 'json'" label="JSON Body">
      <n-input
        :value="jsonCfg.body || ''"
        type="textarea"
        :rows="4"
        placeholder='{"q":"{query}"}'
        class="mono"
        @update:value="(v) => patchJson({ body: v })"
      />
    </n-form-item>
    <n-form-item v-else-if="!httpIsQueryMethod && httpBodyType === 'raw'" label="Raw Body">
      <n-input
        :value="jsonCfg.body || ''"
        type="textarea"
        :rows="4"
        placeholder="纯文本，可用 {query}"
        class="mono"
        @update:value="(v) => patchJson({ body: v })"
      />
    </n-form-item>
    <div
      v-else-if="httpBodyType === 'form' || httpBodyType === 'multipart'"
      class="form-fields-block"
    >
      <div class="inputs-head">
        <span class="inputs-title">{{ httpFormFieldsTitle }}</span>
        <n-button quaternary type="primary" @click="addHttpFormField">+ 添加</n-button>
      </div>
      <div v-if="!httpFormFields.length" class="inputs-empty">暂无字段</div>
      <div v-for="(item, idx) in httpFormFields" :key="idx" class="form-field-row">
        <n-input
          :value="item.key"
          placeholder="字段名"
          class="mono"
          @update:value="(v) => patchHttpFormField(idx, { key: v })"
        />
        <n-input
          :value="item.value"
          placeholder="值，可用 {query} {obj.field}"
          class="mono"
          @update:value="(v) => patchHttpFormField(idx, { value: v })"
        />
        <n-button quaternary type="error" @click="removeHttpFormField(idx)">删除</n-button>
      </div>
    </div>
    <n-form-item label="超时(秒)">
      <n-input-number
        :value="jsonCfg.timeoutSec ?? 15"
        :min="1"
        :max="120"
        @update:value="(v) => patchJson({ timeoutSec: v ?? 15 })"
      />
    </n-form-item>
    <p class="hint">
      URL / 请求头 / 字段值支持变量。上游若为 JSON 字符串，可用
      <code>{obj.field}</code> 或 <code>{output.field}</code> 取字段。GET/DELETE 的表单参数会拼到 Query；POST/PUT/PATCH 的 form /
      multipart 作为 Body 并自动设置 Content-Type。响应体写入 output。
    </p>
  </template>

  <!-- 代码执行 -->
  <template v-else-if="nodeType === 'code'">
    <n-form-item label="语言">
      <n-select
        :value="codeLanguage"
        :options="codeLanguageOptions"
        @update:value="onCodeLanguageChange"
      />
    </n-form-item>
    <n-form-item label="示例">
      <div class="code-example-row">
        <n-select
          :value="codeExampleKey"
          :options="codeExampleOptions"
          placeholder="选择示例填入脚本"
          clearable
          @update:value="onCodeExampleSelect"
        />
        <n-button
          v-if="codeExampleKey"
          secondary
          @click="applyCodeExample(codeExampleKey)"
        >
          重新填入
        </n-button>
      </div>
    </n-form-item>
    <n-form-item label="脚本">
      <WorkflowCodeEditor
        :key="'code-editor-' + codeLanguage"
        :model-value="jsonCfg.script || ''"
        :language="codeLanguage"
        :placeholder="codePlaceholder"
        @update:model-value="(v) => patchJson({ script: v })"
      />
    </n-form-item>
    <p v-if="codeLanguage === 'python'" class="hint">
      可用开始节点参数及上游 <code>output</code>；上游为 JSON 时会自动解析，并提供
      <code>obj</code>。可用 <code>return 值</code>、最后一行表达式，或赋值给 <code>result</code>。
    </p>
    <p v-else class="hint">
      可用开始节点参数及上游 <code>output</code>；上游为 JSON 时会自动解析，并提供
      <code>obj</code>。可用 <code>return 值</code>，或把结果写在最后一行表达式。
    </p>
  </template>

  <!-- SQL 执行 -->
  <template v-else-if="nodeType === 'sql'">
    <n-form-item label="数据源">
      <div class="code-example-row">
        <n-select
          :value="jsonCfg.datasourceId ?? null"
          :options="sqlDsOptions"
          placeholder="选择数据源"
          filterable
          clearable
          @update:value="(v) => patchJson({ datasourceId: v ?? null })"
        />
        <n-button secondary @click="openDsCreate()">+ 添加数据源</n-button>
      </div>
    </n-form-item>
    <n-form-item label="SQL">
      <WorkflowCodeEditor
        :model-value="jsonCfg.sql || ''"
        language="sql"
        placeholder="SELECT id, name FROM agent_agent WHERE status = 1 LIMIT 10"
        @update:model-value="(v) => patchJson({ sql: v })"
      />
    </n-form-item>
    <p class="hint">
      仅允许只读查询（SELECT / WITH / SHOW / DESCRIBE / EXPLAIN），单条语句，最多 100 行。
      支持开始节点参数、<code>{output}</code> 及 JSON 字段 <code>{obj.field}</code>。结果以 JSON 数组写入 output。
    </p>

    <Dialog
      :show="dsVisible"
      title="添加数据源"
      size="md"
      confirm-text="保存"
      :loading="dsSubmitting"
      @update:show="(v) => !v && closeDs()"
      @confirm="submitDs"
    >
      <n-form ref="dsFormRef" :model="dsForm" label-placement="top">
        <n-form-item
          label="名称"
          path="name"
          :rule="{ required: true, message: '请填写名称', trigger: ['blur', 'input'] }"
        >
          <n-input v-model:value="dsForm.name" placeholder="如 主库" />
        </n-form-item>
        <n-form-item label="类型">
          <n-select v-model:value="dsForm.type" :options="dsTypeOptions" />
        </n-form-item>
        <n-form-item label="主机">
          <n-input v-model:value="dsForm.host" placeholder="127.0.0.1" class="mono" />
        </n-form-item>
        <n-form-item label="端口">
          <n-input-number
            v-model:value="dsForm.port"
            :min="1"
            :max="65535"
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item label="数据库">
          <n-input v-model:value="dsForm.database" class="mono" />
        </n-form-item>
        <n-form-item label="用户名">
          <n-input v-model:value="dsForm.username" class="mono" />
        </n-form-item>
        <n-form-item label="密码">
          <n-input
            v-model:value="dsForm.password"
            type="password"
            show-password-on="click"
          />
        </n-form-item>
        <n-form-item label="JDBC URL（可选）">
          <n-input
            v-model:value="dsForm.jdbcUrl"
            placeholder="jdbc:mysql://host:port/db"
            class="mono"
          />
        </n-form-item>
      </n-form>
    </Dialog>
  </template>

  <!-- Condition：如果 / 否则如果 / 否则 -->
  <template v-else-if="nodeType === 'condition'">
    <div class="form-fields-block">
      <div class="inputs-head">
        <span class="inputs-title">分支条件</span>
        <n-button quaternary type="primary" @click="addConditionCase">+ 否则如果</n-button>
      </div>
      <div
        v-for="(item, idx) in conditionCases"
        :key="item.id"
        class="form-field-row condition-row"
      >
        <span class="condition-kind">{{ idx === 0 ? '如果' : '否则如果' }}</span>
        <n-input
          :value="item.match"
          placeholder="如 result.code == 200"
          class="mono"
          @update:value="(v) => patchConditionCase(idx, { match: v })"
        />
        <n-button
          v-if="conditionCases.length > 1"
          quaternary
          type="error"
          @click="removeConditionCase(idx)"
        >
          删除
        </n-button>
        <span v-else class="condition-spacer" />
      </div>
      <div class="form-field-row condition-row else-row">
        <span class="condition-kind">否则</span>
        <span class="condition-else-hint">以上都不匹配时走此出口</span>
      </div>
    </div>
    <p class="hint">
      按 JavaScript 求值，写法与 JS 条件相同，如
      <code>result.code == 200</code>、<code>obj.data.ok &amp;&amp; query</code>。
      JSON 字符串变量会自动 parse 成对象。从节点右侧对应出口连线。
    </p>
  </template>

  <!-- 未知类型兜底 -->
  <template v-else>
    <n-form-item label="配置">
      <n-input
        :value="config"
        type="textarea"
        :rows="4"
        class="mono"
        @update:value="emitConfig"
      />
    </n-form-item>
  </template>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useMessage } from 'naive-ui'
import Dialog from '@/components/Dialog.vue'
import { useFormDialog } from '@/composables/useFormDialog'
import WorkflowCodeEditor from './WorkflowCodeEditor.vue'
import {
  SYSTEM_START_INPUT_KEY,
  ensureSystemInputs,
  isSystemStartInput,
  parseStartInputs
} from './startInputs'
import { defaultLlmConfig, parseConditionConfig } from './nodeMeta'

const props = defineProps({
  nodeType: { type: String, required: true },
  config: { type: String, default: '' },
  skillOptions: { type: Array, default: () => [] },
  toolOptions: { type: Array, default: () => [] },
  mcpOptions: { type: Array, default: () => [] },
  knowledgeOptions: { type: Array, default: () => [] },
  /** [{ id, name, models: [{ modelCode, displayName, modelType, status }] }] */
  providers: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:config'])

const NON_CHAT_MODEL_TYPES = new Set(['embedding', 'rerank', 'speech', 'tts', 'image', 'moderation'])

const httpMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'PATCH', value: 'PATCH' },
  { label: 'DELETE', value: 'DELETE' }
]

const httpBodyTypeOptionsAll = [
  { label: 'JSON', value: 'json' },
  { label: 'x-www-form-urlencoded', value: 'form' },
  { label: 'form-data (multipart)', value: 'multipart' },
  { label: 'Raw 文本', value: 'raw' },
  { label: '无 Body', value: 'none' }
]

const httpBodyTypeOptionsQuery = [
  { label: '表单参数（拼到 URL）', value: 'form' },
  { label: '无参数', value: 'none' }
]

const jsonCfg = computed(() => {
  const raw = (props.config || '').trim()
  if (!raw.startsWith('{')) return {}
  try {
    return JSON.parse(raw) || {}
  } catch {
    return {}
  }
})

const llmCfg = computed(() => {
  const base = defaultLlmConfig()
  const raw = (props.config || '').trim()
  if (!raw.startsWith('{')) return base
  try {
    const parsed = JSON.parse(raw) || {}
    return {
      ...base,
      ...parsed,
      skillCodes: Array.isArray(parsed.skillCodes) ? parsed.skillCodes : [],
      toolCodes: Array.isArray(parsed.toolCodes) ? parsed.toolCodes : [],
      mcpCodes: Array.isArray(parsed.mcpCodes) ? parsed.mcpCodes : [],
      emitAnswer: !!parsed.emitAnswer,
      stream: parsed.stream !== false,
      emitThinking: !!parsed.emitThinking
    }
  } catch {
    return base
  }
})

const providerOptions = computed(() =>
  (props.providers || []).map((p) => ({ label: p.name, value: p.id }))
)

const llmModelOptions = computed(() => {
  const p = (props.providers || []).find((x) => x.id === llmCfg.value.providerId)
  return (p?.models || [])
    .filter((m) => m.status !== 0 && !NON_CHAT_MODEL_TYPES.has(m.modelType))
    .map((m) => ({
      label: m.displayName || m.modelCode,
      value: m.modelCode
    }))
})

const startInputs = computed(() =>
  props.nodeType === 'start' ? parseStartInputs(props.config) : []
)

const httpIsQueryMethod = computed(() => {
  const m = (jsonCfg.value.method || 'GET').toUpperCase()
  return m === 'GET' || m === 'DELETE'
})

const httpBodyTypeOptions = computed(() =>
  httpIsQueryMethod.value ? httpBodyTypeOptionsQuery : httpBodyTypeOptionsAll
)

const httpBodyType = computed(() => {
  const t = String(jsonCfg.value.bodyType || (httpIsQueryMethod.value ? 'form' : 'json')).toLowerCase()
  if (t === 'urlencoded' || t === 'x-www-form-urlencoded') return 'form'
  if (t === 'form-data') return 'multipart'
  if (httpIsQueryMethod.value && t !== 'form' && t !== 'none') return 'form'
  return t || (httpIsQueryMethod.value ? 'form' : 'json')
})

const httpFormFields = computed(() => {
  const list = jsonCfg.value.formFields
  return Array.isArray(list) ? list : []
})

const httpHeaderFields = computed(() => {
  const list = jsonCfg.value.headerFields
  if (Array.isArray(list)) return list
  return parseLegacyHeaders(jsonCfg.value.headers)
})

function parseLegacyHeaders(raw) {
  if (!raw || typeof raw !== 'string') return []
  const out = []
  for (const line of raw.split(/\r?\n/)) {
    const t = line.trim()
    if (!t || t.startsWith('#')) continue
    const idx = t.indexOf(':')
    if (idx <= 0) continue
    out.push({ key: t.slice(0, idx).trim(), value: t.slice(idx + 1).trim() })
  }
  return out
}

const httpFormFieldsTitle = computed(() => {
  if (httpIsQueryMethod.value) return '查询参数（表单）'
  if (httpBodyType.value === 'multipart') return 'form-data 字段'
  return 'x-www-form-urlencoded 字段'
})

function emitConfig(v) {
  emit('update:config', v ?? '')
}

function patchJson(patch) {
  emitConfig(JSON.stringify({ ...jsonCfg.value, ...patch }))
}

// ---------------------- SQL 节点：数据源内联管理 ----------------------
const message = useMessage()

const dsTypeOptions = [
  { label: 'MySQL', value: 'mysql' },
  { label: 'MariaDB', value: 'mariadb' },
  { label: 'PostgreSQL', value: 'postgresql' }
]

const sqlDsOptions = computed(() =>
  (jsonCfg.value.datasources ?? []).map((d) => ({ label: d.name, value: d.id }))
)

const {
  visible: dsVisible,
  submitting: dsSubmitting,
  formRef: dsFormRef,
  form: dsForm,
  openCreate: openDsCreate,
  close: closeDs,
  submit: submitDs
} = useFormDialog({
  defaults: {
    id: '',
    name: '',
    type: 'mysql',
    host: '',
    port: 3306,
    database: '',
    username: '',
    password: '',
    jdbcUrl: ''
  },
  async onSubmit(f) {
    if (!String(f.name || '').trim()) {
      message.warning('请填写数据源名称')
      return false
    }
    const id = f.id || ('ds_' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6))
    const list = (jsonCfg.value.datasources ?? []).filter((d) => d.id !== id)
    list.push({
      id,
      name: f.name,
      type: f.type,
      host: f.host,
      port: f.port,
      database: f.database,
      username: f.username,
      password: f.password,
      jdbcUrl: f.jdbcUrl
    })
    patchJson({ datasources: list, datasourceId: id })
    message.success('已添加数据源')
    return true
  }
})

function onHttpMethodChange(v) {
  const method = v || 'GET'
  const upper = String(method).toUpperCase()
  const patch = { method }
  if (upper === 'GET' || upper === 'DELETE') {
    const t = String(jsonCfg.value.bodyType || '').toLowerCase()
    if (t !== 'form' && t !== 'none') {
      patch.bodyType = 'form'
    }
    if (!Array.isArray(jsonCfg.value.formFields)) {
      patch.formFields = []
    }
  } else if (!jsonCfg.value.bodyType) {
    patch.bodyType = 'json'
  }
  patchJson(patch)
}

function onHttpBodyTypeChange(v) {
  const bodyType = v || (httpIsQueryMethod.value ? 'form' : 'json')
  const patch = { bodyType }
  if ((bodyType === 'form' || bodyType === 'multipart') && !Array.isArray(jsonCfg.value.formFields)) {
    patch.formFields = []
  }
  patchJson(patch)
}

function addHttpFormField() {
  const list = [...httpFormFields.value, { key: '', value: '' }]
  patchJson({ formFields: list })
}

function patchHttpFormField(idx, patch) {
  const list = httpFormFields.value.map((x, i) => (i === idx ? { ...x, ...patch } : { ...x }))
  patchJson({ formFields: list })
}

function removeHttpFormField(idx) {
  patchJson({ formFields: httpFormFields.value.filter((_, i) => i !== idx) })
}

function persistHttpHeaders(list) {
  patchJson({ headerFields: list, headers: '' })
}

function addHttpHeaderField() {
  persistHttpHeaders([...httpHeaderFields.value, { key: '', value: '' }])
}

function patchHttpHeaderField(idx, patch) {
  persistHttpHeaders(httpHeaderFields.value.map((x, i) => (i === idx ? { ...x, ...patch } : { ...x })))
}

function removeHttpHeaderField(idx) {
  persistHttpHeaders(httpHeaderFields.value.filter((_, i) => i !== idx))
}

const conditionCases = computed(() => parseConditionConfig(props.config).cases)

function emitConditionCases(cases) {
  emitConfig(JSON.stringify({ cases }))
}

function addConditionCase() {
  emitConditionCases([
    ...conditionCases.value,
    { id: `elif_${Date.now().toString(36)}`, label: '', match: '' }
  ])
}

function patchConditionCase(idx, patch) {
  emitConditionCases(conditionCases.value.map((x, i) => (i === idx ? { ...x, ...patch } : { ...x })))
}

function removeConditionCase(idx) {
  if (conditionCases.value.length <= 1) return
  emitConditionCases(conditionCases.value.filter((_, i) => i !== idx))
}

const codeLanguageOptions = [
  { label: 'JavaScript', value: 'js' },
  { label: 'Python', value: 'python' }
]

const codeLanguage = computed(() => {
  const l = String(jsonCfg.value.language || 'js').toLowerCase()
  return l === 'py' || l === 'python' ? 'python' : 'js'
})

const codePlaceholder = computed(() =>
  codeLanguage.value === 'python'
    ? '# 可用开始节点参数、output、obj（JSON 上游）'
    : '// 可用开始节点参数、output、obj（JSON 上游）'
)

const JS_CODE_EXAMPLES = {
  passThrough: {
    label: '透传上游 output',
    script: ['// 直接把上游结果传给下游', 'output;'].join('\n')
  },
  concat: {
    label: '拼接查询与上游结果',
    script: [
      '// query 为开始节点系统参数；output 为上游节点结果',
      'var q = query || "";',
      'var prev = output || "";',
      'var result = prev',
      '  ? ("问题：" + q + "\\n\\n上下文：\\n" + prev)',
      '  : q;',
      'result;'
    ].join('\n')
  },
  jsonPick: {
    label: '解析 JSON 取字段',
    script: [
      '// 上游 JSON 已自动解析为 obj',
      'var name = (obj && obj.name) ? obj.name : "";',
      'return "姓名：" + name;'
    ].join('\n')
  },
  jsonBuild: {
    label: '组装 JSON 对象',
    script: [
      'var payload = {};',
      'payload.query = query || "";',
      'payload.context = typeof output === "string" ? output : JSON.stringify(output || "");',
      'JSON.stringify(payload);'
    ].join('\n')
  },
  textClean: {
    label: '清理与截断文本',
    script: [
      'var text = String(output || query || "").trim();',
      'text = text.replace(/\\n{3,}/g, "\\n\\n");',
      'if (text.length > 2000) {',
      '  text = text.substring(0, 2000) + "…";',
      '}',
      'text;'
    ].join('\n')
  },
  template: {
    label: '简单模板填充',
    script: [
      'var tpl = "请根据以下内容回答：\\n\\n【问题】\\n{{query}}\\n\\n【资料】\\n{{output}}";',
      'var result = tpl',
      '  .replace("{{query}}", query || "")',
      '  .replace("{{output}}", typeof output === "string" ? output : JSON.stringify(output || ""));',
      'result;'
    ].join('\n')
  },
  listJoin: {
    label: '多行文本转列表摘要',
    script: [
      'var lines = String(output || "")',
      '  .split(/\\r?\\n/)',
      '  .map(function (s) { return s.trim(); })',
      '  .filter(function (s) { return s.length > 0; });',
      'var result = !lines.length',
      '  ? "（无内容）"',
      '  : lines.map(function (s, i) { return (i + 1) + ". " + s; }).join("\\n");',
      'result;'
    ].join('\n')
  }
}

const PY_CODE_EXAMPLES = {
  passThrough: {
    label: '透传上游 output',
    script: ['# 直接把上游结果传给下游', 'output'].join('\n')
  },
  concat: {
    label: '拼接查询与上游结果',
    script: [
      '# query 为开始节点系统参数；output 为上游节点结果',
      'q = query or ""',
      'prev = output if isinstance(output, str) else ("" if output is None else str(output))',
      '("问题：" + q + "\\n\\n上下文：\\n" + prev) if prev else q'
    ].join('\n')
  },
  jsonPick: {
    label: '解析 JSON 取字段',
    script: [
      '# 上游 JSON 已自动解析为 obj',
      'name = (obj or {}).get("name", "") if isinstance(obj, dict) else ""',
      '"姓名：" + str(name)'
    ].join('\n')
  },
  jsonBuild: {
    label: '组装 JSON 对象',
    script: [
      'import json',
      'payload = {',
      '  "query": query or "",',
      '  "context": output if isinstance(output, str) else output,',
      '}',
      'json.dumps(payload, ensure_ascii=False)'
    ].join('\n')
  },
  textClean: {
    label: '清理与截断文本',
    script: [
      'import re',
      'text = str(output or query or "").strip()',
      'text = re.sub(r"\\n{3,}", "\\n\\n", text)',
      'text[:2000] + ("…" if len(text) > 2000 else "")'
    ].join('\n')
  },
  template: {
    label: '简单模板填充',
    script: [
      'ctx = output if isinstance(output, str) else ("" if output is None else str(output))',
      '("请根据以下内容回答：\\n\\n【问题】\\n%s\\n\\n【资料】\\n%s" % (query or "", ctx))'
    ].join('\n')
  },
  listJoin: {
    label: '多行文本转列表摘要',
    script: [
      'raw = output if isinstance(output, str) else ("" if output is None else str(output))',
      'lines = [s.strip() for s in raw.splitlines() if s.strip()]',
      '"（无内容）" if not lines else "\\n".join("%d. %s" % (i + 1, s) for i, s in enumerate(lines))'
    ].join('\n')
  }
}

const codeExampleKey = ref(null)

const codeExampleOptions = computed(() => {
  const map = codeLanguage.value === 'python' ? PY_CODE_EXAMPLES : JS_CODE_EXAMPLES
  return Object.keys(map).map((key) => ({
    label: map[key].label,
    value: key
  }))
})

function onCodeLanguageChange(v) {
  const lang = v === 'python' || v === 'py' ? 'python' : 'js'
  codeExampleKey.value = null
  patchJson({ language: lang })
}

function applyCodeExample(key) {
  const map = codeLanguage.value === 'python' ? PY_CODE_EXAMPLES : JS_CODE_EXAMPLES
  const ex = map[key]
  if (!ex) return
  codeExampleKey.value = key
  patchJson({ language: codeLanguage.value, script: ex.script })
}

function onCodeExampleSelect(key) {
  if (!key) {
    codeExampleKey.value = null
    return
  }
  applyCodeExample(key)
}

function patchLlm(patch) {
  emitConfig(JSON.stringify({ ...llmCfg.value, ...patch }))
}

function onLlmProviderChange(v) {
  patchLlm({ providerId: v ?? null, modelCode: '' })
}

function sanitizeKey(v) {
  return String(v || '')
    .trim()
    .replace(/[^a-zA-Z0-9_]/g, '_')
}

function emitStartInputs(list) {
  emitConfig(JSON.stringify({ inputs: ensureSystemInputs(list) }))
}

function patchInput(idx, patch) {
  const cur = startInputs.value[idx]
  if (!cur || isSystemStartInput(cur)) return
  const next = { ...patch }
  if (next.key === SYSTEM_START_INPUT_KEY) {
    next.key = cur.key
  }
  const list = startInputs.value.map((x, i) => (i === idx ? { ...x, ...next } : { ...x }))
  emitStartInputs(list)
}

function addInput() {
  const list = [...startInputs.value]
  let n = list.length
  let key = `arg${n}`
  const keys = new Set(list.map((x) => x.key))
  while (keys.has(key) || key === SYSTEM_START_INPUT_KEY) {
    n += 1
    key = `arg${n}`
  }
  list.push({
    key,
    label: `参数${n}`,
    required: false,
    defaultValue: '',
    system: false
  })
  emitStartInputs(list)
}

function removeInput(idx) {
  const cur = startInputs.value[idx]
  if (!cur || isSystemStartInput(cur)) return
  const list = startInputs.value.filter((_, i) => i !== idx)
  emitStartInputs(list)
}
</script>

<style scoped>
.code-example-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.code-example-row > :first-child {
  flex: 1;
  min-width: 0;
}
.hint {
  margin: 0 0 8px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-muted, #8a94a6);
}
.hint code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  padding: 0 3px;
  border-radius: 3px;
  background: color-mix(in srgb, var(--color-fill-secondary, #f0f1f3) 90%, transparent);
}
.inputs-block {
  margin-top: 4px;
}
.inputs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.inputs-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text, #1f2329);
}
.inputs-empty {
  font-size: 12px;
  color: var(--color-text-muted, #8a94a6);
  padding: 8px 0 12px;
}
.input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  grid-template-areas:
    'key label'
    'def def'
    'actions actions';
  gap: 8px;
  align-items: center;
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 90%, transparent);
  background: color-mix(in srgb, var(--color-fill-secondary, #f5f6f8) 45%, transparent);
}
.cell-key {
  grid-area: key;
}
.cell-label {
  grid-area: label;
}
.cell-default {
  grid-area: def;
}
.cell-actions {
  grid-area: actions;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 2px;
}
.mono :deep(textarea),
.mono :deep(input) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
.param-row {
  margin-bottom: 14px;
}
.param-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--color-text, #1f2329);
}
.param-label b {
  font-variant-numeric: tabular-nums;
  color: var(--color-text-muted, #667085);
}
.form-fields-block {
  margin-bottom: 14px;
}
.form-field-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr) auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.condition-row {
  grid-template-columns: 72px minmax(0, 1fr) auto;
}
.condition-kind {
  font-size: 12px;
  font-weight: 600;
  color: #7a5c3a;
}
.condition-else-hint {
  font-size: 12px;
  color: var(--color-text-muted, #8a94a6);
}
.condition-spacer {
  width: 40px;
}
.else-row {
  padding: 4px 0 2px;
}
.switch-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--color-text-muted, #8a94a6);
}
</style>
