package com.designer.admin.system.service;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.model.dto.ResetPwdDTO;
import com.designer.admin.system.model.dto.UserOptionQueryDTO;
import com.designer.admin.system.model.dto.UserQueryDTO;
import com.designer.admin.system.model.dto.UserSaveDTO;
import com.designer.admin.system.model.entity.SysDept;
import com.designer.admin.system.model.entity.SysPost;
import com.designer.admin.system.model.entity.SysRole;
import com.designer.admin.system.model.entity.SysUser;
import com.designer.admin.system.model.entity.SysUserPost;
import com.designer.admin.system.model.entity.SysUserRole;
import com.designer.admin.system.model.vo.UserExcelVO;
import com.designer.admin.system.model.vo.UserOptionVO;
import com.designer.admin.system.mapper.SysDeptMapper;
import com.designer.admin.system.mapper.SysPostMapper;
import com.designer.admin.system.mapper.SysRoleMapper;
import com.designer.admin.system.mapper.SysUserMapper;
import com.designer.admin.system.mapper.SysUserPostMapper;
import com.designer.admin.system.mapper.SysUserRoleMapper;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Component
public class SysUserService {

    private static final String DEFAULT_IMPORT_PASSWORD = "Admin@123";
    private static final int EXPORT_LIMIT = 10000;

    @Db
    private SysUserMapper userMapper;
    @Db
    private SysUserRoleMapper userRoleMapper;
    @Db
    private SysUserPostMapper userPostMapper;
    @Db
    private SysRoleMapper roleMapper;
    @Db
    private SysPostMapper postMapper;
    @Db
    private SysDeptMapper deptMapper;

    public IPage<SysUser> page(UserQueryDTO query) {
        IPage<SysUser> page = userMapper.selectPage(query.toPage(), buildQueryWrapper(query));
        fillRoles(page.getRecords());
        fillPosts(page.getRecords());
        fillDepts(page.getRecords());
        return page;
    }

