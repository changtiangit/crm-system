<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">销售统计</h2>
        <p class="page-desc">
          今年 1-11 月签单走势、销售员业绩与回款质量；卡片和排行都可以点进对应明细，方便领导看出问题后直接查到单子
        </p>
      </div>
      <div class="header-actions">
        <el-button :icon="Document" @click="router.push('/sales/order')">销售订单</el-button>
      </div>
    </div>

    <StatsTabs active="sales" />

    <!-- KPI -->
    <div class="kpi-grid">
      <div
        v-for="k in kpis"
        :key="k.label"
        class="kpi-card page-card"
        :class="{ clickable: has(k.perm) }"
        :title="has(k.perm) ? `查看${k.drill}` : ''"
        @click="open(k.to, k.perm)"
      >
        <div class="kpi-top">
          <span class="kpi-label">{{ k.label }}</span>
          <span class="kpi-icon" :style="{ background: k.bg, color: k.color }">
            <el-icon :size="15"><component :is="k.icon" /></el-icon>
          </span>
        </div>
        <div class="kpi-value">{{ k.value }}</div>
        <div class="kpi-foot">
          <span class="kpi-delta" :class="k.trend">{{ k.delta }}</span>
          <span class="kpi-sub">{{ k.sub }}</span>
        </div>
      </div>
    </div>

    <!-- 月度趋势 + 订单状态 -->
    <div class="two-col wide">
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>月度签单额 / 目标</h3>
            <p>柱子是实际签单额，虚线是当月目标；点柱子可查该月订单</p>
          </div>
          <div class="legend">
            <span><i class="lg-bar"></i>实际</span>
            <span><i class="lg-dash"></i>目标</span>
          </div>
        </header>

        <svg class="trend" viewBox="0 0 760 250" preserveAspectRatio="xMidYMid meet">
          <defs>
            <linearGradient id="barFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#5b8cff" />
              <stop offset="100%" stop-color="#9dbcff" />
            </linearGradient>
            <linearGradient id="barFillHit" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#3b6cff" />
              <stop offset="100%" stop-color="#7fa5ff" />
            </linearGradient>
          </defs>

          <g v-for="(g, i) in gridLines" :key="'g' + i">
            <line x1="58" :y1="g.y" x2="744" :y2="g.y" stroke="#eef1f7" />
            <text x="48" :y="g.y + 4" text-anchor="end" class="axis-text">{{ g.label }}</text>
          </g>

          <g v-for="(b, i) in bars" :key="'b' + i" class="bar-group" @click="drillMonth(b)">
            <rect
              :x="b.x"
              :y="b.y"
              :width="b.w"
              :height="b.h"
              rx="5"
              :fill="'url(#barFill)'"
              class="bar"
            />
            <text :x="b.cx" :y="b.y - 8" text-anchor="middle" class="bar-value">{{ b.label }}</text>
            <text :x="b.cx" y="236" text-anchor="middle" class="axis-text">{{ b.month }}</text>
            <!-- 目标虚线：每根柱子自己的目标点连起来 -->
            <line
              :x1="b.x - 12"
              :y1="b.targetY"
              :x2="b.x + b.w + 12"
              :y2="b.targetY"
              stroke="#f0b429"
              stroke-width="1.4"
              stroke-dasharray="4 3"
            />
          </g>
        </svg>
      </section>

      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>订单状态分布</h3>
            <p>按订单张数占比，悬停看金额</p>
          </div>
        </header>
        <div class="donut-wrap">
          <div class="donut" :style="{ background: donutBg }">
            <div class="donut-hole">
              <b>{{ totalOrderCount }}</b>
              <span>张订单</span>
            </div>
          </div>
        </div>
        <ul class="legend-list">
          <li v-for="s in statuses" :key="s.status">
            <i :style="{ background: colorOf(s.status) }"></i>
            <span class="legend-name">{{ s.status }}</span>
            <span class="legend-count">{{ s.count }} 张 · {{ money(s.amount) }}</span>
          </li>
        </ul>
      </section>
    </div>

    <!-- 销售员排行 + 客户贡献 -->
    <div class="two-col wide">
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>销售员业绩</h3>
            <p>按签单额排序，点一行可查该销售的订单</p>
          </div>
          <span class="panel-badge">共 {{ owners.length }} 人</span>
        </header>
        <table class="rank-table">
          <thead>
            <tr>
              <th class="w-no">#</th>
              <th>销售员</th>
              <th class="num">签单额</th>
              <th class="num">订单</th>
              <th class="num">客单价</th>
              <th class="num">毛利率</th>
              <th class="num">未回款</th>
              <th class="w-share">占比</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(o, i) in owners" :key="o.owner" class="rank-row" @click="drillOwner(o)">
              <td><span class="rank-no" :class="'no' + (i + 1)">{{ i + 1 }}</span></td>
              <td>
                <div class="owner-cell">
                  <span class="owner-avatar">{{ o.owner.slice(0, 1) }}</span>
                  <span>{{ o.owner }}</span>
                </div>
              </td>
              <td class="num strong">{{ money(o.amount) }}</td>
              <td class="num">{{ o.count }}</td>
              <td class="num">{{ money(o.avgOrder) }}</td>
              <td class="num">{{ Math.round((o.profit / o.amount) * 100) }}%</td>
              <td class="num" :class="{ danger: o.unreceived > 0 }">{{ money(o.unreceived) }}</td>
              <td>
                <div class="share-bar"><span :style="{ width: o.share * 100 + '%' }"></span></div>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>客户贡献 Top {{ topCustomers.length }}</h3>
            <p>点客户进 360 视图看全貌</p>
          </div>
          <el-button link type="primary" @click="open('/stats/customer', 'stats:customer:view')">
            采购分析<el-icon><ArrowRight /></el-icon>
          </el-button>
        </header>
        <ul class="rank-list">
          <li v-for="(c, i) in topCustomers" :key="c.customer" class="cust-row" @click="drillCustomer(c)">
            <span class="rank-no" :class="'no' + (i + 1)">{{ i + 1 }}</span>
            <div class="cust-main">
              <div class="cust-line">
                <span class="cust-name">{{ c.customer }}</span>
                <span class="cust-amount">{{ money(c.amount) }}</span>
              </div>
              <div class="cust-bar"><span :style="{ width: c.share / topCustomers[0].share * 100 + '%' }"></span></div>
              <span class="cust-sub">{{ c.level }} 级 · {{ c.count }} 单 · 最近 {{ c.last }}</span>
            </div>
          </li>
        </ul>
      </section>
    </div>

    <!-- 回款提醒 -->
    <section class="page-card panel pay-panel">
      <header class="panel-head">
        <div>
          <h3>回款提醒</h3>
          <p>签单不等于到账，这里盯住应收与逾期</p>
        </div>
        <el-button link type="primary" @click="open('/delivery/receivable', 'receivable:view')">
          应收账款<el-icon><ArrowRight /></el-icon>
        </el-button>
      </header>
      <div class="pay-grid">
        <div class="pay-item">
          <span class="pay-label">累计未收</span>
          <span class="pay-value warn">{{ money(kpi.unreceived) }}</span>
        </div>
        <div class="pay-item">
          <span class="pay-label">其中已逾期</span>
          <span class="pay-value danger">{{ money(kpi.overdue) }}</span>
        </div>
        <div class="pay-item">
          <span class="pay-label">回款率</span>
          <span class="pay-value ok">{{ Math.round((1 - kpi.unreceived / kpi.amount) * 100) }}%</span>
        </div>
        <div class="pay-item">
          <span class="pay-label">本月目标完成</span>
          <span class="pay-value" :class="monthRate >= 80 ? 'ok' : 'warn'">{{ Math.round(monthRate) }}%</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Document, ArrowRight, Money, ShoppingCart, TrendCharts, Wallet } from '@element-plus/icons-vue'
