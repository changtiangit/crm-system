<template>
  <DataPage
    title="商品管理"
    :desc="pageDesc"
    entity-name="商品"
    create-text="新增商品"
    search-placeholder="搜索商品编码 / 名称 / 规格"
    :rows="store.products"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['code', 'name', 'spec', 'category']"
    :filters="filters"
    :query-filter-keys="['category', 'stock', 'state']"
    :filter-labels="{ category: '分类', stock: '库存', state: '状态' }"
    :filter-value-labels="{ stock: { low: '不足 10' }, state: { off: '已下架' } }"
    :query-matchers="queryMatchers"
    :create-row="createRow"
    :save-row="store.saveProduct"
    :remove-row="store.removeProduct"
    :can-create="has('product:add')"
    :can-edit="has('product:edit')"
    :can-delete="has('product:delete')"
    :op-width="opWidth"
  >
    <template #before>
      <div class="stat-bar">
        <div class="stat-item clickable" :class="{ active: activeStat === 'all' }" title="查看全部商品" @click="applyStat('all')">
          <span class="stat-value">{{ store.total }}</span>
          <span class="stat-label">商品总数</span>
        </div>
        <div class="stat-divider"></div>
        <!-- 分类展开成清单，点一类只看这一类；商品多起来以后不用在下拉里翻 -->
        <el-popover v-model:visible="catVisible" placement="bottom-start" :width="228" trigger="click" popper-class="cat-popper">
          <template #reference>
            <div class="stat-item clickable" :class="{ active: activeStat === 'category' }" title="按商品分类筛选">
              <span class="stat-value">{{ store.categories.length }}</span>
              <span class="stat-label">商品分类</span>
            </div>
          </template>
          <div class="cat-list">
            <div class="cat-row" :class="{ on: !route.query.category }" @click="pickCategory('')">
              <span>全部分类</span>
              <b>{{ store.total }}</b>
            </div>
            <div
              v-for="c in categoriesWithCount"
              :key="c.name"
              class="cat-row"
              :class="{ on: route.query.category === c.name }"
              @click="pickCategory(c.name)"
            >
              <span>{{ c.name }}</span>
              <b>{{ c.count }}</b>
            </div>
          </div>
        </el-popover>
        <div class="stat-divider"></div>
        <!-- 点一下直接筛出库存不足的商品，不用再去下拉里找 -->
        <div class="stat-item clickable" :class="{ active: activeStat === 'low' }" title="只看库存不足（< 10）的商品" @click="applyStat('low')">
          <span class="stat-value" :class="{ danger: store.lowStockCount > 0 }">{{ store.lowStockCount }}</span>
          <span class="stat-label">库存不足（&lt; 10）</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item clickable" :class="{ active: activeStat === 'off' }" title="只看已下架的商品" @click="applyStat('off')">
          <span class="stat-value">{{ offlineCount }}</span>
          <span class="stat-label">已下架</span>
        </div>
        <span class="stat-tip">{{ tipText }}</span>
      </div>
    </template>

    <!-- 行内操作：库存增减 / 上下架；两个权限都没有（如售后只读）时不定义这个插槽，操作列就不会空着 -->
    <template v-if="canStock || canOffline" #row-actions="{ row }">
      <!-- 没权限的按钮直接不显示，避免只读角色满屏灰色按钮 -->
      <el-button v-if="canStock" link type="primary" @click="openStock(row)">库存</el-button>

      <el-button
        v-if="canOffline"
        link
        :type="row.statusCode === 0 ? 'success' : 'warning'"
        @click="toggleStatus(row)"
      >
        {{ row.statusCode === 0 ? '上架' : '下架' }}
      </el-button>

    </template>
  </DataPage>

  <!-- 库存调整 -->
  <el-dialog v-model="stockVisible" title="库存调整" width="440px" align-center destroy-on-close>
    <div class="stock-head">
      <div class="stock-name">{{ stockRow?.name }}</div>
      <div class="stock-meta">
        {{ stockRow?.code }} · {{ stockRow?.spec || '无规格' }} · 单位：{{ stockRow?.unit }}
      </div>
    </div>

    <div class="stock-current">
      <span>当前库存</span>
      <b>{{ stockRow?.stock ?? 0 }}</b>
    </div>

    <div class="stock-line">
      <el-radio-group v-model="stockMode">
        <el-radio-button value="in">入库 +</el-radio-button>
        <el-radio-button value="out">出库 −</el-radio-button>
      </el-radio-group>
      <el-input-number v-model="stockQty" :min="1" :max="999999" :controls="false" class="stock-qty" />
    </div>

    <div class="stock-preview">
      调整后库存：
      <b :class="{ minus: nextStock < 0 }">{{ nextStock }}</b>
      <span v-if="nextStock < 0" class="stock-error">出库数量超过现有库存</span>
    </div>

    <template #footer>
      <el-button @click="stockVisible = false">取消</el-button>
      <el-button type="primary" :loading="stockSaving" :disabled="nextStock < 0 || !stockQty" @click="submitStock">
        确认{{ stockMode === 'in' ? '入库' : '出库' }}
      </el-button>
    </template>
  </el-dialog>

  <!-- 技术 / 维修资料 -->
  <el-dialog v-model="techVisible" title="技术 / 维修资料" width="560px" align-center destroy-on-close>
    <div class="stock-head">
      <div class="stock-name">{{ techRow?.name }}</div>
      <div class="stock-meta">{{ techRow?.code }} · {{ techRow?.category }}</div>
    </div>

    <el-input
      v-model="techNote"
      type="textarea"
      :rows="6"
      maxlength="500"
      show-word-limit
      :readonly="!canTech"
      placeholder="技术参数、安装要求、保养周期、常见故障与维修方法等"
    />
    <p class="tech-tip">
      {{ canTech ? '技术 / 维修资料由售后角色维护，保存后所有能查看商品的角色都能看到。' : '当前账号只能查看，技术 / 维修资料由售后角色维护。' }}
    </p>

    <template #footer>
      <el-button @click="techVisible = false">{{ canTech ? '取消' : '关闭' }}</el-button>
      <el-button v-if="canTech" type="primary" :loading="techSaving" @click="submitTech">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataPage from '@/components/DataPage.vue'
