package com.designer.admin.module.agent.model.vo.tool;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 工具视图对象
 *
 * @author 雨夜终端
 */
@Data
public class ToolVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("编码")
    private String code;

    @ApiModelProperty("类型：builtin")
    private String type;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("参数 schema JSON")
    private String schemaJson;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
