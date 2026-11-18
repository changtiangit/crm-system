<template>
  <div class="sales-desk">
    <!-- 销售三个岗位共用一套面板，靠数据范围区分「看全公司 / 看本组 / 只看自己」 -->
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
        <div class="dk-kpi-value">{{ k.value }}</div>
        <div class="dk-kpi-foot">
          <span class="dk-delta" :class="k.trend">{{ k.delta }}</span>
          <span>{{ k.sub }}</span>
        </div>
      </div>
    </div>

    <!-- 新人/空账顾问：不摆一堆 0，直接给起步动作 -->
    <section v-if="showGuide" class="start-guide page-card">
      <div class="guide-text">
        <b>你名下还没有客户和订单</b>
        <p>销售顾问的数据范围是「仅本人」，先从公海领客户、把手上的线索跟出商机，工作台就会有数了</p>
      </div>
      <div class="guide-actions">
        <el-button type="primary" plain @click="router.push('/customer/pool')">去客户公海领取</el-button>
        <el-button plain @click="router.push('/customer/clue')">看我的线索</el-button>
      </div>
    </section>

    <div class="dk-two-col">
      <!-- 商机：销售的核心战场 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>{{ isRep ? '我的在谈商机' : '在谈商机 · 按金额排序' }}</h3>
            <p>金额大、阶段靠后的优先推；点一行进商机列表跟进</p>
          </div>
          <button class="dk-link" @click="router.push('/sales/opportunity')">
            销售商机<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="openOpps.length" class="dk-table">
          <thead>
            <tr>
              <th>商机</th>
              <th>客户</th>
              <th class="num">金额</th>
              <th class="w-stage">阶段</th>
              <th class="w-date">预计签约</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="o in openOpps"
              :key="o.id"
              class="click"
              :title="`查看商机「${o.name}」`"
              @click="router.push('/sales/opportunity')"
            >
              <td class="cell-main">{{ o.name }}</td>
              <td class="muted">{{ o.customer }}</td>
              <td class="num strong">{{ money(o.amount) }}</td>
              <td><span class="dk-tag" :class="stageTone(o.stage)">{{ o.stage }}</span></td>
              <td class="muted">{{ o.expectDate }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">没有在谈商机，去线索里挖一挖</div>
      </section>

      <!-- 排行 / 组员进度 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>{{ isRep ? '我的客户' : isGroup ? '组员进度' : '团队业绩排行' }}</h3>
            <p>{{ isRep ? '最近有下单的客户，优先维护' : '按签单额排序，进度条为完成占比' }}</p>
          </div>
          <button v-if="!isRep" class="dk-link" @click="router.push('/customer/team')">
            销售团队<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <ul class="dk-list">
          <li
            v-for="(r, i) in ranking"
            :key="r.name"
            class="dk-row"
            :class="{ click: true }"
            :title="isRep ? '查看我的客户' : `查看「${r.name}」负责的客户`"
            @click="openRank(r)"
          >
            <span v-if="!isRep" class="rank-no" :class="'no' + (i + 1)">{{ i + 1 }}</span>
            <span v-else class="dk-avatar">{{ r.name.slice(0, 1) }}</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ r.name }}</span>
                <span class="dk-row-side">{{ money(r.amount) }}</span>
              </div>
              <div class="dk-bar"><span :style="{ width: r.rate + '%' }"></span></div>
            </div>
            <span class="dk-row-side sub">{{ r.sub }}</span>
          </li>
          <li v-if="!ranking.length" class="dk-empty" style="padding: 18px 0">暂无可统计的业绩</li>
        </ul>
      </section>
    </div>

    <div class="dk-two-col even">
      <!-- 回款风险：只有能进「应收账款」的岗位才展示，否则换成报价单（销售都看得到） -->
      <section v-if="canReceivable" class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>回款风险</h3>
            <p>未收清的单按回款日排序，逾期的先打电话</p>
          </div>
          <button class="dk-link" @click="router.push('/delivery/receivable')">
            应收账款<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="openReceivables.length" class="dk-table">
          <thead>
            <tr>
              <th>客户</th>
              <th class="num">未收</th>
              <th class="w-date">约定回款日</th>
              <th class="w-state">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="r in openReceivables"
              :key="r.id"
              class="click"
              @click="router.push(`/delivery/receivable?customer=${encodeURIComponent(r.customer)}`)"
            >
              <td class="strong">{{ r.customer }}</td>
              <td class="num strong">{{ money(r.total - r.received) }}</td>
              <td class="muted">{{ r.dueDate }}</td>
              <td>
                <span v-if="daysLate(r) > 0" class="dk-tag danger">逾期 {{ daysLate(r) }} 天</span>
                <span v-else class="dk-tag ok">{{ -daysLate(r) }} 天后</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">回款都清了，没有应收风险</div>
      </section>

      <section v-else class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>我的报价单</h3>
            <p>报价过期就失效，待审批和快到期的先处理</p>
          </div>
          <button class="dk-link" @click="router.push('/sales/quote')">
            报价单<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="myQuotes.length" class="dk-table">
          <thead>
            <tr>
              <th>报价单</th>
              <th>客户</th>
              <th class="num">金额</th>
              <th class="w-state">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="q in myQuotes" :key="q.id" class="click" @click="router.push('/sales/quote')">
              <td class="mono muted">{{ q.quoteNo }}</td>
              <td>{{ q.customer }}</td>
              <td class="num strong">{{ money(q.amount) }}</td>
              <td><span class="dk-tag" :class="quoteTone(q.status)">{{ q.status }}</span></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">还没有报价单，从商机里发起一份</div>
      </section>

      <!-- 今日跟进 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>今日跟进</h3>
            <p>来自跟进记录里的下一步安排</p>
          </div>
          <button class="dk-link" @click="router.push('/customer/followup')">
            跟进记录<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <ul v-if="todayFollows.length" class="dk-list">
          <li
            v-for="f in todayFollows"
            :key="f.id"
            class="dk-row click"
            :title="`查看「${f.customer}」的跟进记录`"
            @click="router.push('/customer/followup')"
          >
            <span class="dk-avatar">{{ (f.contact || f.customer).slice(0, 1) }}</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ f.customer }}</span>
                <span class="dk-tag info">{{ f.type }}</span>
              </div>
              <p class="dk-row-sub">{{ f.next }}</p>
            </div>
            <span class="dk-row-side">{{ (f.time || '').slice(5, 10) }}</span>
          </li>
        </ul>
        <div v-else class="dk-empty">今天没有安排跟进，可以去线索池捞一条</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Aim, TrendCharts, User, Wallet } from '@element-plus/icons-vue'
