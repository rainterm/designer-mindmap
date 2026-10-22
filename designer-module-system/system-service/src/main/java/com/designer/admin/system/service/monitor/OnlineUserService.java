package com.designer.admin.system.service.monitor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.monitor.OnlineUserQueryDTO;
import com.designer.admin.system.model.vo.monitor.OnlineUserVO;

import java.util.List;

/**
 * 在线用户：遍历 Account-Session 终端列表 + 强退
 *
 * @author 雨夜终端
 */
public interface OnlineUserService {

    /**
     * 在线用户分页。
     *
     * <p>会话数据存在 Redis，无法交给数据库分页，故先汇总过滤再内存切片。</p>
     *
     * @param query 分页与账号 / IP 过滤条件
     * @return 在线用户分页结果
     */
    IPage<OnlineUserVO> listPage(OnlineUserQueryDTO query);

    /**
     * 按账号 / IP 过滤在线会话列表。
     *
     * @param username 账号，空表示不过滤
     * @param ip       登录 IP，空表示不过滤
     * @return 在线用户列表，按登录时间倒序
     */
    List<OnlineUserVO> listOnlineUsers(String username, String ip);

    /**
     * 强制下线指定会话。
     *
     * @param tokenId 目标会话 token；不能是当前登录会话
     */
    void forceLogout(String tokenId);

    /**
     * 批量强制下线；当前登录会话会被自动跳过。
     *
     * @param tokenIds 目标会话 token 列表
     */
    void forceLogoutBatch(List<String> tokenIds);
}
