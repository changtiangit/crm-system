<template>
  <div class="page overview">
    <!-- 问候 + 概览口径 -->
    <div class="hero">
      <div class="hero-main">
        <h2 class="hero-title">{{ greeting }}，{{ userName }}</h2>
        <p class="hero-desc">
          {{ todayText }} · {{ deptName }}<template v-if="roleText"> · {{ roleText }}</template>
        </p>
      </div>
      <div class="hero-side">
        <button
          v-if="has('todo:view')"
          class="hero-chip link"
          title="查看今天到期的待办"
          @click="open('/workbench/todo?tab=today', 'todo:view')"
        >
          <span class="dot warn"></span>今日待办 <b>{{ todoToday }}</b> 项
          <el-icon class="chip-go"><ArrowRight /></el-icon>
        </button>
        <button
          v-if="has('todo:view')"
          class="hero-chip link"
          title="查看已逾期的待办"
          @click="open('/workbench/todo?tab=overdue', 'todo:view')"
        >
          <span class="dot danger"></span>逾期 <b>{{ todoOverdue }}</b> 项
          <el-icon class="chip-go"><ArrowRight /></el-icon>
        </button>
        <button
          v-if="has('receivable:view')"
          class="hero-chip link"
          :title="`逾期 ${overdueCount} 笔，合计 ${money(overdueTotal)}，点击去应收账款核销`"
          @click="open('/delivery/receivable', 'receivable:view')"
        >
          <span class="dot ok"></span>应收未收 <b>{{ wan(unreceivedTotal) }}</b>
          <el-icon class="chip-go"><ArrowRight /></el-icon>
        </button>
        <el-button
          v-if="has('stats:sales:view')"
          class="hero-stat"
          type="primary"
          plain
          :icon="DataAnalysis"
          @click="open('/stats/sales', 'stats:sales:view')"
        >
          数据统计
        </el-button>
      </div>
    </div>

    <!-- KPI：按岗位权限出卡，财务看到的是应收，销售看到的是商机 -->
    <div class="kpi-grid">
      <div
        v-for="k in visibleKpis"
        :key="k.label"
        class="kpi-card page-card"
        :class="{ clickable: !k.perm || has(k.perm) }"
        :title="k.to ? `查看${k.drill}` : ''"
        @click="drill(k)"
      >
        <div class="kpi-top">
          <span class="kpi-label">{{ k.label }}</span>
          <span class="kpi-icon" :style="{ background: k.bg, color: k.color }">
            <el-icon :size="16"><component :is="k.icon" /></el-icon>
          </span>
        </div>
        <div class="kpi-value">{{ k.value }}</div>
        <div class="kpi-foot">
          <span class="kpi-delta" :class="k.trend">{{ k.delta }}</span>
          <span class="kpi-sub">{{ k.sub }}</span>
        </div>
      </div>
    </div>

    <!-- 趋势 + 来源：没有线索权限的岗位（财务）只留趋势，单列铺满 -->
    <div
      v-if="has('order:view') || has('clue:view')"
      class="two-col"
      :class="{ single: !(has('order:view') && has('clue:view')) }"
    >
      <section v-if="has('order:view')" class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>销售额趋势</h3>
            <p>今年 1-11 月签单金额（实线）与月度目标（虚线）</p>
          </div>
          <div class="legend">
            <span><i class="lg-line"></i>实际</span>
            <span><i class="lg-dash"></i>目标</span>
          </div>
          <el-button link type="primary" @click="open('/sales/order', 'order:view')">
            销售订单<el-icon><ArrowRight /></el-icon>
          </el-button>
        </header>

        <svg class="trend" viewBox="0 0 720 250" preserveAspectRatio="xMidYMid meet">
          <defs>
            <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#3b6cff" stop-opacity="0.22" />
              <stop offset="100%" stop-color="#3b6cff" stop-opacity="0.02" />
            </linearGradient>
          </defs>

          <!-- 横向网格 + 纵轴刻度 -->
          <g v-for="(g, i) in gridLines" :key="i">
            <line x1="56" :y1="g.y" x2="704" :y2="g.y" stroke="#eef1f7" stroke-width="1" />
            <text x="46" :y="g.y + 4" text-anchor="end" class="axis-text">{{ g.label }}</text>
          </g>

          <!-- 目标线 -->
          <path :d="targetPath" fill="none" stroke="#f0b429" stroke-width="1.6" stroke-dasharray="5 4" />

          <!-- 实际线 + 面积 -->
          <path :d="areaPath" fill="url(#areaFill)" />
          <path :d="linePath" fill="none" stroke="#3b6cff" stroke-width="2.4" stroke-linejoin="round" />

          <!-- 数据点 + 数值：点某个月直接看那个月的订单 -->
          <g
            v-for="(p, i) in points"
            :key="'p' + i"
            class="pt"
            :class="{ link: has('order:view') }"
            @click="open(monthOrderPath(p.month), 'order:view')"
          >
            <title>{{ p.month }}签单 {{ money(p.amount) }}，点击查看当月订单</title>
            <rect class="pt-hit" :x="p.x - 26" y="18" width="52" height="176" rx="6" />
            <circle :cx="p.x" :cy="p.y" r="4" fill="#fff" stroke="#3b6cff" stroke-width="2.2" />
            <text
              :x="p.x + (i === 0 ? 4 : i === points.length - 1 ? -4 : 0)"
              :y="p.y - 12"
              :text-anchor="i === 0 ? 'start' : i === points.length - 1 ? 'end' : 'middle'"
              class="point-text"
            >
              {{ p.label }}
            </text>
            <text :x="p.x" y="238" text-anchor="middle" class="axis-text">{{ p.month }}</text>
          </g>
        </svg>
      </section>

      <section
        v-if="has('clue:view')"
        class="page-card panel"
        :class="{ clickable: has('clue:view') }"
        @click="open('/customer/clue', 'clue:view')"
      >
        <header class="panel-head">
          <div>
            <h3>线索来源分布</h3>
            <p>本月新增线索按渠道拆解</p>
          </div>
          <span class="panel-badge">共 {{ totalSource }} 条<el-icon class="go-icon"><ArrowRight /></el-icon></span>
        </header>

        <div class="source-list">
          <div v-for="s in sourceStats" :key="s.name" class="source-row" :class="{ 'row-link': has('clue:view') }" @click.stop="openSource(s.name)">
            <div class="source-meta">
              <span class="source-name">{{ s.name }}</span>
              <span class="source-count">{{ s.count }} 条 · {{ Math.round((s.count / totalSource) * 100) }}%</span>
            </div>
            <div class="source-bar">
              <span :style="{ width: (s.count / maxSource) * 100 + '%' }"></span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 业绩排行 + 待跟进：财务两个都没有，整行不渲染 -->
    <div
      v-if="has('sales:team:view') || has('followup:view')"
      class="two-col"
      :class="{ single: !(has('sales:team:view') && has('followup:view')) }"
    >
      <section
        v-if="has('sales:team:view')"
        class="page-card panel"
        :class="{ clickable: has('stats:sales:view') || has('sales:team:view') }"
        @click="drillRank({ name: '销售业绩' })"
      >
        <header class="panel-head">
          <div>
            <h3>销售业绩排行</h3>
            <p>按签单额排序，进度条为金额完成占比</p>
          </div>
          <span class="panel-badge">近半年<el-icon class="go-icon"><ArrowRight /></el-icon></span>
        </header>

        <div class="rank-list">
          <div
            v-for="(r, i) in ranking"
            :key="r.name"
            class="rank-row"
            :class="{ 'row-link': has('stats:sales:view') || has('sales:team:view') }"
            @click.stop="drillRank(r)"
          >
            <span class="rank-no" :class="'no' + (i + 1)">{{ i + 1 }}</span>
            <span class="rank-avatar">{{ r.name.slice(0, 1) }}</span>
            <div class="rank-main">
              <div class="rank-line">
                <span class="rank-name">{{ r.name }}</span>
                <span class="rank-amount">{{ money(r.amount) }}</span>
              </div>
              <div class="rank-bar"><span :style="{ width: r.rate + '%' }"></span></div>
            </div>
            <span class="rank-sub">回款 {{ money(r.paid) }}</span>
          </div>
        </div>
      </section>

      <section
        v-if="has('followup:view')"
        class="page-card panel"
        :class="{ clickable: has('followup:view') }"
        @click="open('/customer/followup', 'followup:view')"
      >
        <header class="panel-head">
          <div>
            <h3>今日待跟进</h3>
            <p>来自跟进记录里的下一步安排</p>
          </div>
          <el-button link type="primary" @click.stop="open('/customer/followup', 'followup:view')">
            全部记录
          </el-button>
        </header>

        <ul class="follow-list">
          <li
            v-for="f in todayFollows"
            :key="f.id"
            class="follow-row"
            :class="{ 'row-link': has('customer:view') }"
            @click.stop="openCustomer(f.customer)"
          >
            <span class="follow-avatar">{{ f.contact.slice(0, 1) }}</span>
            <div class="follow-main">
              <div class="follow-line">
                <span class="follow-name">{{ f.customer }}</span>
                <el-tag size="small" effect="light" round>{{ f.type }}</el-tag>
              </div>
              <p class="follow-next">{{ f.next }}</p>
            </div>
            <span class="follow-time">{{ f.time.slice(5, 10) }}</span>
          </li>
        </ul>
      </section>
    </div>

    <!-- 回款与应收：财务的主战场，其他岗位看不到 -->
    <section v-if="has('receivable:view')" class="page-card panel ar-panel">
      <header class="panel-head">
        <div>
          <h3>应收未收</h3>
          <p>按约定回款日排序，过期的排在最前；点行进入应收账款核销</p>
        </div>
        <el-button link type="primary" @click="open('/delivery/receivable', 'receivable:view')">
          应收账款<el-icon><ArrowRight /></el-icon>
        </el-button>
      </header>

      <table class="ar-table">
        <thead>
          <tr>
            <th>客户</th>
            <th>订单号</th>
            <th class="num">订单金额</th>
            <th class="num">已收</th>
            <th class="num">未收</th>
            <th class="w-date">约定回款日</th>
            <th class="w-state">状态</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="r in openReceivables"
            :key="r.id"
            class="ar-row"
            title="去应收账款核销这一单"
            @click="openReceivable(r)"
          >
            <td class="ar-cust">{{ r.customer }}</td>
            <td class="mono muted">{{ r.orderNo }}</td>
            <td class="num">{{ money(r.total) }}</td>
            <td class="num muted">{{ money(r.received) }}</td>
            <td class="num strong">{{ money(r.total - r.received) }}</td>
            <td class="muted">{{ r.dueDate }}</td>
            <td>
              <el-tag v-if="isOverdue(r)" size="small" type="danger" effect="light">逾期 {{ overdueDays(r) }} 天</el-tag>
              <el-tag v-else size="small" type="success" effect="light">未到期</el-tag>
            </td>
          </tr>
          <tr v-if="!openReceivables.length">
            <td colspan="7" class="empty">没有未收款的订单，账期都在正常范围内</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { ArrowRight } from '@element-plus/icons-vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { useOrgStore } from '@/stores/org'
