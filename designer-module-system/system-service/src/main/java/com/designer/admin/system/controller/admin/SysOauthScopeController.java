package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.OauthScopeQueryDTO;
import com.designer.admin.system.model.dto.OauthScopeSaveDTO;
import com.designer.admin.system.model.entity.SysOauthScope;
import com.designer.admin.system.service.SysOauthScopeService;
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
 * 开放接口 Scope：挂在 OAuth 客户端弹框内管理，无独立菜单。
 */
@Valid
@Controller
@Mapping("/admin-api/system/oauth-scope")
public class SysOauthScopeController {

    @Inject
    private SysOauthScopeService scopeService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:oauthclient:list")
    public R<PageResult<SysOauthScope>> page(OauthScopeQueryDTO query) {
        return R.ok(PageResult.of(scopeService.page(query)));
    }

    @Get
    @Mapping("/options")
    @SaCheckPermission(value = {"system:oauthclient:list", "system:oauthclient:add", "system:oauthclient:edit"}, mode = SaMode.OR)
    public R<List<SysOauthScope>> options() {
        return R.ok(scopeService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:oauthclient:query")
    public R<SysOauthScope> detail(Long id) {
        return R.ok(scopeService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "OAuth开放接口", businessType = BusinessType.INSERT)
    @SaCheckPermission(value = {"system:oauthclient:add", "system:oauthclient:edit"}, mode = SaMode.OR)
    public R<SysOauthScope> add(@Body OauthScopeSaveDTO dto) {
        return R.ok(scopeService.save(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "OAuth开放接口", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:oauthclient:edit")
    public R<Void> update(@Body OauthScopeSaveDTO dto) {
        scopeService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "OAuth开放接口", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:oauthclient:edit")
    public R<Void> remove(List<Long> ids) {
        scopeService.remove(ids);
        return R.ok();
    }
}
