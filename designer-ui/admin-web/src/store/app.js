import { defineStore } from 'pinia'
import { DEFAULT_LAYOUT_MODE, LAYOUT_OPTIONS } from '@/constants/layout'
import {
  DEFAULT_PRIMARY,
  applyThemeCssVars,
  normalizePrimaryColor
} from '@/theme'

const STORAGE_KEY = 'designer-admin-app'
const LAYOUT_VALUES = new Set(LAYOUT_OPTIONS.map((o) => o.value))

function loadPersisted() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const data = JSON.parse(raw)
    return data && typeof data === 'object' ? data : {}
  } catch {
    return {}
  }
}

function normalizeLayoutMode(mode) {
  return LAYOUT_VALUES.has(mode) ? mode : DEFAULT_LAYOUT_MODE
}

function savePersisted(state) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      collapsed: !!state.collapsed,
      layoutMode: normalizeLayoutMode(state.layoutMode),
      primaryColor: normalizePrimaryColor(state.primaryColor),
      // 是否在偏好设置面板里自定义过（布局 / 主题色分开记）：
      // 对应维度为 false 时跟随服务端「系统设置」的默认偏好
      customizedLayout: !!state.customizedLayout,
      customizedColor: !!state.customizedColor,
      visitedViews: state.visitedViews || [],
      expandedKeys: state.expandedKeys || []
    })
  )
}

/**
 * 应用状态：侧边栏折叠、菜单展开、多页签（刷新后保持）
 */
