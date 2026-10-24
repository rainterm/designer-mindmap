package com.designer.admin.module.agent.harness.skill;

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

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(AgentSkillMountSupport.class);
    /** Skill 目录名安全字符过滤正则 */
    private static final Pattern SAFE_CODE = Pattern.compile("[^a-zA-Z0-9._-]+");

    /** 工具类，禁止实例化。 */
    private AgentSkillMountSupport() {
    }

    /**
     * 写入绑定 Skill 的 SKILL.md，并返回可注册的挂载配置。
     * 路径相对 workspace：{@code ./.harness/skills-bound/{agentId}}
     *
     * @param workspace 工作区根目录
     * @param agentId   智能体 ID，作为 bindKey
     * @param skills    绑定的 Skill 列表
     * @return 挂载目录配置；无有效写入时返回 null
     * @throws IOException 读写目录失败时抛出
     */
    public static MountDir materializeBoundSkills(Path workspace, Long agentId, List<AgentSkill> skills)
            throws IOException {
        if (agentId == null) {
            return null;
        }
        return materializeBoundSkills(workspace, String.valueOf(agentId), skills);
    }

    /**
     * 工作流等场景：用任意 bindKey 物化 Skill 挂载目录。
     *
     * @param workspace 工作区根目录
     * @param bindKey   绑定键（目录名）
     * @param skills    绑定的 Skill 列表
     * @return 挂载目录配置；无有效写入时返回 null
     * @throws IOException 读写目录失败时抛出
     */
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

    /**
     * 将 Skill code 清洗为安全目录名。
     *
     * @param code 原始 code；可空
     * @param id   回退用 Skill ID；可空
     * @return 小写安全目录名
     */
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

    /**
     * 判断内容是否已含 YAML frontmatter。
     *
     * @param content SKILL.md 正文
     * @return true 表示以 --- 开头
     */
    private static boolean hasFrontmatter(String content) {
        String t = content.replace("\uFEFF", "");
        return t.startsWith("---\n") || t.startsWith("---\r\n");
    }

    /**
     * 为缺少 frontmatter 的正文补齐 YAML 头。
     *
     * @param skill Skill 实体
     * @param code  安全 code
     * @param body  正文
     * @return 带 frontmatter 的完整 Markdown
     */
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

    /**
     * 内容为空时按名称/描述生成最小 SKILL.md。
     *
     * @param skill Skill 实体
     * @param code  安全 code
     * @return 完整 SKILL.md 文本
     */
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

    /**
     * YAML 双引号转义。
     *
     * @param value 原始字符串
     * @return 带引号的安全字面量
     */
    private static String yamlEscape(String value) {
        String v = value == null ? "" : value.replace("\"", "\\\"");
        return "\"" + v + "\"";
    }

    /**
     * 清空目录下文件与子目录（保留根目录本身）。
     *
     * @param root 目标根目录
     * @throws IOException 删除失败时抛出
     */
    private static void clearDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            /** 删除文件。 */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            /** 删除子目录（保留 root）。 */
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
