import { apiFetch } from '@/api/request'

/**
 * 博客公开接口（`/app-api/blog/public/*`）。
 *
 * <p>所有函数都接受可选的 `options`，页面级取数统一传 `{ silent: true }`：
 * 首屏失败应该走错误页，而不是在访客面前弹一个 toast。</p>
 */

/** 分页查询公开文章 */
export function fetchPosts(params, options) {
  return apiFetch('/app-api/blog/public/posts', { query: params, ...options })
}

/**
 * 按 slug 获取文章详情。
 *
 * <p>这是**纯读**接口：阅读数不由它产生（否则一次 GET 就变成一次数据库写）。
 * 计数由 {@link @/plugins/visit-tracker.client} 在浏览器侧驱动，
 * 本接口只把「尚未回写数据库的增量」加进 `viewCount` 展示值。</p>
 */
export function fetchPostBySlug(slug, options) {
  return apiFetch(`/app-api/blog/public/posts/${encodeURIComponent(slug)}`, options)
}

/** 启用分类列表 */
export function fetchCategories(options) {
  return apiFetch('/app-api/blog/public/categories', options)
}

/** 启用标签列表 */
export function fetchTags(options) {
  return apiFetch('/app-api/blog/public/tags', options)
}

/** 启用链接列表（社交/外链） */
export function fetchLinks(options) {
  return apiFetch('/app-api/blog/public/links', options)
}

/** 站点设置（站点名 / Logo / 版权 / 备案号） */
export function fetchSiteSetting(options) {
  return apiFetch('/app-api/blog/public/site', options)
}

/** 文章已审核评论树（按顶级评论分页） */
export function fetchComments(postId, params, options) {
  return apiFetch(`/app-api/blog/public/posts/${postId}/comments`, { query: params, ...options })
}

/** 提交评论（待审核）。滑块验证码凭据由 /captcha/check 得到，见 @captcha 共享模块 */
export function submitComment(data, options) {
  return apiFetch('/app-api/blog/public/comments', { method: 'POST', body: data, ...options })
}

/**
 * 我的待审评论（当前文章）。
 * 密钥走请求头而不是 URL，避免进访问日志 / 浏览器历史 / Referer。
 */
export function fetchMyComments(postId, commenterKey, options) {
  return apiFetch(`/app-api/blog/public/posts/${postId}/comments/mine`, {
    headers: { 'X-Commenter-Key': commenterKey },
    ...options
  })
}

/** 删除自己的评论 */
export function deleteMyComment(id, commenterKey, options) {
  return apiFetch(`/app-api/blog/public/comments/mine/${id}`, {
    method: 'DELETE',
    headers: { 'X-Commenter-Key': commenterKey },
    ...options
  })
}
