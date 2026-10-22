package com.designer.admin.framework.storage;

import lombok.Data;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * 文件存储全局默认项（来自 app.yml {@code designer.storage.*}）
 *
 * @author 雨夜终端
 */
@Data
@Configuration
public class StorageProperties {

    /**
     * 私有文件代理下载前缀（需登录），须与 {@code /admin-api} 下载接口映射一致。
     * 例：{@code /admin-api/system/file/download/}
     */
    @Inject("${designer.storage.default-url-prefix}")
    private String defaultUrlPrefix;

    /**
     * 公开文件代理下载前缀（免登录），须与 {@code /app-api} 下载接口映射一致。
     * 例：{@code /app-api/system/file/download/}
     */
    @Inject("${designer.storage.public-url-prefix}")
    private String publicUrlPrefix;

    /**
     * 本地存储默认根目录。
     * 例：{@code ./data/uploads}
     */
    @Inject("${designer.storage.local-default-path}")
    private String localDefaultPath;

    /** 规范化后的私有下载前缀（保证以 / 结尾） */
    public String normalizedPrivateUrlPrefix() {
        return normalize(defaultUrlPrefix, "designer.storage.default-url-prefix");
    }

    /** 规范化后的公开下载前缀（保证以 / 结尾） */
    public String normalizedPublicUrlPrefix() {
        return normalize(publicUrlPrefix, "designer.storage.public-url-prefix");
    }

    /** 按访问类型取下载前缀 */
    public String urlPrefix(boolean publicAccess) {
        return publicAccess ? normalizedPublicUrlPrefix() : normalizedPrivateUrlPrefix();
    }

    public String resolvedLocalDefaultPath() {
        String p = localDefaultPath == null ? "" : localDefaultPath.trim();
        if (p.isEmpty()) {
            throw new IllegalStateException("缺少配置 designer.storage.local-default-path");
        }
        return p;
    }

    private static String normalize(String prefix, String key) {
        String p = prefix == null ? "" : prefix.trim();
        if (p.isEmpty()) {
            throw new IllegalStateException("缺少配置 " + key);
        }
        return p.endsWith("/") ? p : p + "/";
    }
}
