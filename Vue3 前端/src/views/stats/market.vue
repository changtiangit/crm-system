<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">市场分析</h2>
        <p class="page-desc">
          渠道进来多少线索、多久分到销售手上、最后转化了几条；营销只看线索链路，不涉客户档案与签单金额
        </p>
      </div>
      <div class="header-actions">
        <el-button :icon="Share" @click="router.push('/customer/clue')">销售线索</el-button>
      </div>
    </div>

    <StatsTabs active="market" />

    <!-- KPI -->
    <div class="kpi-grid">
      <div
        v-for="k in kpis"
        :key="k.label"
        class="kpi-card page-card"
        :class="{ clickable: !!k.to }"
        :title="k.to ? `查看${k.drill}` : ''"
        @click="k.to && router.push(k.to)"
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

    <div class="two-col wide">
      <!-- 渠道效果 -->
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>渠道效果</h3>
            <p>哪个渠道进来的线索又多又准：转化率高、废弃率低的渠道值得加预算</p>
          </div>
          <span class="panel-badge">共 {{ total }} 条线索</span>
        </header>

        <table class="rank-table">
          <thead>
            <tr>
              <th>渠道</th>
              <th class="num">线索</th>
              <th class="w-share">占比</th>
              <th class="num">已转化</th>
              <th class="num">转化率</th>
              <th class="num">废弃率</th>
              <th class="num">平均分配</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in sources" :key="s.name" class="row-click" :title="`查看「${s.name}」进来的线索`" @click="toClue({ source: s.name })">
              <td>
                <span class="src-dot" :style="{ background: s.color }"></span>
                <span class="src-name">{{ s.name }}</span>
              </td>
              <td class="num strong">{{ s.total }}</td>
              <td>
                <div class="share-cell">
                  <div class="share-bar"><span :style="{ width: s.share * 100 + '%', background: s.color }"></span></div>
                  <span class="share-text">{{ Math.round(s.share * 100) }}%</span>
                </div>
              </td>
              <td class="num">{{ s.converted }}</td>
              <td class="num" :class="rateClass(s.rate)">{{ pct(s.rate) }}</td>
              <td class="num" :class="{ danger: s.wasteRate >= 0.3 }">{{ pct(s.wasteRate) }}</td>
              <td class="num muted">{{ s.avgHours === null ? '—' : s.avgHours.toFixed(1) + ' h' }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- 线索漏斗 -->
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>线索漏斗</h3>
            <p>从进来到成交每一段的存量；「待分配」压得越少，销售接得越快</p>
          </div>
          <span class="panel-badge">转化 {{ pct(conversionRate) }}</span>
        </header>

        <div class="funnel">
          <div
            v-for="f in funnel"
            :key="f.status"
            class="funnel-row row-click"
            :title="`查看状态为「${f.status}」的线索`"
            @click="toClue({ status: f.status })"
          >
            <span class="funnel-label">{{ f.status }}</span>
            <div class="funnel-track">
              <span class="funnel-bar" :style="{ width: f.width + '%', background: f.color }"></span>
            </div>
            <span class="funnel-value">{{ f.count }}</span>
            <span class="funnel-pct">{{ pct(f.share) }}</span>
          </div>
        </div>

        <div class="funnel-foot">
          <div class="foot-item">
            <span class="foot-label">已分配</span>
            <span class="foot-value">{{ total - unassigned }}</span>
          </div>
          <div class="foot-item">
            <span class="foot-label">待分配</span>
            <span class="foot-value warn">{{ unassigned }}</span>
          </div>
          <div class="foot-item">
            <span class="foot-label">已废弃</span>
            <span class="foot-value">{{ wasted }}</span>
          </div>
        </div>
      </section>
    </div>

    <!-- 月度趋势 -->
    <section class="page-card panel">
      <header class="panel-head">
        <div>
          <h3>线索量与转化率</h3>
          <p>柱子是当月新增线索，折线是当月转化率；当月线索还在跟进，转化率天然偏低</p>
        </div>
        <div class="legend">
          <span><i class="lg-bar"></i>新增线索</span>
          <span><i class="lg-line"></i>转化率</span>
        </div>
      </header>

      <svg class="trend" viewBox="0 0 760 230" preserveAspectRatio="xMidYMid meet">
        <defs>
          <linearGradient id="leadBar" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#5b8cff" />
            <stop offset="100%" stop-color="#b9cdff" />
          </linearGradient>
        </defs>
        <g class="grid">
          <line v-for="g in gridLines" :key="g.y" :x1="40" :x2="716" :y1="g.y" :y2="g.y" />
          <text v-for="g in gridLines" :key="'t' + g.y" :x="34" :y="g.y + 3" text-anchor="end">{{ g.label }}</text>
          <!-- 右轴：转化率（折线对应刻度），与左轴线索量分开读 -->
          <text v-for="r in rateTicks" :key="'r' + r.v" class="axis-right" :x="722" :y="rateY(r.v) + 3">
            {{ r.label }}
          </text>
        </g>

        <g v-for="(m, i) in trend" :key="m.month" class="bar-group" @click="toClue({ month: m.month })">
          <title>{{ m.label }}新增 {{ m.total }} 条，点击查看当月线索</title>
          <rect :x="barX(i) - 4" :y="20" :width="42" height="170" fill="transparent" />
          <rect
            class="bar"
            fill="url(#leadBar)"
            :x="barX(i)"
            :y="barY(m.total)"
            :width="34"
            :height="Math.max(2, 186 - barY(m.total))"
            rx="5"
          />
          <text class="bar-label" :x="barX(i) + 17" :y="barY(m.total) - 8" text-anchor="middle">{{ m.total }}</text>
          <text class="x-label" :x="barX(i) + 17" y="208" text-anchor="middle">{{ m.label }}</text>
        </g>

        <polyline class="rate-line" :points="ratePoints" />
        <g v-for="(m, i) in trend" :key="'r' + m.month">
          <circle class="rate-dot" :cx="barX(i) + 17" :cy="rateY(m.rate)" r="3.6" />
          <text class="rate-label" :x="barX(i) + 17" :y="rateY(m.rate) - 10" text-anchor="middle">
            {{ pct(m.rate) }}
          </text>
        </g>
      </svg>
    </section>

    <div class="two-col">
      <!-- 意向结构 -->
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>意向等级结构</h3>
            <p>高意向线索优先分给能力等级高的销售，跟进成功率更高</p>
          </div>
        </header>
        <div class="intent-list">
          <div
            v-for="it in intents"
            :key="it.level"
            class="intent-row row-click"
            :title="`查看意向「${it.level}」的线索`"
            @click="toClue({ intent: it.level })"
          >
            <span class="intent-tag" :class="'lv-' + it.level">{{ it.level }}</span>
            <div class="intent-main">
              <div class="intent-line">
                <span>{{ it.count }} 条</span>
                <span class="intent-amount">已转化 {{ it.converted }} · {{ pct(it.rate) }}</span>
              </div>
              <div class="intent-bar"><span :style="{ width: it.share * 100 + '%' }"></span></div>
            </div>
          </div>
        </div>
      </section>

      <!-- 销售承接 -->
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>销售承接情况</h3>
            <p>谁接得多、谁转化好；分不出去的线索会堆在「待分配」里</p>
          </div>
          <el-button link type="primary" @click="router.push('/customer/team')">
            销售团队<el-icon><ArrowRight /></el-icon>
          </el-button>
        </header>

        <table class="rank-table">
          <thead>
            <tr>
              <th class="w-no">#</th>
              <th>销售</th>
              <th class="num">接线索</th>
              <th class="num">已转化</th>
              <th class="w-share">转化率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(o, i) in owners" :key="o.name" class="row-click" title="去销售团队看这位销售的负荷" @click="router.push('/customer/team')">
              <td><span class="rank-no">{{ i + 1 }}</span></td>
              <td class="strong">{{ o.name }}</td>
              <td class="num">{{ o.total }}</td>
              <td class="num">{{ o.converted }}</td>
              <td>
                <div class="share-cell">
                  <div class="share-bar"><span :style="{ width: o.rate * 100 + '%' }"></span></div>
                  <span class="share-text">{{ pct(o.rate) }}</span>
                </div>
              </td>
            </tr>
            <tr v-if="!owners.length">
              <td colspan="5" class="empty">还没有线索分配给销售</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Promotion, Share, Timer, TrendCharts, User } from '@element-plus/icons-vue'
