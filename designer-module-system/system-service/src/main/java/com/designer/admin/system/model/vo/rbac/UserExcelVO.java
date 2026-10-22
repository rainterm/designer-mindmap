package com.designer.admin.system.model.vo.rbac;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 用户导入导出 Excel VO
 *
 * @author 雨夜终端
 */
@Data
public class UserExcelVO {

    /** 登录账号（导入时必填） */
    @ExcelProperty("登录账号")
    private String username;

    /** 昵称 */
    @ExcelProperty("昵称")
    private String nickname;

    /** 邮箱 */
    @ExcelProperty("邮箱")
    private String email;

    /** 手机号 */
    @ExcelProperty("手机号")
    private String phone;

    /** 部门ID，需对应已有部门 */
    @ExcelProperty("部门ID")
    private Long deptId;

    /** 状态：1正常 0停用，空值按启用处理 */
    @ExcelProperty("状态(1正常0停用)")
    private Integer status;

    /** 备注 */
    @ExcelProperty("备注")
    private String remark;

    /** 仅导入使用；导出为空。**导入时必填**，不提供默认密码 */
    @ExcelProperty("初始密码")
    private String password;
}
