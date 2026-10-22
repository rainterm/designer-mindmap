package com.designer.admin.common.constant;

/**
 * 数据权限范围（角色的 {@code sys_role.data_scope}）。
 *
 * <p><b>为什么用枚举而不是散落的 int 常量</b>：这组值同时被后端解析（{@link #fromCode}）
 * 与前端下拉使用。散着写时，「3 是本部门还是本部门及以下」只能靠注释对齐，
 * 改一次就要全仓搜。枚举把「编码 → 语义」固定在一处，非法编码在写入时就能被拒绝。</p>
 *
 * <p><b>文案不在这里</b>：中文名放在数据字典 {@code sys_data_scope} 里，
 * 后台「系统管理 → 数据字典」可改；枚举只负责编码与解析，不会再和前端各存一份文案。</p>
 *
 * <p><b>多角色取并集</b>：一个用户可能有多个角色，任一角色是 {@link #ALL} 即看全部；
 * 否则把各角色的部门集合合并，任一角色是 {@link #SELF} 则额外包含「本人」。
 * 这符合「角色是能力叠加」的直觉 —— 给用户加角色不应反而缩小可见范围。</p>
 *
 * @author 雨夜终端
 */
public enum DataScopeType {

    /** 全部数据 */
    ALL(1),
    /** 自定义部门：取 {@code sys_role_dept} 中勾选的部门 */
    CUSTOM(2),
    /** 仅本部门 */
    DEPT(3),
    /** 本部门及其所有下级部门 */
    DEPT_AND_CHILD(4),
    /** 仅本人 */
    SELF(5);

    private final int code;

    DataScopeType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 按编码解析。
     *
     * @param code 数据权限编码
     * @return 匹配的枚举；编码为空或不在 1~5 之间时返回 {@code null}
     */
    public static DataScopeType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataScopeType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
