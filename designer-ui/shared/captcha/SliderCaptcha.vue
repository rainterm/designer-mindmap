<template>
  <div class="slider-captcha" :class="{ 'is-verified': verified, 'is-shaking': shaking }">
    <div class="slider-captcha__stage" ref="stageRef">
      <img v-if="originalImage" class="slider-captcha__back" :src="originalImage" alt="" draggable="false" />
      <img
        v-if="jigsawImage"
        class="slider-captcha__block"
        :src="jigsawImage"
        alt=""
        draggable="false"
        :style="{ left: `${dragX}px` }"
      />
      <div v-if="!originalImage" class="slider-captcha__placeholder">
        {{ loading ? '加载中…' : '加载失败' }}
      </div>
    </div>

    <div class="slider-captcha__bar">
      <div class="slider-captcha__fill" :style="{ width: `${dragX}px` }" />
      <div
        class="slider-captcha__handle"
        :style="{ left: `${dragX}px` }"
        role="slider"
        tabindex="0"
        :aria-valuenow="Math.round(dragX)"
        aria-valuemin="0"
        :aria-valuemax="Math.round(maxDrag())"
        :aria-label="ariaLabel"
        @pointerdown="onPointerDown"
        @keydown="onKeydown"
      >
        <span v-if="verified" class="slider-captcha__tick">✓</span>
        <span v-else>›››</span>
      </div>
      <span class="slider-captcha__hint">{{ hintText }}</span>
    </div>

    <button type="button" class="slider-captcha__refresh" :disabled="loading" @click="refresh">
      换一张
    </button>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { IMAGE_WIDTH, PIECE_WIDTH, buildCredentials, toServerOffset } from './sliderCaptcha.js'

/**
 * 滑块拼图验证码（AJ-Captcha blockPuzzle 的前端实现），多端入口共用（如后台登录）。
 *
 * <p>不直接依赖任何 HTTP 客户端：请求能力由 {@link api} 注入
 * （用 {@code createCaptchaApi} 配各站的传输层生成），组件只负责交互与坐标换算。</p>
 *
 * <p>样式依赖的 CSS 变量（{@code --color-body}、{@code --color-border}、{@code --color-primary}、
 * {@code --color-text-muted}、{@code --radius}）在两个站点的 token 文件里都有定义，
 * 所以同一份组件在两处都能直接贴合主题；{@code --color-success} 部分主题才会定义，
 * 故给一个兜底值而不让后台的「验证通过」态失色。</p>
 */
const props = defineProps({
  /** 由 createCaptchaApi 生成的接口客户端 */
  api: { type: Object, required: true },
  /** 验证码类型，仅用于无障碍文案（块拼图 / 文字点选） */
  ariaLabel: { type: String, default: '拖动滑块完成拼图' }
})

const emit = defineEmits(['verified'])

const stageRef = ref(null)

const loading = ref(false)
const checking = ref(false)
const verified = ref(false)
const shaking = ref(false)

const originalImage = ref('')
const jigsawImage = ref('')
const token = ref('')
const secretKey = ref('')

const dragX = ref(0)
const dragging = ref(false)

/** 舞台当前 CSS 宽度 / 拼图块 CSS 宽度，拖拽时量一次即可 */
let stageWidth = 0
let pieceWidth = 0
let dragStartX = 0
let dragStartOffset = 0

const hintText = computed(() => {
  if (verified.value) return '验证通过'
  if (checking.value) return '校验中…'
  return '拖动滑块将拼图对齐缺口'
})

/** 拼图块可拖动的最大距离（到底就贴住右边缘） */
function maxDrag() {
  return Math.max(0, stageWidth - pieceWidth)
}

function measure() {
  const el = stageRef.value
  if (!el) return
  stageWidth = el.clientWidth
  pieceWidth = (stageWidth * PIECE_WIDTH) / IMAGE_WIDTH
}

/**
 * 取一张新验证码。
 *
 * <p>每次都要重置拖拽状态与 token —— 服务端的验证码是**一次性**的：
 * 校验一次（无论成败）这张就作废了，沿用旧 token 只会一直失败。</p>
 */
async function refresh() {
  loading.value = true
  verified.value = false
  dragX.value = 0
  originalImage.value = ''
  jigsawImage.value = ''
  token.value = ''
  secretKey.value = ''
  try {
    const data = await props.api.fetchPuzzle()
    originalImage.value = `data:image/png;base64,${data.originalImageBase64}`
    jigsawImage.value = `data:image/png;base64,${data.jigsawImageBase64}`
    token.value = data.token
    secretKey.value = data.secretKey || ''
  } catch {
    // 失败提示交给调用方统一处理，这里只留下「加载失败」占位
  } finally {
    loading.value = false
  }
}

