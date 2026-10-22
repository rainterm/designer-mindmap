package com.designer.admin.system.model.vo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量导入结果：成功写入的行数。
 *
 * <p>前端按 {@code data.success} 提示「成功导入 N 条」，字段名即接口契约，不能改。</p>
 *
 * @author 雨夜终端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultVO {

    /** 成功导入的行数 */
    private int success;
}
