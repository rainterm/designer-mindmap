package com.designer.admin.framework.storage.provider;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.spi.ConfigFieldMeta;
import com.designer.admin.framework.storage.FileStorageProvider;
import com.designer.admin.framework.storage.StorageProperties;
import com.designer.admin.framework.storage.StorageRuntimeConfig;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * 本地磁盘存储
 *
 * @author 雨夜终端
 */
@Component
public class LocalFileStorageProvider implements FileStorageProvider {

    public static final String TYPE = "local";

    /** URL 路径分隔符 */
    private static final String URL_SEPARATOR = "/";

    @Inject
    private StorageProperties storageProperties;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String label() {
        return "本地存储";
    }

    @Override
    public List<ConfigFieldMeta> configFields() {
        String localDefault = storageProperties.resolvedLocalDefaultPath();
        return List.of(
                ConfigFieldMeta.builder()
                        .name("localPath")
                        .label("本地目录")
                        .inputType("text")
                        .required(true)
                        .defaultValue(localDefault)
                        .placeholder(localDefault)
                        .help("相对 jar 启动目录或绝对路径")
                        .build()
        );
    }

    @Override
    public void validateConfig(Map<String, Object> props) {
        String path = str(props, "localPath");
        if (path.isBlank()) {
            throw new BizException("本地目录不能为空");
        }
    }

    @Override
    public void store(InputStream in, long size, String filePath, String contentType, StorageRuntimeConfig config) {
        Path target = resolve(filePath, config);
        try {
            Files.createDirectories(target.getParent());
            // 流由调用方关闭（门面可能传入压缩后的内存流）
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
    }

    @Override
    public void delete(String filePath, StorageRuntimeConfig config) {
        try {
            Files.deleteIfExists(resolve(filePath, config));
        } catch (Exception ignored) {
            // ignore
        }
    }

    @Override
    public InputStream open(String filePath, StorageRuntimeConfig config) throws Exception {
        Path target = resolve(filePath, config);
        if (!Files.isRegularFile(target)) {
            throw new BizException("文件不存在");
        }
        return Files.newInputStream(target);
    }

    @Override
    public String buildAccessUrl(String filePath, StorageRuntimeConfig config) {
        String prefix = config.getUrlPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = storageProperties.urlPrefix(config.isPublicAccess());
        }
        if (!prefix.endsWith(URL_SEPARATOR)) {
            prefix = prefix + URL_SEPARATOR;
        }
        return prefix + filePath;
    }

    private Path resolve(String filePath, StorageRuntimeConfig config) {
        Path root = Paths.get(config.prop("localPath", storageProperties.resolvedLocalDefaultPath()))
                .toAbsolutePath()
                .normalize();
        Path target = root.resolve(filePath).normalize();
        if (!target.startsWith(root)) {
            throw new BizException("非法的文件路径");
        }
        return target;
    }

    private static String str(Map<String, Object> props, String key) {
        if (props == null || props.get(key) == null) {
            return "";
        }
        return String.valueOf(props.get(key)).trim();
    }
}