    /**
     * 人员选择：仅启用用户，返回精简字段；关键字同时匹配账号与昵称。
     */
    public IPage<UserOptionVO> pageOptions(UserOptionQueryDTO query) {
        String keyword = query.getKeyword() != null ? query.getKeyword().trim() : null;
        IPage<SysUser> page = userMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname, SysUser::getDeptId)
                .eq(SysUser::getStatus, 1)
                .and(notBlank(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getNickname, keyword))
                .orderByDesc(SysUser::getId));
        fillDepts(page.getRecords());
        return page.convert(this::toOption);
    }

    private UserOptionVO toOption(SysUser user) {
        UserOptionVO vo = new UserOptionVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setDeptName(user.getDeptName());
        return vo;
    }

    private LambdaQueryWrapper<SysUser> buildQueryWrapper(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<SysUser>()
                .like(notBlank(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(notBlank(query.getNickname()), SysUser::getNickname, query.getNickname())
                .like(notBlank(query.getPhone()), SysUser::getPhone, query.getPhone())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .eq(query.getDeptId() != null, SysUser::getDeptId, query.getDeptId())
                .orderByDesc(SysUser::getId);
        if (query.getRoleId() != null) {
            List<Long> userIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getRoleId, query.getRoleId()))
                    .stream()
                    .map(SysUserRole::getUserId)
                    .distinct()
                    .toList();
            if (userIds.isEmpty()) {
                w.eq(SysUser::getId, -1L);
            } else {
                w.in(SysUser::getId, userIds);
            }
        }
        return w;
    }

    public SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        fillRoles(List.of(user));
        fillPosts(List.of(user));
        fillDepts(List.of(user));
        return user;
    }

    /**
     * 新增用户：密码必填且 BCrypt 加密；校验账号唯一；状态为空时默认启用；
     * 事务内同步写入用户-角色绑定。
     */
    @Transaction
    public void save(UserSaveDTO dto) {
        if (!notBlank(dto.getPassword())) {
            throw new BizException("新增用户时密码不能为空");
        }
        String username = requireUsername(dto.getUsername());
        assertUsernameUnique(username, null);
        dto.setUsername(username);
        SysUser user = new SysUser();
        copyEditable(dto, user);
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        saveUserRoles(user.getId(), dto.getRoleIds());
        saveUserPosts(user.getId(), dto.getPostIds());
    }

    /**
     * 更新用户：密码为空表示不修改；事务内可选重建用户-角色绑定。
     */
    @Transaction
    public void update(UserSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("用户ID不能为空");
        }
        SysUser exist = userMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        String username = requireUsername(dto.getUsername());
        assertUsernameUnique(username, dto.getId());
        dto.setUsername(username);
        SysUser user = new SysUser();
        user.setId(dto.getId());
        copyEditable(dto, user);
        if (notBlank(dto.getPassword())) {
            user.setPassword(BCrypt.hashpw(dto.getPassword()));
        }
        userMapper.updateById(user);
        if (dto.getRoleIds() != null) {
            saveUserRoles(dto.getId(), dto.getRoleIds());
        }
        if (dto.getPostIds() != null) {
            saveUserPosts(dto.getId(), dto.getPostIds());
        }
    }

    /** 登录账号是否可用（不含当前用户自身） */
    public boolean isUsernameAvailable(String username, Long excludeId) {
        if (!notBlank(username)) {
            return false;
        }
        Long cnt = userMapper.countByUsername(username.trim(), excludeId);
        return cnt == null || cnt == 0;
    }

    private String requireUsername(String username) {
        if (!notBlank(username)) {
            throw new BizException("账号不能为空");
        }
        return username.trim();
    }

    private void assertUsernameUnique(String username, Long excludeId) {
        Long cnt = userMapper.countByUsername(username, excludeId);
        if (cnt != null && cnt > 0) {
            throw new BizException("登录账号已存在：" + username);
        }
    }

    /**
     * 批量删除用户：ID=1 的超级管理员不允许删除；同时清理 sys_user_role 绑定。
     */
    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (ids.contains(1L)) {
            throw new BizException("超级管理员不允许删除");
        }
        userMapper.deleteByIds(ids);
        for (Long id : ids) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, id));
        }
    }

    public void resetPassword(ResetPwdDTO dto) {
        if (dto.getUserId() == null || !notBlank(dto.getPassword())) {
            throw new BizException("参数不完整");
        }
        SysUser user = new SysUser();
        user.setId(dto.getUserId());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        userMapper.updateById(user);
    }

    /**
     * 启/停用用户：ID=1 的超级管理员不允许停用。
     */
    public void changeStatus(Long id, Integer status) {
        if (id != null && id == 1L && status != null && status == 0) {
            throw new BizException("超级管理员不允许停用");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /** 个人中心：更新当前用户资料 */
    public void updateProfile(Long userId, String nickname, String avatar, String email, String phone) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setEmail(email);
        user.setPhone(phone);
        userMapper.updateById(user);
    }

    /**
     * 个人中心：修改自己的密码，BCrypt 校验原密码后写入新密码。
     *
     * @throws BizException 原密码不正确或用户不存在
     */
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        SysUser db = userMapper.selectById(userId);
        if (db == null) {
            throw new BizException("用户不存在");
        }
        if (!BCrypt.checkpw(oldPwd, db.getPassword())) {
            throw new BizException("原密码不正确");
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(BCrypt.hashpw(newPwd));
        userMapper.updateById(user);
    }

    /** 按当前查询条件导出用户（不含密码） */
    public List<UserExcelVO> exportRows(UserQueryDTO query) {
        List<SysUser> users = userMapper.selectList(buildQueryWrapper(query).last("LIMIT " + EXPORT_LIMIT));
        fillRoles(users);
        fillPosts(users);
        fillDepts(users);
        List<UserExcelVO> rows = new ArrayList<>(users.size());
        for (SysUser user : users) {
            UserExcelVO row = new UserExcelVO();
            row.setUsername(user.getUsername());
            row.setNickname(user.getNickname());
            row.setEmail(user.getEmail());
            row.setPhone(user.getPhone());
            row.setDeptId(user.getDeptId());
            row.setStatus(user.getStatus());
            row.setRemark(user.getRemark());
            row.setPassword("");
            rows.add(row);
        }
        return rows;
    }

    /**
     * 批量导入用户：账号已存在则跳过；密码为空时使用默认 Admin@123。
     *
     * @return 成功导入条数
     */
    @Transaction
    public int importRows(List<UserExcelVO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BizException("导入数据为空");
        }
        int success = 0;
        int line = 1;
        for (UserExcelVO row : rows) {
            line++;
            if (row == null || !notBlank(row.getUsername())) {
                continue;
            }
            Long dup = userMapper.countByUsername(row.getUsername().trim(), null);
            if (dup != null && dup > 0) {
                continue;
            }
            if (row.getDeptId() != null && deptMapper.selectById(row.getDeptId()) == null) {
                throw new BizException("第 " + line + " 行部门ID不存在：" + row.getDeptId());
            }
            SysUser user = new SysUser();
            user.setUsername(row.getUsername().trim());
            user.setNickname(notBlank(row.getNickname()) ? row.getNickname().trim() : row.getUsername().trim());
            user.setEmail(row.getEmail() == null ? "" : row.getEmail().trim());
            user.setPhone(row.getPhone() == null ? "" : row.getPhone().trim());
            user.setDeptId(row.getDeptId());
            user.setStatus(row.getStatus() == null ? 1 : row.getStatus());
            user.setRemark(row.getRemark() == null ? "" : row.getRemark());
            String pwd = notBlank(row.getPassword()) ? row.getPassword().trim() : DEFAULT_IMPORT_PASSWORD;
            user.setPassword(BCrypt.hashpw(pwd));
            userMapper.insert(user);
            success++;
        }
        if (success == 0) {
            throw new BizException("没有可导入的新用户（账号可能均已存在）");
        }
        return success;
    }

    private void copyEditable(UserSaveDTO dto, SysUser user) {
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        user.setStatus(dto.getStatus());
        user.setRemark(dto.getRemark());
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    private void saveUserPosts(Long userId, List<Long> postIds) {
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
        if (postIds != null) {
            for (Long postId : postIds) {
                SysUserPost up = new SysUserPost();
                up.setUserId(userId);
                up.setPostId(postId);
                userPostMapper.insert(up);
            }
        }
    }

    /**
     * 回填角色 ID 与角色名称。列表列使用 {@code roleNames}，编辑回显使用 {@code roleIds}。
     */
    private void fillRoles(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(SysUser::getId).filter(Objects::nonNull).toList();
        List<SysUserRole> links = userIds.isEmpty() ? List.of() : userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        Map<Long, SysRole> roleMap = Map.of();
        if (!links.isEmpty()) {
            List<Long> roleIds = links.stream().map(SysUserRole::getRoleId).distinct().toList();
            roleMap = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                            .in(SysRole::getId, roleIds)
                            .orderByAsc(SysRole::getSort))
                    .stream()
                    .collect(Collectors.toMap(SysRole::getId, role -> role, (a, b) -> a));
        }
        Map<Long, List<Long>> userRoleIds = links.stream().collect(Collectors.groupingBy(
                SysUserRole::getUserId,
                Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
        for (SysUser user : users) {
            List<SysRole> roles = userRoleIds.getOrDefault(user.getId(), List.of()).stream()
                    .map(roleMap::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(SysRole::getSort, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
            user.setRoleIds(roles.stream().map(SysRole::getId).toList());
            user.setRoleNames(roles.stream().map(SysRole::getRoleName).toList());
        }
    }

    /**
     * 回填岗位 ID 与岗位名称。列表列使用 {@code postNames}，编辑回显使用 {@code postIds}。
     */
    private void fillPosts(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(SysUser::getId).filter(Objects::nonNull).toList();
        List<SysUserPost> links = userIds.isEmpty() ? List.of() : userPostMapper.selectList(
                new LambdaQueryWrapper<SysUserPost>().in(SysUserPost::getUserId, userIds));
        Map<Long, SysPost> postMap = Map.of();
        if (!links.isEmpty()) {
            List<Long> postIds = links.stream().map(SysUserPost::getPostId).distinct().toList();
            List<SysPost> posts = postMapper.selectList(new LambdaQueryWrapper<SysPost>()
                    .in(SysPost::getId, postIds)
                    .orderByAsc(SysPost::getSort));
            postMap = posts.stream().collect(Collectors.toMap(SysPost::getId, post -> post, (a, b) -> a));
        }
        Map<Long, List<Long>> userPostIds = links.stream().collect(Collectors.groupingBy(
                SysUserPost::getUserId,
                Collectors.mapping(SysUserPost::getPostId, Collectors.toList())));
        for (SysUser user : users) {
            List<SysPost> posts = userPostIds.getOrDefault(user.getId(), List.of()).stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(SysPost::getSort, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
            user.setPostIds(posts.stream().map(SysPost::getId).toList());
            user.setPostNames(posts.stream().map(SysPost::getPostName).toList());
        }
    }

    private void fillDepts(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> deptIds = users.stream().map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList();
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, String> deptNameMap = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                        .in(SysDept::getId, deptIds))
                .stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        for (SysUser user : users) {
            if (user.getDeptId() != null) {
                user.setDeptName(deptNameMap.get(user.getDeptId()));
            }
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
