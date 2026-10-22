package com.designer.admin.module.agent.workflow.activity;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.util.AgentJson;
import com.designer.admin.module.agent.workflow.support.WorkflowVarSupport;
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

import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.intVal;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.parseCfg;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.str;
/**
 * SQL 活动节点执行。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowSqlActivity {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSqlActivity.class);
    private static final int SQL_MAX_ROWS = 100;
    private static final int SQL_TIMEOUT_SEC = 10;
    private static final Pattern SQL_FORBIDDEN = Pattern.compile(
            "(?is).*\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|replace|merge|call|exec|execute|into\\s+outfile|load_file)\\b.*");

    @Inject
    private DataSource dataSource;

    public String runSql(String config, Map<String, String> vars) throws Exception {
        Map<String, Object> cfg = parseCfg(config);
        String sqlTpl = str(cfg.get("sql"), "");
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
     */
    private Connection openSqlConnection(Map<String, Object> cfg) throws Exception {
        String dsId = str(cfg.get("datasourceId"), "");
        if (dsId.isBlank()) {
            return dataSource.getConnection();
        }
        Object dsObj = cfg.get("datasources");
        if (!(dsObj instanceof List<?> list) || list.isEmpty()) {
            throw new BizException("SQL 节点已选择数据源但未配置 datasources：datasourceId=" + dsId);
        }
        Map<String, Object> ds = null;
        for (Object item : list) {
            if (item instanceof Map<?, ?> m && dsId.equals(String.valueOf(m.get("id")))) {
                ds = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    ds.put(String.valueOf(e.getKey()), e.getValue());
                }
                break;
            }
        }
        if (ds == null) {
            throw new BizException("未找到选中的数据源配置：datasourceId=" + dsId);
        }

        String type = str(ds.get("type"), "mysql").toLowerCase(Locale.ROOT);
        String jdbcUrl = str(ds.get("jdbcUrl"), "");
        if (jdbcUrl.isBlank()) {
            jdbcUrl = buildJdbcUrl(type,
                    str(ds.get("host"), "127.0.0.1"),
                    intVal(ds.get("port"), defaultPort(type)),
                    str(ds.get("database"), ""));
        }
        String user = str(ds.get("username"), "");
        String pass = str(ds.get("password"), "");
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

    private static int defaultPort(String type) {
        return "postgresql".equals(type) ? AgentConsts.DEFAULT_POSTGRES_PORT : AgentConsts.DEFAULT_MYSQL_PORT;
    }

    private static String buildJdbcUrl(String type, String host, int port, String database) {
        String scheme = "postgresql".equals(type) ? "jdbc:postgresql" : "jdbc:mysql";
        return scheme + "://" + host + ":" + port + "/" + database;
    }

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
        return AgentJson.toJson(rows);
    }
}
