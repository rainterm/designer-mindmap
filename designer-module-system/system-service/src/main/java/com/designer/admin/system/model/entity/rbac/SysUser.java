package com.designer.admin.system.model.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

import java.util.List;

/**
 * 系统用户（BCrypt 密码、逻辑删除，{@link SysUser#getPassword()} 经 {@code @ONodeAttr} 屏蔽序列化）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 登录账号（唯一） */
    private String username;

    /** BCrypt 密码，永不返回给前端 */
    @ONodeAttr(ignore = true)
    private String password;

    /** 昵称（前端展示用） */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 部门ID */
    private Long deptId;

    /** 1正常 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 拥有的角色ID，非数据库字段 */
    @TableField(exist = false)
    private List<Long> roleIds;

    /** 拥有的角色名称，非数据库字段 */
    @TableField(exist = false)
    private List<String> roleNames;

    /** 拥有的岗位ID，非数据库字段 */
    @TableField(exist = false)
    private List<Long> postIds;

    /** 拥有的岗位名称，非数据库字段 */
    @TableField(exist = false)
    private List<String> postNames;

    /** 部门名称，非数据库字段 */
    @TableField(exist = false)
    private String deptName;
}