import StatsTabs from './components/StatsTabs.vue'
import request from '@/utils/request'

const router = useRouter()

/** 线索是营销唯一的业务数据源：列表接口已按数据范围过滤，这里只做聚合 */
const rows = ref([])
const loading = ref(false)

const SOURCE_COLORS = {
  官网表单: '#3b6cff',
  展会: '#12a594',
  广告投放: '#8b5cf6',
  老客户推荐: '#f5a524',
  电话咨询: '#0ea5e9'
}
const STATUS_COLORS = {
  待分配: '#f5a524',
  待跟进: '#3b6cff',
  跟进中: '#6366f1',
  已转化: '#12a594',
  已废弃: '#98a2b3'
}
const INTENT_ORDER = ['高', '中', '低']

const CONVERTED = '已转化'
const WASTED = '已废弃'
const UNASSIGNED = '待分配'
/** 线索从进来到分给销售的目标时效（小时） */
const SLA_HOURS = 8

const total = computed(() => rows.value.length)
const unassigned = computed(() => rows.value.filter((r) => r.status === UNASSIGNED || !r.owner).length)
const converted = computed(() => rows.value.filter((r) => r.status === CONVERTED).length)
const wasted = computed(() => rows.value.filter((r) => r.status === WASTED).length)
const conversionRate = computed(() => (total.value ? converted.value / total.value : 0))

