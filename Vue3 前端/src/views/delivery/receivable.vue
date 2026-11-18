<template>
  <DataPage
    show-scope
    title="应收账款"
    desc="订单金额减去已收金额就是应收：过了约定回款日还差钱的自动标红，按逾期天数排在最前面催收"
    entity-name="应收单"
    search-placeholder="搜索客户 / 订单号"
    :rows="rows"
    :query-filter-keys="['customer', 'owner']"
    :filter-labels="{ customer: '客户', owner: '归属销售' }"
    :columns="columns"
    :form-fields="[]"
    :search-keys="['customer', 'orderNo', 'owner']"
    :filters="filters"
    show-actions
    hide-default-actions
    :show-index="false"
    :op-width="90"
  >
    <template #row-actions="{ row }">
      <el-button
        v-if="row.unreceived > 0 && canConfirm"
        link
        type="primary"
        @click="confirm(row)"
      >
        核销
      </el-button>
      <span v-else-if="row.unreceived <= 0" class="settled">已收齐</span>
    </template>
  </DataPage>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import DataPage from '@/components/DataPage.vue'
import request from '@/utils/request'
import { receivables } from '@/mock/data'
import { useModuleData } from '@/composables/useModuleData'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useUserStore } from '@/stores/user'
import { useCustomerNav, useOrderNav } from '@/utils/links'

const { has } = usePermission()
const { isAll, owners: scopeOwners, canSee } = useDataScope()
const { openCustomer } = useCustomerNav()
const { openOrder } = useOrderNav()
const canConfirm = computed(() => has('receivable:confirm'))

const TODAY = new Date().toISOString().slice(0, 10)

/** 未收金额、账龄、状态都是算出来的，不用手填，也不会和收款记录对不上 */
function decorate(row) {
  const total = Number(row.total) || 0
  const received = Number(row.received) || 0
  const unreceived = Math.max(0, total - received)
  const days = Math.round(
    (new Date(row.dueDate + 'T00:00:00') - new Date(TODAY + 'T00:00:00')) / 86400000
  )
  row.total = total
  row.received = received
  row.unreceived = unreceived
  row.daysLeft = days
  if (unreceived <= 0) {
    row.ageText = '已结清'
    row.statusText = '已结清'
  } else if (days < 0) {
    row.ageText = `逾期 ${-days} 天`
    row.statusText = '已逾期'
  } else if (days === 0) {
    row.ageText = '今天到期'
    row.statusText = '未到期'
  } else {
    row.ageText = `剩 ${days} 天`
    row.statusText = '未到期'
  }
  return row
}

// 应收跟着归属销售走；全公司范围才看得到全部欠款
const rows = reactive(
  receivables
    .filter((r) => isAll.value || canSee(r.owner) || scopeOwners.value.length === 0)
    .map(decorate)
)
// 从接口读回来的行要重新算未收金额与账龄，所以把 decorate 传给模块数据层
const moduleData = useModuleData('receivables', rows, receivables, decorate)
onMounted(moduleData.initialize)

const columns = [
  { prop: 'customer', label: '客户名称', type: 'link', minWidth: 165, onClick: (row) => openCustomer(row.customer) },
  { prop: 'orderNo', label: '关联订单', width: 140, type: 'link', onClick: (row) => openOrder(row.orderNo) },
  { prop: 'total', label: '应收金额', type: 'money', width: 96, align: 'right' },
  { prop: 'received', label: '已收金额', type: 'money', width: 96, align: 'right' },
  { prop: 'unreceived', label: '未收金额', type: 'money', width: 98, align: 'right' },
  { prop: 'dueDate', label: '约定回款日', width: 106 },
  {
    prop: 'ageText',
    label: '账龄',
    width: 104,
    type: 'tag',
    tagType: (v, row) =>
      row?.statusText === '已结清' ? 'success' : row?.statusText === '已逾期' ? 'danger' : 'warning'
  },
  { prop: 'owner', label: '归属销售', width: 80 }
]

const filters = [{ prop: 'statusText', label: '回款状态', options: ['未到期', '已逾期', '已结清'] }]

function confirm(row) {
  const updated = decorate({ ...row, received: row.total })
  moduleData.saveRow(row.id, updated)
  ElMessage.success(`${row.customer} 的 ${row.orderNo} 已核销，未收金额清零`)
}
</script>

<style scoped>
.settled {
  font-size: 12px;
  color: #a7b2c4;
}
</style>
