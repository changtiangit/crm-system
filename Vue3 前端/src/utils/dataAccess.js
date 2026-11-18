/**
 * 数据范围相关的取数辅助。
 * 客户、线索这类「自有数据」按负责人过滤；发货、收款、工单这类单据的负责人不是销售，
 * 就按「客户是否在自己可见的客户列表里」过滤。
 */
import { customers } from '@/mock/data'
import { readVisibleClaims } from '@/utils/claims'
import { isAllScope, scopeAllows } from '@/utils/scope'

/** 当前账号可见的客户名集合（含从公海领取的客户） */
export function visibleCustomerNames() {
  const names = customers.filter((c) => scopeAllows(c.owner)).map((c) => c.name)
  const claimed = readVisibleClaims().map((c) => c.name)
  return new Set([...names, ...claimed])
}

/** 全公司范围时不必过滤，返回 null 让调用方跳过 */
export function customerFilter() {
  return isAllScope.value ? null : visibleCustomerNames()
}
