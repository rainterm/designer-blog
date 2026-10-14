import request from '@/api/request'

/** 分页查询公开文章 */
export function fetchPosts(params) {
  return request.get('/app-api/blog/public/posts', { params })
}

/** 按 slug 获取文章详情 */
export function fetchPostBySlug(slug) {
  return request.get(`/app-api/blog/public/posts/${encodeURIComponent(slug)}`)
}

/** 启用分类列表 */
export function fetchCategories() {
  return request.get('/app-api/blog/public/categories')
}

/** 启用标签列表 */
export function fetchTags() {
  return request.get('/app-api/blog/public/tags')
}

/** 启用链接列表（社交/外链） */
export function fetchLinks() {
  return request.get('/app-api/blog/public/links')
}

/** 站点设置（站点名 / Logo / 版权 / 备案号） */
export function fetchSiteSetting() {
  return request.get('/app-api/blog/public/site')
}

/** 文章已审核评论树（按顶级评论分页） */
export function fetchComments(postId, params) {
  return request.get(`/app-api/blog/public/posts/${postId}/comments`, { params })
}

/** 评论验证码（提交前必须获取，一次性使用） */
export function fetchCommentCaptcha() {
  return request.get('/app-api/blog/public/comment/captcha')
}

/** 提交评论（待审核） */
export function submitComment(data) {
  return request.post('/app-api/blog/public/comments', data)
}

/**
 * 我的待审评论（当前文章）。
 * 密钥走请求头而不是 URL，避免进访问日志 / 浏览器历史 / Referer。
 */
export function fetchMyComments(postId, commenterKey) {
  return request.get(`/app-api/blog/public/posts/${postId}/comments/mine`, {
    headers: { 'X-Commenter-Key': commenterKey }
  })
}

/** 删除自己的评论 */
export function deleteMyComment(id, commenterKey) {
  return request.delete(`/app-api/blog/public/comments/mine/${id}`, {
    headers: { 'X-Commenter-Key': commenterKey }
  })
}
