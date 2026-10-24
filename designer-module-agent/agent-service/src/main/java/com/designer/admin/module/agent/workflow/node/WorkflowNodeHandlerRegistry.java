package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * 按 {@link WorkflowNodeType} 解析节点处理器；新增 Handler 只需声明为 Bean。
 *
 * <p>使用 {@link AppContext#subBeansOfType} 收集，避免 {@code @Inject List<>} 拿不到实现。</p>
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowNodeHandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNodeHandlerRegistry.class);

    @Inject
    private AppContext appContext;

    /** 节点类型到处理器的映射。 */
    private final Map<WorkflowNodeType, WorkflowNodeHandler> byType = new EnumMap<>(WorkflowNodeType.class);

    @Init
    public void init() {
        byType.clear();
        appContext.subBeansOfType(WorkflowNodeHandler.class, h -> {
            if (h == null || h.type() == null) {
                return;
            }
            WorkflowNodeHandler old = byType.put(h.type(), h);
            if (old != null && old != h) {
                log.warn("工作流节点 Handler 重复 type={}，后者覆盖前者", h.type());
            }
        });
        if (byType.isEmpty()) {
            log.warn("未发现任何 WorkflowNodeHandler 实现");
        } else {
            log.info("已注册 {} 个 WorkflowNodeHandler", byType.size());
        }
    }

    /**
     * 按节点类型获取处理器。
     *
     * @param type 节点类型，为 null 时返回 null
     * @return 对应处理器；未注册时返回 null
     */
    public WorkflowNodeHandler get(WorkflowNodeType type) {
        return type == null ? null : byType.get(type);
    }
}
