package com.designer.admin.system.model.vo.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全部标记已读的结果。
 *
 * @author 雨夜终端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeReadCountVO {

    /** 本次标记为已读的条数 */
    private int count;
}