import StatsTabs from './components/StatsTabs.vue'
import { usePermission } from '@/composables/usePermission'
import { byOwner, byStatus, byCustomer, salesKpi, money, wan, CURRENT_MONTH } from '@/utils/analytics'
import { monthSales } from '@/mock/data'
import { customerPath } from '@/utils/links'

const router = useRouter()
const { has } = usePermission()

const kpi = salesKpi()
const owners = byOwner()
const statuses = byStatus()
const topCustomers = byCustomer().slice(0, 5)
const totalOrderCount = statuses.reduce((s, x) => s + x.count, 0)
const monthRate = (CURRENT_MONTH.amount / CURRENT_MONTH.target) * 100

const kpis = computed(() => [
  {
    label: '累计签单额',
    value: money(kpi.amount),
    delta: `${kpi.count} 张订单`,
    trend: 'up',
    sub: `平均客单价 ${money(kpi.avgOrder)}`,
    icon: Money,
    color: '#2f5fe0',
    bg: '#eef3ff',
    to: '/sales/order',
    perm: 'order:view',
    drill: '销售订单'
  },
  {
    label: '毛利额',
    value: money(kpi.profit),
    delta: `毛利率 ${Math.round(kpi.profitRate * 100)}%`,
    trend: kpi.profitRate > 0.4 ? 'up' : 'down',
    sub: `共卖出 ${kpi.qty.toLocaleString('zh-CN')} 件`,
    icon: TrendCharts,
    color: '#0f9d76',
    bg: '#e8f8f2',
    to: '/stats/product',
    perm: 'stats:product:view',
    drill: '产品销售分析'
  },
  {
    label: '本月签单额',
    value: money(kpi.monthAmount),
    delta: `目标完成 ${Math.round(monthRate)}%`,
    trend: monthRate >= 80 ? 'up' : 'down',
    sub: `目标 ${money(kpi.target)}`,
    icon: ShoppingCart,
    color: '#b45309',
    bg: '#fdf4e3',
    to: `/sales/order?month=${monthKey(CURRENT_MONTH.month)}`,
    perm: 'order:view',
    drill: '本月订单'
  },
  {
    label: '未回款',
    value: money(kpi.unreceived),
    delta: kpi.overdue ? `逾期 ${money(kpi.overdue)}` : '暂无逾期',
    trend: kpi.overdue ? 'down' : 'up',
    sub: '按约定回款日催收',
    icon: Wallet,
    color: '#c0392b',
    bg: '#fdeceb',
    to: '/delivery/receivable',
    perm: 'receivable:view',
    drill: '应收账款'
  }
])