import { customers, opportunities, receivables, followups, quotes, monthSales } from '@/mock/data'
import { money, byOwner, scopedValidOrders } from '@/utils/analytics'
import { scopeRows } from '@/utils/scope'
import { usePermission } from '@/composables/usePermission'

const props = defineProps({
  profile: { type: Object, required: true }
})

const router = useRouter()
const { has } = usePermission()

const isRep = computed(() => props.profile.key === 'sales_rep')
const isGroup = computed(() => props.profile.key === 'sales_group')

/**
 * 销售角色里只有总监/组长和财务才有「应收账款」权限，
 * 顾问（SALES_G1 的 SELF 账号）没有，所以回款面板要按权限换内容，
 * 否则会出现「金额 ¥0 + 点进去 403」。
 */
const canReceivable = computed(() => has('receivable:view'))

/** 顾问名下有客户或订单才算「有账可看」，否则给起步指引而不是一排 0 */
const showGuide = computed(() => isRep.value && scopeRows(customers).length === 0 && scopedValidOrders().length === 0)

const today = new Date().toISOString().slice(0, 10)
const todayTime = new Date(today + 'T00:00:00').getTime()
const daysLate = (r) => Math.round((todayTime - new Date(r.dueDate + 'T00:00:00').getTime()) / 86400000)

