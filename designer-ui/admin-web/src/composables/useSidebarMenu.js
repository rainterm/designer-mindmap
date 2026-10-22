import { computed, h, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NBadge, NIcon } from 'naive-ui'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import { useMenuBadgeStore } from '@/store/menuBadge'
import { resolveIcon } from '@/utils/icon'
import { openSsoClient } from '@/utils/sso'

/** 跳转页（编辑/详情/对话）不进侧栏 */
function isJumpPage(item) {
  if (item?.meta?.hidden) return true
  const p = String(item?.path || '')
  return /\/(edit|detail|chat)$/.test(p)
}

/**
 * 侧栏 / 顶栏菜单共用逻辑
 * @param {{
 *   collapsed?: import('vue').Ref<boolean>|import('vue').ComputedRef<boolean>
 *   scope?: import('vue').Ref<string>|import('vue').ComputedRef<string>|string
 *   iconBadge?: import('vue').Ref<boolean>|import('vue').ComputedRef<boolean>|boolean
 * }} [options] 图标栏（标题不可见）时是否把红点挂到图标上，见 {@link iconBadge}
 */
export function useSidebarMenu(options = {}) {
  const route = useRoute()
  const router = useRouter()
  const appStore = useAppStore()
  const userStore = useUserStore()
  const badgeStore = useMenuBadgeStore()

  /**
   * 菜单红点。
   *
   * <p>返回渲染函数而不是数量：数字变化时只有这一个节点重渲染，不必重建整棵菜单。
   * {@code showZero=false} 让数量为 0 时什么都不显示。</p>
   */
  function renderBadge(value) {
    return h(NBadge, {
      value,
      max: 99,
      showZero: false
    })
  }

  /** 节点自身是否注册了红点 */
  function hasOwnBadge(node) {
    return badgeStore.isRegistered(node?.path)
  }

  /**
   * 节点在菜单树里是否有红点（自身或子孙）。
   *
   * <p>只看子孙里有没有注册，不依赖当前数量：数量是动态的，若按「数量 > 0」决定
   * 挂不挂 extra，菜单结构会随轮询结果反复变化。</p>
   */
  function hasBadgeInTree(node) {
    if (hasOwnBadge(node)) return true
    return (node?.children || []).some(hasBadgeInTree)
  }

  /**
   * 节点要展示的红点数量。
   *
   * <p>自身注册了就用自身数量；否则汇总子孙，让折叠起来的父级菜单也能看到待办。
   * 汇总到最近的注册节点为止，避免嵌套注册时重复计数。</p>
   */
  function badgeTotal(node) {
    if (hasOwnBadge(node)) return badgeStore.countOf(node.path)
    return (node?.children || []).reduce((sum, child) => sum + badgeTotal(child), 0)
  }

  const collapsedRef = computed(() => {
    if (options.collapsed) return !!options.collapsed.value
    return appStore.collapsed
  })

  const scopeRef = computed(() => {
    const s = options.scope
    if (s && typeof s === 'object' && 'value' in s) return s.value || 'full'
    return s || 'full'
  })

  const activeKey = computed(() => route.path)
  const expandedKeys = computed({
    get: () => appStore.expandedKeys,
    set: (keys) => appStore.setExpandedKeys(keys)
  })

  /** 只有图标可见（侧栏折叠）时，红点改挂到图标上，否则会被隐藏的标题区一起藏掉 */
  const iconBadgeRef = computed(() => {
    const v = options.iconBadge
    if (v && typeof v === 'object' && 'value' in v) return !!v.value
    return !!v
  })

  /**
   * 菜单图标。
   *
   * <p>图标栏里红点只能压在图标上：折叠态下 naive 会给标题区设 opacity:0，
   * 挂在标题区的徽标随之不可见。</p>
   */
  function menuIcon(item, node) {
    const IconComp = resolveIcon(item.meta?.icon)
    return () => {
      const icon = h(NIcon, null, { default: () => h(IconComp) })
      if (!iconBadgeRef.value || !node || !hasBadgeInTree(node)) return icon
      return h(NBadge, { value: badgeTotal(node), max: 99, showZero: false }, {
        default: () => icon
      })
    }
  }

  /** 把后端路由树转换成 n-menu 需要的 options（跳过 hidden 跳转页） */
  function toOptions(routers = []) {
    return routers
      .filter((item) => !isJumpPage(item))
      .filter((item) => item.component !== 'Layout' || (item.children && item.children.length))
      .map((item) => {
        const children = item.children?.length ? toOptions(item.children) : null
        const hasVisibleChildren = !!children?.length
        if (item.children?.length && !hasVisibleChildren) {
          if (!item.component || item.component === 'Layout') {
            return null
          }
        }
        const node = {
          label: item.meta?.title || item.name,
          key: hasVisibleChildren ? String(item.id || item.name) : item.path,
          icon: null,
          /** 原始路径，双列一级栏定位用 */
          path: item.path,
          component: item.component,
          meta: item.meta
        }
        if (hasVisibleChildren) {
          node.children = children
        }
        // 红点按路径注册（见 constants/menuBadge.js），这里不需要知道是哪个业务：
        // 自身注册的直接展示，父级汇总子级数字，折叠起来也能看到待办。
        if (hasBadgeInTree(node)) {
          node.extra = () => renderBadge(badgeTotal(node))
        }
        // 图标要等 children / 红点信息就绪后再建，折叠态需要把红点画在图标上
        node.icon = menuIcon(item, node)
        return node
      })
      .filter(Boolean)
  }

  const menuOptions = computed(() => {
    void collapsedRef.value
    const dynamic = toOptions(userStore.routers)
    return [
      {
        label: '运行监控',
        key: '/dashboard',
        path: '/dashboard',
        icon: () => h(NIcon, null, { default: () => h(resolveIcon('dashboard')) })
      },
      ...dynamic
    ]
  })

  /** 根据当前路由找到应展开的目录链 */
  function findExpandKeys(optionsList, path, parents = []) {
    for (const opt of optionsList || []) {
      if (opt.key === path) {
        return parents
      }
      if (opt.children?.length) {
        const found = findExpandKeys(opt.children, path, [...parents, opt.key])
        if (found) return found
      }
    }
    return null
  }

  /** 找到包含 path 的一级菜单（含 edit/detail 等跳转页的前缀归属） */
  function findTopLevel(optionsList, path) {
    for (const opt of optionsList || []) {
      if (opt.key === path || containsPath(opt, path)) return opt
    }
    let best = null
    let bestLen = -1
    const walk = (opt, top) => {
      const p = String(opt.key).startsWith('/') ? opt.key : opt.path
      if (p && (path === p || path.startsWith(`${p}/`)) && p.length > bestLen) {
        best = top
        bestLen = p.length
      }
      for (const child of opt.children || []) walk(child, top)
    }
    for (const opt of optionsList || []) walk(opt, opt)
    return best
  }

  function containsPath(opt, path) {
    if (opt.key === path || opt.path === path) return true
    return (opt.children || []).some((c) => containsPath(c, path))
  }

  const currentTop = computed(() => findTopLevel(menuOptions.value, activeKey.value))

  const displayOptions = computed(() => {
    const scope = scopeRef.value
    if (scope === 'top') {
      return (menuOptions.value || []).map((opt) => ({
        label: opt.label,
        key: opt.key,
        icon: opt.icon,
        extra: opt.extra,
        path: opt.path,
        meta: opt.meta,
        component: opt.component
      }))
    }
    if (scope === 'children') {
      return currentTop.value?.children?.length ? currentTop.value.children : []
    }
    return menuOptions.value
  })

  const menuValue = computed(() => {
    if (scopeRef.value === 'top') return currentTop.value?.key || null
    return activeKey.value
  })

  function findByKey(list, key) {
    for (const opt of list || []) {
      if (opt.key === key) return opt
      const found = findByKey(opt.children, key)
      if (found) return found
    }
    return null
  }

  function isExternalLeafKey(key) {
    const k = String(key || '')
    return k.startsWith('/') || k.startsWith('http://') || k.startsWith('https://')
  }

  function findFirstLeaf(list) {
    for (const opt of list || []) {
      if (!opt.children?.length && isExternalLeafKey(opt.key)) return opt
      const nested = findFirstLeaf(opt.children)
      if (nested) return nested
    }
    return null
  }

  function openExternalMenu(opt) {
    const linkUrl = opt?.meta?.linkUrl
    if (linkUrl) {
      window.open(linkUrl, '_blank', 'noopener,noreferrer')
      return true
    }
    const comp = String(opt?.component || '')
    if (opt?.meta?.external || comp.toLowerCase().startsWith('external')) {
      openSsoClient(opt?.meta?.ssoClient || '', '/')
      return true
    }
    if (comp.toLowerCase().startsWith('link')) {
      const colon = comp.indexOf(':')
      const url = colon >= 0 ? comp.slice(colon + 1).trim() : ''
      const target = url || opt?.path
      if (target && (target.startsWith('http://') || target.startsWith('https://'))) {
        window.open(target, '_blank', 'noopener,noreferrer')
        return true
      }
    }
    if (opt?.path && (opt.path.startsWith('http://') || opt.path.startsWith('https://'))) {
      window.open(opt.path, '_blank', 'noopener,noreferrer')
      return true
    }
    return false
  }

  function handleSelect(key) {
    const opt = findByKey(menuOptions.value, key)
    if (opt?.children?.length) {
      const leaf = findFirstLeaf(opt.children)
      if (leaf?.key && leaf.key !== route.path) handleSelect(leaf.key)
      return
    }
    if (openExternalMenu(opt)) return
    if (key && String(key).startsWith('/') && key !== route.path) {
      router.push(key)
    }
  }

  function syncExpanded() {
    if (scopeRef.value === 'top') return
    const keys = findExpandKeys(displayOptions.value, activeKey.value)
    if (keys) {
      expandedKeys.value = keys
    }
  }

  watch([activeKey, displayOptions, scopeRef], syncExpanded, { immediate: true })

  return {
    route,
    router,
    appStore,
    activeKey: menuValue,
    expandedKeys,
    menuOptions: displayOptions,
    fullMenuOptions: menuOptions,
    currentTop,
    handleSelect,
    findTopLevel,
    findExpandKeys,
    findFirstLeaf,
    containsPath
  }
}
