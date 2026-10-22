package com.designer.admin.system.model.dto.message;

import com.designer.admin.common.annotation.Sensitive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * 消息通道配置新增 / 修改入参。
 *
 * @author 雨夜终端
 */
@Data
public class MessageConfigSaveDTO {

    @ApiModelProperty("配置 ID，新增时留空")
    private Long id;

    @ApiModelProperty("配置名称")
    private String configName;

    @ApiModelProperty("通道类型，如 email / inbox")
    private String channelType;

    @ApiModelProperty("是否设为默认：0否 1是")
    private Integer isDefault;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("通道专属配置（含密钥，返回时整段脱敏）")
    @Sensitive
    private Map<String, Object> config;

    @ApiModelProperty("备注")
    private String remark;
}
