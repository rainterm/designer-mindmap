package com.designer.admin.system.model.vo.auth;

import lombok.Data;

/**
 * 登录前的人机验证提示。
 *
 * <p>滑块验证码本身不从这里下发：底图与拼图由 AJ-Captcha 的 {@code /app-api/captcha/get} 现生成，
 * 凭据由 {@code /app-api/captcha/check} 下发（各入口共用同一套端点，
 * 由 framework 的 AppCaptchaController 映射到同一个 CaptchaService）。
 * 这个对象只回答一个问题 —— <b>本次登录要不要验</b>
 * （全局开关开启且该账号连续失败达阈值，见 {@code LoginAttemptService}）。</p>
 *
 * @author 雨夜终端
 */
@Data
public class CaptchaVO {

    /** 是否需要人机验证；false 时登录请求不必携带 captchaVerification */
    private Boolean enabled;
}
