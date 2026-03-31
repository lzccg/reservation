import request from '@/utils/request'

export const getStudentProfile = () => {
  return request({ url: '/student/profile', method: 'get' })
}

export const updateStudentProfile = (data) => {
  return request({ url: '/student/profile', method: 'put', data })
}

export const getSessionList = (params) => {
  return request({ url: '/student/sessions', method: 'get', params })
}

export const reserveSession = (sessionId) => {
  return request({ url: `/student/reserve/${sessionId}`, method: 'post' })
}

export const getReserveHistory = () => {
  return request({ url: '/student/history', method: 'get' })
}
