package com.designer.admin.system.service.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.model.dto.auth.ProfileUpdateDTO;
import com.designer.admin.system.model.dto.rbac.ResetPwdDTO;
import com.designer.admin.system.model.dto.rbac.UserOptionQueryDTO;
import com.designer.admin.system.model.dto.rbac.UserQueryDTO;
import com.designer.admin.system.model.dto.rbac.UserSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysUser;
import com.designer.admin.system.model.vo.rbac.UserExcelVO;
import com.designer.admin.system.model.vo.rbac.UserOptionVO;

import java.util.List;

/**
 * 用户服务
 *
 * @author 雨夜终端
 */
public interface SysUserService {

    /**
     * 用户分页。
     *
     * @param query 分页与账号 / 昵称 / 部门 / 状态筛选条件
     * @return 用户分页结果
     */
    IPage<SysUser> listPage(UserQueryDTO query);

    /**
     * 用户选择器分页（精简字段）。
     *
     * @param query 分页与关键字筛选条件
     * @return 用户选项分页结果
     */
    IPage<UserOptionVO> listOptionPage(UserOptionQueryDTO query);

    /**
     * 按主键查询用户，不存在时抛出业务异常。
     *
     * @param id 用户 ID
     * @return 用户
     */
    SysUser getById(Long id);

    /**
     * 新增用户：校验账号唯一，并保存部门 / 岗位 / 角色关联。
     *
     * @param dto 保存参数
     */
    void add(UserSaveDTO dto);

    /**
     * 修改用户及其部门 / 岗位 / 角色关联。
     *
     * @param dto 保存参数
     */
    void update(UserSaveDTO dto);

    /**
     * 判断账号是否可用（可排除指定用户自身）。
     *
     * @param username  待校验的账号
     * @param excludeId 需要排除的用户 ID，可空
     * @return 可用返回 true
     */
    boolean isUsernameAvailable(String username, Long excludeId);

    /**
     * 批量删除用户；内置管理员不允许删除。
     *
     * @param ids 用户 ID 列表
     */
    void remove(List<Long> ids);

    /**
     * 重置用户密码。
     *
     * @param dto 重置参数（用户 ID 与新密码）
     */
    void resetPassword(ResetPwdDTO dto);

    /**
     * 启用 / 停用用户。
     *
     * @param id     用户 ID
     * @param status 1=启用，0=停用
     */
    void updateStatus(Long id, Integer status);

    /**
     * 更新当前用户的个人资料。
     *
     * @param userId 用户 ID，由调用方从登录态取得
     * @param dto    昵称 / 头像 / 邮箱 / 手机号
     */
    void updateProfile(Long userId, ProfileUpdateDTO dto);

    /**
     * 修改密码，需校验原密码。
     *
     * @param userId 用户 ID
     * @param oldPwd 原密码
     * @param newPwd 新密码
     */
    void updatePassword(Long userId, String oldPwd, String newPwd);

    /**
     * 导出用户数据行（Excel）。
     *
     * @param query 筛选条件
     * @return 可写入 Excel 的数据行
     */
    List<UserExcelVO> exportRows(UserQueryDTO query);

    /**
     * 导入用户，跳过空行与已存在的账号。
     *
     * @param rows Excel 解析出的数据行
     * @return 成功导入条数
     */
    int importRows(List<UserExcelVO> rows);
}
