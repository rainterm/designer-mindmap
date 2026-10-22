package com.designer.admin.module.agent.service.knowledge;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.storage.FileStorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 从已上传原文件抽取可索引正文（PDF / DOCX / 纯文本）。
 *
 * @author 雨夜终端
 */
@Component
public class AgentKnowledgeTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(AgentKnowledgeTextExtractor.class);
    private static final int MAX_CHARS = 2_000_000;

    @Inject
    private FileStorageService fileStorageService;

    public String extract(String url, String fileName, String fileType) {
        String path = resolveStoragePath(url);
        if (path == null || path.isBlank()) {
            throw new BizException("无法定位原文件路径，请重新上传");
        }
        String ext = normalizeExt(fileType, fileName, path);
        try (InputStream in = fileStorageService.open(path, null, null)) {
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) {
                return "";
            }
            String text = switch (ext) {
                case "pdf" -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                case "txt", "md", "markdown", "json", "csv", "log", "xml", "yml", "yaml",
                     "html", "htm", "js", "ts", "java", "py", "go", "sql", "css" -> extractPlain(bytes);
                default -> throw new BizException("暂不支持从 ." + ext + " 自动抽取正文，请手动粘贴文本后重索引");
            };
            if (text == null) {
                return "";
            }
            text = text.replace("\u0000", "").trim();
            if (text.length() > MAX_CHARS) {
                text = text.substring(0, MAX_CHARS);
            }
            return text;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("抽取正文失败 path={} ext={}", path, ext, e);
            throw new BizException("抽取正文失败: " + e.getMessage(), e);
        }
    }

    static String resolveStoragePath(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String u = url.trim();
        int marker = u.indexOf("/download/");
        if (marker >= 0) {
            String relative = u.substring(marker + "/download/".length());
            int q = relative.indexOf('?');
            if (q >= 0) {
                relative = relative.substring(0, q);
            }
            while (relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            return relative.contains("..") ? null : relative;
        }
        // 已是相对存储路径
        if (!u.startsWith("http://") && !u.startsWith("https://") && !u.startsWith("/")) {
            return u.contains("..") ? null : u;
        }
        return null;
    }

    private static String normalizeExt(String fileType, String fileName, String path) {
        String ext = fileType == null ? "" : fileType.trim().toLowerCase(Locale.ROOT);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        if (!ext.isBlank() && !"bin".equals(ext) && !"url".equals(ext) && !"text".equals(ext)) {
            return ext;
        }
        String name = fileName != null && !fileName.isBlank() ? fileName : path;
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            return name.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        return "bin";
    }

    private static String extractPdf(byte[] bytes) throws Exception {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static String extractDocx(byte[] bytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String extractPlain(byte[] bytes) {
        Charset cs = StandardCharsets.UTF_8;
        // 简单 UTF-8 BOM
        int offset = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        return new String(bytes, offset, bytes.length - offset, cs);
    }
}
