package com.designer.admin.module.agent.controller.admin.knowledge;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeQueryDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeRetrieveTestDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeSaveDTO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeRetrieveTestVO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeVO;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeService;
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
 * 知识库。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/knowledge")
@Api(tags = "知识库")
public class AgentKnowledgeController {

    @Inject
    private AgentKnowledgeService knowledgeService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:knowledge:list")
    @ApiOperation(value = "分页查询知识库")
    public Result<PageResult<KnowledgeVO>> page(KnowledgeQueryDTO query) {
        return Result.ok(PageResult.of(knowledgeService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("agent:knowledge:list")
    @ApiOperation(value = "查询已启用知识库列表")
    public Result<List<KnowledgeVO>> list() {
        return Result.ok(knowledgeService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("agent:knowledge:query")
    @ApiOperation(value = "查询知识库详情")
    public Result<KnowledgeVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(knowledgeService.getById(id));
    }

    @Post
    @Mapping("/{id}/retrieve-test")
    @SaCheckPermission("agent:knowledge:query")
    @ApiOperation(value = "知识库检索测试")
    public Result<KnowledgeRetrieveTestVO> retrieveTest(@NotNull(message = "ID不能为空") Long id, @Body @Validated KnowledgeRetrieveTestDTO dto) {
        return Result.ok(knowledgeService.retrieveTest(id, dto));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "知识库", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:knowledge:add")
    @ApiOperation(value = "新增知识库")
    public Result<KnowledgeVO> add(@Body @Validated KnowledgeSaveDTO dto) {
        return Result.ok(knowledgeService.save(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "知识库", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:knowledge:edit")
    @ApiOperation(value = "修改知识库")
    public Result<KnowledgeVO> update(@Body @Validated KnowledgeSaveDTO dto) {
        return Result.ok(knowledgeService.update(dto));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "知识库", businessType = BusinessType.DELETE)
    @SaCheckPermission("agent:knowledge:remove")
    @ApiOperation(value = "删除知识库")
    public Result<Void> remove(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        knowledgeService.remove(ids);
        return Result.ok();
    }
}
