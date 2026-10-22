package com.designer.admin.framework.client;

/**
 * IPv4 / IPv6 网段（CIDR）匹配。
 *
 * <p>用于判断请求来源是否属于「可信反向代理」，据此决定要不要采信
 * {@code X-Forwarded-For} / {@code X-Real-IP} 这类可被客户端伪造的请求头。</p>
 *
 * <p>⚠️ 本类<b>只解析字面量 IP，绝不触发 DNS</b>：这些字符串可能来自攻击者可控的请求头，
 * 用 {@link java.net.InetAddress#getByName} 直接解析会让对方通过构造主机名引发
 * DNS 查询（阻塞线程 + 内网探测）。IPv4 手工解析；IPv6 必然含冒号，
 * 而主机名不允许含冒号，因此含冒号时用 getByName 也不会走 DNS。</p>
 *
 * @author 雨夜终端
 */
public final class IpRange {

    /** IPv6 字面量允许的字符（冒号 + 十六进制 + IPv4-mapped 里的点） */
    private static final String IPV6_CHARS = "[0-9a-fA-F:.]+";

    /** IPv6 方括号字面量的左括号 */
    private static final char BRACKET_START = '[';

    /** IPv6 方括号字面量的右括号 */
    private static final char BRACKET_END = ']';

    /** IPv6 段分隔符 */
    private static final char IPV6_SEPARATOR = ':';

    /** 点分十进制分隔符 */
    private static final String IPV4_SEPARATOR = "\\.";

    /** IPv4 地址字节数 */
    private static final int IPV4_BYTE_LENGTH = 4;

    /** IPv6 地址字节数 */
    private static final int IPV6_BYTE_LENGTH = 16;

    /** IPv4-mapped IPv6 中前缀零字节的数量 */
    private static final int IPV4_MAPPED_ZERO_PREFIX = 10;

    /** IPv4-mapped IPv6 中 IPv4 部分的起始偏移 */
    private static final int IPV4_MAPPED_OFFSET = 12;

    /** IPv4 段最大位数 */
    private static final int IPV4_SEGMENT_MAX_DIGITS = 3;

    /** IPv4 段最大值 */
    private static final int IPV4_SEGMENT_MAX_VALUE = 255;

    private final byte[] network;
    private final int prefixLength;

    private IpRange(byte[] network, int prefixLength) {
        this.network = network;
        this.prefixLength = prefixLength;
    }

    /**
     * 解析 {@code 10.0.0.0/8}、{@code 127.0.0.1}、{@code ::1} 这类写法。
     * 不带前缀长度时按单机地址处理（IPv4 为 /32，IPv6 为 /128）。
     *
     * @return 解析失败返回 {@code null}（由调用方决定是忽略还是报错）
     */
    public static IpRange parse(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.isEmpty()) {
            return null;
        }
        String host = s;
        int prefix = -1;
        int slash = s.indexOf('/');
        if (slash >= 0) {
            host = s.substring(0, slash).trim();
            String prefixText = s.substring(slash + 1).trim();
            if (prefixText.isEmpty() || !prefixText.chars().allMatch(Character::isDigit)) {
                return null;
            }
            try {
                prefix = Integer.parseInt(prefixText);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        byte[] bytes = parseIpLiteral(host);
        if (bytes == null) {
            return null;
        }
        int maxPrefix = bytes.length * 8;
        if (prefix < 0) {
            prefix = maxPrefix;
        }
        if (prefix > maxPrefix) {
            return null;
        }
        return new IpRange(bytes, prefix);
    }

    /** 是否为合法的 IP 字面量（不会触发 DNS） */
    public static boolean isIpLiteral(String text) {
        return parseIpLiteral(text) != null;
    }

    /**
     * 判断 IP 是否落在本网段内。
     *
     * <p>IPv4 与 IPv6 地址长度不同，无法直接比较，一律视为不匹配；
     * IPv4-mapped IPv6（{@code ::ffff:a.b.c.d}）会先还原成 IPv4 再比较。</p>
     */
    public boolean matches(String ip) {
        byte[] bytes = parseIpLiteral(ip);
        if (bytes == null || bytes.length != network.length) {
            return false;
        }
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (bytes[i] != network[i]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (bytes[fullBytes] & mask) == (network[fullBytes] & mask);
    }

    /** 解析 IP 字面量为字节数组；非法或含主机名返回 {@code null}（不触发 DNS） */
    private static byte[] parseIpLiteral(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.isEmpty()) {
            return null;
        }
        // remoteIp 可能是带方括号的 IPv6： [::1]
        if (s.charAt(0) == BRACKET_START && s.charAt(s.length() - 1) == BRACKET_END) {
            s = s.substring(1, s.length() - 1);
        }
        if (s.indexOf(IPV6_SEPARATOR) >= 0) {
            return parseIpv6(s);
        }
        return parseIpv4(s);
    }

    /** IPv6：含冒号即不可能是主机名，故 getByName 不会发起 DNS 查询 */
    private static byte[] parseIpv6(String s) {
        if (!s.matches(IPV6_CHARS) || s.indexOf(IPV6_SEPARATOR) < 0) {
            return null;
        }
        try {
            byte[] bytes = java.net.InetAddress.getByName(s).getAddress();
            // Java 会把 ::ffff:1.2.3.4 这类映射地址直接归一成 Inet4Address（4 字节）
            if (bytes.length == IPV4_BYTE_LENGTH) {
                return bytes;
            }
            if (bytes.length != IPV6_BYTE_LENGTH) {
                return null;
            }
            // 兜底：仍以 16 字节形式出现的映射地址，还原为 IPv4 后再比较
            return isIpv4Mapped(bytes)
                    ? java.util.Arrays.copyOfRange(bytes, IPV4_MAPPED_OFFSET, IPV6_BYTE_LENGTH)
                    : bytes;
        } catch (java.net.UnknownHostException e) {
            return null;
        }
    }

    /** IPv4：手工解析，完全不做名称解析 */
    private static byte[] parseIpv4(String s) {
        String[] parts = s.split(IPV4_SEPARATOR, -1);
        if (parts.length != IPV4_BYTE_LENGTH) {
            return null;
        }
        byte[] out = new byte[IPV4_BYTE_LENGTH];
        for (int i = 0; i < IPV4_BYTE_LENGTH; i++) {
            String part = parts[i];
            // 拒绝空段、前导零以外的非数字、超 3 位
            if (part.isEmpty() || part.length() > IPV4_SEGMENT_MAX_DIGITS) {
                return null;
            }
            for (int j = 0; j < part.length(); j++) {
                char c = part.charAt(j);
                if (c < '0' || c > '9') {
                    return null;
                }
            }
            int value = Integer.parseInt(part);
            if (value > IPV4_SEGMENT_MAX_VALUE) {
                return null;
            }
            out[i] = (byte) value;
        }
        return out;
    }

    /** 判断是否为 IPv4-mapped IPv6：前 10 字节为 0，第 11、12 字节为 0xFF */
    private static boolean isIpv4Mapped(byte[] b) {
        for (int i = 0; i < IPV4_MAPPED_ZERO_PREFIX; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF;
    }

    @Override
    public String toString() {
        return "IpRange{prefix=" + prefixLength + "}";
    }
}
