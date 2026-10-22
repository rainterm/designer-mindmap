package com.designer.admin.framework.mybatis;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表 → 列名与列类型（数据权限的「约定字段」判定依据）。
 *
 * <p><b>为什么查数据库元数据而不是维护一份表清单</b>：项目要求「新表只要按约定建好列就自动生效」。
 * 清单要么写在代码里（新增表必须改代码，等于又回到硬编码），要么写在配置里（多一份要同步的文档，
 * 而且漏配是静默失效）。{@link DatabaseMetaData} 是 JDBC 标准能力，换库不用改代码。</p>
 *
 * <p><b>为什么连列类型一起读</b>：归属列可能是 {@code user_id}（BIGINT，比用户 ID），
 * 也可能是 {@code create_by}（VARCHAR，比用户名）。两者拼进 SQL 的字面量完全不同，
 * 把 {@code 'admin'} 拼成数字或把 {@code 3} 拼成字符串，要么报错要么静默查不到数据。
 * 类型信息只有元数据知道，靠「列名白名单」猜迟早猜错。</p>
 *
 * <p><b>为什么缓存无上限</b>：键是表名，数量级等于库里的表数量（几十到几百），
 * 每个值只是几个列名到类型的映射。真正危险的是「每次查询都开连接查元数据」，
 * 所以这里缓存到底 —— 表结构变更需要重启（或调用 {@link #clear()}）才生效。</p>
 *
 * <p><b>失败即忽略</b>：元数据读取失败时返回空表，拦截器按「该表不参与数据权限」处理。
 * 这是刻意的 fail-open：数据权限是旁路能力，不能因为一次元数据读取失败把整个系统的查询打成 500。
 * 代价是极端情况下有一段时间不过滤，日志里会留下告警。反过来，如果某张表<b>确实</b>没有约定列，
 * 也会缓存空表，不会反复回源。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class TableColumnResolver {

    /** 表名 → (列名 → 类型名)，均为小写 */
    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    private volatile DataSource dataSource;

    /**
     * 查询表的列名与类型。
     *
     * @param table 表名（可带反引号 / 库名前缀）
     * @return 列名（小写）→ 类型名（小写）的映射；表不存在或读取失败返回空映射
     */
    public Map<String, String> columnsOf(String table) {
        String name = normalize(table);
        if (name == null) {
            return Collections.emptyMap();
        }
        return cache.computeIfAbsent(name, this::load);
    }

    /**
     * 该列是否承载字符串（用户名）而不是数字（用户 ID）。
     *
     * <p>按 {@code char} / {@code text} 判断而不是枚举具体类型名：
     * 各库对 VARCHAR 的 {@code TYPE_NAME} 不完全一致（{@code VARCHAR} / {@code VARCHAR2} / {@code TEXT}），
     * 但「含 char 或 text」这一特征在主流数据库上都成立。</p>
     *
     * @param typeName 数据库返回的类型名
     * @return 字符串类型返回 {@code true}
     */
    public static boolean isTextType(String typeName) {
        if (typeName == null) {
            return false;
        }
        String type = typeName.toLowerCase(Locale.ROOT);
        return type.contains("char") || type.contains("text");
    }

    /**
     * 清空缓存（表结构变更后使用；正常的 DDL 走发布流程，重启即可）。
     */
    public void clear() {
        cache.clear();
    }

    private Map<String, String> load(String table) {
        DataSource ds = dataSource();
        if (ds == null) {
            log.warn("[data-permission] 未找到 DataSource，表 {} 不参与数据权限过滤", table);
            return Collections.emptyMap();
        }
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            // 不同库对表名大小写处理不同：先按原样，再按常见的大小写变体试一遍
            for (String candidate : new String[]{table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT)}) {
                Map<String, String> columns = readColumns(meta, conn.getCatalog(), candidate);
                if (!columns.isEmpty()) {
                    return columns;
                }
            }
        } catch (Exception e) {
            log.warn("[data-permission] 读取表 {} 的元数据失败，按不参与数据权限过滤处理", table, e);
        }
        return Collections.emptyMap();
    }

    private Map<String, String> readColumns(DatabaseMetaData meta, String catalog, String table) throws Exception {
        Map<String, String> columns = new LinkedHashMap<>();
        // schemaPattern 传 null：MySQL 用 catalog 定位库，其它库通常不区分 schema
        try (ResultSet rs = meta.getColumns(catalog, null, table, null)) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                if (column != null) {
                    String type = rs.getString("TYPE_NAME");
                    columns.put(column.toLowerCase(Locale.ROOT), type == null ? "" : type.toLowerCase(Locale.ROOT));
                }
            }
        }
        return columns;
    }

    private DataSource dataSource() {
        DataSource local = dataSource;
        if (local == null) {
            try {
                local = Solon.context().getBean(DataSource.class);
                dataSource = local;
            } catch (Exception e) {
                // 容器未就绪（例如启动阶段）；下一次调用会重试
                return null;
            }
        }
        return local;
    }

    /**
     * 去掉库名前缀与引号，统一小写。
     *
     * @param table SQL 中解析出的表名
     * @return 归一化后的表名；为空返回 {@code null}
     */
    static String normalize(String table) {
        if (table == null) {
            return null;
        }
        String name = table.trim();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(dot + 1);
        }
        name = name.replace("`", "").replace("\"", "").trim();
        return name.isEmpty() ? null : name.toLowerCase(Locale.ROOT);
    }
}
