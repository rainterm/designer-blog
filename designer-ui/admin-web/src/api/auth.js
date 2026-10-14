import request from './request'

/** 登录 */
export const login = (data) => request.post('/admin-api/system/auth/login', data)

/** 获取验证码（按账号失败次数决定是否返回图片） */
export const getCaptcha = (username) =>
  request.get('/admin-api/system/auth/captcha', { params: { username: username || '' } })

/** 退出登录 */
export const logout = () => request.post('/admin-api/system/auth/logout')

/** 当前用户信息与权限 */
export const getUserInfo = () => request.get('/admin-api/system/auth/info')

/** 当前用户动态路由 */
export const getRouters = () => request.get('/admin-api/system/auth/routers')

/** 已登录签发一次性 SSO 票据；data.redirect 可选，服务端按白名单校验 */
export const createSsoTicket = (data) => request.post('/admin-api/system/auth/sso/ticket', data || {})

/** 一次性票据换 token */
export const exchangeSsoTicket = (ticket) =>
  request.post('/admin-api/system/auth/sso/exchange', { ticket })

/** 个人中心（后端从 query 绑定，无 @Body） */
export const getProfile = () => request.get('/admin-api/system/profile')
export const updateProfile = (data) => request.put('/admin-api/system/profile/update', null, { params: data })
export const changePassword = (data) => request.put('/admin-api/system/profile/password', null, { params: data })
