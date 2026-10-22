package com.designer.admin.framework.client;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.core.util.HostUtil;

import java.util.Arrays;
import java.util.List;

/**
 * 真实客户端 IP 解析配置：把 Solon 默认的 {@link HostUtil} 换成本项目的可信代理版本。
 *
 * <p>Solon 默认无条件采信 {@code X-Real-IP} / {@code X-Forwarded-For}，
 * 导致 IP 可被客户端伪造、按 IP 的限流形同虚设。这里在容器启动时全局替换，
 * 让「不可伪造的 socket IP」优先于请求头（详见 {@link TrustedProxyHostUtil}）。</p>
 *
 * <p>配置项 {@code designer.client-ip.trusted-proxies}（逗号分隔的 IP / CIDR）：
 * 只有来自这些地址的请求才允许携带转发头。默认包含回环地址与 Docker 默认网桥网段。
 * <b>反向代理不在这些网段内时必须补上</b>，否则真实客户端 IP 会变成代理自身地址。</p>
 *
 * @author 雨夜终端
 */
@Configuration
@Slf4j
public class ClientIpConfig {

    /** 默认可信：回环（nginx 同机）+ Docker 默认网桥网段（nginx 与 server 同 compose 网络） */
    private static final String DEFAULT_TRUSTED_PROXIES = "127.0.0.1/32, ::1/128, 172.16.0.0/12";

    @Bean
    public TrustedProxyHostUtil trustedProxyHostUtil() {
        String raw = Solon.cfg().get("designer.client-ip.trusted-proxies", DEFAULT_TRUSTED_PROXIES);
        List<String> cidrs = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        TrustedProxyHostUtil hostUtil = new TrustedProxyHostUtil(cidrs);
        // 全局替换：必须在开始接收请求之前完成，否则早期请求仍按默认逻辑解析
        HostUtil.globalSet(hostUtil);
        log.info("可信代理网段已加载：{}", cidrs);
        return hostUtil;
    }

    /**
     * IP 归属地解析器（离线 ip2region）。
     *
     * <p>数据文件默认使用随应用打包的类路径 {@code ip2region.xdb}；
     * 配置 {@code designer.ip-location.xdb-path} 可指向外部文件以便单独升级数据。
     * 数据文件缺失时不会阻止启动，归属地会统一退化为「未知」。</p>
     */
    @Bean
    public IpRegionResolver ipRegionResolver() {
        String xdbPath = Solon.cfg().get("designer.ip-location.xdb-path", "");
        return new IpRegionResolver(xdbPath);
    }
}
