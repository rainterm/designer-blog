package com.designer.admin.system.model.vo.monitor;

import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 在线用户会话
 *
 * @author 雨夜终端
 */
@Data
public class OnlineUserVO {

    /** token 值 */
    private String tokenId;

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 登录 IP */
    private String ip;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录时间 */
    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    /** 剩余有效秒数；-1 永久 */
    private Long timeout;
}
