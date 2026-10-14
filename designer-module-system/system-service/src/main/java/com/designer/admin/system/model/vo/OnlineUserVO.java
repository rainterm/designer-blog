package com.designer.admin.system.model.vo;

import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 在线用户会话
 */
@Data
public class OnlineUserVO {

    /** token 值 */
    private String tokenId;

    private Long userId;
    private String username;
    private String nickname;
    private String ip;
    private String browser;
    private String os;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    /** 剩余有效秒数；-1 永久 */
    private Long timeout;
}
