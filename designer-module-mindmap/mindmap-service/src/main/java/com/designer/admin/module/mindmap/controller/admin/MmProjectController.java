package com.designer.admin.module.mindmap.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.mindmap.model.dto.ProjectQueryDTO;
import com.designer.admin.module.mindmap.model.dto.ProjectSaveDTO;
import com.designer.admin.module.mindmap.model.vo.ProjectVO;
import com.designer.admin.module.mindmap.service.MmProjectService;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 思维导图项目管理接口。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/mindmap/project")
@Api(tags = "思维导图-项目")
public class MmProjectController {

    @Inject
    private MmProjectService projectService;

    @Get
    @Mapping("/page")
    @ApiOperation(value = "分页查询导图项目")
    @SaCheckPermission("mindmap:project:list")
    public Result<PageResult<ProjectVO>> listPage(ProjectQueryDTO query) {
        return Result.ok(PageResult.of(projectService.listPage(query)));
    }

    @Get
    @Mapping("/{id}")
    @ApiOperation(value = "查询导图项目详情")
    @SaCheckPermission("mindmap:project:query")
    public Result<ProjectVO> getDetail(Long id) {
        return Result.ok(projectService.getById(id));
    }

    @Post
    @Mapping("/add")
    @ApiOperation(value = "新增导图项目")
    @OperLog(title = "思维导图", businessType = BusinessType.INSERT)
    @SaCheckPermission("mindmap:project:add")
    public Result<ProjectVO> add(@Body @Validated ProjectSaveDTO dto) {
        return Result.ok(projectService.add(dto));
    }

    @Put
    @Mapping("/update")
    @ApiOperation(value = "修改导图项目")
    @OperLog(title = "思维导图", businessType = BusinessType.UPDATE)
    @SaCheckPermission("mindmap:project:edit")
    public Result<ProjectVO> update(@Body @Validated ProjectSaveDTO dto) {
        return Result.ok(projectService.update(dto));
    }

    @Delete
    @Mapping("/remove")
    @ApiOperation(value = "删除导图项目")
    @OperLog(title = "思维导图", businessType = BusinessType.DELETE)
    @SaCheckPermission("mindmap:project:remove")
    public Result<Void> remove(String ids) {
        projectService.remove(parseIds(ids));
        return Result.ok();
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
