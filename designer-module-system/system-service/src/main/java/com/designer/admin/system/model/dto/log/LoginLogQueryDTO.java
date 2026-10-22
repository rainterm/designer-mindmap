package com.designer.admin.system.model.dto.log;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录日志分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQueryDTO extends PageQuery {

    @ApiModelProperty("登录账号，模糊匹配")
    private String username;

    @ApiModelProperty("状态：1成功 0失败，为空表示不限")
    private Integer status;
}
