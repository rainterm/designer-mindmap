package com.designer.admin.module.agent.model.dto.session;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话工具调用
 *
 * @author 雨夜终端
 */
@Data
public class ChatToolCallDTO {

    @ApiModelProperty("工具调用 ID")
    private String callId;

    @ApiModelProperty("start | end")
    private String phase;

    @ApiModelProperty("工具名称")
    private String toolName;

    @ApiModelProperty("工具展示名")
    private String toolLabel;

    @ApiModelProperty("工具调用参数")
    private Map<String, Object> args = new LinkedHashMap<>();

    @ApiModelProperty("工具执行结果")
    private String result;

    @ApiModelProperty("错误信息")
    private String error;

    @ApiModelProperty("耗时（毫秒）")
    private Long durationMs;

    @ApiModelProperty("从结果中提取的链接（如网页搜索）")
    private List<String> links = new ArrayList<>();
}
