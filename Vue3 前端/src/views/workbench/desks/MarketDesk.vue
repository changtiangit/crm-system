<template>
  <div class="market-desk">
    <!-- 营销的四张卡：进来多少、压了多少、转了多少、分得快不快 -->
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
      <!-- 待分配线索池：营销每天最先要清的账 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>待分配线索池</h3>
            <p>按意向从高到低排，分出去就进入销售的跟进队列；高意向线索越晚分配，越容易凉</p>
          </div>
          <button class="dk-link" @click="toClue({ status: UNASSIGNED })">
            全部 {{ unassigned.length }} 条<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="poolPreview.length" class="dk-table">
          <thead>
            <tr>
              <th>线索</th>
              <th class="w-src">来源</th>
              <th class="w-intent">意向</th>
              <th class="w-time">进入时间</th>
              <th class="w-wait num">已等待</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="c in poolPreview"
              :key="c.id"
              class="click"
              :title="`查看「${c.name}」这条线索`"
              @click="toClue({ q: c.name })"
            >
              <td>
                <div class="cell-main">{{ c.name }}</div>
                <div class="cell-sub">{{ c.contact }} · {{ c.phone }}</div>
              </td>
              <td class="muted">{{ c.source }}</td>
              <td><span class="dk-tag" :class="intentTone(c.intent)">{{ c.intent || '中' }}意向</span></td>
              <td class="muted mono">{{ (c.createTime || '').slice(5, 16) }}</td>
              <td class="num" :class="{ danger: waitHours(c) > SLA }">{{ waitText(c) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">线索池已清空，全部线索都已分到销售手上</div>
      </section>

      <!-- 渠道效果：营销的「产品」质量看板 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>渠道效果</h3>
            <p>哪个渠道进来的线索又多又准</p>
          </div>
          <button class="dk-link" @click="router.push('/stats/market')">
            市场分析<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <ul class="dk-list">
          <li
            v-for="s in sources"
            :key="s.name"
            class="dk-row click"
            :title="`查看「${s.name}」进来的线索`"
            @click="toClue({ source: s.name })"
          >
            <span class="src-dot" :style="{ background: s.color }"></span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ s.name }}</span>
                <span class="dk-row-side">{{ s.total }} 条</span>
              </div>
              <div class="dk-bar"><span :style="{ width: s.share * 100 + '%', background: s.color }"></span></div>
            </div>
            <span class="dk-row-side rate" :class="rateTone(s.rate)">{{ pct(s.rate) }}</span>
          </li>
        </ul>
        <p class="foot-note">右侧百分比为该渠道线索的成交转化率</p>
      </section>
    </div>

    <div class="dk-two-col even">
      <!-- 已分配待承接：分出去了但销售还没动，需要营销去 push -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>已分配待承接</h3>
            <p>分给销售 48 小时还没开始跟的线索，建议直接找组长对齐</p>
          </div>
          <span class="dk-badge">{{ staleAssigned.length }} 条待催</span>
        </header>

        <ul v-if="staleAssigned.length" class="dk-list">
          <li
            v-for="c in staleAssigned"
            :key="c.id"
            class="dk-row click"
            :title="`查看「${c.name}」这条线索`"
            @click="toClue({ status: '待跟进' })"
          >
            <span class="dk-avatar">{{ (c.owner || '?').slice(0, 1) }}</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ c.name }}</span>
                <span class="dk-tag warn">待跟进</span>
              </div>
              <p class="dk-row-sub">{{ c.owner }} 承接 · {{ c.source }} · {{ (c.createTime || '').slice(5, 10) }} 进入</p>
            </div>
            <span class="dk-row-side">{{ waitText(c) }}</span>
          </li>
        </ul>
        <div v-else class="dk-empty">没有积压：分出去的线索销售都已开始跟进</div>
      </section>

      <!-- 意向结构：决定分配优先级 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>意向结构</h3>
            <p>高意向线索优先分给能力等级高的销售</p>
          </div>
          <span class="dk-badge">共 {{ total }} 条</span>
        </header>

        <ul class="dk-list">
          <li
            v-for="i in intents"
            :key="i.level"
            class="dk-row click"
            :title="`查看「${i.level}意向」的线索`"
            @click="toClue({ intent: i.level })"
          >
            <span class="dk-tag" :class="intentTone(i.level)">{{ i.level }}意向</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ i.count }} 条</span>
                <span class="dk-row-side">已转化 {{ i.converted }}</span>
              </div>
              <div class="dk-bar"><span :style="{ width: i.share * 100 + '%' }"></span></div>
            </div>
            <span class="dk-row-side">{{ pct(i.share) }}</span>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Promotion, Timer, TrendCharts, User } from '@element-plus/icons-vue'
