<template>
  <div class="page sheet">
    <div v-if="loading" class="page-card state-box">
      <el-icon class="is-loading" :size="22"><Loading /></el-icon>
      <p>正在读取订单…</p>
    </div>

    <div v-else-if="!order" class="page-card state-box">
      <el-icon :size="28" color="#c3cddd"><Warning /></el-icon>
      <p>没有找到单号 {{ route.params.orderNo }} 的订单，可能已被删除</p>
      <el-button type="primary" plain @click="router.push('/sales/order')">返回销售订单</el-button>
    </div>

    <template v-else>
      <!-- 单据抬头：谁、哪一单、多少钱，一眼看完 -->
      <div class="hero page-card">
        <div class="hero-left">
          <div class="doc-tile"><el-icon :size="22"><Document /></el-icon></div>
          <div class="hero-main">
            <div class="hero-title-line">
              <h2 class="hero-title">{{ order.orderNo }}</h2>
              <el-tag size="small" round effect="light" :type="statusType[order.status] || 'info'">
                {{ order.status }}
              </el-tag>
              <el-tag size="small" round effect="plain" type="info">销售合同</el-tag>
            </div>
            <p class="hero-desc">
              <span class="link" @click="openCustomerSafe">{{ order.customer }}</span>
              · 合同金额 <b class="hero-amount">{{ money(summary.amount) }}</b>
              · 下单 {{ order.orderDate }} · 归属 {{ order.owner }}
            </p>
          </div>
        </div>
        <div class="hero-actions no-print">
          <el-button :icon="Back" @click="router.back()">返回</el-button>
          <el-button v-if="has('order:edit')" :icon="EditPen" @click="editSheet">编辑清单</el-button>
          <el-button :icon="Printer" @click="printSheet">打印清单</el-button>
          <el-button type="primary" :icon="User" @click="openCustomerSafe">客户 360</el-button>
        </div>
      </div>

      <!-- 关键指标：金额、赚多少、收了多少、卖了几种 -->
      <div class="kpi-grid">
        <div v-for="k in kpis" :key="k.label" class="kpi-card page-card">
          <span class="kpi-label">{{ k.label }}</span>
          <div class="kpi-value" :class="k.tone">{{ k.value }}</div>
          <div class="kpi-sub" :class="{ danger: k.danger }">{{ k.sub }}</div>
        </div>
      </div>

      <div class="sheet-grid">
        <div class="col-main">
          <!-- 合同清单：领导最关心的部分，商品、数量、单价、金额、毛利一屏看完 -->
          <section class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>合同清单</h3>
                <p>共 {{ items.length }} 行 · 合计 {{ summary.qty }} 件 · 金额与明细逐行对齐，可打印归档</p>
              </div>
              <span class="panel-tag">金额 = 单价 × 数量</span>
            </header>

            <div v-if="items.length" class="table-wrap">
              <table class="list-table">
              <thead>
                <tr>
                  <th class="idx">#</th>
                  <th>商品名称</th>
                  <th>分类</th>
                  <th class="num">数量</th>
                  <th class="num">单价</th>
                  <th class="num">金额</th>
                  <th class="num">占比</th>
                  <th v-if="canSeeProfit" class="num">毛利率</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="l in items" :key="l.id ?? l.idx">
                  <td class="idx muted">{{ l.idx }}</td>
                  <td>
                    <span class="prod-name prod-link" @click="openProduct(l.product)">{{ l.product }}</span>
                  </td>
                  <td><span class="cat">{{ l.category || '—' }}</span></td>
                  <td class="num">{{ l.qty }}</td>
                  <td class="num">{{ money(l.price) }}</td>
                  <td class="num strong">{{ money(l.amount) }}</td>
                  <td class="num muted">{{ share(l.amount) }}</td>
                  <td
                    v-if="canSeeProfit"
                    class="num"
                    :class="l.margin >= 40 ? 'ok' : l.margin >= 20 ? '' : 'warn'"
                  >
                    {{ l.margin.toFixed(0) }}%
                  </td>
                </tr>
              </tbody>
              </table>
            </div>

            <div v-else class="mini-empty">
              这张订单还没有录入商品明细，金额为手工填写
              <el-button v-if="has('order:edit')" link type="primary" class="no-print" @click="editSheet">
                去录入明细
              </el-button>
            </div>

            <div v-if="items.length" class="sum-bar">
              <span class="sum-label">合计</span>
              <span class="sum-item">{{ items.length }} 行 · {{ summary.qty }} 件</span>
              <span v-if="canSeeProfit" class="sum-item">成本 {{ money(summary.cost) }}</span>
              <span v-if="canSeeProfit" class="sum-item">毛利 {{ money(summary.profit) }}</span>
              <span class="sum-amount">{{ money(summary.amount) }}</span>
            </div>
          </section>

          <!-- 合同与交付口径：把这张单后续的货和钱串起来；销售没有交付回款权限，整块不展示 -->
          <section v-if="has('receivable:view')" class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>履约与回款</h3>
                <p>合同签完到货款结清，中间的发货、收款都挂在这张单上</p>
              </div>
              <el-button
                v-if="has('receivable:view')"
                class="no-print"
                link
                type="primary"
                @click="goReceivable"
              >
                应收账款<el-icon><ArrowRight /></el-icon>
              </el-button>
            </header>

            <div class="flow">
              <div class="flow-step" :class="{ done: order.status !== '已取消' }">
                <span class="flow-dot"></span>
                <div class="flow-main">
                  <span class="flow-title">合同签订</span>
                  <span class="flow-sub">{{ order.orderDate }} · {{ money(summary.amount) }}</span>
                </div>
              </div>
              <div class="flow-step" :class="{ done: shipDocs.length }">
                <span class="flow-dot"></span>
                <div class="flow-main">
                  <span class="flow-title">发货交付</span>
                  <span class="flow-sub">
                    {{ shipDocs.length ? `${shipDocs.length} 张发货单 · ${shipQty} 件` : '尚未发货' }}
                  </span>
                </div>
              </div>
              <div class="flow-step" :class="{ done: summary.paid > 0 }">
                <span class="flow-dot"></span>
                <div class="flow-main">
                  <span class="flow-title">收款回款</span>
                  <span class="flow-sub">已收 {{ money(summary.paid) }} / 合同 {{ money(summary.amount) }}</span>
                </div>
              </div>
              <div class="flow-step" :class="{ done: summary.unpaid === 0 }">
                <span class="flow-dot"></span>
                <div class="flow-main">
                  <span class="flow-title">货款结清</span>
                  <span class="flow-sub">
                    {{ summary.unpaid === 0 ? '已结清，可归档' : `未收 ${money(summary.unpaid)}` }}
                  </span>
                </div>
              </div>
            </div>

            <el-progress
              class="recv-progress"
              :percentage="summary.paidRate"
              :stroke-width="10"
              :color="summary.overdue ? '#d64545' : '#17a67a'"
            />
            <p class="recv-note">
              回款进度 {{ summary.paidRate }}%
              <template v-if="summary.unpaid > 0 && recv?.dueDate">
                · 约定 {{ recv.dueDate }} 前付清
              </template>
              <template v-else-if="summary.unpaid === 0">· 货款已结清</template>
              <span v-if="summary.overdue" class="danger"> · 已逾期，需催收</span>
            </p>
          </section>
        </div>

        <div class="col-side">
          <!-- 单据信息 -->
          <section class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>单据信息</h3>
                <p>签字、收货、对账都按这里的人联系</p>
              </div>
            </header>
            <dl class="info-list">
              <div class="info-row">
                <dt>客户单位</dt>
                <dd><span class="link" @click="openCustomerSafe">{{ order.customer }}</span></dd>
              </div>
              <div class="info-row">
                <dt>对接联系人</dt>
                <dd>{{ order.contact || '—' }}</dd>
              </div>
              <div class="info-row">
                <dt>联系电话</dt>
                <dd>
                  <a v-if="order.phone" class="phone" :href="'tel:' + order.phone">{{ order.phone }}</a>
                  <span v-else>—</span>
                </dd>
              </div>
              <div class="info-row">
                <dt>下单日期</dt>
                <dd>{{ order.orderDate }}</dd>
              </div>
              <div class="info-row">
                <dt>归属销售</dt>
                <dd>{{ order.owner }}</dd>
              </div>
              <div class="info-row">
                <dt>订单状态</dt>
                <dd>
                  <el-tag size="small" round effect="light" :type="statusType[order.status] || 'info'">
                    {{ order.status }}
                  </el-tag>
                </dd>
              </div>
            </dl>
          </section>

          <!-- 关联单据：发货、收款、退换货，点单号去对应模块 -->
          <section class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>关联单据</h3>
                <p>{{ has('receivable:view') ? '围绕这张合同的发货、收款与售后' : '围绕这张合同的售后记录' }}</p>
              </div>
              <span class="panel-tag">{{ related.length }} 张</span>
            </header>
            <ul v-if="related.length" class="doc-list">
              <li v-for="d in related" :key="d.key" class="doc-row doc-click" @click="openDoc(d)">
                <el-tag size="small" round effect="plain" :type="d.tagType">{{ d.type }}</el-tag>
                <div class="doc-main">
                  <span class="doc-no">{{ d.no }}</span>
                  <span class="doc-sub">{{ d.label }}</span>
                </div>
                <span class="doc-amount" :class="{ muted: !d.amount }">
                  {{ d.amount ? money(d.amount) : d.status }}
                </span>
              </li>
            </ul>
            <div v-else class="mini-empty">
              {{ has('receivable:view') ? '还没有关联单据，发货、收款后会自动挂上来' : '这张单还没有退换货记录' }}
            </div>
          </section>

          <!-- 同客户的其他合同：领导看这张单时，顺手对比这家客户还签了什么 -->
          <section v-if="siblings.length" class="page-card panel">
            <header class="panel-head">
              <div>
                <h3>同客户其他合同</h3>
                <p>{{ order.customer }} 共 {{ siblingTotal }} 张单，点单号可切换查看</p>
              </div>
              <el-button v-if="has('order:view')" link type="primary" @click="goCustomerOrders">
                全部<el-icon><ArrowRight /></el-icon>
              </el-button>
            </header>
            <ul class="doc-list">
              <li
                v-for="o in siblings"
                :key="o.id"
                class="doc-row sib-row"
                @click="openSibling(o.orderNo)"
              >
                <div class="doc-main">
                  <span class="doc-no">{{ o.orderNo }}</span>
                  <span class="doc-sub">{{ o.orderDate }} · {{ o.owner }}</span>
                </div>
                <div class="sib-right">
                  <span class="sib-amount">{{ money(o.amount) }}</span>
                  <span class="sib-status">{{ o.status }}</span>
                </div>
              </li>
            </ul>
          </section>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Printer, User, Document, Loading, Warning, ArrowRight, EditPen } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { usePermission } from '@/composables/usePermission'
