package com.designer.admin.system.constant;

/**
 * 消息中心（{@code sys_message_config} / {@code sys_message_record}）的固定值。
 *
 * <p><b>为什么脱敏占位符要集中</b>：回显时把密钥替换成占位符、保存时把占位符识别为「未修改」，
 * 是同一个字符串的两面。两处各写一份，只要有一边拼错，就会把占位符当成新密钥存进库 ——
 * 表现为「改完配置后密钥变成六个星号」，而且没有任何报错。</p>
 *
 * @author 雨夜终端
 */
public final class SysMessageConsts {

    /** 敏感字段回显时的脱敏占位符；保存时出现该值表示「保持原值」 */
    public static final String SECRET_MASK = "******";

    /** 消息正文最大长度，超出部分截断后入库 */
    public static final int MAX_CONTENT_LENGTH = 2000;

    private SysMessageConsts() {
    }
}
