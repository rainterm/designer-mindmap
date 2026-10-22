package com.designer.admin.framework.captcha;

import com.anji.captcha.controller.CaptchaController;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;

/**
 * 滑块验证码的公开入口：把 AJ-Captcha 的端点收进 `/app-api` 统一前缀。
 *
 * <p><b>为什么不直接改插件的 Mapping</b>：{@code captcha-solon-plugin} 的
 * {@code XPluginImp} 里只有一句 {@code context.beanMake(CaptchaController.class)}，
 * 路径由该类上的 {@code @Mapping("/captcha")} 写死，{@code AjCaptchaProperties}
 * 也没有 prefix 配置项；Solon 的 {@code beanMake(Class)} 同样不支持「注册时加前缀」。
 * 所以这里再注册一个薄 controller，复用同一个 {@link CaptchaService}，
 * 让后端直接认 {@code /app-api/captcha/**}。</p>
 *
 * <p><b>为什么可以复用 {@link CaptchaController#getRemoteId}</b>：插件在
 * {@code get} / {@code check} 里都执行 {@code data.setBrowserInfo(realIp + userAgent)}，
 * 这个值会参与服务端的频控与二次校验。两个入口必须算出**完全相同**的字符串，
 * 否则同一张验证码会在 get 与 check 之间对不上，表现为「随机校验失败」。
 * 直接调插件的静态方法而不是自己拼 {@code ctx.realIp() + ctx.userAgent()}，
 * 就是为了让这条约束只有一个来源。</p>
 *
 * <p><b>为什么插件原来的 /captcha/** 仍然存在</b>：排除插件要连带自己注册
 * {@code AjCaptchaProperties} / {@code AjCaptchaServiceConfiguration} /
 * {@code AjCaptchaStorageConfiguration}，插件一升级就得回来对一遍，风险大于收益。
 * 根路径在 nginx 的两个 server 块里都已显式 404，对公网不可见；
 * 这里只保证「对外只有一套 URL」。</p>
 *
 * @author 雨夜终端
 */
@Controller
@Mapping("/app-api/captcha")
@Api(tags = "滑块验证码")
public class AppCaptchaController {

    @Inject
    private CaptchaService captchaService;

    /**
     * 取一张滑块验证码（底图 + 拼图 + token + secretKey）。
     */
    @Post
    @Mapping("/get")
    @ApiOperation(value = "获取滑块验证码")
    public ResponseModel get(CaptchaVO data, Context ctx) {
        data.setBrowserInfo(CaptchaController.getRemoteId(ctx));
        return captchaService.get(data);
    }

    /**
     * 校验拖拽位置，通过则下发一次性的 captchaVerification 凭据。
     */
    @Post
    @Mapping("/check")
    @ApiOperation(value = "校验滑块位置")
    public ResponseModel check(CaptchaVO data, Context ctx) {
        data.setBrowserInfo(CaptchaController.getRemoteId(ctx));
        return captchaService.check(data);
    }
}