const monthKey = (r) => (r.createTime || '').slice(0, 7)

/** 本月：数据里最新一个月，避免演示数据与真实日期错位 */
const currentMonth = computed(() => {
  const keys = rows.value.map(monthKey).filter(Boolean).sort()
  return keys.length ? keys[keys.length - 1] : ''
})
const monthLeads = computed(() => rows.value.filter((r) => monthKey(r) === currentMonth.value))

/** 平均分配时长：创建到分配的间隔，单位小时 */
function avgAssignHours(list) {
  const hours = list
    .filter((r) => r.assignTime && r.createTime)
    .map((r) => (new Date(r.assignTime.replace(/-/g, '/')) - new Date(r.createTime.replace(/-/g, '/'))) / 3600000)
    .filter((h) => h >= 0 && h < 24 * 30)
  if (!hours.length) return null
  return hours.reduce((a, b) => a + b, 0) / hours.length
}

const kpis = computed(() => {
  const monthCount = monthLeads.value.length
  const prevKey = prevMonth(currentMonth.value)
  const prevCount = rows.value.filter((r) => monthKey(r) === prevKey).length
  const diff = prevCount ? monthCount - prevCount : 0
  const rateMonth = monthLeads.value.filter((r) => r.status === CONVERTED).length
  const hours = avgAssignHours(rows.value)
  return [
    {
      label: '线索总量',
      value: total.value,
      delta: prevCount ? (diff >= 0 ? `较上月 +${diff}` : `较上月 ${diff}`) : '上月无数据',
      trend: diff >= 0 ? 'up' : 'down',
      sub: `本月新增 ${monthCount}`,
      icon: Promotion,
      bg: '#eef3ff',
      color: '#3b6cff',
      to: '/customer/clue',
      drill: '全部线索'
    },
    {
      label: '待分配',
      value: unassigned.value,
      delta: unassigned.value ? '需尽快分配' : '已全部分配',
      trend: unassigned.value ? 'down' : 'up',
      sub: `占全部 ${pct(total.value ? unassigned.value / total.value : 0)}`,
      icon: User,
      bg: '#fff5e6',
      color: '#f5a524',
      to: `/customer/clue?status=${encodeURIComponent(UNASSIGNED)}`,
      drill: '待分配线索'
    },
    {
      label: '线索转化率',
      value: pct(conversionRate.value),
      delta: `已转化 ${converted.value} 条`,
      trend: 'up',
      sub: `本月转化 ${rateMonth} 条`,
      icon: TrendCharts,
      bg: '#e8f8f2',
      color: '#12a594',
      to: `/customer/clue?status=${encodeURIComponent(CONVERTED)}`,
      drill: '已转化线索'
    },
    {
      label: '平均分配时长',
      value: hours === null ? '—' : hours.toFixed(1) + ' h',
      delta: hours === null ? '暂无分配' : hours <= SLA_HOURS ? '≤ 8h 达标' : '超 8h',
      trend: hours !== null && hours <= SLA_HOURS ? 'up' : 'down',
      sub: '从进来到分给销售',
      icon: Timer,
      bg: '#f3edff',
      color: '#8b5cf6',
      to: `/customer/clue?status=${encodeURIComponent(UNASSIGNED)}`,
      drill: '还没分配出去的线索'
    }
  ]
})

