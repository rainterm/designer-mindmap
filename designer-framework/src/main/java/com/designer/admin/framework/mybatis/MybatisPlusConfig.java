package com.designer.admin.framework.mybatis;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

/**
 * MyBatis-Plus 扩展：数据权限 SQL 改写、分页、全表更新/删除防护。
 *
 * <p><b>顺序是有讲究的</b>：数据权限（多表改写）必须在分页之前 ——
 * 分页插件会在改写后的 SQL 外面再包一层 count / limit，顺序反了会把数据权限条件加到外层，
 * 变成一个既不报错又完全不生效的「伪过滤」。这与官方文档里「多租户 / 动态表名 → 分页 → 防全表」的顺序一致。</p>
 *
 * @author 雨夜终端
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public void db1Cfg(@Db("db1") MybatisConfiguration cfg) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 数据权限：按表里的约定列（dept_id / user_id）自动追加 WHERE，业务代码零侵入。
        // handler 在第一次执行 SQL 时才解析（配置类扫描阶段容器还没注册普通组件，见类注释）
        interceptor.addInnerInterceptor(new LazyHandlerDataPermissionInterceptor());

        // 不写死 DbType：由 PaginationInnerInterceptor 按 JDBC URL 自动识别方言，
        // 换库（MySQL / PostgreSQL / 达梦 …）时无需改代码
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor();
        pagination.setMaxLimit(500L);
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        cfg.addInterceptor(interceptor);
    }
}
