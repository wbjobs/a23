import request from '@/utils/request'

export function uploadPaper(formData) {
  return request({
    url: '/paper/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getPaperById(id) {
  return request({
    url: `/paper/${id}`,
    method: 'get'
  })
}

export function getPaperList(params) {
  return request({
    url: '/paper/list',
    method: 'get',
    params
  })
}

export function deletePaper(id) {
  return request({
    url: `/paper/${id}`,
    method: 'delete'
  })
}

export function getPaperGraph(id) {
  return request({
    url: `/paper/${id}/graph`,
    method: 'get'
  })
}

export function getSimilarPapers(id) {
  return request({
    url: `/paper/${id}/similar`,
    method: 'get'
  })
}
