package com.designer.admin.system.controller.admin.file;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.file.StorageConfigQueryDTO;
import com.designer.admin.system.model.dto.file.StorageConfigSaveDTO;
import com.designer.admin.system.model.entity.file.SysStorageConfig;
import com.designer.admin.system.model.vo.file.StorageConfigDetailVO;
import com.designer.admin.system.model.vo.file.StorageTypeVO;
import com.designer.admin.system.service.file.SysStorageConfigService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 文件存储配置管理（数据库配置，可扩展 Provider）
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/storage")
@Api(tags = "存储配置")
public class SysStorageConfigController {

    @Inject
    private SysStorageConfigService storageConfigService;

    @Get
    @Mapping("/types")
    // 文件列表页也要用这份「存储类型」元数据做筛选与标签，不能只对存储配置的可见权限放行，
    // 否则只有文件权限的角色会 403，只能在前端再抄一份 本地 / S3。
    @SaCheckPermission(value = {"system:storage:list", "system:file:list"}, mode = SaMode.OR)
    @ApiOperation(value = "查询存储类型")
    public Result<List<StorageTypeVO>> listTypes() {
        return Result.ok(storageConfigService.listTypes());
    }

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:storage:list")
    @ApiOperation(value = "分页查询存储配置")
    public Result<PageResult<SysStorageConfig>> listPage(StorageConfigQueryDTO query) {
        return Result.ok(PageResult.of(storageConfigService.listPage(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:storage:query")
    @ApiOperation(value = "获取存储配置详情")
    public Result<StorageConfigDetailVO> getDetail(Long id) {
        return Result.ok(storageConfigService.getDetailView(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "存储配置", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:storage:add")
    @ApiOperation(value = "新增存储配置")
    public Result<Void> add(@Body @Validated StorageConfigSaveDTO dto) {
        storageConfigService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "存储配置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:storage:edit")
    @ApiOperation(value = "修改存储配置")
    public Result<Void> update(@Body @Validated StorageConfigSaveDTO dto) {
        storageConfigService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "存储配置", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:storage:remove")
    @ApiOperation(value = "删除存储配置")
    public Result<Void> remove(List<Long> ids) {
        storageConfigService.removeByIds(ids);
        return Result.ok();
    }

    @Put
    @Mapping("/{id}/default")
    @OperLog(title = "存储配置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:storage:edit")
    @ApiOperation(value = "设置默认存储配置")
    public Result<Void> updateDefault(Long id) {
        storageConfigService.updateDefault(id);
        return Result.ok();
    }

    @Put
    @Mapping("/{id}/status")
    @OperLog(title = "存储配置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:storage:edit")
    @ApiOperation(value = "修改存储配置状态")
    public Result<Void> updateStatus(Long id, Integer status) {
        storageConfigService.updateStatus(id, status);
        return Result.ok();
    }
}
