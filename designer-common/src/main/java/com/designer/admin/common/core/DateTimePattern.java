package com.designer.admin.common.core;

/**
 * 接口返回给前端的时间格式。
 *
 * <p><b>时间格式只在这里定义。</b>后端用 {@code @ONodeAttr(format = ...)} 在序列化阶段
 * 就把 {@code LocalDateTime} 转成字符串，前端原样展示、不再做任何拼接或格式化 ——
 * 否则同一个时间在列表、详情、后台三处会各自格式化一次，格式迟早跑偏，
 * 更麻烦的是「时间串能不能被浏览器解析」也变成了前端问题
 * （{@code yyyy-MM-dd HH:mm:ss} 这种空格分隔的写法在旧版 Safari 里是非法日期）。</p>
 *
 * <p>注解的 {@code format} 要求编译期常量，所以这里必须是 {@code static final String}，
 * 不能换成方法调用。</p>
 *
 * @author 雨夜终端
 */
public final class DateTimePattern {

    /** 日期 + 时分秒，用于发布 / 更新 / 创建等需要精确到秒的展示 */
    public static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    /** 仅日期，用于按天聚合的统计 */
    public static final String DATE = "yyyy-MM-dd";

    private DateTimePattern() {
    }
}
