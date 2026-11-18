<template>
  <DataPage
    show-scope
    ref="pageRef"
    title="销售线索"
    desc="运营从官网、展会、电话、广告等渠道收到的线索先进待分配池，再按销售的能力与剩余配额分下去；点「智能分配」会自动算好给谁"
    entity-name="线索"
    create-text="新建线索"
    search-placeholder="搜索线索名称 / 联系人 / 手机号 / 负责人"
    :rows="rows"
    :query-filter-keys="['status', 'source', 'intent', 'month']"
    :filter-labels="{ status: '状态', source: '来源', intent: '意向', month: '创建月份' }"
    :query-matchers="{ month: (row, v) => String(row.createTime || '').startsWith(v) }"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['name', 'contact', 'phone', 'owner', 'source']"
    :filters="filters"
    :create-row="createRow"
    :save-row="saveRow"
    :remove-row="removeRow"
    :refresh-rows="loadAll"
    :can-create="has('clue:add')"
    :can-edit="has('clue:edit')"
    :can-delete="has('clue:delete')"
    :op-width="168"
    :selectable="canAssign"
    @selection-change="onSelectionChange"
  >
    <!-- 待分配池概览 -->
    <template #before>
      <div v-if="canAssign" class="assign-bar">
        <div class="ab-left">
          <div class="ab-item">
            <span class="ab-label">待分配</span>
            <b class="ab-num warn">{{ pendingRows.length }}</b>
            <span class="ab-unit">条</span>
          </div>
          <div class="ab-item">
            <span class="ab-label">高意向待分</span>
            <b class="ab-num danger">{{ highIntentPending }}</b>
            <span class="ab-unit">条</span>
          </div>
          <div class="ab-item">
            <span class="ab-label">本月新进</span>
            <b class="ab-num">{{ monthNew }}</b>
            <span class="ab-unit">条</span>
          </div>
          <div
            class="ab-item"
            :class="{ link: has('sales:team:view') }"
            :title="has('sales:team:view') ? '查看销售团队与配额' : ''"
            @click="has('sales:team:view') && router.push('/customer/team')"
          >
            <span class="ab-label">团队剩余可接</span>
            <b class="ab-num">{{ teamRemaining }}</b>
            <span class="ab-unit">条</span>
          </div>
        </div>
        <div class="ab-right">
          <el-popover placement="bottom-end" :width="330" trigger="click">
            <template #reference>
              <el-button link type="info" :icon="InfoFilled">分配规则</el-button>
            </template>
            <div class="rule-pop">
              <p class="rule-title">线索怎么分</p>
              <ol>
                <li>待分配池按<b>意向高 → 低</b>、先到先分排序；</li>
                <li>每位销售打分 = <b>剩余容量 × 能力权重</b>（S 1.2 / A 1.1 / B 1.0 / C 0.8）；</li>
                <li>每条都分给当前分数最高的人，分完扣减其容量，再分下一条；</li>
                <li>线索够分时每人先<b>保底 1 条</b>，从低意向里取，保证新人也有练手机会。</li>
              </ol>
              <p class="rule-note">能力强的容量大、权重高，自然接得多；新人容量小、权重低，只接少量。</p>
            </div>
          </el-popover>
          <el-button
            type="primary"
            :icon="MagicStick"
            :disabled="!canAssign || !pendingRows.length"
            @click="openAssign(pendingRows, 'auto')"
          >
            智能分配（{{ pendingRows.length }}）
          </el-button>
        </div>
      </div>
    </template>

    <!-- 勾选后的批量操作 -->
    <template #selection-actions="{ selected, clear }">
      <div v-if="selected.length" class="sel-bar">
        <span>已选 <b>{{ selected.length }}</b> 条</span>
        <el-button size="small" type="primary" plain @click="openAssign(selected, 'manual')">分配</el-button>
        <el-button size="small" @click="openAssign(selected, 'auto')">智能分配</el-button>
        <el-button size="small" link @click="clear">取消勾选</el-button>
      </div>
    </template>

    <!-- 行内：待分配显示「分配」，已分配显示「转派」 -->
    <template #row-actions="{ row }">
      <el-button v-if="canAssign" link type="primary" @click="openAssign([row], 'manual')">
        {{ row.owner ? '转派' : '分配' }}
      </el-button>
    </template>
  </DataPage>

  <!-- 分配弹窗 -->
  <el-dialog
    v-model="assignVisible"
    :title="`分配线索（${targetRows.length} 条）`"
    width="760px"
    align-center
    destroy-on-close
  >
    <div class="as-toolbar">
      <el-radio-group v-model="assignMode" size="small">
        <el-radio-button value="manual">指定销售</el-radio-button>
        <el-radio-button value="auto">按能力智能分配</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="assignGroup" size="small">
        <el-radio-button value="">全部小组</el-radio-button>
        <el-radio-button v-for="g in groupNames" :key="g" :value="g">{{ g }}</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 指定销售 -->
    <template v-if="assignMode === 'manual'">
      <div class="as-hint">
        选一个人接手这 {{ targetRows.length }} 条线索；卡片上的进度条是在手线索 / 配额。
      </div>
      <div class="member-grid">
        <div
          v-for="m in assignCandidates"
          :key="m.id"
          class="member-card"
          :class="{ active: pickedOwner === m.name, full: m.remaining <= 0, off: m.status !== 1 }"
          @click="pickMember(m)"
        >
          <div class="mc-head">
            <span class="avatar" :class="'lv-' + m.level">{{ m.name.slice(0, 1) }}</span>
            <div class="mc-id">
              <span class="mc-name">{{ m.name }}</span>
              <span class="mc-sub">{{ m.position }} · {{ m.grp }}</span>
            </div>
            <el-tag size="small" effect="plain" round :type="levelTagType(m.level)">{{ m.level }} 级</el-tag>
          </div>
          <div class="mc-bar">
            <span class="bar"><i :style="{ width: loadWidth(m) }" :class="loadClass(m)"></i></span>
            <span class="mc-load">{{ m.inHand }} / {{ m.capacity }}</span>
          </div>
          <div class="mc-foot">
            <span>剩余可接 <b>{{ m.remaining }}</b> 条</span>
            <span>转化 {{ m.convRate }}%</span>
          </div>
          <div v-if="m.remaining <= 0" class="mc-full">配额已满</div>
        </div>
      </div>
      <div v-if="pickedMember" class="pick-tip" :class="{ danger: pickedMember.remaining < targetRows.length }">
        将分配给 <b>{{ pickedMember.name }}</b>（{{ pickedMember.grp }} · {{ pickedMember.level }} 级）：
        当前在手 {{ pickedMember.inHand }} 条、配额 {{ pickedMember.capacity }} 条
        <template v-if="pickedMember.remaining < targetRows.length">
          ，本次超出配额 {{ targetRows.length - pickedMember.remaining }} 条，确认时会再提示一次
        </template>
      </div>
    </template>

    <!-- 智能分配预览 -->
    <template v-else>
      <div class="as-hint">
        意向高的线索优先给强手，线索够分时先给每人保底 1 条（从低意向里取）；下面是本次 {{ targetsInGroup.length }} 条线索的预计去向。
      </div>
      <div class="plan">
        <div class="plan-head">
          <span>销售</span>
          <span>等级 / 权重</span>
          <span>分配前在手</span>
          <span>本次分到</span>
          <span>分配后</span>
        </div>
        <div v-for="p in autoPlan" :key="p.name" class="plan-row">
          <span class="plan-name">
            <span class="avatar sm" :class="'lv-' + p.level">{{ p.name.slice(0, 1) }}</span>
            {{ p.name }}
            <i class="plan-grp">{{ p.grp }}</i>
          </span>
          <span>{{ p.level }} 级 · ×{{ Number(p.weight).toFixed(2) }}</span>
          <span>{{ p.inHand }} / {{ p.capacity }}</span>
          <span class="plan-count" :class="{ zero: !p.count }">+{{ p.count }}</span>
          <span>{{ p.inHand + p.count }} / {{ p.capacity }}</span>
        </div>
        <div v-if="!autoPlan.length" class="plan-empty">该小组暂无可接线索的销售</div>
      </div>
      <div class="auto-note">
        <el-icon><InfoFilled /></el-icon>
        能力强的销售配额更高、权重更大，因此会分到更多；新人只接少量，避免线索被压住跟不过来。
      </div>
    </template>

    <template #footer>
      <el-button @click="assignVisible = false">取消</el-button>
      <el-button type="primary" :loading="assigning" @click="confirmAssign">
        {{ assignMode === 'manual' ? '确认分配' : `确认智能分配 ${targetsInGroup.length} 条` }}
      </el-button>
    </template>
  </el-dialog>

  <!-- 分配结果 -->
  <el-dialog v-model="resultVisible" title="分配完成" width="720px" align-center>
    <div class="result-top">
      <div class="result-num">
        <b>{{ lastResult?.assigned ?? 0 }}</b>
        <span>条已分到销售手中</span>
      </div>
      <div v-if="lastResult?.left" class="result-left">
        还有 {{ lastResult.left }} 条因团队容量不足留在待分配池
      </div>
    </div>
    <div v-if="resultByOwner.length" class="result-chips">
      <span v-for="r in resultByOwner" :key="r.owner" class="rc">
        {{ r.owner }} <b>+{{ r.count }}</b>
      </span>
    </div>
    <el-table :data="resultDetails" size="small" max-height="320" style="width: 100%">
      <el-table-column prop="clueName" label="线索" min-width="220" show-overflow-tooltip />
      <el-table-column prop="intent" label="意向" width="70">
        <template #default="{ row }">
          <el-tag size="small" effect="light" :type="row.intent === '高' ? 'danger' : row.intent === '中' ? 'warning' : 'info'">
            {{ row.intent }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="owner" label="接收销售" width="100" />
      <el-table-column prop="grp" label="小组" width="100" />
      <el-table-column prop="level" label="等级" width="70" />
    </el-table>
    <template #footer>
      <el-button type="primary" @click="resultVisible = false">知道了</el-button>
      <el-button @click="goTeam">查看销售团队</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, InfoFilled } from '@element-plus/icons-vue'
