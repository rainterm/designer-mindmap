<template>
  <div ref="elRef" class="mm-canvas" />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import MindMap from 'simple-mind-map'
import Export from 'simple-mind-map/src/plugins/Export.js'
import ExportXMind from 'simple-mind-map/src/plugins/ExportXMind.js'
import Drag from 'simple-mind-map/src/plugins/Drag.js'
import KeyboardNavigation from 'simple-mind-map/src/plugins/KeyboardNavigation.js'
import Select from 'simple-mind-map/src/plugins/Select.js'
import { transformMarkdownTo } from 'simple-mind-map/src/parse/markdownTo.js'
import xmind from 'simple-mind-map/src/parse/xmind.js'
import 'simple-mind-map/dist/simpleMindMap.esm.css'
import { ensureThemes } from '../themes'

MindMap.usePlugin(Export)
MindMap.usePlugin(ExportXMind)
MindMap.usePlugin(Drag)
MindMap.usePlugin(KeyboardNavigation)
MindMap.usePlugin(Select)
ensureThemes()

const props = defineProps({
  /** 节点树 JSON 字符串，或 getData(true) 完整 JSON */
  content: { type: String, default: '' },
  layout: { type: String, default: 'logicalStructure' },
  theme: { type: String, default: 'default' }
})

const emit = defineEmits(['change', 'ready', 'node-active'])

const elRef = ref(null)
let mindMap = null
let silent = false

const EMPTY_TREE = { data: { text: '中心主题' }, children: [] }

const STYLE_KEYS = [
  'shape',
  'fillColor',
  'color',
  'fontSize',
  'fontWeight',
  'paddingX',
  'paddingY',
  'borderColor',
  'borderWidth',
  'borderRadius',
  'borderDasharray'
]

function parsePayload(raw, layout, theme) {
  let parsed = null
  if (raw) {
    try {
      parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    } catch {
      parsed = null
    }
  }
  if (parsed?.root) {
    return parsed
  }
  if (parsed?.data) {
    return {
      root: parsed,
      layout: layout || 'logicalStructure',
      theme: { template: theme || 'default', config: {} }
    }
  }
  return {
    root: EMPTY_TREE,
    layout: layout || 'logicalStructure',
    theme: { template: theme || 'default', config: {} }
  }
}

function init() {
  if (!elRef.value || mindMap) return
  const full = parsePayload(props.content, props.layout, props.theme)
  mindMap = new MindMap({
    el: elRef.value,
    data: full.root,
    layout: full.layout || props.layout || 'logicalStructure',
    theme: full.theme?.template || props.theme || 'default',
    themeConfig: full.theme?.config || {}
  })
  if (full.view) {
    mindMap.view.setTransformData(full.view)
  }
  // 点击/拖动画布空白不取消选中（command.add 是追加，需先 remove 原处理器）
  mindMap.command.remove('CLEAR_ACTIVE_NODE')
  mindMap.command.add('CLEAR_ACTIVE_NODE', () => {})
  mindMap.renderer.clearActiveNodeListOnDrawClick = () => {}
  mindMap.on('data_change', onDataChange)
  mindMap.on('node_active', onNodeActive)
  emit('ready', mindMap)
}

function onDataChange() {
  if (silent || !mindMap) return
  emit('change', getFullData())
}

function onNodeActive(_node, activeNodeList = []) {
  const list = activeNodeList || []
  emit('node-active', {
    count: list.length,
    style: list.length ? readNodeStyle(list[0]) : null,
    note: list.length ? readNodeNote(list[0]) : ''
  })
}

function readNodeNote(node) {
  if (!node) return ''
  try {
    return node.getData?.('note') || ''
  } catch {
    return ''
  }
}

function readNodeStyle(node) {
  if (!node) return null
  const style = {}
  STYLE_KEYS.forEach((key) => {
    try {
      style[key] = typeof node.getStyle === 'function' ? node.getStyle(key) : node.style?.merge?.(key)
    } catch {
      style[key] = undefined
    }
  })
  return style
}

function getActiveNodes() {
  return mindMap?.renderer?.activeNodeList || []
}

