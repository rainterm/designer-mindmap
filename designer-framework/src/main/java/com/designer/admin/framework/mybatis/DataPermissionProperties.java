package com.designer.admin.framework.mybatis;

import lombok.Data;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 数据权限约定配置。
 *
 * <p><b>核心约定</b>：表里只要有「部门列」或「归属列」，就自动参与数据权限过滤，
 * 业务代码不需要写任何条件；两类列都没有的表（字典、日志、菜单、角色……）天然不受影响。</p>
 *
 * <p>配置项（{@code app.yml}，全部可省略）：</p>
 * <pre>
 * designer:
 *   data-permission:
 *     enabled: true
 *     dept-column: dept_id          # 部门列，命中的表按「可见部门 IN」过滤
 *     owner-column: create_by       # 归属列（创建人），命中的表额外放行「本人创建的」
 *     owner-column-overrides:       # 单表覆盖：该表的这一列代表「这条数据属于谁」
 *       sys_user: id
 *     dept-key-tables:              # 本表的主键就是部门 ID（部门树自身）
 *       - sys_dept
 *     ignore-tables:                # 明确排除的表（全局共享 / 关联授权表）
 *       - sys_role_dept
 * </pre>
 *
 * <p><b>为什么用列名约定而不是注解</b>：注解要挂在每个 Mapper 方法上，新增表 / 新增查询必须记得加，
 * 漏一处就是一处越权；列名约定是数据库结构的一部分，建表时就决定了，后续所有查询自动继承。</p>
 *
 * <p><b>为什么默认归属列是 {@code create_by}</b>：「仅本人」的业务语义就是「我创建的数据」，
 * 而 {@code create_by} 由 {@code AutoFillHandler} 在插入时统一填当前登录账号，不需要业务表额外加字段。
 * 注意它存的是<b>用户名（字符串）</b>，与 {@code user_id}（数字）拼进 SQL 的字面量不同，
 * 拦截器按列的真实类型自动选择，配置里不用区分。</p>
 *
 * <p><b>为什么归属列的生效条件比较讲究</b>：{@code create_by} 几乎每张表都有（审计字段）。
 * 如果无条件按创建人裁剪，角色下拉、菜单树、字典项都会被裁成「我创建的」——
 * 字典还有 {@code @Cache}，空结果会按类型缓存给所有人。所以：</p>
 * <ul>
 *   <li>带部门列的表 → 部门维度 + 创建人维度一起生效（{@code dept_id IN (...) OR create_by = ?}）；</li>
 *   <li>不带部门列的表 → 只在「仅本人」档按创建人裁剪，且跳过 {@link #ownerExcludedPrefixes}
 *       声明的框架表（默认 {@code sys_}）；</li>
 *   <li>需要「归属不是创建人」的表用 {@link #ownerColumnOverrides} 覆盖
 *       （例如 {@code sys_user} 的归属就是它自己的主键）。</li>
 * </ul>
 *
 * @author 雨夜终端
 */
@Data
@Component
public class DataPermissionProperties {

    /** 配置前缀 */
    private static final String PREFIX = "designer.data-permission.";

    /** 总开关：默认开启（数据权限是安全能力，不能因为漏配就默认关闭） */
    private boolean enabled = true;

    /** 部门列名 */
    private String deptColumn = "dept_id";

    /** 归属列名（默认创建人；存用户名，按字符串比较） */
    private String ownerColumn = "create_by";

    /** 单表归属列覆盖：{@code 表名 -> 列名} */
    private Map<String, String> ownerColumnOverrides = Map.of();

    /**
     * 「归属列」维度不参与的表名前缀。
     *
     * <p>框架自带的 {@code sys_*} 表（字典、菜单、角色、配置、存储…）是被所有角色共享的基础数据，
     * 按创建人裁剪会让下拉框、菜单树直接变空；而字典查询还带 {@code @Cache}，
     * 空结果会按「字典类型」缓存给所有用户，把一个人的可见范围放大成全局效果。</p>
     *
     * <p>业务表约定不用 {@code sys_} 前缀，所以默认值就能覆盖现有所有情况。</p>
     */
    private Set<String> ownerExcludedPrefixes = new LinkedHashSet<>(Set.of("sys_"));

    /** 主键即部门 ID 的表 */
    private Set<String> deptKeyTables = new LinkedHashSet<>();

    /** 不参与过滤的表 */
    private Set<String> ignoreTables = new LinkedHashSet<>();

    /**
     * 用户表名：用于「按创建人反查部门」。
     *
     * <p>业务表只存了 {@code create_by}（用户名）时，部门维度无法直接判断。
     * 拦截器会把它翻译成 {@code create_by IN (SELECT username FROM sys_user WHERE dept_id IN (...))}，
     * 从而<b>不需要给每张业务表加 {@code dept_id}</b>。</p>
     *
     * <p>表名放在配置里而不是写死在 framework：框架层不该知道自己跑在哪个业务库上。</p>
     */
    private String userTable = "sys_user";

    /** 用户表中的账号列名（与 {@code create_by} 存的值一致） */
    private String usernameColumn = "username";

    @Init
    public void init() {
        // 表名 / 列名在 SQL 解析结果与数据库元数据里大小写不一致，统一按小写比较
        enabled = Solon.cfg().getBool(PREFIX + "enabled", true);
        deptColumn = normalize(Solon.cfg().get(PREFIX + "dept-column", "dept_id"));
        ownerColumn = normalize(Solon.cfg().get(PREFIX + "owner-column", "create_by"));

        Map<String, String> overrides = new LinkedHashMap<>();
        Solon.cfg().getMap(PREFIX + "owner-column-overrides")
                .forEach((table, column) -> overrides.put(normalize(table), normalize(column)));
        ownerColumnOverrides = overrides;

        deptKeyTables = readSet(PREFIX + "dept-key-tables");
        ignoreTables = readSet(PREFIX + "ignore-tables");

        String userTableValue = Solon.cfg().get(PREFIX + "user-table", "sys_user");
        userTable = userTableValue == null || userTableValue.isBlank() ? "sys_user" : normalize(userTableValue);
        String usernameValue = Solon.cfg().get(PREFIX + "username-column", "username");
        usernameColumn = usernameValue == null || usernameValue.isBlank() ? "username" : normalize(usernameValue);

        Set<String> prefixes = readSet(PREFIX + "owner-excluded-prefixes");
        // 不配置时保持 sys_ 这个默认：把「漏配」的方向指向「不裁剪」而不是「裁掉基础数据」
        ownerExcludedPrefixes = prefixes.isEmpty() ? new LinkedHashSet<>(Set.of("sys_")) : prefixes;
    }

    /**
     * 该表使用的归属列。
     *
     * @param table 表名（小写）
     * @return 归属列名；没有配置也没有 {@link #ownerColumn} 时返回 {@code null}
     */
    public String ownerColumnOf(String table) {
        String override = ownerColumnOverrides.get(table);
        return override != null ? override : ownerColumn;
    }

    /**
     * 该表是否用主键代表部门 ID。
     *
     * @param table 表名（小写）
     * @return 是返回 {@code true}
     */
    public boolean isDeptKeyTable(String table) {
        return deptKeyTables.contains(table);
    }

    /**
     * 该表是否被排除。
     *
     * @param table 表名（小写）
     * @return 排除返回 {@code true}
     */
    public boolean isIgnored(String table) {
        return ignoreTables.contains(table);
    }

    /**
     * 「归属列」维度是否跳过该表（框架自带的共享基础数据）。
     *
     * <p>只影响<b>没有部门列</b>的表：{@code sys_user} / {@code sys_dept} 带部门维度，
     * 按部门裁剪是正确行为，不受此前缀影响。</p>
     *
     * @param table 表名（小写）
     * @return 跳过返回 {@code true}
     */
    public boolean isOwnerExcluded(String table) {
        if (table == null) {
            return false;
        }
        for (String prefix : ownerExcludedPrefixes) {
            if (prefix != null && !prefix.isEmpty() && table.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> readSet(String key) {
        Set<String> result = new LinkedHashSet<>();
        for (String item : Solon.cfg().getList(key)) {
            if (item != null && !item.isBlank()) {
                result.add(normalize(item));
            }
        }
        return result;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 1 && (trimmed.startsWith("`") || trimmed.startsWith("\""))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
