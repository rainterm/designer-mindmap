package com.designer.admin.system.service.file;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.framework.storage.StorageConfigSource;
import com.designer.admin.framework.storage.StorageRuntimeConfig;
import com.designer.admin.system.model.dto.file.StorageConfigQueryDTO;
import com.designer.admin.system.model.dto.file.StorageConfigSaveDTO;
import com.designer.admin.system.model.entity.file.SysStorageConfig;
import com.designer.admin.system.model.vo.file.StorageConfigDetailVO;
import com.designer.admin.system.model.vo.file.StorageOptionVO;
import com.designer.admin.system.model.vo.file.StorageTypeVO;

import java.util.List;

/**
 * 文件存储配置：CRUD + 默认配置缓存 + 运行时解析；实现 framework {@link StorageConfigSource}。
 *
 * @author 雨夜终端
 */
public interface SysStorageConfigService {

    /**
     * 存储配置分页，密钥字段脱敏返回。
     *
     * @param query 分页与名称 / 类型 / 状态筛选条件
     * @return 配置分页结果
     */
    IPage<SysStorageConfig> listPage(StorageConfigQueryDTO query);

    /**
     * 按主键查询存储配置，不存在时抛出业务异常。
     *
     * @param id 配置 ID
     * @return 存储配置
     */
    SysStorageConfig getById(Long id);

    /**
     * 存储配置详情视图（密钥脱敏），供编辑页回显。
     *
     * @param id 配置 ID
     * @return 详情数据
     */
    StorageConfigDetailVO getDetailView(Long id);

    /**
     * 列出可用的存储类型，以及该类型需要的配置字段。
     *
     * @return 存储类型列表
     */
    List<StorageTypeVO> listTypes();

    /**
     * 新增存储配置；首个启用配置自动成为默认。
     *
     * @param dto 保存参数
     */
    void add(StorageConfigSaveDTO dto);

    /**
     * 修改存储配置并清理缓存。
     *
     * @param dto 保存参数
     */
    void update(StorageConfigSaveDTO dto);

    /**
     * 批量删除存储配置；默认配置不允许删除。
     *
     * @param ids 配置 ID 列表
     */
    void removeByIds(List<Long> ids);

    /**
     * 把指定配置设为默认存储。
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
     * 取默认存储的运行时配置（带缓存），无启用配置时抛出业务异常。
     *
     * @return 运行时配置
     */
    StorageRuntimeConfig requireDefaultRuntime();

    /**
     * 按配置 ID 取运行时配置。
     *
     * @param configId 配置 ID；为空时取默认配置
     * @return 运行时配置
     */
    StorageRuntimeConfig requireRuntimeById(Long configId);

    /**
     * 公开存储配置下拉：只列启用且公开的配置，默认项排在最前。
     *
     * @return 存储配置选项
     */
    List<StorageOptionVO> listPublicOptions();

    /**
     * 取上传用的运行时配置，可指定配置并要求其公开。
     *
     * @param configId      配置 ID；null 表示默认配置
     * @param requirePublic 是否要求配置为公开
     * @return 运行时配置
     */
    StorageRuntimeConfig requireUploadRuntime(Long configId, boolean requirePublic);

    /**
     * 把配置实体转换为运行时配置。
     *
     * @param cfg 配置实体
     * @return 运行时配置
     */
    StorageRuntimeConfig toRuntime(SysStorageConfig cfg);

    /**
     * 清除存储配置缓存。
     */
    void clearCache();
}
