import { clientUid } from './clientUid.js'

/**
 * 滑块验证码接口（AJ-Captcha），传输层由调用方注入。
 *
 * <p><b>为什么要注入 request 而不是直接用某个 HTTP 客户端</b>：
 * 这两个端点可能被多套前端消费 —— 例如 Nuxt 3（用全局 `$fetch`），
 * admin-web 是 Vite + axios（且带统一拦截器）。把 `$fetch` 写死在这里就绑死了 Nuxt；
 * 反过来绑 axios 又给 Nuxt 引入一个不必要的依赖。所以这里只声明**形状**：
 * {@code request(path, body)} 返回已解析的响应 JSON。</p>
 *
 * <p><b>为什么不用各站的统一响应封装</b>：两站的封装都建立在后端统一的
 * `R` 结构（{@code {code, msg, data}}）上，而这几个端点由第三方插件提供，
 * 返回的是它自己的 {@code {repCode, repMsg, repData}}（成功码是字符串 `0000`）。
 * 硬套过去会落进「非 R 结构就原样返回」的分支，语义靠巧合成立 ——
 * 不如在这里显式处理对方协议。</p>
 */

/** AJ-Captcha 的成功码（字符串，不是数字） */
export const REP_CODE_SUCCESS = '0000'

/** 验证码类型：插件只支持 blockPuzzle（滑块）与 clickWord（点选），这里用滑块 */
export const CAPTCHA_TYPE = 'blockPuzzle'

/**
 * 公开 URL 前缀。
 *
 * <p>AJ-Captcha 插件把端点注册在根路径 {@code /captcha/**}，且没有前缀配置项。
 * 后端在 framework 里另注册了一个映射 {@code /app-api/captcha/**} 的薄 controller
 * （见 {@code AppCaptchaController}），所以浏览器侧只需要认这一个前缀 ——
 * nginx 与 dev 代理都按普通 {@code /app-api} 转发，不做任何改写。
 * 插件原生的 {@code /captcha/**} 两个站点都已显式 404，不对外。</p>
 */
const BASE_PATH = '/app-api/captcha'

/**
 * 造一个验证码接口客户端。
 *
 * @param {(path: string, body: object) => Promise<any>} request
 *        POST 请求函数，返回解析后的 JSON（AJ-Captcha 的原始响应体）
 * @returns {{fetchPuzzle: Function, check: Function}}
 */
export function createCaptchaApi(request) {
  return {
    /**
     * 取一张滑块验证码。
     *
     * @returns {Promise<{token: string, secretKey: string, originalImageBase64: string, jigsawImageBase64: string}>}
     */
    async fetchPuzzle() {
      const res = await request(`${BASE_PATH}/get`, {
        captchaType: CAPTCHA_TYPE,
        clientUid: clientUid(),
        ts: Date.now()
      })
      if (res?.repCode !== REP_CODE_SUCCESS || !res.repData?.token) {
        throw new Error(res?.repMsg || '验证码加载失败，请稍后重试')
      }
      return res.repData
    },

    /**
     * 校验滑块位置。
     *
     * @param {string} token 本次验证码的 token
     * @param {string} pointJson {@link ./sliderCaptcha.js} 生成的加密偏移量
     * @returns {Promise<boolean>} 是否对齐（未对齐需要换一张重试）
     */
    async check(token, pointJson) {
      const res = await request(`${BASE_PATH}/check`, {
        captchaType: CAPTCHA_TYPE,
        token,
        pointJson,
        clientUid: clientUid(),
        ts: Date.now()
      })
      return res?.repCode === REP_CODE_SUCCESS
    }
  }
}
