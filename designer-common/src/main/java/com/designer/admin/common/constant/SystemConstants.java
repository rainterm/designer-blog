package com.designer.admin.common.constant;

/**
 * 系统常量
 */
public interface SystemConstants {

    /** 是否超级管理员：1是 */
    int IS_SUPER_YES = 1;
    /** 是否超级管理员：0否 */
    int IS_SUPER_NO = 0;

    /** 角色类型：1=系统内置（不可修改/删除） */
    int ROLE_TYPE_BUILTIN = 1;
    /** 角色类型：2=自定义 */
    int ROLE_TYPE_CUSTOM = 2;

    /** 菜单类型：目录 */
    String MENU_TYPE_DIR = "M";
    /** 菜单类型：菜单 */
    String MENU_TYPE_MENU = "C";
    /** 菜单类型：按钮 */
    String MENU_TYPE_BUTTON = "F";

    /** 通用状态：启用 */
    int STATUS_ENABLE = 1;
}
