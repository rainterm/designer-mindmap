package com.designer.admin.framework.storage;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * 上传图片压缩：落盘前按质量重新编码，只有比原文件更小才采用。
 *
 * <p><b>为什么放在服务端</b>：压缩属于「存储策略」，封面、编辑器插图、附件等所有入口
 * 最终都走 {@link FileStorageService#store}。在这里做一次即可覆盖全部上传路径；
 * 交给前端则每个调用点都要记得压，漏一处就是一张原图，而且无法防止绕过页面的直接调用。</p>
 *
 * <p><b>支持的格式刻意收窄</b>：</p>
 * <ul>
 *   <li>JPEG：有损重编码，质量由配置决定（10% ~ 100%）；</li>
 *   <li>PNG：无损重编码，质量参数无意义，固定用最高压缩级别；</li>
 *   <li>GIF：可能是动图，重新编码只会留下第一帧，跳过；</li>
 *   <li>WebP / AVIF / SVG / ICO：JDK 内置 ImageIO 不支持编码，跳过。</li>
 * </ul>
 * <p>跳过不影响上传，只是保持原文件。压缩失败同样降级为原文件 —— 宁可多占空间，
 * 也不能因为压缩器的问题让用户的图片传不上去。</p>
 *
 * @author 雨夜终端
 */
@Slf4j
public final class ImageCompressor {

    /** 可压缩的扩展名（小写、无点） */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    /** 允许解码的像素上限：约 4000 万像素，防止「压缩炸弹」把堆吃光 */
    private static final long MAX_PIXELS = 40_000_000L;

    /** 质量下限：再低画质劣化明显，收益也有限 */
    public static final int MIN_QUALITY = 10;

    /** 质量上限 */
    public static final int MAX_QUALITY = 100;

    /** 未配置时的默认质量 */
    public static final int DEFAULT_QUALITY = 80;

    private ImageCompressor() {
    }

    /**
     * 该扩展名是否可能被压缩。
     *
     * @param ext 小写、无点的扩展名
     * @return 支持时返回 {@code true}
     */
    public static boolean supports(String ext) {
        return ext != null && SUPPORTED_EXTENSIONS.contains(ext);
    }

    /**
     * 压缩图片。
     *
     * @param source        原文件字节
     * @param ext           小写、无点的扩展名
     * @param qualityPercent JPEG 质量百分比（10 ~ 100），PNG 忽略
     * @return 压缩后的字节；格式不支持、解码失败、压缩后反而更大时返回 {@code null}
     */
    public static byte[] compress(byte[] source, String ext, int qualityPercent) {
        if (source == null || source.length == 0 || !supports(ext)) {
            return null;
        }
        String format = "png".equals(ext) ? "png" : "jpeg";
        try {
            BufferedImage image = read(source);
            if (image == null) {
                return null;
            }
            BufferedImage prepared = "jpeg".equals(format) ? toRgb(withExifOrientation(image, source)) : image;
            // PNG 的 compressionQuality 与 JPEG 语义相反：0 才是最大压缩，且始终无损
            float quality = "png".equals(format) ? 0F : normalizeQuality(qualityPercent) / 100F;
            byte[] compressed = write(prepared, format, quality);
            // 原图往往已经过优化（例如导出工具处理过的 PNG），重编码可能更大，此时保留原文件
            if (compressed.length == 0 || compressed.length >= source.length) {
                return null;
            }
            return compressed;
        } catch (Exception e) {
            log.warn("图片压缩失败，按原文件上传：{}", e.getMessage());
            return null;
        }
    }

    /** 质量归一化：空值/越界都收敛到合法区间 */
    public static int normalizeQuality(Integer quality) {
        if (quality == null) {
            return DEFAULT_QUALITY;
        }
        return Math.max(MIN_QUALITY, Math.min(MAX_QUALITY, quality));
    }

    /**
     * 解码图片。
     *
     * <p>先读尺寸再解码：一张几 KB 的 PNG 可以声明上亿像素，直接 {@code ImageIO.read}
     * 会在分配像素数组时 OOM。超限时按「不压缩」处理，文件大小仍由存储配置的上限把关。</p>
     */
    private static BufferedImage read(byte[] source) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            if (input == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    log.warn("图片像素数 {} 超过压缩上限 {}，跳过压缩", pixels, MAX_PIXELS);
                    return null;
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    private static byte[] write(BufferedImage image, String format, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            return new byte[0];
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream output = ImageIO.createImageOutputStream(out)) {
                if (output == null) {
                    return new byte[0];
                }
                writer.setOutput(output);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return out.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    /**
     * JPEG 编码器只接受 RGB 系图像，带透明通道或 CMYK 会直接抛异常。
     * 统一铺白底再画一遍：PNG 转 JPEG 时透明区域也不会变成黑块。
     */
    private static BufferedImage toRgb(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    /**
     * 按 EXIF 方向把图片摆正。
     *
     * <p>手机拍摄的 JPEG 通常像素是横的、靠 EXIF {@code Orientation} 标记旋转展示。
     * ImageIO 不认识这个标记，重编码会丢掉元数据，于是原样写出的图在浏览器里就躺倒了。
     * 这里先按标记把像素转正，再丢掉元数据，展示效果与压缩前一致。</p>
     */
    private static BufferedImage withExifOrientation(BufferedImage image, byte[] source) {
        int orientation = exifOrientation(source);
        if (orientation <= 1 || orientation > 8) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swap = orientation >= 5;
        int dstW = swap ? h : w;
        int dstH = swap ? w : h;
        AffineTransform transform = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
            default -> new AffineTransform();
        };
        int type = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage out = new BufferedImage(dstW, dstH, type);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(image, transform, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /** 读取 JPEG 的 EXIF Orientation；找不到或格式异常时返回 1（正常方向） */
    private static int exifOrientation(byte[] src) {
        if (src.length < 4 || (src[0] & 0xFF) != 0xFF || (src[1] & 0xFF) != 0xD8) {
            return 1;
        }
        int pos = 2;
        while (pos + 4 <= src.length) {
            if ((src[pos] & 0xFF) != 0xFF) {
                return 1;
            }
            int marker = src[pos + 1] & 0xFF;
            // 无长度的独立标记
            if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                pos += 2;
                continue;
            }
            // 图像数据开始 / 文件结束，其后不会再有 EXIF
            if (marker == 0xDA || marker == 0xD9) {
                return 1;
            }
            int length = readUInt16(src, pos + 2, false);
            if (length < 2 || pos + 2 + length > src.length) {
                return 1;
            }
            if (marker == 0xE1 && length >= 8
                    && src[pos + 4] == 'E' && src[pos + 5] == 'x' && src[pos + 6] == 'i' && src[pos + 7] == 'f') {
                return readTiffOrientation(src, pos + 10, pos + 2 + length);
            }
            pos += 2 + length;
        }
        return 1;
    }

    /** 解析 TIFF 头后的第一个 IFD，只关心 Orientation（0x0112） */
    private static int readTiffOrientation(byte[] src, int tiff, int end) {
        if (tiff + 8 > end) {
            return 1;
        }
        boolean little = src[tiff] == 'I' && src[tiff + 1] == 'I';
        boolean big = src[tiff] == 'M' && src[tiff + 1] == 'M';
        if (!little && !big) {
            return 1;
        }
        int ifd = tiff + readUInt32(src, tiff + 4, little);
        if (ifd < tiff || ifd + 2 > end) {
            return 1;
        }
        int count = readUInt16(src, ifd, little);
        for (int i = 0; i < count; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > end) {
                return 1;
            }
            if (readUInt16(src, entry, little) == 0x0112) {
                // SHORT 值左对齐存放在 4 字节值域的前两个字节
                return readUInt16(src, entry + 8, little);
            }
        }
        return 1;
    }

    private static int readUInt16(byte[] src, int offset, boolean little) {
        if (offset + 2 > src.length) {
            return 0;
        }
        int b0 = src[offset] & 0xFF;
        int b1 = src[offset + 1] & 0xFF;
        return little ? (b1 << 8) | b0 : (b0 << 8) | b1;
    }

    private static int readUInt32(byte[] src, int offset, boolean little) {
        if (offset + 4 > src.length) {
            return -1;
        }
        int b0 = src[offset] & 0xFF;
        int b1 = src[offset + 1] & 0xFF;
        int b2 = src[offset + 2] & 0xFF;
        int b3 = src[offset + 3] & 0xFF;
        return little
                ? (b3 << 24) | (b2 << 16) | (b1 << 8) | b0
                : (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }
}
