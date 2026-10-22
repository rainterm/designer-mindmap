package com.designer.admin.framework.satoken;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisx;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * Sa-Token 会话持久化到 Redis（redisx 客户端）
 *
 * <p>配置节 sa-token-dao-redis 会被映射进 SaTokenDaoForRedisx 的 Properties 构造。
 *
 * @author 雨夜终端
 */
@Configuration
public class SaTokenDaoConfig {

    @Bean
    public SaTokenDao saTokenDaoInit(@Inject("${sa-token-dao-redis}") SaTokenDaoForRedisx saTokenDao) {
        return saTokenDao;
    }
}
