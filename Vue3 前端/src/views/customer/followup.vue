<template>
  <DataPage
    show-scope
    title="跟进记录"
    desc="每次电话、上门、微信沟通都留一条记录，写清「聊了什么 + 下一步做什么」，客户档案里随时可查"
    entity-name="跟进记录"
    create-text="写跟进"
    search-placeholder="搜索客户 / 联系人 / 跟进内容"
    :rows="rows"
    :query-filter-keys="['customer']"
    :filter-labels="{ customer: '客户' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['customer', 'contact', 'content', 'next']"
    :filters="filters"
    :create-row="createRow"
    :save-row="moduleData.saveRow"
    :remove-row="moduleData.removeRow"
    :refresh-rows="moduleData.refreshRows"
    :can-create="has('followup:add')"
    :can-edit="has('followup:edit')"
    :can-delete="has('followup:delete')"
    :op-width="112"
    label-width="92px"
    @field-change="onFieldChange"
  />
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import DataPage from '@/components/DataPage.vue'
import { followups, customers, contacts } from '@/mock/data'
import { useModuleData } from '@/composables/useModuleData'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useUserStore } from '@/stores/user'
import { useCustomerNav } from '@/utils/links'
import { visibleCustomerNames } from '@/utils/dataAccess'

const { has } = usePermission()
const { isAll, owners: scopeOwners, canSee } = useDataScope()
const userStore = useUserStore()
const { openCustomer } = useCustomerNav()

// 跟进记录同样受数据范围约束：销售只看自己写的，组长看本组
const scoped = followups.filter((f) => canSee(f.owner))
// 按时间倒序，最新的跟进排最上面
const rows = reactive(
  scoped.map((f) => ({ ...f })).sort((a, b) => String(b.time).localeCompare(String(a.time)))
)
const moduleData = useModuleData('followups', rows, followups)
onMounted(moduleData.initialize)

// 只能给自己负责的客户写跟进，避免把别家的客户名带出来
const visibleNames = visibleCustomerNames()
const CUSTOMER_NAMES = customers.filter((c) => visibleNames.has(c.name)).map((c) => c.name)
const TYPES = ['电话', '上门拜访', '微信', '邮件']
const OWNERS = isAll.value
  ? [...new Set([...scopeOwners.value, '张明', '李思', '周婷'])]
  : scopeOwners.value

const columns = [
  { prop: 'time', label: '跟进时间', width: 152 },
  { prop: 'customer', label: '客户单位', type: 'link', minWidth: 148, onClick: (row) => openCustomer(row.customer) },
  { prop: 'contact', label: '联系人', width: 76 },
  {
    prop: 'type',
    label: '方式',
    width: 96,
    type: 'tag',
    tagType: { 电话: 'primary', 上门拜访: 'success', 微信: 'warning', 邮件: 'info' }
  },
  {
    prop: 'content',
    label: '跟进内容 / 下一步',
    minWidth: 256,
    // 主内容是这次聊了什么，下面一行小字是下一步安排
    type: 'stack',
    formatter: (row) => ({ main: row.content, sub: row.next ? `下一步：${row.next}` : '' })
  },
  { prop: 'owner', label: '跟进人', width: 72 }
]

const formFields = [
  { prop: 'time', label: '跟进时间', type: 'date', required: true },
  { prop: 'customer', label: '客户单位', type: 'select', options: CUSTOMER_NAMES, filterable: true, required: true },
  { prop: 'contact', label: '联系人', required: true, placeholder: '选客户后自动带出主联系人' },
  { prop: 'type', label: '跟进方式', type: 'select', options: TYPES, required: true },
  { prop: 'content', label: '跟进内容', type: 'textarea', required: true },
  { prop: 'next', label: '下一步', type: 'textarea', placeholder: '如：11-14 前发正式报价单' },
  {
    prop: 'owner',
    label: '跟进人',
    type: 'select',
    options: OWNERS,
    required: true,
    // 销售只能以自己名义写跟进
    disabled: () => !isAll.value && scopeOwners.value.length <= 1
  }
]

const filters = computed(() => {
  const list = [{ prop: 'type', label: '全部方式', options: TYPES }]
  // 组长能看组员的跟进记录，按跟进人筛一下就能看出谁跟得勤
  if (!isAll.value && scopeOwners.value.length > 1) {
    list.unshift({ prop: 'owner', label: '全部跟进人', options: scopeOwners.value })
  }
  return list
})

const createRow = () => ({
  time: new Date().toISOString().slice(0, 10),
  customer: CUSTOMER_NAMES[0],
  contact: contacts.find((c) => c.customer === CUSTOMER_NAMES[0] && c.primary === '是')?.name || '',
  type: '电话',
  content: '',
  next: '',
  owner: userStore.userInfo?.name || OWNERS[0] || ''
})

/** 选客户后自动带出该客户的主联系人 */
function onFieldChange(prop, value, model) {
  if (prop !== 'customer') return
  const hit = contacts.find((c) => c.customer === value && c.primary === '是')
  if (hit) model.contact = hit.name
}
</script>

<style scoped>
/* 跟进内容只展示一行，完整内容用悬浮提示看（列上已开 show-overflow-tooltip） */
:deep(.stack-main),
:deep(.stack-sub) {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
