package com.designer.admin.module.agent.service.knowledge;

import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.constant.knowledge.ChunkStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本分块：fixed（固定窗口）/ section_recursive（按章节+递归，切片保留章节目录）。
 *
 * @author 雨夜终端
 */
public final class AgentTextChunker {

    /** @deprecated use {@link ChunkStrategy#FIXED} */
    public static final String STRATEGY_FIXED = ChunkStrategy.FIXED.getCode();
    /** @deprecated use {@link ChunkStrategy#SECTION_RECURSIVE} */
    public static final String STRATEGY_SECTION_RECURSIVE = ChunkStrategy.SECTION_RECURSIVE.getCode();

    private static final Pattern MD_HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern CN_CHAPTER = Pattern.compile(
            "^(第[零一二三四五六七八九十百千0-9]+[章节篇部条]|[（(]?[一二三四五六七八九十]+[）)]|[0-9]+\\s*[.、]|[一二三四五六七八九十]+、)\\s*.+$");

    private static final String[] RECURSIVE_SEPS = {"\n\n", "\n", "。", "！", "？", "；", ".", "!", "?", ";", " "};

    private AgentTextChunker() {
    }

    public static String normalizeStrategy(String strategy) {
        return ChunkStrategy.normalize(strategy);
    }

    public static List<String> chunk(String text, String strategy, int chunkSize, int chunkOverlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String content = text.trim();
        int size = Math.max(AgentConsts.MIN_CHUNK_SIZE, chunkSize);
        int overlap = Math.max(0, Math.min(chunkOverlap, size - 1));
        String st = normalizeStrategy(strategy);
        if (ChunkStrategy.FIXED.eq(st)) {
            return fixedChunk(content, size, overlap);
        }
        return sectionRecursiveChunk(content, size, overlap);
    }

    /** 兼容旧调用：固定窗口。 */
    public static List<String> chunk(String text, int chunkSize, int chunkOverlap) {
        return chunk(text, STRATEGY_FIXED, chunkSize, chunkOverlap);
    }

