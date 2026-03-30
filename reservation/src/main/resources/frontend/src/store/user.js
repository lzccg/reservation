import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // State
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const token = ref(localStorage.getItem('token') || '')
  
  // Getters
  const isLogin = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '') // student, company, admin
  
  // Actions
  const setUserInfo = (data) => { 
    userInfo.value = data 
    localStorage.setItem('userInfo', JSON.stringify(data))
  }
  
  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const logout = () => {
    userInfo.value = null
    token.value = ''
    localStorage.removeItem('userInfo')
    localStorage.removeItem('token')
  }
  
  return { userInfo, token, isLogin, role, setUserInfo, setToken, logout }
})
