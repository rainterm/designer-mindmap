package com.designer.admin.module.agent.workflow.activity;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.model.dto.workflow.node.SqlDatasourceDTO;
import com.designer.admin.module.agent.model.dto.workflow.node.SqlNodeConfigDTO;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import com.designer.admin.module.agent.workflow.support.WorkflowVarSupport;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SQL 活动节点执行。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowSqlActivity {

    /** 本类日志 */
    private static final Logger log = LoggerFactory.getLogger(WorkflowSqlActivity.class);
    /** 结果集最大行数，防止一次拉爆内存 */
    private static final int SQL_MAX_ROWS = 100;
    /** 单次查询超时（秒） */
    private static final int SQL_TIMEOUT_SEC = 10;
    /** 禁止出现的写操作 / 危险关键字（忽略大小写） */
    private static final Pattern SQL_FORBIDDEN = Pattern.compile(
            "(?is).*\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|replace|merge|call|exec|execute|into\\s+outfile|load_file)\\b.*");

    /** 默认数据源（主库），datasourceId 未选时回退使用 */
    @Inject
    private DataSource dataSource;

    /**
     * 执行只读 SQL：变量绑定后校验单语句与白名单前缀，结果集序列化为 JSON 数组。
     *
     * @param config 节点 JSON 配置（sql / datasourceId / datasources）
     * @param vars   工作流变量表，用于绑定 SQL 占位符
     * @return 结果集 JSON，或无结果集时的提示文案
     * @throws Exception 驱动/连接等底层异常；业务校验与执行失败为 {@link BizException}
     */
    public String runSql(String config, Map<String, String> vars) throws Exception {
        SqlNodeConfigDTO cfg = AgentJsonUtil.parse(config, SqlNodeConfigDTO.class);
        if (cfg == null) {
            cfg = new SqlNodeConfigDTO();
        }
        String sqlTpl = cfg.getSql() == null ? "" : cfg.getSql();
        if (sqlTpl.isBlank()) {
            throw new BizException("SQL 节点未填写语句");
        }
        WorkflowVarSupport.BoundSql bound;
        try {
            bound = WorkflowVarSupport.bindSql(sqlTpl, vars);
        } catch (IllegalArgumentException e) {
            throw new BizException(e.getMessage(), e);
        }
        String sql = bound.sql().trim();
        // 去掉末尾分号，禁止多语句
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (sql.contains(";")) {
            throw new BizException("仅允许单条 SQL");
        }
        String lower = sql.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("select") || lower.startsWith("with") || lower.startsWith("show")
                || lower.startsWith("describe") || lower.startsWith("desc ") || lower.startsWith("explain"))) {
            throw new BizException("仅允许 SELECT / WITH / SHOW / DESCRIBE / EXPLAIN");
        }
        if (SQL_FORBIDDEN.matcher(sql).matches()) {
            throw new BizException("SQL 包含禁止关键字");
        }

        try (Connection conn = openSqlConnection(cfg);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            List<Object> params = bound.params();
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.setMaxRows(SQL_MAX_ROWS);
            stmt.setQueryTimeout(SQL_TIMEOUT_SEC);
            boolean hasResult = stmt.execute();
            if (!hasResult) {
                return "（无结果集，更新行数=" + stmt.getUpdateCount() + "）";
            }
            try (ResultSet rs = stmt.getResultSet()) {
                return resultSetToJson(rs, SQL_MAX_ROWS);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("SQL 执行失败", e);
            throw new BizException("SQL 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 SQL 节点选中的数据源：选中且匹配上时按内联连接信息建连接；
     * 未选（datasourceId 为空）时回退到默认 dataSource（主库），保持向后兼容；
     * 已选但 datasources 缺失或 id 未匹配时直接报错，不回退主库。
     *
     * @param cfg 节点配置
     * @return JDBC 连接（调用方负责关闭）
     * @throws Exception 连接失败或驱动缺失时抛出
     */
    private Connection openSqlConnection(SqlNodeConfigDTO cfg) throws Exception {
        String dsId = cfg.getDatasourceId() == null ? "" : cfg.getDatasourceId().trim();
        if (dsId.isBlank()) {
            return dataSource.getConnection();
        }
        List<SqlDatasourceDTO> list = cfg.getDatasources();
        if (list == null || list.isEmpty()) {
            throw new BizException("SQL 节点已选择数据源但未配置 datasources：datasourceId=" + dsId);
        }
        SqlDatasourceDTO ds = null;
        for (SqlDatasourceDTO item : list) {
            if (item != null && dsId.equals(item.getId())) {
                ds = item;
                break;
            }
        }
        if (ds == null) {
            throw new BizException("未找到选中的数据源配置：datasourceId=" + dsId);
        }

        String type = Utils.isBlank(ds.getType()) ? "mysql" : ds.getType().toLowerCase(Locale.ROOT);
        String jdbcUrl = ds.getJdbcUrl() == null ? "" : ds.getJdbcUrl();
        if (jdbcUrl.isBlank()) {
            int port = ds.getPort() == null ? defaultPort(type) : ds.getPort();
            jdbcUrl = buildJdbcUrl(type,
                    Utils.isBlank(ds.getHost()) ? "127.0.0.1" : ds.getHost(),
                    port,
                    ds.getDatabase() == null ? "" : ds.getDatabase());
        }
        String user = ds.getUsername() == null ? "" : ds.getUsername();
        String pass = ds.getPassword() == null ? "" : ds.getPassword();
        try {
            return DriverManager.getConnection(jdbcUrl, user, pass);
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            String low = msg.toLowerCase(Locale.ROOT);
            if (low.contains("no suitable driver")) {
                throw new BizException("数据源类型 " + type + " 未配置 JDBC 驱动，无法连接", e);
            }
            throw new BizException("数据源连接失败：" + msg, e);
        }
    }

    /**
     * 按库类型给出默认端口（PostgreSQL / MySQL）。
     *
     * @param type 数据源类型
     * @return 默认端口
     */
    private static int defaultPort(String type) {
        return "postgresql".equals(type) ? AgentConsts.DEFAULT_POSTGRES_PORT : AgentConsts.DEFAULT_MYSQL_PORT;
    }

    /**
     * 由 host/port/database 拼装 JDBC URL。
     *
     * @param type     数据源类型
     * @param host     主机
     * @param port     端口
     * @param database 库名
     * @return jdbc:mysql 或 jdbc:postgresql URL
     */
    private static String buildJdbcUrl(String type, String host, int port, String database) {
        String scheme = "postgresql".equals(type) ? "jdbc:postgresql" : "jdbc:mysql";
        return scheme + "://" + host + ":" + port + "/" + database;
    }

    /**
     * 将结果集前 maxRows 行转为 JSON 数组字符串（列名取 label）。
     *
     * @param rs      结果集
     * @param maxRows 最大行数
     * @return JSON 数组
     * @throws Exception 读取结果集失败时抛出
     */
    private static String resultSetToJson(ResultSet rs, int maxRows) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        int n = 0;
        while (rs.next() && n < maxRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                String label = meta.getColumnLabel(i);
                Object val = rs.getObject(i);
                row.put(label, val);
            }
            rows.add(row);
            n++;
        }
        return AgentJsonUtil.toJson(rows);
    }
}
