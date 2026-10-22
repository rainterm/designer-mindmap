package com.designer.admin.module.agent.model.vo.mcp;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MCP 视图对象
 *
 * @author 雨夜终端
 */
@Data
public class McpVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("编码")
    private String code;

    @ApiModelProperty("传输方式：stdio / sse")
    private String transport;

    @ApiModelProperty("stdio 启动命令")
    private String command;

    @ApiModelProperty("stdio 启动参数")
    private String args;

    @ApiModelProperty("URL")
    private String url;

    @ApiModelProperty("环境变量 JSON")
    private String envJson;

    @ApiModelProperty("工具清单")
    private List<Map<String, Object>> tools;

    @ApiModelProperty("toolCount")
    private int toolCount;

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
