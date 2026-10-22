package com.designer.admin.module.agent.controller.admin.provider;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.agent.model.dto.provider.ProviderFetchModelsDTO;
import com.designer.admin.module.agent.model.dto.provider.ProviderQueryDTO;
import com.designer.admin.module.agent.model.dto.provider.ProviderSaveDTO;
import com.designer.admin.module.agent.model.vo.provider.ProviderVO;
import com.designer.admin.module.agent.service.provider.AgentProviderService;
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
import java.util.Map;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.validation.annotation.Validated;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.NotEmpty;

/**
 * 模型供应商。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/provider")
@Api(tags = "模型供应商")
public class AgentProviderController {

    @Inject
    private AgentProviderService providerService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:provider:list")
    @ApiOperation(value = "分页查询模型供应商")
    public Result<PageResult<ProviderVO>> page(ProviderQueryDTO query) {
        return Result.ok(PageResult.of(providerService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("agent:provider:list")
    @ApiOperation(value = "查询已启用供应商列表")
    public Result<List<ProviderVO>> list() {
        return Result.ok(providerService.listEnabled());
    }

    @Get
    @Mapping("/vendors")
    @SaCheckPermission("agent:provider:list")
    @ApiOperation(value = "查询 Solon 方言预设（新建/编辑下拉）")
    public Result<List<Map<String, Object>>> vendors() {
        return Result.ok(providerService.listVendors());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("agent:provider:query")
    @ApiOperation(value = "查询供应商详情")
    public Result<ProviderVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(providerService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "模型供应商", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:provider:add")
    @ApiOperation(value = "新增供应商")
    public Result<ProviderVO> add(@Body @Validated ProviderSaveDTO dto) {
        return Result.ok(providerService.save(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "模型供应商", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:provider:edit")
    @ApiOperation(value = "修改供应商")
    public Result<ProviderVO> update(@Body @Validated ProviderSaveDTO dto) {
        return Result.ok(providerService.update(dto));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "模型供应商", businessType = BusinessType.DELETE)
    @SaCheckPermission("agent:provider:remove")
    @ApiOperation(value = "删除供应商")
    public Result<Void> remove(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        providerService.remove(ids);
        return Result.ok();
    }

    @Post
    @Mapping("/fetch-models")
    @SaCheckPermission("agent:provider:list")
    @ApiOperation(value = "从远端拉取模型列表（OpenAI /v1/models 或 Ollama /api/tags）")
    public Result<List<Map<String, Object>>> fetchModels(@Body @Validated ProviderFetchModelsDTO dto) {
        return Result.ok(providerService.fetchModels(dto));
    }
}
