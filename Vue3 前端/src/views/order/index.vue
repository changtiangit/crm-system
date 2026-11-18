<template>
  <DataPage
    show-scope
    title="销售订单"
    :desc="pageDesc"
    entity-name="订单"
    create-text="新建订单"
    search-placeholder="搜索订单号 / 客户 / 联系人"
    :rows="rows"
    :query-filter-keys="['customer', 'owner', 'month']"
    :filter-labels="{ customer: '客户', owner: '归属销售', month: '下单月份' }"
    :query-matchers="{ month: (row, v) => String(row.orderDate).startsWith(v) }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['orderNo', 'customer', 'contact', 'phone', 'owner']"
    :filters="filters"
    :create-row="createRow"
    :save-row="saveRow"
    :remove-row="removeRow"
    :refresh-rows="loadRows"
    :row-click="(row) => openOrder(row.orderNo)"
    :can-create="has('order:add')"
    :can-edit="has('order:edit')"
    :can-delete="has('order:delete')"
    :op-width="132"
    label-width="104px"
    dialog-width="860px"
    @field-change="onFieldChange"
  >
    <!-- 商品清单：新增 / 编辑订单时直接维护明细，金额由明细汇总 -->
    <template #form-extra="{ model }">
      <div class="items-editor">
        <div class="ie-head">
          <div class="ie-title-wrap">
            <span class="ie-title">商品清单</span>
            <span class="ie-tip">选商品自动带出售价，金额按「单价 × 数量」汇总，订单金额随明细更新</span>
          </div>
          <el-button size="small" :icon="Plus" @click="addItem(model)">添加商品</el-button>
        </div>

        <div v-if="model.items?.length" class="ie-table">
          <div class="ie-tr ie-th">
            <span class="ie-c-prod">商品</span>
            <span class="ie-c-qty">数量</span>
            <span class="ie-c-price">单价</span>
            <span class="ie-c-amt">金额</span>
            <span class="ie-c-op"></span>
          </div>
          <div v-for="(it, i) in model.items" :key="i" class="ie-tr">
            <span class="ie-c-prod">
              <el-select
                v-model="it.product"
                filterable
                size="small"
                placeholder="搜索商品名称"
                @change="(v) => onPickProduct(model, i, v)"
              >
                <el-option
                  v-for="p in productOptions(model, i)"
                  :key="p.id"
                  :label="p.name"
                  :value="p.name"
                >
                  <span class="opt-name">{{ p.name }}</span>
                  <span class="opt-sub">{{ p.category }} · 库存 {{ p.stock }}</span>
                </el-option>
              </el-select>
            </span>
            <span class="ie-c-qty">
              <el-input-number
                v-model="it.qty"
                :min="1"
                :controls="false"
                size="small"
                @change="recalc(model)"
              />
            </span>
            <span class="ie-c-price">
              <el-input-number
                v-model="it.price"
                :min="0"
                :precision="2"
                :controls="false"
                size="small"
                @change="recalc(model)"
              />
            </span>
            <span class="ie-c-amt">{{ money(lineAmount(it)) }}</span>
            <span class="ie-c-op">
              <el-button link type="danger" @click="removeItem(model, i)">删除</el-button>
            </span>
          </div>
        </div>
        <div v-else class="ie-empty">
          还没有商品明细。不录明细也能保存，此时订单金额就是上面手填的数。
        </div>

        <div v-if="model.items?.length" class="ie-sum">
          <span>{{ model.items.length }} 行 · 共 {{ totalQty(model) }} 件</span>
          <span>合计 <b>{{ money(model.amount) }}</b></span>
        </div>
      </div>
    </template>
  </DataPage>
</template>

<script setup>
import { reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import DataPage from '@/components/DataPage.vue'
import { customers as seedCustomers } from '@/mock/data'
import request from '@/utils/request'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useUserStore } from '@/stores/user'
import { useCustomerNav, useOrderNav } from '@/utils/links'
import { money } from '@/utils/analytics'

