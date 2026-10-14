package com.designer.admin.system.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 跨模块用户/部门查询（审批人解析等）。
 */
public interface SystemUserQueryApi {

    Long getUserDeptId(Long userId);

    /**
     * 按岗位沿部门上级链查找任职用户：岗位为全局字典，从 baseUser 所在部门起向上，
     * 找持有该岗位的启用用户，返回最近一级命中的用户 ID 列表（可能多人）。
     */
    List<Long> listSuperiorUserIdsByPost(Long baseUserId, Long postId);

    List<Long> listUserIdsByRoleIds(Collection<Long> roleIds);

    List<Long> listUserIdsByDeptIds(Collection<Long> deptIds);

    /** id -> nickname */
    Map<Long, String> mapUserNicknames(Collection<Long> userIds);

    String getUserNickname(Long userId);

    /** 岗位名称；不存在时返回 null */
    String getPostName(Long postId);
}
