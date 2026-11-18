<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">产品销售分析</h2>
        <p class="page-desc">
          卖得动的是哪些、赚得多的是哪些、库房压着的是哪些；点商品直接跳到商品资料，价格、库存、技术资料都在那边维护
        </p>
      </div>
      <div class="header-actions">
        <el-button :icon="Goods" @click="router.push('/sales/product')">商品资料</el-button>
      </div>
    </div>

    <StatsTabs active="product" />

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

    <div class="two-col">
      <!-- 品类占比 -->
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>品类销售占比</h3>
            <p>按销售额拆解，占比高的品类优先保供</p>
          </div>
          <span class="panel-badge">{{ categories.length }} 个品类</span>
        </header>
        <div class="cat-list">
          <div v-for="c in categories" :key="c.category" class="cat-row">
            <div class="cat-meta">
              <span class="cat-name">{{ c.category }}</span>
              <span class="cat-amount">{{ money(c.amount) }} · {{ Math.round(c.share * 100) }}%</span>
            </div>
            <div class="cat-bar">
              <span :style="{ width: c.share / categories[0].share * 100 + '%' }"></span>
            </div>
            <span class="cat-qty">卖出 {{ c.qty.toLocaleString('zh-CN') }} 件 · 毛利 {{ Math.round((c.profit / c.amount) * 100) }}%</span>
          </div>
        </div>
      </section>

      <!-- 库存 / 滞销 -->
      <section class="page-card panel">
        <header class="panel-head">
          <div>
            <h3>库存与滞销提醒</h3>
            <p>销量靠前但库存见底的先补货；有库存却零销量的考虑调价或搭赠</p>
          </div>
          <el-button link type="primary" @click="open('/sales/product', 'product:view')">
            商品资料<el-icon><ArrowRight /></el-icon>
          </el-button>
        </header>

        <div class="risk-block">
          <span class="risk-title">热销缺货 / 库存偏低</span>
          <ul v-if="stockRisk.length" class="risk-list">
            <li v-for="r in stockRisk" :key="r.name" class="risk-row" @click="drillProduct(r.name)">
              <span class="risk-name">{{ r.name }}</span>
              <span class="risk-sub">已售 {{ r.qty }} 件 · 库存 {{ r.stock }} {{ r.unit }}</span>
              <el-tag size="small" round effect="light" :type="r.stock <= 0 ? 'danger' : 'warning'">
                {{ r.stock <= 0 ? '缺货' : '偏低' }}
              </el-tag>
            </li>
          </ul>
          <div v-else class="mini-empty">没有缺货风险</div>
        </div>

        <div class="risk-block">
          <span class="risk-title">有库存但本期没销量</span>
          <ul v-if="slowMovers.length" class="risk-list">
            <li v-for="r in slowMovers" :key="r.name" class="risk-row" @click="drillProduct(r.name)">
              <span class="risk-name">{{ r.name }}</span>
              <span class="risk-sub">{{ r.category }} · 库存 {{ r.stock }} {{ r.unit }}</span>
              <el-tag size="small" round effect="plain" type="info">滞销</el-tag>
            </li>
          </ul>
          <div v-else class="mini-empty">没有明显滞销品</div>
        </div>
      </section>
    </div>

    <!-- 商品排行 -->
    <section class="page-card panel">
      <header class="panel-head">
        <div>
          <h3>商品销售排行</h3>
          <p>按销售额排序；「毛利率」低于 30% 的品建议复核进价或调价</p>
        </div>
        <span class="panel-badge">共 {{ products.length }} 个在售品</span>
      </header>

      <table class="rank-table">
        <thead>
          <tr>
            <th class="w-no">#</th>
            <th>商品名称</th>
            <th>品类</th>
            <th class="num">销量</th>
            <th class="num">销售额</th>
            <th class="num">毛利</th>
            <th class="num">毛利率</th>
            <th class="num">成交客户</th>
            <th class="num">库存</th>
            <th class="w-op">关联</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(p, i) in products" :key="p.product" class="rank-row">
            <td><span class="rank-no" :class="'no' + (i + 1)">{{ i + 1 }}</span></td>
            <td>
              <span class="prod-name" @click="drillProduct(p.product)">{{ p.product }}</span>
              <span v-if="p.ticketCount" class="prod-flag">{{ p.ticketCount }} 张售后工单</span>
            </td>
            <td class="muted">{{ p.category }}</td>
            <td class="num">{{ p.qty.toLocaleString('zh-CN') }}</td>
            <td class="num strong">{{ money(p.amount) }}</td>
            <td class="num">{{ money(p.profit) }}</td>
            <td class="num" :class="p.rate < 0.3 ? 'warn' : ''">{{ Math.round(p.rate * 100) }}%</td>
            <td class="num">{{ p.customerCount }}</td>
            <td class="num">
              <template v-if="stockOf(p.product) !== null">
                <span :class="stockClass(p.product)">{{ stockOf(p.product) }}</span>
              </template>
              <span v-else class="muted">—</span>
            </td>
            <td>
              <el-button v-if="p.ticketCount" link type="primary" @click="drillTickets(p.product)">工单</el-button>
              <span v-else class="muted">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Goods, ArrowRight, GoodsFilled, Money, TrendCharts, Box } from '@element-plus/icons-vue'
import StatsTabs from './components/StatsTabs.vue'
import { usePermission } from '@/composables/usePermission'
import { useProductStore } from '@/stores/product'
import { byProduct, byCategory, money, sum, validOrders } from '@/utils/analytics'

const router = useRouter()
const { has } = usePermission()
const productStore = useProductStore()

