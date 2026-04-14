import request from '@/utils/request'

export const getAdminStats = () => {
  return request({ url: '/admin/stats', method: 'get' })
}

export const getStudentList = (params) => {
  return request({ url: '/admin/students', method: 'get', params })
}

export const getStudentDetail = (id) => {
  return request({ url: `/admin/student/${id}`, method: 'get' })
}

export const deleteStudent = (id) => {
  return request({ url: `/admin/student/${id}`, method: 'delete' })
}

export const toggleStudentStatus = (id, status) => {
  return request({ url: `/admin/student/${id}/status`, method: 'put', data: { status } })
}

export const getCompanyList = (params) => {
  return request({ url: '/admin/companies', method: 'get', params })
}

export const getCompanyDetail = (id) => {
  return request({ url: `/admin/company/${id}`, method: 'get' })
}

export const auditCompany = (id, action, remark) => {
  return request({ url: `/admin/company/${id}/audit`, method: 'post', data: { action, remark } })
}

export const revokeCompany = (id) => {
  return request({ url: `/admin/company/${id}/revoke`, method: 'post' })
}

export const getAllSessions = (params) => {
  return request({ url: '/admin/sessions', method: 'get', params })
}

export const getSessionDetail = (id) => {
  return request({ url: `/admin/session/${id}`, method: 'get' })
}

export const auditSession = (id, action, data) => {
  return request({ url: `/admin/session/${id}/audit`, method: 'post', data: { action, ...data } })
}

export const getTodayCheckinSessions = () => {
  return request({ url: '/admin/checkin/today-sessions', method: 'get' })
}
