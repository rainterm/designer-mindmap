package com.designer.admin.module.agent.service.skill;

import com.designer.admin.module.agent.model.dto.skill.SkillImportDTO;
import com.designer.admin.module.agent.model.dto.skill.SkillQueryDTO;
import com.designer.admin.module.agent.model.dto.skill.SkillSaveDTO;
import com.designer.admin.module.agent.model.vo.skill.SkillVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

/**
 * Skill 管理
 *
 * @author 雨夜终端
 */
public interface AgentSkillService {

    /**
     * Skill 分页。
     *
     * @param query 分页与名称 / 编码 / 状态筛选条件
     * @return Skill 分页结果
     */
    IPage<SkillVO> page(SkillQueryDTO query);

    /**
     * 已启用 Skill 列表（下拉等场景）。
     *
     * @return 已启用 Skill 列表
     */
    List<SkillVO> listEnabled();

    /**
     * 按主键查询 Skill 详情。
     *
     * @param id Skill ID
     * @return Skill 详情
     */
    SkillVO getById(Long id);

    /**
     * 新增 Skill。
     *
     * @param dto 保存参数
     * @return 新建后的 Skill
     */
    SkillVO save(SkillSaveDTO dto);

    /**
     * 修改 Skill。
     *
     * @param dto 保存参数
     * @return 更新后的 Skill
     */
    SkillVO update(SkillSaveDTO dto);

    /**
     * 按 code upsert 导入压缩包 Skill。
     *
     * @param payload 导入参数（包内容与元数据）
     * @return 导入后的 Skill
     */
    SkillVO importPackage(SkillImportDTO payload);

    /**
     * 批量删除 Skill。
     *
     * @param ids Skill ID 列表
     */
    void remove(List<Long> ids);

}
