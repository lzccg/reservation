import request from '@/utils/request'

export const getAdminStats = () => {
  return request({ url: '/api/admin/stats', method: 'get' })
}

export const getStudentList = (params) => {
  return request({ url: '/api/admin/students', method: 'get', params })
}

export const toggleStudentStatus = (id, status) => {
  return request({ url: `/api/admin/student/${id}/status`, method: 'put', data: { status } })
}

export const getCompanyList = (params) => {
  return request({ url: '/api/admin/companies', method: 'get', params })
}

export const auditCompany = (id, action, remark) => {
  return request({ url: `/api/admin/company/${id}/audit`, method: 'post', data: { action, remark } })
}

export const getAllSessions = (params) => {
  return request({ url: '/api/admin/sessions', method: 'get', params })
}

export const auditSession = (id, action, data) => {
  return request({ url: `/api/admin/session/${id}/audit`, method: 'post', data: { action, ...data } })
}
