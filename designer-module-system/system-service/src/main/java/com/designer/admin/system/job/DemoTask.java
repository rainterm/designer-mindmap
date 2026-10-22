package com.designer.admin.system.job;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

/**
 * 示例任务 Bean，页面 invoke_target 可填写：
 * <ul>
 *   <li>demoTask.noParams</li>
 *   <li>demoTask.withParams('hello')</li>
 * </ul>
 *
 * @author 雨夜终端
 */
@Component("demoTask")
@Slf4j
public class DemoTask {

    public void noParams() {
        log.info("[demoTask] 执行无参方法");
    }

    public void withParams(String params) {
        log.info("[demoTask] 执行有参方法：{}", params);
    }
}
