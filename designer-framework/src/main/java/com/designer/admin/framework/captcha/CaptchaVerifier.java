package com.designer.admin.framework.captcha;

import com.anji.captcha.model.common.CaptchaTypeEnum;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.designer.admin.common.exception.BizException;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 人机验证的**二次校验**，各业务入口共用的收口点。
 *
 * <p><b>为什么业务侧还要再校验一次</b>：AJ-Captcha 是两段式设计。</p>
 * <ol>
 *   <li>前端拖完滑块调 {@code POST /app-api/captcha/check}，服务端比对偏移量、
 *       判定「是不是人」。
 *       通过后它会往缓存里写一条以本次 token 为键的「二次校验凭据」，并把
 *       {@code captchaVerification}（= {@code AES(token + "---" + {x,y})}）交给前端；</li>
 *   <li>业务请求带上这个 {@code captchaVerification}，服务端调
 *       {@link CaptchaService#verification} 解密、核对第 1 步留下的那条凭据，**然后删除**。</li>
 * </ol>
 * <p>第 2 步不是为了重复判人机，而是三件事：</p>
 * <ul>
 *   <li><b>不可重放</b>：凭据用掉即失效，同一张验证码无法放行两次提交；</li>
 *   <li><b>不可伪造</b>：{@code captchaVerification} 是 AES 加密的，且只对服务端下发过的
 *       token 有效，脚本没法自己编一个；</li>
 *   <li><b>绑定到业务动作</b>：只有真正走完人机判定的请求才拿得到凭据，
 *       所以「拿到凭据后不提交」不会白白消费掉业务侧的配额。</li>
 * </ul>
 *
 * <p><b>调用时机</b>：放在「业务上是否真的需要校验」判断之后。
 * 凭据是一次性的，站点关停 / 无需验证码的请求先拒掉，免得白白消费掉一张
 * （消费了用户就得重新拖一次滑块）。</p>
 *
 * <p><b>与 {@code com.designer.admin.system.service.auth.LoginAttemptService} 的分工</b>：
 * 这个类只管「凭据是否有效」，不关心谁在调、要不要调；
 * 「什么时候该要求验证码」（如登录失败达阈值）属于业务策略，留在各自模块里。</p>
 *
 * @author 雨夜终端
 */
@Component
public class CaptchaVerifier {

    @Inject
    private CaptchaService captchaService;

    /**
     * 校验并通过则返回；失败抛业务异常。
     *
     * @param captchaVerification 前端从 {@code POST /app-api/captcha/check} 拿到的凭据
     */
    public void assertVerified(String captchaVerification) {
        if (captchaVerification == null || captchaVerification.isBlank()) {
            throw new BizException("请先完成滑块验证");
        }

        CaptchaVO vo = new CaptchaVO();
        // 当前容器里的 CaptchaService 已是 BlockPuzzle 实现，这里的类型只是显式声明意图
        vo.setCaptchaType(CaptchaTypeEnum.BLOCKPUZZLE.getCodeValue());
        vo.setCaptchaVerification(captchaVerification);

        ResponseModel result = captchaService.verification(vo);
        if (result == null || !result.isSuccess()) {
            // 失败原因不细分（过期 / 已用过 / 解密失败 / 偏移不符），
            // 对用户而言处置都一样：重新拖一次滑块。
            throw new BizException("验证码校验失败，请重新拖动滑块");
        }
    }
}
