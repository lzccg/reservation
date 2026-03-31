import request from '@/utils/request'

export const getCompanyProfile = () => {
  return request({ url: '/company/profile', method: 'get' })
}

export const updateCompanyProfile = (data) => {
  return request({ url: '/company/profile', method: 'put', data })
}

export const publishSession = (data) => {
  return request({ url: '/company/session', method: 'post', data })
}

export const updateSession = (id, data) => {
  return request({ url: `/company/session/${id}`, method: 'put', data })
}

export const getCompanySessions = (params) => {
  return request({ url: '/company/sessions', method: 'get', params })
}
