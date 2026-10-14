package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.MessageRecordQueryDTO;
import com.designer.admin.system.model.entity.SysMessageRecord;
import com.designer.admin.system.service.SysMessageRecordService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

/**
 * 消息发送记录
 */
@Valid
@Controller
@Mapping("/admin-api/system/msgrecord")
public class SysMessageRecordController {

    @Inject
    private SysMessageRecordService messageRecordService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:msgrecord:list")
    public R<PageResult<SysMessageRecord>> page(MessageRecordQueryDTO query) {
        return R.ok(PageResult.of(messageRecordService.page(query)));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "消息记录", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:msgrecord:remove")
    public R<Void> remove(String ids) {
        messageRecordService.removeByIds(ids);
        return R.ok();
    }

    @Delete
    @Mapping("/clear")
    @OperLog(title = "消息记录", businessType = BusinessType.CLEAN)
    @SaCheckPermission("system:msgrecord:remove")
    public R<Void> clear() {
        messageRecordService.clear();
        return R.ok();
    }
}