const products = byProduct()
const categories = byCategory()

const totalQty = sum(products, 'qty')
const totalAmount = sum(products, 'amount')
const totalProfit = sum(products, 'profit')

// 商品模块（MySQL）的数据：用来在分析里直接看库存，点过去还能维护
onMounted(() => {
  productStore.loadAll().catch(() => {
    // 后端没起时分析页照常用订单数据，只是库存列显示「—」
  })
})

/** 按名称在商品档案里找库存，找不到返回 null */
function findProduct(name) {
  return productStore.products.find((p) => p.name === name) || null
}

function stockOf(name) {
  const p = findProduct(name)
  return p ? p.stock : null
}

function stockClass(name) {
  const p = findProduct(name)
  if (!p) return ''
  if (p.stock <= 0) return 'danger'
  if (p.stock < 10) return 'warn'
  return 'ok'
}

/** 热销但库存告急：销量前 12 名里库存 < 10 的 */
const stockRisk = computed(() =>
  products
    .slice(0, 12)
    .map((r) => ({ ...r, ...(findProduct(r.product) || {}) }))
    .filter((r) => r.stock !== undefined && r.stock < 10)
    .map((r) => ({ name: r.product, qty: r.qty, stock: r.stock, unit: r.unit || '件' }))
)

/** 商品档案里有库存、但这批订单里一件没卖的 */
const slowMovers = computed(() => {
  const sold = new Set(products.map((p) => p.product))
  return productStore.products
    .filter((p) => !sold.has(p.name) && p.stock > 20)
    .sort((a, b) => b.stock - a.stock)
    .slice(0, 5)
})

const kpis = computed(() => [
  {
    label: '累计销量',
    value: totalQty.toLocaleString('zh-CN') + ' 件',
    delta: `${products.length} 个在售品`,
    trend: 'up',
    sub: `平均每单 ${Math.round(totalQty / (validOrders().length || 1))} 件`,
    icon: Box,
    color: '#2f5fe0',
    bg: '#eef3ff',
    to: '/sales/product',
    perm: 'product:view'
  },
  {
    label: '销售额',
    value: money(totalAmount),
    delta: `${categories.length} 个品类`,
    trend: 'up',
    sub: '按订单明细汇总',
    icon: GoodsFilled,
    color: '#0f9d76',
    bg: '#e8f8f2',
    to: '/sales/order',
    perm: 'order:view'
  },
  {
    label: '毛利额',
    value: money(totalProfit),
    delta: `毛利率 ${Math.round((totalProfit / totalAmount) * 100)}%`,
    trend: 'up',
    sub: '售价 − 进价，未含运费',
    icon: Money,
    color: '#b45309',
    bg: '#fdf4e3',
    to: '/stats/sales',
    perm: 'stats:sales:view'
  },
  {
    label: '库存告急',
    value: `${stockRisk.value.length} 个`,
    delta: '销量靠前、库存 < 10',
    trend: stockRisk.value.length ? 'down' : 'up',
    sub: stockRisk.value.length ? '需通知生产/采购补货' : '主销品库存充足',
    icon: TrendCharts,
    color: '#c0392b',
    bg: '#fdeceb',
    to: '/sales/product?stock=low',
    perm: 'product:view'
  }
])

function drillProduct(name) {
  router.push(`/sales/product?q=${encodeURIComponent(name)}`)
}

function drillTickets(name) {
  router.push(`/service/ticket?q=${encodeURIComponent(name)}`)
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
}
.kpi-delta.up {
  color: #0f9d76;
  background: #e8f8f2;
}
.kpi-delta.down {
  color: #c0392b;
  background: #fdeceb;
}

.two-col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 14px;
  margin-bottom: 14px;
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

/* 品类 */
.cat-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.cat-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  margin-bottom: 5px;
}

.cat-name {
  color: #33415a;
  font-weight: 500;
}

.cat-amount {
  color: #5c6b82;
  font-variant-numeric: tabular-nums;
}

.cat-bar {
  height: 8px;
  border-radius: 999px;
  background: #f1f4fa;
  overflow: hidden;
  margin-bottom: 4px;
}

.cat-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #8fb4ff, #3b6cff);
}

.cat-qty {
  font-size: 11px;
  color: #97a3b6;
}

/* 风险 */
.risk-block + .risk-block {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #eef1f7;
}

.risk-title {
  font-size: 12px;
  color: #8b98ab;
  display: block;
  margin-bottom: 6px;
}

.risk-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.risk-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  border-bottom: 1px dashed #f2f5fa;
  cursor: pointer;
  font-size: 12.5px;
}
.risk-row:last-child {
  border-bottom: none;
}
.risk-row:hover .risk-name {
  color: #2f5fe0;
}

.risk-name {
  color: #273449;
  font-weight: 500;
  white-space: nowrap;
}

.risk-sub {
  flex: 1;
  min-width: 0;
  color: #97a3b6;
  font-size: 11.5px;
  overflow: hidden;
  text-overflow: ellipsis;
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

.rank-row:hover td {
  background: #f9fbff;
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
.w-op {
  width: 62px;
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

.prod-name {
  color: #273449;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 1px dashed transparent;
}
.prod-name:hover {
  color: #2f5fe0;
  border-bottom-color: #2f5fe0;
}

.prod-flag {
  display: block;
  font-size: 11px;
  color: #b45309;
  margin-top: 2px;
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

.mini-empty {
  padding: 12px 0;
  font-size: 12px;
  color: #a7b2c4;
  text-align: center;
}
</style>
