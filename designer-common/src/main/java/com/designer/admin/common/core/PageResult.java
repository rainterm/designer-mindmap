package com.designer.admin.common.core;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页返回结构
 *
 * @author 雨夜终端
 */
@Data
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long pageNum;

    /** 每页条数 */
    private long pageSize;

    /** 当前页数据 */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, long pageNum, long pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records == null ? Collections.emptyList() : records;
    }

    /** 由 MyBatis-Plus 分页对象构建 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
