package com.designer.admin.module.agent.service.knowledge.vector;

import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 向量存储入口：按配置选定一种 {@link AgentVectorStore} 实现并对外提供。
 *
 * <p>容器里可能同时存在 Milvus、Redis 等多个实现，直接按类型注入会变成
 * 「谁先注册谁生效」。这里用 {@code designer.agent.vector.type} 显式选择，
 * 配错在启动时立即失败。</p>
 *
 * @author 雨夜终端
 */
@Component
public class AgentVectorStoreManager {

    private static final Logger log = LoggerFactory.getLogger(AgentVectorStoreManager.class);

    private static final String TYPE_CONFIG_KEY = "designer.agent.vector.type";

    /** 默认 Redis Stack（本地 deploy-db 已含 RediSearch）；大规模可改为 milvus。 */
    private static final String DEFAULT_TYPE = "redis";

    private AgentVectorStore store;

    @Init(index = 0)
    public void init() {
        String type = Solon.cfg().get(TYPE_CONFIG_KEY, DEFAULT_TYPE);
        List<AgentVectorStore> candidates = Solon.context().getBeansOfType(AgentVectorStore.class);

        for (AgentVectorStore candidate : candidates) {
            if (type.equalsIgnoreCase(candidate.type())) {
                store = candidate;
                break;
            }
        }

        if (store == null) {
            Set<String> available = new TreeSet<>();
            for (AgentVectorStore candidate : candidates) {
                available.add(candidate.type());
            }
            throw new IllegalStateException("未找到向量存储实现：" + TYPE_CONFIG_KEY + "=" + type
                    + "，当前可用实现 " + new ArrayList<>(available)
                    + "（新增实现只需实现 AgentVectorStore 并标注 @Component）");
        }

        store.onSelected();
        log.info("知识库向量存储已启用：{}", store.type());
    }

    /**
     * 当前生效的实现。
     *
     * @return 向量存储
     */
    public AgentVectorStore store() {
        if (store == null) {
            throw new IllegalStateException("向量存储尚未初始化");
        }
        return store;
    }

    /**
     * 当前实现标识。
     *
     * @return milvus / redis 等
     */
    public String type() {
        return store().type();
    }
}
