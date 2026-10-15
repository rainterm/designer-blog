package com.designer.admin.system.model.vo;

import lombok.Data;

/**
 * 登录前的人机验证提示。
 *
 * <p>滑块验证码本身不从这里下发：底图与拼图由 AJ-Captcha 的 {@code /captcha/get} 现生成，
 * 凭据由 {@code /captcha/check} 下发（前台评论与后台登录共用同一套端点）。
 * 这个对象只回答一个问题 —— <b>本次登录要不要验</b>
 * （全局开关开启且该账号连续失败达阈值，见 {@code LoginAttemptService}）。</p>
 */
@Data
public class CaptchaVO {

    /** 是否需要人机验证；false 时登录请求不必携带 captchaVerification */
    private boolean enabled;
}