import DataPage from '@/components/DataPage.vue'
import request from '@/utils/request'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'

const { has } = usePermission()
const { isSelf } = useDataScope()
const router = useRouter()
// 功能权限决定能不能进分配流程，数据范围决定能不能当分配人（顾问只能接线索）
const canAssign = computed(() => has('clue:assign') && !isSelf.value)

const pageRef = ref(null)
const rows = reactive([])
const team = reactive({ members: [], stats: {}, groups: [] })
const selected = ref([])

async function loadRows() {
  const data = await request.get('/business/clues')
  rows.splice(0, rows.length, ...(data || []))
}
async function loadTeam() {
  const data = await request.get('/sales/team')
  team.members = data?.members || []
  team.stats = data?.stats || {}
  team.groups = data?.groups || []
}
async function loadAll() {
  await Promise.all([loadRows(), loadTeam()])
}
onMounted(loadAll)

async function saveRow(id, row) {
  // 没选负责人就是待分配；选了人但状态还停在待分配，就顺延成待跟进
  const payload = { ...row }
  if (!payload.owner) payload.status = '待分配'
  else if (!payload.status || payload.status === '待分配') payload.status = '待跟进'
  const saved = await (id
    ? request.put(`/business/clues/${id}`, payload)
    : request.post('/business/clues', payload))
  await loadAll()
  return saved
}
async function removeRow(id) {
  await request.delete(`/business/clues/${id}`)
  await loadAll()
}

