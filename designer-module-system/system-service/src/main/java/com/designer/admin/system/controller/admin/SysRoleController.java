package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.RoleQueryDTO;
import com.designer.admin.system.model.dto.RoleSaveDTO;
import com.designer.admin.system.model.entity.SysRole;
import com.designer.admin.system.service.SysRoleService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;

import java.util.List;

/**
 * 角色管理：CRUD + 启停 + 角色授权菜单
 */
@Valid
@Controller
@Mapping("/admin-api/system/role")
public class SysRoleController {

    @Inject
    private SysRoleService roleService;

    /**
     * 分页查询角色列表
     */
    @Get
    @Mapping("/page")
    @SaCheckPermission("system:role:list")
    public R<PageResult<SysRole>> page(RoleQueryDTO query) {
        return R.ok(PageResult.of(roleService.page(query)));
    }

    /**
     * 查询启用的角色选项（下拉用）
     */
    @Get
    @Mapping("/options")
    public R<List<SysRole>> options() {
        return R.ok(roleService.options());
    }

    /**
     * 查询角色详情
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:role:query")
    public R<SysRole> detail(Long id) {
        return R.ok(roleService.getById(id));
    }

    /**
     * 新增角色；会校验 roleKey 唯一性
     */
    @Post
    @Mapping("/add")
    @OperLog(title = "角色管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:role:add")
    public R<Void> add(@Body RoleSaveDTO dto) {
        roleService.save(dto);
        return R.ok();
    }

    /**
     * 更新角色（内置角色不可改；is_super/role_type 字段由后端管控，前端 DTO 提交的值被忽略）
     */
    @Put
    @Mapping("/update")
    @OperLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:role:edit")
    public R<Void> update(@Body RoleSaveDTO dto) {
        roleService.update(dto);
        return R.ok();
    }

    /**
     * 批量删除角色（内置角色与已分配用户的角色不可删）
     */
    @Delete
    @Mapping("/remove")
    @OperLog(title = "角色管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:role:remove")
    public R<Void> remove(List<Long> ids) {
        roleService.remove(ids);
        return R.ok();
    }

    /**
     * 查询角色已分配的菜单ID
     */
    @Get
    @Mapping("/{id}/menus")
    @SaCheckPermission("system:role:query")
    public R<List<Long>> menuIds(Long id) {
        return R.ok(roleService.menuIds(id));
    }

    /**
     * 角色授权菜单（全量替换）
     */
    @Put
    @Mapping("/{id}/menus")
    @OperLog(title = "角色授权", businessType = BusinessType.GRANT)
    @SaCheckPermission("system:role:edit")
    public R<Void> assignMenus(Long id, @Body List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }
}