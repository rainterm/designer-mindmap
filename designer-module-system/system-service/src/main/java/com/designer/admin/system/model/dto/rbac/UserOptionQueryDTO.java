package com.designer.admin.system.model.dto.rbac;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员选项分页查询（账号/昵称模糊）。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserOptionQueryDTO extends PageQuery {

    @ApiModelProperty("账号或昵称关键字，模糊匹配")
    private String keyword;
}
