package com.designer.admin.module.mindmap.util;

import org.noear.snack4.ONode;
import org.noear.solon.Utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 Markdown 标题大纲转为 simple-mind-map 节点树 JSON。
 *
 * <p>规则：以 {@code #}～{@code ######} 标题建树；无标题时把首行非空文本作为根，其余行作一级子节点；
 * 完全空白则回退默认「中心主题」。</p>
 *
 * @author 雨夜终端
 */
public final class MindmapOutlineUtil {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final String DEFAULT_ROOT = "中心主题";

    private MindmapOutlineUtil() {
    }

    /**
     * 解析大纲为节点树 JSON 字符串（不含 layout/theme 包装）。
     *
     * @param outlineMd Markdown 文本
     * @return {@code {"data":{"text":"..."},"children":[...]}}
     */
    public static String toContentJson(String outlineMd) {
        List<Heading> headings = extractHeadings(outlineMd);
        if (headings.isEmpty()) {
            return fallbackFromPlainText(outlineMd);
        }
        ONode root = buildTree(headings);
        return root.toJson();
    }

    private static List<Heading> extractHeadings(String outlineMd) {
        List<Heading> list = new ArrayList<>();
        if (Utils.isBlank(outlineMd)) {
            return list;
        }
        for (String raw : outlineMd.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher m = HEADING.matcher(line);
            if (m.matches()) {
                int level = m.group(1).length();
                String text = m.group(2).trim();
                if (!text.isEmpty()) {
                    list.add(new Heading(level, text));
                }
            }
        }
        return list;
    }

    private static String fallbackFromPlainText(String outlineMd) {
        if (Utils.isBlank(outlineMd)) {
            return nodeJson(DEFAULT_ROOT, List.of()).toJson();
        }
        List<String> lines = new ArrayList<>();
        for (String raw : outlineMd.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = raw.trim();
            if (!line.isEmpty()) {
                // 去掉列表前缀
                line = line.replaceFirst("^[-*+]\\s+", "").replaceFirst("^\\d+[.)]\\s+", "").trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }
        if (lines.isEmpty()) {
            return nodeJson(DEFAULT_ROOT, List.of()).toJson();
        }
        String rootText = lines.get(0);
        List<ONode> children = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            children.add(nodeJson(lines.get(i), List.of()));
        }
        return nodeJson(rootText, children).toJson();
    }

    private static ONode buildTree(List<Heading> headings) {
        Heading first = headings.get(0);
        // 若首条不是最浅级，仍以其为根
        ONode root = nodeJson(first.text(), new ArrayList<>());
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(first.level(), root));

        for (int i = 1; i < headings.size(); i++) {
            Heading h = headings.get(i);
            ONode node = nodeJson(h.text(), new ArrayList<>());
            while (stack.size() > 1 && stack.peek().level() >= h.level()) {
                stack.pop();
            }
            // 同级或更浅：挂到当前栈顶的父（弹出后的）
            if (stack.peek().level() >= h.level()) {
                // 只剩根且同级：作为根的兄弟不合理，降为根的子节点
                stack.peek().node().get("children").add(node);
            } else {
                stack.peek().node().get("children").add(node);
            }
            stack.push(new Frame(h.level(), node));
        }
        // 回到真正根
        while (stack.size() > 1) {
            stack.pop();
        }
        return stack.peek().node();
    }

    private static ONode nodeJson(String text, List<ONode> children) {
        ONode node = new ONode();
        ONode data = new ONode();
        data.set("text", text == null || text.isBlank() ? DEFAULT_ROOT : text);
        node.set("data", data);
        ONode kids = new ONode().asArray();
        if (children != null) {
            for (ONode c : children) {
                kids.add(c);
            }
        }
        node.set("children", kids);
        return node;
    }

    private record Heading(int level, String text) {
    }

    private record Frame(int level, ONode node) {
    }
}
