<template>
  <div class="service-desk">
    <!-- 售后看的是「响应速度」和「处理结果」，金额不是主角 -->
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
      <!-- 工单队列：按等级与等待时长排，高等级在最前 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>工单队列</h3>
            <p>高等级优先、等得久的优先；点一行进该客户的工单列表跟进处理</p>
          </div>
          <button class="dk-link" @click="router.push('/service/ticket')">
            全部工单<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="queue.length" class="dk-table">
          <thead>
            <tr>
              <th>工单</th>
              <th>客户 / 问题</th>
              <th class="w-level">等级</th>
              <th class="w-state">状态</th>
              <th class="w-wait num">已等待</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="t in queue"
              :key="t.id"
              class="click"
              :title="`查看「${t.customer}」的工单`"
              @click="router.push(`/service/ticket?customer=${encodeURIComponent(t.customer)}`)"
            >
              <td class="mono muted">{{ t.ticketNo }}</td>
              <td>
                <div class="cell-main">{{ t.customer }}</div>
                <div class="cell-sub">{{ t.product }} · {{ t.issue }}</div>
              </td>
              <td><span class="dk-tag" :class="levelTone(t.level)">{{ t.level }}</span></td>
              <td><span class="dk-tag" :class="statusTone(t.status)">{{ t.status }}</span></td>
              <td class="num" :class="{ danger: waited(t) > 24 }">{{ waitedText(t) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">工单都处理完了，没有待办结的售后问题</div>
      </section>

      <!-- 今日回访：售后闭环的最后一步 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>回访与满意度</h3>
            <p>回访是复购的前置动作，不满意的客户要当天回电</p>
          </div>
          <button class="dk-link" @click="router.push('/service/revisit')">
            回访记录<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <ul class="dk-list">
          <li
            v-for="r in recentRevisits"
            :key="r.id"
            class="dk-row click"
            :title="`查看「${r.customer}」的回访记录`"
            @click="router.push('/service/revisit')"
          >
            <span class="dk-avatar">{{ (r.contact || r.customer).slice(0, 1) }}</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ r.customer }}</span>
                <span class="dk-tag" :class="scoreTone(r.score)">{{ r.score }}</span>
              </div>
              <p class="dk-row-sub">{{ r.content }}</p>
            </div>
            <span class="dk-row-side">{{ (r.time || '').slice(5, 10) }}</span>
          </li>
        </ul>
      </section>
    </div>

    <div class="dk-two-col even">
      <!-- 退换货：售后要盯的执行动作 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>退换货处理</h3>
            <p>待审核的先看证据，已受理的盯补发与退款</p>
          </div>
          <button class="dk-link" @click="router.push('/service/return')">
            退换货单<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <ul v-if="openReturns.length" class="dk-list">
          <li
            v-for="r in openReturns"
            :key="r.id"
            class="dk-row click"
            :title="`查看退换货单 ${r.returnNo}`"
            @click="router.push('/service/return')"
          >
            <span class="dk-tag" :class="r.type === '退货' ? 'danger' : 'warn'">{{ r.type }}</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ r.customer }}</span>
                <span class="dk-row-side">{{ money(r.amount) }}</span>
              </div>
              <p class="dk-row-sub">{{ r.reason }}</p>
            </div>
            <span class="dk-row-side">{{ r.status }}</span>
          </li>
        </ul>
        <div v-else class="dk-empty">没有待处理的退换货</div>
      </section>

      <!-- 服务质量：用回访结果反推 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>服务质量</h3>
            <p>按回访评分统计，不满意项要逐条闭环</p>
          </div>
          <span class="dk-badge">近 {{ revisits.length }} 次回访</span>
        </header>

        <div class="score-grid">
          <div v-for="s in scores" :key="s.label" class="score-card">
            <span class="score-value" :style="{ color: s.color }">{{ s.count }}</span>
            <span class="score-label">{{ s.label }}</span>
            <div class="dk-bar"><span :style="{ width: s.share * 100 + '%', background: s.color }"></span></div>
          </div>
        </div>

        <ul class="dk-list follow-ups">
          <li
            v-for="f in needFollow"
            :key="f.id"
            class="dk-row click"
            @click="router.push(`/service/ticket?customer=${encodeURIComponent(f.customer)}`)"
          >
            <span class="dk-tag danger">待跟进</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ f.customer }}</span>
              </div>
              <p class="dk-row-sub">{{ f.next }}</p>
            </div>
          </li>
          <li v-if="!needFollow.length" class="dk-empty" style="padding: 16px 0">没有需要升级的客诉</li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, CircleCheck, Service, Star, Tickets } from '@element-plus/icons-vue'
import { tickets, revisits, returnOrders } from '@/mock/data'
import { money } from '@/utils/analytics'

defineProps({
  profile: { type: Object, required: true }
})

const router = useRouter()