const openOpps = computed(() =>
  scopeRows(opportunities)
    .filter((o) => !['赢单', '输单'].includes(o.stage))
    .slice()
    .sort((a, b) => Number(b.amount) - Number(a.amount))
    .slice(0, 6)
)

const openOppAmount = computed(() =>
  scopeRows(opportunities)
    .filter((o) => !['赢单', '输单'].includes(o.stage))
    .reduce((s, o) => s + Number(o.amount || 0), 0)
)

const openReceivables = computed(() =>
  scopeRows(receivables)
    .filter((r) => r.total - r.received > 0)
    .slice()
    .sort((a, b) => String(a.dueDate).localeCompare(String(b.dueDate)))
)
const unreceived = computed(() => openReceivables.value.reduce((s, r) => s + (r.total - r.received), 0))
const overdueCount = computed(() => openReceivables.value.filter((r) => daysLate(r) > 0).length)

/** 没有应收权限的岗位，第三张卡换成「待处理报价」，页面上不留无效入口 */
const myQuotes = computed(() => {
  const open = ['待审批', '草稿', '已通过']
  return scopeRows(quotes).filter((q) => open.includes(q.status)).slice(0, 5)
})

const thirdKpi = computed(() => {
  if (canReceivable.value) {
    return {
      label: isGroup.value ? '组内未收款' : isRep.value ? '我的未收款' : '应收未收',
      value: money(unreceived.value),
      delta: overdueCount.value ? `逾期 ${overdueCount.value} 单` : '无逾期',
      trend: overdueCount.value ? 'down' : 'up',
      sub: `${openReceivables.value.length} 单待收`,
      icon: TrendCharts,
      bg: '#f3edff',
      color: '#7c3aed',
      to: '/delivery/receivable',
      drill: '应收账款'
    }
  }
  return {
    label: '待处理报价',
    value: myQuotes.value.length,
    delta: `${myQuotes.value.filter((q) => q.status === '待审批').length} 份待审批`,
    trend: 'info',
    sub: '过期未跟进会自动失效',
    icon: TrendCharts,
    bg: '#f3edff',
    color: '#7c3aed',
    to: '/sales/quote',
    drill: '报价单'
  }
})

/** 本月口径按「订单里最新的那个月」算，避免演示数据与真实日期错位 */
const monthKey = computed(() => {
  const keys = scopedValidOrders()
    .map((o) => String(o.orderDate || '').slice(0, 7))
    .filter(Boolean)
    .sort()
  return keys.length ? keys[keys.length - 1] : ''
})

const monthOrders = computed(() => scopedValidOrders().filter((o) => String(o.orderDate || '').slice(0, 7) === monthKey.value))
const monthAmount = computed(() => monthOrders.value.reduce((s, o) => s + Number(o.amount || 0), 0))
const monthTarget = computed(() => {
  const m = monthSales.find((x) => Number(String(x.month).replace('月', '')) === Number(monthKey.value.slice(5)))
  return Number(m?.target || 0)
})
const monthRate = computed(() => (monthTarget.value ? Math.round((monthAmount.value / monthTarget.value) * 100) : 0))

/** 排行：总监/组长看团队（按数据范围），顾问看自己的客户贡献 */
const ranking = computed(() => {
  if (isRep.value) {
    const mine = scopeRows(customers).map((c) => {
      const amount = scopedValidOrders()
        .filter((o) => o.customer === c.name)
        .reduce((s, o) => s + Number(o.amount || 0), 0)
      return { name: c.name, amount, sub: c.level + ' 级客户', to: '/customer/list' }
    })
    const max = Math.max(...mine.map((r) => r.amount), 1)
    return mine
      .sort((a, b) => b.amount - a.amount)
      .slice(0, 6)
      .map((r) => ({ ...r, rate: Math.round((r.amount / max) * 100) }))
  }
  const list = byOwner()
  const max = Math.max(...list.map((r) => r.amount), 1)
  return list.slice(0, 6).map((r) => ({
    name: r.owner,
    amount: r.amount,
    rate: Math.round((r.amount / max) * 100),
    sub: `${r.count} 单 · 未收 ${money(r.unreceived)}`,
    to: `/customer/list?owner=${encodeURIComponent(r.owner)}`
  }))
})

