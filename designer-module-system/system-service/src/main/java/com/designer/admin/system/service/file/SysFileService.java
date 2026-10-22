package com.designer.admin.system.service.file;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.file.FileQueryDTO;
import com.designer.admin.system.model.dto.file.FileUploadDTO;
import com.designer.admin.system.model.entity.file.SysFile;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;

import java.util.List;

/**
 * 系统文件：上传落库、分页查询、删除（含物理文件）
 *
 * @author 雨夜终端
 */
public interface SysFileService {

    /**
     * 文件记录分页。
     *
     * @param query 分页与文件名 / 类型 / 时间筛选条件
     * @return 文件分页结果
     */
    IPage<SysFile> listPage(FileQueryDTO query);

    /**
     * 按主键查询文件元数据，不存在时抛出业务异常。
     *
     * @param id 文件 ID
     * @return 文件元数据
     */
    SysFile getById(Long id);

    /**
     * 上传文件并写入元数据，可指定业务目录与存储配置。
     *
     * @param file 上传的文件
     * @param dto  业务目录 / 存储配置 / 是否要求公开
     * @return 文件元数据
     */
    SysFile upload(UploadedFile file, FileUploadDTO dto);

    /**
     * 批量删除文件记录及其物理文件。
     *
     * @param ids 文件 ID 列表
     */
    void removeByIds(List<Long> ids);

    /**
     * 输出文件流（下载 / 预览）。
     *
     * @param ctx        当前请求上下文
     * @param publicOnly true：公开下载（/app-api），仅允许公开存储配置下的文件匿名访问；
     *                   false：私有下载（/admin-api），由 Sa-Token 保证已登录
     * @throws Exception 读写文件失败
     */
    void download(Context ctx, boolean publicOnly) throws Exception;
}
