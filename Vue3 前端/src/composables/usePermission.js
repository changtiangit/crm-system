import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 功能权限判断，数据全部来自登录接口返回的当前用户。
 * - 管理员（ADMIN 角色）拥有全部功能
 * - 其他账号取各角色功能的并集（后端已算好，前端直接读）
 */
export function usePermission() {
  const userStore = useUserStore()

  const isAdmin = computed(() => (userStore.userInfo?.roles || []).some((r) => r.code === 'ADMIN'))
  const roleNames = computed(() => (userStore.userInfo?.roles || []).map((r) => r.name))
  const permissionCodes = computed(() => userStore.permissionCodes)

  function has(code) {
    if (!code) return true
    if (isAdmin.value) return true
    return permissionCodes.value.includes(code)
  }

  function hasAny(codes = []) {
    if (!codes.length) return true
    return codes.some((c) => has(c))
  }

  return { isAdmin, roleNames, permissionCodes, has, hasAny }
}

/** 供路由守卫等非组件场景使用 */
export function checkPermission(code) {
  if (!code) return true
  const userStore = useUserStore()
  const user = userStore.userInfo
  if (!user) return false
  if ((user.roles || []).some((r) => r.code === 'ADMIN')) return true
  return (user.permissions || []).includes(code)
}
