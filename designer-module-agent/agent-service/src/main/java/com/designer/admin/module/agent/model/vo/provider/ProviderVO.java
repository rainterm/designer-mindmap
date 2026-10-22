package com.designer.admin.module.agent.model.vo.provider;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模型供应商视图对象
 *
 * @author 雨夜终端
 */
@Data
public class ProviderVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("类型")
    private String type;

    @ApiModelProperty("API Base URL")
    private String baseUrl;

    @ApiModelProperty("API Key（永不返回明文，固定空串）")
    private String apiKey = "";

    @ApiModelProperty("API Key 脱敏展示")
    private String apiKeyMasked;

    @ApiModelProperty("模型列表")
    private List<Map<String, Object>> models;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
