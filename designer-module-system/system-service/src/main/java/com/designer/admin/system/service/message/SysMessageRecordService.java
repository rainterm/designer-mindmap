package com.designer.admin.system.service.message;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.framework.message.MessagePayload;
import com.designer.admin.framework.message.MessageRecordWriter;
import com.designer.admin.framework.message.MessageRuntimeConfig;
import com.designer.admin.system.model.dto.message.MessageRecordQueryDTO;
import com.designer.admin.system.model.entity.message.SysMessageRecord;

import java.util.List;

/**
 * 消息发送记录；实现 framework {@link MessageRecordWriter}。
 *
 * @author 雨夜终端
 */
public interface SysMessageRecordService {

    /**
     * 消息发送记录分页。
     *
     * @param query 分页与通道 / 标题 / 状态筛选条件
     * @return 发送记录分页结果
     */
    IPage<SysMessageRecord> listPage(MessageRecordQueryDTO query);

    /**
     * 批量删除发送记录。
     *
     * @param ids 记录 ID 列表
     */
    void removeByIds(List<Long> ids);

    /**
     * 清空全部发送记录。
     */
    void removeAll();

    /**
     * 删除指定天数之前的发送记录。
     *
     * @param days 保留天数，最小按 1 天处理
     * @return 删除条数
     */
    int removeOlderThan(int days);

    /**
     * 异步写入一条发送记录，不阻断消息发送主流程。
     *
     * @param config   本次发送使用的运行时配置
     * @param payload  消息内容
     * @param success  是否发送成功
     * @param errorMsg 失败原因，成功时可为空
     * @param costTime 发送耗时（毫秒）
     */
    void recordAsync(MessageRuntimeConfig config, MessagePayload payload,
        boolean success, String errorMsg, long costTime);
}
