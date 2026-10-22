package com.designer.admin.system.controller.admin.message;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.message.MessageConfigQueryDTO;
import com.designer.admin.system.model.dto.message.MessageConfigSaveDTO;
import com.designer.admin.system.model.dto.message.MessageSendDTO;
import com.designer.admin.system.model.entity.message.SysMessageConfig;
import com.designer.admin.system.model.vo.message.MessageChannelTypeVO;
import com.designer.admin.system.model.vo.message.MessageConfigDetailVO;
import com.designer.admin.system.service.message.SysMessageConfigService;
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
 * 消息通道配置（邮件等，可扩展短信/钉钉/微信/飞书）
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/message")
@Api(tags = "消息配置")
public class SysMessageConfigController {

    @Inject
    private SysMessageConfigService messageConfigService;

    @Get
    @Mapping("/types")
    @SaCheckPermission(value = {"system:message:list", "system:msgrecord:send"}, mode = SaMode.OR)
    @ApiOperation(value = "查询消息渠道类型")
    public Result<List<MessageChannelTypeVO>> listTypes() {
        return Result.ok(messageConfigService.listTypes());
    }

    @Get
    @Mapping("/page")
    @SaCheckPermission(value = {"system:message:list", "system:msgrecord:send"}, mode = SaMode.OR)
    @ApiOperation(value = "分页查询消息配置")
    public Result<PageResult<SysMessageConfig>> listPage(MessageConfigQueryDTO query) {
        return Result.ok(PageResult.of(messageConfigService.listPage(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:message:query")
    @ApiOperation(value = "获取消息配置详情")
    public Result<MessageConfigDetailVO> getDetail(Long id) {
        return Result.ok(messageConfigService.getDetailView(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "消息通道", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:message:add")
    @ApiOperation(value = "新增消息配置")
    public Result<Void> add(@Body @Validated MessageConfigSaveDTO dto) {
        messageConfigService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "消息通道", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:message:edit")
    @ApiOperation(value = "修改消息配置")
    public Result<Void> update(@Body @Validated MessageConfigSaveDTO dto) {
        messageConfigService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "消息通道", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:message:remove")
    @ApiOperation(value = "删除消息配置")
    public Result<Void> remove(List<Long> ids) {
        messageConfigService.removeByIds(ids);
        return Result.ok();
    }

    @Put
    @Mapping("/{id}/default")
    @OperLog(title = "消息通道", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:message:edit")
    @ApiOperation(value = "设置默认消息配置")
    public Result<Void> updateDefault(Long id) {
        messageConfigService.updateDefault(id);
        return Result.ok();
    }

    @Put
    @Mapping("/{id}/status")
    @OperLog(title = "消息通道", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:message:edit")
    @ApiOperation(value = "修改消息配置状态")
    public Result<Void> updateStatus(Long id, Integer status) {
        messageConfigService.updateStatus(id, status);
        return Result.ok();
    }

    @Post
    @Mapping("/send")
    @OperLog(title = "消息通道", businessType = BusinessType.OTHER)
    @SaCheckPermission(value = {"system:message:send", "system:msgrecord:send"}, mode = SaMode.OR)
    @ApiOperation(value = "测试发送消息")
    public Result<Void> testSend(@Body @Validated MessageSendDTO dto) {
        messageConfigService.testSend(dto);
        return Result.ok();
    }
}
