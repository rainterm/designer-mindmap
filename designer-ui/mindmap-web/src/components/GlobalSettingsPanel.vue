<template>
  <div class="global-settings">
    <div class="panel-toolbar">
      <n-button quaternary @click="emit('reset')">重置设置</n-button>
    </div>
    <n-form label-placement="left" label-width="80" class="panel-form">
      <n-form-item label="结构">
        <n-select
          :value="model.layout"
          :options="layoutOptions"
          @update:value="(v) => patch('layout', v)"
        />
      </n-form-item>
      <n-form-item label="主题">
        <n-select
          :value="model.theme"
          :options="themeOptions"
          @update:value="(v) => patch('theme', v)"
        />
      </n-form-item>
      <n-form-item label="背景">
        <n-color-picker
          :value="model.backgroundColor || '#fafafa'"
          :modes="['hex']"
          :show-alpha="false"
          @update:value="(v) => patch('backgroundColor', v)"
        />
      </n-form-item>
      <n-divider title-placement="left" style="margin: 4px 0 10px">连线</n-divider>
      <n-form-item label="连线色">
        <n-color-picker
          :value="model.lineColor || '#549688'"
          :modes="['hex']"
          :show-alpha="false"
          @update:value="(v) => patch('lineColor', v)"
        />
      </n-form-item>
      <n-form-item label="连线宽">
        <n-input-number
          :value="Number(model.lineWidth) || 1"
          :min="1"
          :max="12"
          style="width: 100%"
          @update:value="(v) => patch('lineWidth', v)"
        />
      </n-form-item>
      <n-form-item label="连线风">
        <n-select
          :value="model.lineStyle || 'straight'"
          :options="lineStyleOptions"
          @update:value="(v) => patch('lineStyle', v)"
        />
      </n-form-item>
      <n-form-item label="线型">
        <n-select
          :value="model.lineDasharray || 'none'"
          :options="dashOptions"
          @update:value="(v) => patch('lineDasharray', v)"
        />
      </n-form-item>
      <n-divider title-placement="left" style="margin: 4px 0 10px">一级（根）</n-divider>
      <n-form-item label="填充">
        <n-color-picker
          :value="model.rootFillColor || '#549688'"
          :modes="['hex']"
          :show-alpha="false"
          @update:value="(v) => patch('rootFillColor', v)"
        />
      </n-form-item>
      <n-form-item label="字号">
        <n-input-number
          :value="Number(model.rootFontSize) || 16"
          :min="10"
          :max="48"
          style="width: 100%"
          @update:value="(v) => patch('rootFontSize', v)"
        />
      </n-form-item>
      <n-form-item label="水平内边">
        <n-input-number
          :value="Number(model.rootPaddingX) || 15"
          :min="0"
          :max="80"
          style="width: 100%"
          @update:value="(v) => patch('rootPaddingX', v)"
        />
      </n-form-item>
      <n-form-item label="垂直内边">
        <n-input-number
          :value="Number(model.rootPaddingY) || 5"
          :min="0"
          :max="40"
          style="width: 100%"
          @update:value="(v) => patch('rootPaddingY', v)"
        />
      </n-form-item>
      <n-divider title-placement="left" style="margin: 4px 0 10px">二级</n-divider>
      <n-form-item label="填充">
        <n-color-picker
          :value="model.secondFillColor || '#ffffff'"
          :modes="['hex']"
          :show-alpha="false"
          @update:value="(v) => patch('secondFillColor', v)"
        />
      </n-form-item>
      <n-form-item label="边框色">
        <n-color-picker
          :value="model.secondBorderColor || '#549688'"
          :modes="['hex']"
          :show-alpha="false"
          @update:value="(v) => patch('secondBorderColor', v)"
        />
      </n-form-item>
      <n-form-item label="字号">
        <n-input-number
          :value="Number(model.secondFontSize) || 16"
          :min="10"
          :max="48"
          style="width: 100%"
          @update:value="(v) => patch('secondFontSize', v)"
        />
      </n-form-item>
      <n-form-item label="水平内边">
        <n-input-number
          :value="Number(model.secondPaddingX) || 15"
          :min="0"
          :max="80"
          style="width: 100%"
          @update:value="(v) => patch('secondPaddingX', v)"
        />
      </n-form-item>
      <n-form-item label="垂直内边">
        <n-input-number
          :value="Number(model.secondPaddingY) || 5"
          :min="0"
          :max="40"
          style="width: 100%"
          @update:value="(v) => patch('secondPaddingY', v)"
        />
      </n-form-item>
      <n-form-item label="水平间距">
        <n-input-number
          :value="Number(model.secondMarginX) || 100"
          :min="20"
          :max="300"
          style="width: 100%"
          @update:value="(v) => patch('secondMarginX', v)"
        />
      </n-form-item>
      <n-form-item label="垂直间距">
        <n-input-number
          :value="Number(model.secondMarginY) || 40"
          :min="0"
          :max="200"
          style="width: 100%"
          @update:value="(v) => patch('secondMarginY', v)"
        />
      </n-form-item>
      <n-divider title-placement="left" style="margin: 4px 0 10px">三级及以下</n-divider>
      <n-form-item label="填充">
        <div class="color-row">
          <n-color-picker
            :value="colorOrFallback(model.nodeFillColor, '#ffffff')"
            :modes="['hex']"
            :show-alpha="false"
            @update:value="(v) => patch('nodeFillColor', v)"
          />
          <n-button
            v-if="isTransparent(model.nodeFillColor)"
            quaternary
            @click="patch('nodeFillColor', '#ffffff')"
          >
            透明，点击应用白色
          </n-button>
        </div>
      </n-form-item>
      <n-form-item label="文字">
        <n-color-picker
          :value="model.nodeColor || '#333333'"
          :modes="['hex']"
          :show-alpha="false"
          @update:value="(v) => patch('nodeColor', v)"
        />
      </n-form-item>
      <n-form-item label="边框色">
        <div class="color-row">
          <n-color-picker
            :value="colorOrFallback(model.nodeBorderColor, '#549688')"
            :modes="['hex']"
            :show-alpha="false"
            @update:value="(v) => patch('nodeBorderColor', v)"
          />
          <n-button
            v-if="isTransparent(model.nodeBorderColor)"
            quaternary
            @click="patch('nodeBorderColor', '#549688')"
          >
            透明，点击应用边框色
          </n-button>
        </div>
      </n-form-item>
      <n-form-item label="边框宽">
        <n-input-number
          :value="model.nodeBorderWidth ?? 0"
          :min="0"
          :max="12"
          style="width: 100%"
          @update:value="(v) => patch('nodeBorderWidth', v ?? 0)"
        />
      </n-form-item>
      <n-form-item label="字号">
        <n-input-number
          :value="Number(model.nodeFontSize) || 14"
          :min="10"
          :max="48"
          style="width: 100%"
          @update:value="(v) => patch('nodeFontSize', v)"
        />
      </n-form-item>
      <n-form-item label="水平内边">
        <n-input-number
          :value="Number(model.nodePaddingX) || 15"
          :min="0"
          :max="80"
          style="width: 100%"
          @update:value="(v) => patch('nodePaddingX', v)"
        />
      </n-form-item>
      <n-form-item label="垂直内边">
        <n-input-number
          :value="Number(model.nodePaddingY) || 5"
          :min="0"
          :max="40"
          style="width: 100%"
          @update:value="(v) => patch('nodePaddingY', v)"
        />
      </n-form-item>
      <n-form-item label="水平间距">
        <n-input-number
          :value="Number(model.nodeMarginX) || 50"
          :min="10"
          :max="200"
          style="width: 100%"
          @update:value="(v) => patch('nodeMarginX', v)"
        />
      </n-form-item>
      <n-form-item label="垂直间距">
        <n-input-number
          :value="Number(model.nodeMarginY) || 0"
          :min="0"
          :max="120"
          style="width: 100%"
          @update:value="(v) => patch('nodeMarginY', v)"
        />
      </n-form-item>
    </n-form>
  </div>
