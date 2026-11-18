<template>
  <DataPage
    show-scope
    title="售后工单"
    desc="客户反馈的质量、安装、规格问题都走工单：谁受理、处理到哪一步、什么时候关单，一条线跟到底"
    entity-name="售后工单"
    create-text="新建工单"
    search-placeholder="搜索工单号 / 客户 / 商品 / 问题"
    :rows="rows"
    :query-filter-keys="['customer', 'owner']"
    :filter-labels="{ customer: '客户', owner: '受理人' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['ticketNo', 'customer', 'product', 'issue', 'owner']"
    :filters="filters"
    :create-row="createRow"
    :save-row="moduleData.saveRow"
    :remove-row="moduleData.removeRow"
    :refresh-rows="moduleData.refreshRows"
    :can-create="has('ticket:add')"
    :can-edit="has('ticket:edit')"
    :can-delete="false"
    :op-width="104"
    label-width="96px"
    :show-index="false"
    @field-change="onFieldChange"
  />
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import DataPage from '@/components/DataPage.vue'
import { tickets, customers, contacts } from '@/mock/data'
import { useModuleData } from '@/composables/useModuleData'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useUserStore } from '@/stores/user'
import { visibleCustomerNames } from '@/utils/dataAccess'
import { useCustomerNav } from '@/utils/links'

const router = useRouter()
const { has } = usePermission()
const { isAll, owners: scopeOwners, canSee } = useDataScope()
const userStore = useUserStore()
const { openCustomer } = useCustomerNav()
// 工单按客户可见范围过滤：销售只看到自己客户的售后
const visibleNames = visibleCustomerNames()
const rows = reactive(
  tickets
    .filter((t) => isAll.value || visibleNames.has(t.customer) || canSee(t.owner))
    .map((t) => ({ ...t }))
)
const moduleData = useModuleData('tickets', rows, tickets)
onMounted(moduleData.initialize)

const CUSTOMER_NAMES = customers.filter((c) => visibleNames.has(c.name)).map((c) => c.name)
const LEVELS = ['高', '中', '低']
const STATUSES = ['待受理', '处理中', '已解决', '已关闭']
const OWNERS = isAll.value ? [...new Set([...scopeOwners.value, '孙浩', '吴磊'])] : scopeOwners.value
const PRODUCTS = [
  '变速箱油',
  '刹车片',
  '火花塞',
  '机油滤芯',
  '空气滤芯',
  '镀膜剂',
  '洗车液',
  '防冻液'
]

const columns = [
  { prop: 'ticketNo', label: '工单号', width: 140 },
  {
    prop: 'customer',
    label: '客户 / 联系人',
    minWidth: 160,
    type: 'stack',
    formatter: (row) => ({ main: row.customer, sub: row.contact }),
    onClick: (row) => openCustomer(row.customer)
  },
  {
    prop: 'product',
    label: '涉及商品',
    width: 138,
    type: 'link',
    onClick: (row) => router.push(`/sales/product?q=${encodeURIComponent(row.product)}`)
  },
  { prop: 'issue', label: '问题描述', minWidth: 118 },
  {
    prop: 'level',
    label: '紧急度',
    width: 66,
    type: 'tag',
    tagType: { 高: 'danger', 中: 'warning', 低: 'info' }
  },
  {
    prop: 'status',
    label: '处理状态',
    width: 88,
    type: 'tag',
    tagType: { 待受理: 'warning', 处理中: 'primary', 已解决: 'success', 已关闭: 'info' }
  },
  {
    prop: 'createTime',
    label: '时间 / 受理',
    width: 108,
    type: 'stack',
    formatter: (row) => ({ main: row.createTime.slice(5), sub: `受理：${row.owner}` })
  }
]

const formFields = [
  { prop: 'ticketNo', label: '工单号', required: true, maxlength: 30 },
  { prop: 'customer', label: '客户名称', type: 'select', options: CUSTOMER_NAMES, filterable: true, required: true },
  { prop: 'contact', label: '对接联系人', required: true, placeholder: '选客户后自动带出' },
  { prop: 'product', label: '涉及商品', type: 'select', options: PRODUCTS, allowCreate: true, filterable: true, required: true },
  { prop: 'issue', label: '问题描述', required: true, maxlength: 40 },
  { prop: 'level', label: '紧急度', type: 'select', options: LEVELS, required: true },
  { prop: 'status', label: '处理状态', type: 'select', options: STATUSES, required: true },
  { prop: 'createTime', label: '创建时间', required: true, placeholder: '如 2026-11-12 09:10' },
  {
    prop: 'owner',
    label: '受理人',
    type: 'select',
    options: OWNERS,
    required: true,
    disabled: () => !isAll.value && scopeOwners.value.length <= 1
  }
]

const filters = [
  { prop: 'level', label: '紧急度', options: LEVELS },
  { prop: 'status', label: '处理状态', options: STATUSES }
]

const now = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const createRow = () => ({
  ticketNo: 'GD' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + String(Math.floor(Math.random() * 900) + 100),
  customer: CUSTOMER_NAMES[0],
  contact: customers[0]?.contact || '',
  product: PRODUCTS[0],
  issue: '',
  level: '中',
  status: '待受理',
  createTime: now(),
  owner: userStore.userInfo?.name || OWNERS[0] || ''
})

/** 选客户带出主联系人；工单里对接人就是报修的那个人 */
function onFieldChange(prop, value, model) {
  if (prop !== 'customer') return
  const primary = contacts.find((c) => c.customer === value && c.primary === '是')
  const hit = primary || customers.find((c) => c.name === value)
  if (hit?.name || hit?.contact) model.contact = hit.contact || hit.name
}
</script>