export const useAppStore = defineStore('app', {
  state: () => {
    const saved = loadPersisted()
    return {
      collapsed: !!saved.collapsed,
      /** 页面布局模式 */
      layoutMode: normalizeLayoutMode(saved.layoutMode),
      /** 主题主色 */
      primaryColor: normalizePrimaryColor(saved.primaryColor),
      /**
       * 是否在偏好设置面板里自定义过布局 / 主题色（分开记录）。
       * 对应维度为 false 时跟随服务端「系统设置」的默认偏好（见 applySystemDefaults）。
       *
       * <p>必须分维度：只调过布局的用户，仍应跟随服务端下发的默认主题色。
       * 两者共用一个标记时，改过布局会连带把默认主题色一起挡掉。</p>
       *
       * <p>`saved.customized` 是早期版本的单标记，这里做一次迁移读取。</p>
       */
      customizedLayout: saved.customizedLayout ?? !!saved.customized,
      customizedColor: saved.customizedColor ?? !!saved.customized,
      /** 服务端默认偏好（来自系统设置，非持久化，每次启动由 setting store 注入） */
      systemDefaults: {
        layoutMode: DEFAULT_LAYOUT_MODE,
        primaryColor: DEFAULT_PRIMARY
      },
      /** 偏好设置面板是否打开 */
      preferenceVisible: false,
      /** 侧栏已展开的目录 key */
      expandedKeys: Array.isArray(saved.expandedKeys) ? saved.expandedKeys : [],
      /** 已打开的页签 */
      visitedViews: Array.isArray(saved.visitedViews) ? saved.visitedViews : []
    }
  },

  actions: {
    persist() {
      savePersisted(this.$state)
    },

    toggleCollapsed() {
      this.collapsed = !this.collapsed
      this.persist()
    },

    setLayoutMode(mode) {
      const next = normalizeLayoutMode(mode)
      if (next === this.layoutMode) return
      this.layoutMode = next
      this.customizedLayout = true
      this.persist()
    },

    setPrimaryColor(color) {
      const next = normalizePrimaryColor(color)
      if (next === this.primaryColor) return
      this.primaryColor = next
      this.customizedColor = true
      applyThemeCssVars(next)
      this.persist()
    },

    /**
     * 注入服务端默认偏好（来自「系统设置」）。
     *
     * <p>布局与主题色分开判断：只自定义过布局的用户，仍会跟随服务端下发的默认主题色；
     * 只更新兜底值而不覆盖用户已自定义的那一维。</p>
     *
     * @param {string} layoutMode 服务端默认布局
     * @param {string} primaryColor 服务端默认主题色
     * @param {{ layout?: boolean, color?: boolean }} [adopt] 强制采纳的维度。
     *   后台「系统设置」保存后传它：改默认的人必须立刻看到改动，
     *   否则会以为「改了默认主题色却不起作用」。未指定时按「已自定义」的规则豁免。
     */
    applySystemDefaults(layoutMode, primaryColor, adopt) {
      this.systemDefaults = {
        layoutMode: normalizeLayoutMode(layoutMode),
        primaryColor: normalizePrimaryColor(primaryColor)
      }
      const takeLayout = adopt?.layout || !this.customizedLayout
      const takeColor = adopt?.color || !this.customizedColor
      if (!takeLayout && !takeColor && !adopt) return

      let dirty = false
      if (takeLayout) {
        if (adopt?.layout) this.customizedLayout = false
        if (this.layoutMode !== this.systemDefaults.layoutMode) {
          this.layoutMode = this.systemDefaults.layoutMode
          dirty = true
        }
      }
      if (takeColor) {
        if (adopt?.color) this.customizedColor = false
        if (this.primaryColor !== this.systemDefaults.primaryColor) {
          this.primaryColor = this.systemDefaults.primaryColor
          applyThemeCssVars(this.primaryColor)
          dirty = true
        }
      }
      // 采纳会清掉自定义标记，即便颜色没变也要落盘
      if (dirty || adopt?.layout || adopt?.color) this.persist()
    },

    openPreference() {
      this.preferenceVisible = true
    },

    closePreference() {
      this.preferenceVisible = false
    },

    /** 重置偏好：回到服务端默认（未配置则内置默认），并标记为未自定义 */
    resetPreference() {
      this.customizedLayout = false
      this.customizedColor = false
      this.layoutMode = this.systemDefaults.layoutMode
      this.primaryColor = this.systemDefaults.primaryColor
      applyThemeCssVars(this.primaryColor)
      this.persist()
    },

    /** @deprecated 使用 resetPreference */
    resetLayoutPreference() {
      this.resetPreference()
    },

    setExpandedKeys(keys) {
      const next = Array.isArray(keys) ? keys : []
      // 避免无意义写入触发多余序列化
      if (
        next.length === this.expandedKeys.length &&
        next.every((k, i) => k === this.expandedKeys[i])
      ) {
        return
      }
      this.expandedKeys = next
      this.persist()
    },

    addVisitedView(route) {
      if (!route.name || route.meta?.hidden || route.name === 'Login') return
      if (this.visitedViews.some((v) => v.path === route.path)) return
      this.visitedViews.push({
        name: route.name,
        path: route.path,
        title: route.meta?.title || route.name,
        icon: route.meta?.icon || '',
        affix: !!route.meta?.affix
      })
      this.persist()
    },

    /** 根据路由表初始化固定页签（meta.affix === true） */
    initAffixViews(routes) {
      for (const r of routes || []) {
        if (!r.meta?.affix || !r.name) continue
        this.addVisitedView({
          name: r.name,
          path: r.path,
          meta: r.meta
        })
      }
    },

    removeVisitedView(path) {
      const target = this.visitedViews.find((v) => v.path === path)
      if (target?.affix) return
      this.visitedViews = this.visitedViews.filter((v) => v.path !== path)
      this.persist()
    },

    removeOtherViews(path) {
      this.visitedViews = this.visitedViews.filter((v) => v.affix || v.path === path)
      this.persist()
    },

    clearViews() {
      this.visitedViews = this.visitedViews.filter((v) => v.affix)
      this.persist()
    },

    /** 登出时清理页签与菜单展开（折叠偏好保留） */
    resetLayout() {
      this.visitedViews = []
      this.expandedKeys = []
      this.persist()
    }
  }
})
