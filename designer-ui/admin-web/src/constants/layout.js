/** 布局模式常量与选项（偏好设置面板） */

export const LAYOUT_MODES = {
  VERTICAL: 'vertical',
  DUAL_COLUMN: 'dual-column',
  HORIZONTAL: 'horizontal',
  SIDE_NAV: 'side-nav',
  MIXED_VERTICAL: 'mixed-vertical',
  MIXED_DUAL: 'mixed-dual',
  CONTENT_FULL: 'content-full'
}

export const DEFAULT_LAYOUT_MODE = LAYOUT_MODES.MIXED_VERTICAL

export const LAYOUT_OPTIONS = [
  {
    value: LAYOUT_MODES.VERTICAL,
    label: '垂直',
    tip: '经典左侧菜单，适合功能较多的后台'
  },
  {
    value: LAYOUT_MODES.DUAL_COLUMN,
    label: '双列菜单',
    tip: '左侧图标栏 + 二级菜单，模块切换更清晰'
  },
  {
    value: LAYOUT_MODES.HORIZONTAL,
    label: '水平',
    tip: '顶部水平菜单，内容区更开阔'
  },
  {
    value: LAYOUT_MODES.SIDE_NAV,
    label: '侧边导航',
    tip: '顶栏放 Logo，侧栏图标 + 菜单名导航'
  },
  {
    value: LAYOUT_MODES.MIXED_VERTICAL,
    label: '混合垂直',
    tip: '一级菜单在顶栏，对应子菜单在左侧'
  },
  {
    value: LAYOUT_MODES.MIXED_DUAL,
    label: '混合双列',
    tip: '顶栏通栏 + 双列菜单'
  },
  {
    value: LAYOUT_MODES.CONTENT_FULL,
    label: '内容全屏',
    tip: '隐藏侧栏，最大化内容展示区域'
  }
]

/** 是否展示侧栏（含双列） */
export function hasSider(mode) {
  return [
    LAYOUT_MODES.VERTICAL,
    LAYOUT_MODES.DUAL_COLUMN,
    LAYOUT_MODES.SIDE_NAV,
    LAYOUT_MODES.MIXED_VERTICAL,
    LAYOUT_MODES.MIXED_DUAL
  ].includes(mode)
}

/** 是否双列菜单 */
export function isDualColumn(mode) {
  return mode === LAYOUT_MODES.DUAL_COLUMN || mode === LAYOUT_MODES.MIXED_DUAL
}

/** 顶栏是否通栏（Logo 在顶栏） */
export function isHeaderFull(mode) {
  return [
    LAYOUT_MODES.HORIZONTAL,
    LAYOUT_MODES.SIDE_NAV,
    LAYOUT_MODES.MIXED_VERTICAL,
    LAYOUT_MODES.MIXED_DUAL,
    LAYOUT_MODES.CONTENT_FULL
  ].includes(mode)
}

/** 侧栏是否强制折叠为图标栏 */
export function isIconSider(mode) {
  return mode === LAYOUT_MODES.SIDE_NAV
}
