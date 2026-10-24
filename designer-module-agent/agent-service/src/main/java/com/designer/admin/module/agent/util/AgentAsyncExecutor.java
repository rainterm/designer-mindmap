package com.designer.admin.module.agent.util;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.util.ThreadsUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * 智能体模块异步任务执行器（SSE 流式对话 / 工作流试跑 / 索引进度等）。
 * <p>
 * 使用 Solon 虚拟线程池承载 I/O 等待型任务；以信号量限制同时运行数，
 * 避免打爆下游 LLM / Embedding / Milvus。禁止使用 {@code CompletableFuture.runAsync} 默认公共池。
 *
 * @author 雨夜终端
 */
@Component
public class AgentAsyncExecutor {

    /** 同时运行的流式 / 异步任务上限，见 {@code designer.agent.async.max-concurrent} */
    @Inject("${designer.agent.async.max-concurrent:64}")
    private int maxConcurrent;

    private ExecutorService executor;
    private Semaphore permits;

    @Init
    public void init() {
        int permitsCount = Math.max(1, maxConcurrent);
        this.executor = ThreadsUtil.newVirtualThreadPerTaskExecutor("agent-async-");
        this.permits = new Semaphore(permitsCount);
    }

    public void execute(Runnable task) {
        if (!permits.tryAcquire()) {
            // 饱和时在调用方执行，等同原 CallerRunsPolicy，形成背压
            task.run();
            return;
        }
        executor.execute(() -> {
            try {
                task.run();
            } finally {
                permits.release();
            }
        });
    }

    @Destroy
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
