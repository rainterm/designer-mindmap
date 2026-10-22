package com.designer.admin.system.service.config;

import com.designer.admin.system.model.dto.config.SystemSettingSaveDTO;
import com.designer.admin.system.model.entity.config.SysSetting;
import com.designer.admin.system.model.vo.config.SystemSettingVO;

/**
 * 系统设置（单行）：系统名称 / 登录页副标题 / Logo / 默认偏好设置。
 *
 * <p>与通用的参数配置（{@code sys_config} 键值对）不同，这里是后台「系统设置」菜单
 * 专属的结构化单行配置，固定 id=1。默认偏好用于「用户未在偏好设置面板自定义」时的兜底。</p>
 *
 * @author 雨夜终端
 */
public interface SysSettingService {

    /**
     * 读取系统设置（单行配置），缺省时返回默认值对象。
     *
     * @return 系统设置
     */
    SysSetting getSetting();

    /**
     * 匿名可读的公开设置：系统名称、Logo、主题色、布局等。
     *
     * @return 公开设置
     */
    SystemSettingVO getPublic();

    /**
     * 保存系统设置并失效公开设置缓存。
     *
     * @param dto 保存参数
     */
    void update(SystemSettingSaveDTO dto);
}
