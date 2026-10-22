package com.designer.admin.framework.mybatis;

import java.util.concurrent.Callable;

/**
 * 数据权限抑制开关（线程内生效）。
 *
 * <p><b>为什么必须有这个开关</b>：数据权限是全局 SQL 改写，它会连「用来判断权限本身」的查询一起改写。
 * 最典型的死循环：装载权限快照 → 查 {@code sys_user} → 拦截器又要先装载快照 → 再查 {@code sys_user} ……
 * 除了死循环，还有一类<b>正确性</b>问题：账号唯一性校验、超管保护、唯一键冲突判断都必须看全表，
 * 被数据权限裁掉的行会让校验静默失效（例如另一个部门已存在同名账号，却判断成「可用」）。</p>
 *
 * <p>因此凡是「系统自身的事实性查询」都应在 {@link #ignore(Callable)} 里执行；
 * 业务查询不要用它 —— 用了就等于这块数据不受数据权限约束，属于越权口子。</p>
 *
 * @author 雨夜终端
 */
public final class DataPermissionContext {

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private DataPermissionContext() {
    }

    /**
     * 在「不受数据权限约束」的上下文里执行。
     *
     * <p>用深度计数而不是布尔值：装载权限快照的过程中可能再嵌套一层（例如缓存 miss 后回源、
     * 或某个校验里又调了一次权限服务），布尔值会在内层 {@code finally} 里提前把开关关掉。</p>
     *
     * @param action 查询逻辑
     * @param <T>    返回类型
     * @return 查询结果
     */
    public static <T> T ignore(Callable<T> action) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Callable 的受检异常无法直接上抛；这里的调用点都是 Mapper，不应产生受检异常
            throw new IllegalStateException("执行数据权限忽略块失败", e);
        } finally {
            int depth = DEPTH.get() - 1;
            if (depth <= 0) {
                DEPTH.remove();
            } else {
                DEPTH.set(depth);
            }
        }
    }

    /** 无返回值版本 */
    public static void ignore(Runnable action) {
        ignore(() -> {
            action.run();
            return null;
        });
    }

    /** 当前线程是否处于忽略状态（拦截器用） */
    static boolean isIgnored() {
        return DEPTH.get() > 0;
    }
}
