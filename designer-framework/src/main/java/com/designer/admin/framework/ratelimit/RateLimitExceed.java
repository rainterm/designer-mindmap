package com.designer.admin.framework.ratelimit;

/**
 * 超限后的行为。
 *
 * @author 雨夜终端
 */
public enum RateLimitExceed {

    /**
     * 抛出 {@link RateLimitException}（由全局异常处理转成 {@code code=429} 的 Result 结构）。
     *
     * <p>默认值：适合用户直接触发的接口，调用方能拿到明确原因。</p>
     */
    THROW,

    /**
     * 静默跳过：不执行被标注的方法，直接返回 {@code null}。
     *
     * <p>适合「埋点 / 上报」这类失败也无所谓的旁路动作 —— 抛异常会让访客的页面报错，
     * 而这类动作本来就该被悄悄丢弃。</p>
     *
     * <p>⚠️ 返回 {@code null}，所以只应标注在 {@code void} 方法上；
     * 若标注在有返回值的方法上，调用方必须能容忍 {@code null}。</p>
     */
    SILENT
}
