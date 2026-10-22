package com.designer.admin.system.service.rbac.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.TreeUtils;
import com.designer.admin.system.mapper.rbac.SysDeptMapper;
import com.designer.admin.system.mapper.rbac.SysUserMapper;
import com.designer.admin.system.model.dto.rbac.DeptSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysDept;
import com.designer.admin.system.model.entity.rbac.SysUser;
import com.designer.admin.system.service.rbac.SysDeptService;
import com.designer.admin.system.spi.PermissionInvalidator;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;

/**
 * 部门树 CRUD
 *
 * @author 雨夜终端
 */
@Component
public class SysDeptServiceImpl implements SysDeptService {

    /** 祖先路径分隔符 */
    private static final String ANCESTOR_SEPARATOR = ",";

    @Db
    private SysDeptMapper deptMapper;
    @Db
    private SysUserMapper userMapper;

    @Inject
    private PermissionInvalidator permissionInvalidator;

    @Override
    public List<SysDept> listTree(String deptName, Integer status) {
        // 数据权限由拦截器按 sys_dept.id 自动追加（sys_dept 在 dept-key-tables 中声明）
        List<SysDept> list = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .like(notBlank(deptName), SysDept::getDeptName, deptName)
                .eq(status != null, SysDept::getStatus, status)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getSort));
        return TreeUtils.buildTree(list, 0L);
    }

    @Override
    public List<SysDept> listOptions() {
        List<SysDept> list = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, SystemConsts.STATUS_ENABLE)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getSort));
        return TreeUtils.buildTree(list, 0L);
    }

    @Override
    public SysDept getById(Long id) {
        // 越权 ID 会被数据权限过滤成 null，等价于不存在
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BizException("部门不存在");
        }
        return dept;
    }

    @Override
    @Transaction
    public void add(DeptSaveDTO dto) {
        SysDept dept = toEntity(dto);
        Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
        dept.setParentId(parentId);
        dept.setAncestors(buildAncestors(parentId));
        if (dept.getStatus() == null) {
            dept.setStatus(SystemConsts.STATUS_ENABLE);
        }
        if (dept.getSort() == null) {
            dept.setSort(0);
        }
        deptMapper.insert(dept);
        // 部门树变了：「本部门及以下」的可见集合需要重算
        permissionInvalidator.evictAllUsers();
    }

    @Override
    @Transaction
    public void update(DeptSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("部门ID不能为空");
        }
        SysDept exist = getById(dto.getId());
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (dto.getId().equals(parentId)) {
            throw new BizException("上级部门不能是自己");
        }
        // 不能把父节点设为自己的子孙
        if (parentId != 0L) {
            SysDept parent = getById(parentId);
            String ancestors = parent.getAncestors() == null ? "" : parent.getAncestors();
            String wrappedAncestors = ANCESTOR_SEPARATOR + ancestors + ANCESTOR_SEPARATOR;
            String target = ANCESTOR_SEPARATOR + dto.getId() + ANCESTOR_SEPARATOR;
            if (wrappedAncestors.contains(target)) {
                throw new BizException("上级部门不能是自己的下级");
            }
        }

        String oldAncestors = exist.getAncestors();
        String newAncestors = buildAncestors(parentId);

        SysDept dept = toEntity(dto);
        dept.setParentId(parentId);
        dept.setAncestors(newAncestors);
        deptMapper.updateById(dept);

        // 同步更新子孙 ancestors
        if (!newAncestors.equals(oldAncestors)) {
            List<SysDept> children = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                    .likeRight(SysDept::getAncestors, oldAncestors + "," + exist.getId()));
            for (SysDept child : children) {
                String childAncestors = child.getAncestors();
                if (childAncestors != null && childAncestors.startsWith(oldAncestors)) {
                    child.setAncestors(newAncestors + childAncestors.substring(oldAncestors.length()));
                    deptMapper.updateById(child);
                }
            }
        }
        // 子树整体搬家后，所有「本部门及以下」的可见集合都失效；
        // 改名 / 排序不影响范围，但没必要为此再做一次「是否动了结构」的判定 —— 部门变更是低频操作
        permissionInvalidator.evictAllUsers();
    }

    @Override
    @Transaction
    public void remove(Long id) {
        getById(id);
        Long childCnt = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCnt != null && childCnt > 0) {
            throw new BizException("存在下级部门，不允许删除");
        }
        Long userCnt = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (userCnt != null && userCnt > 0) {
            throw new BizException("部门下存在用户，不允许删除");
        }
        deptMapper.deleteById(id);
        // 删除会让「自定义部门」里的授权悬空、也会改变子树范围，统一失效一次
        permissionInvalidator.evictAllUsers();
    }

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysDept parent = getById(parentId);
        String pa = parent.getAncestors();
        if (pa == null || pa.isBlank()) {
            return "0," + parentId;
        }
        return pa + "," + parentId;
    }

    private SysDept toEntity(DeptSaveDTO dto) {
        SysDept dept = new SysDept();
        dept.setId(dto.getId());
        dept.setParentId(dto.getParentId());
        dept.setDeptName(dto.getDeptName());
        dept.setSort(dto.getSort());
        dept.setStatus(dto.getStatus());
        return dept;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
