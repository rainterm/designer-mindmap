package com.designer.admin.system.mapper.rbac;

import com.designer.admin.system.model.entity.rbac.SysPost;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

/**
 * 岗位数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysPostMapper extends MPJBaseMapper<SysPost> {

    /**
     * 统计岗位编码占用数（只统计未删除的岗位）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的岗位不再占位。</p>
     *
     * @param postCode  岗位编码
     * @param excludeId 需要排除的岗位 ID，新增时传 {@code null}
     * @return 占用该编码的岗位数
     */
    default Long countByPostCode(String postCode, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysPost.class)
                .eq(SysPost::getPostCode, postCode)
                .ne(excludeId != null, SysPost::getId, excludeId));
    }
}