const now = Date.now()
const hoursSince = (time) => {
  if (!time) return 0
  const t = new Date(String(time).replace(/-/g, '/')).getTime()
  return Math.max(0, (now - t) / 3600000)
}
const waited = (t) => hoursSince(t.createTime)
const waitedText = (t) => {
  const h = waited(t)
  if (h < 1) return '不到 1h'
  if (h < 48) return Math.round(h) + 'h'
  return Math.round(h / 24) + ' 天'
}

const OPEN = ['待受理', '处理中']

/** 未办结工单：等级高的、等得久的排前面 */
const openTickets = computed(() => tickets.filter((t) => OPEN.includes(t.status)))
const queue = computed(() => {
  const order = { 高: 0, 中: 1, 低: 2 }
  return openTickets.value
    .slice()
    .sort((a, b) => (order[a.level] ?? 1) - (order[b.level] ?? 1) || waited(b) - waited(a))
})

const pendingAccept = computed(() => tickets.filter((t) => t.status === '待受理').length)
const processing = computed(() => tickets.filter((t) => t.status === '处理中').length)
const solved = computed(() => tickets.filter((t) => ['已解决', '已关闭'].includes(t.status)).length)
const solveRate = computed(() => (tickets.length ? solved.value / tickets.length : 0))

const recentRevisits = computed(() =>
  revisits.slice().sort((a, b) => String(b.time).localeCompare(String(a.time))).slice(0, 4)
)

const scores = computed(() => {
  const total = revisits.length || 1
  const cfg = [
    { label: '满意', color: '#0f9d76', match: '满意' },
    { label: '一般', color: '#f5a524', match: '一般' },
    { label: '不满意', color: '#c0392b', match: '不满意' }
  ]
  return cfg.map((c) => {
    const count = revisits.filter((r) => r.score === c.match).length
    return { ...c, count, share: count / total }
  })
})

const needFollow = computed(() =>
  revisits.filter((r) => r.score === '不满意' || (r.next || '').includes('跟进')).slice(0, 3)
)

const openReturns = computed(() => returnOrders.filter((r) => ['待审核', '已受理'].includes(r.status)))

const kpis = computed(() => [
  {
    label: '待受理工单',
    value: pendingAccept.value,
    delta: pendingAccept.value ? '需当天响应' : '无积压',
    trend: pendingAccept.value ? 'warn' : 'up',
    sub: `SLA 24 小时内受理`,
    icon: Tickets,
    bg: '#fdf4e3',
    color: '#b45309',
    to: '/service/ticket',
    drill: '售后工单',
    alarm: pendingAccept.value > 0
  },
  {
    label: '处理中',
    value: processing.value,
    delta: `共 ${tickets.length} 张工单`,
    trend: 'info',
    sub: `已解决 ${solved.value} 张`,
    icon: Service,
    bg: '#eef3ff',
    color: '#3b6cff',
    to: '/service/ticket',
    drill: '售后工单'
  },
  {
    label: '工单解决率',
    value: Math.round(solveRate.value * 100) + '%',
    delta: solveRate.value >= 0.6 ? '高于基准' : '低于 60%',
    trend: solveRate.value >= 0.6 ? 'up' : 'down',
    sub: '已解决 + 已关闭',
    icon: CircleCheck,
    bg: '#e8f8f2',
    color: '#0f9d76',
    to: '/service/ticket',
    drill: '售后工单'
  },
  {
    label: '回访满意度',
    value: scores.value[0].count + ' / ' + revisits.length,
    delta: needFollow.value.length ? `${needFollow.value.length} 条待跟进` : '全部闭环',
    trend: needFollow.value.length ? 'warn' : 'up',
    sub: '满意回访 / 全部回访',
    icon: Star,
    bg: '#f3edff',
    color: '#8b5cf6',
    to: '/service/revisit',
    drill: '客户回访'
  }
])

const levelTone = (l) => (l === '高' ? 'danger' : l === '中' ? 'warn' : 'info')
const statusTone = (s) => (s === '待受理' ? 'warn' : s === '处理中' ? 'info' : 'ok')
const scoreTone = (s) => (s === '满意' ? 'ok' : s === '一般' ? 'warn' : 'danger')
</script>

<style scoped>
.w-level {
  width: 66px;
}

.w-state {
  width: 82px;
}

.w-wait {
  width: 72px;
}

.cell-main {
  font-size: 12.5px;
  font-weight: 500;
  color: #1f2b3d;
}

.cell-sub {
  font-size: 11.5px;
  color: #8b98ab;
  margin-top: 2px;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 4px 0 12px;
}

.score-card {
  border: 1px solid #eef1f7;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.score-value {
  font-size: 20px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.score-label {
  font-size: 12px;
  color: #8b98ab;
}

.follow-ups {
  border-top: 1px solid #f2f5fa;
  padding-top: 4px;
}
</style>
