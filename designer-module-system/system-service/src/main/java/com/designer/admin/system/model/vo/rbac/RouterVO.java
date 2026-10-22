package com.designer.admin.system.model.vo.rbac;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 前端路由节点。
 *
 * @author 雨夜终端
 */
@Data
public class RouterVO implements Serializable {

    /** 菜单ID */
    private Long id;

    /** 父菜单ID，0 为顶级 */
    private Long parentId;

    /** 路由地址 */
    private String path;

    /** 路由名称（唯一） */
    private String name;

    /** 前端组件路径，目录为 Layout */
    private String component;

    /** 菜单元信息 */
    private MetaVO meta;

    /** 子路由 */
    private List<RouterVO> children;

    /**
     * 前端路由元信息（标题 / 图标 / 缓存 / 外链）。
     *
     * @author 雨夜终端
     */
    @Data
    public static class MetaVO implements Serializable {

        /** 菜单标题（侧栏与页签文案） */
        private String title;

        /** 菜单图标 */
        private String icon;

        /** 是否缓存页面（keep-alive） */
        private Boolean keepAlive;

        /** 侧栏/页签不展示（对应菜单 visible=0 的跳转页） */
        private Boolean hidden;

        /** 外链（普通 URL 或 SSO 应用），侧栏点击新窗口打开 */
        private Boolean external;

        /** SSO 客户端标识（component=External:xxx 时的 xxx） */
        private String ssoClient;

        /** 普通外链地址（component=Link 或 path 为 http(s)） */
        private String linkUrl;
    }
}