function setActiveNodeNote(note = '') {
  const nodes = getActiveNodes()
  if (!nodes.length || !mindMap) return
  const text = note == null ? '' : String(note)
  nodes.forEach((node) => {
    mindMap.execCommand('SET_NODE_NOTE', node, text)
  })
}

function setActiveNodeStyles(style = {}) {
  const nodes = getActiveNodes()
  if (!nodes.length) return
  const patch = {}
  STYLE_KEYS.forEach((key) => {
    if (style[key] !== undefined && style[key] !== null && style[key] !== '') {
      patch[key] = style[key]
    }
  })
  if (!Object.keys(patch).length) return
  nodes.forEach((node) => {
    if (typeof node.setStyles === 'function') {
      node.setStyles(patch)
    } else {
      mindMap.execCommand('SET_NODE_STYLES', node, patch)
    }
  })
}

function resetActiveNodeStyles() {
  const nodes = getActiveNodes()
  if (!nodes.length || !mindMap) return
  nodes.forEach((node) => {
    mindMap.execCommand('REMOVE_CUSTOM_STYLES', node)
  })
  const list = getActiveNodes()
  emit('node-active', {
    count: list.length,
    style: list.length ? readNodeStyle(list[0]) : null,
    note: list.length ? readNodeNote(list[0]) : ''
  })
}

function expandAll() {
  if (!mindMap) return
  mindMap.execCommand('EXPAND_ALL')
}

function collapseAll() {
  if (!mindMap) return
  mindMap.execCommand('UNEXPAND_ALL')
}

function getFullData() {
  if (!mindMap) return null
  return mindMap.getData(true)
}

function getContentJson() {
  const full = getFullData()
  return full ? JSON.stringify(full) : ''
}

function applyContent(content, layout, theme) {
  if (!mindMap) return
  silent = true
  try {
    const full = parsePayload(content, layout, theme)
    mindMap.setFullData(full)
  } finally {
    silent = false
  }
}

function setTheme(theme) {
  if (!mindMap || !theme) return
  silent = true
  try {
    mindMap.setTheme(theme)
  } finally {
    silent = false
  }
  emit('change', getFullData())
}

function setLayout(layout) {
  if (!mindMap || !layout) return
  silent = true
  try {
    mindMap.setLayout(layout)
  } finally {
    silent = false
  }
  emit('change', getFullData())
}

function getGlobalSettings() {
  if (!mindMap) return null
  const cfg = mindMap.getThemeConfig?.() || {}
  const custom = mindMap.getCustomThemeConfig?.() || {}
  const topPadX = cfg.paddingX ?? 15
  const topPadY = cfg.paddingY ?? 5
  return {
    layout: mindMap.getLayout?.() || 'logicalStructure',
    theme: mindMap.getTheme?.() || 'default',
    backgroundColor: cfg.backgroundColor || '#fafafa',
    lineColor: cfg.lineColor || '#549688',
    lineWidth: cfg.lineWidth ?? 1,
    lineStyle: cfg.lineStyle || 'straight',
    lineDasharray: cfg.lineDasharray || 'none',
    rootFillColor: cfg.root?.fillColor || custom.root?.fillColor || '#549688',
    rootFontSize: cfg.root?.fontSize ?? custom.root?.fontSize ?? 16,
    rootPaddingX: cfg.root?.paddingX ?? custom.root?.paddingX ?? topPadX,
    rootPaddingY: cfg.root?.paddingY ?? custom.root?.paddingY ?? topPadY,
    secondFillColor: cfg.second?.fillColor || custom.second?.fillColor || '#ffffff',
    secondBorderColor: cfg.second?.borderColor || custom.second?.borderColor || '#549688',
    secondFontSize: cfg.second?.fontSize ?? custom.second?.fontSize ?? 16,
    secondPaddingX: cfg.second?.paddingX ?? custom.second?.paddingX ?? topPadX,
    secondPaddingY: cfg.second?.paddingY ?? custom.second?.paddingY ?? topPadY,
    secondMarginX: cfg.second?.marginX ?? custom.second?.marginX ?? 100,
    secondMarginY: cfg.second?.marginY ?? custom.second?.marginY ?? 40,
    nodeFillColor: cfg.node?.fillColor ?? custom.node?.fillColor ?? 'transparent',
    nodeColor: cfg.node?.color || custom.node?.color || '#6a6d6c',
    nodeBorderColor: cfg.node?.borderColor ?? custom.node?.borderColor ?? 'transparent',
    nodeBorderWidth: cfg.node?.borderWidth ?? custom.node?.borderWidth ?? 0,
    nodeFontSize: cfg.node?.fontSize ?? custom.node?.fontSize ?? 14,
    nodePaddingX: cfg.node?.paddingX ?? custom.node?.paddingX ?? topPadX,
    nodePaddingY: cfg.node?.paddingY ?? custom.node?.paddingY ?? topPadY,
    nodeMarginX: cfg.node?.marginX ?? custom.node?.marginX ?? 50,
    nodeMarginY: cfg.node?.marginY ?? custom.node?.marginY ?? 0
  }
}

