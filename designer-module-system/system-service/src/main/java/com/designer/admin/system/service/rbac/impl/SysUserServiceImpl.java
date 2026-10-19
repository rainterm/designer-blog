package com.designer.admin.system.service.rbac.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.crypto.PasswordUtil;
import com.designer.admin.system.constant.SysUserConsts;
import com.designer.admin.system.mapper.rbac.SysDeptMapper;
import com.designer.admin.system.mapper.rbac.SysPostMapper;
import com.designer.admin.system.mapper.rbac.SysRoleMapper;
import com.designer.admin.system.mapper.rbac.SysUserMapper;
import com.designer.admin.system.mapper.rbac.SysUserPostMapper;
import com.designer.admin.system.mapper.rbac.SysUserRoleMapper;
import com.designer.admin.system.model.dto.auth.ProfileUpdateDTO;
import com.designer.admin.system.model.dto.rbac.ResetPwdDTO;
import com.designer.admin.system.model.dto.rbac.UserOptionQueryDTO;
import com.designer.admin.system.model.dto.rbac.UserQueryDTO;
import com.designer.admin.system.model.dto.rbac.UserSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysDept;
import com.designer.admin.system.model.entity.rbac.SysPost;
import com.designer.admin.system.model.entity.rbac.SysRole;
import com.designer.admin.system.model.entity.rbac.SysUser;
import com.designer.admin.system.model.entity.rbac.SysUserPost;
import com.designer.admin.system.model.entity.rbac.SysUserRole;
import com.designer.admin.system.model.vo.rbac.UserExcelVO;
import com.designer.admin.system.model.vo.rbac.UserOptionVO;
import com.designer.admin.system.service.rbac.SysUserService;
import com.designer.admin.system.spi.PermissionInvalidator;
import com.designer.admin.system.util.PasswordValidatorUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户服务
 *
 * @author 雨夜终端
 */
@Component
public class SysUserServiceImpl implements SysUserService {

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
    @Inject
    private PermissionInvalidator permissionInvalidator;

    @Override
    public IPage<SysUser> listPage(UserQueryDTO query) {
        IPage<SysUser> page = userMapper.selectPage(query.toPage(), buildQueryWrapper(query));
        fillRoles(page.getRecords());
        fillPosts(page.getRecords());
        fillDepts(page.getRecords());
        return page;
    }

    @Override
    public IPage<UserOptionVO> listOptionPage(UserOptionQueryDTO query) {
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

    @Override
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

    @Override
    @Transaction
    public void add(UserSaveDTO dto) {
        if (!notBlank(dto.getPassword())) {
            throw new BizException("新增用户时密码不能为空");
        }
        assertPasswordPolicy(dto.getPassword());
        String username = requireUsername(dto.getUsername());
        assertUsernameUnique(username, null);
        dto.setUsername(username);
        SysUser user = new SysUser();
        copyEditable(dto, user);
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(SystemConsts.STATUS_ENABLE);
        }
        userMapper.insert(user);
        saveUserRoles(user.getId(), dto.getRoleIds());
        saveUserPosts(user.getId(), dto.getPostIds());
    }

