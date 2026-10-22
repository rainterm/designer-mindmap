package com.designer.admin.system.util;

import com.designer.admin.framework.spi.ConfigFieldMeta;
import com.designer.admin.system.model.vo.config.ConfigFieldVO;

/**
 * Provider 字段元数据 → 前端表单 VO 的转换。
 *
 * <p>消息通道与文件存储的 Provider 都返回 {@link ConfigFieldMeta}，
 * 两个模块此前各写了一份逐字段 copy 的 {@code toConfigField}，加字段时容易漏。
 * 转换集中在这里，VO 本身仍不依赖 framework 类型。</p>
 *
 * @author 雨夜终端
 */
public final class ConfigFieldConverter {

    private ConfigFieldConverter() {
    }

    public static ConfigFieldVO toVO(ConfigFieldMeta field) {
        if (field == null) {
            return null;
        }
        return ConfigFieldVO.builder()
                .name(field.getName())
                .label(field.getLabel())
                .inputType(field.getInputType())
                .required(field.isRequired())
                .placeholder(field.getPlaceholder())
                .defaultValue(field.getDefaultValue())
                .help(field.getHelp())
                .build();
    }
}
