import request from '@/utils/request'

export function recommendReviewers(params) {
  return request({
    url: '/reviewer/recommend',
    method: 'get',
    params
  })
}

export function getReviewerById(id) {
  return request({
    url: `/reviewer/${id}`,
    method: 'get'
  })
}
