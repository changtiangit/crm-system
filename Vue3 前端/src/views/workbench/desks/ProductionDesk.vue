<template>
  <div class="production-desk">
    <!-- 生产看「货能不能按时出去」：排产、备货、发货、补发 -->
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
          <span class="dk-delta" :class="k.trend">{{ k.kdelta }}</span>
          <span>{{ k.sub }}</span>
        </div>
      </div>
    </div>

    <div class="dk-two-col">
      <!-- 待交付订单：签了单还没发出去的，是生产的第一优先级 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>待交付订单</h3>
            <p>已签单但还没安排发货的订单，按下单时间排产；点一行看订单明细清单</p>
          </div>
          <button class="dk-link" @click="router.push('/sales/order')">
            销售订单<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="pendingOrders.length" class="dk-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>客户</th>
              <th class="num">件数</th>
              <th class="num">金额</th>
              <th class="w-date">下单日</th>
              <th class="w-state">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="o in pendingOrders"
              :key="o.id"
              class="click"
              :title="`查看订单 ${o.orderNo}`"
              @click="router.push(`/sales/order/detail/${o.orderNo}`)"
            >
              <td class="mono muted">{{ o.orderNo }}</td>
              <td class="strong">{{ o.customer }}</td>
              <td class="num">{{ qtyOf(o) }}</td>
              <td class="num">{{ money(o.amount) }}</td>
              <td class="muted">{{ o.orderDate }}</td>
              <td><span class="dk-tag" :class="payTone(o.status)">{{ o.status }}</span></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">没有待交付订单，全部已安排发货</div>
      </section>

      <!-- 发货排期 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>发货排期</h3>
            <p>待发货优先，其次在途跟单号</p>
          </div>
          <button class="dk-link" @click="router.push('/delivery/shipment')">
            发货记录<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <ul v-if="shipList.length" class="dk-list">
          <li
            v-for="s in shipList"
            :key="s.id"
            class="dk-row click"
            :title="`查看「${s.customer}」的发货记录`"
            @click="router.push(`/delivery/shipment?customer=${encodeURIComponent(s.customer)}`)"
          >
            <span class="dk-avatar">{{ s.customer.slice(0, 1) }}</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ s.customer }}</span>
                <span class="dk-tag" :class="s.status === '待发货' ? 'warn' : 'ok'">{{ s.status }}</span>
              </div>
              <p class="dk-row-sub">{{ s.shipNo }} · {{ s.qty }} 件 · {{ s.carrier }}</p>
            </div>
            <span class="dk-row-side">{{ (s.shipDate || '').slice(5) }}</span>
          </li>
        </ul>
        <div v-else class="dk-empty">近期没有发货记录</div>
      </section>
    </div>

    <div class="dk-two-col even">
      <!-- 库存预警：备货动作的起点 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>库存预警</h3>
            <p>库存低于 10 的 SKU，先补货再接单</p>
          </div>
          <button class="dk-link" @click="router.push('/sales/product?stock=low')">
            商品库存<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <ul v-if="lowStock.length" class="dk-list">
          <li
            v-for="p in lowStock"
            :key="p.id"
            class="dk-row click"
            :title="`查看商品「${p.name}」`"
            @click="router.push('/sales/product?stock=low')"
          >
            <span class="stock-num" :class="{ zero: p.stock <= 0 }">{{ p.stock }}</span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ p.name }}</span>
                <span class="dk-row-side">{{ p.category }}</span>
              </div>
              <p class="dk-row-sub">{{ p.code }} · {{ p.spec || '无规格' }} · 单位 {{ p.unit }}</p>
            </div>
          </li>
        </ul>
        <div v-else class="dk-empty">库存都在安全线以上</div>
      </section>

      <!-- 退换货补发 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>退换货补发</h3>
            <p>售后受理后要生产这边安排出货</p>
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
                <span class="dk-row-side">{{ r.qty }} 件</span>
              </div>
              <p class="dk-row-sub">{{ r.reason }}</p>
            </div>
            <span class="dk-row-side">{{ r.status }}</span>
          </li>
        </ul>
        <div v-else class="dk-empty">没有需要补发的退换货</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Box, Van, Warning, Wallet } from '@element-plus/icons-vue'
