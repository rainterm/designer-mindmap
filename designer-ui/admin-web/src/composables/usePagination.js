import { onMounted, reactive, ref } from 'vue'

/** 去掉空字符串 / null / undefined */
export function cleanParams(query) {
  const out = {}
  for (const [key, value] of Object.entries(query || {})) {
    if (value === '' || value === null || value === undefined) continue
    out[key] = value
  }
  return out
}

function normalizeResult(res) {
  if (!res) return { records: [], total: 0 }
  if (Array.isArray(res.records) || typeof res.total === 'number') {
    return { records: res.records || [], total: res.total || 0 }
  }
  const data = res.data ?? res
  return {
    records: data?.records || data?.list || [],
    total: data?.total ?? 0
  }
}

/**
 * 列表页分页查询。返回值可直接作为 SearchBar / PageTable 的 :pager。
 * fetcher(query) 可返回 { records, total }，或 axios 风格 { data: { records, total } }。
 *
 * @param {Function} fetcher
 * @param {object} [options]
 * @param {object} [options.defaults]
 * @param {number} [options.pageSize=10]
 * @param {number[]} [options.pageSizes]
 * @param {boolean} [options.immediate=true]
 * @param {boolean} [options.clean=true]
 * @param {boolean} [options.paged=true] false 时不带分页参数（全量列表）
 * @param {boolean} [options.remoteSort=false] 启用 sortField/sortOrder
 */
export function usePagination(fetcher, options = {}) {
  const {
    defaults = {},
    pageSize = 10,
    pageSizes = [10, 20, 50],
    immediate = true,
    clean = true,
    paged = true,
    remoteSort = false
  } = options

  const query = reactive({
    ...(paged ? { pageNum: 1, pageSize } : {}),
    ...(remoteSort ? { sortField: null, sortOrder: null } : {}),
    ...defaults
  })

  const rows = ref([])
  const total = ref(0)
  const loading = ref(false)
  const checkedRowKeys = ref([])

  async function loadData() {
    loading.value = true
    try {
      const params = clean ? cleanParams(query) : { ...query }
      const res = await fetcher(params)
      const normalized = normalizeResult(res)
      rows.value = normalized.records
      total.value = normalized.total
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    if (paged) query.pageNum = 1
    loadData()
  }

  function handleReset() {
    Object.assign(query, {
      ...(paged ? { pageNum: 1, pageSize: query.pageSize } : {}),
      ...(remoteSort ? { sortField: null, sortOrder: null } : {}),
      ...defaults
    })
    checkedRowKeys.value = []
    loadData()
  }

  function handlePageChange(page) {
    if (!paged) return
    query.pageNum = page
    loadData()
  }

  function handlePageSizeChange(size) {
    if (!paged) return
    query.pageSize = size
    query.pageNum = 1
    loadData()
  }

  function handleSorterChange(sorter) {
    if (!remoteSort) return
    const s = Array.isArray(sorter) ? sorter.find((x) => x && x.order) || sorter[0] : sorter
    if (!s || !s.order) {
      query.sortField = null
      query.sortOrder = null
    } else {
      query.sortField = s.columnKey
      query.sortOrder = s.order
    }
    if (paged) query.pageNum = 1
    loadData()
  }

  function sortOrderOf(key) {
    return query.sortField === key ? query.sortOrder || false : false
  }

  function sortableCol(col) {
    return {
      ...col,
      sorter: true,
      sortOrder: sortOrderOf(col.key)
    }
  }

  function clearSelection() {
    checkedRowKeys.value = []
  }

  /** 导出等场景：当前筛选项（不含分页） */
  function getFilters() {
    const { pageNum: _n, pageSize: _s, ...rest } = query
    return clean ? cleanParams(rest) : { ...rest }
  }

  if (immediate) {
    onMounted(loadData)
  }

  return {
    query,
    rows,
    total,
    loading,
    checkedRowKeys,
    pageSizes,
    paged,
    loadData,
    handleSearch,
    handleReset,
    handlePageChange,
    handlePageSizeChange,
    handleSorterChange,
    sortOrderOf,
    sortableCol,
    clearSelection,
    getFilters
  }
}
