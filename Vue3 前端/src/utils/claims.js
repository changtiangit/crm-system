/**
 * 演示环境的「公海领取」记录：领取后写浏览器本地，
 * 这样刷新页面、重新登录都还能在客户档案里看到，不会一刷新就没了。
 * 接后端时把 readClaims / addClaim 换成接口即可。
 */
import { scopeAllows } from '@/utils/scope'

const KEY = 'crm.claimedCustomers'

export function readClaims() {
  try {
    const rows = JSON.parse(localStorage.getItem(KEY) || '[]')
    return Array.isArray(rows) ? rows : []
  } catch {
    return []
  }
}

/**
 * 只返回当前账号可见的领取记录：同一浏览器切换账号时，
 * 别人领走的客户不能跟着露出来（领取记录里存了 owner）。
 */
export function readVisibleClaims() {
  return readClaims().filter((r) => scopeAllows(r.owner))
}

export function claimedNames() {
  return new Set(readClaims().map((r) => r.name))
}

export function addClaim(row) {
  const list = readClaims().filter((r) => r.name !== row.name)
  list.unshift(row)
  try {
    localStorage.setItem(KEY, JSON.stringify(list))
  } catch {
    // 隐私模式下不可写，忽略
  }
  return list
}