function cloneThemeCustom(raw = {}) {
  return {
    ...raw,
    root: { ...(raw.root || {}) },
    second: { ...(raw.second || {}) },
    node: { ...(raw.node || {}) }
  }
}

function isTransparentColor(v) {
  return !v || v === 'transparent'
}

/** 三级节点默认透明；一旦用户配置色块相关项，自动补齐可见的填充/边框 */
function ensureNodeBoxStyle(custom) {
  const mergedNode = mindMap?.getThemeConfig?.()?.node || {}
  const node = custom.node || {}
  const fill = node.fillColor ?? mergedNode.fillColor
  const border = node.borderColor ?? mergedNode.borderColor
  const bw = node.borderWidth ?? mergedNode.borderWidth ?? 0
  const line = custom.lineColor || mindMap?.getThemeConfig?.()?.lineColor || '#549688'
  const wantsBox =
    bw > 0 ||
    (!isTransparentColor(fill) && node.fillColor !== undefined) ||
    (!isTransparentColor(border) && node.borderColor !== undefined)
  if (!wantsBox) {
    custom.node = node
    return
  }
  if (isTransparentColor(node.fillColor ?? mergedNode.fillColor)) {
    node.fillColor = '#ffffff'
  }
  if (isTransparentColor(node.borderColor ?? mergedNode.borderColor)) {
    node.borderColor = line
  }
  if (node.borderWidth === undefined || node.borderWidth === null) {
    node.borderWidth = 1
  }
  custom.node = node
}

function applyGlobalSetting(key, value) {
  if (!mindMap) return
  if (key === 'layout') {
    setLayout(value)
    return
  }
  if (key === 'theme') {
    setTheme(value)
    return
  }
  const custom = cloneThemeCustom(mindMap.getCustomThemeConfig?.() || {})
  const layerKeyMap = {
    rootFillColor: ['root', 'fillColor'],
    rootFontSize: ['root', 'fontSize'],
    rootPaddingX: ['root', 'paddingX'],
    rootPaddingY: ['root', 'paddingY'],
    secondFillColor: ['second', 'fillColor'],
    secondBorderColor: ['second', 'borderColor'],
    secondFontSize: ['second', 'fontSize'],
    secondPaddingX: ['second', 'paddingX'],
    secondPaddingY: ['second', 'paddingY'],
    secondMarginX: ['second', 'marginX'],
    secondMarginY: ['second', 'marginY'],
    nodeFillColor: ['node', 'fillColor'],
    nodeColor: ['node', 'color'],
    nodeBorderColor: ['node', 'borderColor'],
    nodeBorderWidth: ['node', 'borderWidth'],
    nodeFontSize: ['node', 'fontSize'],
    nodePaddingX: ['node', 'paddingX'],
    nodePaddingY: ['node', 'paddingY'],
    nodeMarginX: ['node', 'marginX'],
    nodeMarginY: ['node', 'marginY']
  }
  if (layerKeyMap[key]) {
    const [layer, prop] = layerKeyMap[key]
    custom[layer] = { ...(custom[layer] || {}), [prop]: value }
    // 同步顶层内边距兜底，避免未单独配置的层级读到旧值
    if (prop === 'paddingX' || prop === 'paddingY') {
      custom[prop] = value
    }
    if (layer === 'node') {
      ensureNodeBoxStyle(custom)
    }
  } else if (key === 'lineColor') {
    custom.lineColor = value
    custom.root = { ...(custom.root || {}), lineColor: value }
    custom.second = { ...(custom.second || {}), lineColor: value }
    custom.node = { ...(custom.node || {}), lineColor: value }
  } else {
    custom[key] = value
  }
  silent = true
  try {
    // 传入全新对象，避免和实例内部配置共用引用
    mindMap.setThemeConfig(cloneThemeCustom(custom))
    // 强制按换主题链路重绘，确保三级节点形状/描边立即生效
    mindMap.render?.(null, 'changeTheme')
  } finally {
    silent = false
  }
  emit('change', getFullData())
}

