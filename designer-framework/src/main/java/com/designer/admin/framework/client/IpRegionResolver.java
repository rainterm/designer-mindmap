package com.designer.admin.framework.client;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * IP 归属地离线解析（基于 ip2region 的 xdb 数据文件）。
 *
 * <p>特点：纯本地文件查询，<b>不发起任何网络调用</b>——既不依赖第三方接口可用性，
 * 也不会把访客 IP 泄露给外部服务，且没有调用额度限制。</p>
 *
 * <p>数据文件加载顺序：外部路径（配置项）优先 → 类路径 {@code ip2region.xdb}（随应用打包）。
 * 数据文件缺失或损坏时不会导致启动失败，退化为统一返回「未知」。</p>
 *
 * <p>线程安全：使用「全量载入内存」的 {@link Searcher}，其内部只读 buffer，可被多线程并发查询。</p>
 *
 * @author 雨夜终端
 */
@Slf4j
public class IpRegionResolver {

    /** 类路径下的默认数据文件位置 */
    public static final String CLASSPATH_LOCATION = "ip2region.xdb";

    /** 无法判定时的兜底值 */
    public static final String UNKNOWN = "未知";
    /** 内网 / 保留地址 */
    public static final String INTRANET = "内网";

    /** 数据文件对内网地址返回的标记 */
    private static final String INTRANET_LABEL = "内网IP";

    /** 字段占位符：ip2region 用「0」表示空字段 */
    private static final String EMPTY_FIELD = "0";

    /** 省级名称最短可归一长度（如「广东省」） */
    private static final int MIN_PROVINCE_LENGTH = 2;

    /** 省 / 直辖市的行政区后缀 */
    private static final String PROVINCE_SUFFIX = "省";
    private static final String CITY_SUFFIX = "市";

    /**
     * 内网与保留网段：无需查库，直接判定（也避免依赖数据文件的内部表现）。
     *
     * <p>依次为：私有网段、回环、链路本地、运营商级 NAT（CGNAT）、保留、组播，
     * 以及 IPv6 回环 / 唯一本地地址 / 链路本地地址。</p>
     */
    private static final List<IpRange> PRIVATE_RANGES = List.of(
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "127.0.0.0/8",
            "169.254.0.0/16",
            "100.64.0.0/10",
            "0.0.0.0/8",
            "224.0.0.0/4",
            "240.0.0.0/4",
            "::1/128",
            "fc00::/7",
            "fe80::/10"
    ).stream().map(IpRange::parse).filter(java.util.Objects::nonNull).toList();

    /** 省级行政区的后缀，用于把「广东省」归一为「广东」 */
    private static final Map<String, String> PROVINCE_ALIASES = Map.ofEntries(
            Map.entry("内蒙古自治区", "内蒙古"),
            Map.entry("广西壮族自治区", "广西"),
            Map.entry("西藏自治区", "西藏"),
            Map.entry("宁夏回族自治区", "宁夏"),
            Map.entry("新疆维吾尔自治区", "新疆"),
            Map.entry("香港特别行政区", "香港"),
            Map.entry("澳门特别行政区", "澳门"),
            Map.entry("台湾省", "台湾")
    );

    private final Searcher searcher;

    /**
     * @param externalPath xdb 外部文件路径；为空或不存在时回退到类路径
     */
    public IpRegionResolver(String externalPath) {
        this.searcher = load(externalPath);
        if (searcher == null) {
            log.warn("IP 归属地数据文件不可用，归属地将统一记录为「{}」。"
                    + "请确认类路径下存在 {}，或正确配置外部数据文件路径", UNKNOWN, CLASSPATH_LOCATION);
        }
    }

    /** 数据文件是否加载成功 */
    public boolean available() {
        return searcher != null;
    }

