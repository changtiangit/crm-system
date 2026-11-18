<template>
  <div class="finance-desk">
    <!-- 财务只关心三件事：该收多少、逾期多少、这个月收进来多少 -->
    <div class="dk-kpi-grid">
      <div
        v-for="k in kpis"
        :key="k.label"
        class="dk-kpi page-card"
        :class="{ click: !!k.to }"
        :title="k.to ? `查看${k.drill}` : ''"
        @click="k.to && router.push(k.to)"
      >
        <div class="dk-kpi-top">
          <span class="dk-kpi-label">{{ k.label }}</span>
          <span class="dk-kpi-icon" :style="{ background: k.bg, color: k.color }">
            <el-icon :size="15"><component :is="k.icon" /></el-icon>
          </span>
        </div>
        <div class="dk-kpi-value" :class="{ 'dk-danger': k.alarm }">{{ k.value }}</div>
        <div class="dk-kpi-foot">
          <span class="dk-delta" :class="k.trend">{{ k.delta }}</span>
          <span>{{ k.sub }}</span>
        </div>
      </div>
    </div>

    <div class="dk-two-col">
      <!-- 逾期与临期：财务每天上班先看这张表 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>应收未收 · 按回款日排序</h3>
            <p>逾期排最前，越红越急；点一行进应收账款做核销或登记承诺回款</p>
          </div>
          <button class="dk-link" @click="router.push('/delivery/receivable')">
            应收账款<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="openList.length" class="dk-table">
          <thead>
            <tr>
              <th>客户</th>
              <th>订单号</th>
              <th class="num">未收</th>
              <th class="w-date">约定回款日</th>
              <th class="w-state">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="r in openList"
              :key="r.id"
              class="click"
              :title="`去应收账款处理「${r.customer}」这一单`"
              @click="router.push(`/delivery/receivable?customer=${encodeURIComponent(r.customer)}`)"
            >
              <td class="strong">{{ r.customer }}</td>
              <td class="mono muted">{{ r.orderNo }}</td>
              <td class="num strong">{{ money(r.total - r.received) }}</td>
              <td class="muted">{{ r.dueDate }}</td>
              <td>
                <span v-if="daysLate(r) > 0" class="dk-tag danger">逾期 {{ daysLate(r) }} 天</span>
                <span v-else-if="daysLate(r) === 0" class="dk-tag warn">今天到期</span>
                <span v-else class="dk-tag ok">{{ -daysLate(r) }} 天后到期</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">没有未收款的订单，账期都在正常范围内</div>
      </section>

      <!-- 账龄结构：决定催收优先级 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>账龄结构</h3>
            <p>逾期越久越难收，90 天以上要单独出方案</p>
          </div>
          <span class="dk-badge">共 {{ openList.length }} 单</span>
        </header>

        <ul class="dk-list">
          <li
            v-for="b in aging"
            :key="b.label"
            class="dk-row click"
            :title="`查看应收账款明细`"
            @click="router.push('/delivery/receivable')"
          >
            <span class="dk-tag" :class="b.tone">{{ b.label }}</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ money(b.amount) }}</span>
                <span class="dk-row-side">{{ b.count }} 单</span>
              </div>
              <div class="dk-bar"><span :style="{ width: b.share * 100 + '%' }"></span></div>
            </div>
            <span class="dk-row-side">{{ pct(b.share) }}</span>
          </li>
        </ul>
      </section>
    </div>

    <div class="dk-two-col even">
      <!-- 最近收款：核对到账与订单是否对得上 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>最近收款</h3>
            <p>到账后及时核销，避免「钱到了账没平」</p>
          </div>
          <button class="dk-link" @click="router.push('/delivery/receipt')">
            收款记录<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="recentReceipts.length" class="dk-table">
          <thead>
            <tr>
              <th>收款单</th>
              <th>客户</th>
              <th class="num">金额</th>
              <th class="w-date">到账日</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="r in recentReceipts"
              :key="r.id"
              class="click"
              @click="router.push(`/delivery/receipt?customer=${encodeURIComponent(r.customer)}`)"
            >
              <td class="mono muted">{{ r.receiptNo }}</td>
              <td>{{ r.customer }}</td>
              <td class="num strong">{{ money(r.amount) }}</td>
              <td class="muted">{{ r.receiptDate }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">本月还没有到账记录</div>
      </section>

      <!-- 待处理事项：财务的日常三大块 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>本月工作提示</h3>
            <p>从订单和退换货里自动带出来的财务动作</p>
          </div>
        </header>

        <ul class="dk-list">
          <li
            v-for="t in hints"
            :key="t.title"
            class="dk-row click"
            :title="`进入${t.label}`"
            @click="router.push(t.to)"
          >
            <span class="hint-icon" :class="t.tone">{{ t.count }}</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ t.title }}</span>
              </div>
              <p class="dk-row-sub">{{ t.desc }}</p>
            </div>
            <el-icon class="hint-go"><ArrowRight /></el-icon>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Money, Tickets, TrendCharts, Wallet } from '@element-plus/icons-vue'
