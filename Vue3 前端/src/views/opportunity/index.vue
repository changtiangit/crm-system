<template>
  <DataPage
    show-scope
    title="商机管理"
    desc="跟踪销售机会的推进阶段、预计金额与成交时间，提升赢单率"
    entity-name="商机"
    create-text="新建商机"
    search-placeholder="搜索商机名称 / 客户 / 负责人"
    :rows="rows"
    :query-filter-keys="['customer', 'owner']"
    :filter-labels="{ customer: '客户', owner: '负责人' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['name', 'customer', 'owner']"
    :filters="filters"
    :create-row="createRow"
    :save-row="saveRow"
    :remove-row="removeRow"
    :refresh-rows="loadRows"
    :can-create="has('opportunity:add')"
    :can-edit="has('opportunity:edit')"
    :can-delete="has('opportunity:delete')"
  />
</template>

<script setup>
import { reactive, onMounted, computed } from 'vue'
import DataPage from '@/components/DataPage.vue'
import { customers as seedCustomers } from '@/mock/data'
import request from '@/utils/request'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useUserStore } from '@/stores/user'
import { useCustomerNav } from '@/utils/links'

const { has } = usePermission()
const { isAll, owners: scopeOwners } = useDataScope()
const userStore = useUserStore()
const { openCustomer } = useCustomerNav()
const rows = reactive([])
const customers = reactive([])
async function loadRows() {
  const [items, customerRows] = await Promise.all([request.get('/business/opportunities'), request.get('/business/customers')])
  rows.splice(0, rows.length, ...(items || []))
  // 没有可见客户时不要拿演示数据兜底，否则等于把别家的客户名漏出来
  customers.splice(0, customers.length, ...(customerRows?.length ? customerRows : isAll.value ? seedCustomers : []))
}
async function saveRow(id, row) { const saved = await (id ? request.put(`/business/opportunities/${id}`, row) : request.post('/business/opportunities', row)); await loadRows(); return saved }
async function removeRow(id) { await request.delete(`/business/opportunities/${id}`); await loadRows() }
onMounted(loadRows)

const STAGES = ['初步沟通', '需求确认', '方案报价', '商务谈判', '赢单', '输单']
const CUSTOMER_NAMES = computed(() => customers.map((c) => c.name))
const OWNERS = computed(() =>
  isAll.value ? [...new Set([...scopeOwners.value, '张明', '李思', '周婷'])] : scopeOwners.value
)

const columns = [
  { prop: 'name', label: '商机名称', type: 'strong', minWidth: 176 },
  { prop: 'customer', label: '关联客户', type: 'link', minWidth: 168, onClick: (row) => openCustomer(row.customer) },
  { prop: 'amount', label: '预计金额', type: 'money', width: 128, align: 'right' },
  {
    prop: 'stage',
    label: '销售阶段',
    width: 110,
    type: 'tag',
    tagType: {
      初步沟通: 'info',
      需求确认: 'primary',
      方案报价: 'warning',
      商务谈判: 'warning',
      赢单: 'success',
      输单: 'danger'
    }
  },
  { prop: 'expectDate', label: '预计成交', width: 116 },
  { prop: 'owner', label: '负责人', width: 100 }
]

const formFields = computed(() => [
  { prop: 'name', label: '商机名称', required: true },
  { prop: 'customer', label: '关联客户', type: 'select', options: CUSTOMER_NAMES.value, required: true },
  { prop: 'amount', label: '预计金额', type: 'number', required: true },
  { prop: 'stage', label: '销售阶段', type: 'select', options: STAGES, required: true },
  { prop: 'expectDate', label: '预计成交', type: 'date', required: true },
  {
    prop: 'owner',
    label: '负责人',
    type: 'select',
    options: OWNERS.value,
    required: true,
    disabled: () => !isAll.value && scopeOwners.value.length <= 1
  }
])

const filters = [{ prop: 'stage', label: '全部阶段', options: STAGES }]

const createRow = () => ({
  name: '',
  customer: CUSTOMER_NAMES.value[0] || '',
  amount: 100000,
  stage: '初步沟通',
  expectDate: '',
  owner: userStore.userInfo?.name || OWNERS.value[0] || ''
})
</script>
