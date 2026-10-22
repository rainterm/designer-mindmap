package com.designer.admin.system.service.message;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.framework.message.MessageConfigSource;
import com.designer.admin.framework.message.MessageRuntimeConfig;
import com.designer.admin.system.model.dto.message.MessageConfigQueryDTO;
import com.designer.admin.system.model.dto.message.MessageConfigSaveDTO;
import com.designer.admin.system.model.dto.message.MessageSendDTO;
import com.designer.admin.system.model.entity.message.SysMessageConfig;
import com.designer.admin.system.model.vo.message.MessageChannelTypeVO;
import com.designer.admin.system.model.vo.message.MessageConfigDetailVO;

import java.util.List;

/**
 * 消息通道配置：CRUD + 按通道类型的默认配置 + 测试发送；实现 framework {@link MessageConfigSource}。
 *
 * @author 雨夜终端
 */
public interface SysMessageConfigService {

    /**
     * 消息通道配置分页，密钥字段脱敏返回。
     *
     * @param query 分页与名称 / 通道类型 / 状态筛选条件
     * @return 配置分页结果
     */
    IPage<SysMessageConfig> listPage(MessageConfigQueryDTO query);

    /**
     * 配置详情视图（密钥脱敏），供编辑页回显。
     *
     * @param id 配置 ID
     * @return 详情数据
     */
    MessageConfigDetailVO getDetailView(Long id);

    /**
     * 列出已注册的消息通道类型及其配置字段元信息（站内通知置前）。
     *
     * @return 通道类型列表
     */
    List<MessageChannelTypeVO> listTypes();

    /**
     * 新增通道配置；该通道类型的首个启用配置自动成为默认。
     *
     * @param dto 保存参数
     */
    void add(MessageConfigSaveDTO dto);

    /**
     * 修改通道配置并清理相关缓存。
     *
     * @param dto 保存参数
     */
    void update(MessageConfigSaveDTO dto);

    /**
     * 批量删除通道配置；默认配置不允许删除。
     *
     * @param ids 配置 ID 列表
     */
    void removeByIds(List<Long> ids);

    /**
     * 把指定配置设为该通道类型的默认项。
     *
     * @param id 配置 ID
     */
    void updateDefault(Long id);

    /**
     * 启用 / 停用配置；默认配置不允许停用。
     *
     * @param id     配置 ID
     * @param status 1=启用，0=停用
     */
    void updateStatus(Long id, Integer status);

    /**
     * 使用指定配置或通道类型的默认配置发送一条测试消息。
     *
     * @param dto 测试发送参数（收件人必填）
     */
    void testSend(MessageSendDTO dto);

    /**
     * 取通道类型的默认运行时配置（带本地缓存）。
     *
     * @param channelType 通道类型
     * @return 运行时配置
     */
    MessageRuntimeConfig requireDefaultRuntime(String channelType);

    /**
     * 按配置 ID 取运行时配置。
     *
     * @param configId 配置 ID
     * @return 运行时配置
     */
    MessageRuntimeConfig requireRuntimeById(Long configId);

    /**
     * 把配置实体转换为运行时配置。
     *
     * @param cfg 配置实体
     * @return 运行时配置
     */
    MessageRuntimeConfig toRuntime(SysMessageConfig cfg);

    /**
     * 清除指定通道类型的默认配置缓存。
     *
     * @param channelType 通道类型
     */
    void clearCache(String channelType);
}
