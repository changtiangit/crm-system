<template>
  <DataPage
    show-scope
    title="客户回访"
    desc="货到之后定期回访：用得怎么样、有没有问题、下一单什么时候谈；满意度不满意的那几单要优先处理"
    entity-name="回访记录"
    create-text="新增回访"
    search-placeholder="搜索客户 / 联系人 / 回访内容"
    :rows="rows"
    :query-filter-keys="['customer', 'owner']"
    :filter-labels="{ customer: '客户', owner: '回访人' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['customer', 'contact', 'content', 'owner']"
    :filters="filters"
    :create-row="createRow"
    :save-row="moduleData.saveRow"
    :remove-row="moduleData.removeRow"
    :refresh-rows="moduleData.refreshRows"
    :can-create="has('revisit:add')"
    :can-edit="has('revisit:edit')"
    :can-delete="has('revisit:edit')"
    :op-width="104"
    label-width="96px"
    @field-change="onFieldChange"
  />
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import DataPage from '@/components/DataPage.vue'
import { revisits, customers, contacts } from '@/mock/data'
import { useModuleData } from '@/composables/useModuleData'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useUserStore } from '@/stores/user'
import { visibleCustomerNames } from '@/utils/dataAccess'
import { useCustomerNav } from '@/utils/links'

const { has } = usePermission()
const { isAll, owners: scopeOwners, canSee } = useDataScope()
const userStore = useUserStore()
const { openCustomer } = useCustomerNav()
// 回访同样按客户可见范围过滤
const visibleNames = visibleCustomerNames()
const rows = reactive(
  revisits
    .filter((r) => isAll.value || visibleNames.has(r.customer) || canSee(r.owner))
    .map((r) => ({ ...r }))
)
const moduleData = useModuleData('revisits', rows, revisits)
onMounted(moduleData.initialize)

const CUSTOMER_NAMES = customers.filter((c) => visibleNames.has(c.name)).map((c) => c.name)
const WAYS = ['电话', '微信', '上门', '邮件']
const SCORES = ['满意', '一般', '不满意']
const OWNERS = isAll.value
  ? [...new Set([...scopeOwners.value, '孙浩', '吴磊', '张明', '李思', '周婷'])]
  : scopeOwners.value

const columns = [
  { prop: 'time', label: '回访时间', width: 148 },
  { prop: 'customer', label: '客户名称', type: 'link', minWidth: 170, onClick: (row) => openCustomer(row.customer) },
  { prop: 'contact', label: '联系人', width: 78 },
  {
    prop: 'way',
    label: '方式',
    width: 76,
    type: 'tag',
    tagType: { 上门: 'primary', 电话: 'success', 微信: 'warning', 邮件: 'info' }
  },
  {
    prop: 'score',
    label: '满意度',
    width: 84,
    type: 'tag',
    tagType: { 满意: 'success', 一般: 'warning', 不满意: 'danger' }
  },
  {
    prop: 'content',
    label: '回访内容 / 下一步',
    minWidth: 200,
    type: 'stack',
    formatter: (row) => ({ main: row.content, sub: row.next ? `下一步：${row.next}` : '' })
  },
  { prop: 'owner', label: '回访人', width: 72 }
]

const formFields = [
  { prop: 'time', label: '回访时间', required: true, placeholder: '如 2026-11-12 10:20' },
  { prop: 'customer', label: '客户名称', type: 'select', options: CUSTOMER_NAMES, filterable: true, required: true },
  { prop: 'contact', label: '对接联系人', required: true, placeholder: '选客户后自动带出' },
  { prop: 'way', label: '回访方式', type: 'select', options: WAYS, required: true },
  { prop: 'score', label: '满意度', type: 'select', options: SCORES, required: true },
  { prop: 'content', label: '回访内容', type: 'textarea', required: true },
  { prop: 'next', label: '下一步安排', maxlength: 60 },
  {
    prop: 'owner',
    label: '回访人',
    type: 'select',
    options: OWNERS,
    required: true,
    disabled: () => !isAll.value && scopeOwners.value.length <= 1
  }
]

const filters = [
  { prop: 'score', label: '满意度', options: SCORES },
  { prop: 'way', label: '回访方式', options: WAYS }
]

const now = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const createRow = () => ({
  time: now(),
  customer: CUSTOMER_NAMES[0],
  contact: customers[0]?.contact || '',
  way: '电话',
  score: '满意',
  content: '',
  next: '',
  owner: userStore.userInfo?.name || OWNERS[0] || ''
})

function onFieldChange(prop, value, model) {
  if (prop !== 'customer') return
  const primary = contacts.find((c) => c.customer === value && c.primary === '是')
  const hit = primary || customers.find((c) => c.name === value)
  model.contact = hit?.contact || hit?.name || ''
}
</script>
