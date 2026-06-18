import request from '@/utils/request'

export function getDashboard() {
  return request({
    url: '/quality/dashboard',
    method: 'get'
  })
}

export function getReviewerQuality(reviewerId) {
  return request({
    url: `/quality/reviewer/${reviewerId}`,
    method: 'get'
  })
}

export function getReviewerRanking(params) {
  return request({
    url: '/quality/reviewer/ranking',
    method: 'get',
    params
  })
}

export function getScoreDistribution(params) {
  return request({
    url: '/quality/distribution/score',
    method: 'get',
    params
  })
}

export function getDurationDistribution(params) {
  return request({
    url: '/quality/distribution/duration',
    method: 'get',
    params
  })
}

export function getTrendData(params) {
  return request({
    url: '/quality/trend',
    method: 'get',
    params
  })
}

export function calculateQuality(data) {
  return request({
    url: '/quality/calculate',
    method: 'post',
    data
  })
}