function onPointerDown(event) {
  if (verified.value || checking.value || loading.value || !token.value) return
  measure()
  dragStartX = event.clientX
  dragStartOffset = dragX.value
  dragging.value = true
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
  window.addEventListener('pointercancel', onPointerUp)
  event.preventDefault()
}

function onPointerMove(event) {
  if (!dragging.value) return
  const next = dragStartOffset + (event.clientX - dragStartX)
  dragX.value = Math.min(Math.max(next, 0), maxDrag())
}

async function onPointerUp() {
  if (!dragging.value) return
  dragging.value = false
  detachDragListeners()
  if (dragX.value <= 0) return
  await submit()
}

/** 键盘可达：方向键微调，回车提交（滑块不能只认鼠标） */
function onKeydown(event) {
  if (verified.value || checking.value || !token.value) return
  measure()
  const step = event.shiftKey ? 10 : 2
  if (event.key === 'ArrowRight') {
    dragX.value = Math.min(dragX.value + step, maxDrag())
  } else if (event.key === 'ArrowLeft') {
    dragX.value = Math.max(dragX.value - step, 0)
  } else if (event.key === 'Enter' || event.key === ' ') {
    if (dragX.value > 0) submit()
  } else {
    return
  }
  event.preventDefault()
}

async function submit() {
  checking.value = true
  try {
    const offset = toServerOffset(dragX.value, stageWidth)
    const { pointJson, captchaVerification } = buildCredentials({
      token: token.value,
      offset,
      secretKey: secretKey.value
    })

    if (await props.api.check(token.value, pointJson)) {
      verified.value = true
      emit('verified', captchaVerification)
    } else {
      // 没对齐就换一张重来：服务端判定失败后这张已经作废，原地重试必然继续失败
      shake()
      await refresh()
    }
  } catch {
    shake()
    await refresh()
  } finally {
    checking.value = false
  }
}

function shake() {
  shaking.value = true
  window.setTimeout(() => {
    shaking.value = false
  }, 400)
}

function detachDragListeners() {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('pointercancel', onPointerUp)
}

onMounted(refresh)
onBeforeUnmount(detachDragListeners)

defineExpose({
  /** 供上层在凭据被消费后重新拉一张（如提交失败需要重新验证） */
  refresh,
  /** 上层若自行重置流程（如登录成功回显后再失败），可主动清掉「验证通过」态 */
  reset: () => {
    verified.value = false
    dragX.value = 0
  }
})
</script>

<style scoped>
.slider-captcha {
  position: relative;
  width: 100%;
  max-width: 310px;
}

.slider-captcha__stage {
  position: relative;
  width: 100%;
  /* 按底图 310×155 的固有比例留位，图片到位前不跳动 */
  aspect-ratio: 310 / 155;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body);
  user-select: none;
  touch-action: none;
}

.slider-captcha__back {
  display: block;
  width: 100%;
  height: 100%;
}

.slider-captcha__block {
  position: absolute;
  top: 0;
  height: 100%;
  width: auto;
  /* 拖动时不要过渡，否则滑块会「追」不上指针 */
  transition: none;
}

.slider-captcha__placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  font-size: 13px;
}

.slider-captcha__bar {
  position: relative;
  height: 40px;
  margin-top: 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body);
  overflow: hidden;
}

.slider-captcha__fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: color-mix(in srgb, var(--color-primary) 16%, transparent);
}

.slider-captcha__handle {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 100%;
  border-radius: var(--radius);
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  letter-spacing: 1px;
  cursor: grab;
}

.slider-captcha__handle:active {
  cursor: grabbing;
}

.slider-captcha__handle:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.slider-captcha__hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: var(--color-text-muted);
  font-size: 13px;
  pointer-events: none;
}

.slider-captcha__refresh {
  position: absolute;
  top: 6px;
  right: 6px;
  padding: 2px 8px;
  border: none;
  border-radius: var(--radius);
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
}

.slider-captcha__refresh:disabled {
  opacity: 0.5;
  cursor: default;
}

.slider-captcha.is-verified .slider-captcha__fill {
  background: color-mix(in srgb, var(--color-success, #18a058) 22%, transparent);
}

.slider-captcha.is-verified .slider-captcha__handle {
  background: var(--color-success, #18a058);
}

.slider-captcha.is-verified .slider-captcha__hint {
  color: var(--color-success, #18a058);
}

@keyframes slider-captcha-shake {
  0%,
  100% {
    transform: translateX(0);
  }
  20%,
  60% {
    transform: translateX(-6px);
  }
  40%,
  80% {
    transform: translateX(6px);
  }
}

.slider-captcha.is-shaking .slider-captcha__stage {
  animation: slider-captcha-shake 0.4s;
}
</style>
