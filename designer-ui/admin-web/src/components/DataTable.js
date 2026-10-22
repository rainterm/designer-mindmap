import { defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { NDataTable } from 'naive-ui'
import TableEmpty from '@/components/TableEmpty.vue'

/**
 * 包装 Naive DataTable：
 * - 默认空状态
 * - 自动计算表体高度（Naive 的 min/maxHeight 不含表头）
 * - 同时设置 minHeight + maxHeight：数据少也撑满到底，数据多则表体内滚动
 * - 传显式 :max-height / :min-height，或 :flex-height="false" 可关闭自动计算
 */
export default defineComponent({
  name: 'NDataTable',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    const tableHeight = ref(undefined)
    let tableEl = null
    let ro = null

    function resolveEl(inst) {
      if (!inst) return null
      const el = inst?.$el || inst?.el || inst
      return el instanceof Element ? el : null
    }

    function shouldAutoHeight() {
      const flex = attrs.flexHeight ?? attrs['flex-height']
      if (flex === false || flex === 'false') return false
      if (attrs.maxHeight != null || attrs['max-height'] != null) return false
      if (attrs.minHeight != null || attrs['min-height'] != null) return false
      return true
    }

    function findPagination(el) {
      const host = el.closest('.n-card, .page-container') || el.parentElement
      return host?.querySelector('.pagination-wrap') || null
    }

    function measure() {
      if (!shouldAutoHeight() || !tableEl) return

      const header = tableEl.querySelector('.n-data-table-base-table-header') || tableEl.querySelector('.n-data-table-thead')
      const headerH = header ? header.getBoundingClientRect().height : 40
      const hasScrollX = attrs.scrollX != null || attrs['scroll-x'] != null
      const hScroll = hasScrollX ? 12 : 0

      const pag = findPagination(tableEl)
      const pagH = pag ? pag.getBoundingClientRect().height + 8 : 12
      const bottomReserve = 16
      const top = tableEl.getBoundingClientRect().top
      const next = Math.floor(window.innerHeight - top - headerH - pagH - hScroll - bottomReserve)
      const clamped = next > 160 ? next : 160
      if (tableHeight.value !== clamped) {
        tableHeight.value = clamped
      }

      nextTick(() => {
        if (!pag || !tableEl) return
        const overflow = Math.ceil(pag.getBoundingClientRect().bottom - (window.innerHeight - 8))
        if (overflow > 0) {
          const adjusted = Math.max(160, (tableHeight.value || clamped) - overflow)
          if (tableHeight.value !== adjusted) {
            tableHeight.value = adjusted
          }
        }
      })
    }

    /**
     * 观察会影响表格可用高度的元素（通用推导，不依赖具体页面的 class）：
     * - 各层祖先节点之前的兄弟节点：查询区、左侧树等，高度变化会推移表格顶部
     * - 分页条本身：高度变化（如换行）会挤压表体
     * 表格顶部位置只受前置内容影响，配合 measure() 中的等值判断不会形成循环。
     */
    function observeLayoutSources() {
      if (!ro || !tableEl) return
      const page = tableEl.closest('.page-container')
      const stop = page || tableEl.parentElement
      for (let node = tableEl; node; node = node.parentElement) {
        for (let sib = node.previousElementSibling; sib; sib = sib.previousElementSibling) {
          ro.observe(sib)
        }
        if (node === stop) break
      }
      const pag = findPagination(tableEl)
      if (pag) ro.observe(pag)
    }

    function bind(inst) {
      tableEl = resolveEl(inst)
      nextTick(() => {
        observeLayoutSources()
        measure()
        requestAnimationFrame(measure)
      })
    }

    onMounted(() => {
      nextTick(() => {
        measure()
        requestAnimationFrame(measure)
      })
      window.addEventListener('resize', measure)
      ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(measure) : null
      if (ro && document.body) ro.observe(document.body)
      // 监听影响表格顶部/底部空间的元素，避免其高度变化后底部留白或溢出
      nextTick(() => {
        observeLayoutSources()
      })
    })

    onBeforeUnmount(() => {
      window.removeEventListener('resize', measure)
      ro?.disconnect()
      ro = null
      tableEl = null
    })

    watch(
      () => [attrs.data, attrs.loading, attrs.columns],
      () => {
        // 用语句块而不是直接返回 nextTick 的 Promise：
        // watch 回调的返回值会被当作清理函数处理，返回 Promise 语义上无意义。
        nextTick(() => {
          measure()
          requestAnimationFrame(measure)
        })
      }
    )

    return () => {
      const props = { ...attrs, flexHeight: false, ref: bind }
      if (shouldAutoHeight() && tableHeight.value != null) {
        props.minHeight = tableHeight.value
        props.maxHeight = tableHeight.value
      }
      return h(NDataTable, props, {
        ...slots,
        empty: slots.empty || (() => h(TableEmpty))
      })
    }
  }
})
