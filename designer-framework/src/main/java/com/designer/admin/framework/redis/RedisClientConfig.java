package com.designer.admin.framework.redis;

import org.noear.redisx.RedisClient;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

/**
 * 业务侧 Redis 客户端（与 Sa-Token 共用 sa-token-dao-redis 配置）
 *
 * @author 雨夜终端
 */
@Configuration
public class RedisClientConfig {

    @Bean
    public RedisClient redisClient() {
        return new RedisClient(Solon.cfg().getProp("sa-token-dao-redis"));
    }
}
