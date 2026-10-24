<template>
  <div class="easy-panel">
    <section class="block">
      <div class="block-title">快捷操作</div>
      <ul class="tips">
        <li><kbd>Tab</kbd> 添加子节点</li>
        <li><kbd>Enter</kbd> 添加同级节点</li>
        <li><kbd>Delete</kbd> 删除节点</li>
        <li>双击文字即可编辑</li>
      </ul>
    </section>
    <section class="block">
      <div class="block-title">备注</div>
      <p v-if="!hasSelection" class="hint">先点选画布上的节点，再编辑备注</p>
      <n-input
        v-else
        class="note-input"
        type="textarea"
        :value="note"
        :rows="3"
        maxlength="2000"
        placeholder="备注（可选）"
        @update:value="(v) => emit('note-change', v || '')"
      />
    </section>
    <section class="block">
      <div class="block-title">外观主题</div>
      <div class="theme-grid">
        <button
          v-for="t in themes"
          :key="t.value"
          type="button"
          class="theme-card"
          :class="{ active: model.theme === t.value }"
          :title="t.label"
          @click="patchGlobal('theme', t.value)"
        >
          <span class="theme-preview">
            <i :style="{ background: t.root }" />
            <i :style="{ background: t.second }" />
            <i :style="{ background: t.bg }" />
          </span>
          <span class="theme-name">{{ t.label }}</span>
        </button>
      </div>
    </section>
    <section class="block">
      <div class="block-title">展开方式</div>
      <div class="layout-grid">
        <button
          v-for="l in layouts"
          :key="l.value"
          type="button"
          class="layout-btn"
          :class="{ active: model.layout === l.value }"
          @click="patchGlobal('layout', l.value)"
        >
          <span class="layout-icon" aria-hidden="true">{{ l.icon }}</span>
          <span>{{ l.label }}</span>
        </button>
      </div>
    </section>
    <section class="block">
      <div class="block-title">
        节点颜色
        <n-button
          v-if="hasSelection"
          quaternary
          class="reset-btn"
          @click="emit('node-reset')"
        >
          清除
        </n-button>
      </div>
      <p v-if="!hasSelection" class="hint">先点选画布上的节点，再改颜色</p>
      <template v-else>
        <div class="swatch-row">
          <button
            v-for="c in colors"
            :key="c"
            type="button"
            class="swatch"
            :class="{ active: isActiveColor(c) }"
            :style="{ background: c }"
            :title="c"
            @click="applyColor(c)"
          />
        </div>
        <n-button
          :type="model.fontWeight === 'bold' ? 'primary' : 'default'"
          secondary
          block
          style="margin-top: 10px"
          @click="toggleBold"
        >
          {{ model.fontWeight === 'bold' ? '已加粗' : '加粗文字' }}
        </n-button>
      </template>
    </section>
  </div>
</template>

<script setup>
import { themePresets } from '../themes'

const props = defineProps({
  hasSelection: { type: Boolean, default: false },
  note: { type: String, default: '' },
  model: {
    type: Object,
    default: () => ({})
  },
  nodeStyle: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['global-change', 'node-change', 'note-change', 'node-reset'])

const themes = [
  { label: '默认', value: 'default', root: '#549688', second: '#ffffff', bg: '#fafafa' },
  ...themePresets
]

const layouts = [
  { label: '向右', value: 'logicalStructure', icon: '→' },
  { label: '左右', value: 'mindMap', icon: '↔' },
  { label: '向下', value: 'organizationStructure', icon: '↓' },
  { label: '时间轴', value: 'timeline', icon: '⇢' }
]

const colors = ['#549688', '#3b6ef5', '#dd6b32', '#7a5af0', '#e0484d', '#17916a', '#22263a', '#ffffff']

function patchGlobal(key, value) {
  emit('global-change', { key, value })
}

function isLight(hex) {
  const h = String(hex || '').replace('#', '')
  if (h.length < 6) return true
  const r = parseInt(h.slice(0, 2), 16)
  const g = parseInt(h.slice(2, 4), 16)
  const b = parseInt(h.slice(4, 6), 16)
  return (r * 299 + g * 587 + b * 114) / 1000 > 160
}

function isActiveColor(c) {
  return String(props.nodeStyle.fillColor || '').toLowerCase() === c.toLowerCase()
}

function applyColor(color) {
  emit('node-change', {
    ...props.nodeStyle,
    fillColor: color,
    color: isLight(color) ? '#333333' : '#ffffff',
    borderColor: color,
    borderWidth: props.nodeStyle.borderWidth > 0 ? props.nodeStyle.borderWidth : 1
  })
}

function toggleBold() {
  const next = props.nodeStyle.fontWeight === 'bold' ? 'normal' : 'bold'
  emit('node-change', { ...props.nodeStyle, fontWeight: next })
}
</script>

<style scoped>
.easy-panel {
  height: 100%;
  overflow: auto;
  padding: 12px 14px 20px;
  color: var(--color-text, #161a17);
}
.block + .block {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border, #e2e8e4);
}
.block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
}
.reset-btn {
  margin: -4px 0;
}
.tips {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
  font-size: 12px;
  color: var(--color-text-secondary, #55605a);
  line-height: 1.5;
}
.tips kbd {
  display: inline-block;
  min-width: 42px;
  padding: 1px 6px;
  margin-right: 4px;
  border-radius: 4px;
  border: 1px solid var(--color-border, #e2e8e4);
  background: var(--color-body, #f2f5f3);
  font-size: 11px;
  text-align: center;
}
.theme-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.theme-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  border: 1px solid var(--color-border, #e2e8e4);
  border-radius: 8px;
  background: var(--color-surface, #fff);
  cursor: pointer;
  text-align: left;
}
.theme-card:hover {
  border-color: color-mix(in srgb, var(--color-primary) 45%, var(--color-border));
}
.theme-card.active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}
.theme-preview {
  display: flex;
  height: 18px;
  border-radius: 4px;
  overflow: hidden;
}
.theme-preview i {
  flex: 1;
}
.theme-name {
  font-size: 12px;
  color: var(--color-text-secondary, #55605a);
}
.layout-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.layout-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 6px;
  border: 1px solid var(--color-border, #e2e8e4);
  border-radius: 8px;
  background: var(--color-surface, #fff);
  cursor: pointer;
  font-size: 12px;
  color: var(--color-text-secondary, #55605a);
}
.layout-btn:hover {
  border-color: color-mix(in srgb, var(--color-primary) 45%, var(--color-border));
}
.layout-btn.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 8%, #fff);
}
.layout-icon {
  font-size: 16px;
  line-height: 1;
  font-weight: 700;
}
.hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-muted, #87918b);
}
.swatch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.swatch {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid var(--color-border, #e2e8e4);
  cursor: pointer;
  padding: 0;
}
.swatch.active {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
}
.note-input {
  width: 100%;
}
</style>
