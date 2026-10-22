package com.designer.admin.module.agent.util;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.common.exception.BizException;

import java.util.Collection;

/**
 * 实体查询 / 删除等 CRUD 公共逻辑。
 *
 * @author 雨夜终端
 */
public final class AgentEntitySupport {

    private AgentEntitySupport() {
    }

    public static <T> T require(T row, String notFoundMessage) {
        if (row == null) {
            throw new BizException(notFoundMessage);
        }
        return row;
    }

    public static void assertCodeUnique(Long count, String duplicatedMessage) {
        if (count != null && count > 0) {
            throw new BizException(duplicatedMessage);
        }
    }

    public static void deleteBatchIds(BaseMapper<?> mapper, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        mapper.deleteBatchIds(ids);
    }

    /** 草稿版本号递增（null 视为 1）。 */
    public static int bumpDraftVersion(Integer draftVersion) {
        return (draftVersion == null ? 1 : draftVersion) + 1;
    }

    /** 发布版本号递增（null 视为 0）。 */
    public static int nextPublishVersion(Integer version) {
        return (version == null ? 0 : version) + 1;
    }
}
