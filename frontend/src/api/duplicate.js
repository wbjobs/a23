import request from '@/utils/request'

export function checkPaper(data) {
  return request({
    url: '/duplicate/check',
    method: 'post',
    data
  })
}

export function getCheckResult(paperId) {
  return request({
    url: `/duplicate/paper/${paperId}/result`,
    method: 'get'
  })
}

export function getCheckHistory(paperId) {
  return request({
    url: `/duplicate/paper/${paperId}/history`,
    method: 'get'
  })
}

export function getReport(reportId) {
  return request({
    url: `/duplicate/report/${reportId}`,
    method: 'get'
  })
}

export function updateReportStatus(reportId, data) {
  return request({
    url: `/duplicate/report/${reportId}/status`,
    method: 'put',
    data
  })
}

export function detectSausage(paperId) {
  return request({
    url: `/duplicate/paper/${paperId}/sausage`,
    method: 'get'
  })
}