import { useTodoStore } from '@/stores/todo'
import { useCustomerNav } from '@/utils/links'
import { byOwner } from '@/utils/analytics'
import { scopeRows } from '@/utils/scope'
import {
  orders,
  customers,
  contacts,
  followups,
  opportunities,
  poolCustomers,
  monthSales,
  receivables,
  sourceStats
} from '@/mock/data'

const router = useRouter()
const userStore = useUserStore()
const orgStore = useOrgStore()
const todoStore = useTodoStore()
const { roleNames, has } = usePermission()
const { openCustomer } = useCustomerNav()

// 待办与「我的待办」页共用一份数据（按账号存本地）
todoStore.load()

const userName = computed(() => userStore.userInfo?.name || '同事')
const deptName = computed(() => userStore.userInfo?.deptName || '未分配部门')
const roleText = computed(() => roleNames.value.join(' · '))
const todayText = computed(() =>
  new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
)
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const money = (v) => '¥' + Number(v).toLocaleString('zh-CN')

/** 今日 / 逾期待办数 */
const today = new Date().toISOString().slice(0, 10)
const todoToday = computed(() => todoStore.pending.filter((t) => t.due === today).length)
const todoOverdue = computed(() => todoStore.overdue.length)

/** KPI：工作台展示的是「我能看到的」数据，销售看到的是自己的业绩 */
const myCustomers = computed(() => scopeRows(customers))
const myContacts = computed(() => scopeRows(contacts))
const myOpportunities = computed(() => scopeRows(opportunities))
const myFollowups = computed(() => scopeRows(followups))
const monthSeries = computed(() => {
  const list = scopeRows(orders).filter((o) => o.status !== '已取消')
  return monthSales.map((m) => {
    const mm = Number(m.month.replace('月', ''))
    return {
      ...m,
      amount: list
        .filter((o) => Number(String(o.orderDate).slice(5, 7)) === mm)
        .reduce((s, o) => s + o.amount, 0)
    }
  })
})
const monthAmount = computed(() => monthSeries.value[monthSeries.value.length - 1])
const payRate = computed(() => Math.round((monthAmount.value.amount / monthAmount.value.target) * 100))
const oppTotal = computed(() => myOpportunities.value.filter((o) => o.stage !== '输单').reduce((s, o) => s + o.amount, 0))

