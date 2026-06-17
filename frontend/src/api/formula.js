import request from '@/utils/request'

export function getFormulaList(paperId) {
  return request({
    url: `/formula/paper/${paperId}`,
    method: 'get'
  })
}

export function getFormulaByNumber(paperId, number) {
  return request({
    url: `/formula/paper/${paperId}/number/${number}`,
    method: 'get'
  })
}

export function searchFormula(paperId, pattern) {
  return request({
    url: `/formula/paper/${paperId}/search`,
    method: 'get',
    params: { pattern }
  })
}