import { useProductStore } from '@/stores/product'
import { usePermission } from '@/composables/usePermission'

const store = useProductStore()
const { has } = usePermission()
// 进价属于成本数据：只有管理员、财务、销售总监可见；销售组长与顾问只看售价
const canSeeCost = computed(() => has('product:cost:view'))
const pageDesc = computed(() =>
  canSeeCost.value
    ? '订单模块下的商品资料库：编码、分类、规格、进价售价与库存都保存在数据库中；新增与下架、库存增减由生产角色维护，技术 / 维修资料由售后角色维护。'
    : '订单模块下的商品资料库：编码、分类、规格、售价与库存都保存在数据库中；进价与毛利由财务与销售总监维护，此处只显示销售价。'
)
const route = useRoute()
const router = useRouter()
const catVisible = ref(false)

/**
 * 顶部统计可点：库存不足 / 已下架 / 商品总数。
 * 条件写进地址栏（?stock=low、?state=off），这样刷新、分享、从统计页跳进来都能还原，
 * 表格上方会出现可关闭的条件标签，点「查看全部」回到 57 条。
 */
const queryMatchers = {
  category: (row, v) => String(row.category ?? '') === v,
  stock: (row, v) => (v === 'low' ? Number(row.stock) < 10 : true),
  state: (row, v) => (v === 'off' ? Number(row.statusCode) === 0 : true)
}

const activeStat = computed(() => {
  if (route.query.category) return 'category'
  if (route.query.stock === 'low') return 'low'
  if (route.query.state === 'off') return 'off'
  return 'all'
})

/** 分类清单带数量，一眼看出哪类商品多 */
const categoriesWithCount = computed(() =>
  store.categories
    .map((name) => ({ name, count: store.products.filter((p) => p.category === name).length }))
    .filter((c) => c.count > 0)
    .sort((a, b) => b.count - a.count)
)

/** 顶部的几个快捷筛选一次只生效一个，避免筛出空列表还让用户猜是哪条卡住了 */
function applyStat(kind) {
  const query = { ...route.query }
  delete query.category
  delete query.stock
  delete query.state
  if (kind === 'low') query.stock = 'low'
  if (kind === 'off') query.state = 'off'
  router.replace({ path: route.path, query })
}

function pickCategory(name) {
  catVisible.value = false
  const query = { ...route.query }
  delete query.stock
  delete query.state
  if (name) query.category = name
  else delete query.category
  router.replace({ path: route.path, query })
}

const canEdit = computed(() => has('product:edit'))
const canStock = computed(() => has('product:stock'))
const canOffline = computed(() => has('product:offline'))
const canTech = computed(() => has('product:tech'))

const offlineCount = computed(() => store.products.filter((p) => p.statusCode === 0).length)

const tipText = computed(() => {
  if (canStock.value) return '库存可入库 / 出库，下架后该商品不再对外销售'
  if (canTech.value) return '可维护商品的技术参数与维修方法'
  if (canEdit.value) return '编码不可重复；价格、库存支持两位小数与整数'
  return '当前账号仅可查看商品，新增 / 下架 / 库存由生产角色负责'
})