function prevMonth(key) {
  if (!key) return ''
  const [y, m] = key.split('-').map(Number)
  const d = new Date(y, m - 2, 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

/** 渠道：按固定顺序输出，未知渠道追加在后 */
const sources = computed(() => {
  const map = new Map()
  for (const r of rows.value) {
    const key = r.source || '未标注'
    if (!map.has(key)) map.set(key, { name: key, total: 0, converted: 0, wasted: 0, _hours: [] })
    const item = map.get(key)
    item.total += 1
    if (r.status === CONVERTED) item.converted += 1
    if (r.status === WASTED) item.wasted += 1
    if (r.assignTime && r.createTime) {
      const h = (new Date(r.assignTime.replace(/-/g, '/')) - new Date(r.createTime.replace(/-/g, '/'))) / 3600000
      if (h >= 0 && h < 24 * 30) item._hours.push(h)
    }
  }
  const order = Object.keys(SOURCE_COLORS)
  const list = [...map.values()].sort((a, b) => {
    const ai = order.indexOf(a.name)
    const bi = order.indexOf(b.name)
    return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi)
  })
  return list.map((s) => ({
    name: s.name,
    total: s.total,
    converted: s.converted,
    rate: s.total ? s.converted / s.total : 0,
    wasteRate: s.total ? s.wasted / s.total : 0,
    share: total.value ? s.total / total.value : 0,
    avgHours: s._hours.length ? s._hours.reduce((a, b) => a + b, 0) / s._hours.length : null,
    color: SOURCE_COLORS[s.name] || '#98a2b3'
  }))
})

/** 漏斗：按线索状态的实际顺序，未出现过的状态不占位 */
const funnel = computed(() => {
  const counts = new Map()
  for (const r of rows.value) {
    const key = r.status || UNASSIGNED
    counts.set(key, (counts.get(key) || 0) + 1)
  }
  const order = Object.keys(STATUS_COLORS)
  const keys = [...counts.keys()].sort((a, b) => {
    const ai = order.indexOf(a)
    const bi = order.indexOf(b)
    return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi)
  })
  const max = Math.max(1, ...counts.values())
  return keys.map((k) => {
    const count = counts.get(k)
    return {
      status: k,
      count,
      share: total.value ? count / total.value : 0,
      width: Math.max(6, (count / max) * 100),
      color: STATUS_COLORS[k] || '#98a2b3'
    }
  })
})