function openRank(r) {
  router.push(r.to || '/customer/list')
}

const todayFollows = computed(() => scopeRows(followups).slice(0, 5))

const kpis = computed(() => {
  if (isRep.value) {
    return [
      {
        label: '我的本月签单',
        value: money(monthAmount.value),
        // 个人目标由公司目标拆解，演示数据里不写死，改看单量与客单价更实在
        delta: `本月 ${monthOrders.value.length} 单`,
        trend: 'up',
        sub: `客单价 ${money(monthOrders.value.length ? monthAmount.value / monthOrders.value.length : 0)}`,
        icon: Wallet,
        bg: '#eef3ff',
        color: '#2f5fe0',
        to: '/sales/order',
        drill: '我的订单'
      },
      {
        label: '我的在谈商机',
        value: money(openOppAmount.value),
        delta: `${openOpps.value.length} 个商机`,
        trend: 'info',
        sub: '待推进',
        icon: Aim,
        bg: '#e8f8f2',
        color: '#0f9d76',
        to: '/sales/opportunity',
        drill: '销售商机'
      },
      thirdKpi.value,
      {
        label: '我的客户',
        value: scopeRows(customers).length,
        delta: `本月 ${monthOrders.value.length} 单`,
        trend: 'up',
        sub: '在手客户数',
        icon: User,
        bg: '#fdf4e3',
        color: '#b45309',
        to: '/customer/list',
        drill: '客户档案'
      }
    ]
  }

  const team = byOwner()
  const repCount = team.length
  return [
    {
      label: isGroup.value ? '本组本月签单' : '本月签单额',
      value: money(monthAmount.value),
      delta: `目标完成 ${monthRate.value}%`,
      trend: monthRate.value >= 80 ? 'up' : 'down',
      sub: `目标 ${money(monthTarget.value)}`,
      icon: Wallet,
      bg: '#eef3ff',
      color: '#2f5fe0',
      // 总监/组长不一定有「销售统计」权限，没权限就落到订单列表，避免点进 403
      to: has('stats:sales:view') ? '/stats/sales' : '/sales/order',
      drill: has('stats:sales:view') ? '销售统计' : '销售订单'
    },
    {
      label: '在谈商机',
      value: money(openOppAmount.value),
      delta: `${openOpps.value.length} 个重点商机`,
      trend: 'info',
      sub: '金额合计',
      icon: Aim,
      bg: '#e8f8f2',
      color: '#0f9d76',
      to: '/sales/opportunity',
      drill: '销售商机'
    },
    thirdKpi.value,
    {
      label: isGroup.value ? '在册组员' : '在册销售',
      value: repCount,
      delta: `本月 ${monthOrders.value.length} 单`,
      trend: 'up',
      sub: '有业绩记录的销售',
      icon: User,
      bg: '#fdf4e3',
      color: '#b45309',
      to: '/customer/team',
      drill: '销售团队'
    }
  ]
})

const stageTone = (s) => (s === '商务谈判' ? 'danger' : s === '方案报价' ? 'warn' : 'info')
const quoteTone = (s) => (s === '待审批' ? 'warn' : s === '草稿' ? 'info' : 'ok')
</script>

<style scoped>
.start-guide {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 18px;
  margin-bottom: 16px;
  background: linear-gradient(120deg, #eef3ff, #ffffff 70%);
  border: 1px solid #e3eaf9;
}

.guide-text b {
  font-size: 14px;
  color: #1f2b3d;
}

.guide-text p {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: #5c6b82;
}

.guide-actions {
  display: flex;
  gap: 8px;
}

.w-stage {
  width: 84px;
}

.w-date {
  width: 94px;
}

.w-state {
  width: 104px;
}

.cell-main {
  font-size: 12.5px;
  font-weight: 500;
  color: #1f2b3d;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.sub {
  min-width: 105px;
  text-align: right;
}
</style>