import request from '@/utils/request'

defineProps({
  profile: { type: Object, required: true }
})

const router = useRouter()

/** 线索是营销唯一的业务数据源，直接取列表接口（后端已按数据范围过滤） */
const rows = ref([])

const UNASSIGNED = '待分配'
const CONVERTED = '已转化'
const WASTED = '已废弃'
/** 线索从进来到分给销售的目标时效（小时） */
const SLA = 8

const SOURCE_COLORS = {
  官网表单: '#3b6cff',
  展会: '#12a594',
  广告投放: '#8b5cf6',
  老客户推荐: '#f5a524',
  电话咨询: '#0ea5e9'
}

const now = Date.now()
const hoursSince = (time) => {
  if (!time) return 0
  const t = new Date(String(time).replace(/-/g, '/')).getTime()
  return Math.max(0, (now - t) / 3600000)
}
const waitHours = (c) => hoursSince(c.createTime)
const waitText = (c) => {
  const h = waitHours(c)
  if (h < 1) return '不到 1h'
  if (h < 48) return Math.round(h) + 'h'
  return Math.round(h / 24) + ' 天'
}

const total = computed(() => rows.value.length)
const unassigned = computed(() => rows.value.filter((c) => !c.owner || c.status === UNASSIGNED))
const converted = computed(() => rows.value.filter((c) => c.status === CONVERTED).length)
const wasted = computed(() => rows.value.filter((c) => c.status === WASTED).length)
const conversionRate = computed(() => (total.value ? converted.value / total.value : 0))

/** 待分配池按「意向高 → 等待久」排序，前 6 条 */
const poolPreview = computed(() => {
  const order = { 高: 0, 中: 1, 低: 2 }
  return unassigned.value
    .slice()
    .sort((a, b) => (order[a.intent] ?? 1) - (order[b.intent] ?? 1) || waitHours(b) - waitHours(a))
    .slice(0, 6)
})

/** 已分配但仍是「待跟进」且超过 48 小时的，算销售没接住 */
const staleAssigned = computed(() =>
  rows.value
    .filter((c) => c.owner && c.status === '待跟进' && waitHours(c) > 48)
    .sort((a, b) => waitHours(b) - waitHours(a))
    .slice(0, 5)
)

const avgAssignHours = computed(() => {
  const list = rows.value
    .filter((c) => c.assignTime && c.createTime)
    .map((c) => (new Date(String(c.assignTime).replace(/-/g, '/')) - new Date(String(c.createTime).replace(/-/g, '/'))) / 3600000)
    .filter((h) => h >= 0 && h < 24 * 30)
  if (!list.length) return null
  return list.reduce((a, b) => a + b, 0) / list.length
})

