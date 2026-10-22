import { defineStore } from 'pinia'
import { ref } from 'vue'
import { MENU_BADGES } from '@/constants/menuBadge'
import { useUserStore } from '@/store/user'

/** 默认轮询间隔：红点不属于强实时指标，一分钟一次足够 */
const DEFAULT_INTERVAL = 60 * 1000

/**
 * 菜单红点数量中心。
 *
 * <p>定义来自各模块的 {@code menuBadges.js}（见 {@link MENU_BADGES}），这里只负责
 * 「按路径拉取 + 缓存 + 轮询」。侧栏通过 {@link countOf} 取数，并按菜单树自动汇总，
 * 因此新增红点只需要加一条定义，不用改布局或菜单组件。</p>
 *
 * <p>布局挂载时 {@link start}，卸载时 {@link stop}；业务动作（审核、删除等）之后调用
 * {@link refresh} 立即校正，不必等下一轮轮询。</p>
 */
export const useMenuBadgeStore = defineStore('menuBadge', () => {
  /** path -> 当前数量（未拉取到 / 无需展示时都是 0） */
  const counts = ref({})

  /** path -> 定义。启动时固定，不需要响应式 */
  const definitions = new Map()
  for (const def of MENU_BADGES) {
    if (def?.path && typeof def.load === 'function') {
      definitions.set(def.path, def)
    }
  }

  const paths = [...definitions.keys()]
  const timers = new Map()
  let visibilityHandler = null

  /** 该路径是否注册了红点：用来区分「0 条」与「没有红点」 */
  function isRegistered(path) {
    return definitions.has(path)
  }

  /** 取某个菜单路径的当前数量；未注册返回 undefined */
  function countOf(path) {
    if (!definitions.has(path)) return undefined
    return counts.value[path] || 0
  }

  async function refresh(path) {
    const def = definitions.get(path)
    if (!def) return
    // 没有菜单权限就不请求：既省一次接口，也避免权限不足时反复收到 403
    if (def.perm && !useUserStore().hasPerm(def.perm)) {
      counts.value[path] = 0
      return
    }
    try {
      const value = Number(await def.load())
      // 负数 / NaN 一律收敛成 0，避免把脏数据渲染到徽标上
      counts.value[path] = Number.isFinite(value) && value > 0 ? value : 0
    } catch {
      // 拉取失败保留上一次的数字，等下一轮轮询校正，不打断用户操作
    }
  }

  function refreshAll() {
    paths.forEach((path) => refresh(path))
  }

  /** 布局挂载：立即拉一次 + 按各自间隔轮询 + 回到前台补拉 */
  function start() {
    if (timers.size === 0) {
      for (const [path, def] of definitions) {
        const interval = def.interval ?? DEFAULT_INTERVAL
        // interval 为 0 / null 表示不轮询，只在业务动作后手动 refresh
        if (!interval) continue
        timers.set(
          path,
          window.setInterval(() => refresh(path), interval)
        )
      }
    }
    refreshAll()
    // 后台标签的定时器会被浏览器节流，切回前台时补一次
    if (!visibilityHandler && typeof document !== 'undefined') {
      visibilityHandler = () => {
        if (document.visibilityState === 'visible') refreshAll()
      }
      document.addEventListener('visibilitychange', visibilityHandler)
    }
  }

  function stop() {
    timers.forEach((id) => clearInterval(id))
    timers.clear()
    if (visibilityHandler) {
      document.removeEventListener('visibilitychange', visibilityHandler)
      visibilityHandler = null
    }
  }

  return {
    counts,
    isRegistered,
    countOf,
    refresh,
    refreshAll,
    start,
    stop
  }
})