const STATUS = ['待分配', '待跟进', '跟进中', '已转化', '已废弃']
const SOURCE = ['官网表单', '电话咨询', '展会', '老客户推荐', '广告投放']
const INTENT = ['高', '中', '低']

const groupNames = computed(() => [...new Set(team.members.map((m) => m.grp).filter(Boolean))])

const pendingRows = computed(() => rows.filter((r) => !r.owner || r.status === '待分配'))
const highIntentPending = computed(() => pendingRows.value.filter((r) => r.intent === '高').length)
const monthNew = computed(() => {
  const prefix = new Date().toISOString().slice(0, 7)
  return rows.filter((r) => String(r.createTime || '').startsWith(prefix)).length
})
const teamRemaining = computed(() => team.stats?.remaining ?? 0)

const columns = [
  { prop: 'name', label: '线索名称', type: 'strong', minWidth: 186 },
  {
    prop: 'contact',
    label: '联系人',
    width: 124,
    type: 'stack',
    formatter: (row) => ({ main: row.contact || '—', sub: row.phone || '' })
  },
  { prop: 'source', label: '来源', width: 96 },
  {
    prop: 'intent',
    label: '意向',
    width: 70,
    type: 'tag',
    tagType: { 高: 'danger', 中: 'warning', 低: 'info' }
  },
  {
    prop: 'status',
    label: '状态',
    width: 86,
    type: 'tag',
    tagType: { 待分配: 'danger', 待跟进: 'warning', 跟进中: 'primary', 已转化: 'success', 已废弃: 'info' }
  },
  {
    prop: 'owner',
    label: '负责人',
    width: 110,
    type: 'stack',
    formatter: (row) => {
      if (!row.owner) return { main: '待分配', sub: '尚未指派' }
      const m = team.members.find((x) => x.name === row.owner)
      return { main: row.owner, sub: m?.grp || '' }
    }
  },
  {
    // 分配与创建合成一列：主行是分配时间，下面小字是线索进入的时间
    prop: 'assignTime',
    label: '分配 / 创建',
    width: 146,
    type: 'stack',
    formatter: (row) => {
      const t = (v) => String(v || '').replace('T', ' ').slice(5, 16)
      return {
        main: row.assignTime ? t(row.assignTime) : '未分配',
        sub: row.createTime ? `创建 ${t(row.createTime)}` : ''
      }
    }
  }
]

