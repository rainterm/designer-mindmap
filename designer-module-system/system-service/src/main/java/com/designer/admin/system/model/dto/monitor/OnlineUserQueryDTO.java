package com.designer.admin.system.model.dto.monitor;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 在线用户查询
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnlineUserQueryDTO extends PageQuery {

    @ApiModelProperty("登录账号，模糊匹配")
    private String username;

    @ApiModelProperty("登录 IP，模糊匹配")
    private String ip;
}
