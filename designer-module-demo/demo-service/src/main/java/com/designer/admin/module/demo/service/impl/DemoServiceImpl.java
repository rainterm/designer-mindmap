package com.designer.admin.module.demo.service.impl;

import com.designer.admin.module.demo.api.model.DemoPingVO;
import com.designer.admin.module.demo.service.DemoService;
import org.noear.solon.annotation.Component;

/**
 * Demo 模块内部服务（不对外暴露；跨模块请依赖 {@link com.designer.admin.module.demo.api.DemoApi}）。
 *
 * @author 雨夜终端
 */
@Component
public class DemoServiceImpl implements DemoService {

    @Override
    public DemoPingVO ping() {
        return DemoPingVO.builder()
                .ok(true)
                .module("demo")
                .build();
    }
}
