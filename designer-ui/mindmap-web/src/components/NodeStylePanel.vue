<template>
  <div class="node-style-panel">
    <div class="panel-toolbar">
      <n-button quaternary :disabled="!hasSelection" @click="emit('reset')">
        重置样式
      </n-button>
    </div>
    <div v-if="!hasSelection" class="panel-empty">选中节点后可自定义样式</div>
    <n-form v-else label-placement="left" label-width="80" class="panel-form">
      <n-form-item label="备注" label-placement="top">
        <n-input
          type="textarea"
          :value="note"
          :rows="3"
          maxlength="500"
          show-count
          placeholder="选中节点的备注，悬停图标可查看"
          @update:value="onNoteInput"
        />
      </n-form-item>
      <n-divider style="margin: 4px 0 12px" />
      <n-form-item label="形状">
        <n-select
          :value="model.shape"
          :options="shapeOptions"
          @update:value="(v) => patch('shape', v)"
        />
      </n-form-item>
      <n-form-item label="填充">
        <n-color-picker
          :value="model.fillColor || '#ffffff'"
          :modes="['hex']"
          :show-alpha="false"
          @update:value="(v) => patch('fillColor', v)"
        />
      </n-form-item>
      <n-form-item label="文字">
        <n-color-picker
          :value="model.color || '#333333'"
          :modes="['hex']"
          :show-alpha="false"
          @update:value="(v) => patch('color', v)"
        />
      </n-form-item>
      <n-form-item label="字号">
        <n-input-number
          :value="Number(model.fontSize) || 14"
          :min="10"
          :max="48"
          style="width: 100%"
          @update:value="(v) => patch('fontSize', v)"
        />
      </n-form-item>
      <n-form-item label="粗细">
        <n-radio-group
          :value="model.fontWeight || 'normal'"
          @update:value="(v) => patch('fontWeight', v)"
        >
          <n-radio-button value="normal">常规</n-radio-button>
          <n-radio-button value="bold">加粗</n-radio-button>
        </n-radio-group>
      </n-form-item>
      <n-form-item label="水平内边">
        <n-input-number
          :value="Number(model.paddingX) || 15"
          :min="0"
          :max="80"
          style="width: 100%"
          @update:value="(v) => patch('paddingX', v)"
        />
      </n-form-item>
      <n-form-item label="垂直内边">
        <n-input-number
          :value="Number(model.paddingY) || 5"
          :min="0"
          :max="40"
          style="width: 100%"
          @update:value="(v) => patch('paddingY', v)"
        />
      </n-form-item>
      <n-form-item label="边框色">
        <n-color-picker
          :value="model.borderColor || '#549688'"
          :modes="['hex']"
          :show-alpha="false"
          @update:value="(v) => patch('borderColor', v)"
        />
      </n-form-item>
      <n-form-item label="边框宽">
        <n-input-number
          :value="Number(model.borderWidth) || 0"
          :min="0"
          :max="12"
          style="width: 100%"
          @update:value="(v) => patch('borderWidth', v)"
        />
      </n-form-item>
      <n-form-item label="圆角">
        <n-input-number
          :value="Number(model.borderRadius) || 0"
          :min="0"
          :max="40"
          style="width: 100%"
          @update:value="(v) => patch('borderRadius', v)"
        />
      </n-form-item>
      <n-form-item label="线型">
        <n-select
          :value="model.borderDasharray || 'none'"
          :options="dashOptions"
          @update:value="(v) => patch('borderDasharray', v)"
        />
      </n-form-item>
      <div class="preset-row">
        <span class="preset-label">快捷色</span>
        <button
          v-for="c in presets"
          :key="c"
          type="button"
          class="swatch"
          :style="{ background: c }"
          :title="c"
          @click="applyPreset(c)"
        />
      </div>
    </n-form>
  </div>
</template>

<script setup>
const props = defineProps({
  hasSelection: { type: Boolean, default: false },
  note: { type: String, default: '' },
  model: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['change', 'reset', 'note-change'])

const shapeOptions = [
  { label: '矩形', value: 'rectangle' },
  { label: '圆角矩形', value: 'roundedRectangle' },
  { label: '菱形', value: 'diamond' },
  { label: '平行四边形', value: 'parallelogram' },
  { label: '八角矩形', value: 'octagonalRectangle' },
  { label: '椭圆', value: 'ellipse' },
  { label: '圆形', value: 'circle' }
]

const dashOptions = [
  { label: '实线', value: 'none' },
  { label: '虚线', value: '5,5' },
  { label: '点划', value: '8,4,2,4' }
]

const presets = ['#549688', '#3b6ef5', '#dd6b32', '#7a5af0', '#e0484d', '#17916a', '#22263a', '#ffffff']

function patch(key, value) {
  emit('change', { ...props.model, [key]: value })
}

function onNoteInput(value) {
  emit('note-change', value || '')
}

function applyPreset(color) {
  emit('change', {
    ...props.model,
    fillColor: color,
    color: isLight(color) ? '#333333' : '#ffffff',
    borderColor: color,
    borderWidth: props.model.borderWidth > 0 ? props.model.borderWidth : 1
  })
}

function isLight(hex) {
  const h = String(hex || '').replace('#', '')
  if (h.length < 6) return true
  const r = parseInt(h.slice(0, 2), 16)
  const g = parseInt(h.slice(2, 4), 16)
  const b = parseInt(h.slice(4, 6), 16)
  return (r * 299 + g * 587 + b * 114) / 1000 > 160
}
</script>

<style scoped>
.node-style-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  color: var(--color-text, #161a17);
}
.panel-toolbar {
  flex: none;
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px 0;
}
.panel-empty {
  padding: 40px 18px;
  text-align: center;
  color: var(--color-text-muted, #87918b);
  font-size: 12px;
  line-height: 1.7;
}
.panel-form {
  flex: 1;
  overflow: auto;
  padding: 8px 12px 16px;
}
.panel-form :deep(.n-form-item-label) {
  white-space: nowrap;
}
.preset-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.preset-label {
  width: 100%;
  font-size: 12px;
  color: var(--color-text-muted, #87918b);
}
.swatch {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid var(--color-border, #e2e8e4);
  cursor: pointer;
  padding: 0;
}
.swatch:hover {
  transform: scale(1.08);
}
</style>
