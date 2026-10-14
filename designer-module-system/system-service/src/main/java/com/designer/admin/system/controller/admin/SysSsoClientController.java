package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.SsoClientQueryDTO;
import com.designer.admin.system.model.dto.SsoClientSaveDTO;
import com.designer.admin.system.model.entity.SysSsoClient;
import com.designer.admin.system.service.SysSsoClientService;
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
 * SSO 客户端管理：白名单 CRUD
 */
@Valid
@Controller
@Mapping("/admin-api/system/sso-client")
public class SysSsoClientController {

    @Inject
    private SysSsoClientService ssoClientService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:ssoclient:list")
    public R<PageResult<SysSsoClient>> page(SsoClientQueryDTO query) {
        return R.ok(PageResult.of(ssoClientService.page(query)));
    }

    /** 已启用列表（任意登录用户，供侧栏打开外链） */
    @Get
    @Mapping("/enabled")
    public R<List<SysSsoClient>> enabled() {
        return R.ok(ssoClientService.listEnabled());
    }

    @Get
    @Mapping("/by-key/{clientKey}")
    public R<SysSsoClient> byKey(String clientKey) {
        return R.ok(ssoClientService.getEnabledByKey(clientKey));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:ssoclient:query")
    public R<SysSsoClient> detail(Long id) {
        return R.ok(ssoClientService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "SSO客户端", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:ssoclient:add")
    public R<Void> add(@Body SsoClientSaveDTO dto) {
        ssoClientService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "SSO客户端", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:ssoclient:edit")
    public R<Void> update(@Body SsoClientSaveDTO dto) {
        ssoClientService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "SSO客户端", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:ssoclient:remove")
    public R<Void> remove(List<Long> ids) {
        ssoClientService.remove(ids);
        return R.ok();
    }
}
