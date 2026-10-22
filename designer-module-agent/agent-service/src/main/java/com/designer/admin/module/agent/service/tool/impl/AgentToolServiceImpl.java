package com.designer.admin.module.agent.service.tool.impl;

import java.util.List;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.mapper.tool.AgentToolMapper;
import com.designer.admin.module.agent.model.dto.tool.ToolQueryDTO;
import com.designer.admin.module.agent.model.dto.tool.ToolSaveDTO;
import com.designer.admin.module.agent.model.entity.tool.AgentTool;
import com.designer.admin.module.agent.model.vo.tool.ToolVO;
import com.designer.admin.module.agent.service.tool.AgentToolService;
import com.designer.admin.module.agent.util.AgentTexts;

/**
 * 平台内置工具。
 *
 * @author 雨夜终端
 */
@Component
public class AgentToolServiceImpl implements AgentToolService {

    @Db
    private AgentToolMapper toolMapper;

    @Override
    public IPage<ToolVO> page(ToolQueryDTO query) {
        return toolMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AgentTool>()
                .and(AgentTexts.notBlank(query.getName()), w -> w
                        .like(AgentTool::getName, query.getName())
                        .or()
                        .like(AgentTool::getCode, query.getName()))
                .eq(AgentTexts.notBlank(query.getType()), AgentTool::getType, query.getType())
                .eq(query.getStatus() != null, AgentTool::getStatus, query.getStatus())
                .orderByAsc(AgentTool::getId))
                .convert(this::toVo);
    }

    @Override
    public List<ToolVO> listEnabled() {
        return toolMapper.selectList(new LambdaQueryWrapper<AgentTool>()
                        .eq(AgentTool::getStatus, SystemConsts.STATUS_ENABLE)
                        .orderByAsc(AgentTool::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public void create() {
        throw new BizException("工具为平台内置，不可手动新增");
    }

    @Override
    public ToolVO update(ToolSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("工具ID不能为空");
        }
        AgentTool existing = toolMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException("工具不存在");
        }
        existing.setStatus(dto.getStatus());
        toolMapper.updateById(existing);
        return toVo(existing);
    }

    @Override
    public void remove(List<Long> ids) {
        throw new BizException("内置工具不可删除，只能停用");
    }

    private ToolVO toVo(AgentTool e) {
        ToolVO vo = new ToolVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setCode(e.getCode());
        vo.setType(e.getType());
        vo.setDescription(e.getDescription());
        vo.setSchemaJson(e.getSchemaJson());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
