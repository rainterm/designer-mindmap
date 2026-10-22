package com.designer.admin.system.constant;

/**
 * 文件服务（上传路径校验 / 存储配置）的固定值。
 *
 * @author 雨夜终端
 */
public final class SysFileConsts {

    /** 虚拟路径的最大长度，超出直接拒绝（防止构造超长路径绕过校验） */
    public static final int MAX_PATH_LENGTH = 512;

    /** 上级目录标记：出现在相对路径中即视为路径穿越 */
    public static final String PARENT_DIR = "..";

    /** 敏感字段回显时的脱敏占位符；保存时出现该值表示「保持原值」 */
    public static final String SECRET_MASK = "******";

    private SysFileConsts() {
    }
}
