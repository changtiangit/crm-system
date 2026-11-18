<template>
  <div class="page hub">
    <div v-if="!profile" class="page-card empty-box">
      <el-icon :size="30" color="#c3cddd"><Warning /></el-icon>
      <p>没有找到这家客户的档案，可能已被删除或名称有变化</p>
      <el-button type="primary" plain @click="router.push('/customer/list')">返回客户档案</el-button>
    </div>

    <!-- 行级权限：不是自己负责（或本组）的客户，直接拦住，避免用链接绕过列表 -->
    <div v-else-if="!allowed" class="page-card empty-box">
      <el-icon :size="30" color="#e6a23c"><Lock /></el-icon>
      <p>无权查看这家客户：档案由「{{ profile.base.owner }}」负责</p>
      <p class="empty-sub">你的数据范围是「{{ scopeLabel }}」，只能查看自己负责的客户</p>
      <el-button type="primary" plain @click="router.push('/customer/list')">返回我的客户</el-button>
      <el-button v-if="has('pool:claim')" plain @click="router.push('/customer/pool')">去客户公海领取</el-button>
    </div>

    <template v-else>
      <!-- 客户抬头：一眼看清是谁、归谁、怎么联系 -->
      <div class="hero page-card">
        <div class="hero-left">
          <div class="avatar">{{ profile.base.name.slice(0, 1) }}</div>
          <div class="hero-main">
            <div class="hero-title-line">
              <h2 class="hero-title">{{ profile.base.name }}</h2>
              <el-tag size="small" round effect="light" :type="levelType">{{ profile.base.level }} 级客户</el-tag>
              <el-tag size="small" round effect="plain" type="info">{{ profile.base.industry }}</el-tag>
            </div>
            <p class="hero-desc">
              负责人 {{ profile.base.owner }} · 主联系人 {{ profile.base.contact || '—' }}
              <a class="phone" :href="'tel:' + profile.base.phone">{{ profile.base.phone }}</a>
              · 建档 {{ profile.base.createTime }}
            </p>
          </div>
        </div>
        <div class="hero-actions">
          <el-button :icon="Back" @click="router.back()">返回</el-button>
          <el-button
            :icon="ChatLineSquare"
            :disabled="!has('followup:view')"
            @click="go('/customer/followup', 'followup:view')"
          >
            写跟进
          </el-button>
          <el-button
            type="primary"
            :icon="Document"
            :disabled="!has('order:view')"
            @click="go('/sales/order', 'order:view')"
          >
            销售订单
          </el-button>
        </div>
      </div>

      <!-- 关键指标 -->
      <div class="kpi-grid">
        <div
          v-for="k in kpis"
          :key="k.label"
          class="kpi-card page-card"
          :class="{ clickable: !!k.to }"
          :title="k.to ? `查看${k.drill}` : ''"
          @click="k.to && go(k.to, k.perm)"
        >
          <span class="kpi-label">{{ k.label }}</span>
          <div class="kpi-value">{{ k.value }}</div>
          <div class="kpi-sub">{{ k.sub }}</div>
        </div>
      </div>

      <div class="hub-grid">
        <div class="col-main">
          <!-- 最近跟进 -->
          <section class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>最近跟进</h3>
                <p>电话、上门、微信都记在这里，下一步安排一眼可见</p>
              </div>
              <el-button link type="primary" @click="go('/customer/followup', 'followup:view')">
                全部 {{ profile.followups.length }} 条<el-icon><ArrowRight /></el-icon>
              </el-button>
            </header>
            <ul v-if="profile.followups.length" class="follow-list">
              <li
                v-for="f in profile.followups.slice(0, 4)"
                :key="f.id"
                class="follow-row row-link"
                title="去跟进记录看这家客户的完整沟通历史"
                @click="go('/customer/followup', 'followup:view')"
              >
                <span class="follow-dot" :class="'t-' + f.type"></span>
                <div class="follow-main">
                  <div class="follow-line">
                    <span class="follow-name">{{ f.contact }}</span>
                    <el-tag size="small" round effect="plain">{{ f.type }}</el-tag>
                    <span class="follow-time">{{ f.time.slice(5, 16) }}</span>
                    <span class="follow-owner">{{ f.owner }}</span>
                  </div>
                  <p class="follow-content">{{ f.content }}</p>
                  <p v-if="f.next" class="follow-next">下一步：{{ f.next }}</p>
                </div>
              </li>
            </ul>
            <div v-else class="mini-empty">还没有跟进记录，建议先电话确认一次需求</div>
          </section>

          <!-- 订单与回款 -->
          <section class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>订单与回款</h3>
                <p>这家客户签过的单，点单号可看完整合同清单</p>
              </div>
              <el-button link type="primary" @click="go('/sales/order', 'order:view')">
                全部 {{ profile.orders.length }} 张<el-icon><ArrowRight /></el-icon>
              </el-button>
            </header>
            <table v-if="profile.orders.length" class="mini-table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>下单日期</th>
                  <th class="num">金额</th>
                  <th v-if="has('receivable:view')" class="num">回款</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in profile.orders.slice(0, 6)" :key="o.id">
                  <td>
                    <span class="link" @click="goOrder(o.orderNo)">{{ o.orderNo }}</span>
                  </td>
                  <td class="muted">{{ o.orderDate }}</td>
                  <td class="num strong">{{ money(o.amount) }}</td>
                  <td
                    v-if="has('receivable:view')"
                    class="num"
                    :class="paidOf(o) >= o.amount ? 'ok' : paidOf(o) > 0 ? 'warn' : 'muted'"
                  >
                    {{ money(paidOf(o)) }}
                  </td>
                  <td>
                    <el-tag size="small" round effect="light" :type="statusType[o.status] || 'info'">
                      {{ o.status }}
                    </el-tag>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-else class="mini-empty">还没有成交订单，可先看商机与报价推进情况</div>
          </section>

          <!-- 销售进程：商机 + 报价 -->
          <div class="two-col">
            <section class="page-card panel">
              <header class="panel-head">
                <div>
                  <h3>在谈商机</h3>
                  <p>推进阶段与预计成交</p>
                </div>
                <el-button v-if="has('opportunity:view')" link type="primary" @click="go('/sales/opportunity', 'opportunity:view')">全部</el-button>
              </header>
              <ul v-if="profile.opportunities.length" class="row-list">
                <li
                  v-for="o in profile.opportunities.slice(0, 4)"
                  :key="o.id"
                  class="row-item row-link"
                  title="去销售商机看这条商机的推进记录"
                  @click="go('/sales/opportunity', 'opportunity:view')"
                >
                  <div class="row-main">
                    <span class="row-name">{{ o.name }}</span>
                    <span class="row-sub">{{ o.stage }} · 预计 {{ o.expectDate }}</span>
                  </div>
                  <span class="row-amount">{{ money(o.amount) }}</span>
                </li>
              </ul>
              <div v-else class="mini-empty">暂无商机</div>
            </section>

            <section class="page-card panel">
              <header class="panel-head">
                <div>
                  <h3>报价单</h3>
                  <p>有效期与审批状态</p>
                </div>
                <el-button v-if="has('quote:view')" link type="primary" @click="go('/sales/quote', 'quote:view')">全部</el-button>
              </header>
              <ul v-if="profile.quotes.length" class="row-list">
                <li
                  v-for="q in profile.quotes.slice(0, 4)"
                  :key="q.id"
                  class="row-item row-link"
                  title="去报价单看审批与有效期"
                  @click="go('/sales/quote', 'quote:view')"
                >
                  <div class="row-main">
                    <span class="row-name">{{ q.quoteNo }}</span>
                    <span class="row-sub">{{ q.quoteDate }} 至 {{ q.validTo }}</span>
                  </div>
                  <div class="row-right">
                    <span class="row-amount">{{ money(q.amount) }}</span>
                    <el-tag size="small" round effect="plain" :type="quoteType[q.status] || 'info'">{{ q.status }}</el-tag>
                  </div>
                </li>
              </ul>
              <div v-else class="mini-empty">暂无报价单</div>
            </section>
          </div>
        </div>

        <div class="col-side">
          <!-- 联系人 -->
          <section class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>联系人</h3>
                <p>签字、收货、对账的具体人</p>
              </div>
              <el-button v-if="has('contact:view')" link type="primary" @click="go('/customer/contact', 'contact:view')">全部</el-button>
            </header>
            <ul v-if="profile.contacts.length" class="contact-list">
              <li
                v-for="c in profile.contacts"
                :key="c.id"
                class="contact-row row-link"
                title="去联系人模块维护这位对接人"
                @click="go('/customer/contact', 'contact:view')"
              >
                <span class="contact-avatar">{{ c.name.slice(0, 1) }}</span>
                <div class="contact-main">
                  <span class="contact-name">
                    {{ c.name }}
                    <el-tag v-if="c.primary === '是'" size="small" effect="light" round>主</el-tag>
                  </span>
                  <span class="contact-sub">{{ c.title }} · {{ c.phone }}</span>
                </div>
                <a class="call" :href="'tel:' + c.phone">拨打</a>
              </li>
            </ul>
            <div v-else class="mini-empty">还没有维护联系人</div>
          </section>

          <!-- 应收：属于交付与回款模块，没有该权限的岗位不展示明细 -->
          <section v-if="has('receivable:view')" class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>应收账款</h3>
                <p>未收金额与约定回款日</p>
              </div>
              <el-button v-if="has('receivable:view')" link type="primary" @click="go('/delivery/receivable', 'receivable:view')">全部</el-button>
            </header>
            <div v-if="profile.receivables.length" class="recv-list">
              <div v-for="r in profile.receivables" :key="r.id" class="recv-row">
                <div class="recv-main">
                  <span class="recv-order">{{ r.orderNo }}</span>
                  <span class="recv-due" :class="{ danger: r.total - r.received > 0 && r.dueDate < today }">
                    {{ r.dueDate }} 到期
                  </span>
                </div>
                <span class="recv-amount" :class="{ danger: r.total - r.received > 0 }">
                  {{ money(Math.max(0, r.total - r.received)) }}
                </span>
              </div>
            </div>
            <div v-else class="mini-empty">没有应收款</div>
          </section>

          <!-- 售后 -->
          <section class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>售后与回访</h3>
                <p>工单处理与满意度</p>
              </div>
              <el-button v-if="has('ticket:view')" link type="primary" @click="go('/service/ticket', 'ticket:view')">工单</el-button>
            </header>
            <ul v-if="profile.tickets.length" class="row-list">
              <li
                v-for="t in profile.tickets.slice(0, 3)"
                :key="t.id"
                class="row-item row-link"
                title="去售后工单看处理进度"
                @click="go('/service/ticket', 'ticket:view')"
              >
                <div class="row-main">
                  <span class="row-name">{{ t.product }}</span>
                  <span class="row-sub">{{ t.issue }} · {{ t.createTime.slice(5, 10) }}</span>
                </div>
                <el-tag size="small" round effect="light" :type="ticketType[t.status] || 'info'">{{ t.status }}</el-tag>
              </li>
            </ul>
            <div v-else class="mini-empty">没有售后工单，服务质量良好</div>

            <div v-if="profile.revisits.length" class="revisit-strip">
              <span class="revisit-label">最近回访</span>
              <span class="revisit-text">
                {{ profile.revisits[0].time.slice(0, 10) }} · {{ profile.revisits[0].score }} ·
                {{ profile.revisits[0].content }}
              </span>
            </div>
          </section>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Document, ChatLineSquare, ArrowRight, Warning, Lock } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { orderPath } from '@/utils/links'
