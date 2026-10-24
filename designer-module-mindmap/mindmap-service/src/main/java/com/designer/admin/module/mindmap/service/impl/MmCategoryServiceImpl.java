package com.designer.admin.module.mindmap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.TreeUtils;
import com.designer.admin.module.mindmap.mapper.MmCategoryMapper;
import com.designer.admin.module.mindmap.mapper.MmProjectMapper;
import com.designer.admin.module.mindmap.model.dto.CategorySaveDTO;
import com.designer.admin.module.mindmap.model.entity.MmCategory;
import com.designer.admin.module.mindmap.model.entity.MmProject;
import com.designer.admin.module.mindmap.model.vo.CategoryVO;
import com.designer.admin.module.mindmap.service.MmCategoryService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * 思维导图分类服务实现。
 *
 * @author 雨夜终端
 */
@Component
public class MmCategoryServiceImpl implements MmCategoryService {

    private static final int MAX_DEPTH = 5;

    @Db
    private MmCategoryMapper categoryMapper;
    @Db
    private MmProjectMapper projectMapper;

    @Override
    public List<CategoryVO> listTree() {
        List<MmCategory> rows = categoryMapper.selectList(new LambdaQueryWrapper<MmCategory>()
                .orderByAsc(MmCategory::getSort)
                .orderByAsc(MmCategory::getId));
        Map<Long, Long> countMap = countByCategory();
        List<CategoryVO> flat = new ArrayList<>(rows.size());
        for (MmCategory row : rows) {
            CategoryVO vo = toVo(row);
            long direct = countMap.getOrDefault(row.getId(), 0L);
            vo.setProjectCount(direct);
            vo.setTotalCount(direct);
            flat.add(vo);
        }
        List<CategoryVO> tree = TreeUtils.buildTree(flat, 0L);
        fillTotalCount(tree);
        return tree;
    }

