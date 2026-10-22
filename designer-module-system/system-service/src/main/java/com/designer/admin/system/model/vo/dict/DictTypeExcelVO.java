package com.designer.admin.system.model.vo.dict;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 字典类型导入导出 Excel VO
 *
 * @author 雨夜终端
 */
@Data
public class DictTypeExcelVO {

    /** 字典名称 */
    @ExcelProperty("字典名称")
    private String dictName;

    /** 字典类型标识（唯一） */
    @ExcelProperty("字典类型")
    private String dictType;

    /** 状态：1正常 0停用，空值按启用处理 */
    @ExcelProperty("状态(1正常0停用)")
    private Integer status;

    /** 备注 */
    @ExcelProperty("备注")
    private String remark;
}