const { has } = usePermission()
const canSeeProfit = computed(() => has('order:profit:view'))
const pageDesc = computed(() =>
  canSeeProfit.value
    ? '点单号可进详情看这张合同的完整清单（商品、数量、单价、金额、毛利），点客户名可看这家客户的 360 全景'
    : '点单号可进详情看这张合同的完整清单（商品、数量、单价、金额），点客户名可看这家客户的 360 全景；成本与毛利由财务与销售总监查看'
)
const { isAll, owners: scopeOwners } = useDataScope()
const userStore = useUserStore()
const { openCustomer } = useCustomerNav()
const { openOrder } = useOrderNav()
const rows = reactive([])
const customers = reactive([])
const products = reactive([])

async function loadRows() {
  const [items, customerRows, productRows] = await Promise.all([
    request.get('/business/orders'),
    request.get('/business/customers'),
    request.get('/products')
  ])
  rows.splice(0, rows.length, ...(items || []))
  // 没有可见客户时不要拿演示数据兜底，否则等于把别家的客户名漏出来
  customers.splice(0, customers.length, ...(customerRows?.length ? customerRows : isAll.value ? seedCustomers : []))
  // 明细里的商品从商品资料里选，售价、进价直接带出来，避免手填对不上
  products.splice(0, products.length, ...(productRows || []))
}

async function saveRow(id, row) {
  const items = row.items || []
  if (items.some((it) => !it.product)) {
    ElMessage.warning('有明细还没选商品，请先选好或删掉这一行')
    throw new Error('明细缺少商品')
  }
  const payload = { ...row, items }
  const saved = await (id
    ? request.put(`/business/orders/${id}`, payload)
    : request.post('/business/orders', payload))
  await loadRows()
  return saved
}

async function removeRow(id) {
  await request.delete(`/business/orders/${id}`)
  await loadRows()
}
onMounted(loadRows)

/** 一行明细的金额：单价 × 数量 */
function lineAmount(it) {
  return Number(it.qty || 0) * Number(it.price || 0)
}

function totalQty(model) {
  return (model.items || []).reduce((s, it) => s + Number(it.qty || 0), 0)
}

/** 明细一变就重算行金额和订单总额，保证表头金额 = 清单合计 */
function recalc(model) {
  const items = model.items || []
  items.forEach((it) => (it.amount = lineAmount(it)))
  if (items.length) model.amount = items.reduce((s, it) => s + it.amount, 0)
  return model.amount
}

function addItem(model) {
  if (!Array.isArray(model.items)) model.items = []
  model.items.push({ product: '', category: '', qty: 1, price: 0, cost: 0, amount: 0 })
}

/** 选了商品：带出分类、售价、进价，金额重算 */
function onPickProduct(model, index, name) {
  const p = products.find((x) => x.name === name)
  const it = model.items[index]
  if (!p || !it) return
  it.category = p.category
  it.price = Number(p.salePrice) || 0
  it.cost = Number(p.purchasePrice) || 0
  recalc(model)
}

function removeItem(model, index) {
  model.items.splice(index, 1)
  // 删掉最后一行时保留当前金额，避免突然变 0 让人以为算错
  recalc(model)
}

/** 下拉里排除其他行已选过的商品：同一张订单同一商品只留一行（库里也是这个唯一约束） */
function productOptions(model, index) {
  const used = new Set(
    (model.items || [])
      .map((it, i) => (i === index ? null : it.product))
      .filter(Boolean)
  )
  return products.filter((p) => !used.has(p.name))
}

const STATUS = ['待付款', '部分回款', '已付款', '已完成', '已取消']
const CUSTOMER_NAMES = computed(() => customers.map((c) => c.name))
const OWNERS = computed(() =>
  isAll.value ? [...new Set([...scopeOwners.value, '张明', '李思', '周婷'])] : scopeOwners.value
)

const columns = [
  {
    prop: 'orderNo',
    label: '订单编号',
    width: 156,
    type: 'stack',
    // 点单号进详情，完整看这张合同的商品清单；下面补一行构成，不点也知道卖的是什么
    onClick: (row) => openOrder(row.orderNo),
    formatter: (row) => ({
      main: row.orderNo,
      sub: row.items ? `${row.items.length} 种商品 · ${row.items.reduce((s, l) => s + l.qty, 0)} 件` : '',
      hint: '清单'
    })
  },
  {
    prop: 'customer',
    label: '客户名称',
    type: 'link',
    minWidth: 176,
    onClick: (row) => openCustomer(row.customer)
  },
  {
    prop: 'contact',
    label: '对接联系人',
    width: 126,
    type: 'stack',
    formatter: (row) => ({ main: row.contact, sub: row.phone })
  },
  { prop: 'amount', label: '订单金额', type: 'money', width: 102, align: 'right' },
  {
    prop: 'status',
    label: '订单状态',
    width: 96,
    type: 'tag',
    tagType: { 待付款: 'warning', 部分回款: 'primary', 已付款: 'success', 已完成: 'success', 已取消: 'info' }
  },
  { prop: 'orderDate', label: '下单日期', width: 104 },
  { prop: 'owner', label: '归属销售', width: 84 }
]

