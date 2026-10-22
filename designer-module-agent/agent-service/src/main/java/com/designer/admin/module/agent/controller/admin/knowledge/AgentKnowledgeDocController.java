package com.designer.admin.module.agent.controller.admin.knowledge;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeChunkUpdateDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeDocQueryDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeDocSaveDTO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeChunkVO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeDocVO;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeService;
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
import org.noear.solon.web.sse.SseEmitter;

import java.util.List;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.Min;

/**
 * 知识库文档。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/knowledge/doc")
@Api(tags = "知识库文档")
public class AgentKnowledgeDocController {

    @Inject
    private AgentKnowledgeService knowledgeService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:knowledge:list")
    @ApiOperation(value = "分页查询知识库文档")
    public Result<PageResult<KnowledgeDocVO>> page(@Validated KnowledgeDocQueryDTO query) {
        return Result.ok(PageResult.of(knowledgeService.pageDocs(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("agent:knowledge:query")
    @ApiOperation(value = "查询文档详情")
    public Result<KnowledgeDocVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(knowledgeService.getDoc(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "知识库文档", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:knowledge:edit")
    @ApiOperation(value = "新增知识库文档")
    public Result<KnowledgeDocVO> add(@Body @Validated KnowledgeDocSaveDTO dto) {
        return Result.ok(knowledgeService.saveDoc(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "知识库文档", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:knowledge:edit")
    @ApiOperation(value = "修改知识库文档")
    public Result<KnowledgeDocVO> update(@Body @Validated KnowledgeDocSaveDTO dto) {
        return Result.ok(knowledgeService.updateDoc(dto));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "知识库文档", businessType = BusinessType.DELETE)
    @SaCheckPermission("agent:knowledge:edit")
    @ApiOperation(value = "删除知识库文档")
    public Result<Void> remove(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        knowledgeService.removeDocs(ids);
        return Result.ok();
    }

    @Get
    @Mapping("/{id}/chunks")
    @SaCheckPermission("agent:knowledge:list")
    @ApiOperation(value = "查询文档分片列表")
    public Result<List<KnowledgeChunkVO>> chunks(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(knowledgeService.listChunks(id));
    }

    @Put
    @Mapping("/{id}/chunks/{chunkIndex}")
    @OperLog(title = "知识库分块", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:knowledge:edit")
    @ApiOperation(value = "更新文档分片内容")
    public Result<KnowledgeChunkVO> updateChunk(@NotNull(message = "文档ID不能为空") Long id, @NotNull(message = "分块序号不能为空") @Min(value = 0, message = "分块序号无效") Integer chunkIndex, @Body @Validated KnowledgeChunkUpdateDTO dto) {
        return Result.ok(knowledgeService.updateChunk(id, chunkIndex, dto));
    }

    @Post
    @Mapping("/{id}/reindex")
    @OperLog(title = "文档重建索引", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:knowledge:edit")
    @ApiOperation(value = "重建文档索引（异步）")
    public Result<KnowledgeDocVO> reindex(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(knowledgeService.reindex(id));
    }

    @Post
    @Mapping("/{id}/reindex-stream")
    @OperLog(title = "文档重建索引", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:knowledge:edit")
    @ApiOperation(value = "流式重建文档索引（SSE：progress / done / error）")
    public SseEmitter reindexStream(@NotNull(message = "ID不能为空") Long id) {
        return knowledgeService.reindexStream(id);
    }
}