/** 应收：未收金额与逾期笔数，财务口径 */
const openReceivables = computed(() =>
  receivables
    .filter((r) => r.total - r.received > 0)
    .slice()
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
)
const unreceivedTotal = computed(() => openReceivables.value.reduce((s, r) => s + (r.total - r.received), 0))
const overdueCount = computed(() => openReceivables.value.filter((r) => isOverdue(r)).length)
const overdueTotal = computed(() =>
  openReceivables.value.filter((r) => isOverdue(r)).reduce((s, r) => s + (r.total - r.received), 0)
)
function isOverdue(r) {
  return r.dueDate < today && r.total - r.received > 0
}
function overdueDays(r) {
  return Math.max(1, Math.round((new Date(today) - new Date(r.dueDate)) / 86400000))
}
const wan = (v) => (Number(v) / 10000).toFixed(1).replace(/\.0$/, '') + '万'

const kpis = computed(() => [
  {
    label: '本月签单额',
    value: money(monthAmount.value.amount),
    delta: `目标完成 ${payRate.value}%`,
    trend: payRate.value >= 80 ? 'up' : 'down',
    sub: `目标 ${money(monthAmount.value.target)}`,
    icon: 'Money',
    color: '#2f5fe0',
    bg: '#eef3ff',
    // 直接落到「本月订单」，和销售统计里点柱子的去向一致
    to: `/sales/order?month=2026-${String(Number(monthAmount.value.month.replace('月', ''))).padStart(2, '0')}`,
    perm: 'order:view',
    drill: '本月订单'
  },
  {
    label: '应收未收',
    value: money(unreceivedTotal.value),
    delta: overdueCount.value ? `逾期 ${overdueCount.value} 笔` : '全部在账期内',
    trend: overdueCount.value ? 'down' : 'up',
    sub: overdueCount.value ? `逾期金额 ${wan(overdueTotal.value)}` : `共 ${openReceivables.value.length} 单待收`,
    icon: 'Wallet',
    color: '#7c3aed',
    bg: '#f3edff',
    to: '/delivery/receivable',
    perm: 'receivable:view',
    // 财务的主 KPI；管理员另有「应收未收」面板与顶部小结，不再重复占一张卡
    when: () => has('receivable:view') && !has('opportunity:view'),
    drill: '应收账款'
  },
  {
    label: '在谈商机金额',
    value: money(oppTotal.value),
    delta: `${myOpportunities.value.filter((o) => o.stage !== '输单').length} 个商机`,
    trend: 'up',
    sub: `已赢单 ${myOpportunities.value.filter((o) => o.stage === '赢单').length} 个`,
    icon: 'TrendCharts',
    color: '#0f9d76',
    bg: '#e8f8f2',
    to: '/sales/opportunity',
    perm: 'opportunity:view',
    drill: '销售商机'
  },
  {
    label: '客户 / 联系人',
    value: `${myCustomers.value.length} / ${myContacts.value.length}`,
    delta: '本月新增 2 家',
    trend: 'up',
    sub: `公海待领取 ${poolCustomers.length} 家`,
    icon: 'User',
    color: '#b45309',
    bg: '#fdf4e3',
    to: '/customer/list',
    perm: 'customer:view',
    drill: '客户档案'
  },
  {
    label: '待办 / 逾期',
    value: `${todoStore.pending.length} / ${todoOverdue.value}`,
    delta: todoOverdue.value ? '有逾期需处理' : '暂无逾期',
    trend: todoOverdue.value ? 'down' : 'up',
    sub: `今日到期 ${todoToday.value} 项`,
    icon: 'AlarmClock',
    color: '#c0392b',
    bg: '#fdeceb',
    to: '/workbench/todo',
    perm: 'todo:view',
    drill: '我的待办'
  }
])

