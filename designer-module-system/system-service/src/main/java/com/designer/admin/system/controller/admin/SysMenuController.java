package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.MenuSaveDTO;
import com.designer.admin.system.model.entity.SysMenu;
import com.designer.admin.system.service.SysMenuService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 菜单管理：CRUD + 菜单树（前端权限与路由源数据）
 */
@Valid
@Controller
@Mapping("/admin-api/system/menu")
public class SysMenuController {

    @Inject
    private SysMenuService menuService;

    /**
     * 查询菜单树（按名称/状态过滤）
     */
    @Get
    @Mapping("/tree")
    @SaCheckPermission("system:menu:list")
    public R<List<SysMenu>> tree(String keyword, Integer status) {
        return R.ok(menuService.tree(keyword, status));
    }

    /**
     * 查询菜单选项（用于父菜单下拉，不含按钮）
     */
    @Get
    @Mapping("/options")
    public R<List<SysMenu>> options() {
        return R.ok(menuService.options());
    }

    /**
     * 查询菜单详情
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:menu:query")
    public R<SysMenu> detail(Long id) {
        return R.ok(menuService.getById(id));
    }

    /**
     * 新增菜单；新增后若存在父菜单会自动修正父节点类型为目录
     */
    @Post
    @Mapping("/add")
    @OperLog(title = "菜单管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:menu:add")
    public R<Void> add(@Body @Validated MenuSaveDTO dto) {
        menuService.save(dto);
        return R.ok();
    }

    /**
     * 更新菜单
     */
    @Put
    @Mapping("/update")
    @OperLog(title = "菜单管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:menu:edit")
    public R<Void> update(@Body @Validated MenuSaveDTO dto) {
        menuService.update(dto);
        return R.ok();
    }

    /**
     * 删除菜单（级联删除子孙菜单，并清理角色-菜单关联）
     */
    @Delete
    @Mapping("/{id}")
    @OperLog(title = "菜单管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:menu:remove")
    public R<Void> remove(Long id) {
        menuService.remove(id);
        return R.ok();
    }
}