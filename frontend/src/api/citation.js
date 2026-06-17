import request from '@/utils/request'

export function getCitationNetwork(paperId) {
  return request({
    url: `/citation/paper/${paperId}/network`,
    method: 'get'
  })
}

export function getCoreReferences(paperId, topK = 5) {
  return request({
    url: `/citation/paper/${paperId}/core`,
    method: 'get',
    params: { topK }
  })
}