const kpis = computed(() => [
  {
    label: '待分配线索',
    value: unassigned.value.length,
    delta: unassigned.value.length ? '今日需清空' : '已全部分配',
    trend: unassigned.value.length ? 'warn' : 'up',
    sub: `占全部 ${pct(total.value ? unassigned.value.length / total.value : 0)}`,
    icon: User,
    bg: '#fff5e6',
    color: '#f5a524',
    to: `/customer/clue?status=${encodeURIComponent(UNASSIGNED)}`,
    drill: '待分配线索',
    alarm: unassigned.value.length > 0
  },
  {
    label: '线索总量',
    value: total.value,
    delta: `已转化 ${converted.value}`,
    trend: 'info',
    sub: `已废弃 ${wasted.value} 条`,
    icon: Promotion,
    bg: '#eef3ff',
    color: '#3b6cff',
    to: '/customer/clue',
    drill: '全部线索'
  },
  {
    label: '线索转化率',
    value: pct(conversionRate.value),
    delta: conversionRate.value >= 0.3 ? '高于基准' : '低于 30%',
    trend: conversionRate.value >= 0.3 ? 'up' : 'down',
    sub: '成交 / 全部线索',
    icon: TrendCharts,
    bg: '#e8f8f2',
    color: '#12a594',
    to: `/customer/clue?status=${encodeURIComponent(CONVERTED)}`,
    drill: '已转化线索'
  },
  {
    label: '平均分配时长',
    value: avgAssignHours.value === null ? '—' : avgAssignHours.value.toFixed(1) + ' h',
    delta: avgAssignHours.value === null ? '暂无分配' : avgAssignHours.value <= SLA ? `≤ ${SLA}h 达标` : `超 ${SLA}h`,
    trend: avgAssignHours.value !== null && avgAssignHours.value <= SLA ? 'up' : 'warn',
    sub: '从进来到分给销售',
    icon: Timer,
    bg: '#f3edff',
    color: '#8b5cf6',
    to: `/customer/clue?status=${encodeURIComponent(UNASSIGNED)}`,
    drill: '还没分配出去的线索'
  }
])

const sources = computed(() => {
  const map = new Map()
  for (const c of rows.value) {
    const key = c.source || '未标注'
    if (!map.has(key)) map.set(key, { name: key, total: 0, converted: 0 })
    const item = map.get(key)
    item.total += 1
    if (c.status === CONVERTED) item.converted += 1
  }
  const order = Object.keys(SOURCE_COLORS)
  return [...map.values()]
    .sort((a, b) => b.total - a.total || (order.indexOf(a.name) - order.indexOf(b.name)))
    .slice(0, 5)
    .map((s) => ({
      ...s,
      share: total.value ? s.total / total.value : 0,
      rate: s.total ? s.converted / s.total : 0,
      color: SOURCE_COLORS[s.name] || '#98a2b3'
    }))
})

const intents = computed(() =>
  ['高', '中', '低'].map((level) => {
    const group = rows.value.filter((c) => (c.intent || '中') === level)
    return {
      level,
      count: group.length,
      converted: group.filter((c) => c.status === CONVERTED).length,
      share: total.value ? group.length / total.value : 0
    }
  })
)

const pct = (v) => Math.round((Number(v) || 0) * 100) + '%'
const intentTone = (i) => (i === '高' ? 'danger' : i === '低' ? 'info' : 'warn')
const rateTone = (r) => (r >= 0.4 ? 'dk-ok' : r < 0.25 ? 'dk-danger' : '')

/** 点任何一条线索/渠道，都落到线索列表的对应筛选上 */
function toClue(params) {
  const q = new URLSearchParams(params).toString()
  router.push(q ? `/customer/clue?${q}` : '/customer/clue')
}

onMounted(async () => {
  rows.value = (await request.get('/business/clues')) || []
})
</script>

<style scoped>
.src-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.rate {
  min-width: 44px;
  text-align: right;
  font-weight: 600;
  color: #41506a;
}

.cell-main {
  font-size: 12.5px;
  font-weight: 500;
  color: #1f2b3d;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-sub {
  font-size: 11.5px;
  color: #8b98ab;
  margin-top: 2px;
}

.w-src {
  width: 92px;
}

.w-intent {
  width: 78px;
}

.w-time {
  width: 92px;
}

.w-wait {
  width: 62px;
}

.foot-note {
  margin: 10px 0 0;
  font-size: 11.5px;
  color: #a7b2c4;
}
</style>
