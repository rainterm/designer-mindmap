package com.designer.admin.framework.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.noear.solon.Solon;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 延迟解析 {@link AutoDataScopeHandler} 的数据权限拦截器。
 *
 * <p><b>为什么要包一层</b>：MyBatis-Plus 的拦截器是在 Solon 配置类扫描阶段装配的，
 * 那个时刻 {@code Solon.context().getBean(...)} 还拿不到普通组件，只能拿到 {@code null}。
 * 直接 {@code new DataPermissionInterceptor(null)} 不报任何错，SQL 也全部正常执行，
 * 只是<b>一条条件都不会追加</b> —— 这是最难发现的失效方式：功能「看起来」上线了。</p>
 *
 * <p>所以这里把解析推迟到第一次真正执行 SQL 时（容器早已就绪），并在之后复用同一个实例。</p>
 *
 * @author 雨夜终端
 */
public class LazyHandlerDataPermissionInterceptor extends DataPermissionInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        ensureHandler();
        super.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        ensureHandler();
        super.beforePrepare(sh, connection, transactionTimeout);
    }

    private void ensureHandler() {
        if (getDataPermissionHandler() != null) {
            return;
        }
        DataPermissionHandler handler = Solon.context().getBean(AutoDataScopeHandler.class);
        if (handler != null) {
            setDataPermissionHandler(handler);
        }
    }
}
