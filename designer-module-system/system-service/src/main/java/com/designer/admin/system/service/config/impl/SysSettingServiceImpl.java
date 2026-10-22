package com.designer.admin.system.service.config.impl;

import com.designer.admin.system.constant.SysSettingConsts;
import com.designer.admin.system.constant.SystemCacheConsts;
import com.designer.admin.system.mapper.config.SysSettingMapper;
import com.designer.admin.system.model.dto.config.SystemSettingSaveDTO;
import com.designer.admin.system.model.entity.config.SysSetting;
import com.designer.admin.system.model.vo.config.SystemSettingVO;
import com.designer.admin.system.service.config.SysSettingService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.CacheRemove;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 系统设置（单行）：系统名称 / 登录页副标题 / Logo / 默认偏好设置。
 *
 * <p>与通用的参数配置（{@code sys_config} 键值对）不同，这里是后台「系统设置」菜单
 * 专属的结构化单行配置，固定 id=1。默认偏好用于「用户未在偏好设置面板自定义」时的兜底。</p>
 *
 * @author 雨夜终端
 */
@Component
public class SysSettingServiceImpl implements SysSettingService {

    /** 合法布局模式白名单（与前端 constants/layout.js 的 LAYOUT_OPTIONS 保持一致） */
    private static final Set<String> LAYOUT_MODES = Set.of(
            "vertical", "dual-column", "horizontal", "side-nav",
            "mixed-vertical", "mixed-dual", "content-full");

    /** 主题色：仅接受 #RRGGBB */
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    @Db
    private SysSettingMapper settingMapper;

    @Override
    public SysSetting getSetting() {
        SysSetting setting = settingMapper.selectById(SysSettingConsts.SINGLETON_ID);
        return setting == null ? empty() : setting;
    }

    @Override
    public SystemSettingVO getPublic() {
        SysSetting s = getSetting();
        SystemSettingVO vo = new SystemSettingVO();
        vo.setSystemName(normalizeName(s.getSystemName()));
        vo.setLoginSubtitle(trim(s.getLoginSubtitle()));
        vo.setLogoUrl(trim(s.getLogoUrl()));
        vo.setLogoRadius(normalizeRadius(s.getLogoRadius()));
        vo.setDefaultLayoutMode(normalizeLayoutMode(s.getDefaultLayoutMode()));
        vo.setDefaultPrimaryColor(normalizeColor(s.getDefaultPrimaryColor()));
        return vo;
    }

    @Override
    @CacheRemove(tags = SystemCacheConsts.SETTING_PUBLIC)
    public void update(SystemSettingSaveDTO dto) {
        SysSetting entity = new SysSetting();
        entity.setId(SysSettingConsts.SINGLETON_ID);
        entity.setSystemName(normalizeName(dto.getSystemName()));
        entity.setLoginSubtitle(trim(dto.getLoginSubtitle()));
        entity.setLogoUrl(trim(dto.getLogoUrl()));
        entity.setLogoRadius(normalizeRadius(dto.getLogoRadius()));
        entity.setDefaultLayoutMode(normalizeLayoutMode(dto.getDefaultLayoutMode()));
        entity.setDefaultPrimaryColor(normalizeColor(dto.getDefaultPrimaryColor()));

        if (settingMapper.selectById(SysSettingConsts.SINGLETON_ID) == null) {
            settingMapper.insert(entity);
        } else {
            settingMapper.updateById(entity);
        }
        // 缓存失效由方法上的 @CacheRemove 在返回后执行
    }

    private SysSetting empty() {
        SysSetting s = new SysSetting();
        s.setId(SysSettingConsts.SINGLETON_ID);
        s.setSystemName(SysSettingConsts.DEFAULT_SYSTEM_NAME);
        s.setLoginSubtitle(SysSettingConsts.DEFAULT_LOGIN_SUBTITLE);
        s.setLogoUrl("");
        s.setLogoRadius(SysSettingConsts.DEFAULT_LOGO_RADIUS);
        s.setDefaultLayoutMode(SysSettingConsts.DEFAULT_LAYOUT_MODE);
        s.setDefaultPrimaryColor(SysSettingConsts.DEFAULT_PRIMARY_COLOR);
        return s;
    }

    /** 系统名称归一化：空白回默认值，避免登录页标题变成空白 */
    private String normalizeName(String raw) {
        String name = trim(raw);
        return name.isEmpty() ? SysSettingConsts.DEFAULT_SYSTEM_NAME : name;
    }

    /** 圆角兜底与裁剪：非法回默认，0 为合法值（直角） */
    private int normalizeRadius(Integer radius) {
        if (radius == null || radius < 0) {
            return SysSettingConsts.DEFAULT_LOGO_RADIUS;
        }
        return Math.min(radius, SysSettingConsts.MAX_LOGO_RADIUS);
    }

    /** 布局模式：白名单外一律回默认，避免脏值让前端布局失效 */
    private String normalizeLayoutMode(String mode) {
        String value = trim(mode);
        return LAYOUT_MODES.contains(value) ? value : SysSettingConsts.DEFAULT_LAYOUT_MODE;
    }

    /** 主题色：仅接受 #RRGGBB，非法回默认 */
    private String normalizeColor(String color) {
        String value = trim(color);
        return HEX_COLOR.matcher(value).matches() ? value.toUpperCase() : SysSettingConsts.DEFAULT_PRIMARY_COLOR;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
