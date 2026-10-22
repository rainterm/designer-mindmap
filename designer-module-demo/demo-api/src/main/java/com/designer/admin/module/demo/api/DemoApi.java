package com.designer.admin.module.demo.api;

import com.designer.admin.module.demo.api.model.DemoPingVO;

/**
 * Demo 对外 API（按需暴露；其他模块只依赖 demo-api）。
 *
 * @author 雨夜终端
 */
public interface DemoApi {

    /**
     * 连通性探测。
     *
     * @return 探测结果
     */
    DemoPingVO ping();
}