import { customerProfile, money } from '@/utils/analytics'

const route = useRoute()
const router = useRouter()
const { has } = usePermission()
const { canSee, label: scopeLabel } = useDataScope()

const today = new Date().toISOString().slice(0, 10)

// 路径参数优先（正常进档案），name 参数兜底（公海刚领取、临时客户）
const profile = computed(
  () => customerProfile(route.params.id) || customerProfile(route.query.name)
)

// 越权拦截：列表已经过滤，这里再挡一次直接输 URL / 从别处跳进来的情况
const allowed = computed(() => !profile.value || canSee(profile.value.base.owner))

const levelType = computed(() => ({ A: 'primary', B: 'success', C: 'info' }[profile.value?.base.level] || 'info'))
const statusType = {
  待付款: 'warning',
  部分回款: 'primary',
  已付款: 'success',
  已完成: 'success',
  已取消: 'info'
}
const quoteType = { 草稿: 'info', 待审批: 'warning', 已通过: 'success', 已转订单: 'primary', 已失效: 'info' }
const ticketType = { 待受理: 'warning', 处理中: 'primary', 已解决: 'success', 已关闭: 'info' }

const kpis = computed(() => {
  if (!profile.value) return []
  const k = profile.value.kpi
  // 每张卡都是入口：点哪张就去哪个模块，且已按这家客户筛好
  const rows = [
    {
      label: '累计成交额',
      value: money(k.amount),
      sub: `含 ${k.orderCount} 张订单 · ${k.qty} 件商品`,
      to: '/sales/order',
      perm: 'order:view',
      drill: '这家客户的订单'
    },
    {
      label: '平均客单价',
      value: money(k.avgOrder),
      // 毛利是成本数据，销售看不到，换成订单件数这类中性口径
      sub: has('order:profit:view') ? `毛利 ${money(k.profit)}` : `累计 ${k.qty} 件商品`,
      to: '/sales/order',
      perm: 'order:view',
      drill: '订单清单'
    }
  ]
  // 回款属于「交付与回款」模块，没有该权限的岗位（销售）只看成交与商机
  if (has('receivable:view')) {
    rows.push({
      label: '未回款',
      value: money(k.unreceived),
      sub: k.unreceived ? '需按约定回款日催收' : '货款已结清',
      to: '/delivery/receivable',
      perm: 'receivable:view',
      drill: '这家客户的应收账款'
    })
  } else {
    rows.push({
      label: '最近下单',
      value: k.lastOrder,
      sub: `共 ${k.orderCount} 张有效订单`,
      to: '/sales/order',
      perm: 'order:view',
      drill: '订单清单'
    })
  }
  rows.push({
    label: '在谈商机 / 未结工单',
    value: `${money(k.openOpportunity)} / ${k.openTickets}`,
    sub: '按当前可见范围统计',
    to: '/sales/opportunity',
    perm: 'opportunity:view',
    drill: '这家客户的商机'
  })
  return rows
})

