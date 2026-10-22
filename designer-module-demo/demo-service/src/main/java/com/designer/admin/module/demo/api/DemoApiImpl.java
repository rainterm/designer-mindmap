package com.designer.admin.module.demo.api;

import com.designer.admin.module.demo.api.model.DemoPingVO;
import com.designer.admin.module.demo.service.DemoService;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * {@link DemoApi} 实现，位于 service 子模块的 api 包。
 *
 * @author 雨夜终端
 */
@Component
public class DemoApiImpl implements DemoApi {

    @Inject
    private DemoService demoService;

    @Override
    public DemoPingVO ping() {
        return demoService.ping();
    }
}
