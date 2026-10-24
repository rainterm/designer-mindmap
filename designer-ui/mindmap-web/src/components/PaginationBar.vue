<template>
  <div v-if="visible" class="pagination-wrap">
    <n-pagination
      :page="currentPage"
      :page-size="currentPageSize"
      :item-count="currentItemCount"
      :page-sizes="currentPageSizes"
      :show-size-picker="showSizePicker && !simple"
      :show-quick-jumper="showQuickJumper && !simple"
      :simple="simple"
      @update:page="onPage"
      @update:page-size="onPageSize"
    />
  </div>
</template>

<script setup>
import { computed, unref } from 'vue'

/**
 * 统一分页条：可接 usePagination 的 pager，或手动传 page / pageSize / itemCount。
 * 根节点保留 class="pagination-wrap"，供表格高度计算识别。
 */
const props = defineProps({
  /** usePagination 返回值 */
  pager: { type: Object, default: null },
  page: { type: Number, default: undefined },
  pageSize: { type: Number, default: undefined },
  itemCount: { type: Number, default: undefined },
  pageSizes: { type: Array, default: undefined },
  showSizePicker: { type: Boolean, default: true },
  showQuickJumper: { type: Boolean, default: true },
  /** 简洁模式（仅翻页） */
  simple: { type: Boolean, default: false },
  /** itemCount 为 0 时隐藏 */
  hideOnEmpty: { type: Boolean, default: false }
})

const emit = defineEmits(['update:page', 'update:pageSize', 'update:page-size'])

const currentPage = computed(() => {
  if (props.pager?.query) return props.pager.query.pageNum ?? 1
  return props.page ?? 1
})

const currentPageSize = computed(() => {
  if (props.pager?.query) return props.pager.query.pageSize ?? 10
  return props.pageSize ?? 10
})

const currentItemCount = computed(() => {
  if (props.pager) return Number(unref(props.pager.total) || 0)
  return Number(props.itemCount || 0)
})

const currentPageSizes = computed(() => {
  if (props.pageSizes?.length) return props.pageSizes
  if (props.pager?.pageSizes?.length) return props.pager.pageSizes
  return [10, 20, 50]
})

const visible = computed(() => {
  if (props.pager?.paged === false) return false
  if (props.hideOnEmpty && currentItemCount.value <= 0) return false
  return true
})

function onPage(page) {
  if (typeof props.pager?.handlePageChange === 'function') {
    props.pager.handlePageChange(page)
  }
  emit('update:page', page)
}

function onPageSize(size) {
  if (typeof props.pager?.handlePageSizeChange === 'function') {
    props.pager.handlePageSizeChange(size)
  }
  emit('update:pageSize', size)
  emit('update:page-size', size)
}
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-shrink: 0;
  margin-top: 8px;
  padding-top: 4px;
  box-sizing: border-box;
}
</style>
