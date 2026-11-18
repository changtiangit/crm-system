<template>
  <DataPage
    show-scope
    title="收款记录"
    desc="每笔到账都由财务在这里登记：收款方式、对应订单、金额，系统再自动算出还有多少没收回来"
    entity-name="收款单"
    create-text="登记收款"
    search-placeholder="搜索收款单号 / 客户 / 订单号"
    :rows="rows"
    :query-filter-keys="['customer']"
    :filter-labels="{ customer: '客户' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['receiptNo', 'customer', 'orderNo', 'note']"
    :filters="filters"
    :create-row="createRow"
    :save-row="moduleData.saveRow"
    :remove-row="moduleData.removeRow"
    :refresh-rows="moduleData.refreshRows"
    :can-create="has('receipt:add')"
    :can-edit="has('receipt:add')"
    :can-delete="false"
    :show-index="false"
    :op-width="100"
    label-width="96px"
    @field-change="onFieldChange"
  />
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import DataPage from '@/components/DataPage.vue'
import { receipts, orders } from '@/mock/data'
import { useModuleData } from '@/composables/useModuleData'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { visibleCustomerNames } from '@/utils/dataAccess'
import { useCustomerNav, useOrderNav } from '@/utils/links'

const { has } = usePermission()
const { isAll } = useDataScope()
const { openCustomer } = useCustomerNav()
const { openOrder } = useOrderNav()
// 收款记录按客户可见范围过滤
const visibleNames = visibleCustomerNames()
const visibleOrders = orders.filter((o) => isAll.value || visibleNames.has(o.customer))
const rows = reactive(
  receipts
    .filter((r) => isAll.value || visibleNames.has(r.customer))
    .map((r) => ({ ...r }))
)
const moduleData = useModuleData('receipts', rows, receipts)
onMounted(moduleData.initialize)

const CUSTOMER_NAMES = [...new Set(visibleOrders.map((o) => o.customer))]
const ORDER_NOS = visibleOrders.map((o) => o.orderNo)
const METHODS = ['银行转账', '承兑汇票', '现金', '微信收款', '支付宝']

const columns = [
  { prop: 'receiptNo', label: '收款单号', width: 140 },
  { prop: 'customer', label: '客户名称', type: 'link', minWidth: 160, onClick: (row) => openCustomer(row.customer) },
  { prop: 'orderNo', label: '关联订单', width: 140, type: 'link', onClick: (row) => openOrder(row.orderNo) },
  { prop: 'amount', label: '收款金额', type: 'money', width: 110, align: 'right' },
  { prop: 'method', label: '收款方式', width: 90 },
  { prop: 'receiptDate', label: '收款日期', width: 106 },
  { prop: 'note', label: '备注', minWidth: 120 }
]

const formFields = [
  { prop: 'receiptNo', label: '收款单号', required: true, maxlength: 30 },
  { prop: 'customer', label: '客户名称', type: 'select', options: CUSTOMER_NAMES, filterable: true, required: true },
  { prop: 'orderNo', label: '关联订单', type: 'select', options: ORDER_NOS, filterable: true, required: true },
  { prop: 'amount', label: '收款金额', type: 'number', min: 0, precision: 2, required: true },
  { prop: 'method', label: '收款方式', type: 'select', options: METHODS, required: true },
  { prop: 'receiptDate', label: '收款日期', type: 'date', required: true },
  { prop: 'operator', label: '经办人', required: true, maxlength: 20 },
  { prop: 'note', label: '备注', type: 'textarea', maxlength: 100 }
]

const filters = [{ prop: 'method', label: '收款方式', options: METHODS }]

const today = () => new Date().toISOString().slice(0, 10)

const createRow = () => ({
  receiptNo: 'SK' + today().replace(/-/g, '') + String(Math.floor(Math.random() * 900) + 100),
  customer: CUSTOMER_NAMES[0],
  orderNo: ORDER_NOS[0],
  amount: 0,
  method: '银行转账',
  receiptDate: today(),
  operator: '刘洋',
  note: ''
})

function onFieldChange(prop, value, model) {
  if (prop !== 'orderNo') return
  const hit = orders.find((o) => o.orderNo === value)
  if (hit?.customer) model.customer = hit.customer
}
</script>