</template>

<script setup>
import { themeOptions } from '../themes'

defineProps({
  model: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['change', 'reset'])

const layoutOptions = [
  { label: '逻辑结构', value: 'logicalStructure' },
  { label: '思维导图', value: 'mindMap' },
  { label: '组织结构', value: 'organizationStructure' },
  { label: '目录组织', value: 'catalogOrganization' },
  { label: '时间轴(横)', value: 'timeline' },
  { label: '时间轴(竖)', value: 'verticalTimeline' },
  { label: '鱼骨图', value: 'fishbone' }
]

const lineStyleOptions = [
  { label: '直线', value: 'straight' },
  { label: '曲线', value: 'curve' },
  { label: '直连', value: 'direct' }
]

const dashOptions = [
  { label: '实线', value: 'none' },
  { label: '虚线', value: '5,5' },
  { label: '点划', value: '8,4,2,4' }
]

function isTransparent(v) {
  return !v || v === 'transparent'
}

function colorOrFallback(v, fallback) {
  return isTransparent(v) ? fallback : v
}

function patch(key, value) {
  emit('change', { key, value })
}
</script>

<style scoped>
.global-settings {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-toolbar {
  flex: none;
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px 0;
}
.panel-form {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 8px 12px 16px;
}
.panel-form :deep(.n-form-item-label) {
  white-space: nowrap;
}
.color-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}
</style>
