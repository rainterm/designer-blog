package com.designer.admin.module.blog.util;

import com.designer.admin.common.exception.BizException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 评论人匿名标识处理。
 *
 * <p>博客没有读者账号体系，只能用「浏览器本地随机密钥」来识别评论归属：
 * 前端把密钥存在 localStorage，随提交一起上传；服务端只保存它的 <b>SHA-256</b>，
 * 这样即便数据库泄露，也无法反推出密钥去删除别人的评论。</p>
 *
 * <p>⚠️ 这个密钥等同于管理自己评论的凭证，客户端清空浏览器数据后即失效（匿名评论的固有取舍）。</p>
 */
public final class CommenterKeyUtil {

    /** 只接受 URL 安全字符，长度 16~64：挡掉空值、超长值和注入式垃圾 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

    private CommenterKeyUtil() {
    }

    /**
     * 校验并归一化客户端密钥。
     *
     * @return 归一化后的密钥
     * @throws BizException 缺失或格式非法（查看 / 删除自己的评论时必须提供）
     */
    public static String requireKey(String raw) {
        String key = normalize(raw);
        if (key == null) {
            throw new BizException("缺少评论人标识，无法定位你的评论");
        }
        return key;
    }

    /**
     * 归一化客户端密钥，非法或缺失返回 {@code null}（提交评论时允许缺失）。
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim();
        return KEY_PATTERN.matcher(key).matches() ? key : null;
    }

    /**
     * 计算密钥的 SHA-256（小写十六进制），用于入库比对。
     *
     * @return 64 位十六进制串；入参非法时返回空串
     */
    public static String hash(String raw) {
        String key = normalize(raw);
        if (key == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必须支持的算法，走到这里说明运行环境异常
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
