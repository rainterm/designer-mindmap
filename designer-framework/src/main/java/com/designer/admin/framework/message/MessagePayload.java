package com.designer.admin.framework.message;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 发送消息的统一载荷（各通道按需取字段）
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class MessagePayload {

    /** 接收人：邮箱 / 手机号 / openId 等 */
    private List<String> to;

    /** 抄送（邮件等） */
    private List<String> cc;

    /** 标题 / 主题 */
    private String title;

    /** 正文 */
    private String content;

    /** 是否 HTML（邮件） */
    @Builder.Default
    private boolean html = false;

    /** 扩展参数（模板变量、钉钉机器人签名等） */
    @Builder.Default
    private Map<String, Object> extras = Collections.emptyMap();
}