/** 概览是「按权限拼出来的」：没有该模块权限的卡片直接不出，避免点进 403 */
const visibleKpis = computed(() =>
  kpis.value.filter((k) => (!k.perm || has(k.perm)) && (!k.when || k.when()))
)

/** 折线图几何计算 */
const CHART = { left: 56, right: 704, top: 26, bottom: 208 }
// 纵轴上界跟着数据走：签单量跨月波动大，写死 30 万会把线顶出画布
const yMax = computed(() => {
  const peak = Math.max(...monthSeries.value.map((m) => Math.max(m.amount, m.target)), 1)
  const step = peak > 600000 ? 250000 : peak > 300000 ? 100000 : 50000
  return Math.ceil(peak / step) * step
})

const points = computed(() => {
  const span = monthSeries.value.length - 1 || 1
  return monthSeries.value.map((m, i) => {
    const x = CHART.left + ((CHART.right - CHART.left) / span) * i
    const y = CHART.bottom - (m.amount / yMax.value) * (CHART.bottom - CHART.top)
    return { x, y, month: m.month, label: Math.round(m.amount / 1000) + 'k', target: m.target, amount: m.amount }
  })
})

const linePath = computed(() =>
  points.value.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ')
)
const areaPath = computed(
  () => `${linePath.value} L${CHART.right},${CHART.bottom} L${CHART.left},${CHART.bottom} Z`
)
const targetPath = computed(() =>
  points.value
    .map((p, i) => {
      const y = CHART.bottom - (p.target / yMax.value) * (CHART.bottom - CHART.top)
      return `${i === 0 ? 'M' : 'L'}${p.x},${y}`
    })
    .join(' ')
)
// 三档刻度（0 / 10 万 / 20 万 / 30 万），避免出现 22 万这种不好读的中间值
const gridLines = computed(() =>
  [0, 1 / 3, 2 / 3, 1].map((r) => ({
    y: CHART.bottom - r * (CHART.bottom - CHART.top),
    label: Math.round((yMax.value * r) / 10000) + '万'
  }))
)

