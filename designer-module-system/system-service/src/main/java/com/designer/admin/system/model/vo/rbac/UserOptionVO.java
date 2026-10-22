package com.designer.admin.system.model.vo.rbac;

import lombok.Data;

/**
 * 人员选择弹框/下拉用：仅暴露选人所需字段，不含手机、邮箱、角色等敏感信息。
 *
 * @author 雨夜终端
 */
@Data
public class UserOptionVO {

    /** 用户ID */
    private Long id;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 部门名称 */
    private String deptName;
}
