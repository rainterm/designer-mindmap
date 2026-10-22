package com.designer.admin.system.service.file.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.constant.HttpConsts;
import com.designer.admin.framework.storage.FileStorageService;
import com.designer.admin.framework.storage.StorageCachePolicy;
import com.designer.admin.framework.storage.StorageRuntimeConfig;
import com.designer.admin.framework.storage.StoredFile;
import com.designer.admin.system.constant.SysFileConsts;
import com.designer.admin.system.mapper.file.SysFileMapper;
import com.designer.admin.system.model.dto.file.FileQueryDTO;
import com.designer.admin.system.model.dto.file.FileUploadDTO;
import com.designer.admin.system.model.entity.file.SysFile;
import com.designer.admin.system.service.file.SysFileService;
import com.designer.admin.system.service.file.SysStorageConfigService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.data.annotation.Transaction;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 系统文件：上传落库、分页查询、删除（含物理文件）
 *
 * @author 雨夜终端
 */
@Component
public class SysFileServiceImpl implements SysFileService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 下载路径白名单：业务目录 / 日期 / UUID 文件名只会命中这些字符 */
    private static final Pattern SAFE_PATH = Pattern.compile("^[A-Za-z0-9._/-]+$");

    /** 允许 inline 预览的扩展名；其余一律 attachment，避免 SVG / HTML 在同源下执行脚本 */
    private static final Set<String> INLINE_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "ico", "avif",
            "pdf", "txt", "md", "markdown", "csv", "log",
            "mp3", "wav", "ogg", "mp4", "webm");

    /**
     * 扩展名 → Content-Type。**不读客户端上传时声明的类型**，只认白名单。
     * 未列出的扩展名按 application/octet-stream 下载。
     */
    private static final Map<String, String> MIME_BY_EXT = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("avif", "image/avif"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("txt", "text/plain; charset=utf-8"),
            Map.entry("md", "text/plain; charset=utf-8"),
            Map.entry("markdown", "text/plain; charset=utf-8"),
            Map.entry("csv", "text/plain; charset=utf-8"),
            Map.entry("log", "text/plain; charset=utf-8"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"));

    @Db
    private SysFileMapper fileMapper;

    @Inject
    private FileStorageService storageService;

    @Inject
    private SysStorageConfigService storageConfigService;

    @Override
    public IPage<SysFile> listPage(FileQueryDTO query) {
        LambdaQueryWrapper<SysFile> w = new LambdaQueryWrapper<SysFile>()
                .like(notBlank(query.getOriginalName()), SysFile::getOriginalName, query.getOriginalName())
                .eq(notBlank(query.getBizDir()), SysFile::getBizDir, query.getBizDir())
                .eq(notBlank(query.getStorageType()), SysFile::getStorageType, query.getStorageType())
                .eq(notBlank(query.getExtension()), SysFile::getExtension, query.getExtension())
                .like(notBlank(query.getCreateBy()), SysFile::getCreateBy, query.getCreateBy())
                .orderByDesc(SysFile::getId);
        // 条件为 false 时 MP 仍会先求值第三个参数，时间解析必须包在 if 里
        if (notBlank(query.getBeginTime())) {
            w.ge(SysFile::getCreateTime, parseBegin(query.getBeginTime()));
        }
        if (notBlank(query.getEndTime())) {
            w.le(SysFile::getCreateTime, parseEnd(query.getEndTime()));
        }
        return fileMapper.selectPage(query.toPage(), w);
    }

    @Override
    public SysFile getById(Long id) {
        SysFile file = fileMapper.selectById(id);
        if (file == null) {
            throw new BizException("文件不存在");
        }
        return file;
    }

    @Override
    public SysFile upload(UploadedFile file, FileUploadDTO dto) {
        String dir = dto.getDir();
        String bizDir = dir == null || dir.isBlank() ? "common" : dir.trim();
        StorageRuntimeConfig config = storageConfigService.requireUploadRuntime(dto.getConfigId(), dto.requirePublic());
        StoredFile stored = storageService.store(file, bizDir, config);

        SysFile entity = new SysFile();
        entity.setOriginalName(stored.getOriginalName());
        entity.setFileName(stored.getFileName());
        entity.setFilePath(stored.getFilePath());
        entity.setUrl(stored.getUrl());
        entity.setFileSize(stored.getFileSize());
        entity.setContentType(stored.getContentType());
        entity.setExtension(stored.getExtension());
        entity.setBizDir(bizDir);
        entity.setStorageType(stored.getStorageType());
        entity.setConfigId(stored.getConfigId());
        fileMapper.insert(entity);
        return entity;
    }

    @Override
    @Transaction
    public void removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的文件");
        }
        List<SysFile> files = fileMapper.selectByIds(ids);
        for (SysFile file : files) {
            storageService.delete(file.getFilePath(), file.getStorageType(), file.getConfigId());
            fileMapper.deleteById(file.getId());
        }
    }

    @Override
    public void download(Context ctx, boolean publicOnly) throws Exception {
        String relative = downloadPath(ctx);
        if (!isSafePath(relative)) {
            notFound(ctx);
            return;
        }

        // 必须命中 sys_file：不按「猜测的路径」读物理文件，
        // 否则任意登录用户都能遍历存储根目录下未登记的文件。
        SysFile meta = fileMapper.selectOne(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getFilePath, relative)
                .last("LIMIT 1"));
        if (meta == null) {
            notFound(ctx);
            return;
        }
        Long configId = meta.getConfigId();
        String storageType = meta.getStorageType();

        if (publicOnly) {
            boolean accessible;
            try {
                accessible = storageService.isPublicAccess(configId, storageType);
            } catch (Exception ignored) {
                // 配置已删除 / 不可用时同样按不存在处理
                accessible = false;
            }
            if (!accessible) {
                notFound(ctx);
                return;
            }
        }

        // Content-Type 一律由扩展名推导，不采信上传时客户端声明的值：
        // 否则上传 .txt 却声明 text/html，就会在站点同源下渲染出可执行脚本（存储型 XSS）。
        String ext = extOf(relative);
        String contentType = MIME_BY_EXT.getOrDefault(ext, "application/octet-stream");
        // 只有确认安全的类型才 inline，其余强制下载，杜绝 SVG / HTML 一类主动内容被浏览器执行
        boolean inline = INLINE_EXT.contains(ext);

        try (InputStream in = storageService.open(relative, configId, storageType)) {
            String name = relative.substring(relative.lastIndexOf('/') + 1);
            ctx.contentType(contentType);
            ctx.headerSet("Content-Disposition",
                    (inline ? "inline" : "attachment") + "; filename=\"" + name + "\"");
            ctx.headerSet("X-Content-Type-Options", "nosniff");
            // 兜底：即便将来有人把危险类型加进 inline 白名单，CSP 也不让脚本执行
            ctx.headerSet("Content-Security-Policy", "default-src 'none'; sandbox");
            // 文件路径含 UUID 且内容不会被原地覆盖，公开文件可长缓存；私有文件禁止落盘缓存。
            // 与 S3 Provider 上传时写入的对象元数据共用同一份策略，避免代理链路和 CDN 链路分叉
            ctx.headerSet("Cache-Control", StorageCachePolicy.of(publicOnly));
            in.transferTo(ctx.outputStream());
        } catch (Exception e) {
            notFound(ctx);
        }
    }

    private static String downloadPath(Context ctx) {
        String path = ctx.path();
        // 相对路径取 /download/ 之后，避免与配置前缀硬编码耦合
        int marker = path.indexOf("/download/");
        String relative = marker >= 0 ? path.substring(marker + "/download/".length()) : "";
        while (relative.startsWith(HttpConsts.PATH_SEPARATOR)) {
            relative = relative.substring(1);
        }
        return relative;
    }

    /**
     * 路径白名单：只放行本模块自己生成的形状（业务目录/yyyy/MM/dd/uuid.ext）。
     *
     * <p>拒绝 {@code ..}、反斜杠、引号、控制字符等，既是路径穿越的第二道防线
     * （第一道在 Provider 的 {@code startsWith(root)}），也避免文件名被拼进
     * {@code Content-Disposition} 时造成响应头注入。</p>
     */
    private static boolean isSafePath(String relative) {
        if (relative.isBlank() || relative.length() > SysFileConsts.MAX_PATH_LENGTH) {
            return false;
        }
        if (relative.contains(SysFileConsts.PARENT_DIR)) {
            return false;
        }
        return SAFE_PATH.matcher(relative).matches();
    }

    private static String extOf(String path) {
        int dot = path.lastIndexOf('.');
        int slash = path.lastIndexOf('/');
        if (dot < 0 || dot < slash || dot == path.length() - 1) {
            return "";
        }
        return path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static void notFound(Context ctx) {
        ctx.status(404);
        ctx.output("文件不存在");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static LocalDateTime parseBegin(String day) {
        return LocalDate.parse(day, DAY).atStartOfDay();
    }

    private static LocalDateTime parseEnd(String day) {
        return LocalDate.parse(day, DAY).atTime(LocalTime.MAX);
    }
}
