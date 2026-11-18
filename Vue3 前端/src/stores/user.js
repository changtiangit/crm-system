import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import request from '@/utils/request'
import { setDataScope } from '@/utils/scope'

const TOKEN_KEY = 'crm_token'
const USER_KEY = 'crm_user_info'
const SESSIONS_KEY = 'crm_sessions'
/** 同一浏览器最多保留的账号数量（用于「切换账号」） */
const MAX_SESSIONS = 5
const API_BASE = import.meta.env.VITE_API_BASE || '/api'

function readJSON(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function toSession(user, token) {
  return {
    empNo: user.empNo,
    name: user.name,
    phone: user.phone,
    deptName: user.deptName,
    roleNames: (user.roles || []).map((r) => r.name),
    isAdmin: !!user.isAdmin,
    token,
    user,
    loginAt: Date.now()
  }
}

/**
 * 登录态：token 与当前用户（含角色、功能权限）都来自后端。
 * 同时把多个账号的会话保留在本地，实现「切换账号」——切换时不必重新输密码。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref(readJSON(USER_KEY, null))
  const sessions = ref(readJSON(SESSIONS_KEY, []))
  // 数据范围随登录态一起初始化，刷新页面后仍按上次账号的范围过滤
  setDataScope(userInfo.value?.dataScope)

  const isLogin = computed(() => !!token.value)
  const permissionCodes = computed(() => userInfo.value?.permissions || [])
  const currentEmpNo = computed(() => userInfo.value?.empNo || '')

  function persistSessions() {
    localStorage.setItem(SESSIONS_KEY, JSON.stringify(sessions.value))
  }

  /** 把某个账号写入本地会话列表（去重、限量、最新在前） */
  function upsertSession(user, nextToken) {
    if (!user?.empNo) return
    const next = toSession(user, nextToken)
    sessions.value = [next, ...sessions.value.filter((s) => s.empNo !== user.empNo)].slice(0, MAX_SESSIONS)
    persistSessions()
  }

  function removeSession(empNo) {
    sessions.value = sessions.value.filter((s) => s.empNo !== empNo)
    persistSessions()
  }

  /** 应用一个会话为当前登录态 */
  function applySession(nextToken, nextUser) {
    token.value = nextToken || ''
    userInfo.value = nextUser || null
    if (nextToken) localStorage.setItem(TOKEN_KEY, nextToken)
    else localStorage.removeItem(TOKEN_KEY)
    if (nextUser) localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
    else localStorage.removeItem(USER_KEY)
    setDataScope(nextUser?.dataScope)
  }

  /**
   * 登录：手机号/工号 + 密码，或手机号 + 短信验证码。
   * @param {{mode?:'password'|'sms', account?:string, password?:string, phone?:string, code?:string}} payload
   */
  async function login(payload = {}) {
    const res = await request.post('/auth/login', { mode: 'password', ...payload })
    applySession(res?.token, res?.user)
    upsertSession(res?.user, res?.token)
    return userInfo.value
  }

  /** 重新拉取当前用户，用于权限变更后同步（失败时保持原状） */
  async function refreshMe() {
    if (!token.value) return null
    try {
      const user = await request.get('/auth/me')
      if (user) {
        userInfo.value = user
        setDataScope(user.dataScope)
        localStorage.setItem(USER_KEY, JSON.stringify(user))
        const saved = sessions.value.find((s) => s.empNo === user.empNo)
        if (saved) {
          saved.user = user
          saved.name = user.name
          saved.phone = user.phone
          saved.deptName = user.deptName
          saved.roleNames = (user.roles || []).map((r) => r.name)
          saved.isAdmin = !!user.isAdmin
          persistSessions()
        }
      }
      return user
    } catch {
      return null
    }
  }

  /**
   * 切换到另一个已登录的账号。
   * 先用该账号的令牌探活，失效则清理本地记录并提示重新登录。
   */
  async function switchAccount(empNo) {
    if (empNo === currentEmpNo.value) return userInfo.value
    const target = sessions.value.find((s) => s.empNo === empNo)
    if (!target) throw new Error('未找到该账号的登录记录')

    let user
    try {
      const res = await axios.get(`${API_BASE}/auth/me`, {
        headers: { Authorization: `Bearer ${target.token}` },
        timeout: 8000
      })
      user = res.data
    } catch (err) {
      const stale = err?.response?.status === 401
      if (stale) removeSession(empNo)
      const error = new Error(stale ? '该账号的登录状态已失效，请重新输入密码登录' : '切换失败，请检查网络或后端服务')
      error.stale = stale
      throw error
    }

    applySession(target.token, user)
    upsertSession(user, target.token)
    return user
  }

  /** 修改登录密码，后端会作废旧令牌并下发新令牌 */
  async function changePassword(oldPassword, newPassword) {
    const res = await request.post('/auth/password', { oldPassword, newPassword })
    if (res?.token) {
      token.value = res.token
      localStorage.setItem(TOKEN_KEY, res.token)
      const saved = sessions.value.find((s) => s.empNo === currentEmpNo.value)
      if (saved) {
        saved.token = res.token
        persistSessions()
      }
    }
    return res
  }

  /** 退出当前账号：吊销当前令牌、移除该账号会话，其他账号会话保留 */
  async function logout() {
    const currentToken = token.value
    const empNo = currentEmpNo.value
    applySession('', null)
    if (empNo) removeSession(empNo)
    if (currentToken) {
      try {
        await axios.post(`${API_BASE}/auth/logout`, null, {
          headers: { Authorization: `Bearer ${currentToken}` },
          timeout: 5000
        })
      } catch {
        // 令牌可能已失效，忽略
      }
    }
  }

  /** 退出全部账号：吊销所有会话令牌并清空本地记录 */
  async function logoutAll() {
    const all = [...sessions.value]
    applySession('', null)
    sessions.value = []
    persistSessions()
    await Promise.all(
      all.map((s) =>
        axios
          .post(`${API_BASE}/auth/logout`, null, {
            headers: { Authorization: `Bearer ${s.token}` },
            timeout: 5000
          })
          .catch(() => {})
      )
    )
  }

  return {
    token,
    userInfo,
    sessions,
    isLogin,
    permissionCodes,
    currentEmpNo,
    login,
    refreshMe,
    switchAccount,
    changePassword,
    logout,
    logoutAll,
    removeSession
  }
})
