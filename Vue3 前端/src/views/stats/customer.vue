<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">客户采购分析</h2>
        <p class="page-desc">
          谁买得多、买得勤、买什么品类，一眼看清客户结构；点客户直接进 360 视图，跟进、订单、回款、售后一页看全
        </p>
      </div>
      <div class="header-actions">
        <el-button :icon="User" @click="router.push('/customer/list')">客户档案</el-button>
      </div>
    </div>

    <StatsTabs active="customer" />

    <!-- KPI -->
    <div class="kpi-grid">
      <div
        v-for="k in kpis"
        :key="k.label"
        class="kpi-card page-card"
        :class="{ clickable: has(k.perm) }"
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

    <!-- 采购排行 -->
    <section class="page-card panel">
      <header class="panel-head">
        <div>
          <h3>客户采购排行</h3>
          <p>按累计采购额排序；「主要品类」是该客户花钱最多的品类，点行看客户 360</p>
        </div>
        <span class="panel-badge">共 {{ customers.length }} 家</span>
      </header>

      <table class="rank-table">
        <thead>
          <tr>
            <th class="w-no">#</th>
            <th>客户名称</th>
            <th class="w-lv">级别</th>
            <th class="num">订单</th>
            <th class="num">采购额</th>
            <th class="w-share">金额占比</th>
            <th>主要品类</th>
            <th class="w-date">最近下单</th>
            <th class="num">未回款</th>
            <th class="w-owner">负责人</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(c, i) in customers" :key="c.customer" class="rank-row" @click="drill(c)">
            <td><span class="rank-no" :class="'no' + (i + 1)">{{ i + 1 }}</span></td>
            <td>
              <span class="cust-name">{{ c.customer }}</span>
              <span class="cust-industry">{{ c.industry }}</span>
            </td>
            <td>
              <el-tag size="small" round effect="light" :type="levelType[c.level]">{{ c.level }}</el-tag>
            </td>
            <td class="num">{{ c.count }}</td>
            <td class="num strong">{{ money(c.amount) }}</td>
            <td>
              <div class="share-cell">
                <div class="share-bar"><span :style="{ width: c.share / customers[0].share * 100 + '%' }"></span></div>
                <span class="share-text">{{ Math.round(c.share * 100) }}%</span>
              </div>
            </td>
            <td class="muted">{{ c.topCategory }}</td>
            <td class="muted">{{ timeAgo(c.last) }}</td>
            <td class="num" :class="{ danger: c.unreceived > 0 }">{{ c.unreceived ? money(c.unreceived) : '—' }}</td>
            <td class="muted">{{ c.owner }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <div class="two-col">
      <!-- 客户级别 -->
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>客户级别结构</h3>
            <p>A 类客户是重点维护对象，采购额占比要看住</p>
          </div>
          <el-button link type="primary" @click="open('/customer/list', 'customer:view')">
            客户档案<el-icon><ArrowRight /></el-icon>
          </el-button>
        </header>
        <div class="level-list">
          <div v-for="l in levels" :key="l.level" class="level-row">
            <span class="level-tag" :class="'lv-' + l.level">{{ l.level }}</span>
            <div class="level-main">
              <div class="level-line">
                <span>{{ l.count }} 家客户</span>
                <span class="level-amount">{{ money(l.amount) }} · {{ Math.round(l.share * 100) }}%</span>
              </div>
              <div class="level-bar"><span :style="{ width: l.share * 100 + '%' }" :class="'bar-' + l.level"></span></div>
            </div>
          </div>
        </div>

        <div class="split-row">
          <div class="split-item">
            <span class="split-label">老客户（合作满 3 个月）</span>
            <span class="split-value">{{ split.old.count }} 家 · {{ money(split.old.amount) }}</span>
          </div>
          <div class="split-item">
            <span class="split-label">新客户（近 3 个月首单）</span>
            <span class="split-value fresh">{{ split.fresh.count }} 家 · {{ money(split.fresh.amount) }}</span>
          </div>
        </div>
      </section>

      <!-- 活跃度 -->
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>客户活跃度提醒</h3>
            <p>按距上次下单的天数排序，越靠前越该安排一次回访</p>
          </div>
          <el-button link type="primary" @click="open('/service/revisit', 'revisit:view')">
            客户回访<el-icon><ArrowRight /></el-icon>
          </el-button>
        </header>
        <ul v-if="quiet.length" class="sleep-list">
          <li v-for="s in quiet" :key="s.customer" class="sleep-row" @click="drill(s)">
            <span class="sleep-avatar" :class="{ danger: s.idleDays > 60 }">{{ s.customer.slice(0, 1) }}</span>
            <div class="sleep-main">
              <div class="sleep-line">
                <span class="sleep-name">{{ s.customer }}</span>
                <span class="sleep-days" :class="{ danger: s.idleDays > 60 }">{{ s.idleDays }} 天未下单</span>
              </div>
              <span class="sleep-sub">上次下单 {{ s.last }} · 累计 {{ money(s.amount) }}</span>
            </div>
            <el-icon class="sleep-go"><ArrowRight /></el-icon>
          </li>
        </ul>
        <div v-else class="mini-empty">还没有成交记录</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, ArrowRight, UserFilled, Refresh, Money, PieChart } from '@element-plus/icons-vue'
