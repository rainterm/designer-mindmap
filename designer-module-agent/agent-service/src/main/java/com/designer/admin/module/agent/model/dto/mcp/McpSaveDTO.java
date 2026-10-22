package com.designer.admin.module.agent.model.dto.mcp;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * MCP 保存参数
 *
 * @author 雨夜终端
 */
@Data
public class McpSaveDTO {

    @ApiModelProperty("主键 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "名称", required = true)
    @NotBlank(message = "MCP 名称不能为空")
    @Length(max = 100, message = "MCP 名称长度不能超过 100 个字符")
    private String name;

    @ApiModelProperty(value = "编码", required = true)
    @NotBlank(message = "MCP 编码不能为空")
    @Length(max = 64, message = "MCP 编码长度不能超过 64 个字符")
    private String code;

    @ApiModelProperty("传输方式：stdio / sse")
    @Length(max = 32, message = "传输方式长度不能超过 32 个字符")
    private String transport;

    @ApiModelProperty("stdio 启动命令")
    @Length(max = 500, message = "启动命令长度不能超过 500 个字符")
    private String command;

    @ApiModelProperty("stdio 启动参数")
    @Length(max = 2000, message = "启动参数长度不能超过 2000 个字符")
    private String args;

    @ApiModelProperty("SSE 服务地址")
    @Length(max = 500, message = "SSE 地址长度不能超过 500 个字符")
    private String url;

    @ApiModelProperty("环境变量 JSON")
    private String envJson;

    @ApiModelProperty("工具清单")
    private List<Map<String, Object>> tools;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    @Length(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;
}
