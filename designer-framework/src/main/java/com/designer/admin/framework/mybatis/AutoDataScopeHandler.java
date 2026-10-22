package com.designer.admin.framework.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.framework.satoken.PermissionLoader;
import com.designer.admin.framework.satoken.PermissionSnapshot;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据权限 SQL 自动改写：按「表里的约定列」给 SELECT / UPDATE / DELETE 追加 WHERE 条件。
 *
 * <p><b>约定</b>（列名可配，默认如下）：</p>
 * <ul>
 *   <li>表有 {@code dept_id} → {@code dept_id IN (可见部门) OR create_by = 当前账号}；</li>
 *   <li>表有 {@code create_by} 但没有 {@code dept_id} → 部门维度用<b>反查用户表</b>表达：
 *       {@code create_by IN (SELECT username FROM sys_user WHERE dept_id IN (可见部门))}，
 *       因此<b>不需要给业务表加 {@code dept_id}</b>；「仅本人」档再 OR 上 {@code create_by = 当前账号}。
 *       注意此时归属语义是「创建人<b>当前</b>所在部门」，人调岗数据跟着走；</li>
 *   <li>{@code sys_} 前缀的框架表（字典 / 菜单 / 角色 / 配置）跳过创建人维度，它们是共享基础数据；</li>
 *   <li>表被 {@code owner-column-overrides} 指定归属列 → 用该列判断「本人」
 *       （典型：{@code sys_user} 的归属就是主键 {@code id}，比创建人更准确）；</li>
 *   <li>表是 {@code dept-key-tables} → 主键即部门 ID（典型：{@code sys_dept} 自身），
 *       此处「本人」按「本人所在部门」解释，而不是「本人创建的部门」；</li>
 *   <li>其余情况（没有这两类列）不参与。</li>
 * </ul>
 *
 * <p><b>为什么把「约定」放在列上而不是注解上</b>：注解挂在 Mapper 方法上，
 * 新增查询忘加就是越权；列名是表结构的一部分，建表时就固定了，后续任何查询（包括 MPJ 连表、
 * 子查询、分页 count）都会自动继承。业务代码里不出现任何 {@code dept_id IN ...}。</p>
 *
 * <p><b>可见范围从哪来</b>：复用鉴权快照 {@link PermissionSnapshot}（Redis 缓存，10 分钟 TTL，
 * 写侧主动失效）。拦截器只读快照，不额外查库；同一请求内再用 {@link Context} 属性缓存一次，
 * 避免一条多表 SQL 触发多次 Redis 读。</p>
 *
 * <p><b>写语句同样改写</b>：{@code UPDATE / DELETE} 也会被追加条件（MP 的
 * {@code DataPermissionInterceptor} 内置支持），越权写入会变成「影响 0 行」，
 * 而不是「先查一下再决定」——后者一旦漏查就是越权写。</p>
 *
 * <p><b>逃生门</b>：{@code @InterceptorIgnore(dataPermission = "true")} 注解、
 * {@link DataPermissionContext#ignore} 代码块，或配置 {@code ignore-tables}。</p>
 *
 * @author 雨夜终端
 */
@Component
public class AutoDataScopeHandler implements MultiDataPermissionHandler {

    /** 同一请求内缓存权限快照的 Context 属性名 */
    private static final String SNAPSHOT_ATTR = "data-permission.snapshot";

    @Inject
    private PermissionLoader permissionLoader;

    @Inject
    private DataPermissionProperties properties;

    @Inject
    private TableColumnResolver columnResolver;

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        if (!properties.isEnabled() || DataPermissionContext.isIgnored()) {
            return null;
        }
        String tableName = TableColumnResolver.normalize(table.getName());
        if (tableName == null || properties.isIgnored(tableName)) {
            return null;
        }

        Long userId = LoginHelper.getUserIdOrNull();
        if (userId == null) {
            // 无登录态：定时任务 / 内部调用 / 登录前置查询。这些场景代表系统本身，不受数据权限约束
            return null;
        }

        PermissionSnapshot snapshot = snapshotOf(userId);
        if (snapshot == null || snapshot.dataScopeUnrestricted()) {
            return null;
        }

        Map<String, String> columns = columnResolver.columnsOf(tableName);
        if (columns.isEmpty()) {
            return null;
        }

        String deptColumn = resolveDeptColumn(tableName, columns);
        String ownerColumn = resolveOwnerColumn(tableName, columns);

        if (deptColumn == null && ownerColumn == null) {
            return null;
        }

        String qualifier = qualifier(table);
        List<Long> deptIds = snapshot.getDataScopeDeptIds();

        if (deptColumn != null) {
            // 部门维度：可见部门 IN (...) OR 本人数据。
            // 这条 OR 有两个作用：CUSTOM 档可能不含本人所在部门（不留口子会把自己锁在门外），
            // 以及「自己创建的数据在任何档位下都仍然可见」，避免调岗后自己的历史数据消失
            Expression deptScope = (deptIds == null || deptIds.isEmpty())
                    ? null
                    : in(qualifier, deptColumn, deptIds);
            Expression selfScope = buildSelfExpression(tableName, deptColumn, qualifier, ownerColumn, columns, snapshot, userId);
            return or(deptScope, selfScope);
        }

        // 没有部门列：用「创建人反查部门」表达部门维度，并在「仅本人」档额外放行本人数据。
        // sys_ 前缀的框架表（字典 / 菜单 / 角色 / 配置）是共享基础数据，按创建人裁会让
        // 下拉框、菜单树变空，字典还带 @Cache 会把空结果缓存给所有人 —— 这类表直接跳过
        if (properties.isOwnerExcluded(tableName)) {
            return null;
        }
        return buildOwnerScope(tableName, deptColumn, qualifier, ownerColumn, columns, snapshot, userId);
    }

    /**
     * 构造「没有部门列」的表上的过滤条件。
     *
     * <p>这是本方案的关键：业务表只存了 {@code create_by} 时，部门维度通过<b>反查用户表</b>表达，
     * 不需要给每张表加 {@code dept_id}：</p>
     * <pre>
     * create_by IN (SELECT u.username FROM sys_user u WHERE u.dept_id IN (可见部门))
     * </pre>
     *
     * <p>为什么用子查询而不是把用户名展开成 {@code IN ('a','b',...)}：可见人数没有上界，
     * 上千人就会拼出几十 KB 的 SQL，既受 {@code max_allowed_packet} 限制，也让执行计划缓存失效。
     * 子查询的 SQL 长度恒定，且用户表的 {@code dept_id} 有索引。</p>
     *
     * <p>子查询里不过滤 {@code deleted}：逻辑删除的用户行仍在，{@code dept_id} 还在，
     * 他创建的数据依然归属原部门。如果加上 {@code deleted = 0}，一旦有人离职销号，
     * 他的历史数据会集体变成「谁都看不到」的孤儿数据。</p>
     *
     * <p><b>语义提醒</b>：这里的归属是「创建人<b>当前</b>所在部门」，创建人调岗后历史数据会跟着走。
     * 如果业务要求「数据属于创建时的部门」，就必须在表里存 {@code dept_id}（由
     * {@code AutoFillHandler} 在插入时填充），不能靠反查。</p>
     */
    private Expression buildOwnerScope(String tableName, String deptColumn, String qualifier, String ownerColumn,
                                       Map<String, String> columns, PermissionSnapshot snapshot, Long userId) {
        Expression deptScope = (ownerColumn == null)
                ? null
                : buildDeptViaOwner(qualifier, ownerColumn, columns, snapshot);
        Expression selfScope = buildSelfExpression(tableName, deptColumn, qualifier, ownerColumn, columns, snapshot, userId);

        if (deptScope == null && selfScope == null) {
            // 走到这里说明「当前用户确实有数据范围，但目标表上无法表达」：
            // 例如归属列不是字符串（不能装用户名）又没有部门列。安全起见按无可见数据处理，
            // 不能返回 null —— 那等于让 SQL 退化成全表可见
            return alwaysFalse();
        }
        return or(deptScope, selfScope);
    }

    /**
     * 部门维度 → 归属列子查询。
     *
     * @return 可见部门为空、或归属列不是字符串类型时返回 {@code null}
     */
    private Expression buildDeptViaOwner(String qualifier, String ownerColumn,
                                         Map<String, String> columns, PermissionSnapshot snapshot) {
        List<Long> deptIds = snapshot.getDataScopeDeptIds();
        if (deptIds == null || deptIds.isEmpty()) {
            return null;
        }
        // 只有字符串列能承载用户名；数字列（user_id）比较的是用户 ID，无法反查部门
        if (!TableColumnResolver.isTextType(columns.get(ownerColumn))) {
            return null;
        }

        String userAlias = "dp_owner";
        List<Expression> deptValues = new ArrayList<>(deptIds.size());
        for (Long deptId : deptIds) {
            deptValues.add(new LongValue(deptId));
        }

        Table userTable = new Table(properties.getUserTable());
        userTable.setAlias(new Alias(userAlias, false));
        PlainSelect subSelect = new PlainSelect();
        subSelect.setSelectItems(List.of(
                new SelectItem<>(new Column(new Table(userAlias), properties.getUsernameColumn()))));
        subSelect.setFromItem(userTable);
        subSelect.setWhere(new InExpression(
                new Column(new Table(userAlias), properties.getDeptColumn()),
                new ParenthesedExpressionList<>(deptValues)));

        ParenthesedSelect parenthesed = new ParenthesedSelect();
        parenthesed.setSelect(subSelect);

        return new InExpression(new Column(qualified(qualifier, ownerColumn)), parenthesed);
    }

    /**
     * 解析部门列：dept-key 表用主键，其余表取约定列。
     */
    private String resolveDeptColumn(String tableName, Map<String, String> columns) {
        if (properties.isDeptKeyTable(tableName)) {
            return columns.containsKey("id") ? "id" : null;
        }
        return columns.containsKey(properties.getDeptColumn()) ? properties.getDeptColumn() : null;
    }

    /**
     * 解析归属列：表里有这一列就参与（是否真的裁剪由调用方按档位和表前缀决定）。
     */
    private String resolveOwnerColumn(String tableName, Map<String, String> columns) {
        String candidate = properties.ownerColumnOf(tableName);
        return candidate != null && columns.containsKey(candidate) ? candidate : null;
    }

    /**
     * 构造「本人数据」条件。
     *
     * <p>部门树的语义特殊：行本身就是部门，所以「本人」= 本人所在部门，
     * 而不是「本人创建的部门」—— 否则「仅本人」档的用户会看到一棵空树，
     * 连自己的部门导航都点不到。</p>
     *
     * <p>列类型决定字面量：{@code create_by} 是 VARCHAR 存用户名，{@code id} / {@code user_id}
     * 是 BIGINT 存用户 ID。拼错类型的表现是「不报错但永远查不到」。</p>
     */
    private Expression buildSelfExpression(String tableName, String deptColumn, String qualifier,
                                           String ownerColumn, Map<String, String> columns,
                                           PermissionSnapshot snapshot, Long userId) {
        if (properties.isDeptKeyTable(tableName)) {
            Long deptId = snapshot.getDataScopeDeptId();
            if (!snapshot.dataScopeHasSelf() || deptId == null) {
                return null;
            }
            // 部门表用主键（部门 ID）比较，不能拿归属列去比 —— 那是创建人账号
            return eqLong(qualifier, deptColumn, deptId);
        }
        if (ownerColumn == null) {
            return null;
        }
        if (TableColumnResolver.isTextType(columns.get(ownerColumn))) {
            String username = LoginHelper.getUsernameOrNull();
            // 登录态存在但拿不到账号（会话未装载 LoginUser）：宁可不加这一条，
            // 也不要拼一个空字符串去匹配，那会把「创建人为空」的历史数据全部漏出来
            return username == null || username.isBlank()
                    ? null
                    : eqString(qualifier, ownerColumn, username);
        }
        return eqLong(qualifier, ownerColumn, userId);
    }

    /**
     * 取当前请求的权限快照，同一请求内只读一次。
     *
     * <p>{@link PermissionLoader#load} 自带 Redis 缓存，但一条 SQL 里每张表都会回调本方法，
     * 多表连查就是多次 Redis 往返。Context 属性随请求销毁，不存在跨请求串用。</p>
     */
    private PermissionSnapshot snapshotOf(Long userId) {
        Context ctx = Context.current();
        if (ctx == null) {
            return permissionLoader.load(userId);
        }
        Object cached = ctx.attr(SNAPSHOT_ATTR);
        if (cached instanceof PermissionSnapshot snapshot) {
            return snapshot;
        }
        PermissionSnapshot snapshot = permissionLoader.load(userId);
        ctx.attrSet(SNAPSHOT_ATTR, snapshot);
        return snapshot;
    }

    /** 条件里的表限定符：有别名用别名，否则用表名 */
    private static String qualifier(Table table) {
        if (table.getAlias() != null && table.getAlias().getName() != null) {
            return table.getAlias().getName();
        }
        return TableColumnResolver.normalize(table.getName());
    }

    /** 两段条件取 OR；任一为空则返回另一个；都为空返回恒假 */
    private static Expression or(Expression left, Expression right) {
        if (left == null && right == null) {
            // 1 = 0：范围为空必须显式翻译成恒假，绝不能返回 null 让 SQL 退化成全表可见
            return alwaysFalse();
        }
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return new ParenthesedExpressionList<>(new OrExpression(left, right));
    }

    private static Expression in(String qualifier, String column, List<Long> ids) {
        List<Expression> values = new ArrayList<>(ids.size());
        for (Long id : ids) {
            values.add(new LongValue(id));
        }
        InExpression in = new InExpression();
        in.setLeftExpression(new Column(qualified(qualifier, column)));
        in.setRightExpression(new ParenthesedExpressionList<>(values));
        return in;
    }

    private static Expression eqLong(String qualifier, String column, long value) {
        return new EqualsTo(new Column(qualified(qualifier, column)), new LongValue(value));
    }

    private static Expression eqString(String qualifier, String column, String value) {
        return new EqualsTo(new Column(qualified(qualifier, column)), new StringValue(value));
    }

    private static Expression alwaysFalse() {
        return new EqualsTo(new LongValue(1), new LongValue(0));
    }

    private static String qualified(String qualifier, String column) {
        return qualifier == null || qualifier.isBlank() ? column : qualifier + "." + column;
    }
}