const formFields = computed(() => [
  { prop: 'name', label: '线索名称', required: true },
  { prop: 'contact', label: '联系人', required: true },
  { prop: 'phone', label: '手机号', required: true },
  { prop: 'source', label: '线索来源', type: 'select', options: SOURCE, required: true },
  { prop: 'intent', label: '意向等级', type: 'select', options: INTENT, required: true },
  { prop: 'status', label: '当前状态', type: 'select', options: STATUS, required: true }
  // 负责人不放在表单里：新建的线索统一进待分配池，认领 / 转派走列表上方的「分配」按钮
])

const filters = [
  { prop: 'status', label: '全部状态', options: STATUS },
  { prop: 'intent', label: '全部意向', options: INTENT },
  { prop: 'source', label: '全部来源', options: SOURCE }
]

const createRow = () => ({
  name: '',
  contact: '',
  phone: '',
  source: '官网表单',
  intent: '中',
  status: '待分配',
  owner: ''
})

function onSelectionChange(list) {
  selected.value = list
}

// ---------------- 分配 ----------------
const assignVisible = ref(false)
const assignMode = ref('manual')
const assignGroup = ref('')
const targetRows = ref([])
const pickedOwner = ref('')
const assigning = ref(false)

const resultVisible = ref(false)
const lastResult = ref(null)
const resultDetails = ref([])
const resultByOwner = computed(() =>
  Object.entries(lastResult.value?.byOwner || {}).map(([owner, count]) => ({ owner, count }))
)

const assignCandidates = computed(() =>
  team.members.filter((m) => !assignGroup.value || m.grp === assignGroup.value)
)

const pickedMember = computed(() => team.members.find((m) => m.name === pickedOwner.value) || null)

/** 本次可参与智能分配的目标：选中的待分配线索（已分配的转派不参与批量） */
const targetsInGroup = computed(() => {
  const pool = targetRows.value.filter((r) => !r.owner || r.status === '待分配')
  return pool
})

const autoPlan = computed(() => {
  const pool = team.members
    .filter((m) => m.assignable && (!assignGroup.value || m.grp === assignGroup.value))
    .map((m) => ({ ...m, left: m.remaining, count: 0 }))
  // 与后端同口径：先按意向高→低、再按创建时间排
  const rank = { 高: 0, 中: 1, 低: 2 }
  const queue = [...targetsInGroup.value].sort(
    (a, b) =>
      (rank[a.intent] ?? 3) - (rank[b.intent] ?? 3) ||
      String(a.createTime).localeCompare(String(b.createTime))
  )
  // 保底轮：线索够分时每人先 1 条，从低意向的尾部取，优先给等级低的
  if (queue.length >= pool.length && pool.length > 1) {
    const warm = [...pool].sort((a, b) => a.weight - b.weight || b.inHand - a.inHand)
    warm.forEach((m) => {
      queue.pop()
      m.count++
      m.left--
    })
  }
  for (const _ of queue) {
    const pick = pool
      .filter((m) => m.left > 0)
      .sort((a, b) => b.left * b.weight - a.left * a.weight || b.weight - a.weight || b.inHand - a.inHand)[0]
    if (!pick) break
    pick.count++
    pick.left--
  }
  return pool.sort((a, b) => b.count - a.count || b.weight - a.weight)
})

function openAssign(list, mode = 'manual') {
  if (!canAssign.value) {
    ElMessage.info('当前账号没有分配线索的权限，可联系管理员开通')
    return
  }
  if (!list?.length) return
  targetRows.value = [...list]
  assignMode.value = mode
  assignGroup.value = ''
  pickedOwner.value = ''
  assignVisible.value = true
}

function pickMember(m) {
  if (m.status !== 1) {
    ElMessage.info(`${m.name} 已停用，不能接收线索`)
    return
  }
  pickedOwner.value = m.name
}