const trend = computed(() => {
  const end = currentMonth.value
  if (!end) return []
  const months = []
  const [y, m] = end.split('-').map(Number)
  for (let i = 5; i >= 0; i--) {
    const d = new Date(y, m - 1 - i, 1)
    months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return months.map((key) => {
    const list = rows.value.filter((r) => monthKey(r) === key)
    const conv = list.filter((r) => r.status === CONVERTED).length
    return {
      month: key,
      label: `${Number(key.slice(5))}月`,
      total: list.length,
      converted: conv,
      rate: list.length ? conv / list.length : 0
    }
  })
})

/** 柱状：左侧 40 是刻度，底边 186，最高 190 条线索留出顶部空隙 */
const maxLeads = computed(() => Math.max(4, ...trend.value.map((t) => t.total)))
function barX(i) {
  const span = 676 / Math.max(1, trend.value.length)
  return 40 + span * i + span / 2 - 17
}
function barY(v) {
  if (!v) return 186
  return 186 - (v / maxLeads.value) * 168
}
const gridLines = computed(() => {
  const max = maxLeads.value
  const step = Math.ceil(max / 3)
  return [0, 1, 2, 3].map((i) => ({ y: 186 - (168 / 3) * i, label: String(step * i) }))
})
const rateY = (rate) => 186 - rate * 168
/** 右轴刻度：0 / 50% / 100%，与折线共用比例尺 */
const rateTicks = [
  { v: 0, label: '0%' },
  { v: 0.5, label: '50%' },
  { v: 1, label: '100%' }
]
const ratePoints = computed(() =>
  trend.value.map((m, i) => `${barX(i) + 17},${rateY(m.rate)}`).join(' ')
)

/** 意向结构 */
const intents = computed(() => {
  const list = INTENT_ORDER.map((level) => {
    const group = rows.value.filter((r) => r.intent === level)
    const conv = group.filter((r) => r.status === CONVERTED).length
    return {
      level,
      count: group.length,
      converted: conv,
      rate: group.length ? conv / group.length : 0,
      share: total.value ? group.length / total.value : 0
    }
  })
  return list
})

/** 销售承接：只统计已经分出去的线索 */
const owners = computed(() => {
  const map = new Map()
  for (const r of rows.value) {
    if (!r.owner) continue
    if (!map.has(r.owner)) map.set(r.owner, { name: r.owner, total: 0, converted: 0 })
    const item = map.get(r.owner)
    item.total += 1
    if (r.status === CONVERTED) item.converted += 1
  }
  return [...map.values()]
    .map((o) => ({ ...o, rate: o.total ? o.converted / o.total : 0 }))
    .sort((a, b) => b.total - a.total || b.converted - a.converted)
    .slice(0, 6)
})

function pct(v) {
  return Math.round((Number(v) || 0) * 100) + '%'
}

/** 任何一块图/表点下去，都落到线索列表的对应筛选条件上 */
function toClue(params) {
  const q = new URLSearchParams(params).toString()
  router.push(q ? `/customer/clue?${q}` : '/customer/clue')
}
function rateClass(rate) {
  if (rate >= 0.4) return 'good'
  if (rate < 0.25) return 'danger'
  return ''
}

async function load() {
  loading.value = true
  try {
    const data = await request.get('/business/clues')
    rows.value = data || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  align-items: start;
}

.two-col.wide {
  grid-template-columns: 1.35fr 1fr;
}

@media (max-width: 1180px) {
  .two-col,
  .two-col.wide {
    grid-template-columns: 1fr;
  }
}

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

/* 能点的元素统一给手型和悬停反馈，别让用户猜 */
.kpi-card.clickable {
  cursor: pointer;
}

.kpi-card.clickable:hover {
  transform: translateY(-2px);
  border-color: #c9d8ff;
  box-shadow: 0 10px 24px rgba(47, 95, 224, 0.12);
}

.row-click {
  cursor: pointer;
}

.rank-table tr.row-click:hover td {
  background: #f8faff;
}

.funnel-row.row-click,
.intent-row.row-click {
  border-radius: 8px;
  transition: background 0.15s ease;
}

.funnel-row.row-click:hover,
.intent-row.row-click:hover {
  background: #f8faff;
}

.trend .bar-group {
  cursor: pointer;
}

/* 折线画在柱子之上：让折线的点/文字不要抢走点击，整列都归柱子 */
.trend .rate-line,
.trend .rate-dot,
.trend .rate-label {
  pointer-events: none;
}

.trend .bar-group:hover .bar {
  fill: #9dbcff;
}

.trend .bar-group:hover .bar-label {
  fill: #2f5fe0;
  font-weight: 600;
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
  min-height: 42px;
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

/* 表格 */
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

.rank-table .strong {
  font-weight: 600;
  color: #1f2b3d;
}

.rank-table .muted {
  color: #8b98ab;
}

.rank-table .good {
  color: #0f9d76;
  font-weight: 600;
}

.rank-table .danger {
  color: #d64545;
}

.rank-table .empty {
  text-align: center;
  color: #97a3b6;
  padding: 26px 0;
}

.rank-no {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 5px;
  background: #f2f5fb;
  color: #7c8aa0;
  font-size: 11px;
}

.w-share {
  width: 108px;
}

.w-no {
  width: 34px;
}

.src-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 7px;
  vertical-align: 1px;
}

.src-name {
  font-weight: 500;
}

.share-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 7px;
}

.share-bar {
  flex: none;
  width: 58px;
  height: 6px;
  border-radius: 999px;
  background: #eef1f7;
  overflow: hidden;
}

.share-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #5b8cff, #3b6cff);
}

