package com.designer.admin.system.controller.admin.dict;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.common.util.ExcelUtil;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.constant.SystemCacheConsts;
import com.designer.admin.system.model.dto.dict.DictTypeQueryDTO;
import com.designer.admin.system.model.dto.dict.DictTypeSaveDTO;
import com.designer.admin.system.model.entity.dict.SysDictData;
import com.designer.admin.system.model.entity.dict.SysDictType;
import com.designer.admin.system.model.vo.dict.DictDataExcelVO;
import com.designer.admin.system.model.vo.dict.DictTypeExcelVO;
import com.designer.admin.system.model.vo.common.ImportResultVO;
import com.designer.admin.system.service.dict.SysDictService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.data.annotation.Cache;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 字典管理：类型与数据两组 CRUD + 启用下拉
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/dict")
@Api(tags = "数据字典")
public class SysDictController {

    @Inject
    private SysDictService dictService;

    // ----------------------------- 字典类型 -----------------------------

    @Get
    @Mapping("/type/page")
    @SaCheckPermission("system:dict:list")
    @ApiOperation(value = "分页查询字典类型")
    public Result<PageResult<SysDictType>> listTypePage(DictTypeQueryDTO query) {
        return Result.ok(PageResult.of(dictService.listTypePage(query)));
    }

    @Get
    @Mapping("/type/options")
    @Cache(key = SystemCacheConsts.DICT_TYPE_OPTIONS,
            tags = SystemCacheConsts.DICT_TYPE_TAG,
            seconds = SystemCacheConsts.CACHE_SECONDS)
    @ApiOperation(value = "查询字典类型选项")
    public Result<List<SysDictType>> listTypeOptions() {
        return Result.ok(dictService.listTypeOptions());
    }

    @Post
    @Mapping("/type")
    @OperLog(title = "字典管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:dict:add")
    @ApiOperation(value = "新增字典类型")
    public Result<Void> addType(@Body @Validated DictTypeSaveDTO dto) {
        dictService.addType(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/type")
    @OperLog(title = "字典管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:dict:edit")
    @ApiOperation(value = "修改字典类型")
    public Result<Void> updateType(@Body @Validated DictTypeSaveDTO dto) {
        dictService.updateType(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/type")
    @OperLog(title = "字典管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:dict:remove")
    @ApiOperation(value = "删除字典类型")
    public Result<Void> removeType(List<Long> ids) {
        dictService.removeType(ids);
        return Result.ok();
    }

    @Get
    @Mapping("/type/export")
    @OperLog(title = "字典管理", businessType = BusinessType.EXPORT, saveResult = false)
    @SaCheckPermission("system:dict:export")
    @ApiOperation(value = "导出字典类型")
    public void exportType(Context ctx, DictTypeQueryDTO query) throws Exception {
        ExcelUtil.write(ctx, "字典类型.xlsx", "字典类型", DictTypeExcelVO.class, dictService.exportTypeRows(query));
    }

    @Get
    @Mapping("/type/importTemplate")
    @SaCheckPermission("system:dict:import")
    @ApiOperation(value = "下载字典类型导入模板")
    public void exportTypeTemplate(Context ctx) throws Exception {
        ExcelUtil.writeTemplate(ctx, "字典类型导入模板.xlsx", "字典类型", DictTypeExcelVO.class);
    }

    @Post
    @Mapping("/type/import")
    @OperLog(title = "字典管理", businessType = BusinessType.IMPORT, saveParam = false, saveResult = false)
    @SaCheckPermission("system:dict:import")
    @ApiOperation(value = "导入字典类型")
    public Result<ImportResultVO> importType(UploadedFile file) {
        int count = dictService.importTypes(ExcelUtil.read(file, DictTypeExcelVO.class));
        return Result.ok(new ImportResultVO(count));
    }

    // ----------------------------- 字典数据 -----------------------------

    @Get
    @Mapping("/data/page")
    @SaCheckPermission("system:dict:list")
    @ApiOperation(value = "分页查询字典数据")
    public Result<PageResult<SysDictData>> listDataPage(DictTypeQueryDTO query, String dictType) {
        return Result.ok(PageResult.of(dictService.listDataPage(query, dictType)));
    }

    @Get
    @Mapping("/data/export")
    @OperLog(title = "字典管理", businessType = BusinessType.EXPORT, saveResult = false)
    @SaCheckPermission("system:dict:export")
    @ApiOperation(value = "导出字典数据")
    public void exportData(Context ctx, String dictType) throws Exception {
        ExcelUtil.write(ctx, "字典数据.xlsx", "字典数据", DictDataExcelVO.class, dictService.exportDataRows(dictType));
    }

    @Get
    @Mapping("/data/importTemplate")
    @SaCheckPermission("system:dict:import")
    @ApiOperation(value = "下载字典数据导入模板")
    public void exportDataTemplate(Context ctx) throws Exception {
        ExcelUtil.writeTemplate(ctx, "字典数据导入模板.xlsx", "字典数据", DictDataExcelVO.class);
    }

    @Post
    @Mapping("/data/import")
    @OperLog(title = "字典管理", businessType = BusinessType.IMPORT, saveParam = false, saveResult = false)
    @SaCheckPermission("system:dict:import")
    @ApiOperation(value = "导入字典数据")
    public Result<ImportResultVO> importData(UploadedFile file) {
        int count = dictService.importData(ExcelUtil.read(file, DictDataExcelVO.class));
        return Result.ok(new ImportResultVO(count));
    }

    @Get
    @Mapping("/data/batch")
    @ApiOperation(value = "批量查询字典数据")
    public Result<List<SysDictData>> listDataBatch(List<String> types) {
        return Result.ok(dictService.listDataBatch(types));
    }

    @Get
    @Mapping("/data/{dictType}")
    @Cache(key = SystemCacheConsts.DICT_DATA_KEY_TMPL,
            tags = SystemCacheConsts.DICT_DATA_TAG,
            seconds = SystemCacheConsts.CACHE_SECONDS)
    @ApiOperation(value = "根据类型查询字典数据")
    public Result<List<SysDictData>> listDataByType(String dictType) {
        return Result.ok(dictService.listDataByType(dictType));
    }

    @Post
    @Mapping("/data")
    @OperLog(title = "字典管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:dict:add")
    @ApiOperation(value = "新增字典数据")
    public Result<Void> addData(@Body @Validated SysDictData data) {
        dictService.addData(data);
        return Result.ok();
    }

    @Put
    @Mapping("/data")
    @OperLog(title = "字典管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:dict:edit")
    @ApiOperation(value = "修改字典数据")
    public Result<Void> updateData(@Body @Validated SysDictData data) {
        dictService.updateData(data);
        return Result.ok();
    }

    @Delete
    @Mapping("/data")
    @OperLog(title = "字典管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:dict:remove")
    @ApiOperation(value = "删除字典数据")
    public Result<Void> removeData(List<Long> ids) {
        dictService.removeData(ids);
        return Result.ok();
    }
}