const formFields = computed(() => [
  { prop: 'orderNo', label: '订单编号', required: true },
  { prop: 'customer', label: '客户单位', type: 'select', options: CUSTOMER_NAMES.value, required: true },
  {
    prop: 'contact',
    label: '对接联系人',
    required: true,
    placeholder: '选客户后自动带出，可改成本次实际对接人'
  },
  {
    prop: 'phone',
    label: '联系电话',
    required: true,
    placeholder: '手机或座机，选客户后自动带出',
    maxlength: 20
  },
  {
    prop: 'amount',
    label: '订单金额',
    type: 'number',
    required: true,
    // 有商品明细时金额由明细汇总，手填会被覆盖，直接置灰更清楚
    disabled: (m) => (m.items || []).length > 0,
    placeholder: '有商品明细时按明细汇总'
  },
  { prop: 'status', label: '订单状态', type: 'select', options: STATUS, required: true },
  { prop: 'orderDate', label: '下单日期', type: 'date', required: true },
  {
    prop: 'owner',
    label: '归属销售',
    type: 'select',
    options: OWNERS.value,
    required: true,
    disabled: () => !isAll.value && scopeOwners.value.length <= 1
  }
])
const filters = [{ prop: 'status', label: '全部状态', options: STATUS }]

function today() {
  return new Date().toISOString().slice(0, 10)
}

const createRow = () => ({
  orderNo: 'SO' + today().replace(/-/g, '') + String(Math.floor(Math.random() * 900) + 100),
  customer: CUSTOMER_NAMES.value[0] || '',
  contact: customers[0]?.contact || '',
  phone: customers[0]?.phone || '',
  amount: 0,
  status: '待付款',
  orderDate: today(),
  owner: userStore.userInfo?.name || OWNERS.value[0] || '',
  items: []
})

/** 选了客户就带出该客户的默认联系人，销售也可改成这次实际对接的人 */
function onFieldChange(prop, value, model) {
  if (prop !== 'customer') return
  const hit = customers.find((c) => c.name === value)
  if (hit?.contact) model.contact = hit.contact
  if (hit?.phone) model.phone = hit.phone
}
</script>

<style scoped>
/* 弹窗里的商品清单编辑器：行式布局，宽度跟着 860px 的弹窗走 */
.items-editor {
  margin-top: 2px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fbfcff;
}

.ie-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.ie-title {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #1f2b3d;
  margin-bottom: 3px;
}

.ie-tip {
  font-size: 11.5px;
  color: #97a3b6;
}

.ie-tr {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 78px 96px 96px 48px;
  gap: 8px;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px dashed #eef1f7;
}

.ie-tr:last-child {
  border-bottom: none;
}

.ie-th {
  padding: 0 0 6px;
  font-size: 11.5px;
  color: #8b98ab;
}

.ie-c-amt {
  text-align: right;
  font-size: 12.5px;
  font-weight: 600;
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

.ie-c-op {
  text-align: right;
}

.ie-sum {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #eef1f7;
  font-size: 12.5px;
  color: #6b7a90;
}

.ie-sum b {
  font-size: 14px;
  color: #2f5fe0;
  font-variant-numeric: tabular-nums;
}

.ie-empty {
  padding: 14px 0;
  text-align: center;
  font-size: 12.5px;
  color: #a7b2c4;
}

.items-editor :deep(.el-select),
.items-editor :deep(.el-input-number) {
  width: 100%;
}

.opt-name {
  color: #33415a;
}

.opt-sub {
  float: right;
  margin-left: 12px;
  font-size: 11.5px;
  color: #97a3b6;
}
</style>
