package com.designer.admin.module.agent.harness.tool;

import com.designer.admin.module.agent.harness.context.MmProjectChatContext;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import com.designer.admin.module.mindmap.api.MmProjectApi;
import com.designer.admin.module.mindmap.api.model.MmProjectBriefVO;
import org.noear.solon.ai.chat.tool.FunctionTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 思维导图落库工具：有 projectId（或编辑页绑定）则更新，否则新建。
 *
 * @author 雨夜终端
 */
public class MmProjectFunctionTool implements FunctionTool {

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(MmProjectFunctionTool.class);

    /** 统一工具 code */
    public static final String CODE = "mm_project_save";

    /** 配套 Skill code（绑定后应同步挂载 {@link #CODE}） */
    public static final String SKILL_CODE = "mindmap-outline";

    /** 历史别名（创建），绑定后仍识别并挂载为统一工具 */
    public static final String CODE_CREATE_LEGACY = "mm_project_create";
    /** 历史别名（更新），绑定后仍识别并挂载为统一工具 */
    public static final String CODE_UPDATE_LEGACY = "mm_project_update";

    /** 当前与历史全部识别的 code 集合 */
    private static final Set<String> ALL_CODES = Set.of(CODE, CODE_CREATE_LEGACY, CODE_UPDATE_LEGACY);

    /** 思维导图项目 API */
    private final MmProjectApi projectApi;
    /** 过程状态回调；可为 null */
    private final Consumer<String> onStatus;

    /**
     * @param projectApi 思维导图项目 API
     * @param onStatus   过程状态回调；可为 null
     */
    public MmProjectFunctionTool(MmProjectApi projectApi, Consumer<String> onStatus) {
        this.projectApi = projectApi;
        this.onStatus = onStatus;
    }

    /**
     * 是否为思维导图落库相关工具 code（含历史别名）。
     *
     * @param code 工具 code
     * @return true 表示应挂载本工具
     */
    public static boolean isMindmapToolCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return ALL_CODES.contains(code.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 统一工具名 {@link #CODE}。
     *
     * @return {@code mm_project_save}
     */
    @Override
    public String name() {
        return CODE;
    }

    /**
     * 工具展示标题。
     *
     * @return 中文标题
     */
    @Override
    public String title() {
        return "保存思维导图";
    }

    /**
     * 按是否绑定编辑页项目，生成差异化调用说明。
     *
     * @return 供模型阅读的描述
     */
    @Override
    public String description() {
        Long bound = MmProjectChatContext.getProjectId();
        if (bound != null && bound > 0) {
            return "保存思维导图到当前编辑项目（覆盖更新）。"
                    + "参数：outline（必填，# / ## / ### 大纲）、name（可选）。"
                    + "已绑定 projectId=" + bound + "，无需再传。";
        }
        return "按 Markdown 标题大纲保存思维导图：传入 projectId 则覆盖更新，否则新建。"
                + "参数：outline（必填）、name（新建时必填 / 更新时可选）、"
                + "projectId（可选）、categoryId（新建时可选）。";
    }

    /**
     * 按是否绑定编辑页项目，生成差异化 JSON Schema。
     *
     * @return inputSchema JSON 字符串
     */
    @Override
    public String inputSchema() {
        Long bound = MmProjectChatContext.getProjectId();
        if (bound != null && bound > 0) {
            return """
                    {"type":"object","properties":{\
                    "outline":{"type":"string","description":"Markdown 标题大纲"},\
                    "name":{"type":"string","description":"可选新名称"}\
                    },"required":["outline"]}""";
        }
        return """
                {"type":"object","properties":{\
                "outline":{"type":"string","description":"Markdown 标题大纲"},\
                "name":{"type":"string","description":"导图名称（新建必填）"},\
                "projectId":{"type":"string","description":"已有导图 ID，传入则更新"},\
                "categoryId":{"type":"string","description":"新建时可选分类 ID"}\
                },"required":["outline"]}""";
    }

    /**
     * 结果回传给模型再组织回答。
     *
     * @return 恒为 false
     */
    @Override
    public boolean returnDirect() {
        return false;
    }

    /**
     * 工具返回值类型。
     *
     * @return {@link String}
     */
    @Override
    public Type returnType() {
        return String.class;
    }

    /**
     * 按大纲新建或更新思维导图，返回 projectId / name / editPath JSON。
     *
     * @param args 模型入参（outline / name / projectId / categoryId）
     * @return 成功摘要 JSON 或失败 JSON
     */
    @Override
    public Object handle(Map<String, Object> args) {
        try {
            String outline = str(args, "outline");
            if (outline == null || outline.isBlank()) {
                return fail("outline 不能为空");
            }
            Long projectId = longVal(args, "projectId");
            if (projectId == null) {
                projectId = MmProjectChatContext.getProjectId();
            }
            MmProjectBriefVO brief;
            if (projectId != null && projectId > 0) {
                emitStatus("正在更新思维导图…");
                brief = projectApi.updateFromOutline(projectId, outline, str(args, "name"));
            } else {
                String name = str(args, "name");
                if (name == null || name.isBlank()) {
                    return fail("新建时 name 不能为空");
                }
                emitStatus("正在创建思维导图…");
                brief = projectApi.createFromOutline(name, outline, longVal(args, "categoryId"));
            }
            return AgentJsonUtil.toJson(Map.of(
                    "projectId", brief.getId(),
                    "name", brief.getName() == null ? "" : brief.getName(),
                    "editPath", brief.getEditPath() == null ? "" : brief.getEditPath()
            ));
        } catch (Exception e) {
            log.warn("思维导图工具失败", e);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return fail(msg);
        }
    }

    /**
     * 向对话侧推送过程状态文案。
     *
     * @param status 状态文案
     */
    private void emitStatus(String status) {
        if (onStatus != null && status != null && !status.isBlank()) {
            onStatus.accept(status);
        }
    }

    /**
     * 构造失败 JSON。
     *
     * @param msg 错误信息
     * @return {@code {"ok":false,"error":...}}
     */
    private static String fail(String msg) {
        return AgentJsonUtil.toJson(Map.of("ok", false, "error", msg == null ? "失败" : msg));
    }

    /**
     * 从入参 Map 读取字符串并 trim。
     *
     * @param args 入参
     * @param key  键
     * @return 字符串；缺失则为 null
     */
    private static String str(Map<String, Object> args, String key) {
        if (args == null || key == null) {
            return null;
        }
        Object v = args.get(key);
        return v == null ? null : String.valueOf(v).trim();
    }

    /**
     * 从入参 Map 读取 Long；非法数字返回 null。
     *
     * @param args 入参
     * @param key  键
     * @return Long；缺失或非法则为 null
     */
    private static Long longVal(Map<String, Object> args, String key) {
        String s = str(args, key);
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