    @Override
    public List<MmCategory> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<MmCategory>()
                .orderByAsc(MmCategory::getSort)
                .orderByAsc(MmCategory::getId));
    }

    @Override
    public long countUncategorized() {
        return projectMapper.selectCount(new LambdaQueryWrapper<MmProject>()
                .and(w -> w.isNull(MmProject::getCategoryId).or().eq(MmProject::getCategoryId, 0L)));
    }

    @Override
    public List<Long> listSelfAndDescendantIds(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return List.of();
        }
        List<MmCategory> all = listAll();
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (MmCategory category : all) {
            long parentId = category.getParentId() == null ? 0L : category.getParentId();
            childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(category.getId());
        }
        List<Long> result = new ArrayList<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        Set<Long> seen = new HashSet<>();
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (id == null || !seen.add(id)) {
                continue;
            }
            result.add(id);
            for (Long child : childrenMap.getOrDefault(id, List.of())) {
                queue.add(child);
            }
        }
        return result;
    }

    @Override
    @Transaction
    public CategoryVO add(CategorySaveDTO dto) {
        String name = dto.getName().trim();
        Long parentId = normalizeParentId(dto.getParentId(), null);
        assertNameUnique(name, parentId, null);
        MmCategory entity = new MmCategory();
        entity.setParentId(parentId);
        entity.setName(name);
        entity.setSort(dto.getSort() == null ? nextSort(parentId) : dto.getSort());
        categoryMapper.insert(entity);
        CategoryVO vo = toVo(entity);
        vo.setProjectCount(0L);
        vo.setTotalCount(0L);
        return vo;
    }

    @Override
    @Transaction
    public CategoryVO update(CategorySaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("分类 ID 不能为空");
        }
        MmCategory exists = getById(dto.getId());
        String name = dto.getName().trim();
        Long parentId = dto.getParentId() == null
                ? (exists.getParentId() == null ? 0L : exists.getParentId())
                : normalizeParentId(dto.getParentId(), dto.getId());
        assertNameUnique(name, parentId, dto.getId());

        MmCategory entity = new MmCategory();
        entity.setId(dto.getId());
        entity.setParentId(parentId);
        entity.setName(name);
        entity.setSort(dto.getSort() != null ? dto.getSort() : exists.getSort());
        categoryMapper.updateById(entity);

        CategoryVO vo = toVo(getById(dto.getId()));
        long direct = projectMapper.selectCount(new LambdaQueryWrapper<MmProject>()
                .eq(MmProject::getCategoryId, entity.getId()));
        vo.setProjectCount(direct);
        vo.setTotalCount(direct);
        return vo;
    }

    @Override
    @Transaction
    public void remove(Long id) {
        if (id == null) {
            return;
        }
        MmCategory exists = getById(id);
        Long parentId = exists.getParentId() == null ? 0L : exists.getParentId();

        // 子分类挂到当前父级，避免删父后整棵子树悬空
        categoryMapper.update(null, new LambdaUpdateWrapper<MmCategory>()
                .eq(MmCategory::getParentId, id)
                .set(MmCategory::getParentId, parentId));

        // 本分类下的导图改为未分类
        projectMapper.update(null, new LambdaUpdateWrapper<MmProject>()
                .eq(MmProject::getCategoryId, id)
                .set(MmProject::getCategoryId, null));
        categoryMapper.deleteById(id);
    }

    @Override
    public MmCategory getById(Long id) {
        MmCategory row = categoryMapper.selectById(id);
        if (row == null) {
            throw new BizException("分类不存在");
        }
        return row;
    }

    private Long normalizeParentId(Long parentId, Long selfId) {
        long pid = parentId == null || parentId < 0 ? 0L : parentId;
        if (pid == 0L) {
            return 0L;
        }
        if (selfId != null && Objects.equals(pid, selfId)) {
            throw new BizException("不能将分类设为自己的子分类");
        }
        getById(pid);
        if (selfId != null) {
            List<Long> descendants = listSelfAndDescendantIds(selfId);
            if (descendants.contains(pid)) {
                throw new BizException("不能将分类移动到自己的子分类下");
            }
        }
        if (depthOf(pid) + 1 >= MAX_DEPTH) {
            throw new BizException("分类最多支持 " + MAX_DEPTH + " 层");
        }
        return pid;
    }

    private int depthOf(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return 0;
        }
        Map<Long, Long> parentMap = new HashMap<>();
        for (MmCategory category : listAll()) {
            parentMap.put(category.getId(), category.getParentId() == null ? 0L : category.getParentId());
        }
        int depth = 0;
        Long current = categoryId;
        Set<Long> seen = new HashSet<>();
        while (current != null && current > 0 && seen.add(current)) {
            depth++;
            current = parentMap.get(current);
            if (depth > MAX_DEPTH + 2) {
                break;
            }
        }
        return depth;
    }

    private void assertNameUnique(String name, Long parentId, Long excludeId) {
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<MmCategory>()
                .eq(MmCategory::getName, name)
                .eq(MmCategory::getParentId, parentId == null ? 0L : parentId)
                .ne(excludeId != null, MmCategory::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("同级下已有同名分类");
        }
    }

    private int nextSort(Long parentId) {
        MmCategory last = categoryMapper.selectOne(new LambdaQueryWrapper<MmCategory>()
                .eq(MmCategory::getParentId, parentId == null ? 0L : parentId)
                .orderByDesc(MmCategory::getSort)
                .orderByDesc(MmCategory::getId)
                .last("limit 1"));
        return last == null || last.getSort() == null ? 1 : last.getSort() + 1;
    }

    private Map<Long, Long> countByCategory() {
        List<MmProject> projects = projectMapper.selectList(new LambdaQueryWrapper<MmProject>()
                .select(MmProject::getId, MmProject::getCategoryId)
                .isNotNull(MmProject::getCategoryId)
                .ne(MmProject::getCategoryId, 0L));
        Map<Long, Long> map = new HashMap<>();
        for (MmProject project : projects) {
            map.merge(project.getCategoryId(), 1L, Long::sum);
        }
        return map;
    }

    private long fillTotalCount(List<CategoryVO> nodes) {
        long sum = 0;
        for (CategoryVO node : nodes) {
            long childTotal = fillTotalCount(node.getChildren() == null ? List.of() : node.getChildren());
            long total = (node.getProjectCount() == null ? 0L : node.getProjectCount()) + childTotal;
            node.setTotalCount(total);
            sum += total;
        }
        return sum;
    }

    private CategoryVO toVo(MmCategory row) {
        CategoryVO vo = new CategoryVO();
        vo.setId(row.getId());
        vo.setParentId(row.getParentId() == null ? 0L : row.getParentId());
        vo.setName(row.getName());
        vo.setSort(row.getSort());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
