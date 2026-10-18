package com.designer.admin.system.constant;

/**
 * 系统模块的 Redis 键（前缀）。
 *
 * <p>键名是跨版本的数据契约：写侧与读侧必须引用同一个常量，不允许在业务类里再写一份字面量，
 * 否则改前缀时漏掉一处，就会出现「写得进去、读不出来」的静默故障。
 * 这里只放系统模块自己拥有的键；限流等框架级键见 {@code framework.redis.RedisKeyConsts}。</p>
 *
 * @author 雨夜终端
 */
public final class SystemRedisKeyConsts {

    /** 登录失败计数：{@code login:fail:{username}}，值为失败次数 */
    public static final String LOGIN_FAIL_PREFIX = "login:fail:";

    /** 账号锁定标记：{@code login:lock:{username}}，存在即表示锁定中，TTL 为剩余锁定时长 */
    public static final String LOGIN_LOCK_PREFIX = "login:lock:";

    /** SSO 一次性票据：{@code sso:ticket:{ticket}}，值为签发方的 Sa-Token */
    public static final String SSO_TICKET_PREFIX = "sso:ticket:";

    /** 参数配置缓存：{@code sys:config:{configKey}}，值为参数值 */
    public static final String CONFIG_CACHE_PREFIX = "sys:config:";

    /**
     * Sa-Token 会话键的固定后缀。
     *
     * <p>完整键由 Sa-Token 决定：{@code {tokenName}:{loginType}:session:{loginId}}。
     * 前缀部分只能在运行时拼（token 名与登录类型都可能被配置改），
     * 这里固定住后缀，避免在线用户列表靠硬编码的 {@code ":session:"} 反推。</p>
     */
    public static final String SA_TOKEN_SESSION_SUFFIX = ":session:";

    private SystemRedisKeyConsts() {
    }
}
