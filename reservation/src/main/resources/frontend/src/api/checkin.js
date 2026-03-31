import request from '@/utils/request'

export const uploadFaceInfo = (base64Str) => {
  return request({ url: '/checkin/face-register', method: 'post', data: { image: base64Str } })
}

export const verifyFaceCheckin = (sessionId, base64Str) => {
  return request({ url: `/checkin/verify/${sessionId}`, method: 'post', data: { image: base64Str } })
}
