package com.designer.admin.system.controller.admin.message;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.message.MessageRecordQueryDTO;
import com.designer.admin.system.model.entity.message.SysMessageRecord;
import com.designer.admin.system.service.message.SysMessageRecordService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

import java.util.List;

/**
 * 消息发送记录
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/msgrecord")
@Api(tags = "消息记录")
public class SysMessageRecordController {

    @Inject
    private SysMessageRecordService messageRecordService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:msgrecord:list")
    @ApiOperation(value = "分页查询消息记录")
    public Result<PageResult<SysMessageRecord>> listPage(MessageRecordQueryDTO query) {
        return Result.ok(PageResult.of(messageRecordService.listPage(query)));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "消息记录", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:msgrecord:remove")
    @ApiOperation(value = "删除消息记录")
    public Result<Void> remove(List<Long> ids) {
        messageRecordService.removeByIds(ids);
        return Result.ok();
    }

    @Delete
    @Mapping("/clear")
    @OperLog(title = "消息记录", businessType = BusinessType.CLEAN)
    @SaCheckPermission("system:msgrecord:remove")
    @ApiOperation(value = "清空消息记录")
    public Result<Void> removeAll() {
        messageRecordService.removeAll();
        return Result.ok();
    }
}
