import request from '@/utils/request'

export function submitReview(data) {
  return request({
    url: '/review/submit',
    method: 'post',
    data
  })
}

export function assignReviewers(data) {
  return request({
    url: '/review/assign',
    method: 'post',
    data
  })
}

export function getReviewsByPaper(paperId) {
  return request({
    url: `/review/paper/${paperId}`,
    method: 'get'
  })
}

export function getReviewsByReviewer() {
  return request({
    url: '/review/reviewer',
    method: 'get'
  })
}
