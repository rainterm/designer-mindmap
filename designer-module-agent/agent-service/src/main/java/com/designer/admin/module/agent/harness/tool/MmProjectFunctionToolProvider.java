package com.designer.admin.module.agent.harness.tool;

import com.designer.admin.module.agent.harness.context.MmProjectChatContext;
import com.designer.admin.module.mindmap.api.MmProjectApi;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.ai.chat.tool.FunctionTool;

import java.util.List;

/**
 * 思维导图落库 FunctionTool 与对应 system prompt 附录。
 *
 * @author 雨夜终端
 */
@Component
public class MmProjectFunctionToolProvider implements FunctionToolProvider {

    /** 思维导图项目 API */
    @Inject
    private MmProjectApi mmProjectApi;

    /**
     * 是否应挂载 mm_project_save：编辑页已绑定项目，或工具 code 含导图相关（含 Assembler 按 Skill 合成的 code）。
     *
     * @param ctx 构建上下文
     * @return true 表示应挂载
     */
    @Override
    public boolean supports(FunctionToolBuildContext ctx) {
        if (ctx == null || mmProjectApi == null) {
            return false;
        }
        Long bound = MmProjectChatContext.getProjectId();
        if (bound != null && bound > 0) {
            return true;
        }
        List<String> toolCodes = ctx.toolCodes();
        return toolCodes != null && toolCodes.stream().anyMatch(MmProjectFunctionTool::isMindmapToolCode);
    }

    /**
     * 构建统一的思维导图落库工具。
     *
     * @param ctx 构建上下文
     * @return 仅含一个 {@link MmProjectFunctionTool} 的列表
     */
    @Override
    public List<FunctionTool> build(FunctionToolBuildContext ctx) {
        return List.of(new MmProjectFunctionTool(mmProjectApi, ctx.onStatus()));
    }

    /**
     * 追加导图落库相关的 system prompt 约束（编辑页绑定 vs 新建）。
     *
     * @param ctx 构建上下文
     * @return 附录文案
     */
    @Override
    public String systemPromptAppendix(FunctionToolBuildContext ctx) {
        Long boundProjectId = MmProjectChatContext.getProjectId();
        boolean bound = boundProjectId != null && boundProjectId > 0;
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## 输出要求（必须遵守）\n");
        if (bound) {
            sb.append("- 当前在导图编辑页：本回合必须立刻调用 mm_project_save 覆盖更新当前导图。\n")
                    .append("- 禁止征求确认、禁止询问「是否写入」、禁止等待用户再回复后才落库。\n")
                    .append("- 无需传 projectId（已绑定），禁止新建另一张导图。\n")
                    .append("- 禁止在正文贴完整大纲或 HTML；大纲只放在工具参数 outline（Markdown # / ## / ###）。\n")
                    .append("- 工具成功后用一两句中文说明已更新即可。\n");
        } else {
            sb.append("- 用户要生成/扩展导图时，本回合必须调用 mm_project_save 落库（有 projectId 则更新，否则新建）。\n")
                    .append("- 禁止征求确认；禁止只输出大纲而不调用工具；禁止输出 HTML。\n")
                    .append("- 工具成功后用一两句中文说明，并提示可打开编辑页。\n");
        }
        return sb.toString();
    }
}
