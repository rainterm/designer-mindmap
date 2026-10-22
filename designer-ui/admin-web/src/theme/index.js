/**
 * 主题色与视觉 token
 * 默认：运动绿（Athletic Green）
 * 可通过 primary 动态生成整套主题，供偏好设置切换
 *
 * 业务侧语义色约定：成功/通过类直接用主色（--color-primary），
 * 失败/驳回类直接用 Naive UI 自带的 error 色（--color-danger），不再自定义色值。
 */
import { lightTheme } from 'naive-ui'

export const DEFAULT_PRIMARY = '#16A34A'

/** 预设主题色（ThemeColorPicker 共享组件：偏好设置外观面板 + 系统设置默认主题色） */
export const PRIMARY_PRESETS = [
  { label: '翠绿', value: '#16A34A' },
  { label: '天蓝', value: '#2563EB' },
  { label: '靛蓝', value: '#4F46E5' },
  { label: '青碧', value: '#0891B2' },
  { label: '青绿', value: '#0D9488' },
  { label: '琥珀', value: '#D97706' },
  { label: '橙色', value: '#EA580C' },
  { label: '玫红', value: '#E11D48' },
  { label: '紫罗兰', value: '#7C3AED' },
  { label: '石板灰', value: '#475569' }
]

/** 统一圆角 */
export const radius = '6px'

/** 非主色的固定表面/文字 token */
const BASE_SURFACE = {
  /** 卡片、弹框等容器背景 */
  surface: '#FFFFFF',
  /** 页面整体背景 */
  body: '#F2F5F3',
  /** 通用边框色 */
  border: '#E2E8E4',
  /** 正文主文字色 */
  text: '#161A17',
  /** 次要文字色（副标题、说明） */
  textSecondary: '#55605A',
  /** 弱化文字色（占位、辅助提示） */
  textMuted: '#87918B',
  /** 侧边栏菜单文字色 */
  textMenu: '#3F4A44',
  /** 侧边栏菜单图标色 */
  textMenuIcon: '#74807A'
}

function normalizeHex(input) {
  if (!input || typeof input !== 'string') return DEFAULT_PRIMARY
  let hex = input.trim()
  if (!hex.startsWith('#')) hex = `#${hex}`
  if (/^#[0-9a-fA-F]{3}$/.test(hex)) {
    hex = `#${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]}`
  }
  if (!/^#[0-9a-fA-F]{6}$/.test(hex)) return DEFAULT_PRIMARY
  return hex.toUpperCase()
}

function hexToRgb(hex) {
  const h = normalizeHex(hex).slice(1)
  const n = parseInt(h, 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}

function rgbToHex({ r, g, b }) {
  const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)))
  return `#${[clamp(r), clamp(g), clamp(b)]
    .map((v) => v.toString(16).padStart(2, '0'))
    .join('')
    .toUpperCase()}`
}

/** 将颜色按比例混入目标色 */
function mixHex(hex, targetHex, amount) {
  const a = hexToRgb(hex)
  const b = hexToRgb(targetHex)
  const t = Math.max(0, Math.min(1, amount))
  return rgbToHex({
    r: a.r + (b.r - a.r) * t,
    g: a.g + (b.g - a.g) * t,
    b: a.b + (b.b - a.b) * t
  })
}

export function withAlpha(hex, alpha) {
  const { r, g, b } = hexToRgb(hex)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/**
 * 由主色派生完整色板
 * @param {string} [primary]
 */
export function buildColors(primary = DEFAULT_PRIMARY) {
  const p = normalizeHex(primary)
  return {
    primary: p,
    primaryHover: mixHex(p, '#FFFFFF', 0.18),
    primaryPressed: mixHex(p, '#000000', 0.14),
    primarySuppl: mixHex(p, '#FFFFFF', 0.22),
    primaryLight: mixHex(p, '#FFFFFF', 0.55),
    ...BASE_SURFACE
  }
}

/** 默认色板（兼容旧引用） */
export const colors = buildColors(DEFAULT_PRIMARY)

/**
 * 将主题色写入 :root
 * @param {string|ReturnType<typeof buildColors>} [primaryOrColors]
 * @param {HTMLElement} [target]
 */
export function applyThemeCssVars(primaryOrColors = DEFAULT_PRIMARY, target = document.documentElement) {
  const c =
    typeof primaryOrColors === 'string' || !primaryOrColors
      ? buildColors(primaryOrColors)
      : primaryOrColors

  const map = {
    '--color-primary': c.primary,
    '--color-primary-hover': c.primaryHover,
    '--color-primary-pressed': c.primaryPressed,
    '--color-primary-light': c.primaryLight,
    '--color-surface': c.surface,
    '--color-body': c.body,
    '--color-border': c.border,
    '--color-text': c.text,
    '--color-text-secondary': c.textSecondary,
    '--color-text-muted': c.textMuted,
    '--color-danger': lightTheme.common.errorColor,
    '--radius': radius,
    '--layout-nav-height': '44px',
    '--layout-tags-height': '36px'
  }
  Object.entries(map).forEach(([key, value]) => {
    target.style.setProperty(key, value)
  })
}

/**
 * 构建 Naive UI theme-overrides
 * @param {string} [primary]
 * @returns {import('naive-ui').GlobalThemeOverrides}
 */
export function buildThemeOverrides(primary = DEFAULT_PRIMARY) {
  const c = buildColors(primary)
  return {
    common: {
      primaryColor: c.primary,
      primaryColorHover: c.primaryHover,
      primaryColorPressed: c.primaryPressed,
      primaryColorSuppl: c.primarySuppl,
      borderRadius: radius,
      borderRadiusSmall: radius,
      borderRadiusMedium: radius,
      borderRadiusLarge: radius,
      borderRadiusHuge: radius,
      fontFamily:
        "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif",
      scrollbarWidth: '12px',
      scrollbarHeight: '12px',
      scrollbarBorderRadius: radius
    },
    Layout: {
      color: c.body,
      siderColor: c.surface,
      headerColor: c.surface
    },
    Menu: {
      itemTextColor: c.textMenu,
      itemIconColor: c.textMenuIcon,
      itemTextColorActive: c.primary,
      itemIconColorActive: c.primary,
      itemTextColorHover: c.primary,
      itemIconColorHover: c.primary,
      itemColorActive: withAlpha(c.primary, 0.12),
      itemColorActiveHover: withAlpha(c.primary, 0.16),
      itemColorActiveCollapsed: withAlpha(c.primary, 0.12)
    },
    Card: {
      borderRadius: radius,
      borderColor: c.border,
      color: c.surface,
      paddingMedium: '10px'
    },
    Empty: {
      iconColor: withAlpha(c.primary, 0.35),
      textColor: c.textMuted,
      iconSizeMedium: '48px',
      fontSizeMedium: '13px'
    },
    Scrollbar: {
      width: '12px',
      height: '12px',
      borderRadius: radius,
      color: 'rgba(0, 0, 0, 0.28)',
      colorHover: 'rgba(0, 0, 0, 0.4)'
    }
  }
}

/** 默认 overrides（兼容旧引用） */
export const themeOverrides = buildThemeOverrides(DEFAULT_PRIMARY)

export { normalizeHex as normalizePrimaryColor }
