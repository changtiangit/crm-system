/**
 * 页面里判断数据范围的小工具：
 *   const { label, isAll, owners, canSee, filterRows } = useDataScope()
 * owners 为当前账号能看到的所有负责人姓名（全公司时为全体销售）。
 */
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { dataScope, isAllScope, scopeAllows, scopeLabel, scopeOwners, scopeRows } from '@/utils/scope'

export function useDataScope() {
  const userStore = useUserStore()

  /** 兜底：切换账号后 userInfo 还没刷新时，沿用 scope.js 里的值 */
  const scope = computed(() => userStore.userInfo?.dataScope || dataScope.value)
  const label = computed(() => scope.value?.label || scopeLabel.value)
  const isAll = computed(() => scope.value?.type === 'ALL' || isAllScope.value)
  const isSelf = computed(() => scope.value?.type === 'SELF')
  const owners = computed(() => scope.value?.owners || scopeOwners.value)
  const type = computed(() => scope.value?.type || 'ALL')

  function canSee(owner) {
    return scopeAllows(owner)
  }

  function filterRows(rows, key = 'owner') {
    return scopeRows(rows, key)
  }

  return { scope, label, isAll, isSelf, type, owners, canSee, filterRows }
}
