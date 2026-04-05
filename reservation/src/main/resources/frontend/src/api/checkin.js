import request from '@/utils/request'

export const getFaceStatus = () => {
  return request({ url: '/checkin/face-status', method: 'get' })
}

export const uploadFaceInfo = (file) => {
  const formData = new FormData()
  formData.append('image', file)
  return request({
    url: '/checkin/face-register',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const verifyFaceCheckin = (sessionId, base64Str) => {
  return request({ url: `/checkin/verify/${sessionId}`, method: 'post', data: { image: base64Str } })
}
