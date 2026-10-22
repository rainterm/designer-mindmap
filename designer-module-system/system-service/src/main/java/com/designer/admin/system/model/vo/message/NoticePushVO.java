package com.designer.admin.system.model.vo.message;

import lombok.Builder;
import lombok.Data;

/**
 * 站内消息 SSE 推送载荷（事件名 {@code notice}）。
 *
 * <p>前端消息中心直接消费该结构；字段名即接口契约，不能改。</p>
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class NoticePushVO {

    /** 消息 ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 正文摘要（超长截断） */
    private String content;

    /** 创建时间（yyyy-MM-dd HH:mm:ss），缺失时为空串 */
    private String createTime;

    /** 点击后的站内跳转地址；为空时不写入 JSON（snack4 默认不输出 null 字段） */
    private String link;
}