import { receivables, receipts, orders, returnOrders } from '@/mock/data'
import { money, wan } from '@/utils/analytics'

defineProps({
  profile: { type: Object, required: true }
})

const router = useRouter()

const today = new Date().toISOString().slice(0, 10)
const todayTime = new Date(today + 'T00:00:00').getTime()
const daysLate = (r) => Math.round((todayTime - new Date(r.dueDate + 'T00:00:00').getTime()) / 86400000)

/** 未收清的单，按回款日升序（逾期的自然排前面） */
const openList = computed(() =>
  receivables
    .filter((r) => r.total - r.received > 0)
    .slice()
    .sort((a, b) => String(a.dueDate).localeCompare(String(b.dueDate)))
)

const unreceived = computed(() => openList.value.reduce((s, r) => s + (r.total - r.received), 0))
const overdueList = computed(() => openList.value.filter((r) => daysLate(r) > 0))
const overdueAmount = computed(() => overdueList.value.reduce((s, r) => s + (r.total - r.received), 0))

/** 本月回款：数据里最新一个月 */
const monthKey = computed(() => {
  const keys = receipts.map((r) => (r.receiptDate || '').slice(0, 7)).filter(Boolean).sort()
  return keys.length ? keys[keys.length - 1] : ''
})
const monthReceipts = computed(() => receipts.filter((r) => (r.receiptDate || '').startsWith(monthKey.value)))
const monthAmount = computed(() => monthReceipts.value.reduce((s, r) => s + Number(r.amount || 0), 0))

/** 平均账龄：未收清的单按未到期/逾期天数算，只统计逾期的平均拖延天数 */
const avgOverdueDays = computed(() => {
  if (!overdueList.value.length) return 0
  return Math.round(overdueList.value.reduce((s, r) => s + daysLate(r), 0) / overdueList.value.length)
})

const kpis = computed(() => [
  {
    label: '应收未收',
    value: money(unreceived.value),
    delta: `${openList.value.length} 单待收`,
    trend: 'info',
    sub: `其中逾期 ${overdueList.value.length} 单`,
    icon: Wallet,
    bg: '#f3edff',
    color: '#7c3aed',
    to: '/delivery/receivable',
    drill: '应收账款'
  },
  {
    label: '逾期金额',
    value: money(overdueAmount.value),
    delta: overdueList.value.length ? `逾期 ${overdueList.value.length} 单` : '全部在账期内',
    trend: overdueList.value.length ? 'down' : 'up',
    sub: overdueList.value.length ? `平均拖 ${avgOverdueDays.value} 天` : '无需催收',
    icon: Tickets,
    bg: '#fdeceb',
    color: '#c0392b',
    to: '/delivery/receivable',
    drill: '应收账款',
    alarm: overdueList.value.length > 0
  },
  {
    label: '本月已回款',
    value: money(monthAmount.value),
    delta: `${monthReceipts.value.length} 笔到账`,
    trend: 'up',
    sub: `占应收 ${pct(unreceived.value ? monthAmount.value / (monthAmount.value + unreceived.value) : 0)}`,
    icon: Money,
    bg: '#e8f8f2',
    color: '#0f9d76',
    to: '/delivery/receipt',
    drill: '收款记录'
  },
  {
    label: '应收总额',
    value: money(receivables.reduce((s, r) => s + Number(r.total || 0), 0)),
    delta: `已收 ${wan(receivables.reduce((s, r) => s + Number(r.received || 0), 0))}`,
    trend: 'up',
    sub: '全部订单口径',
    icon: TrendCharts,
    bg: '#eef3ff',
    color: '#3b6cff',
    to: '/sales/order',
    drill: '销售订单'
  }
])

