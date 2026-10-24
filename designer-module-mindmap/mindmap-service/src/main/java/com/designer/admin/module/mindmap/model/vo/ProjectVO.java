package com.designer.admin.module.mindmap.model.vo;

import com.designer.admin.common.core.DateTimePattern;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 导图项目视图。
 *
 * @author 雨夜终端
 */
@Data
public class ProjectVO {

    @ApiModelProperty("项目 ID")
    private Long id;

    @ApiModelProperty("分类 ID，空表示未分类")
    private Long categoryId;

    @ApiModelProperty("分类名称")
    private String categoryName;

    @ApiModelProperty("项目名称")
    private String name;

    @ApiModelProperty("布局标识")
    private String layout;

    @ApiModelProperty("主题标识")
    private String theme;

    @ApiModelProperty("导图内容 JSON；列表接口不返回")
    private String content;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建时间")
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime updateTime;
}
