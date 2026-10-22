/**
 * 密码策略，与后端 PasswordValidator 是同一份规则的镜像
 * （designer-module-system/system-service/src/main/java/com/designer/admin/system/util/PasswordValidator.java）。
 * 改规则或改措辞时两边一起改，否则会出现「前端放行、后端打回」的两套口径。
 *
 * 两条规则性质不同，落地范围也不同：
 * - 长度：下限 6 是安全要求，上限 50 是防御性收口；
 * - 复杂度（大写字母 + 小写字母 + 数字 + 特殊符号）：**只针对新设密码**，
 *   因此只在写入侧校验（新增用户、重置密码、个人改密）。登录页刻意不验 ——
 *   历史上纯字母或纯数字的老密码即使输对了也登不进来，账号会被锁死
 *   （后端 LoginDTO 同理）。
 */
export const PASSWORD_MIN_LENGTH = 6
export const PASSWORD_MAX_LENGTH = 50

/** 长度不合规的提示，措辞与后端 PasswordValidator.LENGTH_MESSAGE 一致 */
export const PASSWORD_LENGTH_MESSAGE = `密码长度需在 ${PASSWORD_MIN_LENGTH}~${PASSWORD_MAX_LENGTH} 个字符之间`

/** 复杂度不合规的提示，措辞与后端 PasswordValidator.COMPLEXITY_MESSAGE 一致 */
export const PASSWORD_COMPLEXITY_MESSAGE = '密码需同时包含大写字母、小写字母、数字和特殊符号'

/** 输入框下方常驻的规则说明（只讲长度，用于登录这类不校验复杂度的场景） */
export const PASSWORD_LENGTH_HINT = `${PASSWORD_MIN_LENGTH}~${PASSWORD_MAX_LENGTH} 个字符`

/** 输入框下方常驻的规则说明（长度 + 复杂度），用于所有写入密码的表单 */
export const PASSWORD_HINT = `${PASSWORD_LENGTH_HINT}，需包含大写字母、小写字母、数字和特殊符号`

/**
 * 复杂度正则，与后端 PasswordValidator.STRENGTH_REGEXP 等价：
 * 要求同时包含大写字母、小写字母、数字、特殊符号四类字符。
 *
 * 「特殊符号」取的是 ASCII 可见标点（码点 0x21–0x2F、0x3A–0x40、0x5B–0x60、0x7B–0x7E 四段，
 * 即 POSIX 的 `[:punct:]`），**有意排除空白与中文等非 ASCII 字符** —— 否则一个汉字或一个空格
 * 就能凑满「特殊符号」，规则形同虚设。后端正则里 `[` 需要转义，这里保持一致。
 *
 * 这里显式加 `^...$` 是为了对齐 Java 的 `matches()`（整串匹配）语义：
 * JS 的 `test()` 本身是「存在子串即通过」，不加锚点会在个别输入上（如含换行）
 * 与后端判断不一致。末尾 `.+` 不匹配换行，含换行的密码同样会被拒。
 */
export const PASSWORD_STRENGTH_REGEXP = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[!-/:-@[-`{-~]).+$/

/** 密码长度是否合规 */
export function isValidPasswordLength(value) {
  return typeof value === 'string' && value.length >= PASSWORD_MIN_LENGTH && value.length <= PASSWORD_MAX_LENGTH
}

/** 是否同时包含大写字母、小写字母、数字、特殊符号 */
export function hasAllCharTypes(value) {
  return typeof value === 'string' && PASSWORD_STRENGTH_REGEXP.test(value)
}

/** 长度与复杂度是否都合规 */
export function isValidPassword(value) {
  return isValidPasswordLength(value) && hasAllCharTypes(value)
}

/**
 * 密码校验规则，各写入入口共用（新增用户、重置密码、个人改密）。
 *
 * 集中生成而不是各处手写，是因为这几个页面原先各写一份：有的只卡下限、
 * 有的写死 6~50、登录页干脆没有，同一条规则改一处就得记得改四处。
 *
 * @param {object} [options]
 * @param {boolean} [options.required] 是否必填
 * @param {string} [options.requiredMessage] 必填时的提示语
 * @param {boolean} [options.complexity] 是否校验复杂度（登录页传 false）
 * @returns {Array} naive-ui 表单规则
 */
export function buildPasswordRules({ required = false, requiredMessage = '请输入密码', complexity = true } = {}) {
  const rules = []
  if (required) {
    rules.push({ required: true, message: requiredMessage, trigger: ['input', 'blur'] })
  }
  // 空值一律放行，交给上面的 required 规则去提示：修改用户时留空表示不改密码，
  // 这种空值不该被报成「长度/复杂度不合规」
  rules.push({
    validator: (rule, value) => !value || isValidPasswordLength(value),
    message: PASSWORD_LENGTH_MESSAGE,
    trigger: ['input', 'blur']
  })
  if (complexity) {
    rules.push({
      validator: (rule, value) => !value || hasAllCharTypes(value),
      message: PASSWORD_COMPLEXITY_MESSAGE,
      trigger: ['input', 'blur']
    })
  }
  return rules
}

/**
 * 「不能与另一字段相同」规则，例如新密码不能与原密码相同。
 *
 * @param {() => string} getOtherValue 取对照值的函数（惰性取值，避免闭包捕获旧值）
 * @param {string} message 提示语
 */
export function buildNotEqualToRule(getOtherValue, message) {
  return {
    validator: (rule, value) => !value || value !== getOtherValue(),
    message,
    trigger: ['input', 'blur']
  }
}

/**
 * 「必须与另一字段相同」规则，例如确认密码必须等于新密码。
 *
 * @param {() => string} getOtherValue 取对照值的函数
 * @param {string} message 提示语
 */
export function buildEqualToRule(getOtherValue, message) {
  return {
    validator: (rule, value) => !value || value === getOtherValue(),
    message,
    trigger: ['input', 'blur']
  }
}
