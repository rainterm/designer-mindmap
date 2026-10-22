package com.designer.admin.system.mapper.dict;

import com.designer.admin.system.model.entity.dict.SysDictType;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典类型数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysDictTypeMapper extends MPJBaseMapper<SysDictType> {

    /**
     * 统计字典类型标识占用数（只统计未删除的记录）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的标识不再占位。</p>
     *
     * @param dictType  字典类型标识
     * @param excludeId 需要排除的字典类型 ID，新增时传 {@code null}
     * @return 占用该标识的记录数
     */
    default Long countByDictType(String dictType, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysDictType.class)
                .eq(SysDictType::getDictType, dictType)
                .ne(excludeId != null, SysDictType::getId, excludeId));
    }
}
