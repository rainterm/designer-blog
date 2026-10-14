package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.OauthClientQueryDTO;
import com.designer.admin.system.model.dto.OauthClientSaveDTO;
import com.designer.admin.system.model.entity.SysOauthClient;
import com.designer.admin.system.model.vo.OauthClientCreatedVO;
import com.designer.admin.system.service.SysOauthClientService;
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

@Valid
@Controller
@Mapping("/admin-api/system/oauth-client")
public class SysOauthClientController {

    @Inject
    private SysOauthClientService clientService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:oauthclient:list")
    public R<PageResult<SysOauthClient>> page(OauthClientQueryDTO query) {
        return R.ok(PageResult.of(clientService.page(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:oauthclient:query")
    public R<SysOauthClient> detail(Long id) {
        return R.ok(clientService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "OAuth客户端", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:oauthclient:add")
    public R<OauthClientCreatedVO> add(@Body OauthClientSaveDTO dto) {
        return R.ok(clientService.save(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "OAuth客户端", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:oauthclient:edit")
    public R<Void> update(@Body OauthClientSaveDTO dto) {
        clientService.update(dto);
        return R.ok();
    }

    @Post
    @Mapping("/{id}/reset-secret")
    @OperLog(title = "OAuth客户端", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:oauthclient:resetSecret")
    public R<OauthClientCreatedVO> resetSecret(Long id) {
        return R.ok(clientService.resetSecret(id));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "OAuth客户端", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:oauthclient:remove")
    public R<Void> remove(List<Long> ids) {
        clientService.remove(ids);
        return R.ok();
    }
}
