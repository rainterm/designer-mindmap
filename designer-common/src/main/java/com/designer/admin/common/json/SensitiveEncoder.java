package com.designer.admin.common.json;

import com.designer.admin.common.annotation.Sensitive;
import com.designer.admin.common.annotation.SensitiveType;
import org.noear.snack4.ONode;
import org.noear.snack4.codec.EncodeContext;
import org.noear.snack4.codec.ObjectEncoder;

/**
 * {@link Sensitive} 字段序列化脱敏。
 *
 * @author 雨夜终端
 */
public class SensitiveEncoder implements ObjectEncoder<Object> {

    /** 手机号最短可脱敏长度：至少能保留前 3 位与后 4 位 */
    private static final int MIN_MASKABLE_PHONE_LENGTH = 7;

    @Override
    public ONode encode(EncodeContext ctx, Object value, ONode target) {
        if (value == null) {
            return null;
        }
        Sensitive anno = ctx == null ? null : ctx.getAnnotation(Sensitive.class);
        SensitiveType type = anno == null ? SensitiveType.ALL : anno.type();
        String mask = (anno == null || anno.mask().isBlank()) ? "******" : anno.mask();
        String text = String.valueOf(value);
        return target.setValue(switch (type) {
            case PHONE -> maskPhone(text, mask);
            case EMAIL -> maskEmail(text, mask);
            case ALL -> mask;
        });
    }

    /** 11 位手机号保留前 3 后 4；过短则整段脱敏 */
    static String maskPhone(String phone, String fallback) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String s = phone.trim();
        if (s.length() < MIN_MASKABLE_PHONE_LENGTH) {
            return fallback;
        }
        return s.substring(0, 3) + "****" + s.substring(s.length() - 4);
    }

    /** 保留 @ 前首字符与域名；无 @ 则整段脱敏 */
    static String maskEmail(String email, String fallback) {
        if (email == null || email.isBlank()) {
            return email;
        }
        String s = email.trim();
        int at = s.indexOf('@');
        if (at <= 0 || at == s.length() - 1) {
            return fallback;
        }
        String name = s.substring(0, at);
        String domain = s.substring(at);
        char first = name.charAt(0);
        return first + "***" + domain;
    }
}
