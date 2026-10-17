import request from '@/api/request'

// ---------------------- 分类 ----------------------
export const pageCategories = (params) => request.get('/admin-api/blog/category/page', { params })
export const listCategories = () => request.get('/admin-api/blog/category/list')
export const getCategory = (id) => request.get(`/admin-api/blog/category/${id}`)
export const createCategory = (data) => request.post('/admin-api/blog/category/add', data)
export const updateCategory = (data) => request.put('/admin-api/blog/category/update', data)
export const deleteCategories = (ids) =>
  request.delete('/admin-api/blog/category/remove', { params: { ids: ids.join(',') } })

// ---------------------- 标签 ----------------------
export const pageTags = (params) => request.get('/admin-api/blog/tag/page', { params })
export const listTags = () => request.get('/admin-api/blog/tag/list')
export const getTag = (id) => request.get(`/admin-api/blog/tag/${id}`)
export const createTag = (data) => request.post('/admin-api/blog/tag/add', data)
export const updateTag = (data) => request.put('/admin-api/blog/tag/update', data)
export const deleteTags = (ids) =>
  request.delete('/admin-api/blog/tag/remove', { params: { ids: ids.join(',') } })

// ---------------------- 文章 ----------------------
export const pagePosts = (params) => request.get('/admin-api/blog/post/page', { params })
export const getPost = (id) => request.get(`/admin-api/blog/post/${id}`)
export const createPost = (data) => request.post('/admin-api/blog/post/add', data)
export const updatePost = (data) => request.put('/admin-api/blog/post/update', data)
export const publishPost = (id, publish = true) =>
  request.put(`/admin-api/blog/post/publish/${id}`, { publish })
export const deletePosts = (ids) =>
  request.delete('/admin-api/blog/post/remove', { params: { ids: ids.join(',') } })

// ---------------------- 评论 ----------------------
export const pageComments = (params) => request.get('/admin-api/blog/comment/page', { params })
/** 待审批评论数：菜单红点用；轮询请求失败不弹提示 */
export const getPendingCommentCount = () =>
  request.get('/admin-api/blog/comment/pending-count', { silent: true })
export const treeComments = (params) => request.get('/admin-api/blog/comment/tree', { params })
export const auditComment = (data) => request.put('/admin-api/blog/comment/audit', data)
export const replyComment = (data) => request.post('/admin-api/blog/comment/reply', data)
export const addComment = (data) => request.post('/admin-api/blog/comment/add', data)
export const updateComment = (data) => request.put('/admin-api/blog/comment/update', data)
export const topComment = (data) => request.put('/admin-api/blog/comment/top', data)
export const deleteComments = (ids) =>
  request.delete('/admin-api/blog/comment/remove', { params: { ids: ids.join(',') } })

// ---------------------- 链接 ----------------------
export const pageLinks = (params) => request.get('/admin-api/blog/link/page', { params })
export const getLink = (id) => request.get(`/admin-api/blog/link/${id}`)
export const createLink = (data) => request.post('/admin-api/blog/link/add', data)
export const updateLink = (data) => request.put('/admin-api/blog/link/update', data)
export const deleteLinks = (ids) =>
  request.delete('/admin-api/blog/link/remove', { params: { ids: ids.join(',') } })

// ---------------------- 站点设置 ----------------------
export const getSetting = () => request.get('/admin-api/blog/setting')
export const updateSetting = (data) => request.put('/admin-api/blog/setting', data)

// ---------------------- 访问统计 ----------------------
export const getVisitOverview = (params) => request.get('/admin-api/blog/visit/overview', { params })