/** 线索来源 */
const totalSource = computed(() => sourceStats.reduce((s, x) => s + x.count, 0))
const maxSource = computed(() => Math.max(...sourceStats.map((s) => s.count)))

/** 销售业绩：与「数据统计 · 销售统计」同一口径（签单额 + 已回款） */
const ranking = computed(() => {
  const list = byOwner().map((o) => ({
    name: o.owner,
    amount: o.amount,
    paid: o.amount - o.unreceived,
    rate: Math.round(o.share * 100),
    count: o.count
  }))
  const max = Math.max(...list.map((r) => r.amount), 1)
  return list.map((r) => ({ ...r, rate: Math.round((r.amount / max) * 100) }))
})

/** 今日待跟进：取跟进记录里的下一步 */
const todayFollows = computed(() => myFollowups.value.slice(0, 5))

function go(path) {
  router.push(path)
}

/** 卡片里的指标点进去，看的是这个指标背后的明细页 */
function open(path, perm) {
  if (perm && !has(perm)) return
  router.push(path)
}

function drill(k) {
  if (!k.to) return
  open(k.to, k.perm)
}

/** 趋势图上某个月 → 那个月的订单（订单列表支持 ?month=2026-11 前缀筛） */
function monthOrderPath(month) {
  const mm = String(Number(String(month).replace('月', ''))).padStart(2, '0')
  return `/sales/order?month=2026-${mm}`
}

/** 线索来源某一行 → 线索列表按该渠道筛 */
function openSource(name) {
  if (!has('clue:view')) return
  router.push(`/customer/clue?source=${encodeURIComponent(name)}`)
}

/** 业绩排行某一行：有销售统计看统计，否则回销售团队 */
function drillRank(row) {
  if (has('stats:sales:view')) return open('/stats/sales', 'stats:sales:view')
  if (has('sales:team:view')) return open('/customer/team', 'sales:team:view')
  if (has('opportunity:view')) return open('/sales/opportunity', 'opportunity:view')
  ElMessage.info(`当前岗位没有销售明细权限，${row.name} 的数据可让管理员在权限授权里开放`)
}