/** 月份中文名 → 2026-11，订单列表按它筛 */
function monthKey(month) {
  const m = Number(String(month).replace('月', ''))
  return `2026-${String(m).padStart(2, '0')}`
}

/* ----- 柱状图几何 ----- */
const CH = { left: 58, right: 730, top: 30, bottom: 210 }

// 纵轴取整：先按 2.5 万 / 5 万 / 10 万试，选出不超过 6 档的步长，避免出现 22.2 万这种刻度
const axis = computed(() => {
  const max = Math.max(...monthSales.map((m) => Math.max(m.amount, m.target)))
  const step = [25000, 50000, 100000, 200000].find((s) => max / s <= 5) || 200000
  const top = Math.ceil(max / step) * step
  const lines = []
  for (let v = 0; v <= top; v += step) lines.push(v)
  return { max: top * 1.02, step, lines }
})

const maxV = computed(() => axis.value.max)

const bars = computed(() =>
  monthSales.map((m, i) => {
    const step = (CH.right - CH.left) / monthSales.length
    const w = Math.min(40, step * 0.42)
    const x = CH.left + step * i + (step - w) / 2
    const h = Math.max(2, (m.amount / maxV.value) * (CH.bottom - CH.top))
    return {
      ...m,
      x,
      w,
      cx: x + w / 2,
      y: CH.bottom - h,
      h,
      targetY: CH.bottom - (m.target / maxV.value) * (CH.bottom - CH.top),
      label: wan(m.amount),
      monthKey: monthKey(m.month)
    }
  })
)
const gridLines = computed(() =>
  axis.value.lines.map((v) => ({
    y: CH.bottom - (v / maxV.value) * (CH.bottom - CH.top),
    label: wan(v)
  }))
)

