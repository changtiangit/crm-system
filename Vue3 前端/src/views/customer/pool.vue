<template>
  <DataPage
    title="客户公海"
    desc="超过 30 天未跟进、销售离职交接、客户主动退回的客户会落到公海；领取后进入自己的客户档案，公海里的客户任何人都可以领"
    entity-name="公海客户"
    search-placeholder="搜索客户名称 / 联系人 / 城市"
    :rows="rows"
    :columns="columns"
    :form-fields="[]"
    :search-keys="['name', 'contact', 'phone', 'city']"
    :filters="filters"
    :op-width="96"
    show-actions
    hide-default-actions
  >
    <template #row-actions="{ row }">
      <el-tooltip :disabled="canClaim" content="当前账号没有领取公海客户的权限" placement="top">
        <span>
          <el-button link type="primary" :disabled="!canClaim" @click="claim(row)">领取</el-button>
        </span>
      </el-tooltip>
      <el-button link type="info" @click="showDetail(row)">详情</el-button>
    </template>

    <template #before>
      <div class="rule-bar">
        <div class="rule-item">
          <span class="rule-label">公海规则</span>
          <span>30 天未跟进自动流回公海 · 同一客户最多被领取 3 次 · 领取后 7 天内需有跟进记录</span>
        </div>
        <div class="rule-stat">
          可领取 <b>{{ rows.length }}</b> 家
        </div>
      </div>
    </template>
  </DataPage>

  <el-dialog v-model="detailVisible" title="公海客户详情" width="520px" align-center>
    <div v-if="current" class="detail">
      <div class="detail-head">
        <span class="detail-name">{{ current.name }}</span>
        <el-tag size="small" effect="light" round>{{ current.level }} 级客户</el-tag>
      </div>
      <dl class="detail-grid">
        <div><dt>所属行业</dt><dd>{{ current.industry }}</dd></div>
        <div><dt>所在城市</dt><dd>{{ current.city }}</dd></div>
        <div><dt>联系人</dt><dd>{{ current.contact || '—' }}</dd></div>
        <div><dt>联系电话</dt><dd>{{ current.phone || '—' }}</dd></div>
        <div><dt>线索来源</dt><dd>{{ current.source }}</dd></div>
        <div><dt>最后跟进</dt><dd>{{ current.lastFollow }}</dd></div>
        <div class="full"><dt>进入公海原因</dt><dd>{{ current.reason }}</dd></div>
      </dl>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button v-if="canClaim" type="primary" @click="claim(current)">领取该客户</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataPage from '@/components/DataPage.vue'
import { poolCustomers } from '@/mock/data'
import { useModuleData } from '@/composables/useModuleData'
import request from '@/utils/request'
import { claimedNames, addClaim } from '@/utils/claims'
import { usePermission } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'

const { has } = usePermission()
const userStore = useUserStore()

const canClaim = computed(() => has('pool:claim'))
// 已经被领走的客户不再出现在公海
const rows = reactive(poolCustomers.filter((c) => !claimedNames().has(c.name)).map((c) => ({ ...c })))
const moduleData = useModuleData('pool-customers', rows, poolCustomers)
onMounted(moduleData.initialize)

const INDUSTRY = ['汽车维修', '4S 店', '洗车连锁', '汽车美容', '汽配经销', '车队物流']

const columns = [
  { prop: 'name', label: '客户名称', type: 'strong', minWidth: 176 },
  { prop: 'industry', label: '所属行业', width: 92 },
  {
    prop: 'level',
    label: '级别',
    width: 62,
    type: 'tag',
    tagType: { A: 'primary', B: 'success', C: 'info' }
  },
  { prop: 'city', label: '城市', width: 64 },
  {
    prop: 'contact',
    label: '联系人',
    width: 100,
    type: 'stack',
    formatter: (row) => ({ main: row.contact, sub: row.phone })
  },
  { prop: 'lastFollow', label: '最后跟进', width: 104 },
  {
    prop: 'idleDays',
    label: '停留天数',
    width: 82,
    align: 'right',
    type: 'tag',
    tagType: (v) => (v >= 45 ? 'danger' : v >= 30 ? 'warning' : 'info')
  },
  { prop: 'reason', label: '公海原因', minWidth: 136 }
]

const filters = [{ prop: 'industry', label: '全部行业', options: INDUSTRY }]

const detailVisible = ref(false)
const current = ref(null)

function showDetail(row) {
  current.value = row
  detailVisible.value = true
}

async function claim(row) {
  if (!row) return
  if (!canClaim.value) {
    ElMessage.warning('当前账号没有领取公海客户的权限')
    return
  }
  try {
    await ElMessageBox.confirm(
      `领取后「${row.name}」会进入你的客户档案，并在 7 天内需要补一条跟进记录。确定领取吗？`,
      '领取客户',
      { confirmButtonText: '领取', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  const owner = userStore.userInfo?.name || '当前账号'
  const claimed = {
    id: Date.now(),
    name: row.name,
    industry: row.industry,
    level: row.level,
    contact: row.contact,
    phone: row.phone,
    owner,
    createTime: new Date().toISOString().slice(0, 10)
  }
  await request.post('/business/customers', claimed)
  addClaim(claimed)

  const idx = rows.findIndex((r) => r.id === row.id)
  if (idx > -1) rows.splice(idx, 1)
  await moduleData.removeRow(row.id)
  detailVisible.value = false
  ElMessage.success(`已领取「${row.name}」，可在「客户档案」里继续跟进`)
}
</script>

<style scoped>
.rule-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  padding: 10px 16px;
  border-radius: var(--radius);
  background: #f0f6ff;
  border: 1px solid #dbe7ff;
  font-size: 12.5px;
  color: #52627a;
}

.rule-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rule-label {
  flex-shrink: 0;
  color: #2f5fe0;
  font-weight: 600;
  background: #e2ecff;
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 12px;
}

.rule-stat {
  color: #52627a;
}

.rule-stat b {
  color: #2f5fe0;
  font-size: 14px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--border);
}

.detail-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2b3d;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
  margin: 14px 0 0;
}

.detail-grid div {
  min-width: 0;
}

.detail-grid .full {
  grid-column: 1 / -1;
}

.detail-grid dt {
  font-size: 12px;
  color: #97a3b6;
  margin-bottom: 3px;
}

.detail-grid dd {
  margin: 0;
  font-size: 13.5px;
  color: #273449;
}
</style>
