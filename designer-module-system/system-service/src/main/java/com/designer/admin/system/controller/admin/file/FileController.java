package com.designer.admin.system.controller.admin.file;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.designer.admin.common.core.Result;
import com.designer.admin.system.model.dto.file.FileUploadDTO;
import com.designer.admin.system.model.entity.file.SysFile;
import com.designer.admin.system.model.vo.file.FileUploadVO;
import com.designer.admin.system.model.vo.file.StorageOptionVO;
import com.designer.admin.system.service.file.SysFileService;
import com.designer.admin.system.service.file.SysStorageConfigService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;

import java.util.List;

/**
 * 文件上传 / 私有下载（上传会写入 sys_file 元数据；下载按文件记录的存储配置读取）
 *
 * @author 雨夜终端
 */
@Controller
@Mapping("/admin-api/system/file")
@Api(tags = "文件上传")
public class FileController {

    @Inject
    private SysFileService fileService;

    @Inject
    private SysStorageConfigService storageConfigService;

    @Post
    @Mapping("/upload")
    @ApiOperation(value = "上传文件")
    public Result<FileUploadVO> upload(UploadedFile file, FileUploadDTO dto) {
        SysFile saved = fileService.upload(file, dto);
        return Result.ok(FileUploadVO.from(saved));
    }

    @Get
    @Mapping("/storage-options")
    @ApiOperation(value = "查询可用存储选项")
    public Result<List<StorageOptionVO>> listStorageOptions() {
        return Result.ok(storageConfigService.listPublicOptions());
    }

    @Get
    @Mapping("/download/**")
    @ApiOperation(value = "下载私有文件")
    public void download(Context ctx) throws Exception {
        fileService.download(ctx, false);
    }
}
