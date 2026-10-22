package com.designer.admin.module.agent.harness;

import com.designer.admin.module.agent.model.entity.skill.AgentSkill;
import org.noear.solon.ai.talents.mount.MountDir;
import org.noear.solon.ai.talents.mount.MountType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 将数据库 Skill 物化到 Harness skills 挂载目录，供 SkillTalent / skillread 使用。
 *
 * @author 雨夜终端
 */
public final class AgentSkillMountSupport {

    private static final Logger log = LoggerFactory.getLogger(AgentSkillMountSupport.class);
    private static final Pattern SAFE_CODE = Pattern.compile("[^a-zA-Z0-9._-]+");

    private AgentSkillMountSupport() {
    }

    /**
     * 写入绑定 Skill 的 SKILL.md，并返回可注册的挂载配置。
     * 路径相对 workspace：{@code ./.harness/skills-bound/{agentId}}
     */
    public static MountDir materializeBoundSkills(Path workspace, Long agentId, List<AgentSkill> skills)
            throws IOException {
        if (agentId == null) {
            return null;
        }
        return materializeBoundSkills(workspace, String.valueOf(agentId), skills);
    }

    /** 工作流等场景：用任意 bindKey 物化 Skill 挂载目录。 */
    public static MountDir materializeBoundSkills(Path workspace, String bindKey, List<AgentSkill> skills)
            throws IOException {
        if (workspace == null || bindKey == null || bindKey.isBlank() || skills == null || skills.isEmpty()) {
            return null;
        }
        String key = sanitizeCode(bindKey, null);
        Path root = workspace.resolve(".harness").resolve("skills-bound").resolve(key)
                .toAbsolutePath().normalize();
        Files.createDirectories(root);
        clearDirectory(root);

        int written = 0;
        for (AgentSkill skill : skills) {
            if (skill == null) {
                continue;
            }
            String code = sanitizeCode(skill.getCode(), skill.getId());
            String content = skill.getContent() == null ? "" : skill.getContent().trim();
            if (content.isBlank()) {
                content = buildFallbackSkillMd(skill, code);
            } else if (!hasFrontmatter(content)) {
                content = wrapFrontmatter(skill, code, content);
            }
            Path dir = root.resolve(code);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("SKILL.md"), content, StandardCharsets.UTF_8);
            written++;
        }
        if (written == 0) {
            return null;
        }
        log.info("已物化 {} 个 Skill 到 {}", written, root);
        return MountDir.builder()
                .alias("@bound-skills")
                .description("绑定 Skill")
                .type(MountType.SKILLS)
                .path("./.harness/skills-bound/" + key)
                .primary(true)
                .enabled(true)
                .writeable(false)
                .build();
    }

    static String sanitizeCode(String code, Long id) {
        String raw = code == null ? "" : code.trim();
        if (raw.isBlank()) {
            raw = "skill-" + (id == null ? "unknown" : id);
        }
        String safe = SAFE_CODE.matcher(raw).replaceAll("-")
                .replaceAll("^-+|-+$", "");
        if (safe.isBlank() || ".".equals(safe) || "..".equals(safe)) {
            safe = "skill-" + (id == null ? "unknown" : id);
        }
        return safe.toLowerCase(Locale.ROOT);
    }

    private static boolean hasFrontmatter(String content) {
        String t = content.replace("\uFEFF", "");
        return t.startsWith("---\n") || t.startsWith("---\r\n");
    }

    private static String wrapFrontmatter(AgentSkill skill, String code, String body) {
        String name = skill.getName() == null || skill.getName().isBlank() ? code : skill.getName().trim();
        String desc = skill.getDescription() == null ? "" : skill.getDescription().trim();
        if (desc.isBlank()) {
            desc = name;
        }
        return "---\n"
                + "name: " + yamlEscape(code) + "\n"
                + "description: " + yamlEscape(desc) + "\n"
                + "title: " + yamlEscape(name) + "\n"
                + "---\n\n"
                + body.trim()
                + "\n";
    }

    private static String buildFallbackSkillMd(AgentSkill skill, String code) {
        String name = skill.getName() == null || skill.getName().isBlank() ? code : skill.getName().trim();
        String desc = skill.getDescription() == null || skill.getDescription().isBlank()
                ? name
                : skill.getDescription().trim();
        return "---\n"
                + "name: " + yamlEscape(code) + "\n"
                + "description: " + yamlEscape(desc) + "\n"
                + "title: " + yamlEscape(name) + "\n"
                + "---\n\n"
                + "# " + name + "\n\n"
                + desc + "\n";
    }

    private static String yamlEscape(String value) {
        String v = value == null ? "" : value.replace("\"", "\\\"");
        return "\"" + v + "\"";
    }

    private static void clearDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.equals(root)) {
                    Files.deleteIfExists(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
