package com.designer.admin.framework.job;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.quartz.utils.ConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Quartz 连接提供者：复用 Solon 已装配的数据源。
 *
 * <p><b>为什么不让 Quartz 自己建池</b>（例如 {@code HikariCpPoolingConnectionProvider}）：
 * 那会再建一个连接池，连接上限、超时、探测全都不受 {@code app.yml} 控制。
 * 线上出现「连接耗尽」时，也无法从一份配置判断是业务池还是调度池打满。
 * 这里把 Quartz 挂到同一个 {@link DataSource} 上，连接数统一由 Hikari 管理。</p>
 *
 * <p><b>{@link #shutdown()} 为什么是空的</b>：Quartz 停止时会调用它，
 * 但这里拿到的是<b>共享</b>池。一旦 {@code dataSource.close()}，
 * 业务侧同一个池也会被关掉，表现为应用还在跑、所有 SQL 突然报连接已关闭。
 * 池的生命周期归 Solon 管，不应由调度器负责。</p>
 *
 * <p>数据源名取自 {@code designer.quartz.data-source}（默认 {@code db1}），
 * 与 {@code solon.dataSources} 下的条目名一致（配置里的结尾 {@code !} 只是「默认数据源」标记，不属于名字）。</p>
 *
 * @author 雨夜终端
 */
@Slf4j
public class SolonDataSourceConnectionProvider implements ConnectionProvider {

    /** 数据源名配置项；键名带 designer 前缀，避免与 Quartz 自己的 org.quartz.* 命名空间混淆 */
    private static final String CONFIG_KEY = "designer.quartz.data-source";

    /** 与 app.yml 中 solon.dataSources 的条目名一致 */
    private static final String DEFAULT_DATA_SOURCE_NAME = "db1";

    private DataSource dataSource;

    @Override
    public void initialize() throws SQLException {
        String name = Solon.cfg().get(CONFIG_KEY, DEFAULT_DATA_SOURCE_NAME);
        Object bean = Solon.context().getBean(name);
        if (bean == null) {
            // 退一步按类型找：单数据源时一定命中；多数据源时必须显式配置名字
            bean = Solon.context().getBean(DataSource.class);
        }
        if (!(bean instanceof DataSource ds)) {
            throw new SQLException("Quartz 无法获取数据源 [" + name + "]，请检查 " + CONFIG_KEY
                    + " 与 solon.dataSources 配置是否一致");
        }
        this.dataSource = ds;
        log.info("Quartz 使用数据源 [{}]（复用 Solon 连接池，不额外建池）", name);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() {
        // 共享池由 Solon 关闭，这里不能关；原因见类注释
    }
}
