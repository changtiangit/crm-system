/**
 * 数据范围（行级权限）在前端的统一入口。
 *
 * 后端在登录 / 刷新用户信息时会下发 dataScope：
 *   { type: 'ALL' | 'GROUP' | 'SELF', label: '仅本人负责数据', owners: ['李思', ...] }
 * 页面只负责「显示哪些行」，真正的拦截仍在后端（列表过滤 + 写操作归属校验）。
 *
 * 这里用 ref 保存当前范围，模板/computed 中读取即可随账号切换自动刷新。
 */
import { computed, ref } from 'vue'

const EMPTY = { type: 'ALL', label: '全公司数据', owners: null }

const scope = ref(EMPTY)

export function setDataScope(next) {
  scope.value = next && next.type ? next : EMPTY
}

export const dataScope = computed(() => scope.value)
export const isAllScope = computed(() => scope.value.type === 'ALL')
export const scopeLabel = computed(() => scope.value.label || '全公司数据')
export const scopeOwners = computed(() => scope.value.owners || [])

/** 某个负责人是否在可见范围内 */
export function scopeAllows(owner) {
  if (isAllScope.value) return true
  if (owner === null || owner === undefined || String(owner).trim() === '') return false
  return scopeOwners.value.includes(String(owner).trim())
}

/** 按负责人过滤列表；全公司范围原样返回，避免无谓复制 */
export function scopeRows(rows, key = 'owner') {
  if (!Array.isArray(rows)) return []
  if (isAllScope.value) return rows
  return rows.filter((row) => scopeAllows(row?.[key]))
}

/** 按客户名过滤（发货、收款、工单这类单据的负责人不是销售本人） */
export function scopeByCustomer(rows, visibleNames) {
  if (!Array.isArray(rows)) return []
  if (isAllScope.value) return rows
  const set = visibleNames instanceof Set ? visibleNames : new Set(visibleNames || [])
  return rows.filter((row) => set.has(row?.customer))
}
