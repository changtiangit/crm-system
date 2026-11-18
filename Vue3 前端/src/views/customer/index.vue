<template>
  <DataPage
    show-scope
    title="客户档案"
    desc="已成交与合作中的客户单位档案；点客户名进 360 全景，跟进、订单、回款、售后一页看全，具体对接人在「联系人」里维护"
    entity-name="客户"
    create-text="新建客户"
    search-placeholder="搜索客户名称 / 联系人 / 负责人"
    :rows="rows"
    :query-filter-keys="['owner']"
    :filter-labels="{ owner: '归属销售' }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['name', 'contact', 'phone', 'owner']"
    :filters="filters"
    :create-row="createRow"
    :save-row="saveRow"
    :remove-row="removeRow"
    :refresh-rows="loadRows"
    :can-create="has('customer:add')"
    :can-edit="has('customer:edit')"
    :can-delete="has('customer:delete')"
    :row-click="(row) => openCustomer(row.name)"
  />
</template>

<script setup>
import { reactive, computed, ref, onMounted } from 'vue'
import DataPage from '@/components/DataPage.vue'
import request from '@/utils/request'
import { readVisibleClaims } from '@/utils/claims'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useUserStore } from '@/stores/user'
import { useCustomerNav } from '@/utils/links'

const { has } = usePermission()
const { owners: scopeOwners, isAll } = useDataScope()
const userStore = useUserStore()
const { openCustomer } = useCustomerNav()
// 从公海领取的客户存在本地，刷新后仍要能在这里看到
const rows = reactive([])
async function loadRows() { const data = await request.get('/business/customers'); rows.splice(0, rows.length, ...(data || []), ...readVisibleClaims()) }
async function saveRow(id, row) { const saved = await (id ? request.put(`/business/customers/${id}`, row) : request.post('/business/customers', row)); await loadRows(); return saved }
async function removeRow(id) { await request.delete(`/business/customers/${id}`); await loadRows() }
onMounted(() => {
  loadRows()
  loadTeamNames()
})

// 全公司范围时，负责人下拉给出销售团队的全部成员
const teamNames = ref([])
async function loadTeamNames() {
  try {
    const team = await request.get('/sales/team')
    teamNames.value = (team?.members || []).map((m) => m.name)
  } catch {
    teamNames.value = []
  }
}

// 客户都是汽修 / 洗车产业链上的企业，不是互联网客户
const INDUSTRY = ['汽车维修', '4S 店', '洗车连锁', '汽车美容', '汽配经销', '车队物流']
const LEVELS = ['A', 'B', 'C']
// 负责人只能从自己的数据范围里选：销售只有自己，组长有本组成员，管理层有全部销售
const OWNER_OPTIONS = computed(() =>
  isAll.value
    ? [...new Set([...teamNames.value, ...scopeOwners.value, '张明', '李思', '周婷'])]
    : scopeOwners.value
)

const columns = [
  {
    prop: 'name',
    label: '客户名称',
    type: 'link',
    minWidth: 168,
    onClick: (row) => openCustomer(row.name)
  },
  { prop: 'industry', label: '所属行业', width: 98 },
  {
    prop: 'level',
    label: '客户级别',
    width: 84,
    type: 'tag',
    tagType: { A: 'primary', B: 'success', C: 'info' }
  },
  { prop: 'contact', label: '联系人', width: 100 },
  { prop: 'phone', label: '联系电话', width: 130 },
  { prop: 'owner', label: '负责人', width: 100 },
  { prop: 'createTime', label: '创建日期', width: 116, formatter: (row) => String(row.createTime || '').slice(0, 10) }
]

const formFields = computed(() => [
  { prop: 'name', label: '客户名称', required: true },
  { prop: 'industry', label: '所属行业', type: 'select', options: INDUSTRY, required: true },
  { prop: 'level', label: '客户级别', type: 'select', options: LEVELS, required: true },
  { prop: 'contact', label: '联系人', required: true },
  { prop: 'phone', label: '联系电话', required: true },
  {
    prop: 'owner',
    label: '负责人',
    type: 'select',
    options: OWNER_OPTIONS.value,
    required: true,
    // 销售只有自己一个选项，直接锁住，避免误改归属
    disabled: () => !isAll.value && scopeOwners.value.length <= 1
  }
])

const filters = computed(() => {
  const list = [
    { prop: 'level', label: '全部级别', options: LEVELS },
    { prop: 'industry', label: '全部行业', options: INDUSTRY }
  ]
  // 组长看本组时可按组员筛选，方便逐个跟进度；销售只有自己，不需要这个下拉
  if (!isAll.value && scopeOwners.value.length > 1) {
    list.unshift({ prop: 'owner', label: '全部负责人', options: scopeOwners.value })
  }
  return list
})

const createRow = () => ({
  name: '',
  industry: '汽车维修',
  level: 'B',
  contact: '',
  phone: '',
  owner: userStore.userInfo?.name || OWNER_OPTIONS.value[0] || ''
  // 创建日期由数据库默认值生成，前端不再提交（否则后端 LocalDateTime 解析失败）
})
</script>