/** 操作列宽度随可用按钮增减 */
const opWidth = computed(() => 144 + (canStock.value ? 44 : 0) + (canOffline.value ? 44 : 0))

const columns = [
  { prop: 'code', label: '商品编码', width: 84 },
  { prop: 'name', label: '商品名称', type: 'strong', minWidth: 112 },
  { prop: 'category', label: '商品分类', type: 'tag', width: 92 },
  {
    prop: 'spec',
    label: '规格 / 单位',
    minWidth: 88,
    formatter: (row) => (row.spec ? `${row.spec} / ${row.unit}` : row.unit)
  },
  {
    prop: 'stock',
    label: '库存',
    width: 68,
    align: 'right',
    type: 'tag',
    tagType: (v, row) => (row?.stockLabel === '缺货' ? 'danger' : row?.stockLabel === '偏低' ? 'warning' : 'success')
  },
  canSeeCost.value
    ? {
        prop: 'priceText',
        label: '进价 / 售价',
        minWidth: 120,
        align: 'right',
        formatter: (row) => `¥${Number(row.purchasePrice).toFixed(2)} / ¥${Number(row.salePrice).toFixed(2)}`
      }
    : {
        prop: 'salePrice',
        label: '销售价',
        width: 96,
        align: 'right',
        formatter: (row) => `¥${Number(row.salePrice).toFixed(2)}`
      },
  {
    prop: 'status',
    label: '状态',
    width: 66,
    type: 'tag',
    tagType: { 启用: 'success', 停用: 'info' }
  },
  {
    prop: 'techLabel',
    label: '技术资料',
    width: 84,
    type: 'tag',
    tagType: { 已录入: 'success', 待补充: 'info' },
    // 点标签直接打开资料弹窗（售后可编辑，其它角色只读）
    onClick: (row) => openTech(row)
  }
]

const UNITS = ['个', '套', '支', '只', '瓶', '桶', '盒', '包', '卷', '条', '米', '对', '罐']
const STATUS = ['启用', '停用']

const formFields = computed(() => [
  { prop: 'code', label: '商品编码', required: true, maxlength: 32, placeholder: '如 SP0058' },
  { prop: 'name', label: '商品名称', required: true, maxlength: 100 },
  {
    prop: 'category',
    label: '商品分类',
    type: 'select',
    options: store.categories,
    required: true,
    filterable: true,
    allowCreate: true
  },
  { prop: 'spec', label: '规格型号', maxlength: 100 },
  { prop: 'unit', label: '单位', type: 'select', options: UNITS, required: true },
  // 没有进价权限的账号（销售）表单里也不出现这一项，避免误填成本
  ...(canSeeCost.value ? [{ prop: 'purchasePrice', label: '参考进价', type: 'number', precision: 2 }] : []),
  { prop: 'salePrice', label: '销售价', type: 'number', precision: 2, required: true },
  { prop: 'stock', label: '库存数量', type: 'number', precision: 0 },
  { prop: 'status', label: '商品状态', type: 'select', options: STATUS, required: true },
  { prop: 'remark', label: '备注', type: 'textarea' }
])

const filters = computed(() => [
  { prop: 'category', label: '全部分类', options: store.categories },
  { prop: 'stockLabel', label: '全部库存状态', options: ['充足', '偏低', '缺货'] },
  { prop: 'status', label: '全部状态', options: STATUS }
])

/** 新编码顺延现有最大编号，如 SP0058 */
function nextCode() {
  const max = store.products.reduce((acc, p) => {
    const n = Number(String(p.code).replace(/\D/g, ''))
    return Number.isFinite(n) ? Math.max(acc, n) : acc
  }, 0)
  return 'SP' + String(max + 1).padStart(4, '0')
}

const createRow = () => ({
  code: nextCode(),
  name: '',
  category: store.categories[0] || '',
  spec: '',
  unit: '个',
  purchasePrice: 0,
  salePrice: 0,
  stock: 0,
  status: '启用',
  remark: ''
})

// ---------- 库存调整 ----------
const stockVisible = ref(false)
const stockRow = ref(null)
const stockMode = ref('in')
const stockQty = ref(1)
const stockSaving = ref(false)

const nextStock = computed(() => {
  const current = Number(stockRow.value?.stock ?? 0)
  const qty = Number(stockQty.value) || 0
  return stockMode.value === 'in' ? current + qty : current - qty
})

function openStock(row) {
  stockRow.value = row
  stockMode.value = 'in'
  stockQty.value = 1
  stockVisible.value = true
}

async function submitStock() {
  if (nextStock.value < 0) return
  const delta = stockMode.value === 'in' ? stockQty.value : -stockQty.value
  stockSaving.value = true
  try {
    await store.adjustStock(stockRow.value.id, delta)
    ElMessage.success(
      `${stockRow.value.name} 已${stockMode.value === 'in' ? '入库' : '出库'} ${stockQty.value} ${stockRow.value.unit}`
    )
    stockVisible.value = false
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    stockSaving.value = false
  }
}

