package com.designer.admin.module.blog.util;

import java.text.Normalizer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 评论昵称清洗与「保留昵称」比对。
 *
 * <p>背景：评论区是匿名接口，昵称完全由访客自填。若只按字面比较，
 * 「博主」很容易被 {@code "博主 "}、全角空格 {@code "博　主"}、零宽字符
 * {@code "博\u200b主"} 或全角字母等变体绕过。所以比较前先做归一化：</p>
 *
 * <ul>
 *   <li>{@link #sanitize}：入库用 —— 去首尾空白、NFKC 折叠全角/兼容字符、
 *   去掉零宽与控制字符、把连续空白压成一个空格；</li>
 *   <li>{@link #key}：比对用 —— 在 {@code sanitize} 基础上再去掉**所有**空白并转小写，
 *   于是「博 主」与「博主」视为同名，{@code ADMIN} / {@code ａｄｍｉｎ} 与 {@code admin} 同形。</li>
 * </ul>
 *
 * <p><b>匹配是「包含」而不是「等于」</b>（{@link #firstMatch}）：昵称里只要出现任意一个
 * 保留词就拒绝。这样才能挡住「博主小号」「我是站长」「admin01」这类夹带式冒充；
 * 代价是保留词本身就是该昵称的一部分时也会被拦（例如站长署名叫「雨」，
 * 那么任何含「雨」的昵称都不行），所以保留词应填完整、有辨识度的词。</p>
 *
 * <p><b>保留名单由后台配置</b>（{@code blog_setting.reserved_nicknames}），
 * 不再硬编码在代码里；{@code BlogSettingConsts#DEFAULT_RESERVED_NICKNAMES} 只用于新建库 / 设置行缺失时的兜底。
 * 站长署名会由 {@link #reserved} 自动追加，不必在配置里重复填一遍。</p>
 *
 * <p>⚠️ 归一化 + 包含匹配挡不住异体字 / 形近字（如「愽主」）。因此它只用于**减少**视觉混淆，
 * 不是身份鉴别的依据 —— 真正的身份由 {@code blog_comment.author_type} 决定，
 * 展示层认标记不认昵称。</p>
 *
 * @author 雨夜终端
 */
public final class NicknameUtil {

    /** 保留昵称文本的分隔符：半角/全角逗号、分号、换行 */
    private static final String RESERVED_SEPARATORS = "[,，;；\\r\\n]+";

    private NicknameUtil() {
    }

    /**
     * 清洗昵称：可安全入库、可安全展示。
     *
     * @return 清洗后的昵称，入参为空 / 全是不可见字符时返回空串
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        // NFKC 把全角字母数字、全角空格等兼容字符折叠成常规形态
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        StringBuilder sb = new StringBuilder(s.length());
        boolean pendingSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isInvisible(c)) {
                continue;
            }
            if (Character.isWhitespace(c)) {
                // 连续空白（含换行/制表/全角空格 NFKC 后的普通空格）只留一个
                pendingSpace = sb.length() > 0;
                continue;
            }
            if (pendingSpace) {
                sb.append(' ');
                pendingSpace = false;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 比对键：去掉全部空白并转小写。
     *
     * <p>转小写正是「英文忽略大小写」的实现点：{@code ADMIN} / {@code Admin} / 全角
     * {@code ａｄｍｉｎ}（NFKC 后为半角）都会归一成 {@code admin}。</p>
     */
    public static String key(String raw) {
        return sanitize(raw).replaceAll("\\s+", "").toLowerCase();
    }

    /**
     * 解析后台配置的保留昵称文本。
     *
     * <p>兼容半角/全角逗号、换行、分号作分隔；每项都过 {@link #sanitize}，
     * 并按 {@link #key} 去重（于是重复填 {@code admin} / {@code ADMIN} / {@code ａｄｍｉｎ}
     * 只会留一条）。空白项直接丢弃，空输入返回空列表。</p>
     *
     * <p>输出的**大小写保持站长填写的样子**（只做 NFKC 折半角），转小写只发生在
     * {@link #key} 的比对阶段：这样后台里看到的就是自己填的词，而匹配依旧忽略大小写。</p>
     */
    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        LinkedHashMap<String, String> byKey = new LinkedHashMap<>();
        for (String part : raw.split(RESERVED_SEPARATORS)) {
            putReserved(byKey, part);
        }
        return List.copyOf(byKey.values());
    }

    /**
     * 完整的保留名单：后台配置的名单 + 站长署名，按归一化键去重。
     *
     * <p>去重是必要的：站长署名常常就是配置里已有的「博主」；不去重的话
     * 下发给前台的数组会出现重复项。保留首次出现的位置，顺序稳定。</p>
     *
     * @param configuredText 后台配置的保留昵称（逗号分隔），可为空 —— 为空即表示不拦截
     * @param ownerNickname  站长署名，可为空
     */
    public static List<String> reserved(String configuredText, String ownerNickname) {
        // LinkedHashMap 按 key 去重且保持插入顺序
        LinkedHashMap<String, String> byKey = new LinkedHashMap<>();
        for (String name : parse(configuredText)) {
            putReserved(byKey, name);
        }
        putReserved(byKey, ownerNickname);
        return List.copyOf(byKey.values());
    }

    /**
     * 昵称是否**包含**任一保留词。
     *
     * @return 命中的保留词（原样返回，便于给出「不能包含『xx』」这类提示）；未命中返回 {@code null}
     */
    public static String firstMatch(String raw, Collection<String> reserved) {
        if (reserved == null || reserved.isEmpty()) {
            return null;
        }
        String key = key(raw);
        if (key.isEmpty()) {
            return null;
        }
        for (String name : reserved) {
            String reservedKey = key(name);
            if (!reservedKey.isEmpty() && key.contains(reservedKey)) {
                return name;
            }
        }
        return null;
    }

    private static void putReserved(LinkedHashMap<String, String> byKey, String name) {
        String k = key(name);
        // 空值 / 全是不可见字符的项不构成有效保留项
        if (!k.isEmpty()) {
            byKey.putIfAbsent(k, sanitize(name));
        }
    }

    /** 零宽、方向控制、BOM 等不可见字符：会影响肉眼判断，统一剔除 */
    private static boolean isInvisible(char c) {
        return switch (c) {
            case '\u200B', '\u200C', '\u200D', '\u200E', '\u200F',
                 '\u2060', '\uFEFF', '\u00AD' -> true;
            default -> Character.isISOControl(c);
        };
    }
}
