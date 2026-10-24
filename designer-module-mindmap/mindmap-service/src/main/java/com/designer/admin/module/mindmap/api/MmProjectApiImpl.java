package com.designer.admin.module.mindmap.api;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.mindmap.api.model.MmProjectBriefVO;
import com.designer.admin.module.mindmap.model.dto.ProjectSaveDTO;
import com.designer.admin.module.mindmap.model.vo.ProjectVO;
import com.designer.admin.module.mindmap.service.MmProjectService;
import com.designer.admin.module.mindmap.util.MindmapOutlineUtil;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * {@link MmProjectApi} 实现。
 *
 * @author 雨夜终端
 */
@Component
public class MmProjectApiImpl implements MmProjectApi {

    @Inject
    private MmProjectService projectService;

    @Override
    public MmProjectBriefVO createFromOutline(String name, String outlineMd, Long categoryId) {
        if (Utils.isBlank(outlineMd)) {
            throw new BizException("大纲内容不能为空");
        }
        ProjectSaveDTO dto = new ProjectSaveDTO();
        dto.setName(Utils.isBlank(name) ? null : name.trim());
        dto.setContent(MindmapOutlineUtil.toContentJson(outlineMd));
        if (categoryId != null && categoryId > 0) {
            dto.setCategoryId(categoryId);
        } else {
            dto.setClearCategory(true);
        }
        ProjectVO vo = projectService.add(dto);
        return toBrief(vo);
    }

    @Override
    public MmProjectBriefVO updateFromOutline(Long projectId, String outlineMd, String name) {
        if (projectId == null) {
            throw new BizException("导图项目 ID 不能为空");
        }
        if (Utils.isBlank(outlineMd)) {
            throw new BizException("大纲内容不能为空");
        }
        ProjectSaveDTO dto = new ProjectSaveDTO();
        dto.setId(projectId);
        dto.setContent(MindmapOutlineUtil.toContentJson(outlineMd));
        if (Utils.isNotBlank(name)) {
            dto.setName(name.trim());
        }
        ProjectVO vo = projectService.update(dto);
        return toBrief(vo);
    }

    private static MmProjectBriefVO toBrief(ProjectVO vo) {
        return MmProjectBriefVO.builder()
                .id(vo.getId())
                .name(vo.getName())
                .editPath("/mindmap/project/edit?id=" + vo.getId())
                .build();
    }
}
