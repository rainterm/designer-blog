package com.designer.admin.system.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 前端路由节点
 */
@Data
public class RouterVO implements Serializable {

    private Long id;

    private Long parentId;

    /** 路由地址 */
    private String path;

    /** 路由名称（唯一） */
    private String name;

    /** 前端组件路径，目录为 Layout */
    private String component;

    /** 菜单元信息 */
    private MetaVO meta;

    private List<RouterVO> children;

    @Data
    public static class MetaVO implements Serializable {
        private String title;
        private String icon;
        private boolean keepAlive;
        /** 侧栏/页签不展示（对应菜单 visible=0 的跳转页） */
        private boolean hidden;
        /** 外链（普通 URL 或 SSO 应用），侧栏点击新窗口打开 */
        private boolean external;
        /** SSO 客户端标识（component=External:xxx 时的 xxx） */
        private String ssoClient;
        /** 普通外链地址（component=Link 或 path 为 http(s)） */
        private String linkUrl;
    }
}
