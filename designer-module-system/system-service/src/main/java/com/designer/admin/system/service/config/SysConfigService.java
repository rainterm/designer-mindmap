package com.designer.admin.system.service.config;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.config.ConfigQueryDTO;
import com.designer.admin.system.model.dto.config.ConfigSaveDTO;
import com.designer.admin.system.model.entity.config.SysConfig;

import java.util.List;

/**
 * 参数配置：CRUD + Redis 缓存按键取值
 *
 * @author 雨夜终端
 */
public interface SysConfigService {

    /**
     * 参数配置分页。
     *
     * @param query 分页与名称 / 键名 / 类型筛选条件
     * @return 参数配置分页结果
     */
    IPage<SysConfig> listPage(ConfigQueryDTO query);

    /**
     * 按主键查询参数配置，不存在时抛出业务异常。
     *
     * @param id 参数配置 ID
     * @return 参数配置
     */
    SysConfig getById(Long id);

    /**
     * 按键取值，优先读 Redis 缓存。
     *
     * @param configKey 参数键名
     * @return 参数值；不存在时返回 null
     */
    String getValue(String configKey);

    /**
     * 按键取布尔值。
     *
     * @param configKey    参数键名
     * @param defaultValue 未配置或无法解析时返回的默认值
     * @return 布尔值
     */
    boolean getBool(String configKey, boolean defaultValue);

    /**
     * 按键取整数值。
     *
     * @param configKey    参数键名
     * @param defaultValue 未配置或无法解析时返回的默认值
     * @return 整数值
     */
    int getInt(String configKey, int defaultValue);

    /**
     * Token 绝对有效期（秒），非法或过小时回退默认值。
     *
     * @return 有效期秒数
     */
    long getTokenTimeoutSeconds();

    /**
     * Token 活跃续期窗口（秒）。
     *
     * <p>-1 表示关闭活跃续期；其它非法或过小的值回退默认 1800 秒。</p>
     *
     * @return 活跃续期秒数
     */
    long getTokenActiveTimeoutSeconds();

    /**
     * 新增参数配置，并校验 configKey 全局唯一。
     *
     * @param dto 保存参数
     */
    void add(ConfigSaveDTO dto);

    /**
     * 修改参数配置，并失效对应缓存。
     *
     * @param dto 保存参数
     */
    void update(ConfigSaveDTO dto);

    /**
     * 批量删除参数配置。
     *
     * @param ids 待删除的配置 ID 列表
     */
    void remove(List<Long> ids);

    /**
     * 清除指定参数的 Redis 缓存。
     *
     * @param configKey 参数键名
     */
    void clearCache(String configKey);

    /**
     * 清空全部参数缓存，后续读取时回源数据库。
     */
    void refreshCache();
}
