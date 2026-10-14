import axios from 'axios'
import { message } from '@/utils/feedback'

/** 统一响应状态码 */
export const CODE = {
  SUCCESS: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  ERROR: 500
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 20000
})

service.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res == null || typeof res !== 'object' || res.code === undefined) {
      return res
    }

    if (res.code === CODE.SUCCESS) {
      return res
    }

    message.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    message.error(error.response?.data?.msg || error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default service
