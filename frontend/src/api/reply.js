import request from '@/utils/request'

export function submitReply(data) {
  return request({
    url: '/reply/submit',
    method: 'post',
    data
  })
}

export function getReplyThreads(reviewRecordId) {
  return request({
    url: `/reply/review/${reviewRecordId}/threads`,
    method: 'get'
  })
}

export function getThreadReplies(reviewRecordId, threadNumber) {
  return request({
    url: `/reply/review/${reviewRecordId}/thread/${threadNumber}`,
    method: 'get'
  })
}

export function getSuggestedRefs(params) {
  return request({
    url: '/reply/suggest',
    method: 'get',
    params
  })
}

export function addReviewerResponse(replyId, data) {
  return request({
    url: `/reply/${replyId}/response`,
    method: 'put',
    data
  })
}

export function resolveReply(replyId) {
  return request({
    url: `/reply/${replyId}/resolve`,
    method: 'put'
  })
}

export function getAuthorReplies(params) {
  return request({
    url: '/reply/author/list',
    method: 'get',
    params
  })
}
