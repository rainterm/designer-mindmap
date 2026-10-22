package com.designer.admin.framework.storage;

import com.designer.admin.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收集所有 {@link FileStorageProvider}，按 type 索引，便于后续扩展新存储。
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class FileStorageProviderRegistry {

    @Inject
    private AppContext appContext;

    private Map<String, FileStorageProvider> providers = Collections.emptyMap();

    @Init
    public void init() {
        Map<String, FileStorageProvider> map = new LinkedHashMap<>();
        appContext.subBeansOfType(FileStorageProvider.class, p -> {
            FileStorageProvider old = map.put(p.type(), p);
            if (old != null && old != p) {
                throw new IllegalStateException("重复的存储类型：" + p.type());
            }
            log.info("注册文件存储 Provider：{} ({})", p.type(), p.label());
        });
        this.providers = Collections.unmodifiableMap(map);
        if (map.isEmpty()) {
            log.warn("未发现任何 FileStorageProvider 实现");
        }
    }

    public FileStorageProvider getRequired(String type) {
        FileStorageProvider p = providers.get(type);
        if (p == null) {
            throw new BizException("不支持的存储类型：" + type);
        }
        return p;
    }

    public boolean supports(String type) {
        return type != null && providers.containsKey(type);
    }

    public List<FileStorageProvider> list() {
        return new ArrayList<>(providers.values());
    }
}
