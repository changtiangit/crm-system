import request from '@/utils/request'
import * as seed from '@/mock/data'

const sources = [
  ['clues', '/business/clues', 'clue:view'],
  ['customers', '/business/customers', 'customer:view'],
  ['opportunities', '/business/opportunities', 'opportunity:view'],
  ['orders', '/business/orders', 'order:view'],
  ['contacts', '/modules/contacts', 'contact:view'],
  ['followups', '/modules/followups', 'followup:view'],
  ['poolCustomers', '/modules/pool-customers', 'pool:view'],
  // 待办不放这里：它按「人 + 岗位」初始化，由 stores/todo.js 统一负责，
  // 否则会给所有账号灌同一套通用待办（营销看到合同回款就是这么来的）
  ['quotes', '/modules/quotes', 'quote:view'],
  ['returnOrders', '/modules/returns', 'return:view'],
  ['shipments', '/modules/shipments', 'delivery:view'],
  ['receipts', '/modules/receipts', 'receipt:view'],
  ['receivables', '/modules/receivables', 'receivable:view'],
  ['tickets', '/modules/tickets', 'ticket:view'],
  ['revisits', '/modules/revisits', 'revisit:view']
]

function replace(target, rows) {
  target.splice(0, target.length, ...rows)
}

/** Read-only dashboard/report pages use the same DB-backed data as the list screens. */
export async function hydrateCrmData(permissions, routeName) {
  const allowed = new Set(permissions || [])
  const fullSnapshot = ['RoleDesk', 'Overview', 'StatsSales', 'StatsCustomer', 'StatsProduct', 'CustomerDetail'].includes(routeName)
  const selected = fullSnapshot ? sources : sources.filter(([key]) => ['customers', 'orders', 'contacts'].includes(key))
  selected.forEach(([key, , permission]) => {
    if (!allowed.has(permission)) replace(seed[key], [])
  })
  const jobs = selected
    .filter(([, , permission]) => allowed.has(permission))
    .map(async ([key, url, permission]) => {
      let rows = await request.get(url)
      if (url.startsWith('/modules/') && rows.length === 0) {
        const module = url.slice('/modules/'.length)
        const seedKey = key
        await request.post(`${url}/seed`, seed[seedKey])
        rows = await request.get(url)
      }
      if (key === 'orders') {
        rows.forEach((order) => {
          order.items = (order.items || []).map((item) => ({
            ...item,
            profit: item.cost == null ? 0 : (Number(item.price) - Number(item.cost)) * Number(item.qty)
          }))
        })
      }
      replace(seed[key], rows || [])
    })

  await Promise.all(jobs)

  if (!fullSnapshot) return

  const monthly = seed.monthSales.map((row) => {
    const month = Number(row.month.replace('月', ''))
    const amount = seed.orders
      .filter((order) => order.status !== '已取消' && Number(String(order.orderDate).slice(5, 7)) === month)
      .reduce((total, order) => total + Number(order.amount || 0), 0)
    return { ...row, amount }
  })
  replace(seed.monthSales, monthly)

  const sourceCounts = new Map()
  seed.clues.forEach((clue) => sourceCounts.set(clue.source, (sourceCounts.get(clue.source) || 0) + 1))
  replace(seed.sourceStats, [...sourceCounts].map(([name, count]) => ({ name, count })))
}
