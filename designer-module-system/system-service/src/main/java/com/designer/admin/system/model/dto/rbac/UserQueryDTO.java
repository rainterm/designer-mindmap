package com.designer.admin.system.model.dto.rbac;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询条件
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQuery {

    @ApiModelProperty("登录账号，模糊匹配")
    private String username;

    @ApiModelProperty("昵称，模糊匹配")
    private String nickname;

    @ApiModelProperty("手机号，模糊匹配")
    private String phone;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;

    @ApiModelProperty("按已分配的角色筛选")
    private Long roleId;

    @ApiModelProperty("按部门筛选，含下级部门时由服务端展开")
    private Long deptId;

    @ApiModelProperty("创建时间起，格式 yyyy-MM-dd")
    private String beginTime;

    @ApiModelProperty("创建时间止，格式 yyyy-MM-dd")
    private String endTime;
}
