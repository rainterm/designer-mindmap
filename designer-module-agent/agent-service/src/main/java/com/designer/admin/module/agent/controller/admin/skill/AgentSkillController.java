package com.designer.admin.module.agent.controller.admin.skill;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.agent.model.dto.skill.SkillImportDTO;
import com.designer.admin.module.agent.model.dto.skill.SkillQueryDTO;
import com.designer.admin.module.agent.model.dto.skill.SkillSaveDTO;
import com.designer.admin.module.agent.model.vo.skill.SkillVO;
import com.designer.admin.module.agent.service.skill.AgentSkillService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;

import java.util.List;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.validation.annotation.Validated;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.NotEmpty;

/**
 * Skill 管理。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/skill")
@Api(tags = "Skill 管理")
public class AgentSkillController {

    @Inject
    private AgentSkillService skillService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:skill:list")
    @ApiOperation(value = "分页查询 Skill")
    public Result<PageResult<SkillVO>> page(SkillQueryDTO query) {
        return Result.ok(PageResult.of(skillService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("agent:skill:list")
    @ApiOperation(value = "查询已启用 Skill 列表")
    public Result<List<SkillVO>> list() {
        return Result.ok(skillService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("agent:skill:query")
    @ApiOperation(value = "查询 Skill 详情")
    public Result<SkillVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(skillService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "Skill", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:skill:add")
    @ApiOperation(value = "新增 Skill")
    public Result<SkillVO> add(@Body @Validated SkillSaveDTO dto) {
        return Result.ok(skillService.save(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "Skill", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:skill:edit")
    @ApiOperation(value = "修改 Skill")
    public Result<SkillVO> update(@Body @Validated SkillSaveDTO dto) {
        return Result.ok(skillService.update(dto));
    }

    @Post
    @Mapping("/import")
    @OperLog(title = "Skill 导入", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:skill:add")
    @ApiOperation(value = "导入 Skill 包（解析 SKILL.md）")
    public Result<SkillVO> importPackage(@Body @Validated SkillImportDTO dto) {
        return Result.ok(skillService.importPackage(dto));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "Skill", businessType = BusinessType.DELETE)
    @SaCheckPermission("agent:skill:remove")
    @ApiOperation(value = "删除 Skill")
    public Result<Void> remove(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        skillService.remove(ids);
        return Result.ok();
    }
}
