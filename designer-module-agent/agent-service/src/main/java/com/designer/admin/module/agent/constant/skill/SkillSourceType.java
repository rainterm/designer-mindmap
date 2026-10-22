package com.designer.admin.module.agent.constant.skill;

/**
 * Skill 来源类型。
 *
 * @author 雨夜终端
 */
public enum SkillSourceType {

    /** 手工编辑 */
    MANUAL("manual"),
    /** 压缩包导入 */
    PACKAGE("package");

    private final String code;

    SkillSourceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean eq(String value) {
        return code.equals(value);
    }
}
