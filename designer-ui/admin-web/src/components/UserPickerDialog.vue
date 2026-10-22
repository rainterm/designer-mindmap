<template>
  <Dialog
    v-model:show="visible"
    :title="title"
    size="lg"
    confirm-text="确定"
    :loading="submitting"
    @confirm="handleConfirm"
    @after-leave="resetState"
  >
    <div class="user-picker">
      <div class="user-picker-search">
        <n-input
          v-model:value="keyword"
          clearable
          placeholder="搜索账号 / 昵称"
          @keyup.enter="load(1)"
        />
        <n-button type="primary" :loading="loading" @click="load(1)">查询</n-button>
      </div>
      <n-data-table
        :columns="columns"
        :data="rows"
        :loading="loading"
        :row-key="(row) => row.id"
        :checked-row-keys="multiple ? checkedKeys : undefined"
        :row-props="rowProps"
        :max-height="360"
        :bordered="true"
        :single-line="false"
        @update:checked-row-keys="onCheckedKeys"
      />
      <div class="user-picker-footer">
        <span class="user-picker-tip">
          <template v-if="multiple">已选 {{ checkedKeys.length }} 人</template>
          <template v-else>{{ selectedRow ? `已选：${displayName(selectedRow)}` : '请选择人员' }}</template>
        </span>
        <PaginationBar
          :page="page"
          :page-size="pageSize"
          :item-count="total"
          :show-size-picker="false"
          :show-quick-jumper="false"
          @update:page="load"
        />
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'
import { NRadio, useMessage } from 'naive-ui'
import Dialog from '@/components/Dialog.vue'
import { pageUserOptions } from '@/modules/system/api/system'

const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: '选择人员' },
  /** 多选 */
  multiple: { type: Boolean, default: false },
  /** 排除的用户 ID */
  excludeIds: { type: Array, default: () => [] },
  /** 确认中（外层提交态，可选） */
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['update:show', 'confirm'])

const message = useMessage()
const visible = computed({
  get: () => props.show,
  set: (v) => emit('update:show', v)
})

const keyword = ref('')
const loading = ref(false)
const rows = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const selectedId = ref(null)
const selectedRow = ref(null)
const checkedKeys = ref([])
const checkedMap = ref(new Map())

const excludeSet = computed(() => new Set((props.excludeIds || []).map((id) => String(id))))

function displayName(u) {
  if (!u) return '-'
  if (u.nickname && u.nickname !== u.username) return `${u.nickname}（${u.username}）`
  return u.username || u.nickname || String(u.id)
}

const columns = computed(() => {
  const cols = []
  if (props.multiple) {
    cols.push({ type: 'selection', multiple: true, disabled: (row) => excludeSet.value.has(String(row.id)) })
  } else {
    cols.push({
      title: '',
      key: '_radio',
      width: 48,
      render: (row) =>
        h(NRadio, {
          checked: selectedId.value === row.id,
          disabled: excludeSet.value.has(String(row.id)),
          onUpdateChecked: () => selectOne(row)
        })
    })
  }
  cols.push(
    { title: '账号', key: 'username', width: 120, ellipsis: { tooltip: true } },
    { title: '昵称', key: 'nickname', width: 120, ellipsis: { tooltip: true } },
    {
      title: '部门',
      key: 'deptName',
      width: 140,
      ellipsis: { tooltip: true },
      render: (row) => row.deptName || '-'
    }
  )
  return cols
})

function selectOne(row) {
  if (excludeSet.value.has(String(row.id))) return
  selectedId.value = row.id
  selectedRow.value = row
}

function onCheckedKeys(keys) {
  const next = keys.filter((id) => !excludeSet.value.has(String(id)))
  checkedKeys.value = next
  const map = new Map(checkedMap.value)
  for (const row of rows.value) {
    if (next.includes(row.id)) map.set(row.id, row)
    else map.delete(row.id)
  }
  // 去掉已不在 keys 里的
  for (const id of [...map.keys()]) {
    if (!next.includes(id)) map.delete(id)
  }
  checkedMap.value = map
}

function rowProps(row) {
  const disabled = excludeSet.value.has(String(row.id))
  return {
    style: disabled ? 'opacity: 0.45; cursor: not-allowed' : 'cursor: pointer',
    onClick: () => {
      if (disabled) return
      if (props.multiple) {
        const set = new Set(checkedKeys.value)
        if (set.has(row.id)) set.delete(row.id)
        else set.add(row.id)
        onCheckedKeys([...set])
      } else {
        selectOne(row)
      }
    },
    onDblclick: () => {
      if (disabled || props.multiple) return
      selectOne(row)
      handleConfirm()
    }
  }
}

async function load(p = page.value) {
  page.value = p
  loading.value = true
  try {
    const res = await pageUserOptions({
      pageNum: page.value,
      pageSize,
      keyword: keyword.value.trim() || undefined
    })
    const data = res.data || {}
    rows.value = data.records || []
    total.value = data.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleConfirm() {
  if (props.multiple) {
    if (!checkedKeys.value.length) {
      message.warning('请选择人员')
      return
    }
    const list = checkedKeys.value.map((id) => checkedMap.value.get(id)).filter(Boolean)
    emit('confirm', list)
    visible.value = false
    return
  }
  if (!selectedRow.value) {
    message.warning('请选择人员')
    return
  }
  emit('confirm', selectedRow.value)
  visible.value = false
}

function resetState() {
  keyword.value = ''
  rows.value = []
  page.value = 1
  total.value = 0
  selectedId.value = null
  selectedRow.value = null
  checkedKeys.value = []
  checkedMap.value = new Map()
}

watch(
  () => props.show,
  (show) => {
    if (show) load(1)
  }
)
</script>

<style scoped>
.user-picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.user-picker-search {
  display: flex;
  gap: 8px;
}
.user-picker-search .n-input {
  flex: 1;
}
.user-picker-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.user-picker-footer :deep(.pagination-wrap) {
  margin-top: 0;
  padding-top: 0;
}
.user-picker-tip {
  font-size: 13px;
  color: #646a73;
}
</style>