/** 某张订单已收金额：从应收里反查，没有应收记录视为已收清 */
function paidOf(order) {
  const r = profile.value.receivables.find((x) => x.orderNo === order.orderNo)
  return r ? r.received : order.status === '已取消' ? 0 : order.amount
}

/** 点单号直接看这一单的合同清单 */
function goOrder(orderNo) {
  if (!has('order:view')) {
    ElMessage.info('当前账号没有销售订单的访问权限，可联系管理员开通')
    return
  }
  router.push(orderPath(orderNo))
}

/** 各卡片统一按「当前客户」筛到对应模块；没有该模块权限时给出提示而不是跳到 403 */
function go(path, perm = 'customer:view') {
  if (perm && !has(perm)) {
    ElMessage.info('当前账号没有该模块的访问权限，可联系管理员开通')
    return
  }
  router.push(`${path}?customer=${encodeURIComponent(profile.value.base.name)}`)
}
</script>

<style scoped>
.hub {
  padding-bottom: 8px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 20px;
  margin-bottom: 14px;
  background: linear-gradient(120deg, #eef3ff, #f8faff 55%, #eef8f5);
  border: 1px solid #e3eaf9;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.avatar {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #79a1ff, #2f5fe0);
}

.hero-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hero-title {
  margin: 0;
  font-size: 19px;
  font-weight: 600;
  color: #1f2b3d;
}

.hero-desc {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--text-sub);
}

