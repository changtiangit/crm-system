/**
 * 经营分析的统一口径。
 * 所有数字都从「销售订单 + 订单明细」现算，不做第二份统计表，
 * 这样概览、数据统计、客户 360 三处看到的金额始终一致。
 */
import {
  orders,
  customers,
  receivables,
  opportunities,
  quotes,
  tickets,
  revisits,
  contacts,
  followups,
  monthSales
} from '@/mock/data'
import { scopeRows } from '@/utils/scope'

/**
 * 统计口径同样受数据范围约束：
 * 全公司账号看到的就是全量；销售只看自己的单，组长看本组。
 * 用函数而不是模块级常量，账号切换后 computed 会重新计算。
 */
export function scopedValidOrders() {
  return scopeRows(orders.filter((o) => o.status !== '已取消'))
}

/** 兼容旧调用：需要数组时用 scopedValidOrders()，需要长度时用 validOrders().length */
export const validOrders = () => scopedValidOrders()

export const sum = (list, key) => list.reduce((s, x) => s + (Number(x[key]) || 0), 0)

/** 金额展示：¥12,345 */
export function money(v) {
  if (v === undefined || v === null) return '—'
  return '¥' + Math.round(Number(v)).toLocaleString('zh-CN')
}

/** 图表轴上的金额：12.3 万 */
export function wan(v) {
  return (Number(v) / 10000).toFixed(1).replace(/\.0$/, '') + '万'
}

export function pct(v, total) {
  if (!total) return '0%'
  return Math.round((v / total) * 100) + '%'
}

/** 本月（演示数据到 11 月） */
export const CURRENT_MONTH = monthSales[monthSales.length - 1]

// ---------------- 销售统计 ----------------
export function salesKpi() {
  const list = scopedValidOrders()
  const amount = sum(list, 'amount')
  const count = list.length
  const items = list.flatMap((o) => o.items)
  const profit = sum(items, 'profit')
  const qty = sum(items, 'qty')
  const myReceivables = scopeRows(receivables, 'owner')
  const unreceived = myReceivables.reduce((s, r) => s + Math.max(0, r.total - r.received), 0)
  const overdue = myReceivables
    .filter((r) => r.total - r.received > 0 && r.dueDate < new Date().toISOString().slice(0, 10))
    .reduce((s, r) => s + (r.total - r.received), 0)
  return {
    amount,
    count,
    avgOrder: count ? amount / count : 0,
    profit,
    profitRate: amount ? profit / amount : 0,
    qty,
    unreceived,
    overdue,
    target: CURRENT_MONTH?.target || 0,
    monthAmount: CURRENT_MONTH?.amount || 0
  }
}

/** 销售员业绩：签单额、单数、毛利、覆盖客户、负责的未回款 */
export function byOwner() {
  const map = new Map()
  scopedValidOrders().forEach((o) => {
    const item = map.get(o.owner) || { owner: o.owner, amount: 0, count: 0, profit: 0, customers: new Set() }
    item.amount += o.amount
    item.count += 1
    item.profit += sum(o.items, 'profit')
    item.customers.add(o.customer)
    map.set(o.owner, item)
  })
  const unreceived = new Map()
  scopeRows(receivables, 'owner').forEach((r) => {
    unreceived.set(r.owner, (unreceived.get(r.owner) || 0) + Math.max(0, r.total - r.received))
  })
  const total = sum([...map.values()], 'amount')
  return [...map.values()]
    .map((r) => ({
      ...r,
      customerCount: r.customers.size,
      unreceived: unreceived.get(r.owner) || 0,
      share: total ? r.amount / total : 0,
      avgOrder: r.count ? r.amount / r.count : 0
    }))
    .sort((a, b) => b.amount - a.amount)
}

/** 订单状态分布 */
export function byStatus() {
  const map = new Map()
  scopeRows(orders).forEach((o) => {
    const item = map.get(o.status) || { status: o.status, count: 0, amount: 0 }
    item.count += 1
    item.amount += o.amount
    map.set(o.status, item)
  })
  return [...map.values()].sort((a, b) => b.count - a.count)
}

// ---------------- 客户采购分析 ----------------
export function byCustomer() {
  const map = new Map()
  scopedValidOrders().forEach((o) => {
    const item = map.get(o.customer) || {
      customer: o.customer,
      amount: 0,
      count: 0,
      qty: 0,
      profit: 0,
      first: o.orderDate,
      last: o.orderDate,
      categories: new Map()
    }
    item.amount += o.amount
    item.count += 1
    item.qty += sum(o.items, 'qty')
    item.profit += sum(o.items, 'profit')
    item.first = o.orderDate < item.first ? o.orderDate : item.first
    item.last = o.orderDate > item.last ? o.orderDate : item.last
    o.items.forEach((l) => item.categories.set(l.category, (item.categories.get(l.category) || 0) + l.amount))
    map.set(o.customer, item)
  })

  const total = sum([...map.values()], 'amount')
  // 客户档案里的级别、行业、负责人、id
  return [...map.values()]
    .map((r) => {
      const base = customers.find((c) => c.name === r.customer) || {}
      const topCat = [...r.categories.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] || '—'
      return {
        ...r,
        id: base.id,
        level: base.level || 'C',
        industry: base.industry || '—',
        owner: base.owner || '—',
        topCategory: topCat,
        avgOrder: r.amount / r.count,
        share: total ? r.amount / total : 0,
        unreceived: scopeRows(receivables, 'owner')
          .filter((x) => x.customer === r.customer)
          .reduce((s, x) => s + Math.max(0, x.total - x.received), 0)
      }
    })
    .sort((a, b) => b.amount - a.amount)
}

