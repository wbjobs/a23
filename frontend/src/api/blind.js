import request from '@/utils/request'

export function getBlindConfig() {
  return request({
    url: '/blind/config',
    method: 'get'
  })
}

export function updateBlindConfig(data) {
  return request({
    url: '/blind/config',
    method: 'put',
    data
  })
}

export function togglePaperBlind(paperId, enabled) {
  return request({
    url: `/blind/paper/${paperId}/toggle`,
    method: 'put',
    data: { enabled }
  })
}

export function getDesensitizedPaper(paperId) {
  return request({
    url: `/blind/paper/${paperId}`,
    method: 'get'
  })
}