/** 应收未收某一行 → 应收账款按客户筛 */
function openReceivable(r) {
  if (!has('receivable:view')) return
  router.push(`/delivery/receivable?customer=${encodeURIComponent(r.customer)}`)
}
</script>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 20px;
  border-radius: var(--radius);
  background: linear-gradient(120deg, #eef3ff, #f7f9ff 55%, #eef8f5);
  border: 1px solid #e3eaf9;
  margin-bottom: 16px;
}

.hero-title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  color: #1f2b3d;
}

.hero-desc {
  margin: 0;
  font-size: 13px;
  color: var(--text-sub);
}

.hero-side {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-family: inherit;
  color: #41506a;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid #e6ebf5;
  border-radius: 999px;
  padding: 6px 12px;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

/* 胶囊本身就是入口：手型 + 悬停浮起 + 右侧箭头，明确「能点」 */
.hero-chip.link {
  cursor: pointer;
  appearance: none;
}

.hero-chip.link:hover {
  border-color: #c3d3f7;
  background: #fff;
  box-shadow: 0 6px 14px rgba(47, 95, 224, 0.12);
  transform: translateY(-1px);
}

.hero-chip.link:active {
  transform: translateY(0);
}

.hero-chip.link:focus-visible {
  outline: 2px solid #9db8ff;
  outline-offset: 2px;
}

/* 胶囊尾部的小箭头：静态也能看出是入口 */
.chip-go {
  font-size: 11px;
  color: #9aa8bd;
  margin-left: 1px;
  transition: transform 0.16s ease, color 0.16s ease;
}

.hero-chip.link:hover .chip-go {
  color: #2f5fe0;
  transform: translateX(2px);
}

.hero-chip b {
  color: #1f2b3d;
}

.hero-stat {
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12.5px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}
.dot.warn {
  background: #f0b429;
}
.dot.danger {
  background: #e05252;
}
.dot.ok {
  background: #17a67a;
}

/* KPI */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.kpi-card {
  padding: 16px 18px;
  transition: box-shadow 0.18s ease, transform 0.18s ease, border-color 0.18s ease;
}

/* 可点击的卡片：鼠标变手、悬停浮起，明确告诉用户能点进去 */
.clickable {
  cursor: pointer;
}

.kpi-card.clickable:hover {
  transform: translateY(-2px);
  border-color: #c9d8ff;
  box-shadow: 0 10px 24px rgba(47, 95, 224, 0.12);
}

.panel.clickable:hover {
  border-color: #d5e0f7;
  box-shadow: 0 8px 20px rgba(24, 42, 76, 0.07);
}

.go-icon {
  margin-left: 2px;
  font-size: 12px;
  vertical-align: -1px;
  color: #7d8ca3;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-label {
  font-size: 13px;
  color: var(--text-sub);
}

.kpi-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
}

.kpi-value {
  font-size: 24px;
  font-weight: 650;
  color: #1f2b3d;
  margin: 10px 0 8px;
  letter-spacing: 0.3px;
  font-variant-numeric: tabular-nums;
}

.kpi-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #97a3b6;
}

.kpi-delta {
  padding: 1px 7px;
  border-radius: 6px;
  font-size: 11.5px;
}
.kpi-delta.up {
  color: #0f9d76;
  background: #e8f8f2;
}
.kpi-delta.down {
  color: #c0392b;
  background: #fdeceb;
}

/* 两列布局 */
.two-col {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

/* 只剩一块面板时铺满，避免半屏空白 */
.two-col.single {
  grid-template-columns: minmax(0, 1fr);
}

@media (max-width: 1180px) {
  .two-col {
    grid-template-columns: minmax(0, 1fr);
  }
}

/* 应收未收 */
.ar-panel {
  margin-bottom: 16px;
}

.ar-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}

.ar-table th {
  text-align: left;
  font-weight: 400;
  font-size: 11.5px;
  color: #8b98ab;
  padding: 6px 8px 8px;
  border-bottom: 1px solid #eef1f7;
}

.ar-table td {
  padding: 9px 8px;
  border-bottom: 1px dashed #f2f5fa;
  color: #33415a;
}

.ar-table th.num,
.ar-table td.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.ar-table .ar-cust {
  font-weight: 500;
  color: #1f2b3d;
}

.ar-table .strong {
  font-weight: 600;
  color: #c0392b;
}

