package com.designer.admin.framework.job;

import com.designer.admin.common.exception.BizException;
import org.noear.solon.Solon;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 解析并调用 invoke_target：beanName.methodName(arg1, arg2, ...)
 *
 * @author 雨夜终端
 */
public final class JobInvokeUtils {

    /** bean 名与方法名的分隔符 */
    private static final String METHOD_SEPARATOR = ".";
    /** 参数列表左括号 */
    private static final char PARAM_OPEN = '(';
    /** 参数列表右括号 */
    private static final char PARAM_CLOSE = ')';
    /** 参数之间的分隔符 */
    private static final char PARAM_SEPARATOR = ',';
    /** 单引号 */
    private static final char SINGLE_QUOTE = '\'';
    /** 双引号 */
    private static final char DOUBLE_QUOTE = '"';
    /** 转义符 */
    private static final char ESCAPE = '\\';

    /** 成对引号包裹的最短长度（两个引号字符） */
    private static final int MIN_QUOTED_LENGTH = 2;

    private JobInvokeUtils() {
    }

    public static void invokeMethod(String invokeTarget) throws Exception {
        String beanName = beanName(invokeTarget);
        String methodName = methodName(invokeTarget);
        List<Object[]> methodParams = methodParams(invokeTarget);

        Object bean = Solon.context().getBean(beanName);
        if (bean == null) {
            throw new BizException("未找到 Bean：" + beanName + "，请确认已用 @Component(\"" + beanName + "\") 注册");
        }
        invoke(bean, methodName, methodParams);
    }

    public static void validate(String invokeTarget) {
        if (invokeTarget == null || invokeTarget.isBlank()) {
            throw new BizException("调用目标不能为空");
        }
        String target = invokeTarget.trim();
        if (!target.contains(METHOD_SEPARATOR)) {
            throw new BizException("调用目标格式错误，应为 bean.method 或 bean.method(args)");
        }
        String beanName = beanName(target);
        if (beanName.isEmpty() || methodName(target).isEmpty()) {
            throw new BizException("调用目标格式错误，应为 bean.method 或 bean.method(args)");
        }
        if (Solon.context().getBean(beanName) == null) {
            throw new BizException("未找到 Bean：" + beanName);
        }
    }

    private static void invoke(Object bean, String methodName, List<Object[]> methodParams) throws Exception {
        if (methodParams != null && !methodParams.isEmpty()) {
            Method method = bean.getClass().getDeclaredMethod(methodName, getMethodParamsType(methodParams));
            method.setAccessible(true);
            method.invoke(bean, getMethodParamsValue(methodParams));
        } else {
            Method method = bean.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(bean);
        }
    }

    private static String beanName(String invokeTarget) {
        String name = invokeTarget.substring(0, invokeTarget.indexOf(METHOD_SEPARATOR));
        return name.trim();
    }

    private static String methodName(String invokeTarget) {
        String method = invokeTarget.substring(invokeTarget.indexOf(METHOD_SEPARATOR) + METHOD_SEPARATOR.length());
        int idx = method.indexOf(PARAM_OPEN);
        return (idx > 0 ? method.substring(0, idx) : method).trim();
    }

    private static List<Object[]> methodParams(String invokeTarget) {
        int start = invokeTarget.indexOf(PARAM_OPEN);
        int end = invokeTarget.lastIndexOf(PARAM_CLOSE);
        if (start < 0 || end <= start + 1) {
            return null;
        }
        String params = invokeTarget.substring(start + 1, end).trim();
        if (params.isEmpty()) {
            return null;
        }
        List<Object[]> result = new LinkedList<>();
        for (String part : splitParams(params)) {
            String p = part.trim();
            if (isQuoted(p)) {
                result.add(new Object[]{p.substring(1, p.length() - 1), String.class});
            } else if ("true".equalsIgnoreCase(p) || "false".equalsIgnoreCase(p)) {
                result.add(new Object[]{Boolean.valueOf(p), Boolean.class});
            } else if (p.endsWith("L") || p.endsWith("l")) {
                result.add(new Object[]{Long.valueOf(p.substring(0, p.length() - 1)), Long.class});
            } else if (p.endsWith("D") || p.endsWith("d")) {
                result.add(new Object[]{Double.valueOf(p.substring(0, p.length() - 1)), Double.class});
            } else if (p.contains(".")) {
                result.add(new Object[]{Double.valueOf(p), Double.class});
            } else {
                result.add(new Object[]{Integer.valueOf(p), Integer.class});
            }
        }
        return result;
    }

    /**
     * 判断参数是否被成对引号包裹。
     *
     * @param param 参数文本
     * @return 被单引号或双引号包裹时返回 {@code true}
     */
    private static boolean isQuoted(String param) {
        if (param.length() < MIN_QUOTED_LENGTH) {
            return false;
        }
        char first = param.charAt(0);
        char last = param.charAt(param.length() - 1);
        return (first == SINGLE_QUOTE || first == DOUBLE_QUOTE) && first == last;
    }

    /**
     * 判断指定位置是否为未转义的引号。
     *
     * @param text  原始文本
     * @param index 字符下标
     * @return 该位置是未转义的引号时返回 {@code true}
     */
    private static boolean isUnescapedQuote(String text, int index) {
        char c = text.charAt(index);
        if (c != SINGLE_QUOTE && c != DOUBLE_QUOTE) {
            return false;
        }
        return index == 0 || text.charAt(index - 1) != ESCAPE;
    }

    /** 按逗号拆分参数，忽略引号内逗号 */
    private static List<String> splitParams(String params) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        char quote = 0;
        for (int i = 0; i < params.length(); i++) {
            char c = params.charAt(i);
            if (isUnescapedQuote(params, i)) {
                if (!inQuote) {
                    inQuote = true;
                    quote = c;
                } else if (c == quote) {
                    inQuote = false;
                }
                cur.append(c);
            } else if (c == PARAM_SEPARATOR && !inQuote) {
                list.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (!cur.isEmpty()) {
            list.add(cur.toString());
        }
        return list;
    }

    private static Class<?>[] getMethodParamsType(List<Object[]> methodParams) {
        Class<?>[] types = new Class<?>[methodParams.size()];
        for (int i = 0; i < methodParams.size(); i++) {
            types[i] = (Class<?>) methodParams.get(i)[1];
        }
        return types;
    }

    private static Object[] getMethodParamsValue(List<Object[]> methodParams) {
        Object[] values = new Object[methodParams.size()];
        for (int i = 0; i < methodParams.size(); i++) {
            values[i] = methodParams.get(i)[0];
        }
        return values;
    }
}
