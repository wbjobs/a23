import request from '@/utils/request'

export function getPaperTopics(paperId) {
  return request({
    url: `/topic/paper/${paperId}`,
    method: 'get'
  })
}

export function getCoreTopics(paperId) {
  return request({
    url: `/topic/paper/${paperId}/core`,
    method: 'get'
  })
}

export function getOptimizedKeywords(paperId) {
  return request({
    url: `/topic/paper/${paperId}/keywords`,
    method: 'get'
  })
}

export function extractTopics(paperId) {
  return request({
    url: `/topic/paper/${paperId}/extract`,
    method: 'post'
  })
}
