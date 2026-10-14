package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.PostQueryDTO;
import com.designer.admin.system.model.dto.PostSaveDTO;
import com.designer.admin.system.model.entity.SysPost;
import com.designer.admin.system.service.SysPostService;
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
 * 岗位管理：CRUD + 启停
 */
@Valid
@Controller
@Mapping("/admin-api/system/post")
public class SysPostController {

    @Inject
    private SysPostService postService;

    /**
     * 分页查询岗位列表
     */
    @Get
    @Mapping("/page")
    @SaCheckPermission("system:post:list")
    public R<PageResult<SysPost>> page(PostQueryDTO query) {
        return R.ok(PageResult.of(postService.page(query)));
    }

    /**
     * 查询启用的岗位选项（下拉用）
     */
    @Get
    @Mapping("/options")
    public R<List<SysPost>> options() {
        return R.ok(postService.options());
    }

    /**
     * 查询岗位详情
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:post:query")
    public R<SysPost> detail(Long id) {
        return R.ok(postService.getById(id));
    }

    /**
     * 新增岗位
     */
    @Post
    @Mapping("/add")
    @OperLog(title = "岗位管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:post:add")
    public R<Void> add(@Body PostSaveDTO dto) {
        postService.save(dto);
        return R.ok();
    }

    /**
     * 更新岗位
     */
    @Put
    @Mapping("/update")
    @OperLog(title = "岗位管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:post:edit")
    public R<Void> update(@Body PostSaveDTO dto) {
        postService.update(dto);
        return R.ok();
    }

    /**
     * 批量删除岗位（已分配用户的岗位不可删）
     */
    @Delete
    @Mapping("/remove")
    @OperLog(title = "岗位管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:post:remove")
    public R<Void> remove(List<Long> ids) {
        postService.remove(ids);
        return R.ok();
    }
}
