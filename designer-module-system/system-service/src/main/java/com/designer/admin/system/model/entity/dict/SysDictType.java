package com.designer.admin.system.model.entity.dict;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型（{@code dict_type} 全局唯一，作为数据字典的分组键）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    /** 字典名称（前端展示用） */
    private String dictName;

    /** 字典类型编码（全局唯一，业务取值使用） */
    private String dictType;

    /** 1正常 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}