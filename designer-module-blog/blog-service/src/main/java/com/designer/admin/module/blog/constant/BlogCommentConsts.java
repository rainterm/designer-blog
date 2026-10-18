package com.designer.admin.module.blog.constant;

/**
 * 评论模块的固定值：状态码、作者类型、长度上限与访客密钥请求头。
 *
 * <p><b>为什么不留在实现类里</b>：这些值跨越了 service、controller 与 DTO 校验三层 ——
 * 状态码被审核、展示、统计同时使用，长度上限必须与数据库列宽一致，
 * 请求头名在「写入」与「读取」两端必须逐字相同。留在实现类里，其他层只能复制字面量，
 * 于是「改了列宽但只改了一处校验」这类问题会静默发生。</p>
 *
 * @author 雨夜终端
 */
public final class BlogCommentConsts {

    /** 内容长度上限，与 blog_comment.content 列宽一致（VARCHAR(2000)） */
    public static final int MAX_CONTENT_LENGTH = 2000;

    /** 昵称长度上限，与 blog_comment.nickname 列宽一致（VARCHAR(50)） */
    public static final int MAX_NICKNAME_LENGTH = 50;

    /** 邮箱长度上限，与 blog_comment.email 列宽一致（VARCHAR(100)） */
    public static final int MAX_EMAIL_LENGTH = 100;

    /** 作者类型：访客 */
    public static final int AUTHOR_TYPE_VISITOR = 0;

    /** 作者类型：博主 */
    public static final int AUTHOR_TYPE_OWNER = 1;

    /** 审核状态：待审核 */
    public static final int STATUS_PENDING = 0;

    /** 审核状态：通过 */
    public static final int STATUS_APPROVED = 1;

    /** 审核状态：拒绝 */
    public static final int STATUS_REJECTED = 2;

    /** 置顶标记：取消置顶 */
    public static final int TOP_FLAG_OFF = 0;

    /** 置顶标记：置顶 */
    public static final int TOP_FLAG_ON = 1;

    /** 访客身份密钥请求头：前端首次提交评论后保存，后续凭它查看 / 删除自己的评论 */
    public static final String COMMENTER_KEY_HEADER = "X-Commenter-Key";

    /** 顶级评论的 parentId */
    public static final long ROOT_PARENT_ID = 0L;

    private BlogCommentConsts() {
    }
}
