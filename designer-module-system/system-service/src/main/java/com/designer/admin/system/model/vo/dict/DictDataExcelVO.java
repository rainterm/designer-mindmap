package com.designer.admin.system.model.vo.dict;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 字典数据导入导出 Excel VO
 *
 * @author 雨夜终端
 */
@Data
public class DictDataExcelVO {

    /** 所属字典类型标识，需对应已有字典类型 */
    @ExcelProperty("字典类型")
    private String dictType;

    /** 字典标签（展示文案） */
    @ExcelProperty("字典标签")
    private String dictLabel;

    /** 字典键值（实际存储值） */
    @ExcelProperty("字典键值")
    private String dictValue;

    /** 显示顺序，越小越靠前 */
    @ExcelProperty("排序")
    private Integer sort;

    /** 是否默认：1是 0否，空值按否处理 */
    @ExcelProperty("是否默认(1是0否)")
    private Integer isDefault;

    /** 状态：1正常 0停用，空值按启用处理 */
    @ExcelProperty("状态(1正常0停用)")
    private Integer status;

    /** 备注 */
    @ExcelProperty("备注")
    private String remark;
}
