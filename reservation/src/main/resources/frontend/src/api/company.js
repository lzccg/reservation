import request from '@/utils/request'

export const getCompanyProfile = () => {
  return request({ url: '/api/company/profile', method: 'get' })
}

export const updateCompanyProfile = (data) => {
  return request({ url: '/api/company/profile', method: 'put', data })
}

export const publishSession = (data) => {
  return request({ url: '/api/company/session', method: 'post', data })
}

export const updateSession = (id, data) => {
  return request({ url: `/api/company/session/${id}`, method: 'put', data })
}

export const getCompanySessions = (params) => {
  return request({ url: '/api/company/sessions', method: 'get', params })
}