function levelTagType(level) {
  return { S: 'warning', A: 'primary', B: 'success', C: 'info' }[level] || 'info'
}

function loadWidth(m) {
  if (!m.capacity) return '0%'
  return `${Math.min(100, Math.round((m.inHand / m.capacity) * 100))}%`
}

function loadClass(m) {
  const r = m.capacity ? m.inHand / m.capacity : 0
  return r >= 1 ? 'full' : r >= 0.8 ? 'high' : ''
}

async function confirmAssign() {
  if (assignMode.value === 'manual') {
    if (!pickedOwner.value) {
      ElMessage.warning('请先选择接收线索的销售')
      return
    }
    const m = pickedMember.value
    let force = false
    if (m && m.remaining < targetRows.value.length) {
      try {
        await ElMessageBox.confirm(
          `${m.name} 剩余配额只有 ${m.remaining} 条，本次要分配 ${targetRows.value.length} 条，确定超出配额分配吗？`,
          '超出线索配额',
          { type: 'warning', confirmButtonText: '仍然分配', cancelButtonText: '重新选择' }
        )
        force = true
      } catch {
        return
      }
    }
    assigning.value = true
    try {
      const res = await request.post('/sales/clues/assign', {
        ids: targetRows.value.map((r) => r.id),
        owner: pickedOwner.value,
        force
      })
      ElMessage.success(`已分配给 ${res.owner} ${res.assigned} 条`)
      assignVisible.value = false
      pageRef.value?.clearSelection()
      await loadAll()
      resultDetails.value = targetRows.value.map((r) => ({
        clueName: r.name,
        intent: r.intent,
        owner: res.owner,
        grp: m?.grp || '',
        level: m?.level || ''
      }))
      lastResult.value = { assigned: res.assigned, byOwner: { [res.owner]: res.assigned } }
      resultVisible.value = true
    } finally {
      assigning.value = false
    }
    return
  }

  // 智能分配
  if (!targetsInGroup.value.length) {
    ElMessage.warning('所选线索都已有负责人，不需要再智能分配')
    return
  }
  assigning.value = true
  try {
    const res = await request.post('/sales/clues/auto-assign', {
      ids: targetsInGroup.value.map((r) => r.id),
      grp: assignGroup.value
    })
    lastResult.value = res
    resultDetails.value = res.details || []
    assignVisible.value = false
    resultVisible.value = true
    pageRef.value?.clearSelection()
    await loadAll()
  } finally {
    assigning.value = false
  }
}

function goTeam() {
  resultVisible.value = false
  router.push('/customer/team')
}
</script>

<style scoped>
.assign-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 12px 18px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: linear-gradient(90deg, #f6f9ff, #fbfcff);
  border: 1px solid #e2eaf8;
}

.ab-left {
  display: flex;
  align-items: center;
  gap: 26px;
  flex-wrap: wrap;
}

.ab-item {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.ab-label {
  font-size: 12.5px;
  color: #7d8ca3;
}

.ab-num {
  font-size: 19px;
  font-weight: 600;
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

.ab-num.warn {
  color: #c47a00;
}

.ab-num.danger {
  color: #d4380d;
}

.ab-unit {
  font-size: 12px;
  color: #97a3b6;
}

.ab-item.link {
  cursor: pointer;
  border-radius: 6px;
  padding: 2px 6px;
  margin: 0 -2px;
}

.ab-item.link:hover {
  background: #eef4ff;
}

.ab-item.link .ab-num {
  color: #2f5fe0;
}

.ab-item.link {
  cursor: pointer;
  border-radius: 6px;
  padding: 2px 6px;
  margin: 0 -2px;
}

.ab-item.link:hover {
  background: #eef4ff;
}

.ab-item.link .ab-num {
  color: #2f5fe0;
}

.ab-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rule-pop .rule-title {
  margin: 0 0 6px;
  font-weight: 600;
  color: #1f2b3d;
}

.rule-pop ol {
  margin: 0 0 8px;
  padding-left: 18px;
  font-size: 12.5px;
  line-height: 1.75;
  color: #52627a;
}

.rule-pop b {
  color: #2f5fe0;
}

.rule-note {
  margin: 0;
  font-size: 12px;
  color: #8493a8;
  line-height: 1.6;
}

.sel-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 10px;
  margin-right: 8px;
  border-radius: 8px;
  background: #eef4ff;
  border: 1px solid #d6e2ff;
  font-size: 12.5px;
  color: #3d6ad6;
}

.sel-bar b {
  font-size: 14px;
}

/* ---------- 分配弹窗 ---------- */
.as-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.as-hint {
  font-size: 12.5px;
  color: #7d8ca3;
  margin-bottom: 12px;
}

.member-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
  padding: 2px;
}