// ---------- 上架 / 下架 ----------
async function toggleStatus(row) {
  const toOffline = row.statusCode !== 0
  try {
    await ElMessageBox.confirm(
      toOffline
        ? `确定下架「${row.name}」吗？下架后该商品不再对外销售，库存与资料仍保留，可随时重新上架。`
        : `确定上架「${row.name}」吗？上架后该商品可正常销售。`,
      toOffline ? '商品下架' : '商品上架',
      { confirmButtonText: toOffline ? '下架' : '上架', cancelButtonText: '取消', type: toOffline ? 'warning' : 'info' }
    )
  } catch {
    return
  }
  try {
    await store.changeStatus(row.id, toOffline ? 0 : 1)
    ElMessage.success(toOffline ? '商品已下架' : '商品已上架')
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

// ---------- 技术 / 维修资料 ----------
const techVisible = ref(false)
const techRow = ref(null)
const techNote = ref('')
const techSaving = ref(false)

function openTech(row) {
  techRow.value = row
  techNote.value = row.techNote || ''
  techVisible.value = true
}

async function submitTech() {
  techSaving.value = true
  try {
    await store.saveTechNote(techRow.value.id, techNote.value)
    ElMessage.success('技术 / 维修资料已保存')
    techVisible.value = false
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    techSaving.value = false
  }
}

onMounted(() => {
  // 每次进入都取最新数据，避免其他账号刚改过看不到
  store.loadAll(true)
})
</script>

<style scoped>
.stat-bar {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  padding: 12px 18px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: linear-gradient(90deg, #f2f7ff, #f8faff);
  border: 1px solid #dde8ff;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

/* 可点的统计项：悬停有反馈，选中后整块变蓝，让「现在是筛过的」一眼看出来 */
.stat-item.clickable {
  cursor: pointer;
  padding: 5px 9px;
  margin: -5px -9px;
  border-radius: 8px;
  user-select: none;
  transition: background 0.16s, box-shadow 0.16s;
}

.stat-item.clickable:hover {
  background: #f2f6ff;
}

.stat-item.clickable.active {
  background: #eaf1ff;
  box-shadow: inset 0 0 0 1px #c9d9ff;
}

.stat-item.clickable.active .stat-value {
  color: #2f5fe0;
}

.stat-item.clickable.active .stat-value.danger {
  color: #d97706;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #273449;
  line-height: 1;
}

.stat-value.danger {
  color: #d97706;
}

.stat-label {
  font-size: 12.5px;
  color: #8493a8;
}

.stat-divider {
  width: 1px;
  height: 22px;
  background: #dde8ff;
}

.stat-tip {
  margin-left: auto;
  font-size: 12.5px;
  color: #8493a8;
}

/* 库存 / 技术资料弹窗 */
.stock-head {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 14px;
}

.stock-name {
  font-size: 15px;
  font-weight: 600;
  color: #273449;
}

.stock-meta {
  margin-top: 4px;
  font-size: 12.5px;
  color: #8493a8;
}

.stock-current {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  font-size: 13.5px;
  color: #5a6a83;
  margin-bottom: 14px;
}

.stock-current b {
  font-size: 22px;
  color: #273449;
  font-variant-numeric: tabular-nums;
}

.stock-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stock-qty {
  width: 140px;
}

.stock-preview {
  margin-top: 14px;
  font-size: 13.5px;
  color: #5a6a83;
}

.stock-preview b {
  font-size: 18px;
  color: #1a7f4b;
  font-variant-numeric: tabular-nums;
}

.stock-preview b.minus {
  color: #d14343;
}

.stock-error {
  margin-left: 10px;
  font-size: 12.5px;
  color: #d14343;
}

.tech-tip {
  margin: 10px 0 0;
  font-size: 12.5px;
  color: #8493a8;
}
</style>

<style>
/* 分类清单弹层被 teleport 到 body，scoped 样式作用不到，这里用全局类名 */
.cat-popper.el-popover {
  padding: 8px;
}

.cat-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 320px;
  overflow-y: auto;
}

.cat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 10px;
  border-radius: 7px;
  font-size: 13px;
  color: #33404f;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.cat-row:hover {
  background: #f2f6ff;
}

.cat-row b {
  font-size: 12.5px;
  font-weight: 600;
  color: #8493a8;
}

.cat-row.on {
  background: #eaf1ff;
  color: #2f5fe0;
  font-weight: 600;
}

.cat-row.on b {
  color: #2f5fe0;
}
</style>
