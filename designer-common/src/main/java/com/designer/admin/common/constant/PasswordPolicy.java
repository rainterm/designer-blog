package com.designer.admin.common.constant;

/**
 * 密码策略：登录入参与所有「写入密码」的入口共用同一组规则。
 *
 * <p><b>为什么要集中定义</b>：同一份规则散在登录、新增/修改用户、重置密码、个人改密
 * 四五个入口里，改一处漏一处就会产出「建得出来、却登不上去」的账号
 * （写入侧没卡、登录侧卡了，或者反过来）。这里定常量，
 * 后端的参数注解、服务层兜底、前端校验都引它。前端有一份镜像：
 * {@code designer-ui/admin-web/src/constants/password.js}，改动时两边一起改。</p>
 *
 * <p><b>三条规则</b>：长度 6~50；且必须同时包含<b>大写字母、小写字母、数字、特殊符号</b>
 * 四类字符（见 {@link #STRENGTH_REGEXP}）。</p>
 *
 * <p><b>两条规则的落地范围不同</b>：</p>
 * <ul>
 *   <li><b>长度</b>：下限 6 是安全要求，上限 50 只是防御性收口（拒绝畸形入参）。
 *   长度**在登录侧也校验**，目的是让超短密码在一次 BCrypt 比对（约百毫秒，
 *   登录接口最贵的一步）之前就被拒掉。</li>
 *   <li><b>复杂度</b>：这是对**新设密码**的要求，只在写入侧校验（新增 / 重置 / 改密）。
 *   登录侧刻意不验 —— 否则历史上那些纯字母或纯数字的老密码即使输对了也登不进来，
 *   账号会被直接锁死。登录只回答「输入形态是否合理」。</li>
 * </ul>
 *
 * <p><b>校验的边界</b>：{@link #firstViolation} 返回首个不满足的要求，
 * 与本项目 Solon 校验器的行为一致 ——
 * {@code @Length}/{@code @Pattern} 遇到空值一律放行（交由 {@code @NotBlank} 负责），
 * 所以这里的空值也需要调用方自行判断含义（「必填」还是「留空表示不修改」）。</p>
 */
public interface PasswordPolicy {

    /** 密码最短长度 */
    int MIN_LENGTH = 6;

    /** 密码最长长度 */
    int MAX_LENGTH = 50;

    /**
     * 长度不合规的提示。
     *
     * <p>写成常量而不是各处拼字符串：注解的 {@code message} 要求编译期常量表达式，
     * 服务层的兜底校验也要用同一句，手写迟早会改出两套措辞。</p>
     */
    String LENGTH_MESSAGE = "密码长度需在 " + MIN_LENGTH + "~" + MAX_LENGTH + " 个字符之间";

    /** 复杂度不合规的提示 */
    String COMPLEXITY_MESSAGE = "密码需同时包含大写字母、小写字母、数字和特殊符号";

    /**
     * 复杂度正则：必须同时包含大写字母、小写字母、数字、特殊符号。
     *
     * <p>四个零宽断言逐个要求一类字符。其中「特殊符号」取的是
     * <b>ASCII 可见标点</b>（码点 0x21–0x2F、0x3A–0x40、0x5B–0x60、0x7B–0x7E 四段，
     * 即 POSIX 的 {@code [:punct:]}），有意排除空白与中日韩等非 ASCII 字符 ——
     * 否则一个汉字或一个空格就能凑满「特殊符号」，规则形同虚设。
     * 若将来要放开（例如允许全角符号），改这里即可。</p>
     *
     * <p>Solon 的 {@code PatternValidator} 用 {@code matches()} 全匹配（并显式说明过这是为了
     * 防止无锚点正则被「包含即通过」绕过），所以这里不加 {@code ^$}；
     * 末尾的 {@code .+} 不匹配换行，顺带把含换行的密码挡掉。
     * 自身也用 {@code String#matches} 校验，语义与注解保持一致。</p>
     */
    String STRENGTH_REGEXP = "(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!-/:-@\\[-`{-~]).+";

    /** 长度是否符合要求 */
    static boolean isValidLength(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        int length = rawPassword.length();
        return length >= MIN_LENGTH && length <= MAX_LENGTH;
    }

    /** 是否同时包含大写字母、小写字母、数字、特殊符号 */
    static boolean hasAllCharTypes(String rawPassword) {
        return rawPassword != null && rawPassword.matches(STRENGTH_REGEXP);
    }

    /**
     * 返回首个不满足的要求提示；全部满足返回 {@code null}。
     *
     * <p>一次只报一条而不是把所有问题罗列出来：DTO 注解层本来就是一失败即返回
     * （{@code VALIDATE_ALL} 默认 false），服务层与它保持同一种口径，
     * 前端才不会出现「注解报一条、service 报一长串」的两套体验。</p>
     */
    static String firstViolation(String rawPassword) {
        if (!isValidLength(rawPassword)) {
            return LENGTH_MESSAGE;
        }
        if (!hasAllCharTypes(rawPassword)) {
            return COMPLEXITY_MESSAGE;
        }
        return null;
    }

    /** 长度与复杂度是否都满足 */
    static boolean isValid(String rawPassword) {
        return firstViolation(rawPassword) == null;
    }
}
