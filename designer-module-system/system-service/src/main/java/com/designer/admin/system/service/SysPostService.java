package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.mapper.SysPostMapper;
import com.designer.admin.system.mapper.SysUserPostMapper;
import com.designer.admin.system.model.dto.PostQueryDTO;
import com.designer.admin.system.model.dto.PostSaveDTO;
import com.designer.admin.system.model.entity.SysPost;
import com.designer.admin.system.model.entity.SysUserPost;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;

/**
 * 岗位服务（全局字典；部门通过用户关联）
 */
@Component
public class SysPostService {

    @Db
    private SysPostMapper postMapper;
    @Db
    private SysUserPostMapper userPostMapper;

    public IPage<SysPost> page(PostQueryDTO query) {
        return postMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysPost>()
                .like(notBlank(query.getPostName()), SysPost::getPostName, query.getPostName())
                .like(notBlank(query.getPostCode()), SysPost::getPostCode, query.getPostCode())
                .eq(query.getStatus() != null, SysPost::getStatus, query.getStatus())
                .orderByAsc(SysPost::getSort)
                .orderByAsc(SysPost::getPostLevel)
                .orderByAsc(SysPost::getId));
    }

    public List<SysPost> options() {
        return postMapper.selectList(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getStatus, 1)
                .orderByAsc(SysPost::getSort)
                .orderByAsc(SysPost::getPostLevel)
                .orderByAsc(SysPost::getId));
    }

    public SysPost getById(Long id) {
        return postMapper.selectById(id);
    }

    @Transaction
    public void save(PostSaveDTO dto) {
        assertPostCodeUnique(dto.getPostCode(), null);
        SysPost post = toEntity(dto);
        if (post.getStatus() == null) {
            post.setStatus(1);
        }
        if (post.getSort() == null) {
            post.setSort(0);
        }
        if (post.getPostLevel() == null) {
            post.setPostLevel(0);
        }
        if (post.getRemark() == null) {
            post.setRemark("");
        }
        postMapper.insert(post);
    }

    @Transaction
    public void update(PostSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("岗位ID不能为空");
        }
        SysPost existing = postMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException("岗位不存在");
        }
        assertPostCodeUnique(dto.getPostCode(), dto.getId());
        postMapper.updateById(toEntity(dto));
    }

    /**
     * 批量删除岗位：已分配用户的岗位不允许删除。
     */
    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            SysPost post = postMapper.selectById(id);
            if (post == null) {
                continue;
            }
            Long bound = userPostMapper.selectCount(new LambdaQueryWrapper<SysUserPost>()
                    .eq(SysUserPost::getPostId, id));
            if (bound != null && bound > 0) {
                throw new BizException("岗位【" + post.getPostName() + "】已分配用户，无法删除");
            }
        }
        postMapper.deleteByIds(ids);
    }

    private void assertPostCodeUnique(String postCode, Long excludeId) {
        if (postCode == null || postCode.isBlank()) {
            throw new BizException("岗位编码不能为空");
        }
        Long count = postMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getPostCode, postCode.trim())
                .ne(excludeId != null, SysPost::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("岗位编码已存在：" + postCode);
        }
    }

    private SysPost toEntity(PostSaveDTO dto) {
        SysPost post = new SysPost();
        post.setId(dto.getId());
        post.setPostName(dto.getPostName());
        post.setPostCode(dto.getPostCode() == null ? null : dto.getPostCode().trim());
        post.setPostLevel(dto.getPostLevel());
        post.setSort(dto.getSort());
        post.setStatus(dto.getStatus());
        post.setRemark(dto.getRemark());
        return post;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
