import { ref } from 'vue'

/**
 * 全局页面加载状态（供路由守卫驱动，App 顶层展示全屏 loading）。
 *
 * 设计要点：
 * - 延迟 200ms 才显示，避免快速导航时的闪烁
 * - 结束后短暂保留 150ms 做淡出，覆盖组件挂载前的一瞬空窗
 */
const loading = ref(false)

let showTimer = null
let hideTimer = null

export function startLoading() {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
  if (showTimer) return
  showTimer = setTimeout(() => {
    loading.value = true
    showTimer = null
  }, 200)
}

export function finishLoading() {
  if (showTimer) {
    clearTimeout(showTimer)
    showTimer = null
  }
  if (!loading.value) return
  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => {
    loading.value = false
    hideTimer = null
  }, 150)
}

export const pageLoading = loading