/* ----- 状态环形图 ----- */
const STATUS_COLOR = {
  已完成: '#3b6cff',
  已付款: '#17a67a',
  部分回款: '#5aa9f8',
  待付款: '#f0b429',
  已取消: '#c3cddd'
}
const colorOf = (s) => STATUS_COLOR[s] || '#98a6bb'
const donutBg = computed(() => {
  let acc = 0
  const parts = statuses.map((s) => {
    const from = acc
    acc += (s.count / totalOrderCount) * 100
    return `${colorOf(s.status)} ${from}% ${acc}%`
  })
  return `conic-gradient(${parts.join(', ')})`
})

function drillMonth(b) {
  if (!has('order:view')) return
  router.push(`/sales/order?month=${b.monthKey}`)
}

function drillOwner(o) {
  if (!has('order:view')) return
  router.push(`/sales/order?owner=${encodeURIComponent(o.owner)}`)
}

function drillCustomer(c) {
  router.push(customerPath(c.customer))
}

function open(path, perm) {
  if (perm && !has(perm)) return
  router.push(path)
}
</script>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.kpi-card {
  padding: 15px 17px;
  transition: box-shadow 0.18s ease, transform 0.18s ease, border-color 0.18s ease;
}

.clickable {
  cursor: pointer;
}

.kpi-card.clickable:hover {
  transform: translateY(-2px);
  border-color: #c9d8ff;
  box-shadow: 0 10px 24px rgba(47, 95, 224, 0.12);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-label {
  font-size: 12.5px;
  color: var(--text-sub);
}

.kpi-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
}

.kpi-value {
  font-size: 22px;
  font-weight: 650;
  color: #1f2b3d;
  margin: 9px 0 7px;
  font-variant-numeric: tabular-nums;
}

.kpi-foot {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: #97a3b6;
}

.kpi-delta {
  padding: 1px 6px;
  border-radius: 6px;
}
.kpi-delta.up {
  color: #0f9d76;
  background: #e8f8f2;
}
.kpi-delta.down {
  color: #c0392b;
  background: #fdeceb;
}

