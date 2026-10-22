package com.designer.admin.framework.client;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.util.HostUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 只信任可信反向代理的 {@link HostUtil}。
 *
 * <p><b>要解决的问题</b>：Solon 默认的 {@code getRealIp} 无条件采信
 * {@code X-Real-IP} / {@code X-Forwarded-For}。这两个头客户端可以随意伪造，
 * 于是「按 IP 限流 / 按 IP 封禁」全部失效——攻击者每次请求换一个头值，
 * 就能拿到一个全新的限流桶。</p>
 *
 * <p><b>策略</b>：</p>
 * <ol>
 *   <li>对端 socket IP（{@code ctx.remoteIp()}）不在可信代理网段内 → <b>直接返回 socket IP</b>，
 *       完全忽略请求头。这是防伪造的关键一步：不可伪造的 socket IP 优先。</li>
 *   <li>对端可信 → 沿 {@code X-Forwarded-For} <b>从右往左</b>找第一个不可信地址，
 *       它就是真实客户端（右边的都是我们自己的代理，逐个跳过）。</li>
 *   <li>没有 XFF 时退回 {@code X-Real-IP}（可信代理会覆写成 {@code $remote_addr}）。</li>
 * </ol>
 *
 * <p>⚠️ 可信网段不要配得过宽：如果真实客户端本身来自某个可信网段（例如办公网 10.x），
 * 该网段内的地址就能互相伪装。按需收窄到只包含自己的代理。</p>
 *
 * @author 雨夜终端
 */
@Slf4j
public class TrustedProxyHostUtil extends HostUtil {

    private final List<IpRange> trustedProxies;

    public TrustedProxyHostUtil(List<String> cidrs) {
        List<IpRange> ranges = new ArrayList<>();
        for (String cidr : cidrs) {
            IpRange range = IpRange.parse(cidr);
            if (range != null) {
                ranges.add(range);
            } else if (cidr != null && !cidr.isBlank()) {
                log.warn("可信代理网段配置无法解析，已忽略：{}", cidr);
            }
        }
        this.trustedProxies = List.copyOf(ranges);
        if (this.trustedProxies.isEmpty()) {
            // 没有任何可信代理时，一律使用 socket IP —— 安全但会让真实客户端 IP 变成本机代理地址，
            // 所以打日志提醒，避免部署后限流按代理 IP 统计却查不出原因。
            log.warn("未配置任何可信代理网段，所有请求都将以 socket IP 为准（不采信任何转发头）");
        }
    }

    @Override
    public String getRealIp(Context ctx) {
        return resolveRealIp(ctx);
    }

    /**
     * 解析真实客户端 IP，供业务直接复用。
     *
     * <p>返回值保证是合法 IP 字面量：请求头里出现的非法值（垃圾字符串、被污染的段）
     * 会被跳过，全部不可用时回退到 socket IP。</p>
     *
     * @return 真实客户端 IP；无法判定时返回 socket IP
     */
    public String resolveRealIp(Context ctx) {
        if (ctx == null) {
            return "";
        }
        String socketIp = ctx.remoteIp();
        if (!isTrusted(socketIp)) {
            // 对端不是我们的代理：头一律不可信（这是防伪造的关键一步）
            return socketIp == null ? "" : socketIp.trim();
        }

        String xff = ctx.header("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            String leftmostValid = null;
            // 从右往左：右侧是我们自己的代理，第一个「合法且不可信」的地址才是真实客户端
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!IpRange.isIpLiteral(candidate)) {
                    // 非法值直接跳过：既不采信，也不让它污染限流键
                    continue;
                }
                leftmostValid = candidate;
                if (!isTrusted(candidate)) {
                    return candidate;
                }
            }
            if (leftmostValid != null) {
                // 整条链都是可信代理：退回最左端的合法值
                return leftmostValid;
            }
        }

        String realIp = ctx.header("X-Real-IP");
        if (realIp != null) {
            String candidate = realIp.trim();
            if (!candidate.isEmpty()
                    && !"unknown".equalsIgnoreCase(candidate)
                    && IpRange.isIpLiteral(candidate)) {
                return candidate;
            }
        }

        return socketIp == null ? "" : socketIp.trim();
    }

    /** 对端是否为可信代理 */
    public boolean isTrusted(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String candidate = ip.trim();
        for (IpRange range : trustedProxies) {
            if (range.matches(candidate)) {
                return true;
            }
        }
        return false;
    }
}
