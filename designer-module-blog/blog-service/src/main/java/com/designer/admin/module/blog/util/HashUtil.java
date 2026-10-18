package com.designer.admin.module.blog.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 哈希工具（SHA-256 十六进制）。
 *
 * @author 雨夜终端
 */
public final class HashUtil {

    private HashUtil() {
    }

    /**
     * SHA-256 小写十六进制。
     *
     * @param raw 原文；{@code null} 视为空串
     */
    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = (raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必须支持的算法，走到这里说明运行环境异常
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
