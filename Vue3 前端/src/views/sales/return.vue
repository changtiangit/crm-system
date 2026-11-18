<template>
  <DataPage
    show-scope
    title="退换货单"
    desc="客户收到货有问题先在这里提退换：退货回库、换货重发，金额与订单对得上，月结对账才不会扯皮"
    entity-name="退换货单"
    create-text="申请退换货"
    search-placeholder="搜索退换货单号 / 订单号 / 客户"
    :rows="rows"
    :query-filter-keys="['customer']"
    :filter-labels="{ customer: '客户' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['returnNo', 'orderNo', 'customer', 'reason']"
    :filters="filters"
    :create-row="createRow"
    :save-row="moduleData.saveRow"
    :remove-row="moduleData.removeRow"
    :refresh-rows="moduleData.refreshRows"
    :can-create="has('return:add')"
    :can-edit="has('return:edit')"
    :can-delete="false"
    :op-width="92"
    label-width="96px"
    :show-index="false"
    @field-change="onFieldChange"
  />
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import DataPage from '@/components/DataPage.vue'
import { returnOrders, orders } from '@/mock/data'
import { useModuleData } from '@/composables/useModuleData'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useUserStore } from '@/stores/user'
import { useCustomerNav } from '@/utils/links'

const { has } = usePermission()
const { isAll, owners: scopeOwners, canSee } = useDataScope()
const userStore = useUserStore()
const { openCustomer } = useCustomerNav()
// 退换货单跟随申请人
const rows = reactive(returnOrders.filter((r) => canSee(r.owner)).map((r) => ({ ...r })))
const moduleData = useModuleData('returns', rows, returnOrders)
onMounted(moduleData.initialize)

const ORDER_NOS = orders.map((o) => o.orderNo)
const TYPES = ['退货', '换货']
const STATUSES = ['待审核', '已受理', '已完成', '已驳回']
const OWNERS = isAll.value
  ? [...new Set([...scopeOwners.value, '张明', '李思', '周婷'])]
  : scopeOwners.value

const columns = [
  {
    prop: 'returnNo',
    label: '单号 / 订单',
    width: 140,
    type: 'stack',
    formatter: (row) => ({ main: row.returnNo, sub: row.orderNo })
  },
  { prop: 'customer', label: '客户名称', type: 'link', minWidth: 150, onClick: (row) => openCustomer(row.customer) },
  {
    prop: 'type',
    label: '类型',
    width: 68,
    type: 'tag',
    tagType: { 退货: 'danger', 换货: 'warning' }
  },
  {
    prop: 'qty',
    label: '数量 / 金额',
    width: 126,
    align: 'right',
    formatter: (row) => `${row.qty} 件 · ¥${Number(row.amount).toLocaleString()}`
  },
  { prop: 'reason', label: '退换原因', minWidth: 132 },
  {
    prop: 'status',
    label: '状态',
    width: 88,
    type: 'tag',
    tagType: { 待审核: 'warning', 已受理: 'primary', 已完成: 'success', 已驳回: 'info' }
  },
  {
    prop: 'applyDate',
    label: '申请日期',
    width: 108,
    type: 'stack',
    formatter: (row) => ({ main: row.applyDate, sub: row.owner })
  }
]

const formFields = [
  { prop: 'returnNo', label: '退换货单号', required: true, maxlength: 30 },
  { prop: 'orderNo', label: '关联订单', type: 'select', options: ORDER_NOS, filterable: true, required: true },
  { prop: 'customer', label: '客户名称', required: true, maxlength: 40 },
  { prop: 'type', label: '类型', type: 'select', options: TYPES, required: true },
  { prop: 'qty', label: '数量（件）', type: 'number', min: 1, required: true },
  { prop: 'amount', label: '金额', type: 'number', min: 0, precision: 2, required: true },
  { prop: 'reason', label: '退换原因', type: 'textarea', required: true },
  { prop: 'status', label: '状态', type: 'select', options: STATUSES, required: true },
  { prop: 'applyDate', label: '申请日期', type: 'date', required: true },
  {
    prop: 'owner',
    label: '申请人',
    type: 'select',
    options: OWNERS,
    required: true,
    disabled: () => !isAll.value && scopeOwners.value.length <= 1
  }
]

const filters = [
  { prop: 'type', label: '类型', options: TYPES },
  { prop: 'status', label: '状态', options: STATUSES }
]

const today = () => new Date().toISOString().slice(0, 10)

const createRow = () => ({
  returnNo: 'RT' + today().replace(/-/g, '') + String(Math.floor(Math.random() * 900) + 100),
  orderNo: ORDER_NOS[0],
  customer: orders[0]?.customer || '',
  type: '退货',
  qty: 1,
  amount: 0,
  reason: '',
  status: '待审核',
  applyDate: today(),
  owner: userStore.userInfo?.name || OWNERS[0] || ''
})

/** 选了订单就把客户带出来，避免同一单填出两个客户名 */
function onFieldChange(prop, value, model) {
  if (prop !== 'orderNo') return
  const hit = orders.find((o) => o.orderNo === value)
  if (hit?.customer) model.customer = hit.customer
}
</script>
