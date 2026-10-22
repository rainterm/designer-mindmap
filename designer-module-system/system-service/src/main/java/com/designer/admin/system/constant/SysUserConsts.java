package com.designer.admin.system.constant;

/**
 * 用户模块的固定值。
 *
 * @author 雨夜终端
 */
public final class SysUserConsts {

    /** 导出单次最多携带的用户行数：超过只导出前 N 条，避免一次导出拖垮内存 */
    public static final int EXPORT_LIMIT = 10000;

    private SysUserConsts() {
    }
}