.share-text {
  font-size: 11.5px;
  color: #5c6b82;
  font-variant-numeric: tabular-nums;
  min-width: 34px;
  text-align: right;
}

/* 漏斗 */
.funnel {
  display: flex;
  flex-direction: column;
  gap: 11px;
  padding: 6px 0 2px;
}

.funnel-row {
  display: grid;
  grid-template-columns: 52px 1fr 34px 40px;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;
}

.funnel-label {
  color: #5c6b82;
}

.funnel-track {
  height: 12px;
  border-radius: 999px;
  background: #f2f5fb;
  overflow: hidden;
}

.funnel-bar {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}

.funnel-value {
  text-align: right;
  font-weight: 600;
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

.funnel-pct {
  text-align: right;
  font-size: 11.5px;
  color: #97a3b6;
}

.funnel-foot {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #eef1f7;
}

.foot-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.foot-label {
  font-size: 11.5px;
  color: #97a3b6;
}

.foot-value {
  font-size: 15px;
  font-weight: 600;
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

.foot-value.warn {
  color: #f5a524;
}

/* 趋势图 */
.legend {
  display: flex;
  gap: 14px;
  font-size: 11.5px;
  color: #8b98ab;
  white-space: nowrap;
}

.legend i {
  display: inline-block;
  vertical-align: 1px;
  margin-right: 5px;
}

.lg-bar {
  width: 9px;
  height: 9px;
  border-radius: 3px;
  background: linear-gradient(180deg, #5b8cff, #9dbcff);
}

.lg-line {
  width: 14px;
  height: 0;
  border-top: 2px dashed #f5a524;
}

.trend {
  width: 100%;
  height: 210px;
}

.trend .grid line {
  stroke: #eef1f7;
  stroke-width: 1;
}

.trend .grid text {
  font-size: 10px;
  fill: #a3aec0;
}

.trend .grid text.axis-right {
  fill: #d9a33a;
}

.trend .bar {
  fill: #cfdcff;
}

.trend .bar-label {
  font-size: 10.5px;
  fill: #5c6b82;
}

.trend .x-label {
  font-size: 11px;
  fill: #8b98ab;
}

.trend .rate-line {
  fill: none;
  stroke: #f5a524;
  stroke-width: 2;
  stroke-linejoin: round;
}

.trend .rate-dot {
  fill: #fff;
  stroke: #f5a524;
  stroke-width: 2;
}

.trend .rate-label {
  font-size: 10.5px;
  fill: #d2870f;
}

/* 意向结构 */
.intent-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 0 4px;
}

.intent-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.intent-tag {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 600;
}

.intent-tag.lv-高 {
  background: #e8f8f2;
  color: #0f9d76;
}

.intent-tag.lv-中 {
  background: #eef3ff;
  color: #3b6cff;
}

.intent-tag.lv-低 {
  background: #f4f6fa;
  color: #8b98ab;
}

.intent-main {
  flex: 1;
}

.intent-line {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: #33415a;
  margin-bottom: 6px;
}

.intent-amount {
  font-size: 11.5px;
  color: #97a3b6;
}

.intent-bar {
  height: 7px;
  border-radius: 999px;
  background: #f2f5fb;
  overflow: hidden;
}

.intent-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #5b8cff, #3b6cff);
}
</style>