import StatsTabs from './components/StatsTabs.vue'
import { usePermission } from '@/composables/usePermission'
import { byCustomer, byLevel, newOldSplit, quietCustomers, money, sum } from '@/utils/analytics'
import { customers as archive } from '@/mock/data'
import { customerPath } from '@/utils/links'

const router = useRouter()
const { has } = usePermission()

const customers = byCustomer()
const levels = byLevel()
const split = newOldSplit()
// 只取最久没下单的 4 家，避免面板被撑太长
const quiet = quietCustomers().slice(0, 4)

const totalAmount = sum(customers, 'amount')
const repeat = customers.filter((c) => c.count >= 2).length
const top1Share = customers[0]?.share || 0

const levelType = { A: 'primary', B: 'success', C: 'info' }

const kpis = computed(() => [
  {
    label: '有采购客户',
    value: `${customers.length} 家`,
    delta: `占档案 ${Math.round((customers.length / (archive.length || 1)) * 100)}%`,
    trend: 'up',
    sub: `共 ${customers.reduce((s, c) => s + c.count, 0)} 张订单`,
    icon: UserFilled,
    color: '#2f5fe0',
    bg: '#eef3ff',
    to: '/customer/list',
    perm: 'customer:view'
  },
  {
    label: '复购客户',
    value: `${repeat} 家`,
    delta: `复购率 ${Math.round((repeat / (customers.length || 1)) * 100)}%`,
    trend: repeat / (customers.length || 1) > 0.6 ? 'up' : 'down',
    sub: '下单 ≥ 2 次视为复购',
    icon: Refresh,
    color: '#0f9d76',
    bg: '#e8f8f2',
    to: '/stats/customer',
    perm: 'stats:customer:view'
  },
  {
    label: '平均客单价',
    value: money(totalAmount / (customers.reduce((s, c) => s + c.count, 0) || 1)),
    delta: `累计 ${money(totalAmount)}`,
    trend: 'up',
    sub: '含税销售额 ÷ 订单数',
    icon: Money,
    color: '#b45309',
    bg: '#fdf4e3',
    to: '/sales/order',
    perm: 'order:view'
  },
  {
    label: '头部客户集中度',
    value: Math.round(top1Share * 100) + '%',
    delta: customers[0]?.customer.slice(0, 6) || '—',
    trend: top1Share > 0.3 ? 'down' : 'up',
    sub: top1Share > 0.3 ? '单一客户依赖偏高，注意分散' : '客户结构较健康',
    icon: PieChart,
    color: '#7c4dff',
    bg: '#f2eeff',
    to: '/stats/customer',
    perm: 'stats:customer:view'
  }
])

/** 最近下单距离今天多久，比直接给日期更好读 */
function timeAgo(date) {
  const days = Math.round((new Date() - new Date(date + 'T00:00:00')) / 86400000)
  if (days <= 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 30) return `${days} 天前`
  return `${Math.floor(days / 30)} 个月前`
}

