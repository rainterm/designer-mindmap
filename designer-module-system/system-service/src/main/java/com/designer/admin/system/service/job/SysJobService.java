package com.designer.admin.system.service.job;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.framework.job.ScheduleManager;
import com.designer.admin.system.model.dto.job.JobQueryDTO;
import com.designer.admin.system.model.dto.job.JobSaveDTO;
import com.designer.admin.system.model.entity.job.SysJob;
import com.designer.admin.system.model.vo.job.JobDetailVO;

import java.util.List;

/**
 * 定时任务：CRUD + 启停/立即执行，并与 Quartz 同步
 *
 * @author 雨夜终端
 */
public interface SysJobService {

    /**
     * 应用启动时把库中任务注册到 Quartz（由 {@code @Init} 调用，勿手工触发）。
     */
    void loadJobs();

    /**
     * 定时任务分页。
     *
     * @param query 分页与名称 / 分组 / 状态筛选条件
     * @return 任务分页结果
     */
    IPage<SysJob> listPage(JobQueryDTO query);

    /**
     * 按主键查询任务，不存在时抛出业务异常。
     *
     * @param id 任务 ID
     * @return 任务
     */
    SysJob getById(Long id);

    /**
     * 任务详情视图，附带下次触发时间。
     *
     * @param id 任务 ID
     * @return 详情数据
     */
    JobDetailVO getDetailView(Long id);

    /**
     * 新增任务并注册到 Quartz。
     *
     * @param dto 保存参数
     */
    void add(JobSaveDTO dto);

    /**
     * 修改任务并同步 Quartz 调度。
     *
     * @param dto 保存参数
     */
    void update(JobSaveDTO dto);

    /**
     * 批量删除任务，同时移除 Quartz 调度。
     *
     * @param ids 任务 ID 列表
     */
    void removeByIds(List<Long> ids);

    /**
     * 启用 / 暂停任务，并同步 Quartz 状态。
     *
     * @param id     任务 ID
     * @param status 1=启用，0=暂停
     */
    void updateStatus(Long id, Integer status);

    /**
     * 立即执行一次任务，不影响既有调度计划。
     *
     * @param id 任务 ID
     */
    void runOnce(Long id);
}
