package com.designer.admin.common.util;

import com.alibaba.excel.EasyExcel;
import com.designer.admin.common.exception.BizException;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * EasyExcel 导入导出辅助
 *
 * @author 雨夜终端
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    public static <T> void write(Context ctx, String filename, String sheetName, Class<T> head, List<T> data)
            throws IOException {
        if (ctx == null) {
            throw new BizException("导出上下文不存在");
        }
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        ctx.contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ctx.headerSet("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        EasyExcel.write(ctx.outputStream(), head).sheet(sheetName == null ? "Sheet1" : sheetName)
                .doWrite(data == null ? Collections.emptyList() : data);
    }

    public static <T> void writeTemplate(Context ctx, String filename, String sheetName, Class<T> head)
            throws IOException {
        write(ctx, filename, sheetName, head, Collections.emptyList());
    }

    public static <T> List<T> read(UploadedFile file, Class<T> head) {
        if (file == null || file.getContentSize() <= 0) {
            throw new BizException("请上传 Excel 文件");
        }
        try {
            return EasyExcel.read(file.getContent()).head(head).sheet().doReadSync();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Excel 解析失败：" + e.getMessage());
        }
    }
}
