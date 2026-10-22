package com.designer.admin.module.agent.service.mcp;

import com.designer.admin.module.agent.model.dto.mcp.McpQueryDTO;
import com.designer.admin.module.agent.model.dto.mcp.McpSaveDTO;
import com.designer.admin.module.agent.model.vo.mcp.McpVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.Map;

/**
 * MCP 管理
 *
 * @author 雨夜终端
 */
public interface AgentMcpService {

    /**
     * MCP 分页。
     *
     * @param query 分页与名称 / 编码 / 状态筛选条件
     * @return MCP 分页结果
     */
    IPage<McpVO> page(McpQueryDTO query);

    /**
     * 已启用 MCP 列表（下拉等场景）。
     *
     * @return 已启用 MCP 列表
     */
    List<McpVO> listEnabled();

    /**
     * 新增 MCP。
     *
     * @param dto 保存参数
     * @return 新建后的 MCP
     */
    McpVO save(McpSaveDTO dto);

    /**
     * 修改 MCP。
     *
     * @param dto 保存参数
     * @return 更新后的 MCP
     */
    McpVO update(McpSaveDTO dto);

    /**
     * 批量删除 MCP。
     *
     * @param ids MCP ID 列表
     */
    void remove(List<Long> ids);

    /**
     * 连接已保存的 MCP，拉取工具清单并落库。
     *
     * @param id MCP ID
     * @return 探测后的 MCP（含工具清单）
     */
    McpVO probe(Long id);

    /**
     * 按表单配置临时连接并拉取工具（不落库，用于新增/编辑预览）。
     *
     * @param dto 连接配置
     * @return 工具清单（不落库）
     */
    List<Map<String, Object>> probeDraft(McpSaveDTO dto);

}
