package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.common.util.ExcelUtil;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.ResetPwdDTO;
import com.designer.admin.system.model.dto.UserOptionQueryDTO;
import com.designer.admin.system.model.dto.UserQueryDTO;
import com.designer.admin.system.model.dto.UserSaveDTO;
import com.designer.admin.system.model.entity.SysUser;
import com.designer.admin.system.model.vo.UserExcelVO;
import com.designer.admin.system.model.vo.UserOptionVO;
import com.designer.admin.system.service.SysUserService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * 用户管理：CRUD + 重置密码 + 启停状态
 */
@Valid
@Controller
@Mapping("/admin-api/system/user")
public class SysUserController {

    @Inject
    private SysUserService userService;

    /**
     * 分页查询用户列表（用户管理，完整字段）
     */
    @Get
    @Mapping("/page")
    @SaCheckPermission("system:user:list")
    public R<PageResult<SysUser>> page(UserQueryDTO query) {
        return R.ok(PageResult.of(userService.page(query)));
    }

    /**
     * 人员选择分页（仅 id/账号/昵称/部门名，登录即可；供转办、流程指派等）
     */
    @Get
    @Mapping("/options")
    public R<PageResult<UserOptionVO>> options(UserOptionQueryDTO query) {
        return R.ok(PageResult.of(userService.pageOptions(query)));
    }

    /**
     * 校验登录账号是否可用（新增/修改前调用）
     */
    @Get
    @Mapping("/check-username")
    @SaCheckPermission("system:user:list")
    public R<Boolean> checkUsername(String username, Long excludeId) {
        return R.ok(userService.isUsernameAvailable(username, excludeId));
    }

    /**
     * 查询用户详情（含已分配角色ID）
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:user:query")
    public R<SysUser> detail(Long id) {
        return R.ok(userService.getById(id));
    }

    /**
     * 新增用户；密码必填，会校验账号唯一性
     */
    @Post
    @Mapping("/add")
    @OperLog(title = "用户管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:user:add")
    public R<Void> add(@Body @Validated UserSaveDTO dto) {
        userService.save(dto);
        return R.ok();
    }

    /**
     * 更新用户；密码为空表示不修改
     */
    @Put
    @Mapping("/update")
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:user:edit")
    public R<Void> update(@Body @Validated UserSaveDTO dto) {
        userService.update(dto);
        return R.ok();
    }

    /**
     * 批量删除用户（ID=1 的超级管理员不可删）
     */
    @Delete
    @Mapping("/remove")
    @OperLog(title = "用户管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:user:remove")
    public R<Void> remove(List<Long> ids) {
        userService.remove(ids);
        return R.ok();
    }

    /**
     * 重置用户密码
     */
    @Put
    @Mapping("/password")
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE, saveParam = false)
    @SaCheckPermission("system:user:resetPwd")
    public R<Void> resetPassword(@Body @Validated ResetPwdDTO dto) {
        userService.resetPassword(dto);
        return R.ok();
    }

    /**
     * 启/停用用户（ID=1 的超级管理员不可停用）
     */
    @Put
    @Mapping("/{id}/status")
    @SaCheckPermission("system:user:edit")
    public R<Void> changeStatus(Long id, Integer status) {
        userService.changeStatus(id, status);
        return R.ok();
    }

    /** 按查询条件导出用户 Excel */
    @Get
    @Mapping("/export")
    @OperLog(title = "用户管理", businessType = BusinessType.EXPORT, saveResult = false)
    @SaCheckPermission("system:user:export")
    public void export(Context ctx, UserQueryDTO query) throws Exception {
        ExcelUtil.write(ctx, "用户数据.xlsx", "用户", UserExcelVO.class, userService.exportRows(query));
    }

    /** 下载用户导入模板 */
    @Get
    @Mapping("/importTemplate")
    @SaCheckPermission("system:user:import")
    public void importTemplate(Context ctx) throws Exception {
        ExcelUtil.writeTemplate(ctx, "用户导入模板.xlsx", "用户", UserExcelVO.class);
    }

    /** 导入用户 Excel；已存在账号跳过；密码为必填项，且逐行按密码策略校验 */
    @Post
    @Mapping("/import")
    @OperLog(title = "用户管理", businessType = BusinessType.IMPORT, saveParam = false, saveResult = false)
    @SaCheckPermission("system:user:import")
    public R<Map<String, Object>> importData(UploadedFile file) {
        int count = userService.importRows(ExcelUtil.read(file, UserExcelVO.class));
        return R.ok(Map.of("success", count));
    }
}