import { useCustomerNav, orderPath } from '@/utils/links'
import { money } from '@/utils/analytics'
import { loadModuleRows } from '@/composables/useModuleData'
import { shipments as seedShipments, receipts as seedReceipts, returnOrders as seedReturns, receivables as seedReceivables } from '@/mock/data'

const route = useRoute()
const router = useRouter()
const { has } = usePermission()
// 成本与毛利是字段级权限：销售组长与顾问只看到金额与件数
const canSeeProfit = computed(() => has('order:profit:view'))
const { openCustomer } = useCustomerNav()

const statusType = {
  待付款: 'warning',
  部分回款: 'primary',
  已付款: 'success',
  已完成: 'success',
  已取消: 'info'
}

const loading = ref(true)
const rows = ref([])
const shipments = ref([])
const receipts = ref([])
const returnOrders = ref([])
const receivables = ref([])

onMounted(async () => {
  try {
    const [orders, shipRows, receiptRows, returnRows, receivableRows] = await Promise.all([
      request.get('/business/orders'),
      loadModuleRows('shipments', seedShipments),
      loadModuleRows('receipts', seedReceipts),
      loadModuleRows('returns', seedReturns),
      loadModuleRows('receivables', seedReceivables)
    ])
    rows.value = orders || []
    shipments.value = shipRows || []
    receipts.value = receiptRows || []
    returnOrders.value = returnRows || []
    receivables.value = receivableRows || []
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
})

const order = computed(() =>
  rows.value.find((o) => String(o.orderNo) === String(route.params.orderNo))
)

/** 明细统一转成数字并算出金额、毛利，后端只存单价与成本，避免两处口径打架 */
const items = computed(() => {
  const list = order.value?.items || []
  return list.map((l, i) => {
    const price = Number(l.price) || 0
    const cost = Number(l.cost) || 0
    const qty = Number(l.qty) || 0
    const amount = Number(l.amount) || price * qty
    return {
      ...l,
      idx: i + 1,
      qty,
      price,
      cost,
      amount,
      profit: (price - cost) * qty,
      margin: price ? ((price - cost) / price) * 100 : 0
    }
  })
})

const recv = computed(() => receivables.value.find((r) => r.orderNo === order.value?.orderNo) || null)

const summary = computed(() => {
  const amount = Number(order.value?.amount) || items.value.reduce((s, l) => s + l.amount, 0)
  const qty = items.value.reduce((s, l) => s + l.qty, 0)
  const cost = items.value.reduce((s, l) => s + l.cost * l.qty, 0)
  const profit = items.value.reduce((s, l) => s + l.profit, 0)
  // 有应收记录以记录为准；没有记录时，已付款 / 已完成的单视为款清
  const paid = recv.value
    ? Number(recv.value.received) || 0
    : ['已付款', '已完成'].includes(order.value?.status)
      ? amount
      : 0
  const unpaid = Math.max(0, amount - paid)
  const dueDate = recv.value?.dueDate
  const today = new Date().toISOString().slice(0, 10)
  return {
    amount,
    qty,
    cost,
    status: order.value?.status || '—',
    orderDate: order.value?.orderDate || '—',
    profit,
    margin: amount ? Math.round((profit / amount) * 1000) / 10 : 0,
    paid,
    unpaid,
    paidRate: amount ? Math.min(100, Math.round((paid / amount) * 100)) : 0,
    overdue: !!dueDate && unpaid > 0 && dueDate < today
  }
})

const kpis = computed(() => {
  const s = summary.value
  const top = [...items.value].sort((a, b) => b.amount - a.amount)[0]
  return [
    { label: '合同金额', value: money(s.amount), sub: `${items.value.length} 种商品 · 合计 ${s.qty} 件` },
    // 成本与毛利属于字段权限：销售（组长 / 顾问）看到的是件均价，不展示赚多少
    canSeeProfit.value
      ? {
          label: '毛利额',
          value: money(s.profit),
          sub: `毛利率 ${s.margin}% · 成本 ${money(s.cost)}`,
          tone: 'ok'
        }
      : {
          label: '件均价',
          value: money(s.qty ? s.amount / s.qty : 0),
          sub: `合计 ${s.qty} 件 · ${items.value.length} 种商品`
        },
    has('receivable:view')
      ? {
          label: '已回款',
          value: money(s.paid),
          sub: s.unpaid > 0 ? `未收 ${money(s.unpaid)}${s.overdue ? ' · 已逾期' : ''}` : '货款已结清',
          danger: s.overdue
        }
      : {
          label: '订单状态',
          value: s.status,
          sub: `${s.orderDate} 下单 · ${items.value.length} 行明细`
        },
    {
      label: '商品构成',
      value: `${items.value.length} 种 / ${s.qty} 件`,
      sub: top ? `最大一项 ${top.product} 占 ${share(top.amount)}` : '暂无明细'
    }
  ]
})

/** 明细金额占合同额的比例 */
function share(amount) {
  const total = summary.value.amount
  return total ? Math.round((amount / total) * 100) + '%' : '0%'
}

/* ---------- 关联单据（发货 / 收款 / 退换货） ---------- */
const shipDocs = computed(() => shipments.value.filter((s) => s.orderNo === order.value?.orderNo))
const shipQty = computed(() => shipDocs.value.reduce((s, x) => s + x.qty, 0))

const related = computed(() => {
  const no = order.value?.orderNo
  if (!no) return []
  const list = []
  // 发货 / 收款属于交付与回款模块，销售没有该权限时只保留退换货这类售后单据
  const canSeeDelivery = has('receivable:view')
  if (canSeeDelivery) shipDocs.value.forEach((s) =>
    list.push({
      key: 'ship-' + s.id,
      type: '发货',
      tagType: 'primary',
      no: s.shipNo,
      label: `${s.shipDate} · ${s.carrier}`,
      status: s.status,
      to: '/delivery/shipment',
      perm: 'delivery:view'
    })
  )
  ;(canSeeDelivery ? receipts.value : [])
    .filter((r) => r.orderNo === no)
    .forEach((r) =>
      list.push({
        key: 'recv-' + r.id,
        type: '收款',
        tagType: 'success',
        no: r.receiptNo,
        label: `${r.receiptDate} · ${r.method}`,
        amount: r.amount,
        to: '/delivery/receipt',
        perm: 'receipt:view'
      })
    )
  returnOrders.value
    .filter((r) => r.orderNo === no)
    .forEach((r) =>
      list.push({
        key: 'return-' + r.id,
        type: '退换货',
        tagType: 'danger',
        no: r.returnNo,
        label: `${r.applyDate} · ${r.type} ${r.qty} 件`,
        amount: r.amount,
        to: '/service/return',
        perm: 'return:view'
      })
    )
  return list
})

/** 看这家客户的应收：跳到应收账款并带上客户条件 */
function goReceivable() {
  go(`/delivery/receivable?customer=${encodeURIComponent(order.value?.customer || '')}`, 'receivable:view')
}

/* ---------- 同客户其他合同 ---------- */
const customerOrders = computed(() =>
  rows.value.filter((o) => o.customer === order.value?.customer)
)

const siblingTotal = computed(() => customerOrders.value.length)

const siblings = computed(() =>
  customerOrders.value
    .filter((o) => o.orderNo !== order.value?.orderNo)
    .sort((a, b) => String(b.orderDate).localeCompare(String(a.orderDate)))
    .slice(0, 4)
)

function openSibling(orderNo) {
  router.push(orderPath(orderNo))
}

/** 看这家客户的全部订单：回到列表按客户筛 */
function goCustomerOrders() {
  router.push({ path: '/sales/order', query: { customer: order.value?.customer } })
}

/* ---------- 跳转 ---------- */
function openCustomerSafe() {
  if (!has('customer:view')) {
    ElMessage.info('当前账号没有客户档案的访问权限，可联系管理员开通')
    return
  }
  openCustomer(order.value?.customer)
}

function go(path, perm) {
  if (perm && !has(perm)) {
    ElMessage.info('当前账号没有该模块的访问权限，可联系管理员开通')
    return
  }
  router.push(path)
}

/** 关联单据点单号：到对应模块按单号搜出来 */
function openDoc(d) {
  go(`${d.to}?q=${encodeURIComponent(d.no)}`, d.perm)
}

/** 编辑清单：回列表并自动打开这一单的编辑弹窗（明细在弹窗里维护） */
function editSheet() {
  router.push({
    path: '/sales/order',
    query: { q: order.value?.orderNo, edit: order.value?.id }
  })
}

/** 点商品名到「商品」模块，按名称搜出这个 SKU 的售价与库存 */
function openProduct(name) {
  if (!has('product:view')) {
    ElMessage.info('当前账号没有商品资料的访问权限，可联系管理员开通')
    return
  }
  router.push({ path: '/sales/product', query: { q: name } })
}

function printSheet() {
  window.print()
}
</script>

<style scoped>
.sheet {
  padding-bottom: 8px;
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 60px 0;
  color: #8b98ab;
  font-size: 13px;
}

.state-box p {
  margin: 0;
}

/* 抬头 */
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

.doc-tile {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 12px;
  display: grid;
  place-items: center;
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
  font-weight: 650;
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

.hero-desc {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--text-sub);
}

.hero-amount {
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

.hero-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.link {
  color: #2f5fe0;
  cursor: pointer;
  border-bottom: 1px dashed transparent;
}

.link:hover {
  border-bottom-color: #2f5fe0;
}

/* 指标 */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.kpi-card {
  padding: 14px 16px;
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

.kpi-value.ok {
  color: #17a67a;
}

.kpi-sub {
  font-size: 11.5px;
  color: #97a3b6;
}

/* 主体两栏 */
.sheet-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

@media (max-width: 1360px) {
  .sheet-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  /* 改单栏后侧栏三张卡片横排，避免一长条把页面拉得很高 */
  .col-side {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    align-items: start;
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
  margin-bottom: 12px;
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

.panel-tag {
  font-size: 11.5px;
  color: #97a3b6;
  white-space: nowrap;
}

/* 合同清单表格 */
/* 极窄屏（如 1024 分屏）时清单可横向滚动，不把商品名挤变形 */
.table-wrap {
  overflow-x: auto;
}

.list-table {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
  font-size: 12.5px;
  table-layout: fixed;
}

.list-table th {
  text-align: left;
  font-weight: 400;
  color: #8b98ab;
  font-size: 11.5px;
  padding: 8px 8px;
  background: #f8fafd;
  border-top: 1px solid #eef1f7;
  border-bottom: 1px solid #eef1f7;
}

.list-table td {
  padding: 9px 8px;
  border-bottom: 1px dashed #f2f5fa;
  color: #33415a;
  vertical-align: middle;
}

.list-table tbody tr:hover td {
  background: #fafcff;
}

.list-table .idx {
  width: 34px;
  text-align: center;
}

.list-table th:nth-child(3) {
  width: 116px;
}

/* 数字列按内容给固定宽度，把剩余宽度全留给商品名称，避免窄屏时名称被挤成竖排 */
.list-table .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  width: 66px;
}

.list-table th:nth-child(5),
.list-table td:nth-child(5) {
  width: 84px;
}

.list-table th:nth-child(6),
.list-table td:nth-child(6) {
  width: 102px;
}

.list-table th:nth-child(7),
.list-table td:nth-child(7) {
  width: 60px;
}

.list-table th:nth-child(8),
.list-table td:nth-child(8) {
  width: 70px;
}

.prod-name {
  color: #273449;
  font-weight: 500;
}

/* 商品名可点，进商品资料看售价与库存；弱化样式，避免像超链接那么扎眼 */
.prod-link {
  cursor: pointer;
}

.prod-link:hover {
  color: #2f5fe0;
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 3px;
}

/* 同客户其他合同 */
.sib-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  white-space: nowrap;
}

.sib-amount {
  font-size: 12.5px;
  font-weight: 600;
  color: #273449;
  font-variant-numeric: tabular-nums;
}

.sib-status {
  font-size: 11px;
  color: #97a3b6;
}

.cat {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 11px;
  color: #52627a;
  background: #f2f5fa;
  border: 1px solid #e6eaf2;
  white-space: nowrap;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.muted {
  color: #8b98ab;
}

.strong {
  font-weight: 600;
  color: #1f2b3d;
}

.ok {
  color: #17a67a;
}

.warn {
  color: #b45309;
}

.danger {
  color: #d64545;
}

/* 合计条 */
.sum-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 4px;
  padding: 12px 8px 2px;
  border-top: 1px solid #eef1f7;
  font-size: 12px;
  color: #6b7a90;
}

.sum-label {
  color: #273449;
  font-weight: 600;
}

.sum-item {
  font-variant-numeric: tabular-nums;
}

.sum-amount {
  font-size: 15px;
  font-weight: 650;
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

/* 履约流程 */
.flow {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.flow-step {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafd;
  border: 1px solid #eef1f7;
}

.flow-dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  margin-top: 4px;
  border-radius: 50%;
  background: #c8d2e2;
}

.flow-step.done .flow-dot {
  background: #17a67a;
}

.flow-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.flow-title {
  font-size: 12.5px;
  color: #273449;
  font-weight: 500;
}

.flow-sub {
  font-size: 11.5px;
  color: #97a3b6;
  font-variant-numeric: tabular-nums;
}

.recv-progress {
  margin: 2px 0 6px;
}

.recv-note {
  margin: 0;
  font-size: 11.5px;
  color: #8b98ab;
}

/* 单据信息 */
.info-list {
  margin: 0;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #f2f5fa;
  font-size: 12.5px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row dt {
  color: #97a3b6;
  flex-shrink: 0;
}

.info-row dd {
  margin: 0;
  color: #33415a;
  text-align: right;
  word-break: break-all;
}

.phone {
  color: #2f5fe0;
  text-decoration: none;
  font-variant-numeric: tabular-nums;
}

/* 关联单据 */
.doc-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.doc-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 6px;
  border-radius: 8px;
  border-bottom: 1px dashed #f2f5fa;
}

.doc-row:last-child {
  border-bottom: none;
}

/* 一整行就是一张单据，整行可点比只点单号好按 */
.doc-click,
.sib-row {
  cursor: pointer;
}

.doc-click:hover,
.sib-row:hover {
  background: #f8faff;
}

.doc-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.doc-no {
  font-size: 12.5px;
  color: #2f5fe0;
  cursor: pointer;
  font-variant-numeric: tabular-nums;
}

.doc-no:hover {
  text-decoration: underline;
}

.doc-sub {
  font-size: 11.5px;
  color: #97a3b6;
}

.doc-amount {
  font-size: 12.5px;
  font-weight: 600;
  color: #17a67a;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.doc-amount.muted {
  font-weight: 400;
  color: #97a3b6;
}

.mini-empty {
  padding: 16px 0;
  font-size: 12.5px;
  color: #a7b2c4;
  text-align: center;
}

/* 打印：只留单据本体，方便直接签字归档 */
@media print {
  :global(.app-layout > *:not(.main-wrap)) {
    display: none !important;
  }

  :global(.main-wrap > *:not(.content)) {
    display: none !important;
  }

  :global(.content) {
    padding: 0 !important;
    overflow: visible !important;
  }

  .no-print {
    display: none !important;
  }

  .hero {
    background: #fff;
  }

  .sheet-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  /* 打印一律单栏：侧栏卡片顺次排下去，不会被截成半张 */
  .col-side {
    display: flex;
    flex-direction: column;
  }

  .table-wrap {
    overflow: visible;
  }

  .page-card {
    box-shadow: none;
    break-inside: avoid;
  }
}
</style>
