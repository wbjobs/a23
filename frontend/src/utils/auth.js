const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  return localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  return localStorage.removeItem(TOKEN_KEY)
}

export function getUserInfo() {
  const info = localStorage.getItem(USER_INFO_KEY)
  if (info) {
    try {
      return JSON.parse(info)
    } catch (e) {
      console.error('解析用户信息失败', e)
      return null
    }
  }
  return null
}

export function setUserInfo(userInfo) {
  return localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

export function removeUserInfo() {
  return localStorage.removeItem(USER_INFO_KEY)
}

export function clearAuth() {
  removeToken()
  removeUserInfo()
}
