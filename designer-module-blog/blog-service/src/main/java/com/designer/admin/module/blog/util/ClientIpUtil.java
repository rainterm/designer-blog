package com.designer.admin.module.blog.util;

import org.noear.solon.core.handle.Context;

/**
 * 客户端 IP 解析。
 *
 * <p>取 {@link Context#realIp()} 的**首段**：多级代理下它是 {@code "client, proxy1, proxy2"}，
 * 只有首段是真实客户端。这依赖反向代理正确覆写 {@code X-Forwarded-For}，
 * 且只信任可信代理传来的值（见 framework 的 {@code TrustedProxyHostUtil}），
 * 否则 IP 可被伪造。</p>
 */
public final class ClientIpUtil {

    /** 取不到 IP 时的兜底值：避免所有无 IP 请求共用一个空键 */
    public static final String UNKNOWN_IP = "unknown";

    /** 与 blog_comment.ip / blog_visit_log.ip 列宽一致 */
    private static final int MAX_LENGTH = 64;

    private ClientIpUtil() {
    }

    /** 归一化后的客户端 IP（当前请求上下文不可用时返回 {@value #UNKNOWN_IP}） */
    public static String current() {
        return normalize(Context.current());
    }

    /** 归一化后的客户端 IP */
    public static String normalize(Context ctx) {
        if (ctx == null) {
            return UNKNOWN_IP;
        }
        String ip = ctx.realIp();
        if (ip == null || ip.isBlank()) {
            return UNKNOWN_IP;
        }
        int comma = ip.indexOf(',');
        if (comma > 0) {
            ip = ip.substring(0, comma);
        }
        ip = ip.trim();
        if (ip.isEmpty()) {
            return UNKNOWN_IP;
        }
        return ip.length() > MAX_LENGTH ? ip.substring(0, MAX_LENGTH) : ip;
    }
}
