<template>
  <n-card :bordered="false">
    <TableToolbar v-if="hasToolbar">
      <template v-if="$slots['toolbar-left']" #left>
        <slot name="toolbar-left" />
      </template>
      <template v-if="$slots['toolbar-right']" #right>
        <slot name="toolbar-right" />
      </template>
    </TableToolbar>

    <n-data-table v-bind="tableAttrs">
      <template v-for="(_, name) in tableSlots" :key="name" #[name]="slotData">
        <slot :name="name" v-bind="slotData || {}" />
      </template>
    </n-data-table>

    <PaginationBar
      v-if="showPagination"
      :page="paginationState.page"
      :page-size="paginationState.pageSize"
      :item-count="paginationState.itemCount"
      :page-sizes="paginationState.pageSizes || [10, 20, 50]"
      @update:page="onPage"
      @update:page-size="onPageSize"
    />
  </n-card>
</template>

<script setup>
import { computed, isRef, unref, useAttrs, useSlots } from 'vue'
import PaginationBar from '@/components/PaginationBar.vue'
import TableToolbar from '@/components/TableToolbar.vue'

defineOptions({ inheritAttrs: false })

/** 固定列阴影等横向余量 */
const SCROLL_X_EXTRA = 48

const props = defineProps({
  /** usePagination 返回值；传入后自动绑定 data / loading / 分页 / 勾选 */
  pager: { type: Object, default: null },
  /** `{ page, pageSize, itemCount, pageSizes? }`；传 `false` 隐藏分页。有 pager 时默认自动生成 */
  pagination: {
    type: [Object, Boolean],
    default: undefined
  },
  /** 表格单元格边框，默认开启 */
  bordered: {
    type: Boolean,
    default: true
  },
  /** 是否单行省略；开边框时建议 false */
  singleLine: {
    type: Boolean,
    default: false
  },
  /**
   * 未显式传 scroll-x 时，按 columns 的 width/width 求和自动计算。
   * 传 false 关闭自动计算。
   */
  autoScrollX: {
    type: Boolean,
    default: true
  },
  /** 有 pager 时默认 remote；可覆盖 */
  remote: {
    type: Boolean,
    default: undefined
  },
  rowKey: {
    type: [Function, String],
    default: undefined
  }
})

const emit = defineEmits(['update:page', 'update:page-size', 'update:checkedRowKeys'])

const attrs = useAttrs()
const slots = useSlots()

const hasToolbar = computed(() => !!slots['toolbar-left'] || !!slots['toolbar-right'])

const paginationState = computed(() => {
  if (props.pagination === false) return null
  if (props.pagination && typeof props.pagination === 'object') return props.pagination
  if (props.pager) {
    if (props.pager.paged === false) return null
    return {
      page: props.pager.query.pageNum,
      pageSize: props.pager.query.pageSize,
      itemCount: unref(props.pager.total),
      pageSizes: props.pager.pageSizes
    }
  }
  return null
})

const showPagination = computed(() => paginationState.value != null)

const reservedSlotNames = new Set(['toolbar-left', 'toolbar-right'])
const tableSlots = computed(() => {
  const result = {}
  for (const name of Object.keys(slots)) {
    if (!reservedSlotNames.has(name)) result[name] = slots[name]
  }
  return result
})

function sumColumnWidth(columns) {
  if (!Array.isArray(columns)) return undefined
  const sum = columns.reduce(
    (acc, col) => acc + (Number(col?.width) || Number(col?.width) || 0),
    0
  )
  return sum > 0 ? sum + SCROLL_X_EXTRA : undefined
}

function defaultRowKey(row) {
  return row.id
}

const tableAttrs = computed(() => {
  const pager = props.pager
  const merged = {
    bordered: props.bordered,
    singleLine: props.singleLine,
    remote: props.remote ?? !!pager,
    rowKey: props.rowKey ?? (pager ? defaultRowKey : undefined),
    ...(pager
      ? {
          data: unref(pager.rows),
          loading: unref(pager.loading),
          checkedRowKeys: unref(pager.checkedRowKeys),
          'onUpdate:checkedRowKeys': (keys) => {
            if (isRef(pager.checkedRowKeys)) pager.checkedRowKeys.value = keys
            emit('update:checkedRowKeys', keys)
          },
          ...(typeof pager.handleSorterChange === 'function'
            ? {
                'onUpdate:sorter': (sorter) => pager.handleSorterChange(sorter)
              }
            : {})
        }
      : {}),
    ...attrs
  }

  const hasScrollX = merged.scrollX != null || merged['scroll-x'] != null
  if (!hasScrollX && props.autoScrollX) {
    const auto = sumColumnWidth(merged.columns)
    if (auto != null) merged.scrollX = auto
  }
  return merged
})

function onPage(page) {
  props.pager?.handlePageChange?.(page)
  emit('update:page', page)
}

function onPageSize(size) {
  props.pager?.handlePageSizeChange?.(size)
  emit('update:page-size', size)
}
</script>
