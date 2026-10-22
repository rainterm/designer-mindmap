package com.designer.admin.module.demo.api.model;

import lombok.Builder;
import lombok.Data;

/**
 * Demo 模块连通性探测结果。
 *
 * <p>放在 demo-api 而非 service：跨模块调用方只依赖本模块的对外契约，
 * 返回类型若定义在实现模块里，调用方就会被倒逼依赖实现。</p>
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class DemoPingVO {

    /** 是否正常 */
    private boolean ok;

    /** 模块名 */
    private String module;
}
