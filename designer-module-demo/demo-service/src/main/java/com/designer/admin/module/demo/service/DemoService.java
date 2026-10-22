package com.designer.admin.module.demo.service;

import com.designer.admin.module.demo.api.model.DemoPingVO;

/**
 * Demo 模块内部服务（不对外暴露；跨模块请依赖 {@link com.designer.admin.module.demo.api.DemoApi}）。
 *
 * @author 雨夜终端
 */
public interface DemoService {

    /**
     * 模块自检。
     *
     * @return 包含 ok 与模块名的结果
     */
    DemoPingVO ping();
}
