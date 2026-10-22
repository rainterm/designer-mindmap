package com.designer.admin.module.agent.service.skill.impl;

import java.util.List;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.skill.SkillSourceType;
import com.designer.admin.module.agent.mapper.skill.AgentSkillMapper;
import com.designer.admin.module.agent.model.dto.skill.SkillImportDTO;
import com.designer.admin.module.agent.model.dto.skill.SkillQueryDTO;
import com.designer.admin.module.agent.model.dto.skill.SkillSaveDTO;
import com.designer.admin.module.agent.model.entity.skill.AgentSkill;
import com.designer.admin.module.agent.model.vo.skill.SkillVO;
import com.designer.admin.module.agent.service.skill.AgentSkillService;
import com.designer.admin.module.agent.util.AgentEntitySupport;
import com.designer.admin.module.agent.util.AgentJson;
import com.designer.admin.module.agent.util.AgentTexts;

/**
 * Skill 管理
 *
 * @author 雨夜终端
 */
@Component
public class AgentSkillServiceImpl implements AgentSkillService {

    @Db
    private AgentSkillMapper skillMapper;

    @Override
    public IPage<SkillVO> page(SkillQueryDTO query) {
        IPage<AgentSkill> page = skillMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AgentSkill>()
                .and(AgentTexts.notBlank(query.getName()), w -> w
                        .like(AgentSkill::getName, query.getName())
                        .or()
                        .like(AgentSkill::getCode, query.getName()))
                .eq(query.getStatus() != null, AgentSkill::getStatus, query.getStatus())
                .orderByDesc(AgentSkill::getId));
        return page.convert(this::toVo);
    }

    @Override
    public List<SkillVO> listEnabled() {
        return skillMapper.selectList(new LambdaQueryWrapper<AgentSkill>()
                        .eq(AgentSkill::getStatus, SystemConsts.STATUS_ENABLE)
                        .orderByDesc(AgentSkill::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public SkillVO getById(Long id) {
        return toVo(require(id));
    }

    @Override
    public SkillVO save(SkillSaveDTO dto) {
        validate(dto, null);
        AgentSkill entity = toEntity(dto);
        if (entity.getStatus() == null) {
            entity.setStatus(SystemConsts.STATUS_ENABLE);
        }
        skillMapper.insert(entity);
        return toVo(entity);
    }

    @Override
    public SkillVO update(SkillSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("Skill ID不能为空");
        }
        require(dto.getId());
        validate(dto, dto.getId());
        AgentSkill entity = toEntity(dto);
        skillMapper.updateById(entity);
        return toVo(skillMapper.selectById(entity.getId()));
    }

    @Override
    public SkillVO importPackage(SkillImportDTO payload) {
        if (payload == null || (!AgentTexts.notBlank(payload.getContent()) && !AgentTexts.notBlank(payload.getPackageName()))) {
            throw new BizException("压缩包无效");
        }
        if (!AgentTexts.notBlank(payload.getCode())) {
            throw new BizException("Skill 编码不能为空");
        }
        AgentSkill exists = skillMapper.selectOne(new LambdaQueryWrapper<AgentSkill>()
                .eq(AgentSkill::getCode, payload.getCode().trim())
                .last("LIMIT 1"));
        if (exists != null) {
            SkillSaveDTO dto = new SkillSaveDTO();
            dto.setId(exists.getId());
            dto.setName(AgentTexts.notBlank(payload.getName()) ? payload.getName() : exists.getName());
            dto.setCode(payload.getCode());
            dto.setDescription(AgentTexts.notBlank(payload.getDescription()) ? payload.getDescription() : exists.getDescription());
            dto.setContent(AgentTexts.notBlank(payload.getContent()) ? payload.getContent() : exists.getContent());
            dto.setStatus(exists.getStatus());
            dto.setSourceType(SkillSourceType.PACKAGE.getCode());
            dto.setPackageName(payload.getPackageName());
            dto.setPackageSize(payload.getPackageSize());
            dto.setFiles(payload.getFiles());
            dto.setSkillPath(payload.getSkillPath());
            return update(dto);
        }
        SkillSaveDTO dto = new SkillSaveDTO();
        dto.setName(payload.getName());
        dto.setCode(payload.getCode());
        dto.setDescription(payload.getDescription());
        dto.setContent(payload.getContent());
        dto.setSourceType(SkillSourceType.PACKAGE.getCode());
        dto.setPackageName(payload.getPackageName());
        dto.setPackageSize(payload.getPackageSize());
        dto.setFiles(payload.getFiles());
        dto.setSkillPath(payload.getSkillPath());
        dto.setStatus(SystemConsts.STATUS_ENABLE);
        return save(dto);
    }

    @Override
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        AgentEntitySupport.deleteBatchIds(skillMapper, ids);
    }

    private void validate(SkillSaveDTO dto, Long excludeId) {
        if (!AgentTexts.notBlank(dto.getName())) {
            throw new BizException("Skill 名称不能为空");
        }
        if (!AgentTexts.notBlank(dto.getCode())) {
            throw new BizException("Skill 编码不能为空");
        }
        Long cnt = skillMapper.selectCount(new LambdaQueryWrapper<AgentSkill>()
                .eq(AgentSkill::getCode, dto.getCode().trim())
                .ne(excludeId != null, AgentSkill::getId, excludeId));
        AgentEntitySupport.assertCodeUnique(cnt, "Skill 编码已存在");
    }

    private AgentSkill require(Long id) {
        return AgentEntitySupport.require(skillMapper.selectById(id), "Skill 不存在");
    }

    private AgentSkill toEntity(SkillSaveDTO dto) {
        AgentSkill e = new AgentSkill();
        e.setId(dto.getId());
        e.setName(dto.getName().trim());
        e.setCode(dto.getCode().trim());
        e.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        e.setContent(dto.getContent() == null ? "" : dto.getContent());
        String packageName = dto.getPackageName() == null ? "" : dto.getPackageName();
        e.setSourceType(AgentTexts.notBlank(dto.getSourceType())
                ? dto.getSourceType()
                : (AgentTexts.notBlank(packageName) ? SkillSourceType.PACKAGE.getCode() : SkillSourceType.MANUAL.getCode()));
        e.setPackageName(packageName);
        e.setPackageSize(dto.getPackageSize() == null ? 0L : dto.getPackageSize());
        e.setFilesJson(AgentJson.toJson(dto.getFiles() == null ? List.of() : dto.getFiles()));
        e.setSkillPath(dto.getSkillPath() == null ? "" : dto.getSkillPath());
        e.setStatus(dto.getStatus());
        return e;
    }

    private SkillVO toVo(AgentSkill e) {
        SkillVO vo = new SkillVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setCode(e.getCode());
        vo.setDescription(e.getDescription());
        vo.setContent(e.getContent());
        vo.setSourceType(e.getSourceType());
        vo.setPackageName(e.getPackageName());
        vo.setPackageSize(e.getPackageSize());
        vo.setFiles(AgentJson.parseStringList(e.getFilesJson()));
        vo.setSkillPath(e.getSkillPath());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