/** 客户级别 × 采购额 */
export function byLevel() {
  const list = byCustomer()
  const total = sum(list, 'amount')
  return ['A', 'B', 'C'].map((level) => {
    const rows = list.filter((r) => r.level === level)
    const amount = sum(rows, 'amount')
    return { level, amount, count: rows.length, share: total ? amount / total : 0 }
  })
}

/** 新客 / 老客：合作不满 3 个月的算新客（按首单日期判断） */
export function newOldSplit() {
  const list = byCustomer()
  const cut = new Date(Date.now() - 90 * 86400000).toISOString().slice(0, 10)
  const fresh = list.filter((r) => r.first >= cut)
  const old = list.filter((r) => r.first < cut)
  return {
    cut,
    fresh: { count: fresh.length, amount: sum(fresh, 'amount') },
    old: { count: old.length, amount: sum(old, 'amount') }
  }
}

/** 按「距离上次下单的天数」排序，排在前面的最该回访 */
export function quietCustomers() {
  const today = new Date()
  return byCustomer()
    .map((r) => ({
      ...r,
      idleDays: Math.round((today - new Date(r.last + 'T00:00:00')) / 86400000)
    }))
    .sort((a, b) => b.idleDays - a.idleDays)
}

/** 沉睡客户：最近一次下单距「今天」超过 60 天 */
export function sleepingCustomers() {
  return quietCustomers().filter((r) => r.idleDays > 60)
}

// ---------------- 产品销售分析 ----------------
export function byProduct() {
  const map = new Map()
  scopedValidOrders().forEach((o) => {
    o.items.forEach((l) => {
      const item = map.get(l.product) || {
        product: l.product,
        category: l.category,
        qty: 0,
        amount: 0,
        profit: 0,
        orderCount: 0,
        customers: new Set()
      }
      item.qty += l.qty
      item.amount += l.amount
      item.profit += l.profit
      item.orderCount += 1
      item.customers.add(o.customer)
      map.set(l.product, item)
    })
  })
  const total = sum([...map.values()], 'amount')
  return [...map.values()]
    .map((r) => ({
      ...r,
      customerCount: r.customers.size,
      rate: r.amount ? r.profit / r.amount : 0,
      share: total ? r.amount / total : 0,
      ticketCount: tickets.filter((t) => t.product === r.product).length
    }))
    .sort((a, b) => b.amount - a.amount)
}

export function byCategory() {
  const map = new Map()
  scopedValidOrders().forEach((o) => {
    o.items.forEach((l) => {
      const item = map.get(l.category) || { category: l.category, qty: 0, amount: 0, profit: 0 }
      item.qty += l.qty
      item.amount += l.amount
      item.profit += l.profit
      map.set(l.category, item)
    })
  })
  const total = sum([...map.values()], 'amount')
  return [...map.values()]
    .map((r) => ({ ...r, share: total ? r.amount / total : 0 }))
    .sort((a, b) => b.amount - a.amount)
}

// ---------------- 客户 360 ----------------
/** 按客户 id 或名称把散在各模块的数据聚到一起 */
export function customerProfile(key) {
  const s = String(key)
  const base =
    customers.find((c) => String(c.id) === s) ||
    customers.find((c) => c.name === s) ||
    null
  if (!base) return null

  const myOrders = scopeRows(orders).filter((o) => o.customer === base.name).sort((a, b) => (a.orderDate < b.orderDate ? 1 : -1))
  const myReceivables = receivables.filter((r) => r.customer === base.name)
  const myQuotes = quotes.filter((q) => q.customer === base.name)
  const myOpportunities = opportunities.filter((o) => o.customer === base.name)
  const myTickets = tickets.filter((t) => t.customer === base.name)
  const myRevisits = revisits.filter((r) => r.customer === base.name)
  const myContacts = contacts.filter((c) => c.customer === base.name)
  // 跟进记录也按范围取，避免在客户 360 里看到别组的沟通内容
  const myFollowups = scopeRows(followups)
    .filter((f) => f.customer === base.name)
    .sort((a, b) => String(b.time).localeCompare(String(a.time)))

  const valid = myOrders.filter((o) => o.status !== '已取消')
  const amount = sum(valid, 'amount')
  const items = valid.flatMap((o) => o.items)
  const unreceived = myReceivables.reduce((s, r) => s + Math.max(0, r.total - r.received), 0)

  return {
    base,
    orders: myOrders,
    receivables: myReceivables,
    quotes: myQuotes,
    opportunities: myOpportunities,
    tickets: myTickets,
    revisits: myRevisits,
    contacts: myContacts,
    followups: myFollowups,
    kpi: {
      amount,
      orderCount: valid.length,
      avgOrder: valid.length ? amount / valid.length : 0,
      profit: sum(items, 'profit'),
      unreceived,
      openOpportunity: sum(myOpportunities.filter((o) => !['赢单', '输单'].includes(o.stage)), 'amount'),
      openTickets: myTickets.filter((t) => !['已解决', '已关闭'].includes(t.status)).length,
      lastOrder: valid[0]?.orderDate || '—',
      qty: sum(items, 'qty')
    }
  }
}