.two-col.wide {
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(0, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}

@media (max-width: 1180px) {
  .two-col.wide {
    grid-template-columns: minmax(0, 1fr);
  }
}

.panel {
  padding: 15px 17px 16px;
  min-width: 0;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.panel-head h3 {
  margin: 0 0 3px;
  font-size: 14.5px;
  font-weight: 600;
  color: #1f2b3d;
}

.panel-head p {
  margin: 0;
  font-size: 11.5px;
  color: var(--text-sub);
}

.panel-head .el-icon {
  font-size: 12px;
  vertical-align: -1px;
}

.panel-badge {
  font-size: 11.5px;
  color: #5c6b82;
  background: #f2f5fb;
  border-radius: 999px;
  padding: 3px 9px;
  white-space: nowrap;
}

.legend {
  display: flex;
  gap: 12px;
  font-size: 11.5px;
  color: #7b8899;
  white-space: nowrap;
}

.legend i {
  display: inline-block;
  width: 14px;
  margin-right: 5px;
  vertical-align: middle;
}
.lg-bar {
  height: 8px;
  border-radius: 3px;
  background: linear-gradient(180deg, #5b8cff, #9dbcff);
}
.lg-dash {
  border-top: 1.4px dashed #f0b429;
}

.trend {
  width: 100%;
  height: auto;
  display: block;
}

.axis-text {
  font-size: 10.5px;
  fill: #97a3b6;
}

.bar-value {
  font-size: 10.5px;
  fill: #3b6cff;
  font-weight: 600;
}

.bar-group {
  cursor: pointer;
}

.bar {
  transition: opacity 0.15s ease;
}

.bar-group:hover .bar {
  opacity: 0.78;
}

/* 环形图 */
.donut-wrap {
  display: flex;
  justify-content: center;
  padding: 6px 0 2px;
}

.donut {
  width: 148px;
  height: 148px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  position: relative;
}

.donut-hole {
  width: 98px;
  height: 98px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px #f2f5fa;
}

.donut-hole b {
  font-size: 21px;
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

.donut-hole span {
  font-size: 11.5px;
  color: #97a3b6;
}

.legend-list {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.legend-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.legend-list i {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-name {
  color: #33415a;
  min-width: 56px;
}

.legend-count {
  margin-left: auto;
  color: #8b98ab;
  font-variant-numeric: tabular-nums;
}

/* 排行表 */
.rank-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}

.rank-table th {
  text-align: left;
  font-weight: 400;
  font-size: 11.5px;
  color: #8b98ab;
  padding: 6px 6px 8px;
  border-bottom: 1px solid #eef1f7;
}

.rank-table th.num,
.rank-table td.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.rank-table td {
  padding: 9px 6px;
  border-bottom: 1px dashed #f2f5fa;
  color: #33415a;
}

.rank-row {
  cursor: pointer;
}

.rank-row:hover td {
  background: #f8faff;
}

.rank-table td.strong {
  font-weight: 600;
  color: #1f2b3d;
}

.w-no {
  width: 34px;
}
.w-share {
  width: 74px;
}

.rank-no {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: inline-grid;
  place-items: center;
  font-size: 11px;
  font-weight: 600;
  color: #7b8899;
  background: #f2f5fb;
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

.owner-cell {
  display: flex;
  align-items: center;
  gap: 7px;
}

.owner-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 11.5px;
  color: #fff;
  background: linear-gradient(135deg, #79a1ff, #3b6cff);
  flex-shrink: 0;
}

.share-bar {
  height: 6px;
  border-radius: 999px;
  background: #f1f4fa;
  overflow: hidden;
}

.share-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #8fb4ff, #3b6cff);
}

.danger {
  color: #d64545;
}

/* 客户排行 */
.rank-list {
  list-style: none;
  margin: 4px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cust-row {
  display: flex;
  gap: 9px;
  cursor: pointer;
  padding: 2px 0;
}

.cust-row:hover .cust-name {
  color: #2f5fe0;
}

.cust-main {
  flex: 1;
  min-width: 0;
}

.cust-line {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12.5px;
  margin-bottom: 5px;
}

.cust-name {
  color: #273449;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cust-amount {
  color: #2f5fe0;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.cust-bar {
  height: 6px;
  border-radius: 999px;
  background: #f1f4fa;
  overflow: hidden;
  margin-bottom: 4px;
}

.cust-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #8fb4ff, #3b6cff);
}

.cust-sub {
  font-size: 11px;
  color: #97a3b6;
}

/* 回款 */
.pay-panel {
  margin-bottom: 4px;
}

.pay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
  margin-top: 4px;
}

.pay-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8fafd;
  border: 1px solid #eef1f7;
}

.pay-label {
  font-size: 11.5px;
  color: #8b98ab;
}

.pay-value {
  font-size: 18px;
  font-weight: 650;
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

.pay-value.warn {
  color: #b45309;
}
.pay-value.danger {
  color: #d64545;
}
.pay-value.ok {
  color: #17a67a;
}
</style>
