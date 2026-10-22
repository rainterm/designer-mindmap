package com.designer.admin.module.agent.service.mcp.impl;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.mcp.McpTransportType;
import com.designer.admin.module.agent.mapper.mcp.AgentMcpMapper;
import com.designer.admin.module.agent.model.dto.mcp.McpQueryDTO;
import com.designer.admin.module.agent.model.dto.mcp.McpSaveDTO;
import com.designer.admin.module.agent.model.entity.mcp.AgentMcp;
import com.designer.admin.module.agent.model.vo.mcp.McpVO;
import com.designer.admin.module.agent.service.mcp.AgentMcpService;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.harness.AgentMcpClientSupport;
import com.designer.admin.module.agent.util.AgentEntitySupport;
import com.designer.admin.module.agent.util.AgentJson;
import com.designer.admin.module.agent.util.AgentTexts;

/**
 * MCP 管理
 *
 * @author 雨夜终端
 */
@Component
public class AgentMcpServiceImpl implements AgentMcpService {

    @Db
    private AgentMcpMapper mcpMapper;

    @Override
    public IPage<McpVO> page(McpQueryDTO query) {
        IPage<AgentMcp> page = mcpMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AgentMcp>()
                .and(AgentTexts.notBlank(query.getName()), w -> w
                        .like(AgentMcp::getName, query.getName())
                        .or()
                        .like(AgentMcp::getCode, query.getName()))
                .eq(AgentTexts.notBlank(query.getTransport()), AgentMcp::getTransport, query.getTransport())
                .eq(query.getStatus() != null, AgentMcp::getStatus, query.getStatus())
                .orderByDesc(AgentMcp::getId));
        return page.convert(this::toVo);
    }

    @Override
    public List<McpVO> listEnabled() {
        return mcpMapper.selectList(new LambdaQueryWrapper<AgentMcp>()
                        .eq(AgentMcp::getStatus, SystemConsts.STATUS_ENABLE)
                        .orderByDesc(AgentMcp::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public McpVO save(McpSaveDTO dto) {
        validate(dto, null);
        AgentMcp entity = toEntity(dto);
        if (entity.getStatus() == null) {
            entity.setStatus(SystemConsts.STATUS_ENABLE);
        }
        mcpMapper.insert(entity);
        return toVo(entity);
    }

    @Override
    public McpVO update(McpSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("MCP ID不能为空");
        }
        AgentMcp existing = require(dto.getId());
        validate(dto, dto.getId());
        AgentMcp entity = toEntity(dto);
        entity.setId(existing.getId());
        if (dto.getTools() == null) {
            entity.setToolsJson(existing.getToolsJson());
        }
        mcpMapper.updateById(entity);
        return toVo(mcpMapper.selectById(entity.getId()));
    }

    @Override
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        AgentEntitySupport.deleteBatchIds(mcpMapper, ids);
    }

    @Override
    public McpVO probe(Long id) {
        AgentMcp row = require(id);
        List<Map<String, Object>> tools = AgentMcpClientSupport.listTools(row);
        row.setToolsJson(AgentJson.toJson(tools));
        mcpMapper.updateById(row);
        return toVo(mcpMapper.selectById(row.getId()));
    }

    @Override
    public List<Map<String, Object>> probeDraft(McpSaveDTO dto) {
        if (dto == null) {
            throw new BizException("MCP 配置不能为空");
        }
        return AgentMcpClientSupport.listTools(toEntity(dto));
    }

    private void validate(McpSaveDTO dto, Long excludeId) {
        if (!AgentTexts.notBlank(dto.getName())) {
            throw new BizException("MCP 名称不能为空");
        }
        if (!AgentTexts.notBlank(dto.getCode())) {
            throw new BizException("MCP 编码不能为空");
        }
        Long cnt = mcpMapper.selectCount(new LambdaQueryWrapper<AgentMcp>()
                .eq(AgentMcp::getCode, dto.getCode().trim())
                .ne(excludeId != null, AgentMcp::getId, excludeId));
        AgentEntitySupport.assertCodeUnique(cnt, "MCP 编码已存在");
    }

    private AgentMcp require(Long id) {
        return AgentEntitySupport.require(mcpMapper.selectById(id), "MCP 不存在");
    }

    private AgentMcp toEntity(McpSaveDTO dto) {
        AgentMcp e = new AgentMcp();
        e.setId(dto.getId());
        e.setName(AgentTexts.notBlank(dto.getName()) ? dto.getName().trim() : AgentConsts.DRAFT);
        e.setCode(AgentTexts.notBlank(dto.getCode()) ? dto.getCode().trim() : AgentConsts.DRAFT);
        e.setTransport(AgentTexts.notBlank(dto.getTransport()) ? dto.getTransport().trim() : McpTransportType.STDIO.getCode());
        e.setCommand(dto.getCommand() == null ? "" : dto.getCommand());
        e.setArgs(dto.getArgs() == null ? "" : dto.getArgs());
        e.setUrl(dto.getUrl() == null ? "" : dto.getUrl());
        e.setEnvJson(dto.getEnvJson() == null ? "" : dto.getEnvJson());
        e.setToolsJson(AgentJson.toJson(dto.getTools() == null ? List.of() : dto.getTools()));
        e.setStatus(dto.getStatus());
        e.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return e;
    }

    private McpVO toVo(AgentMcp e) {
        List<Map<String, Object>> tools = AgentJson.parseMapList(e.getToolsJson());
        McpVO vo = new McpVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setCode(e.getCode());
        vo.setTransport(e.getTransport());
        vo.setCommand(e.getCommand());
        vo.setArgs(e.getArgs());
        vo.setUrl(e.getUrl());
        vo.setEnvJson(e.getEnvJson());
        vo.setTools(tools);
        vo.setToolCount(tools.size());
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
