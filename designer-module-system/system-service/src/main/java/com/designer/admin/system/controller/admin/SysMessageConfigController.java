package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.MessageConfigQueryDTO;
import com.designer.admin.system.model.dto.MessageConfigSaveDTO;
import com.designer.admin.system.model.dto.MessageSendDTO;
import com.designer.admin.system.model.entity.SysMessageConfig;
import com.designer.admin.system.model.vo.MessageChannelTypeVO;
import com.designer.admin.system.service.SysMessageConfigService;
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
import java.util.Map;

/**
 * 消息通道配置（邮件等，可扩展短信/钉钉/微信/飞书）
 */
@Valid
@Controller
@Mapping("/admin-api/system/message")
public class SysMessageConfigController {

    @Inject
    private SysMessageConfigService messageConfigService;

    @Get
    @Mapping("/types")
    @SaCheckPermission(value = {"system:message:list", "system:msgrecord:send"}, mode = SaMode.OR)
    public R<List<MessageChannelTypeVO>> types() {
        return R.ok(messageConfigService.listTypes());
    }

    @Get
    @Mapping("/page")
    @SaCheckPermission(value = {"system:message:list", "system:msgrecord:send"}, mode = SaMode.OR)
    public R<PageResult<SysMessageConfig>> page(MessageConfigQueryDTO query) {
        return R.ok(PageResult.of(messageConfigService.page(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:message:query")
    public R<Map<String, Object>> detail(Long id) {
        return R.ok(messageConfigService.detailView(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "消息通道", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:message:add")
    public R<Void> add(@Body @Validated MessageConfigSaveDTO dto) {
        messageConfigService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "消息通道", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:message:edit")
    public R<Void> update(@Body @Validated MessageConfigSaveDTO dto) {
        messageConfigService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "消息通道", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:message:remove")
    public R<Void> remove(String ids) {
        messageConfigService.removeByIds(ids);
        return R.ok();
    }

    @Put
    @Mapping("/{id}/default")
    @OperLog(title = "消息通道", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:message:edit")
    public R<Void> setDefault(Long id) {
        messageConfigService.setDefault(id);
        return R.ok();
    }

    @Put
    @Mapping("/{id}/status")
    @OperLog(title = "消息通道", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:message:edit")
    public R<Void> changeStatus(Long id, Integer status) {
        messageConfigService.changeStatus(id, status);
        return R.ok();
    }

    @Post
    @Mapping("/send")
    @OperLog(title = "消息通道", businessType = BusinessType.OTHER)
    @SaCheckPermission(value = {"system:message:send", "system:msgrecord:send"}, mode = SaMode.OR)
    public R<Void> send(@Body @Validated MessageSendDTO dto) {
        messageConfigService.testSend(dto);
        return R.ok();
    }
}
