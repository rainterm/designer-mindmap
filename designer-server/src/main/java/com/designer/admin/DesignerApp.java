package com.designer.admin;

import com.designer.admin.common.json.SensitiveSupport;
import org.noear.solon.Solon;

/**
 * Designer Admin 服务启动类
 *
 * @author 雨夜终端
 */
public class DesignerApp {

    public static void main(String[] args) {
        // 必须在 Solon 首次 JSON 反序列化之前注册，否则 ClassEggg 会缓存无脱敏元数据
        SensitiveSupport.register();
        Solon.start(DesignerApp.class, args);
        System.out.println("""
           
                ------------------------------------------------------
                  Designer Admin Server started
                  Admin API : http://localhost:8080/admin-api
                  App API   : http://localhost:8080/app-api
                  Health    : http://localhost:8080/admin-api/system/health
                  API Docs  : http://localhost:8080/doc.html
                ------------------------------------------------------
                """);
    }
}