/** 账龄分档：未到期 / 逾期 1-30 / 31-60 / 61-90 / 90+ */
const aging = computed(() => {
  const buckets = [
    { label: '未到期', tone: 'ok', test: (d) => d <= 0, amount: 0, count: 0 },
    { label: '逾期 1-30 天', tone: 'warn', test: (d) => d > 0 && d <= 30, amount: 0, count: 0 },
    { label: '逾期 31-60 天', tone: 'danger', test: (d) => d > 30 && d <= 60, amount: 0, count: 0 },
    { label: '逾期 60 天以上', tone: 'danger', test: (d) => d > 60, amount: 0, count: 0 }
  ]
  for (const r of openList.value) {
    const d = daysLate(r)
    const b = buckets.find((x) => x.test(d))
    if (!b) continue
    b.amount += r.total - r.received
    b.count += 1
  }
  const total = buckets.reduce((s, b) => s + b.amount, 0) || 1
  return buckets.map((b) => ({ ...b, share: b.amount / total }))
})

const recentReceipts = computed(() =>
  receipts
    .slice()
    .sort((a, b) => String(b.receiptDate).localeCompare(String(a.receiptDate)))
    .slice(0, 5)
)

const hints = computed(() => {
  const pendingReturns = returnOrders.filter((r) => r.status === '待审核').length
  const invoiceOrders = orders.filter((o) => o.status === '已完成' && (o.orderDate || '').startsWith(monthKey.value)).length
  return [
    {
      title: '催收回款',
      desc: `逾期 ${overdueList.value.length} 单，合计 ${money(overdueAmount.value)}`,
      count: overdueList.value.length,
      tone: 'danger',
      label: '应收账款',
      to: '/delivery/receivable'
    },
    {
      title: '到账核销',
      desc: `本月 ${monthReceipts.value.length} 笔已到账，核对订单与收款单`,
      count: monthReceipts.value.length,
      tone: 'ok',
      label: '收款记录',
      to: '/delivery/receipt'
    },
    {
      title: '退换货款项',
      desc: `待审核 ${pendingReturns} 单，涉及退款需同步账务`,
      count: pendingReturns,
      tone: 'warn',
      label: '退换货单',
      to: '/service/return'
    },
    {
      title: '已完成订单开票',
      desc: `本月 ${invoiceOrders} 单已完成，确认开票状态`,
      count: invoiceOrders,
      tone: 'info',
      label: '销售订单',
      to: '/sales/order'
    }
  ]
})

const pct = (v) => Math.round((Number(v) || 0) * 100) + '%'
</script>

<style scoped>
.w-date {
  width: 106px;
}

.w-state {
  width: 108px;
}

.hint-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 650;
  flex-shrink: 0;
  background: #f2f5fb;
  color: #5c6b82;
  font-variant-numeric: tabular-nums;
}

.hint-icon.danger {
  background: #fdeceb;
  color: #c0392b;
}

.hint-icon.ok {
  background: #e8f8f2;
  color: #0f9d76;
}

.hint-icon.warn {
  background: #fdf4e3;
  color: #b45309;
}

.hint-icon.info {
  background: #eef3ff;
  color: #2f5fe0;
}

.hint-go {
  font-size: 12px;
  color: #c3cddd;
}

.dk-row:hover .hint-go {
  color: #7c3aed;
}
</style>
