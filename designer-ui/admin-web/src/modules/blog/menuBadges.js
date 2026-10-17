import { getPendingCommentCount } from '@/modules/blog/api'

/**
 * 菜单红点路径（同时也是后端菜单的 path）。
 *
 * <p>单独导出常量而不是到处写字符串：业务动作后要刷新红点时（如审核、删除评论），
 * 调用方用 {@code badgeStore.refresh(BLOG_MENU_BADGE.COMMENT)}，路径只在这里定义一次。</p>
 */
export const BLOG_MENU_BADGE = {
  /** 待审批评论数 */
  COMMENT: '/blog/comment'
}

/**
 * 博客模块的菜单红点定义。
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code path}：对应菜单项的路径，菜单渲染时按它自动挂载（父级菜单会汇总子级数量）；</li>
 *   <li>{@code load}：返回数量（数字或 Promise），由 {@code useMenuBadgeStore} 统一轮询；</li>
 *   <li>{@code perm}：可选，没有该权限就不请求，避免无谓的 403；</li>
 *   <li>{@code interval}：可选，轮询间隔（毫秒），默认 60s；传 0 表示只在手动 refresh 时拉取。</li>
 * </ul>
 *
 * <p>新增一个红点只需在这里追加一条，侧栏 / 顶栏 / 双列菜单都不需要改。</p>
 */
export default [
  {
    path: BLOG_MENU_BADGE.COMMENT,
    perm: 'blog:comment:list',
    load: async () => (await getPendingCommentCount()).data
  }
]
