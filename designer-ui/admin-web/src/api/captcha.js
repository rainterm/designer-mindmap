import request from './request'
import { createCaptchaApi } from '@captcha'

/**
 * 滑块验证码的接口客户端（后台侧传输层适配）。
 *
 * <p>共享模块只声明请求的形状，具体使用哪个 HTTP 客户端由各站自己决定
 * （见 designer-ui/shared/captcha/createCaptchaApi.js）。这里接上项目的 axios 实例，
 * 于是后台页面不必关心端点细节。</p>
 *
 * <p>公开 URL 是 `/app-api/captcha/**`（见共享模块里的 BASE_PATH），
 * 后端由 framework 的 AppCaptchaController 映射这个前缀，代理不需要改写。</p>
 *
 * <p><b>为什么可以直接复用 {@link request}</b>：它的响应拦截器对「非统一 R 结构」
 * （`code === undefined`）原样返回，而 AJ-Captcha 返回的正是
 * `{repCode, repMsg, repData}`，恰好落在那个分支里，语义是明确的而不是巧合
 * （见共享模块 createCaptchaApi 注释里的取舍）。</p>
 */
export const captchaApi = createCaptchaApi((path, body) => request.post(path, body))