import { orders, shipments, returnOrders } from '@/mock/data'
import { money, sum } from '@/utils/analytics'
import { useProductStore } from '@/stores/product'

defineProps({
  profile: { type: Object, required: true }
})

const router = useRouter()
const productStore = useProductStore()

onMounted(() => {
  if (!productStore.loaded) productStore.loadProducts()
})

const qtyOf = (o) => sum(o.items || [], 'qty')
const payTone = (s) => (s === '待付款' ? 'warn' : s === '部分回款' ? 'info' : 'ok')

const shippedOrders = computed(() => new Set(shipments.map((s) => s.orderNo)))

/** 待交付：已签单、未取消、还没发货的订单 */
const pendingOrders = computed(() =>
  orders
    .filter((o) => o.status !== '已取消' && !['已完成'].includes(o.status) && !shippedOrders.value.has(o.orderNo))
    .slice()
    .sort((a, b) => String(a.orderDate).localeCompare(String(b.orderDate)))
)

const pendingShip = computed(() => shipments.filter((s) => s.status === '待发货'))
const inTransit = computed(() => shipments.filter((s) => s.status === '已发出'))

const shipList = computed(() =>
  shipments
    .slice()
    .sort((a, b) => {
      const w = (s) => (s.status === '待发货' ? 0 : 1)
      return w(a) - w(b) || String(b.shipDate).localeCompare(String(a.shipDate))
    })
    .slice(0, 5)
)

const monthKey = computed(() => {
  const keys = shipments.map((s) => (s.shipDate || '').slice(0, 7)).filter(Boolean).sort()
  return keys.length ? keys[keys.length - 1] : ''
})
const monthShipQty = computed(() =>
  shipments
    .filter((s) => (s.shipDate || '').startsWith(monthKey.value))
    .reduce((n, s) => n + Number(s.qty || 0), 0)
)

const lowStock = computed(() => (productStore.products || []).filter((p) => p.stock < 10).sort((a, b) => a.stock - b.stock).slice(0, 6))

const openReturns = computed(() => returnOrders.filter((r) => ['待审核', '已受理'].includes(r.status)))

const kpis = computed(() => [
  {
    label: '待交付订单',
    value: pendingOrders.value.length,
    kdelta: pendingOrders.value.length ? '需排产' : '全部已排',
    trend: pendingOrders.value.length ? 'warn' : 'up',
    sub: `共 ${pendingOrders.value.reduce((n, o) => n + qtyOf(o), 0)} 件待出`,
    icon: Box,
    bg: '#fdf4e3',
    color: '#b45309',
    to: '/sales/order',
    drill: '销售订单',
    alarm: pendingOrders.value.length > 0
  },
  {
    label: '待发货',
    value: pendingShip.value.length,
    kdelta: `在途 ${inTransit.value.length} 单`,
    trend: pendingShip.value.length ? 'warn' : 'up',
    sub: '仓库待出库',
    icon: Van,
    bg: '#eef3ff',
    color: '#3b6cff',
    to: '/delivery/shipment',
    drill: '发货记录'
  },
  {
    label: '本月发货件数',
    value: monthShipQty.value,
    kdelta: `${monthKey.value.slice(5)} 月`,
    trend: 'up',
    sub: `发货单 ${shipments.filter((s) => (s.shipDate || '').startsWith(monthKey.value)).length} 张`,
    icon: Wallet,
    bg: '#e8f8f2',
    color: '#0f9d76',
    to: '/delivery/shipment',
    drill: '发货记录'
  },
  {
    label: '库存预警 SKU',
    value: lowStock.value.length,
    kdelta: lowStock.value.length ? '需补货' : '库存充足',
    trend: lowStock.value.length ? 'down' : 'up',
    sub: '库存低于 10 件',
    icon: Warning,
    bg: '#fdeceb',
    color: '#c0392b',
    to: '/sales/product?stock=low',
    drill: '库存预警商品',
    alarm: lowStock.value.length > 0
  }
])
</script>

<style scoped>
.w-date {
  width: 96px;
}

.w-state {
  width: 92px;
}

.stock-num {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 650;
  background: #fdf4e3;
  color: #b45309;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.stock-num.zero {
  background: #fdeceb;
  color: #c0392b;
}
</style>
