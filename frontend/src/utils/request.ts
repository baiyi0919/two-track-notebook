// H5开发模式走Vite代理，小程序/生产环境用完整地址
// 注意：uni-app条件编译在.ts文件中不生效，这里用运行时判断
const BASE_URL = import.meta.env.DEV ? '/api' : 'http://127.0.0.1:8080/api'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  data?: any
  header?: Record<string, string>
}

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

function getToken(): string {
  return uni.getStorageSync('token') || ''
}

function request<T = any>(options: RequestOptions): Promise<ApiResponse<T>> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options.header
    }
    if (token) {
      header['Authorization'] = token
    }
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => {
        const data = res.data as ApiResponse<T>
        if (data.code === 200) {
          resolve(data)
        } else if (data.code === 401) {
          // token过期，跳转登录
          uni.removeStorageSync('token')
          uni.removeStorageSync('username')
          uni.reLaunch({ url: '/pages/login/index' })
          reject(new Error('登录已过期，请重新登录'))
        } else {
          uni.showToast({ title: data.message || '请求失败', icon: 'none' })
          reject(new Error(data.message))
        }
      },
      fail: (err) => {
        console.error('请求失败:', err)
        uni.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none', duration: 3000 })
        reject(err)
      }
    })
  })
}

export const http = {
  get<T = any>(url: string, data?: any) {
    return request<T>({ url, method: 'GET', data })
  },
  post<T = any>(url: string, data?: any) {
    return request<T>({ url, method: 'POST', data })
  },
  put<T = any>(url: string, data?: any) {
    return request<T>({ url, method: 'PUT', data })
  },
  patch<T = any>(url: string, data?: any) {
    return request<T>({ url, method: 'PATCH', data })
  },
  delete<T = any>(url: string) {
    return request<T>({ url, method: 'DELETE' })
  }
}
