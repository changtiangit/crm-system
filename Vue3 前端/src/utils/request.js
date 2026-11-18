import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

// 请求拦截：自动带上 token
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一处理业务码与登录失效
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 业务信封 { code, data, message } 里的 code 是数字状态码；
    // 商品等实体自身也有 code 字段（如 SP0001），必须排除，否则会被误判为失败
    if (res && typeof res === 'object' && typeof res.code === 'number') {
      if (res.code === 0 || res.code === 200) return res.data
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    // 后端未启动时，开发代理会直接返回 500 且响应体为空，给出可执行的提示
    const rawMessage = typeof error.response?.data === 'string' ? '' : error.response?.data?.message
    const isGatewayError = (status === 500 || status === 502 || status === 504) && !rawMessage
    if (status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.replace('/login')
      ElMessage.error('登录状态已失效，请重新登录')
    } else if (isGatewayError) {
      ElMessage.error(`服务暂时不可用（${status}），请确认后端已启动后重试`)
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('无法连接服务，请检查网络或后端服务')
    } else {
      ElMessage.error(rawMessage || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default service
