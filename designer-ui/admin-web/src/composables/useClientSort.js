import { computed, ref } from 'vue'

function compareVal(a, b) {
  if (a == null && b == null) return 0
  if (a == null) return -1
  if (b == null) return 1
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b), 'zh-CN', { numeric: true })
}

/**
 * 客户端表格排序（holding / allocation 等无分页列表）。
 *
 * @param {import('vue').Ref<any[]>|(() => any[])} rowsSource
 */
export function useClientSort(rowsSource) {
  const sortState = ref({ columnKey: null, order: false })

  function sortOrderOf(key) {
    return sortState.value.columnKey === key ? sortState.value.order || false : false
  }

  function sortableCol(col) {
    return {
      ...col,
      sorter: true,
      sortOrder: sortOrderOf(col.key)
    }
  }

  function handleSorterChange(sorter) {
    const s = Array.isArray(sorter) ? sorter.find((x) => x && x.order) || sorter[0] : sorter
    if (!s || !s.order) {
      sortState.value = { columnKey: null, order: false }
    } else {
      sortState.value = { columnKey: s.columnKey, order: s.order }
    }
  }

  const displayRows = computed(() => {
    const list = typeof rowsSource === 'function' ? rowsSource() : rowsSource?.value || []
    const { columnKey, order } = sortState.value
    if (!columnKey || !order) return list
    const copy = [...list]
    copy.sort((a, b) => {
      const cmp = compareVal(a?.[columnKey], b?.[columnKey])
      return order === 'descend' ? -cmp : cmp
    })
    return copy
  })

  return {
    sortState,
    sortOrderOf,
    sortableCol,
    handleSorterChange,
    displayRows
  }
}
