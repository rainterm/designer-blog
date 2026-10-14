package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.OnlineUserQueryDTO;
import com.designer.admin.system.model.vo.OnlineUserVO;
import com.designer.admin.system.service.OnlineUserService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在线用户
 */
@Valid
@Controller
@Mapping("/admin-api/system/online")
public class SysOnlineController {

    @Inject
    private OnlineUserService onlineUserService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:online:list")
    public R<PageResult<OnlineUserVO>> page(OnlineUserQueryDTO query) {
        return R.ok(PageResult.of(onlineUserService.page(query)));
    }

    @Delete
    @Mapping("/{tokenId}")
    @OperLog(title = "在线用户", businessType = BusinessType.FORCE)
    @SaCheckPermission("system:online:forceLogout")
    public R<Void> forceLogout(String tokenId) {
        onlineUserService.forceLogout(tokenId);
        return R.ok();
    }

    @Delete
    @Mapping("/batch")
    @OperLog(title = "在线用户", businessType = BusinessType.FORCE)
    @SaCheckPermission("system:online:forceLogout")
    public R<Void> forceLogoutBatch(String tokenIds) {
        onlineUserService.forceLogoutBatch(parseTokens(tokenIds));
        return R.ok();
    }

    private List<String> parseTokens(String tokenIds) {
        if (tokenIds == null || tokenIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tokenIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
