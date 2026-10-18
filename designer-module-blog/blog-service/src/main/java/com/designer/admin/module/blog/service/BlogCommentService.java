package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.module.blog.model.dto.CommentAddDTO;
import com.designer.admin.module.blog.model.dto.CommentAuditDTO;
import com.designer.admin.module.blog.model.dto.CommentQueryDTO;
import com.designer.admin.module.blog.model.dto.CommentReplyDTO;
import com.designer.admin.module.blog.model.dto.CommentTopDTO;
import com.designer.admin.module.blog.model.dto.CommentUpdateDTO;
import com.designer.admin.module.blog.model.dto.PublicCommentSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogComment;
import com.designer.admin.module.blog.model.vo.AdminCommentVO;
import com.designer.admin.module.blog.model.vo.MyCommentVO;
import com.designer.admin.module.blog.model.vo.PublicCommentVO;

import java.util.List;

/**
 * 博客评论：后台审核 / 回复管理，前台提交、展示与访客自助删除。
 *
 * @author 雨夜终端
 */
public interface BlogCommentService {

    /**
     * 评论分页（后台列表）。
     *
     * @param query 分页与文章 / 状态 / 昵称等筛选条件
     * @return 评论分页结果
     */
    IPage<BlogComment> listPage(CommentQueryDTO query);

    /**
     * 待审批评论数，用于侧栏「评论管理」菜单红点。
     *
     * <p>只做一次 count，不复用 {@link #listPage}：红点每次轮询都要拉，没必要为此
     * 查出一行数据、再补一次文章标题。筛选条件与列表页「状态 = 待审」完全一致。</p>
     *
     * @return 待审批条数
     */
    long countPending();

    /**
     * 审核评论：通过或拒绝。
     *
     * @param dto 审核参数（评论 ID 与目标状态）
     */
    void audit(CommentAuditDTO dto);

    /**
     * 批量删除评论。
     *
     * @param ids 评论 ID 列表
     */
    void remove(List<Long> ids);

    /**
     * 管理员回复评论：挂到目标评论下，直接以「已通过」落库，无需再审。
     *
     * <p>与访客提交的区别：</p>
     * <ul>
     *   <li>不走验证码与频率限制——这是站长的正常运营动作；</li>
     *   <li>{@code status} 直接置 1，回复即刻对访客可见；</li>
     *   <li><b>不受站点评论总开关限制</b>：开关约束的是访客入口，站长关闭评论后仍需要能补充说明；</li>
     *   <li><b>不记录 IP / 归属地</b>：回复代表站点方，公开归属地会暴露站长的位置信息，没有展示价值。</li>
     * </ul>
     *
     * @param dto 回复参数（目标评论 ID 与内容）
     */
    void reply(CommentReplyDTO dto);

    /**
     * 站长以站内身份新增评论。
     *
     * <p>与访客提交不同：不走验证码与频率限制，署名使用站点配置的站长昵称，且直接以「已通过」落库。</p>
     *
     * @param dto 评论参数（文章 ID 与内容）
     */
    void addByOwner(CommentAddDTO dto);

    /**
     * 修改评论内容（不改变审核状态）。
     *
     * @param dto 修改参数（评论 ID 与新内容）
     */
    void updateContent(CommentUpdateDTO dto);

    /**
     * 置顶 / 取消置顶评论。
     *
     * @param dto 置顶参数（评论 ID 与置顶标记）
     */
    void updateTop(CommentTopDTO dto);

    /**
     * 前台某篇文章的已通过评论树（两级：顶级评论 + 回复）。
     *
     * @param postId 文章 ID
     * @return 评论树
     */
    List<PublicCommentVO> listApprovedTree(Long postId);

    /**
     * 后台按文章查看评论树；与前台树结构一致，但不按状态过滤，以顶级评论为单位分页。
     *
     * @param postId 文章 ID
     * @param query  分页参数
     * @return 评论树分页结果
     */
    PageResult<AdminCommentVO> listAdminTreePage(Long postId, PageQuery query);

    /**
     * 访客提交评论：校验站点开关、昵称保留词、验证码与频率限制后落库待审。
     *
     * @param dto 评论参数
     */
    void addPublic(PublicCommentSaveDTO dto);

    /**
     * 访客查看自己在该文章下「待审核」的评论（凭评论人密钥匹配）。
     *
     * @param postId 文章 ID
     * @param rawKey 评论人密钥（请求头 X-Commenter-Key 原值）
     * @return 评论列表
     */
    List<MyCommentVO> listMyComments(Long postId, String rawKey);

    /**
     * 访客删除自己的评论：按 ID + 密钥哈希双重匹配，匹配不到一律报「不存在」。
     *
     * @param id     评论 ID
     * @param rawKey 评论人密钥（请求头 X-Commenter-Key 原值）
     */
    void removeMine(Long id, String rawKey);

    /**
     * 前台已通过评论的分页版树：以顶级评论为单位分页，每条顶级评论带上其已通过的回复。
     *
     * @param postId 文章 ID
     * @param query  分页参数
     * @return 评论分页结果
     */
    PageResult<PublicCommentVO> listApprovedPage(Long postId, PageQuery query);
}
