package com.designer.admin.system.model.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户 - 岗位关联表（联合主键：user_id + post_id）
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_user_post")
public class SysUserPost implements Serializable {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 岗位ID */
    @TableField("post_id")
    private Long postId;
}
