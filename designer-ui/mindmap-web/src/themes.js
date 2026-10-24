import MindMap from 'simple-mind-map'

/** 注册几套内置差分主题（0.14 核心仅含 default） */
const THEMES = [
  {
    key: 'freshGreen',
    name: '清新绿',
    config: {
      backgroundColor: '#f3faf6',
      lineColor: '#17916a',
      root: { fillColor: '#17916a', color: '#fff' },
      second: { fillColor: '#fff', color: '#1e3128', borderColor: '#17916a', borderWidth: 1 },
      node: { fillColor: 'transparent', color: '#1e3128', borderColor: 'transparent' }
    }
  },
  {
    key: 'blueSky',
    name: '天空蓝',
    config: {
      backgroundColor: '#f5f8ff',
      lineColor: '#3b6ef5',
      root: { fillColor: '#3b6ef5', color: '#fff' },
      second: { fillColor: '#fff', color: '#22263a', borderColor: '#3b6ef5', borderWidth: 1 },
      node: { fillColor: 'transparent', color: '#22263a', borderColor: 'transparent' }
    }
  },
  {
    key: 'sunset',
    name: '暖阳橙',
    config: {
      backgroundColor: '#fdf5ee',
      lineColor: '#dd6b32',
      root: { fillColor: '#dd6b32', color: '#fff' },
      second: { fillColor: '#fff', color: '#37261a', borderColor: '#dd6b32', borderWidth: 1 },
      node: { fillColor: 'transparent', color: '#37261a', borderColor: 'transparent' }
    }
  },
  {
    key: 'lavender',
    name: '雾紫',
    config: {
      backgroundColor: '#f6f4fd',
      lineColor: '#7a5af0',
      root: { fillColor: '#7a5af0', color: '#fff' },
      second: { fillColor: '#fff', color: '#2a2344', borderColor: '#7a5af0', borderWidth: 1 },
      node: { fillColor: 'transparent', color: '#2a2344', borderColor: 'transparent' }
    }
  },
  {
    key: 'dark',
    name: '暗夜',
    config: {
      backgroundColor: '#11131a',
      lineColor: '#5b8cff',
      root: { fillColor: '#5b8cff', color: '#0d1118' },
      second: { fillColor: '#1d212c', color: '#e3e7f1', borderColor: '#333a4b', borderWidth: 1 },
      node: { fillColor: 'transparent', color: '#e3e7f1', borderColor: 'transparent' }
    }
  }
]

let registered = false

export function ensureThemes() {
  if (registered) return
  THEMES.forEach((t) => {
    try {
      MindMap.defineTheme(t.key, t.config)
    } catch {
      // already defined
    }
  })
  registered = true
}

export const themeOptions = [
  { label: '默认', value: 'default' },
  ...THEMES.map((t) => ({ label: t.name, value: t.key }))
]

/** 简易面板用的主题色预览 */
export const themePresets = THEMES.map((t) => ({
  label: t.name,
  value: t.key,
  root: t.config.root?.fillColor || '#549688',
  second: t.config.second?.fillColor || '#ffffff',
  bg: t.config.backgroundColor || '#fafafa'
}))
