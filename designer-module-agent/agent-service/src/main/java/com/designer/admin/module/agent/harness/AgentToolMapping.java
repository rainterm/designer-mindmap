package com.designer.admin.module.agent.harness;

import org.noear.solon.ai.harness.agent.ToolName;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 管理端内置工具 code → Harness {@link ToolName} 映射。
 *
 * @author 雨夜终端
 */
public final class AgentToolMapping {

    private AgentToolMapping() {
    }

    public static List<ToolName> mapCodes(Iterable<String> codes) {
        Set<ToolName> set = new LinkedHashSet<>();
        if (codes == null) {
            return List.of();
        }
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            switch (code.trim().toLowerCase(Locale.ROOT)) {
                case "web_search":
                case "websearch":
                    set.add(ToolName.TOOL_WEBSEARCH);
                    break;
                case "http_request":
                case "webfetch":
                case "browser":
                    set.add(ToolName.TOOL_WEBFETCH);
                    break;
                case "filesystem":
                case "fs":
                    set.add(ToolName.TOOL_READ);
                    set.add(ToolName.TOOL_WRITE);
                    set.add(ToolName.TOOL_EDIT);
                    set.add(ToolName.TOOL_LS);
                    set.add(ToolName.TOOL_GREP);
                    set.add(ToolName.TOOL_GLOB);
                    break;
                default:
                    // 未知 code 忽略，避免拖垮对话
                    break;
            }
        }
        return new ArrayList<>(set);
    }
}
