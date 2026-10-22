package com.designer.admin.config;

import com.designer.admin.common.core.Result;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.docs.DocDocket;
import org.noear.solon.docs.models.ApiContact;
import org.noear.solon.docs.models.ApiInfo;
import org.noear.solon.docs.models.ApiLicense;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接口文档（OpenAPI2 + Knife4j）分组配置。
 *
 * <p>访问地址：{@code http://localhost:8080/doc.html}。每个分组按 Controller 所在包扫描，
 * 只有同时标注 {@code @Api} 与 {@code @ApiOperation} 的接口才会出现在文档中。</p>
 *
 * @author 雨夜终端
 */
@Configuration
public class DocConfig {

    /** 文档协议版本 */
    private static final String SWAGGER_VERSION = "2.0";

    /** 应用接口版本 */
    private static final String API_VERSION = "1.0.0";

    /** 管理端 Controller 包路径 */
    private static final String SYSTEM_ADMIN_PACKAGE = "com.designer.admin.system.controller.admin";

    /** 示例模块 Controller 包路径 */
    private static final String DEMO_ADMIN_PACKAGE = "com.designer.admin.module.demo.controller.admin";

    /** 智能体模块 Controller 包路径 */
    private static final String AGENT_ADMIN_PACKAGE = "com.designer.admin.module.agent.controller.admin";

    /** 前台 Controller 包路径 */
    private static final String SYSTEM_APP_PACKAGE = "com.designer.admin.system.controller.app";

    /** 智能体前台 / 公开嵌入 Controller 包路径 */
    private static final String AGENT_APP_PACKAGE = "com.designer.admin.module.agent.controller.app";

    /** 开放平台 Controller 包路径 */
    private static final String OPEN_PACKAGE = "com.designer.admin.system.controller.open";

    /** 智能体开放接口 Controller 包路径 */
    private static final String AGENT_OPEN_PACKAGE = "com.designer.admin.module.agent.controller.open";

    /**
     * 管理端接口分组：系统管理、示例模块、智能体模块。
     *
     * @return 管理端接口文档配置
     */
    @Bean("docDocketAdmin")
    public DocDocket docDocketAdmin() {
        return newDocDocket("01-管理端接口", "Designer Admin 管理端接口", "后台管理接口，除登录等白名单外均需登录并校验权限")
                .apis(SYSTEM_ADMIN_PACKAGE)
                .apis(DEMO_ADMIN_PACKAGE)
                .apis(AGENT_ADMIN_PACKAGE);
    }

    /**
     * 前台接口分组：面向 App 端与文件公开下载。
     *
     * @return 前台接口文档配置
     */
    @Bean("docDocketApp")
    public DocDocket docDocketApp() {
        return newDocDocket("02-前台接口", "Designer Admin 前台接口", "面向 App 端与匿名访客的公开接口")
                .apis(SYSTEM_APP_PACKAGE)
                .apis(AGENT_APP_PACKAGE);
    }

    /**
     * 开放接口分组：OAuth2 令牌端点与需 Scope 校验的开放接口。
     *
     * @return 开放接口文档配置
     */
    @Bean("docDocketOpen")
    public DocDocket docDocketOpen() {
        return newDocDocket("03-开放接口", "Designer Admin 开放接口", "供第三方系统调用的 OAuth2 与 OpenAPI 接口")
                .apis(OPEN_PACKAGE)
                .apis(AGENT_OPEN_PACKAGE);
    }

    /**
     * 构造统一元信息的分组文档配置。
     *
     * @param groupName   分组名称（显示在 Knife4j 左侧）
     * @param title       文档标题
     * @param description 文档描述
     * @return 文档配置
     */
    private DocDocket newDocDocket(String groupName, String title, String description) {
        return new DocDocket()
                .groupName(groupName)
                .version(SWAGGER_VERSION)
                .info(new ApiInfo()
                        .title(title)
                        .description(description)
                        .version(API_VERSION)
                        .contact(new ApiContact().name("雨夜终端"))
                        .license(new ApiLicense().name("MIT")))
                .globalResult(Result.class)
                .globalResponseInData(true)
                .globalResponseCodes(globalResponseCodes());
    }

    /**
     * 统一响应状态码说明。
     *
     * @return 状态码与说明的映射
     */
    private Map<Integer, String> globalResponseCodes() {
        Map<Integer, String> codes = new LinkedHashMap<>();
        codes.put(200, "请求成功");
        codes.put(400, "参数校验失败");
        codes.put(401, "未登录或登录已过期");
        codes.put(403, "无访问权限");
        codes.put(500, "服务器内部错误");
        return codes;
    }
}
