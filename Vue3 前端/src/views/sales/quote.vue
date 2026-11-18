<template>
  <DataPage
    show-scope
    title="报价单"
    desc="商机谈得差不多就先出报价单：审批通过才能转销售订单，有效期一过自动失效，避免拿旧价格签单"
    entity-name="报价单"
    create-text="新建报价单"
    search-placeholder="搜索报价单号 / 客户 / 联系人"
    :rows="rows"
    :query-filter-keys="['customer', 'owner']"
    :filter-labels="{ customer: '客户', owner: '负责人' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['quoteNo', 'customer', 'contact', 'owner']"
    :filters="filters"
    :create-row="createRow"
    :save-row="moduleData.saveRow"
    :remove-row="moduleData.removeRow"
    :refresh-rows="moduleData.refreshRows"
    :can-create="has('quote:add')"
    :can-edit="has('quote:edit')"
    :can-delete="has('quote:delete')"
    :op-width="152"
    label-width="96px"
    :show-index="false"
    @field-change="onFieldChange"
  >
    <template #row-actions="{ row }">
      <el-button v-if="row.status === '已通过'" link type="success" @click="toOrder(row)">转订单</el-button>
    </template>
  </DataPage>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DataPage from '@/components/DataPage.vue'
import { quotes, customers } from '@/mock/data'
import { useModuleData } from '@/composables/useModuleData'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useUserStore } from '@/stores/user'
import { useCustomerNav, withCustomer } from '@/utils/links'
import request from '@/utils/request'

const router = useRouter()
const { has } = usePermission()
const { isAll, owners: scopeOwners, canSee } = useDataScope()
const userStore = useUserStore()
const { openCustomer } = useCustomerNav()
// 报价单跟随负责人：销售只看到自己出的报价
const rows = reactive(quotes.filter((q) => canSee(q.owner)).map((q) => ({ ...q })))
const moduleData = useModuleData('quotes', rows, quotes)
onMounted(moduleData.initialize)

const CUSTOMER_NAMES = customers.filter((c) => canSee(c.owner)).map((c) => c.name)
const STATUSES = ['草稿', '待审批', '已通过', '已转订单', '已失效']
const OWNERS = isAll.value
  ? [...new Set([...scopeOwners.value, '张明', '李思', '周婷'])]
  : scopeOwners.value

const columns = [
  { prop: 'quoteNo', label: '报价单号', width: 140 },
  { prop: 'customer', label: '客户名称', type: 'link', minWidth: 170, onClick: (row) => openCustomer(row.customer) },
  { prop: 'contact', label: '联系人', width: 82 },
  { prop: 'amount', label: '报价金额', type: 'money', width: 102, align: 'right' },
  {
    prop: 'status',
    label: '状态',
    width: 96,
    type: 'tag',
    tagType: { 草稿: 'info', 待审批: 'warning', 已通过: 'success', 已转订单: 'primary', 已失效: 'info' }
  },
  {
    prop: 'quoteDate',
    label: '报价 / 有效期',
    width: 112,
    type: 'stack',
    formatter: (row) => ({ main: row.quoteDate, sub: `至 ${row.validTo}` })
  },
  { prop: 'owner', label: '负责人', width: 70 }
]

const formFields = [
  { prop: 'quoteNo', label: '报价单号', required: true, maxlength: 30 },
  { prop: 'customer', label: '客户名称', type: 'select', options: CUSTOMER_NAMES, filterable: true, required: true },
  {
    prop: 'contact',
    label: '对接联系人',
    required: true,
    placeholder: '选客户后自动带出，可改成本次对接人'
  },
  { prop: 'amount', label: '报价金额', type: 'number', min: 0, precision: 2, required: true },
  { prop: 'status', label: '状态', type: 'select', options: STATUSES, required: true },
  { prop: 'quoteDate', label: '报价日期', type: 'date', required: true },
  { prop: 'validTo', label: '有效期至', type: 'date', required: true },
  {
    prop: 'owner',
    label: '负责人',
    type: 'select',
    options: OWNERS,
    required: true,
    disabled: () => !isAll.value && scopeOwners.value.length <= 1
  }
]

const filters = [{ prop: 'status', label: '报价状态', options: STATUSES }]

const today = () => new Date().toISOString().slice(0, 10)

const createRow = () => ({
  quoteNo: 'QT' + today().replace(/-/g, '') + String(Math.floor(Math.random() * 900) + 100),
  customer: CUSTOMER_NAMES[0],
  contact: customers[0]?.contact || '',
  amount: 10000,
  status: '草稿',
  quoteDate: today(),
  validTo: today(),
  owner: userStore.userInfo?.name || OWNERS[0] || ''
})

/** 选了客户就带出该客户的默认联系人，省一次输入 */
function onFieldChange(prop, value, model) {
  if (prop !== 'customer') return
  const hit = customers.find((c) => c.name === value)
  if (hit?.contact) model.contact = hit.contact
}

/** 报价通过后一键转销售订单：真落一张订单，后端会同步生成应收与待发货单 */
async function toOrder(row) {
  const hit = customers.find((c) => c.name === row.customer)
  const today = new Date().toLocaleDateString('sv-SE')
  const orderNo = 'SO' + today.replace(/-/g, '') + 'Q' + String(row.quoteNo).slice(-3)
  try {
    await request.post('/business/orders', {
      orderNo,
      customer: row.customer,
      contact: row.contact || hit?.contact || '',
      phone: hit?.phone || '',
      amount: Number(row.amount) || 0,
      status: '待付款',
      orderDate: today,
      owner: row.owner
    })
    await moduleData.saveRow(row.id, { ...row, status: '已转订单' })
    ElMessage.success(`已按报价单 ${row.quoteNo} 生成销售订单 ${orderNo}，应收单已自动建好，补齐明细后即可安排发货`)
    router.push(withCustomer('/sales/order', row.customer))
  } catch (e) {
    ElMessage.error(e?.message || '转订单失败，请稍后重试')
  }
}
</script>