function resetGlobalSettings() {
  if (!mindMap) return
  silent = true
  try {
    mindMap.setTheme('default')
    mindMap.setThemeConfig({})
    mindMap.setLayout('logicalStructure')
  } finally {
    silent = false
  }
  emit('change', getFullData())
}

async function exportPng(fileName = '思维导图') {
  if (!mindMap) return
  await mindMap.export('png', true, fileName)
}

async function exportFile(type = 'png', fileName = '思维导图') {
  if (!mindMap) return
  const allowed = ['png', 'svg', 'json', 'smm', 'md', 'txt', 'xmind']
  if (!allowed.includes(type)) {
    throw new Error(`不支持导出格式：${type}`)
  }
  await mindMap.export(type, true, fileName)
}

function readFileAsText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsText(file)
  })
}

async function importFile(file) {
  if (!mindMap || !file) return
  const name = String(file.name || '').toLowerCase()
  let payload = null
  if (name.endsWith('.xmind')) {
    payload = await xmind.parseXmindFile(file)
  } else if (name.endsWith('.md') || name.endsWith('.markdown')) {
    const text = await readFileAsText(file)
    payload = transformMarkdownTo(text)
  } else if (name.endsWith('.json') || name.endsWith('.smm') || name.endsWith('.txt')) {
    const text = await readFileAsText(file)
    try {
      payload = JSON.parse(text)
    } catch {
      // 纯文本按 markdown 大纲尝试解析
      if (name.endsWith('.txt') || name.endsWith('.md')) {
        payload = transformMarkdownTo(text)
      } else {
        throw new Error('JSON 文件格式不正确')
      }
    }
  } else {
    throw new Error('仅支持 .json / .smm / .md / .xmind 文件')
  }
  if (!payload) {
    throw new Error('文件内容为空或无法解析')
  }
  silent = true
  try {
    if (payload.root) {
      mindMap.setFullData(payload)
    } else if (payload.data) {
      mindMap.setData(payload)
    } else {
      throw new Error('无法识别的导图数据结构')
    }
  } finally {
    silent = false
  }
  emit('change', getFullData())
}

function fit() {
  mindMap?.view?.fit?.()
}

function resize() {
  mindMap?.resize?.()
}

onMounted(() => {
  init()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (mindMap) {
    mindMap.off('data_change', onDataChange)
    mindMap.off('node_active', onNodeActive)
    mindMap.destroy()
    mindMap = null
  }
})

defineExpose({
  getFullData,
  getContentJson,
  applyContent,
  setTheme,
  setLayout,
  getGlobalSettings,
  applyGlobalSetting,
  resetGlobalSettings,
  setActiveNodeStyles,
  setActiveNodeNote,
  resetActiveNodeStyles,
  expandAll,
  collapseAll,
  exportPng,
  exportFile,
  importFile,
  fit,
  resize,
  getInstance: () => mindMap
})
</script>

<style scoped>
.mm-canvas {
  width: 100%;
  height: 100%;
  min-height: 420px;
  background: var(--color-body, #f2f5f3);
}
</style>
