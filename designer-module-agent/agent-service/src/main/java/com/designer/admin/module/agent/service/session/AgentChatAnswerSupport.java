package com.designer.admin.module.agent.service.session;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 对话答案清洗：去掉混入正文的思考/规划草稿。
 *
 * @author 雨夜终端
 */
public final class AgentChatAnswerSupport {

    private static final Pattern PLANNING_LINE = Pattern.compile(
            "(?i)^(the user wants|let me |design (plan|direction|concept)|subject:|palette:|avoid the ai|"
                    + "concept:|visual direction|i('ll| will) |hmm[,.]|actually[,.]|ok[,.]? let me)");
    private static final Pattern COMPLETE_HTML_FENCED = Pattern.compile(
            "(?is)```html\\s*\\n[\\s\\S]{400,}?</html>[\\s\\S]*?```");

    private AgentChatAnswerSupport() {
    }

    /** 清洗混入正文的思考/规划草稿，避免答案又乱又截断。 */
    public static String sanitizeAnswerContent(String content, String thinking) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String text = content.trim();
        String th = thinking == null ? "" : thinking.trim();
        if (!th.isEmpty() && isThinkingLikeFinal(text, th)) {
            String prefix = keepDeliverablePrefix(text);
            if (prefix.isBlank()) {
                return "（模型仅返回了思考过程，未产出完整正文。可清空 Max Tokens 不限制，或调高后重试。）";
            }
            return prefix;
        }
        if (isPlanningDump(text) && !containsCompleteHtml(text)) {
            String prefix = keepDeliverablePrefix(text);
            if (!prefix.isBlank() && (containsCompleteHtml(prefix) || prefix.contains("```"))) {
                return prefix;
            }
            return "（模型未产出完整 HTML 正文，思考过程请见上方卡片。可清空 Max Tokens 不限制后重试。）";
        }
        return text;
    }

    public static boolean isThinkingLikeFinal(String text, String thinking) {
        if (text == null || text.isBlank() || thinking == null || thinking.isBlank()) {
            return false;
        }
        if (thinking.contains(text) || text.contains(thinking)) {
            return true;
        }
        int n = Math.min(280, Math.min(text.length(), thinking.length()));
        if (n < 80) {
            return false;
        }
        String head = text.substring(0, n);
        return thinking.contains(head);
    }

    public static boolean isPlanningDump(String text) {
        if (text == null || text.length() < 120) {
            return false;
        }
        int hits = 0;
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (PLANNING_LINE.matcher(t).find()) {
                hits++;
            }
        }
        return hits >= 2 && !containsCompleteHtml(text);
    }

    public static boolean containsCompleteHtml(String text) {
        if (text == null) {
            return false;
        }
        if (COMPLETE_HTML_FENCED.matcher(text).find()) {
            return true;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("<!doctype html")
                && lower.contains("</html>")
                && text.length() >= 400;
    }

    private static String keepDeliverablePrefix(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String t = line.trim();
            if (!sb.isEmpty() && PLANNING_LINE.matcher(t).find()) {
                break;
            }
            if (sb.toString().contains("```html")) {
                sb.append(line).append('\n');
                continue;
            }
            sb.append(line).append('\n');
        }
        String out = sb.toString().trim();
        if (!containsCompleteHtml(out) && !out.contains("```") && out.length() < 80) {
            return out;
        }
        if (!containsCompleteHtml(out) && isPlanningDump(out)) {
            String[] parts = out.split("\n\n", 2);
            String first = parts[0].trim();
            if (first.length() <= 120 && !PLANNING_LINE.matcher(first).find()) {
                return first;
            }
            return "";
        }
        return out;
    }
}
