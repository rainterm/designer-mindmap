package com.designer.admin.common.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用树构建工具：按 id / parentId / children 约定反射组装。
 *
 * <p>节点类型需具备方法：{@code getId(): Long}、{@code getParentId(): Long}、
 * {@code setChildren(List)} / {@code getChildren(): List}。
 *
 * @author 雨夜终端
 */
public final class TreeUtils {

    private TreeUtils() {
    }

    public static <T> List<T> buildTree(List<T> nodes, Long rootParentId) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, T> idMap = new LinkedHashMap<>();
        for (T node : nodes) {
            idMap.put(readLong(node, "getId"), node);
        }

        List<T> roots = new ArrayList<>();
        for (T node : nodes) {
            Long pid = readLong(node, "getParentId");
            T parent = pid == null ? null : idMap.get(pid);
            if (parent == null || parent == node || pid.equals(rootParentId)) {
                roots.add(node);
                continue;
            }
            childrenOf(parent).add(node);
        }
        return roots;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> childrenOf(T node) {
        try {
            Method getter = node.getClass().getMethod("getChildren");
            Object val = getter.invoke(node);
            if (val instanceof List<?> list) {
                return (List<T>) list;
            }
            List<T> created = new ArrayList<>();
            node.getClass().getMethod("setChildren", List.class).invoke(node, created);
            return created;
        } catch (Exception e) {
            throw new IllegalStateException("树节点缺少 getChildren/setChildren 方法: " + node.getClass(), e);
        }
    }

    private static Long readLong(Object node, String method) {
        try {
            Object val = node.getClass().getMethod(method).invoke(node);
            return val == null ? null : ((Number) val).longValue();
        } catch (Exception e) {
            throw new IllegalStateException("树节点缺少 " + method + " 方法: " + node.getClass(), e);
        }
    }
}
