package com.designer.admin.framework.ratelimit;

import org.noear.solon.annotation.Around;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式限流：标注在 Controller / Service 方法（或类）上，由 {@link RateLimitInterceptor} 环绕校验。
 *
 * <p>底层是 {@code RedisCounter} 的固定窗口计数（跨实例一致，不依赖单机内存），
 * 因此多实例部署时阈值不会被放大成「实例数 × 阈值」。</p>
 *
 * <p><b>典型用法</b>：</p>
 * <pre>{@code
 * // 匿名接口按 IP：每分钟 20 次
 * @RateLimit(name = "comment-captcha", rules = @RateLimit.Rule(count = 20, window = 60))
 *
 * // 一条规则挡不住全部形态：间隔 + 日配额，两条规则共用一个 name
 * @RateLimit(name = "comment-submit", rules = {
 *         @RateLimit.Rule(count = 1, window = 30, message = "操作太频繁了，请 {} 秒后再试"),
 *         @RateLimit.Rule(count = 20, window = 24 * 60 * 60, message = "今日操作次数已达上限")
 * })
 * }</pre>
 *
 * <p><b>为什么用 {@code rules} 数组而不是把注解做成可重复的</b>：Solon 的
 * {@code MethodWrap} 逐个读方法上的注解、再用 {@code annotationType().getAnnotation(Around.class)}
 * 找拦截器；对可重复注解，方法上留下的是外层容器注解（{@code RateLimit$List}），
 * 拦截器就挂不上了（详见 {@code MethodWrap} 的注解扫描逻辑）。</p>
 *
 * <p><b>规则顺序即校验顺序</b>，前一条被拒就不会消耗后面几条的额度。
 * 因此把「更严 / 更廉价」的规则写在前面（例如先写间隔，再写日配额），
 * 被间隔挡下的突发请求就不会白吃掉日配额。</p>
 *
 * <p><b>拦截顺位</b>：{@code index = -1} 让它成为**拦截器链里最外层**的一个 ——
 * Solon 自带的拦截器顺位都是非负数（{@code @Valid} 为 1、{@code @Cache} 为 111、
 * {@code @Transaction} 为 120），所以被限的请求不会白开事务、白查缓存。</p>
 *
 * <p>但它**挡不住 Filter**：{@code @Valid} 的参数校验实际是由 Filter
 * （{@code ContextValidateHandler}）完成的，而 Filter 在路由和拦截器之前执行，
 * 因此写了校验约束的参数会先被校验拦下、不消耗额度。反过来说，
 * **没写约束的字段等于没校验**，不能指望它替我们挡脏数据。</p>
 *
 * <p><b>⚠️ 只在跨 bean 调用时生效</b>：拦截由 Solon 的 {@code MethodWrap} 代理完成，
 * 同一个 bean 内部的 {@code this.xxx()} 自调用会绕过代理。因此**标注在 Controller 方法上**
 * 是最省心的用法 —— 方法体里调用的 service 一定在代理之外。
 * 反过来把它挂在 service 的私有/自调方法上是无效的（Solon 不报错，只是静默不生效）。</p>
 *
 * @author 雨夜终端
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Around(value = RateLimitInterceptor.class, index = -1)
public @interface RateLimit {

    /**
     * 规则名：限流键的命名空间前缀。
     *
     * <p>同一 {@code name} + 同一维度主体 + 同一规则才会共用一个计数窗口。
     * 要求显式指定而不是自动取方法名：方法重命名不该悄悄换掉限流桶，
     * 而且同一个 name 可以刻意为多个入口共用一份额度。</p>
     */
    String name();

    /** 限流维度：决定「按谁计数」，同一个注解下所有规则共用一个维度 */
    RateLimitDimension dimension() default RateLimitDimension.IP;

    /** 超限行为，默认抛 {@link RateLimitException} */
    RateLimitExceed exceed() default RateLimitExceed.THROW;

    /** {@link RateLimitDimension#HEADER} 维度取值的请求头名 */
    String headerName() default "";

    /** 规则列表：按声明顺序校验 */
    Rule[] rules();

    /**
     * 单条规则：{@code window} 秒内最多 {@code count} 次。
     *
     * <p>窗口是**固定窗口**：锚在窗口内第一次请求，之后不会因为持续请求而延长
     * （否则阈值形同虚设）。这也意味着计数键一定会在 {@code window} 秒后消失，
     * 不存在「超限后永久拉黑」的可能。</p>
     */
    @Documented
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Rule {

        /** 窗口内允许的次数（含第 count 次，第 count + 1 次才被拒） */
        int count();

        /** 窗口长度（秒）；可用 {@code 24 * 60 * 60} 这类常量表达式 */
        int window();

        /**
         * 超限提示语。
         *
         * <p>包含 {@code {}} 时会被替换成**剩余秒数**（例如「请 30 秒后再试」）；
         * 不写则用默认文案。长窗口（如 24 小时）不建议用 {@code {}}，
         * 否则会提示出「请 86399 秒后再试」这种数字。</p>
         */
        String message() default "";
    }
}