.phone {
  color: #2f5fe0;
  text-decoration: none;
  font-variant-numeric: tabular-nums;
}

.hero-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.kpi-card {
  padding: 14px 16px;
  transition: box-shadow 0.18s ease, transform 0.18s ease, border-color 0.18s ease;
}

/* KPI 卡本身就是入口：手型 + 悬停浮起 */
.kpi-card.clickable {
  cursor: pointer;
}

.kpi-card.clickable:hover {
  transform: translateY(-2px);
  border-color: #c9d8ff;
  box-shadow: 0 10px 24px rgba(47, 95, 224, 0.12);
}

.kpi-label {
  font-size: 12.5px;
  color: var(--text-sub);
}

.kpi-value {
  font-size: 21px;
  font-weight: 650;
  color: #1f2b3d;
  margin: 8px 0 6px;
  font-variant-numeric: tabular-nums;
}

.kpi-sub {
  font-size: 11.5px;
  color: #97a3b6;
}

.hub-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 14px;
}

@media (max-width: 1180px) {
  .hub-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.col-main,
.col-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.panel {
  padding: 14px 16px 16px;
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

/* 跟进 */
.follow-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.follow-row {
  display: flex;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed #eef1f7;
}

.follow-row:last-child {
  border-bottom: none;
}

.follow-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  background: #3b6cff;
}
.follow-dot.t-上门拜访 {
  background: #17a67a;
}
.follow-dot.t-微信 {
  background: #f0b429;
}
.follow-dot.t-邮件 {
  background: #98a6bb;
}

.follow-main {
  min-width: 0;
  flex: 1;
}

.follow-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}

