package com.designer.admin.module.mindmap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.mindmap.mapper.MmCategoryMapper;
import com.designer.admin.module.mindmap.mapper.MmProjectMapper;
import com.designer.admin.module.mindmap.model.dto.ProjectQueryDTO;
import com.designer.admin.module.mindmap.model.dto.ProjectSaveDTO;
import com.designer.admin.module.mindmap.model.entity.MmCategory;
import com.designer.admin.module.mindmap.model.entity.MmProject;
import com.designer.admin.module.mindmap.model.vo.ProjectVO;
import com.designer.admin.module.mindmap.service.MmCategoryService;
import com.designer.admin.module.mindmap.service.MmProjectService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 思维导图项目服务实现。
 *
 * @author 雨夜终端
 */
@Component
public class MmProjectServiceImpl implements MmProjectService {

    private static final String DEFAULT_NAME = "未命名导图";
    private static final String DEFAULT_LAYOUT = "logicalStructure";
    private static final String DEFAULT_THEME = "default";
    /** simple-mind-map 默认根节点树 */
    private static final String DEFAULT_CONTENT = "{\"data\":{\"text\":\"中心主题\"},\"children\":[]}";

    @Db
    private MmProjectMapper projectMapper;
    @Db
    private MmCategoryMapper categoryMapper;
    @Inject
    private MmCategoryService categoryService;

    @Override
    public IPage<ProjectVO> listPage(ProjectQueryDTO query) {
        LambdaQueryWrapper<MmProject> wrapper = new LambdaQueryWrapper<MmProject>()
                .select(MmProject::getId, MmProject::getCategoryId, MmProject::getName, MmProject::getLayout,
                        MmProject::getTheme, MmProject::getRemark, MmProject::getCreateBy, MmProject::getCreateTime,
                        MmProject::getUpdateBy, MmProject::getUpdateTime, MmProject::getDeleted)
                .like(Utils.isNotBlank(query.getName()), MmProject::getName, query.getName())
                .orderByDesc(MmProject::getId);

        Long categoryId = query.getCategoryId();
        if (categoryId != null) {
            if (categoryId <= 0) {
                wrapper.and(w -> w.isNull(MmProject::getCategoryId).or().eq(MmProject::getCategoryId, 0L));
            } else {
                List<Long> ids = categoryService.listSelfAndDescendantIds(categoryId);
                if (ids.isEmpty()) {
                    wrapper.eq(MmProject::getCategoryId, categoryId);
                } else {
                    wrapper.in(MmProject::getCategoryId, ids);
                }
            }
        }

        IPage<MmProject> page = projectMapper.selectPage(query.toPage(), wrapper);
        Map<Long, String> nameMap = loadCategoryNames(page.getRecords());
        return page.convert(entity -> toVo(entity, false, nameMap));
    }

    @Override
    public ProjectVO getById(Long id) {
        MmProject entity = require(id);
        return toVo(entity, true, loadCategoryNames(List.of(entity)));
    }

    @Override
    @Transaction
    public ProjectVO add(ProjectSaveDTO dto) {
        ProjectSaveDTO saveDto = dto == null ? new ProjectSaveDTO() : dto;
        MmProject entity = new MmProject();
        entity.setCategoryId(normalizeCategoryId(saveDto));
        entity.setName(Utils.valueOr(saveDto.getName(), DEFAULT_NAME).trim());
        entity.setLayout(Utils.valueOr(saveDto.getLayout(), DEFAULT_LAYOUT).trim());
        entity.setTheme(Utils.valueOr(saveDto.getTheme(), DEFAULT_THEME).trim());
        entity.setContent(Utils.valueOr(saveDto.getContent(), DEFAULT_CONTENT));
        entity.setRemark(saveDto.getRemark() == null ? "" : saveDto.getRemark());
        projectMapper.insert(entity);
        return toVo(entity, true, loadCategoryNames(List.of(entity)));
    }

    @Override
    @Transaction
    public ProjectVO update(ProjectSaveDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BizException("导图项目 ID 不能为空");
        }
        MmProject exists = require(dto.getId());
        MmProject entity = new MmProject();
        entity.setId(dto.getId());
        entity.setName(Utils.valueOr(dto.getName(), exists.getName()).trim());
        entity.setLayout(Utils.valueOr(dto.getLayout(), exists.getLayout()).trim());
        entity.setTheme(Utils.valueOr(dto.getTheme(), exists.getTheme()).trim());
        if (dto.getContent() != null) {
            entity.setContent(Utils.isBlank(dto.getContent()) ? DEFAULT_CONTENT : dto.getContent());
        }
        if (dto.getRemark() != null) {
            entity.setRemark(dto.getRemark());
        }
        projectMapper.updateById(entity);

        if (Boolean.TRUE.equals(dto.getClearCategory())) {
            projectMapper.update(null, new LambdaUpdateWrapper<MmProject>()
                    .eq(MmProject::getId, dto.getId())
                    .set(MmProject::getCategoryId, null));
        } else if (dto.getCategoryId() != null) {
            Long categoryId = normalizeCategoryId(dto);
            projectMapper.update(null, new LambdaUpdateWrapper<MmProject>()
                    .eq(MmProject::getId, dto.getId())
                    .set(MmProject::getCategoryId, categoryId));
        }

        return toVo(require(dto.getId()), true, null);
    }

    @Override
    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        projectMapper.deleteByIds(ids);
    }

    private Long normalizeCategoryId(ProjectSaveDTO dto) {
        if (Boolean.TRUE.equals(dto.getClearCategory()) || dto.getCategoryId() == null || dto.getCategoryId() <= 0) {
            return null;
        }
        MmCategory category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BizException("分类不存在");
        }
        return category.getId();
    }

    private MmProject require(Long id) {
        MmProject row = projectMapper.selectById(id);
        if (row == null) {
            throw new BizException("导图项目不存在");
        }
        return row;
    }

    private Map<Long, String> loadCategoryNames(List<MmProject> projects) {
        Map<Long, String> map = new HashMap<>();
        if (projects == null || projects.isEmpty()) {
            return map;
        }
        List<Long> ids = projects.stream()
                .map(MmProject::getCategoryId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return map;
        }
        List<MmCategory> categories = categoryMapper.selectByIds(ids);
        for (MmCategory category : categories) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }

    private ProjectVO toVo(MmProject entity, boolean withContent, Map<Long, String> nameMap) {
        if (nameMap == null) {
            nameMap = loadCategoryNames(List.of(entity));
        }
        ProjectVO vo = new ProjectVO();
        vo.setId(entity.getId());
        vo.setCategoryId(entity.getCategoryId());
        if (entity.getCategoryId() != null && entity.getCategoryId() > 0) {
            vo.setCategoryName(nameMap.get(entity.getCategoryId()));
        }
        vo.setName(entity.getName());
        vo.setLayout(entity.getLayout());
        vo.setTheme(entity.getTheme());
        if (withContent) {
            vo.setContent(entity.getContent());
        }
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