function drill(c) {
  router.push(customerPath(c.id || c.customer))
}

function open(path, perm) {
  if (perm && !has(perm)) return
  router.push(path)
}
</script>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(215px, 1fr));
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
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kpi-delta.up {
  color: #0f9d76;
  background: #e8f8f2;
}
.kpi-delta.down {
  color: #c0392b;
  background: #fdeceb;
}

.panel {
  padding: 15px 17px 16px;
  margin-bottom: 14px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
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
  padding: 6px 8px 8px;
  border-bottom: 1px solid #eef1f7;
}

.rank-table th.num,
.rank-table td.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.rank-table td {
  padding: 9px 8px;
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

.rank-table td.muted {
  color: #8b98ab;
}

.w-no {
  width: 34px;
}
.w-lv {
  width: 58px;
}
.w-share {
  width: 118px;
}
.w-date {
  width: 88px;
}
.w-owner {
  width: 70px;
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

.cust-name {
  font-weight: 500;
  color: #273449;
}

.rank-row:hover .cust-name {
  color: #2f5fe0;
}

.cust-industry {
  display: block;
  font-size: 11px;
  color: #a7b2c4;
  margin-top: 2px;
}

.share-cell {
  display: flex;
  align-items: center;
  gap: 7px;
}

.share-bar {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: #f1f4fa;
  overflow: hidden;
  min-width: 40px;
}

.share-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #8fb4ff, #3b6cff);
}

.share-text {
  font-size: 11px;
  color: #8b98ab;
  font-variant-numeric: tabular-nums;
  width: 26px;
  text-align: right;
}

.danger {
  color: #d64545 !important;
}

.two-col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 14px;
}

/* 级别 */
.level-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 14px;
}

.level-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.level-tag {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 11.5px;
  font-weight: 600;
  flex-shrink: 0;
}
.lv-A {
  color: #2f5fe0;
  background: #eef3ff;
}
.lv-B {
  color: #0f9d76;
  background: #e8f8f2;
}
.lv-C {
  color: #7b8899;
  background: #f2f5fb;
}

.level-main {
  flex: 1;
  min-width: 0;
}

.level-line {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: #33415a;
  margin-bottom: 5px;
}

.level-amount {
  color: #5c6b82;
  font-variant-numeric: tabular-nums;
}

.level-bar {
  height: 7px;
  border-radius: 999px;
  background: #f1f4fa;
  overflow: hidden;
}

.level-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
}
.bar-A {
  background: linear-gradient(90deg, #8fb4ff, #3b6cff);
}
.bar-B {
  background: linear-gradient(90deg, #6fd3b4, #17a67a);
}
.bar-C {
  background: #c3cddd;
}

.split-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.split-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafd;
  border: 1px solid #eef1f7;
}

.split-label {
  font-size: 11.5px;
  color: #8b98ab;
}

.split-value {
  font-size: 13px;
  font-weight: 600;
  color: #1f2b3d;
}

.split-value.fresh {
  color: #2f5fe0;
}

/* 沉睡客户 */
.sleep-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.sleep-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed #f2f5fa;
  cursor: pointer;
}
.sleep-row:last-child {
  border-bottom: none;
}
.sleep-row:hover .sleep-name {
  color: #2f5fe0;
}

.sleep-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12.5px;
  color: #b45309;
  background: #fdf4e3;
  flex-shrink: 0;
}

.sleep-avatar.danger {
  color: #c0392b;
  background: #fdeceb;
}

.sleep-main {
  flex: 1;
  min-width: 0;
}

.sleep-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.sleep-name {
  font-size: 12.5px;
  color: #273449;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sleep-days {
  font-size: 11.5px;
  color: #b45309;
  white-space: nowrap;
}

.sleep-days.danger {
  color: #c0392b;
  font-weight: 500;
}

.sleep-sub {
  font-size: 11px;
  color: #a7b2c4;
}

.sleep-go {
  font-size: 12px;
  color: #c3cddd;
}

.mini-empty {
  padding: 20px 0;
  font-size: 12.5px;
  color: #a7b2c4;
  text-align: center;
}
</style>
