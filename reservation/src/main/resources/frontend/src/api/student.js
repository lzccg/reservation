import request from '@/utils/request'

export const getStudentProfile = () => {
  return request({ url: '/api/student/profile', method: 'get' })
}

export const updateStudentProfile = (data) => {
  return request({ url: '/api/student/profile', method: 'put', data })
}

export const getSessionList = (params) => {
  return request({ url: '/api/student/sessions', method: 'get', params })
}

export const reserveSession = (sessionId) => {
  return request({ url: `/api/student/reserve/${sessionId}`, method: 'post' })
}

export const getReserveHistory = () => {
  return request({ url: '/api/student/history', method: 'get' })
}