.ar-table .muted {
  color: #8b98ab;
}

.ar-table .mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11.5px;
}

.ar-table .empty {
  text-align: center;
  color: #97a3b6;
  padding: 26px 0;
}

.w-date {
  width: 108px;
}

.w-state {
  width: 96px;
}

.panel {
  padding: 16px 18px 18px;
  min-width: 0;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.panel-head h3 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2b3d;
}

.panel-head p {
  margin: 0;
  font-size: 12px;
  color: var(--text-sub);
}

.panel-badge {
  font-size: 12px;
  color: #5c6b82;
  background: #f2f5fb;
  border-radius: 999px;
  padding: 3px 10px;
  white-space: nowrap;
}

.legend {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #7b8899;
  white-space: nowrap;
}

.legend i {
  display: inline-block;
  width: 16px;
  height: 0;
  margin-right: 5px;
  vertical-align: middle;
}
.lg-line {
  border-top: 2.4px solid #3b6cff;
}
.lg-dash {
  border-top: 1.6px dashed #f0b429;
}

.trend {
  width: 100%;
  height: auto;
  display: block;
  margin-top: 8px;
}

.axis-text {
  font-size: 10.5px;
  fill: #97a3b6;
}

.point-text {
  font-size: 10.5px;
  fill: #3b6cff;
  font-weight: 600;
}

/* 来源分布 */
.source-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.source-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  margin-bottom: 6px;
}

.source-name {
  color: #33415a;
}

.source-count {
  color: #97a3b6;
  font-variant-numeric: tabular-nums;
}

.source-bar {
  height: 8px;
  border-radius: 999px;
  background: #f1f4fa;
  overflow: hidden;
}

.source-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #5b8cff, #3b6cff);
}

/* 排行 */
.rank-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 可点的行：悬停提示能钻进去看明细 */
.row-link {
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.15s ease;
}

.row-link:hover {
  background: #f8faff;
}

/* 趋势图上的数据点：整列都是热区，鼠标移上去给一条浅色竖条 */
.pt-hit {
  fill: transparent;
  transition: fill 0.15s ease;
}

.pt.link:hover .pt-hit {
  fill: rgba(59, 108, 255, 0.07);
}

.pt.link {
  cursor: pointer;
}

.pt.link:hover .point-text {
  fill: #1f47c4;
}

.pt.link:hover circle:nth-of-type(2) {
  r: 5.5;
}

/* 应收未收表格行：整行可点 */
.ar-row {
  cursor: pointer;
  transition: background 0.15s ease;
}

.ar-row:hover td {
  background: #f8faff;
}

.rank-no {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 11.5px;
  font-weight: 600;
  color: #7b8899;
  background: #f2f5fb;
  flex-shrink: 0;
}
.rank-no.no1 {
  color: #b45309;
  background: #fdf4e3;
}
.rank-no.no2 {
  color: #52627a;
  background: #eef1f7;
}
.rank-no.no3 {
  color: #a5603f;
  background: #fbeee6;
}

.rank-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  color: #fff;
  background: linear-gradient(135deg, #79a1ff, #3b6cff);
  flex-shrink: 0;
}

.rank-main {
  flex: 1;
  min-width: 0;
}

.rank-line {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}

.rank-name {
  color: #273449;
  font-weight: 500;
}

.rank-amount {
  color: #2f5fe0;
  font-variant-numeric: tabular-nums;
}

.rank-bar {
  height: 6px;
  border-radius: 999px;
  background: #f1f4fa;
  overflow: hidden;
}

.rank-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #8fb4ff, #3b6cff);
}

.rank-sub {
  font-size: 11.5px;
  color: #97a3b6;
  white-space: nowrap;
  min-width: 96px;
  text-align: right;
}

/* 待跟进 */
.follow-list {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.follow-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed #eef1f7;
}
.follow-row:last-child {
  border-bottom: none;
}

.follow-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  color: #2f5fe0;
  background: #eef3ff;
  flex-shrink: 0;
}

.follow-main {
  flex: 1;
  min-width: 0;
}

.follow-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #273449;
}

.follow-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-next {
  margin: 3px 0 0;
  font-size: 12px;
  color: #7b8899;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-time {
  font-size: 11.5px;
  color: #a7b2c4;
  white-space: nowrap;
}
</style>