.member-card {
  position: relative;
  padding: 10px 12px;
  border: 1px solid #e6ebf4;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: all 0.15s ease;
}

.member-card:hover {
  border-color: #b9cdf7;
  box-shadow: 0 2px 10px rgba(47, 95, 224, 0.08);
}

.member-card.active {
  border-color: #2f5fe0;
  background: #f6f9ff;
  box-shadow: 0 0 0 2px rgba(47, 95, 224, 0.12);
}

.member-card.full {
  background: #fbfbfc;
}

.member-card.off {
  opacity: 0.55;
}

.mc-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: #7f93b5;
  flex: none;
}

.avatar.lv-S {
  background: linear-gradient(135deg, #f0a020, #d97706);
}

.avatar.lv-A {
  background: linear-gradient(135deg, #4f7df3, #2f5fe0);
}

.avatar.lv-B {
  background: linear-gradient(135deg, #3aa675, #2b8a5f);
}

.avatar.lv-C {
  background: linear-gradient(135deg, #9aa8bd, #7f8fa6);
}

.avatar.sm {
  width: 22px;
  height: 22px;
  font-size: 11px;
}

.mc-id {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.mc-name {
  font-size: 13px;
  font-weight: 600;
  color: #1f2b3d;
}

.mc-sub {
  font-size: 11px;
  color: #97a3b6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mc-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 9px 0 6px;
}

.bar {
  flex: 1;
  height: 5px;
  border-radius: 4px;
  background: #eef1f7;
  overflow: hidden;
}

.bar i {
  display: block;
  height: 100%;
  border-radius: 4px;
  background: #6f9bf0;
}

.bar i.high {
  background: #f0a020;
}

.bar i.full {
  background: #e05252;
}

.mc-load {
  font-size: 11px;
  color: #7d8ca3;
  font-variant-numeric: tabular-nums;
}

.mc-foot {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: #8493a8;
}

.mc-foot b {
  color: #2f5fe0;
}

.mc-full {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 10.5px;
  color: #d4380d;
  background: #fff1ee;
  border: 1px solid #ffd6cb;
  border-radius: 999px;
  padding: 0 6px;
}

.pick-tip {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f6f9ff;
  border: 1px solid #e2eaf8;
  font-size: 12.5px;
  color: #52627a;
}

.pick-tip b {
  color: #1f2b3d;
}

.pick-tip.danger {
  background: #fff8f5;
  border-color: #ffdccf;
  color: #a8471f;
}

/* ---------- 智能分配预览 ---------- */
.plan {
  border: 1px solid #e9eef7;
  border-radius: 10px;
  overflow: hidden;
}

.plan-head,
.plan-row {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) 130px 100px 86px 100px;
  gap: 8px;
  align-items: center;
  padding: 9px 14px;
}

.plan-head {
  background: #f7f9fd;
  font-size: 11.5px;
  color: #8493a8;
}

.plan-row {
  font-size: 12.5px;
  color: #33415a;
  border-top: 1px solid #f1f4f9;
}

.plan-name {
  display: flex;
  align-items: center;
  gap: 7px;
  font-weight: 500;
  color: #1f2b3d;
}

.plan-grp {
  font-style: normal;
  font-size: 11px;
  color: #97a3b6;
}

.plan-count {
  font-weight: 700;
  color: #2f5fe0;
  font-variant-numeric: tabular-nums;
}

.plan-count.zero {
  color: #b3bdcc;
}

.plan-empty {
  padding: 20px;
  text-align: center;
  font-size: 12.5px;
  color: #a7b2c4;
}

.auto-note {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 10px;
  font-size: 12px;
  color: #8493a8;
  line-height: 1.6;
}

/* ---------- 分配结果 ---------- */
.result-top {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}

.result-num b {
  font-size: 28px;
  color: #2f5fe0;
  margin-right: 4px;
  font-variant-numeric: tabular-nums;
}

.result-num span {
  font-size: 13px;
  color: #52627a;
}

.result-left {
  font-size: 12.5px;
  color: #c47a00;
}

.result-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.rc {
  font-size: 12.5px;
  color: #52627a;
  background: #f4f7fd;
  border: 1px solid #e4ebf7;
  border-radius: 999px;
  padding: 3px 10px;
}

.rc b {
  color: #2f5fe0;
}
</style>
