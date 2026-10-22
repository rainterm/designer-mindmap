/**
 * 滑块验证码共享模块（多端入口共用的唯一来源）。
 *
 * <p>目录用的是**纯源码共享**而不是独立 npm 包：两个前端各自 `pnpm install --frozen-lockfile`
 * 独立构建（见 deploy 下的两个 Dockerfile），引入 workspace 会改变两者的 node_modules 布局
 * 与锁文件，收益却只是省下三行 alias。这里靠各自的构建配置指过来即可。</p>
 *
 * <p>用法：各站用 {@link createCaptchaApi} 配上自己的传输层，把结果传给 {@code SliderCaptcha}。</p>
 */
export { default as SliderCaptcha } from './SliderCaptcha.vue'
export { createCaptchaApi, CAPTCHA_TYPE, REP_CODE_SUCCESS } from './createCaptchaApi.js'
export { clientUid } from './clientUid.js'
export {
  IMAGE_WIDTH,
  IMAGE_HEIGHT,
  PIECE_WIDTH,
  toServerOffset,
  buildCredentials
} from './sliderCaptcha.js'
