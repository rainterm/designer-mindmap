<template>
  <Dialog
    :show="show"
    title="历史版本"
    size="xl"
    :show-footer="false"
    content-class="app-dialog--agent-history"
    @update:show="onShowUpdate"
  >
    <div class="agent-history">
      <!-- 左：版本列表 -->
      <aside class="history-left">
        <div class="pane-head">
          <span class="pane-title">版本列表</span>
          <n-tag size="small" :bordered="false">{{ totalText }}</n-tag>
        </div>
        <div v-if="loading && !rows.length" class="pane-empty">加载中…</div>
        <div v-else-if="!rows.length" class="pane-empty">暂无历史版本（发布后才会生成快照）</div>
        <ul v-else class="version-list">
          <li
            v-for="r in rows"
            :key="r.id || r.version"
            class="version-item"
            :class="{ active: selected && selected.version === r.version }"
            @click="select(r)"
          >
            <div class="version-row1">
              <span class="version-no">v{{ r.version }}</span>
              <span class="version-time">{{ r.publishedAt || '-' }}</span>
            </div>
            <div class="version-row2">
              <span class="version-by">{{ r.publishedBy || '系统' }}</span>
              <n-button
                size="small"
                type="primary"
                ghost
                :loading="rollingId === r.version"
                @click.stop="confirmRollback(r)"
              >
                回滚
              </n-button>
            </div>
          </li>
        </ul>
        <div v-if="total > pageSize" class="pane-pager">
          <n-pagination
            v-model:page="page"
            :page-count="Math.max(1, Math.ceil(total / pageSize))"
            :page-size="pageSize"
            size="small"
            show-size-picker
            :page-sizes="[10, 20, 50]"
            @update:page="loadList"
            @update:page-size="onPageSizeChange"
          />
        </div>
      </aside>

      <!-- 右：版本配置预览 -->
      <section class="history-right">
        <div class="pane-head">
          <span class="pane-title">
            <template v-if="selected">版本预览 · v{{ selected.version }}</template>
            <template v-else>版本预览</template>
          </span>
          <n-tag v-if="selected" size="small" :bordered="false" type="info">
            {{ selected.publishedBy || '系统' }} · {{ selected.publishedAt || '-' }}
          </n-tag>
        </div>
        <div v-if="!selected" class="pane-empty">选择左侧版本查看配置</div>
        <div v-else-if="detailLoading" class="pane-empty">加载快照中…</div>
        <div v-else class="preview-body">
          <n-descriptions label-placement="left" :column="2" size="small" bordered>
            <n-descriptions-item label="模型">
              {{ config.modelCode || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="供应商 ID">
              {{ config.providerId ?? '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="Temperature">
              {{ config.temperature ?? '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="Top P">
              {{ config.topP ?? '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="Max Tokens">
              {{ config.maxTokens > 0 ? config.maxTokens : '不限制' }}
            </n-descriptions-item>
            <n-descriptions-item label="记忆轮数">
              {{ config.memoryRounds ?? '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="绑定工具">
              {{ idCount(config.toolIds) }}
            </n-descriptions-item>
            <n-descriptions-item label="绑定 MCP">
              {{ idCount(config.mcpIds) }}
            </n-descriptions-item>
            <n-descriptions-item label="绑定 Skill">
              {{ idCount(config.skillIds) }}
            </n-descriptions-item>
            <n-descriptions-item label="绑定工作流">
              {{ idCount(config.workflowIds) }}
            </n-descriptions-item>
            <n-descriptions-item label="绑定知识库">
              {{ idCount(config.knowledgeIds) }}
            </n-descriptions-item>
            <n-descriptions-item label="附件上传">
              {{ config.fileEnabled === 1 ? '开启' : '关闭' }}
            </n-descriptions-item>
            <n-descriptions-item label="RAG Top K">
              {{ config.ragTopK ?? '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="相似度阈值">
              {{ config.ragScoreThreshold ?? '-' }}
            </n-descriptions-item>
          </n-descriptions>
          <div class="prompt-block">
            <div class="prompt-title">系统提示词</div>
            <pre class="prompt-text">{{ config.systemPrompt || '（空）' }}</pre>
          </div>
        </div>
      </section>
    </div>
  </Dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import Dialog from '@/components/Dialog.vue'
import {
  getAgentVersion,
  pageAgentHistory,
  rollbackAgent
} from '@/modules/agent/api'

const props = defineProps({
  show: { type: Boolean, default: false },
  agentId: { type: [Number, String], default: null },
  /** 当前已发布版本号（提示语展示用） */
  currentVersion: { type: Number, default: null }
})
const emit = defineEmits(['update:show', 'rolled-back'])

const message = useMessage()
const dialog = useDialog()

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const selected = ref(null)
const detailLoading = ref(false)
const config = ref({})
const rollingId = ref(null)

const totalText = computed(() => `共 ${total.value} 个版本`)

function onShowUpdate(v) {
  emit('update:show', v)
  if (!v) {
    rows.value = []
    selected.value = null
    config.value = {}
  }
}

async function loadList() {
  if (!props.agentId) return
  loading.value = true
  try {
    const res = await pageAgentHistory(props.agentId, {
      pageNum: page.value,
      pageSize: pageSize.value
    })
    const p = res.data || {}
    rows.value = p.records || []
    total.value = p.total || 0
    // 默认选中当前已发布版本（若存在），否则选第一个
    if (!selected.value && rows.value.length) {
      const cur = rows.value.find((r) => r.version === props.currentVersion) || rows.value[0]
      select(cur)
    }
  } catch (e) {
    message.error(e?.msg || e?.message || '加载历史版本失败')
  } finally {
    loading.value = false
  }
}

function onPageSizeChange(sz) {
  pageSize.value = sz
  page.value = 1
  loadList()
}

async function select(row) {
  selected.value = row
  detailLoading.value = true
  try {
    const res = await getAgentVersion(props.agentId, row.version)
    config.value = res.data?.config || {}
  } catch (e) {
    message.error(e?.msg || e?.message || '加载版本详情失败')
    config.value = {}
  } finally {
    detailLoading.value = false
  }
}

function confirmRollback(row) {
  dialog.warning({
    title: '回滚历史版本',
    content: `将当前草稿的配置覆盖为 v${row.version} 的内容（名称等基本信息不变）；已发布版本 v${props.currentVersion ?? '-'} 的运行不受影响。`,
    positiveText: '回滚',
    negativeText: '取消',
    onPositiveClick: () => doRollback(row)
  })
}

async function doRollback(row) {
  rollingId.value = row.version
  try {
    await rollbackAgent(props.agentId, row.version)
    // 回滚成功即关闭弹窗，露出编辑表单让用户继续编辑
    emit('update:show', false)
    emit('rolled-back', row.version)
  } catch (e) {
    message.error(e?.msg || e?.message || '回滚失败')
  } finally {
    rollingId.value = null
  }
}

function idCount(ids) {
  const n = Array.isArray(ids) ? ids.length : 0
  return n ? `${n} 项` : '未绑定'
}

watch(
  () => [props.show, props.agentId],
  ([s]) => {
    if (s && props.agentId) {
      page.value = 1
      loadList()
    }
  }
)
</script>

<style scoped>
.agent-history {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 14px;
  height: 70vh;
  min-height: 520px;
}
.history-left,
.history-right {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--color-bg, #fff);
  border-radius: var(--radius, 8px);
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 80%, transparent);
}
.pane-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.pane-title {
  font-size: 13px;
  font-weight: 600;
}
.pane-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted, #8a94a6);
  font-size: 12px;
}
.version-list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow: auto;
  flex: 1;
  min-height: 0;
}
.version-item {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  margin-bottom: 8px;
  cursor: pointer;
  background: color-mix(in srgb, var(--color-fill-secondary, #f5f6f8) 60%, transparent);
  transition: border-color 0.15s;
}
.version-item:hover {
  border-color: color-mix(in srgb, var(--color-border, #d0d5dd) 90%, transparent);
}
.version-item.active {
  border-color: var(--color-primary, #18a058);
  background: color-mix(in srgb, var(--color-primary, #18a058) 6%, transparent);
}
.version-row1 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.version-no {
  font-size: 14px;
  font-weight: 600;
}
.version-time {
  font-size: 11px;
  color: var(--color-text-muted, #8a94a6);
}
.version-row2 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}
.version-by {
  font-size: 12px;
  color: var(--color-text-muted, #8a94a6);
}
.pane-pager {
  margin-top: 8px;
  display: flex;
  justify-content: center;
}
.preview-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.prompt-block {
  margin-top: 12px;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 80%, transparent);
  background: color-mix(in srgb, var(--color-fill-secondary, #f5f6f8) 50%, transparent);
  padding: 10px 12px;
}
.prompt-title {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
}
.prompt-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.55;
  max-height: 240px;
  overflow: auto;
}
</style>
