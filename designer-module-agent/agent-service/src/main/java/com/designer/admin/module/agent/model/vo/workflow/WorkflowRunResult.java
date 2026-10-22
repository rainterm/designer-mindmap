package com.designer.admin.module.agent.model.vo.workflow;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行结果
 *
 * @author 雨夜终端
 */
@Data
public class WorkflowRunResult {

    @ApiModelProperty("workflowId")
    private Long workflowId;

    @ApiModelProperty("workflowName")
    private String workflowName;

    @ApiModelProperty("运行输出")
    private String output;

    @ApiModelProperty("执行步骤")
    private List<Step> steps = new ArrayList<>();

    /**
     * 执行步骤
     *
     * @author 雨夜终端
     */
    @Data
    public static class Step {

        @ApiModelProperty("nodeKey")
        private String nodeKey;

        @ApiModelProperty("nodeType")
        private String nodeType;

        @ApiModelProperty("nodeName")
        private String nodeName;

        @ApiModelProperty("摘要说明")
        private String detail;

        @ApiModelProperty("true")
        private boolean ok = true;

        @ApiModelProperty("耗时毫秒")
        private long durationMs;

        @ApiModelProperty("节点执行前的输入变量快照")
        private Map<String, String> input = new LinkedHashMap<>();

        @ApiModelProperty("节点过程参数（配置/解析后的关键字段）")
        private Map<String, Object> params = new LinkedHashMap<>();

        @ApiModelProperty("节点输出（写入 output / 自定义变量的内容）")
        private String output;

        @ApiModelProperty("模型思考过程（LLM 开启输出思考时）")
        private String thinking;
    }
}
