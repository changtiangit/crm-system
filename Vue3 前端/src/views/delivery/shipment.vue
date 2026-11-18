<template>
  <DataPage
    show-scope
    title="发货记录"
    desc="订单确认后由生产按单发货：登记物流公司与运单号，客户催货时直接报单号，不用再翻聊天记录"
    entity-name="发货单"
    create-text="登记发货"
    search-placeholder="搜索发货单号 / 订单号 / 客户 / 运单号"
    :rows="rows"
    :query-filter-keys="['customer']"
    :filter-labels="{ customer: '客户' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['shipNo', 'orderNo', 'customer', 'trackingNo']"
    :filters="filters"
    :create-row="createRow"
    :save-row="moduleData.saveRow"
    :remove-row="moduleData.removeRow"
    :refresh-rows="moduleData.refreshRows"
    :can-create="has('delivery:add')"
    :can-edit="has('delivery:add')"
    :can-delete="false"
    :op-width="86"
    label-width="96px"
    :show-index="false"
    @field-change="onFieldChange"
  />
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import DataPage from '@/components/DataPage.vue'
import { shipments, orders } from '@/mock/data'
import { useModuleData } from '@/composables/useModuleData'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { visibleCustomerNames } from '@/utils/dataAccess'
import { useCustomerNav, useOrderNav } from '@/utils/links'

const { has } = usePermission()
const { isAll } = useDataScope()
const { openCustomer } = useCustomerNav()
const { openOrder } = useOrderNav()
// 发货单没有销售负责人，按「客户是否在可见范围」过滤
const visibleNames = visibleCustomerNames()
const visibleOrders = orders.filter((o) => isAll.value || visibleNames.has(o.customer))
const rows = reactive(
  shipments
    .filter((s) => isAll.value || visibleNames.has(s.customer))
    .map((s) => ({ ...s }))
)
const moduleData = useModuleData('shipments', rows, shipments)
onMounted(moduleData.initialize)

const ORDER_NOS = visibleOrders.map((o) => o.orderNo)
const CARRIERS = ['顺丰速运', '德邦物流', '专线物流', '京东物流', '客户自提']
const STATUSES = ['待发货', '已发出', '已签收']

const columns = [
  { prop: 'shipNo', label: '发货单号', width: 140 },
  { prop: 'orderNo', label: '关联订单', width: 140, type: 'link', onClick: (row) => openOrder(row.orderNo) },
  { prop: 'customer', label: '客户名称', type: 'link', minWidth: 156, onClick: (row) => openCustomer(row.customer) },
  { prop: 'qty', label: '件数', width: 52, align: 'right' },
  { prop: 'carrier', label: '物流公司', width: 90 },
  { prop: 'trackingNo', label: '运单号', width: 136 },
  { prop: 'shipDate', label: '发货日期', width: 106 },
  {
    prop: 'status',
    label: '状态',
    width: 86,
    type: 'tag',
    tagType: { 待发货: 'warning', 已发出: 'primary', 已签收: 'success' }
  }
]

const formFields = [
  { prop: 'shipNo', label: '发货单号', required: true, maxlength: 30 },
  { prop: 'orderNo', label: '关联订单', type: 'select', options: ORDER_NOS, filterable: true, required: true },
  { prop: 'customer', label: '客户名称', required: true, maxlength: 40 },
  { prop: 'qty', label: '件数', type: 'number', min: 1, required: true },
  { prop: 'carrier', label: '物流公司', type: 'select', options: CARRIERS, allowCreate: true, filterable: true, required: true },
  { prop: 'trackingNo', label: '运单号', maxlength: 40 },
  { prop: 'shipDate', label: '发货日期', type: 'date', required: true },
  { prop: 'status', label: '状态', type: 'select', options: STATUSES, required: true }
]

const filters = [
  { prop: 'carrier', label: '物流公司', options: CARRIERS },
  { prop: 'status', label: '发货状态', options: STATUSES }
]

const today = () => new Date().toISOString().slice(0, 10)

const createRow = () => ({
  shipNo: 'FH' + today().replace(/-/g, '') + String(Math.floor(Math.random() * 900) + 100),
  orderNo: ORDER_NOS[0],
  customer: orders[0]?.customer || '',
  qty: 1,
  carrier: CARRIERS[0],
  trackingNo: '',
  shipDate: today(),
  status: '待发货'
})

function onFieldChange(prop, value, model) {
  if (prop !== 'orderNo') return
  const hit = orders.find((o) => o.orderNo === value)
  if (hit?.customer) model.customer = hit.customer
}
</script>
