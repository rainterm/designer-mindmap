package com.designer.admin.common.core;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体公共字段：主键、创建人/时间、更新人/时间、逻辑删除标记
 *
 * <p>主键统一为 {@code Long} 自增，{@code id} 只在此声明一次；子类不要再重复定义，
 * 否则 Lombok 会在子类生成同名 getter/setter 遮蔽父类字段，MyBatis-Plus 也只会认一处。</p>
 *
 * <p>创建/更新人、时间由 MyBatis-Plus 自动填充，见 {@code AutoFillHandler}。</p>
 *
 * <p>逻辑删除标记 {@code deleted} 由 {@code @TableLogic} 生效：删除语句会被改写成
 * {@code UPDATE ... SET deleted = 1}，普通查询自动追加 {@code deleted = 0}。
 * 凡继承本类的实体，对应表都必须有 {@code deleted} 列。</p>
 *
 * <p>关联表（如 {@code sys_user_role}）与流水表（如 {@code sys_oper_log}）不继承本类：
 * 前者随主表整批重写、后者按保留期物理清理，软删只会让数据无限堆积。</p>
 *
 * @author 雨夜终端
 */
@Data
public class BaseEntity implements Serializable {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建人账号 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人账号 */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0 正常 1 已删除，不返回前端 */
    @TableLogic
    @ONodeAttr(ignore = true)
    private Integer deleted;
}
