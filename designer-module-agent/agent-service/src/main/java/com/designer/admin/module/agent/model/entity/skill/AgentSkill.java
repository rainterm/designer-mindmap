package com.designer.admin.module.agent.model.entity.skill;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Skill 实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_skill")
public class AgentSkill extends BaseEntity {

    /** 名称 */
    private String name;

    /** 编码 */
    private String code;

    /** 描述 */
    private String description;

    /** SKILL.md 内容 */
    private String content;

    /** 来源类型：manual / package */
    private String sourceType;

    /** 包名 */
    private String packageName;

    /** 包大小（字节） */
    private Long packageSize;

    /** 包内文件列表 JSON */
    private String filesJson;

    /** Skill 落盘路径 */
    private String skillPath;

    /** 状态：1启用 0停用 */
    private Integer status;
}
