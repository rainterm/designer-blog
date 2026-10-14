import request from '@/api/request'

/** 通用文件上传，dir 为业务子目录（如 avatar） */
export const uploadFile = (file, dir = 'common') => {
  const form = new FormData()
  form.append('file', file)
  form.append('dir', dir)
  return request.post('/admin-api/system/file/upload', form)
}

/** 文件分页列表 */
export const pageFiles = (params) => request.get('/admin-api/system/file/page', { params })

/** 文件详情 */
export const getFile = (id) => request.get(`/admin-api/system/file/${id}`)

/** 批量删除文件 */
export const deleteFiles = (ids) =>
  request.delete('/admin-api/system/file/remove', { params: { ids: ids.join(',') } })
