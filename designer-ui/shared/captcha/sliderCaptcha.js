import AES from 'crypto-js/aes.js'
import Utf8 from 'crypto-js/enc-utf8.js'
import ECB from 'crypto-js/mode-ecb.js'
import Pkcs7 from 'crypto-js/pad-pkcs7.js'

/**
 * 滑块验证码的线协议工具，多端入口共用（如后台登录）。
 *
 * <p>AJ-Captcha 的官方前端是 jQuery 写的 `verify.js`（外加 `ase.js` + `crypto-js`），
 * 直接塞进两个新前端里既带进 jQuery，又要在 `window` 上挂全局。
 * 这里只把它的**线协议**搬过来（已在后端逐个字段验证过），交互层用 Vue 自己实现：</p>
 *
 * <ul>
 *   <li>{@code POST /app-api/captcha/get} → {@code {originalImageBase64, jigsawImageBase64, token, secretKey}}</li>
 *   <li>{@code POST /app-api/captcha/check} ← {@code {captchaType, token, pointJson}}</li>
 *   <li>业务请求 ← {@code captchaVerification}</li>
 * </ul>
 *
 * <p>公开前缀是 {@code /app-api/captcha}（后端另有 AppCaptchaController 映射它，
 * 插件原生端点 {@code /captcha} 不对外），这里只描述浏览器看到的那一层。</p>
 *
 * <p>其中两个凭据都是「明文 JSON 做 AES-128-ECB/PKCS7 再 Base64」，与官方 `ase.js`
 * 中的 {@code aesEncrypt} 完全一致（服务端用同一个 secretKey 解密）。
 * 明文里的 {@code y} 恒为 5 —— 拼图块是整高竖条，纵向不参与比对。</p>
 */

/**
 * 服务端底图的自然宽度（像素）。
 *
 * <p>服务端把「缺口位置」存在这个坐标系里（实测值域落在 0..310），
 * 与是否缩放展示无关。官方客户端把它硬编码为 310，这里保持一致。</p>
 */
export const IMAGE_WIDTH = 310

/** 底图自然高度，仅用于给容器留出宽高比、避免图片加载时布局跳动 */
export const IMAGE_HEIGHT = 155

/** 拼图块画布宽度，用来算出它占底图的比例（底图 310 宽、拼图块 47 宽） */
export const PIECE_WIDTH = 47

/**
 * 拖拽距离（CSS 像素）→ 服务端坐标系里的 x。
 *
 * <p>底图按容器宽度等比缩放展示，而服务端只认原图坐标，
 * 所以必须按 {@code 310 / 展示宽度} 换算回去（官方客户端同此算法）。
 * 不做四舍五入：服务端按 ±slip-offset 容差判定，浮点值更贴近真实拖拽量。</p>
 *
 * @param {number} draggedPx 拖拽了多长（CSS 像素）
 * @param {number} displayedWidth 底图当前展示宽度（CSS 像素）
 */
export function toServerOffset(draggedPx, displayedWidth) {
  if (!displayedWidth || displayedWidth <= 0) return 0
  return (draggedPx * IMAGE_WIDTH) / displayedWidth
}

/**
 * AES-128-ECB + PKCS7 + Base64，等价于官方 `ase.js` 的 `aesEncrypt`。
 *
 * <p>这里用 crypto-js 而不是 Web Crypto：标准库的 `SubtleCrypto` **不提供 ECB 模式**，
 * 而 ECB 正是对方协议的既定形态（换模式服务端就解不开）。</p>
 */
function aesEncrypt(plain, secretKey) {
  return AES.encrypt(Utf8.parse(plain), Utf8.parse(secretKey), {
    mode: ECB,
    padding: Pkcs7
  }).toString()
}

/**
 * 由拖拽结果生成提交 /check 与提交业务请求分别需要的两个凭据。
 *
 * <p>两者必须由**同一个**明文算出（官方客户端也是这么做的）：
 * {@code pointJson} 交给 /check 判人机，{@code captchaVerification}
 * 交给业务接口做二次校验，服务端会核对二者是否指向同一份记录。</p>
 *
 * @param {object} params
 * @param {string} params.token 本次验证码的 token
 * @param {number} params.offset 服务端坐标系里的 x（见 {@link toServerOffset}）
 * @param {string} params.secretKey 本次验证码的 AES 密钥
 * @returns {{pointJson: string, captchaVerification: string}}
 */
export function buildCredentials({ token, offset, secretKey }) {
  const point = JSON.stringify({ x: offset, y: 5 })
  // secretKey 为空说明服务端关了 AES（aj.captcha.aes-status=false），此时官方客户端
  // 也是发明文。保留这条分支免得改配置时前端悄悄失效。
  if (!secretKey) {
    return { pointJson: point, captchaVerification: `${token}---${point}` }
  }
  return {
    pointJson: aesEncrypt(point, secretKey),
    captchaVerification: aesEncrypt(`${token}---${point}`, secretKey)
  }
}
