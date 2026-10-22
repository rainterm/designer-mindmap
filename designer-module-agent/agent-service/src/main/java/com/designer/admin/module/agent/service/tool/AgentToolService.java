package com.designer.admin.module.agent.service.tool;

import com.designer.admin.module.agent.model.dto.tool.ToolQueryDTO;
import com.designer.admin.module.agent.model.dto.tool.ToolSaveDTO;
import com.designer.admin.module.agent.model.vo.tool.ToolVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

/**
 * 平台工具
 *
 * @author 雨夜终端
 */
public interface AgentToolService {

    /**
     * 工具分页。
     *
     * @param query 分页与名称 / 编码 / 状态筛选条件
     * @return 工具分页结果
     */
    IPage<ToolVO> page(ToolQueryDTO query);

    /**
     * 已启用工具列表（下拉等场景）。
     *
     * @return 已启用工具列表
     */
    List<ToolVO> listEnabled();

    /**
     * 从平台内置工具目录同步创建/刷新工具记录。
     */
    void create();

    /**
     * 修改工具（如启用状态、展示名等）。
     *
     * @param dto 保存参数
     * @return 更新后的工具
     */
    ToolVO update(ToolSaveDTO dto);

    /**
     * 批量删除工具。
     *
     * @param ids 工具 ID 列表
     */
    void remove(List<Long> ids);

}