.follow-name {
  font-weight: 500;
  color: #273449;
}

.follow-time {
  color: #97a3b6;
  font-variant-numeric: tabular-nums;
}

.follow-owner {
  margin-left: auto;
  font-size: 11.5px;
  color: #a7b2c4;
}

.follow-content {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: #4b5a71;
  line-height: 1.6;
}

.follow-next {
  margin: 3px 0 0;
  font-size: 11.5px;
  color: #b45309;
}

/* 小表格 */
.mini-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}

.mini-table th {
  text-align: left;
  font-weight: 400;
  color: #8b98ab;
  font-size: 11.5px;
  padding: 6px 6px 8px;
  border-bottom: 1px solid #eef1f7;
}

.mini-table td {
  padding: 8px 6px;
  border-bottom: 1px dashed #f2f5fa;
  color: #33415a;
}

.mini-table .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.mini-table .strong {
  font-weight: 600;
  color: #1f2b3d;
}

.mini-table .muted {
  color: #8b98ab;
  font-variant-numeric: tabular-nums;
}

.mini-table .ok {
  color: #17a67a;
}
.mini-table .warn {
  color: #b45309;
}

.link {
  color: #2f5fe0;
  cursor: pointer;
  border-bottom: 1px dashed transparent;
  font-variant-numeric: tabular-nums;
}
.link:hover {
  border-bottom-color: #2f5fe0;
}

/* 两列小卡 */
.two-col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}

.row-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

/* 360 里的列表行都是入口：手型 + 悬停底色 */
.row-link {
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.15s ease;
}

.row-link:hover {
  background: #f8faff;
}

.row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #f2f5fa;
}
.row-item:last-child {
  border-bottom: none;
}

.row-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.row-name {
  font-size: 12.5px;
  color: #273449;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-sub {
  font-size: 11.5px;
  color: #97a3b6;
}

.row-amount {
  font-size: 12.5px;
  font-weight: 500;
  color: #2f5fe0;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.row-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}

/* 联系人 */
.contact-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.contact-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #f2f5fa;
}
.contact-row:last-child {
  border-bottom: none;
}

.contact-avatar {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12.5px;
  color: #2f5fe0;
  background: #eef3ff;
}

.contact-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.contact-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #273449;
  font-weight: 500;
}

.contact-sub {
  font-size: 11.5px;
  color: #97a3b6;
  font-variant-numeric: tabular-nums;
}

.call {
  font-size: 12px;
  color: #2f5fe0;
  text-decoration: none;
  white-space: nowrap;
}

/* 应收 */
.recv-list {
  display: flex;
  flex-direction: column;
}

.recv-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #f2f5fa;
}
.recv-row:last-child {
  border-bottom: none;
}

.recv-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.recv-order {
  font-size: 12px;
  color: #33415a;
  font-variant-numeric: tabular-nums;
}

.recv-due {
  font-size: 11.5px;
  color: #97a3b6;
}

.recv-amount {
  font-size: 13px;
  font-weight: 600;
  color: #17a67a;
  font-variant-numeric: tabular-nums;
}

.danger {
  color: #d64545 !important;
}

.revisit-strip {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f6f9ff;
  display: flex;
  gap: 8px;
  font-size: 11.5px;
  line-height: 1.6;
}

.revisit-label {
  color: #2f5fe0;
  white-space: nowrap;
}

.revisit-text {
  color: #5c6b82;
}

.mini-empty {
  padding: 14px 0;
  font-size: 12.5px;
  color: #a7b2c4;
  text-align: center;
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 60px 0;
  color: #8b98ab;
  font-size: 13px;
}

.empty-box p {
  margin: 0;
}
</style>
