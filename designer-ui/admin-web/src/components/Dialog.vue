<template>
  <n-modal
    :show="show"
    preset="card"
    :title="hasHeaderSlot ? undefined : title"
    :style="modalStyle"
    :class="['app-dialog', contentClass, { 'app-dialog--full': isFullscreen }]"
    :mask-closable="maskClosable"
    :bordered="bordered"
    :segmented="{ content: true, footer: true }"
    :draggable="canDrag"
    @update:show="onShowUpdate"
    @after-leave="emit('after-leave')"
  >
    <template v-if="hasHeaderSlot" #header>
      <slot name="header" />
    </template>
    <template v-if="fullscreenable" #header-extra>
      <n-tooltip :show-arrow="false" placement="bottom">
        <template #trigger>
          <n-button
            quaternary
            circle
            class="app-dialog__fs-btn"
            :aria-label="isFullscreen ? '还原' : '全屏'"
            @click="toggleFullscreen"
          >
            <template #icon>
              <n-icon :component="isFullscreen ? ContractOutline : ExpandOutline" :size="18" />
            </template>
          </n-button>
        </template>
        {{ isFullscreen ? '还原' : '全屏' }}
      </n-tooltip>
    </template>
    <slot />
    <template v-if="showFooter" #footer>
      <slot name="footer">
        <n-space justify="end">
          <n-button @click="handleCancel">{{ cancelText }}</n-button>
          <n-button type="primary" :loading="loading" @click="emit('confirm')">
            {{ confirmText }}
          </n-button>
        </n-space>
      </slot>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, ref, useSlots, watch } from 'vue'
import { ContractOutline, ExpandOutline } from '@vicons/ionicons5'

/** 常用档位；特殊场景再用 width 覆盖 */
const SIZE_MAP = {
  md: 560,
  lg: 720,
  xl: 960
}

const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: '' },
  /** md=560 / lg=720 / xl=960 / full=初始全屏，默认 md */
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['md', 'lg', 'xl', 'full'].includes(v)
  },
  /** 显式宽度优先于 size（number 为 px，或 CSS 字符串；全屏时忽略） */
  width: { type: [Number, String], default: null },
  /** 是否显示全屏/还原按钮，默认开启 */
  fullscreenable: { type: Boolean, default: true },
  /** 是否可拖动（按住标题栏），全屏时自动禁用 */
  draggable: { type: Boolean, default: true },
  loading: { type: Boolean, default: false },
  maskClosable: { type: Boolean, default: false },
  bordered: { type: Boolean, default: true },
  /** 为 false 时不渲染 footer（只读弹窗 / 图表等） */
  showFooter: { type: Boolean, default: true },
  /** 附加到 n-modal/card 的 class（如聊天弹窗禁内容区滚动） */
  contentClass: { type: String, default: '' },
  confirmText: { type: String, default: '保存' },
  cancelText: { type: String, default: '取消' }
})

const emit = defineEmits(['update:show', 'confirm', 'cancel', 'after-leave', 'update:fullscreen'])
const slots = useSlots()
const hasHeaderSlot = computed(() => !!slots.header)

const isFullscreen = ref(props.size === 'full')
const canDrag = computed(() => props.draggable && !isFullscreen.value)

/** 还原时使用的档位（size=full 时还原为 md） */
const windowedSize = computed(() => (props.size === 'full' ? 'md' : props.size))

watch(
  () => props.show,
  (visible) => {
    if (visible) {
      isFullscreen.value = props.size === 'full'
    }
  }
)

watch(isFullscreen, (v) => emit('update:fullscreen', v))

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
}

const resolvedWidth = computed(() => {
  if (props.width != null && props.width !== '') {
    return typeof props.width === 'number' ? `${props.width}px` : props.width
  }
  const px = SIZE_MAP[windowedSize.value] || SIZE_MAP.md
  return `min(${px}px, calc(100vw - 32px))`
})

const modalStyle = computed(() => {
  if (isFullscreen.value) {
    return {
      width: '100vw',
      maxWidth: '100vw',
      height: '100vh',
      maxHeight: '100vh',
      margin: 0,
      borderRadius: 0
    }
  }
  const style = { width: resolvedWidth.value }
  if (props.contentClass.includes('app-dialog--chat')) {
    style.maxHeight = 'calc(100vh - 48px)'
    style.height = 'min(720px, calc(100vh - 48px))'
    style.display = 'flex'
    style.flexDirection = 'column'
  }
  return style
})

function onShowUpdate(v) {
  emit('update:show', v)
}

function handleCancel() {
  emit('update:show', false)
  emit('cancel')
}
</script>

<style>
/* n-modal 传送到 body，需非 scoped */
.n-modal-body-wrapper:has(.app-dialog--full) {
  padding: 0;
  align-items: stretch;
}
.n-modal-scroll-content:has(.app-dialog--full) {
  min-height: 100%;
  display: flex;
  align-items: stretch;
}
.app-dialog--full.n-card {
  display: flex;
  flex-direction: column;
  width: 100vw !important;
  max-width: 100vw !important;
  height: 100vh !important;
  max-height: 100vh !important;
  margin: 0;
  border-radius: 0;
}
.app-dialog--full .n-card__content {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.app-dialog--full .n-card__footer {
  flex-shrink: 0;
}
/* 聊天类弹窗：外层与 card 内容区不滚动，仅内部消息列表滚动 */
.n-modal-body-wrapper:has(.app-dialog--chat) {
  overflow: hidden;
}
.n-modal-scroll-content:has(.app-dialog--chat) {
  max-height: 100%;
  overflow: hidden;
}
.app-dialog--chat.n-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.app-dialog--chat .n-card__content {
  flex: 1;
  min-height: 0;
  overflow: hidden !important;
  padding: 0 !important;
  display: flex;
  flex-direction: column;
}
.app-dialog--chat.app-dialog--full .n-card__content {
  overflow: hidden !important;
}
.app-dialog__fs-btn {
  margin-right: 2px;
}
.app-dialog .n-card-header.n-draggable {
  cursor: move;
  user-select: none;
}
</style>
