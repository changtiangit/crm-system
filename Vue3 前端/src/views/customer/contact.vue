<template>
  <DataPage
    show-scope
    title="联系人"
    desc="客户是公司，签字、收货、对账的都是具体的人：一个客户可以有多个联系人，标出主联系人方便下单时自动带出"
    entity-name="联系人"
    create-text="新建联系人"
    search-placeholder="搜索姓名 / 客户 / 手机 / 微信"
    :rows="rows"
    :query-filter-keys="['customer']"
    :filter-labels="{ customer: '客户' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['name', 'customer', 'phone', 'wechat']"
    :filters="filters"
    :create-row="createRow"
    :save-row="moduleData.saveRow"
    :remove-row="moduleData.removeRow"
    :refresh-rows="moduleData.refreshRows"
    :can-create="has('contact:add')"
    :can-edit="has('contact:edit')"
    :can-delete="has('contact:delete')"
    :op-width="112"
    label-width="96px"
  />
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import DataPage from '@/components/DataPage.vue'
import { contacts, customers } from '@/mock/data'
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
// 联系人跟客户走：销售只看到自己客户的对接人
const rows = reactive(contacts.filter((c) => canSee(c.owner)).map((c) => ({ ...c })))
const moduleData = useModuleData('contacts', rows, contacts)
onMounted(moduleData.initialize)

const visibleNames = visibleCustomerNames()
const CUSTOMER_NAMES = customers.filter((c) => visibleNames.has(c.name)).map((c) => c.name)
const TITLES = ['老板', '采购经理', '采购主管', '店长', '厂长', '运营总监', '仓储主管', '库管', '财务']
const OWNERS = isAll.value
  ? [...new Set([...scopeOwners.value, '张明', '李思', '周婷'])]
  : scopeOwners.value

const columns = [
  { prop: 'name', label: '姓名', type: 'strong', width: 96 },
  { prop: 'customer', label: '所属客户', type: 'link', minWidth: 150, onClick: (row) => openCustomer(row.customer) },
  { prop: 'title', label: '职务', width: 100 },
  { prop: 'phone', label: '手机号', width: 132 },
  { prop: 'wechat', label: '微信', width: 124 },
  {
    prop: 'primary',
    label: '主联系人',
    width: 86,
    type: 'tag',
    tagType: { 是: 'success', 否: 'info' }
  },
  { prop: 'owner', label: '负责人', width: 78 }
]

const formFields = [
  { prop: 'name', label: '姓名', required: true, maxlength: 20 },
  { prop: 'customer', label: '所属客户', type: 'select', options: CUSTOMER_NAMES, filterable: true, required: true },
  { prop: 'title', label: '职务', type: 'select', options: TITLES, allowCreate: true, filterable: true },
  { prop: 'phone', label: '手机号', required: true, maxlength: 20 },
  { prop: 'wechat', label: '微信号', maxlength: 40 },
  { prop: 'primary', label: '主联系人', type: 'select', options: ['是', '否'], required: true },
  {
    prop: 'owner',
    label: '负责人',
    type: 'select',
    options: OWNERS,
    required: true,
    disabled: () => !isAll.value && scopeOwners.value.length <= 1
  }
]

const filters = [{ prop: 'primary', label: '是否主联系人', options: ['是', '否'] }]

const createRow = () => ({
  name: '',
  customer: CUSTOMER_NAMES[0] || '',
  title: '采购经理',
  phone: '',
  wechat: '',
  primary: '否',
  owner: userStore.userInfo?.name || OWNERS[0] || ''
})
</script>
