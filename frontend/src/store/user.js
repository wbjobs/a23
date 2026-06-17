import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, logout as apiLogout, getUserInfo as apiGetUserInfo } from '@/api/auth'
import { setToken, removeToken, setUserInfo, removeUserInfo, getToken, getUserInfo } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref(getUserInfo() || null)
  const role = ref(userInfo.value?.role || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'admin')
  const isAuthor = computed(() => role.value === 'author')
  const isReviewer = computed(() => role.value === 'reviewer')

  async function login(loginForm) {
    try {
      const res = await apiLogin(loginForm)
      const data = res.data || res
      if (data.token) {
        token.value = data.token
        setToken(data.token)
      }
      if (data.userInfo) {
        userInfo.value = data.userInfo
        role.value = data.userInfo.role || ''
        setUserInfo(data.userInfo)
      } else if (data.username || data.id) {
        userInfo.value = data
        role.value = data.role || ''
        setUserInfo(data)
      }
      return res
    } catch (error) {
      throw error
    }
  }

  async function register(registerForm) {
    try {
      const res = await apiRegister(registerForm)
      return res
    } catch (error) {
      throw error
    }
  }

  async function logout() {
    try {
      await apiLogout()
    } catch (e) {
      console.error('退出登录请求失败', e)
    }
    token.value = ''
    userInfo.value = null
    role.value = ''
    removeToken()
    removeUserInfo()
  }

  async function fetchUserInfo() {
    try {
      const res = await apiGetUserInfo()
      const data = res.data || res
      if (data) {
        userInfo.value = data
        role.value = data.role || ''
        setUserInfo(data)
      }
      return res
    } catch (error) {
      throw error
    }
  }

  return {
    token,
    userInfo,
    role,
    isLoggedIn,
    isAdmin,
    isAuthor,
    isReviewer,
    login,
    register,
    logout,
    fetchUserInfo
  }
})
