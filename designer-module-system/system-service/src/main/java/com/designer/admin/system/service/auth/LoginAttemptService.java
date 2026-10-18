package com.designer.admin.system.service.auth;

/**
 * 登录失败计数与账号锁定，以及「本次登录是否需要人机验证」的判据。
 *
 * <p><b>为什么不叫 CaptchaService</b>：这个类原先叫 CaptchaService，把三件事混在一起 ——
 * 画算术题验证码、累计失败次数、锁定账号。验证码形态换成滑块后
 * （AJ-Captcha，见 {@code com.designer.admin.framework.captcha}），画图那部分整体删除：
 * 底图与拼图由插件现生成，凭据校验交给 {@code CaptchaVerifier}。
 * 唯一与验证码形态无关、因而必须留下的是<b>策略</b>——
 * 「这个账号失败够次数了吗，要不要拦一道人机验证」，以及配套的计数与锁定。
 * 于是改名为此，职责收窄为「登录尝试」本身。</p>
 *
 * <p><b>与 {@code CaptchaVerifier} 的分工</b>：这里只回答「要不要验」，
 * 「凭据是否有效」由 {@code CaptchaVerifier} 回答。调用方（{@code AuthService}）
 * 先问这里，需要验再去调那边，顺序不能反 —— 凭据是一次性的，
 * 对不需要验证的账号调一次校验只会把它白白消费掉。</p>
 *
 * @author 雨夜终端
 */
public interface LoginAttemptService {

    /**
     * 是否需要校验人机验证：全局开启且该账号失败次数达到阈值。
     *
     * <p>滑块本身不需要「按账号生成」，所以这里只回答是 / 否，
     * 前端据此决定要不要调 {@code /app-api/captcha/get} 取一张。</p>
     *
     * @param username 登录账号
     * @return 是否需要滑块验证
     */
    boolean isCaptchaRequired(String username);

    /**
     * 查询账号当前的连续登录失败次数。
     *
     * @param username 登录账号
     * @return 失败次数；账号为空或计数异常时返回 0
     */
    long countLoginFail(String username);

    /**
     * 校验账号是否处于锁定期，锁定中直接抛出业务异常并提示剩余时间。
     *
     * @param username 登录账号
     */
    void assertNotLocked(String username);

    /**
     * 记录一次登录失败。始终累计失败次数（供验证码阈值）；锁定开启时达到上限则锁定。
     *
     * @param username 登录账号
     * @return 当前失败次数
     */
    long saveLoginFail(String username);

    /**
     * 登录成功后清空失败计数与锁定标记。
     *
     * @param username 登录账号
     */
    void removeLoginFail(String username);
}
