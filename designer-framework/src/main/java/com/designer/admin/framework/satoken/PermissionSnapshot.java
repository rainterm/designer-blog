package com.designer.admin.framework.satoken;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 用户权限快照：一次鉴权所需的全部事实（是否超管、权限标识、角色标识）。
 *
 * <p><b>为什么是三合一</b>：{@code @SaCheckPermission} 的校验链路是先问「是否超管」、
 * 再取权限清单，分两次查库就是两次往返。把三者收进一个快照，
 * 缓存未命中时仍然只做一次装载（命中则只读一个 Redis 键），
 * 也避免「超管标记来自新数据、权限清单来自旧数据」这种半新半旧的组合。</p>
 *
 * <p>字段名即缓存 JSON 的契约：改名等于让滚动发布期间的新旧实例互相读不懂缓存，
 * 只能等 TTL 过期，因此不要随手改。</p>
 *
 * @author 雨夜终端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionSnapshot implements Serializable {

    /** 是否超级管理员（拥有全部权限） */
    private Boolean superAdmin;

    /** 权限标识清单；超管固定为单个通配符 {@code *} */
    private List<String> permissions;

    /** 已启用角色标识清单 */
    private List<String> roles;

    /** 空快照：用户无任何角色 / 权限时使用，避免每次鉴权都回源数据库 */
    public static PermissionSnapshot empty() {
        return new PermissionSnapshot(false, List.of(), List.of());
    }
}
