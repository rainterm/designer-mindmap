package com.designer.admin.system.model.entity.dict;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据项（隶属于某个 {@link SysDictType#getDictType()} 字典类型）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    /** 字典类型编码（关联 sys_dict_type.dict_type） */
    private String dictType;

    /** 字典标签（前端下拉/单选展示文本） */
    private String dictLabel;

    /** 字典值（实际业务值） */
    private String dictValue;

    /** 同类型下的显示顺序，升序 */
    private Integer sort;

    /** 是否默认值：1是 0否 */
    private Integer isDefault;

    /** 1正常 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}