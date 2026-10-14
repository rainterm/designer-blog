package com.designer.admin.system.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConstants;
import com.designer.admin.system.mapper.SysDeptMapper;
import com.designer.admin.system.mapper.SysPostMapper;
import com.designer.admin.system.mapper.SysUserMapper;
import com.designer.admin.system.mapper.SysUserPostMapper;
import com.designer.admin.system.mapper.SysUserRoleMapper;
import com.designer.admin.system.model.entity.SysDept;
import com.designer.admin.system.model.entity.SysPost;
import com.designer.admin.system.model.entity.SysUser;
import com.designer.admin.system.model.entity.SysUserPost;
import com.designer.admin.system.model.entity.SysUserRole;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SystemUserQueryApiImpl implements SystemUserQueryApi {

    @Db
    private SysUserMapper userMapper;
    @Db
    private SysDeptMapper deptMapper;
    @Db
    private SysUserRoleMapper userRoleMapper;
    @Db
    private SysPostMapper postMapper;
    @Db
    private SysUserPostMapper userPostMapper;

    @Override
    public Long getUserDeptId(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getDeptId();
    }

    @Override
    public List<Long> listSuperiorUserIdsByPost(Long baseUserId, Long postId) {
        if (baseUserId == null || postId == null) {
            return List.of();
        }
        SysPost targetPost = postMapper.selectById(postId);
        if (targetPost == null || !Objects.equals(targetPost.getStatus(), SystemConstants.STATUS_ENABLE)) {
            return List.of();
        }
        SysUser baseUser = userMapper.selectById(baseUserId);
        if (baseUser == null || baseUser.getDeptId() == null) {
            return List.of();
        }
        // 岗位全局；按用户所属部门向上找「持有该岗位」的人
        Long deptId = baseUser.getDeptId();
        int guard = 0;
        while (deptId != null && deptId > 0 && guard++ < 64) {
            List<SysUser> deptUsers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getDeptId, deptId)
                    .eq(SysUser::getStatus, SystemConstants.STATUS_ENABLE));
            if (!deptUsers.isEmpty()) {
                List<Long> candidateIds = deptUsers.stream()
                        .map(SysUser::getId)
                        .filter(Objects::nonNull)
                        .filter(id -> !id.equals(baseUserId))
                        .toList();
                if (!candidateIds.isEmpty()) {
                    List<SysUserPost> links = userPostMapper.selectList(new LambdaQueryWrapper<SysUserPost>()
                            .eq(SysUserPost::getPostId, postId)
                            .in(SysUserPost::getUserId, candidateIds));
                    if (!links.isEmpty()) {
                        return links.stream()
                                .map(SysUserPost::getUserId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList();
                    }
                }
            }
            SysDept dept = deptMapper.selectById(deptId);
            if (dept == null || dept.getParentId() == null || dept.getParentId() <= 0) {
                break;
            }
            deptId = dept.getParentId();
        }
        return List.of();
    }

    @Override
    public List<Long> listUserIdsByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<SysUserRole> rows = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getRoleId, roleIds));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = rows.stream().map(SysUserRole::getUserId).filter(Objects::nonNull).distinct().toList();
        return filterEnabledUsers(userIds);
    }

    @Override
    public List<Long> listUserIdsByDeptIds(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getDeptId, deptIds)
                .eq(SysUser::getStatus, SystemConstants.STATUS_ENABLE));
        return users.stream().map(SysUser::getId).filter(Objects::nonNull).distinct().toList();
    }

    @Override
    public Map<Long, String> mapUserNicknames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUser> users = userMapper.selectBatchIds(new ArrayList<>(userIds));
        Map<Long, String> map = new LinkedHashMap<>();
        for (SysUser u : users) {
            String name = u.getNickname();
            if (name == null || name.isBlank()) {
                name = u.getUsername();
            }
            map.put(u.getId(), name == null ? "" : name);
        }
        return map;
    }

    @Override
    public String getUserNickname(Long userId) {
        if (userId == null) {
            return "";
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return "";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername() == null ? "" : user.getUsername();
    }

    @Override
    public String getPostName(Long postId) {
        if (postId == null) {
            return null;
        }
        SysPost post = postMapper.selectById(postId);
        return post == null ? null : post.getPostName();
    }

    private List<Long> filterEnabledUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, userIds)
                .eq(SysUser::getStatus, SystemConstants.STATUS_ENABLE));
        return users.stream().map(SysUser::getId).collect(Collectors.toList());
    }
}