    /**
     * 解析 IP 归属地（省级）。
     *
     * @param ip 客户端 IP，可为 IPv4 或 IPv6
     * @return 省份（如「广东」「北京」）；内网地址返回「内网」；无法判定返回「未知」。永不返回 {@code null}
     */
    public String lookup(String ip) {
        if (ip == null || ip.isBlank()) {
            return UNKNOWN;
        }
        String candidate = ip.trim();
        // 非合法 IP 字面量（含被伪造的垃圾值）直接判定未知，且不触发 DNS
        if (!IpRange.isIpLiteral(candidate)) {
            return UNKNOWN;
        }
        if (isPrivate(candidate)) {
            return INTRANET;
        }
        if (searcher == null) {
            return UNKNOWN;
        }
        try {
            // 返回格式：国家|区域|省份|城市|ISP，例如「中国|0|广东省|深圳市|电信」
            String region = searcher.search(candidate);
            return toProvince(region);
        } catch (Exception e) {
            // ip2region 的 xdb 为 IPv4 数据集，IPv6 等无法解析属正常情况
            if (log.isDebugEnabled()) {
                log.debug("解析 IP 归属地失败：{}", candidate, e);
            }
            return UNKNOWN;
        }
    }

    /** 是否为内网 / 保留地址 */
    public boolean isPrivate(String ip) {
        for (IpRange range : PRIVATE_RANGES) {
            if (range.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 ip2region 的 region 字符串中取省级名称。
     *
     * <p>「0」是 ip2region 表示「未知」的占位符。省份未知时退回国家
     * （例如 {@code 美国|0|0|0|Level3} → 「美国」），国家也未知时返回「未知」。</p>
     */
    private static String toProvince(String region) {
        if (region == null || region.isBlank()) {
            return UNKNOWN;
        }
        String[] fields = region.split("\\|", -1);
        String province = fieldAt(fields, 2);
        if (province != null) {
            String normalized = normalizeProvince(province);
            // 数据文件对内网地址也返回「内网IP」标记
            if (INTRANET.equals(normalized) || INTRANET_LABEL.equals(province)) {
                return INTRANET;
            }
            return normalized;
        }
        String country = fieldAt(fields, 0);
        if (country != null) {
            return country;
        }
        return UNKNOWN;
    }

    /** 取出有效字段；空串或占位符「0」视为无效 */
    private static String fieldAt(String[] fields, int index) {
        if (index >= fields.length) {
            return null;
        }
        String value = fields[index].trim();
        if (value.isEmpty() || EMPTY_FIELD.equals(value)) {
            return null;
        }
        return value;
    }

    /** 归一化省级名称：剥离行政区后缀，让「广东省」「北京市」变成「广东」「北京」 */
    private static String normalizeProvince(String province) {
        String alias = PROVINCE_ALIASES.get(province);
        if (alias != null) {
            return alias;
        }
        // 直辖市 / 普通省份：去掉结尾的「省」或「市」
        if (province.length() > MIN_PROVINCE_LENGTH) {
            if (province.endsWith(PROVINCE_SUFFIX) || province.endsWith(CITY_SUFFIX)) {
                return province.substring(0, province.length() - 1);
            }
        }
        return province;
    }

    /** 加载数据文件：外部路径优先，其次类路径 */
    private static Searcher load(String externalPath) {
        byte[] buffer = loadExternal(externalPath);
        if (buffer == null) {
            buffer = loadFromClasspath();
        }
        if (buffer == null) {
            return null;
        }
        try {
            return Searcher.newWithBuffer(buffer);
        } catch (Exception e) {
            log.error("IP 归属地数据文件格式非法，归属地功能不可用", e);
            return null;
        }
    }

    private static byte[] loadExternal(String externalPath) {
        if (externalPath == null || externalPath.isBlank()) {
            return null;
        }
        Path path = Path.of(externalPath.trim());
        if (!Files.isReadable(path)) {
            log.warn("配置的 IP 归属地数据文件不可读，将回退到类路径：{}", path);
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            log.info("已加载外部 IP 归属地数据文件：{}（{} 字节）", path, bytes.length);
            return bytes;
        } catch (Exception e) {
            log.warn("读取外部 IP 归属地数据文件失败，将回退到类路径：{}", path, e);
            return null;
        }
    }

    private static byte[] loadFromClasspath() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = IpRegionResolver.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(CLASSPATH_LOCATION)) {
            if (in == null) {
                return null;
            }
            byte[] bytes = in.readAllBytes();
            log.info("已加载类路径 IP 归属地数据文件：{}（{} 字节）", CLASSPATH_LOCATION, bytes.length);
            return bytes;
        } catch (Exception e) {
            log.warn("读取类路径 IP 归属地数据文件失败：{}", CLASSPATH_LOCATION, e);
            return null;
        }
    }
}
