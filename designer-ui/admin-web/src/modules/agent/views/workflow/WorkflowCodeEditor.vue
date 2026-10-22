<template>
  <div class="wf-code-shell" :class="{ fullscreen: isFullscreen }">
    <div class="wf-code-bar">
      <span v-if="isFullscreen" class="wf-code-bar-title">脚本编辑</span>
      <span v-else class="wf-code-bar-spacer" />
      <div class="wf-code-bar-actions">
        <span v-if="isFullscreen" class="wf-code-bar-hint">Esc 退出</span>
        <n-button quaternary @click="toggleFullscreen">
          {{ isFullscreen ? '退出全屏' : '全屏编辑' }}
        </n-button>
      </div>
    </div>
    <div ref="hostRef" class="wf-code-editor" />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { basicSetup } from 'codemirror'
import { EditorView, keymap, placeholder as cmPlaceholder } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { indentWithTab } from '@codemirror/commands'
import { HighlightStyle, syntaxHighlighting } from '@codemirror/language'
import { tags as t } from '@lezer/highlight'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { sql } from '@codemirror/lang-sql'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  language: { type: String, default: 'js' },
  minHeight: { type: Number, default: 260 }
})

const emit = defineEmits(['update:modelValue'])

const hostRef = ref(null)
const isFullscreen = ref(false)
const shellMinHeight = computed(() => `${props.minHeight}px`)
const scrollerMinHeight = computed(() => `${Math.max(160, props.minHeight - 36)}px`)
let view = null
let applyingExternal = false

const lightHighlight = HighlightStyle.define([
  { tag: t.keyword, color: '#7c3aed' },
  { tag: t.comment, color: '#94a3b8', fontStyle: 'italic' },
  { tag: t.string, color: '#059669' },
  { tag: t.number, color: '#ea580c' },
  { tag: t.bool, color: '#7c3aed' },
  { tag: t.null, color: '#7c3aed' },
  { tag: t.operator, color: '#64748b' },
  { tag: t.definition(t.variableName), color: '#2563eb' },
  { tag: t.function(t.variableName), color: '#0284c7' },
  { tag: t.className, color: '#0d9488' },
  { tag: t.propertyName, color: '#0f766e' },
  { tag: t.typeName, color: '#b45309' }
])

function langExtension(language) {
  const l = String(language || 'js').toLowerCase()
  if (l === 'python' || l === 'py') return python()
  if (l === 'sql') return sql()
  return javascript()
}

function createState(doc) {
  const extensions = [
    basicSetup,
    keymap.of([
      indentWithTab,
      {
        key: 'Escape',
        run: () => {
          if (isFullscreen.value) {
            exitFullscreen()
            return true
          }
          return false
        }
      }
    ]),
    langExtension(props.language),
    syntaxHighlighting(lightHighlight),
    EditorView.lineWrapping,
    EditorView.updateListener.of((update) => {
      if (!update.docChanged || applyingExternal) return
      emit('update:modelValue', update.state.doc.toString())
    }),
    EditorView.theme({
      '&': {
        fontSize: '13px',
        height: '100%',
        borderRadius: '0 0 8px 8px',
        border: 'none',
        background: '#fff',
        overflow: 'hidden'
      },
      '&.cm-focused': {
        outline: 'none'
      },
      '.cm-scroller': {
        fontFamily:
          'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace',
        lineHeight: '1.55'
      },
      '.cm-content': {
        padding: '10px 0',
        caretColor: '#0f172a'
      },
      '.cm-gutters': {
        background: '#f8fafc',
        color: '#94a3b8',
        borderRight: '1px solid #e2e8f0'
      },
      '.cm-activeLineGutter': {
        background: '#eef2ff'
      },
      '.cm-activeLine': {
        background: 'rgba(241, 245, 249, 0.75)'
      }
    })
  ]
  if (props.placeholder) {
    extensions.push(cmPlaceholder(props.placeholder))
  }
  return EditorState.create({
    doc: doc || '',
    extensions
  })
}

function measure() {
  view?.requestMeasure()
  nextTick(() => view?.focus())
}

function enterFullscreen() {
  isFullscreen.value = true
  document.body.style.overflow = 'hidden'
  document.addEventListener('keydown', onGlobalEsc)
  nextTick(measure)
}

function exitFullscreen() {
  if (!isFullscreen.value) return
  isFullscreen.value = false
  document.body.style.overflow = ''
  document.removeEventListener('keydown', onGlobalEsc)
  nextTick(measure)
}

function toggleFullscreen() {
  if (isFullscreen.value) exitFullscreen()
  else enterFullscreen()
}

function onGlobalEsc(e) {
  if (e.key === 'Escape') {
    e.preventDefault()
    exitFullscreen()
  }
}

onMounted(() => {
  if (!hostRef.value) return
  view = new EditorView({
    parent: hostRef.value,
    state: createState(props.modelValue)
  })
})

watch(
  () => props.modelValue,
  (next) => {
    if (!view) return
    const cur = view.state.doc.toString()
    const text = next == null ? '' : String(next)
    if (text === cur) return
    applyingExternal = true
    view.dispatch({
      changes: { from: 0, to: cur.length, insert: text }
    })
    applyingExternal = false
  }
)

watch(
  () => props.language,
  () => {
    if (!view || !hostRef.value) return
    const doc = view.state.doc.toString()
    const fs = isFullscreen.value
    view.destroy()
    view = new EditorView({
      parent: hostRef.value,
      state: createState(doc)
    })
    if (fs) nextTick(measure)
  }
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onGlobalEsc)
  if (isFullscreen.value) {
    document.body.style.overflow = ''
  }
  view?.destroy()
  view = null
})
</script>

<style scoped>
.wf-code-shell {
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--color-border, #e5e7eb) 90%, transparent);
  background: #fff;
  overflow: hidden;
  min-height: v-bind(shellMinHeight);
}

.wf-code-shell.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 4000;
  border-radius: 0;
  min-height: 100vh;
}

.wf-code-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 4px 8px;
  border-bottom: 1px solid #eef2f7;
  background: #f8fafc;
}

.wf-code-shell.fullscreen .wf-code-bar {
  padding: 8px 12px;
}

.wf-code-bar-title {
  font-size: 13px;
  color: #334155;
}

.wf-code-bar-spacer {
  flex: 1;
}

.wf-code-bar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.wf-code-bar-hint {
  font-size: 12px;
  color: #94a3b8;
}

.wf-code-editor {
  flex: 1;
  min-height: 0;
}

.wf-code-editor :deep(.cm-editor) {
  height: 100%;
}

.wf-code-shell:not(.fullscreen) .wf-code-editor :deep(.cm-scroller) {
  min-height: v-bind(scrollerMinHeight);
}

.wf-code-shell.fullscreen .wf-code-editor :deep(.cm-scroller) {
  min-height: calc(100vh - 44px);
}

.wf-code-shell:not(.fullscreen):focus-within {
  border-color: color-mix(in srgb, var(--color-primary, #18a058) 45%, #cbd5e1);
}
</style>