    private static List<String> fixedChunk(String content, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int len = content.length();
        while (start < len) {
            int end = Math.min(start + size, len);
            String piece = content.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= len) {
                break;
            }
            start = end - overlap;
            if (start <= 0 || start >= end) {
                start = end;
            }
        }
        return chunks;
    }

    private static List<String> sectionRecursiveChunk(String content, int size, int overlap) {
        List<Section> sections = parseSections(content);
        List<String> out = new ArrayList<>();
        for (Section sec : sections) {
            String body = sec.body == null ? "" : sec.body.trim();
            if (body.isEmpty()) {
                continue;
            }
            String prefix = formatPathPrefix(sec.path);
            if (body.length() <= size) {
                out.add(withPrefix(prefix, body));
                continue;
            }
            List<String> pieces = recursiveSplit(body, size, overlap);
            for (String piece : pieces) {
                if (piece == null || piece.isBlank()) {
                    continue;
                }
                out.add(withPrefix(prefix, piece.trim()));
            }
        }
        return out;
    }

    private static String formatPathPrefix(List<String> path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return "【" + String.join(" / ", path) + "】";
    }

    private static String withPrefix(String prefix, String body) {
        if (prefix == null || prefix.isBlank()) {
            return body;
        }
        return prefix + "\n" + body;
    }

    private static List<Section> parseSections(String content) {
        String[] lines = content.split("\n", -1);
        List<HeadingLevel> stack = new ArrayList<>();
        List<Section> sections = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        List<String> currentPath = List.of();

        for (String rawLine : lines) {
            Heading heading = detectHeading(rawLine);
            if (heading != null) {
                flushSection(sections, currentPath, body);
                body.setLength(0);
                while (!stack.isEmpty() && stack.get(stack.size() - 1).level >= heading.level) {
                    stack.remove(stack.size() - 1);
                }
                stack.add(new HeadingLevel(heading.level, heading.title));
                currentPath = stack.stream().map(h -> h.title).toList();
                continue;
            }
            if (body.length() > 0) {
                body.append('\n');
            }
            body.append(rawLine);
        }
        flushSection(sections, currentPath, body);
        return sections;
    }

    private static void flushSection(List<Section> sections, List<String> path, StringBuilder body) {
        String text = body.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        sections.add(new Section(path == null ? List.of() : List.copyOf(path), text));
    }

    private static Heading detectHeading(String line) {
        if (line == null) {
            return null;
        }
        String t = line.trim();
        if (t.isEmpty()) {
            return null;
        }
        Matcher md = MD_HEADING.matcher(t);
        if (md.matches()) {
            int level = md.group(1).length();
            String title = md.group(2).trim();
            if (!title.isEmpty()) {
                return new Heading(level, title);
            }
        }
        if (CN_CHAPTER.matcher(t).matches() && t.length() <= 80) {
            return new Heading(1, t);
        }
        return null;
    }

    private static List<String> recursiveSplit(String text, int size, int overlap) {
        List<String> parts = splitBySeparators(text, size, RECURSIVE_SEPS, 0);
        return mergeBySize(parts, size, overlap);
    }

    private static List<String> splitBySeparators(String text, int size, String[] seps, int sepIndex) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (text.length() <= size || sepIndex >= seps.length) {
            return List.of(text);
        }
        String sep = seps[sepIndex];
        List<String> raw = splitKeepSep(text, sep);
        if (raw.size() <= 1) {
            if (sepIndex + 1 < seps.length) {
                return splitBySeparators(text, size, seps, sepIndex + 1);
            }
            return List.of(text);
        }
        List<String> out = new ArrayList<>();
        for (String piece : raw) {
            String p = piece.trim();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() > size && sepIndex + 1 < seps.length) {
                out.addAll(splitBySeparators(p, size, seps, sepIndex + 1));
            } else {
                out.add(p);
            }
        }
        return out.isEmpty() ? List.of(text) : out;
    }

    private static List<String> splitKeepSep(String text, String sep) {
        List<String> out = new ArrayList<>();
        if (sep.isEmpty()) {
            out.add(text);
            return out;
        }
        int from = 0;
        int idx;
        while ((idx = text.indexOf(sep, from)) >= 0) {
            out.add(text.substring(from, idx + sep.length()));
            from = idx + sep.length();
        }
        if (from < text.length()) {
            out.add(text.substring(from));
        }
        return out;
    }

    private static List<String> mergeBySize(List<String> parts, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String part : parts) {
            String p = part == null ? "" : part;
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() > size) {
                if (buf.length() > 0) {
                    chunks.add(buf.toString().trim());
                    buf.setLength(0);
                }
                chunks.addAll(fixedChunk(p, size, overlap));
                continue;
            }
            if (buf.length() == 0) {
                buf.append(p);
                continue;
            }
            if (buf.length() + p.length() <= size) {
                buf.append(p);
            } else {
                chunks.add(buf.toString().trim());
                String prev = buf.toString();
                buf.setLength(0);
                if (overlap > 0 && prev.length() > overlap) {
                    String ov = prev.substring(prev.length() - overlap).trim();
                    if (!ov.isEmpty()) {
                        buf.append(ov);
                        if (!ov.endsWith("\n") && !p.startsWith("\n")) {
                            buf.append('\n');
                        }
                    }
                }
                buf.append(p);
                if (buf.length() > size) {
                    chunks.addAll(fixedChunk(buf.toString(), size, overlap));
                    buf.setLength(0);
                }
            }
        }
        if (buf.length() > 0) {
            String last = buf.toString().trim();
            if (!last.isEmpty()) {
                chunks.add(last);
            }
        }
        return chunks;
    }

    private record Heading(int level, String title) {
    }

    private record HeadingLevel(int level, String title) {
    }

    private record Section(List<String> path, String body) {
    }
}
