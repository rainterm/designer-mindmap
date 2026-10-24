package com.designer.admin.module.mindmap.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.mindmap.model.dto.CategorySaveDTO;
import com.designer.admin.module.mindmap.model.vo.CategoryVO;
import com.designer.admin.module.mindmap.service.MmCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 思维导图分类管理接口。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/mindmap/category")
@Api(tags = "思维导图-分类")
public class MmCategoryController {

    @Inject
    private MmCategoryService categoryService;

    @Get
    @Mapping("/list")
    @ApiOperation(value = "查询分类树")
    @SaCheckPermission("mindmap:project:list")
    public Result<Map<String, Object>> listTree() {
        List<CategoryVO> categories = categoryService.listTree();
        Map<String, Object> data = new HashMap<>();
        data.put("categories", categories);
        data.put("uncategorizedCount", categoryService.countUncategorized());
        return Result.ok(data);
    }

    @Post
    @Mapping("/add")
    @ApiOperation(value = "新增分类")
    @OperLog(title = "导图分类", businessType = BusinessType.INSERT)
    @SaCheckPermission("mindmap:project:add")
    public Result<CategoryVO> add(@Body @Validated CategorySaveDTO dto) {
        return Result.ok(categoryService.add(dto));
    }

    @Put
    @Mapping("/update")
    @ApiOperation(value = "修改分类")
    @OperLog(title = "导图分类", businessType = BusinessType.UPDATE)
    @SaCheckPermission("mindmap:project:edit")
    public Result<CategoryVO> update(@Body @Validated CategorySaveDTO dto) {
        return Result.ok(categoryService.update(dto));
    }

    @Delete
    @Mapping("/remove/{id}")
    @ApiOperation(value = "删除分类")
    @OperLog(title = "导图分类", businessType = BusinessType.DELETE)
    @SaCheckPermission("mindmap:project:remove")
    public Result<Void> remove(Long id) {
        categoryService.remove(id);
        return Result.ok();
    }
}