    @Override
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
        // 编辑接口同样能改状态：不在这里拦，停用保护就能被 PUT /user/update 绕过
        assertNotDisablingSuperUser(dto.getId(), dto.getStatus());
        dto.setUsername(username);
        SysUser user = new SysUser();
        user.setId(dto.getId());
        copyEditable(dto, user);
        if (notBlank(dto.getPassword())) {
            assertPasswordPolicy(dto.getPassword());
            user.setPassword(PasswordUtil.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
        if (dto.getRoleIds() != null) {
            saveUserRoles(dto.getId(), dto.getRoleIds());
        }
        if (dto.getPostIds() != null) {
            saveUserPosts(dto.getId(), dto.getPostIds());
        }
    }

    @Override
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

    @Override
    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 超管身份以 sys_role.is_super 为准，不再硬编码 id == 1
        if (userMapper.existsSuperUser(ids)) {
            throw new BizException("超级管理员不允许删除");
        }
        userMapper.deleteByIds(ids);
        for (Long id : ids) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, id));
        }
        // 用户没了，残留的快照没人会再读；删掉是为了避免「删号后重建同 ID」读到旧权限（ID 不复用，属防御性）
        permissionInvalidator.evictUsers(ids);
    }

    @Override
    public void resetPassword(ResetPwdDTO dto) {
        if (dto.getUserId() == null || !notBlank(dto.getPassword())) {
            throw new BizException("参数不完整");
        }
        SysUser target = userMapper.selectById(dto.getUserId());
        if (target == null) {
            throw new BizException("用户不存在");
        }
        assertPasswordPolicy(dto.getPassword());
        if (PasswordUtil.matches(dto.getPassword(), target.getPassword())) {
            throw new BizException("新密码不能与该用户当前密码相同");
        }
        SysUser user = new SysUser();
        user.setId(dto.getUserId());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        assertNotDisablingSuperUser(id, status);
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 停用保护：超管账号不允许被停用。
     *
     * <p>按 {@code is_super} 判定而不是主键：换库/导入数据后主键会变，
     * 硬编码 {@code id == 1} 会让保护在不知不觉中失效。</p>
     *
     * @param userId 目标用户 ID
     * @param status 目标状态，非「停用」时直接放行
     */
    private void assertNotDisablingSuperUser(Long userId, Integer status) {
        if (userId == null || !Integer.valueOf(SystemConsts.STATUS_DISABLE).equals(status)) {
            return;
        }
        if (userMapper.existsSuperUser(List.of(userId))) {
            throw new BizException("超级管理员不允许停用");
        }
    }

    @Override
    public void updateProfile(Long userId, ProfileUpdateDTO dto) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        userMapper.updateById(user);
    }

    @Override
    public void updatePassword(Long userId, String oldPwd, String newPwd) {
        SysUser db = userMapper.selectById(userId);
        if (db == null) {
            throw new BizException("用户不存在");
        }
        if (!PasswordUtil.matches(oldPwd, db.getPassword())) {
            throw new BizException("原密码不正确");
        }
        assertPasswordPolicy(newPwd);
        if (newPwd.equals(oldPwd)) {
            throw new BizException("新密码不能与原密码相同");
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(PasswordUtil.encode(newPwd));
        userMapper.updateById(user);
    }

    /**
     * 密码长度与复杂度的兜底校验，规则见 {@link PasswordValidatorUtil}。
     *
     * <p>参数注解已经卡过一遍，这里再卡是因为「写入密码」的路径不止 Controller 一条：
     * Excel 导入完全绕过 DTO，将来其它模块（建号、定时任务）也可能直接调本服务。
     * 只有让这条界限在服务层也成立，登录侧才敢同样卡下限 ——
     * 否则一旦漏掉某个写入口子，就会产出「库里躺着 3 位密码、但永远登不进去」的账号。</p>
     */
    private void assertPasswordPolicy(String rawPassword) {
        String violation = PasswordValidatorUtil.firstViolation(rawPassword);
        if (violation != null) {
            throw new BizException(violation);
        }
    }

    @Override
    public List<UserExcelVO> exportRows(UserQueryDTO query) {
        List<SysUser> users = userMapper.selectList(buildQueryWrapper(query).last("LIMIT " + SysUserConsts.EXPORT_LIMIT));
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

    @Override
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
            // 导入绕过 DTO，密码策略必须在这里自己把住，否则 Excel 就是绕开校验的后门。
            // 空密码直接判失败，不兜底成默认密码 —— 见方法注释。
            String pwd = notBlank(row.getPassword()) ? row.getPassword().trim() : null;
            if (pwd == null) {
                throw new BizException("第 " + line + " 行密码不能为空");
            }
            String pwdViolation = PasswordValidatorUtil.firstViolation(pwd);
            if (pwdViolation != null) {
                throw new BizException("第 " + line + " 行密码不符合要求：" + pwdViolation);
            }
            user.setPassword(PasswordUtil.encode(pwd));
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
        // 角色集合变了，该用户的权限快照必须作废（事务提交后删，避免回源读到旧数据）
        permissionInvalidator.evictUser(userId);
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
