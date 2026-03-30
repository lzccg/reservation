import axios from 'axios'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

// 请求拦截器：添加token
request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  response => {
    // 假设后端返回格式为 { code: 200, data: ..., message: "xxx" }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data
  },
  error => {
    ElMessage.error(error.response?.data?.message || '网络请求异常')
    return Promise.reject(error)
  }
)

export default request
