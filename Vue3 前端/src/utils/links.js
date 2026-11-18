/**
 * 跨模块跳转的地址拼装。
 * 约定：详情类页面用路径参数，列表类页面用查询参数（?customer= / ?owner= / ?q=），
 * 列表页由 DataPage 统一接管，跳过去就自动筛好。
 */
import { useRouter } from 'vue-router'
import { customers } from '@/mock/data'
import { readClaims } from '@/utils/claims'

/** 客户档案（含从公海领取的客户）里按 id 或名称找客户 */
export function findCustomer(key) {
  if (key === undefined || key === null || key === '') return null
  const s = String(key)
  const all = [...readClaims(), ...customers]
  return all.find((c) => String(c.id) === s) || all.find((c) => c.name === s) || null
}

/** 客户 360 地址：档案里有的按 id 进，临时客户（如刚领取）按名称进 */
export function customerPath(key) {
  const hit = findCustomer(key)
  if (hit) return `/customer/detail/${hit.id}`
  return `/customer/detail/0?name=${encodeURIComponent(String(key))}`
}

/** 列表页带客户筛选 */
export function withCustomer(path, name) {
  return `${path}?customer=${encodeURIComponent(name)}`
}

/** 列表页带负责人筛选 */
export function withOwner(path, owner) {
  return `${path}?owner=${encodeURIComponent(owner)}`
}

/** 列表页带搜索词（商品名、工单问题等） */
export function withKeyword(path, kw) {
  return `${path}?q=${encodeURIComponent(kw)}`
}

/**
 * 各业务表格里点客户名统一走这里：
 * 传一行数据或客户名都行，跳到该客户的 360 全景。
 */
export function useCustomerNav() {
  const router = useRouter()
  function openCustomer(rowOrName) {
    const name =
      rowOrName && typeof rowOrName === 'object'
        ? rowOrName.customer || rowOrName.name
        : rowOrName
    if (!name) return
    router.push(customerPath(name))
  }
  return { openCustomer }
}

/** 订单详情（合同清单）地址：各处点单号统一走这里 */
export function orderPath(orderNo) {
  if (!orderNo) return '/sales/order'
  return `/sales/order/detail/${encodeURIComponent(String(orderNo))}`
}

/** 单据互跳：点订单号进这一单的合同清单，而不是在列表里再搜一遍 */
export function useOrderNav() {
  const router = useRouter()
  function openOrder(rowOrNo) {
    const no =
      rowOrNo && typeof rowOrNo === 'object' ? rowOrNo.orderNo : rowOrNo
    if (!no) return
    router.push(orderPath(no))
  }
  return { openOrder }
}
