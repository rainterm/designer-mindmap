package com.designer.admin.system.mapper.rbac;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.rbac.SysDept;

import java.util.List;

/**
 * 部门 Mapper
 *
 * @author 雨夜终端
 */
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询「本部门 + 全部下级部门」的 ID。
     *
     * <p>部门树用 {@code ancestors}（如 {@code 0,100,101}）存祖先路径，
     * 因此下级判定就是「祖先路径里出现过我」。两种形态都要匹配：</p>
     * <ul>
     *   <li>{@code ancestors LIKE %,101,%} —— 我是中间节点，子孙的路径里我在中间；</li>
     *   <li>{@code ancestors LIKE %,101} —— 我是路径末尾，即我的直接子部门。</li>
     * </ul>
     *
     * <p><b>为什么不用 {@code FIND_IN_SET}</b>：那是 MySQL 方言，
     * 项目里的分页插件刻意按 JDBC URL 识别方言以支持换库，这里再写死一个方言就没意义了。
     * {@code LIKE} 是标准 SQL，配合参数绑定不存在拼接风险。</p>
     *
     * <p>停用部门也返回：历史数据仍挂在这些部门下，看得见部门不会额外暴露数据，
     * 但漏掉它会让「本部门及以下」少算一整棵子树。</p>
     *
     * @param deptId 部门 ID
     * @return 部门自身 + 全部下级部门 ID
     */
    default List<Long> selectSelfAndDescendantIds(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        String inMiddle = "%," + deptId + ",%";
        String atTail = "%," + deptId;
        return selectList(new LambdaQueryWrapper<SysDept>()
                .select(SysDept::getId)
                .and(w -> w.eq(SysDept::getId, deptId)
                        .or()
                        .apply("ancestors LIKE {0}", inMiddle)
                        .or()
                        .apply("ancestors LIKE {0}", atTail)))
                .stream()
                .map(SysDept::getId)
                .toList();
    }
}
