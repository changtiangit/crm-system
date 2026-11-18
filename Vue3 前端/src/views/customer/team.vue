<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">销售团队</h2>
        <p class="page-desc">
          按小组维护销售的能力等级与线索配额。运营分配线索时按「剩余容量 × 能力权重」择优：能力强的配额大、接得多，新人配额小、只接少量。
        </p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button
          v-if="canAssign"
          :icon="MagicStick"
          :disabled="!stats.pending"
          @click="autoAssignAll"
        >
          一键分配待分配（{{ stats.pending || 0 }}）
        </el-button>
        <el-button v-if="canEdit" type="primary" :icon="Plus" @click="openCreate">新增成员</el-button>
      </div>
    </div>

    <!-- 指标 -->
    <div class="kpi-row">
      <div class="kpi">
        <span class="kpi-label">销售人数</span>
        <span class="kpi-value">{{ stats.members || 0 }}<i>人</i></span>
        <span class="kpi-sub">{{ stats.groups || 0 }} 个分组 · {{ stats.topLevel || 0 }} 位 S 级</span>
      </div>
      <div class="kpi warn">
        <span class="kpi-label">待分配线索</span>
        <span class="kpi-value">{{ stats.pending || 0 }}<i>条</i></span>
        <span class="kpi-sub">
          <el-button v-if="canAssign && stats.pending" link type="primary" @click="goClue">去分配</el-button>
          <span v-else-if="stats.pending">当前账号无分配权限</span>
          <span v-else>池子已清空</span>
        </span>
      </div>
      <div class="kpi">
        <span class="kpi-label">在手线索</span>
        <span class="kpi-value">{{ stats.inHand || 0 }}<i>条</i></span>
        <span class="kpi-sub">本月新增 {{ stats.monthNew || 0 }} 条</span>
      </div>
      <div class="kpi">
        <span class="kpi-label">剩余可接</span>
        <span class="kpi-value">{{ stats.remaining || 0 }}<i>条</i></span>
        <span class="kpi-sub">总配额 {{ stats.capacity || 0 }} 条</span>
      </div>
      <div class="kpi">
        <span class="kpi-label">团队平均负载</span>
        <span class="kpi-value">{{ stats.loadRate || 0 }}<i>%</i></span>
        <span class="kpi-sub">已转化 {{ stats.converted || 0 }} 条线索</span>
      </div>
    </div>

    <!-- 分组 -->
    <div class="team-grid">
      <section v-for="g in groups" :key="g.name" class="group-card">
        <header class="group-head">
          <div class="gh-left">
            <span class="gh-name">{{ g.name }}</span>
            <span v-if="g.leaderName" class="gh-leader">{{ g.leaderPosition }} · {{ g.leaderName }}</span>
            <span v-else class="gh-leader muted">未设组长</span>
          </div>
          <div class="gh-right">
            在手 <b>{{ g.inHand }}</b> / {{ g.capacity }} · 余 <b>{{ g.remaining }}</b>
          </div>
        </header>

        <div class="member-list">
          <div
            v-for="m in g.members"
            :key="m.id"
            class="member-row"
            :class="{ leader: m.position !== '销售顾问' }"
            :title="`看 ${m.name} 名下的客户`"
            @click="openMemberCustomers(m)"
          >
            <div class="mr-top">
              <span class="avatar" :class="'lv-' + m.level">{{ m.name.slice(0, 1) }}</span>
              <div class="mr-name">
                <b>{{ m.name }}</b>
                <span class="mr-post">
                  {{ m.position }}
                  <i v-if="m.leaderName && m.position === '销售顾问'"> · 汇报 {{ m.leaderName }}</i>
                </span>
              </div>
              <div class="mr-tags">
                <el-tag size="small" effect="plain" round :type="levelTagType(m.level)">{{ m.level }} 级</el-tag>
                <el-tag v-if="m.status !== 1" size="small" type="info" effect="plain" round>停用</el-tag>
              </div>
              <span class="mr-remain" :class="{ zero: !m.remaining, off: m.status !== 1 }">
                {{ m.status !== 1 ? '停用' : m.remaining > 0 ? `余 ${m.remaining}` : '已满' }}
              </span>
            </div>

            <div class="mr-bar">
              <span class="bar"><i :style="{ width: loadWidth(m) }" :class="loadClass(m)"></i></span>
              <span class="mr-load">{{ m.inHand }} / {{ m.capacity }}</span>
            </div>

            <div class="mr-meta">
              <span>本月 <b>+{{ m.monthNew }}</b></span>
              <span>已转化 <b>{{ m.converted }}</b></span>
              <span>转化率 <b>{{ m.convRate }}%</b></span>
              <el-button v-if="canEdit" link type="primary" class="mr-op" @click.stop="openEdit(m)">调整</el-button>
            </div>
          </div>
          <div v-if="!g.members.length" class="group-empty">这个组还没有成员</div>
        </div>
      </section>
    </div>

    <!-- 底部：最近动态 + 规则 -->
    <div class="bottom-grid">
      <section class="panel">
        <div class="panel-head">
          <h3>最近分配动态</h3>
          <span class="panel-sub">谁把哪条线索分给了谁</span>
        </div>
        <div class="log-list">
          <div v-for="log in recent" :key="log.id" class="log-row">
            <el-tag size="small" effect="plain" round :type="log.mode === '智能' ? 'primary' : 'info'">
              {{ log.mode }}
            </el-tag>
            <span class="log-text">
              <b>{{ log.operator }}</b> 将「{{ log.clueName }}」
              <template v-if="log.fromOwner">从 {{ log.fromOwner }} 转给</template>
              分配给 <b>{{ log.toOwner }}</b>
            </span>
            <span class="log-time">{{ (log.createdAt || '').replace('T', ' ').slice(5, 16) }}</span>
          </div>
          <div v-if="!recent.length" class="panel-empty">还没有分配记录</div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <h3>分配规则与能力权重</h3>
          <span class="panel-sub">为什么能力强的分得多</span>
        </div>
        <div class="level-table">
          <div class="lt-row lt-head">
            <span>等级</span><span>能力定位</span><span>权重</span><span>建议配额</span>
          </div>
          <div v-for="lv in levelRows" :key="lv.level" class="lt-row">
            <span><em class="lv-dot" :class="'lv-' + lv.level"></em>{{ lv.level }} 级</span>
            <span>{{ lv.name }}</span>
            <span>×{{ lv.weight }}</span>
            <span>{{ lv.quota }}</span>
          </div>
        </div>
        <p class="rule-text">
          每条线索都分给「剩余容量 × 权重」最高的人；分完扣减容量再分下一条。高意向线索优先进入分配，
          因此能力强、余量多的销售会先接到高质量线索。线索够分时每人先保底 1 条（从低意向里取），
          新人不会被完全挤掉。
        </p>
      </section>
    </div>

    <!-- 调整成员 -->
    <el-dialog v-model="editVisible" :title="`调整 ${form.name || ''} 的团队信息`" width="560px" align-center>
      <el-form label-width="96px" class="member-form">
        <el-form-item label="姓名">
          <el-input :model-value="form.name" disabled />
        </el-form-item>
        <el-form-item label="职位">
          <el-select v-model="form.position" style="width: 100%">
            <el-option v-for="p in POSITIONS" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属小组">
          <el-select v-model="form.grp" style="width: 100%">
            <el-option v-for="d in groupOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="能力等级">
          <el-radio-group v-model="form.level">
            <el-radio-button v-for="lv in LEVELS" :key="lv" :value="lv">{{ lv }} 级</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="线索配额">
          <el-input-number v-model="form.capacity" :min="0" :max="99" :controls="false" style="width: 140px" />
          <span class="form-tip">在手线索达到配额后，智能分配不再派给他</span>
        </el-form-item>
        <el-form-item label="直属组长">
          <el-select v-model="form.leaderId" clearable placeholder="不指定" style="width: 100%">
            <el-option
              v-for="l in leaderOptions"
              :key="l.id"
              :label="`${l.name} · ${l.position}（${l.grp}）`"
              :value="l.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在职</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增成员 -->
    <el-dialog v-model="createVisible" title="新增销售成员" width="600px" align-center>
      <el-form label-width="96px" class="member-form">
        <el-form-item label="员工编号" required>
          <el-input v-model="newForm.empNo" placeholder="如 EMP016" maxlength="20" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="newForm.name" placeholder="真实姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="newForm.phone" placeholder="11 位手机号，同时作为登录账号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所属小组" required>
          <el-select v-model="newForm.grp" style="width: 100%">
            <el-option v-for="d in groupOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位">
          <el-select v-model="newForm.position" style="width: 100%">
            <el-option v-for="p in POSITIONS" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="能力等级">
          <el-radio-group v-model="newForm.level">
            <el-radio-button v-for="lv in LEVELS" :key="lv" :value="lv">{{ lv }} 级</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="线索配额">
          <el-input-number v-model="newForm.capacity" :min="0" :max="99" :controls="false" style="width: 140px" />
          <span class="form-tip">新人建议 2-3 条</span>
        </el-form-item>
        <el-form-item label="直属组长">
          <el-select v-model="newForm.leaderId" clearable placeholder="不指定" style="width: 100%">
            <el-option
              v-for="l in leaderOptions"
              :key="l.id"
              :label="`${l.name} · ${l.position}（${l.grp}）`"
              :value="l.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="create-tip">
        <el-icon><InfoFilled /></el-icon>
        创建后即可用手机号登录，初始密码 <b>123456</b>；角色自动跟随所属小组。
      </div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCreate">创建成员</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Plus, Refresh, InfoFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { usePermission } from '@/composables/usePermission'
import { useDataScope } from '@/composables/useDataScope'
import { useOrgStore } from '@/stores/org'

const router = useRouter()
const { has } = usePermission()
const { isAll, isSelf } = useDataScope()
const orgStore = useOrgStore()
// 团队编制属于管理动作：只有全公司范围（管理员 / 销售总监）能新增与调整，
// 组长可以看本组但不能自行扩编，后端同样会拦截
const canEdit = computed(() => has('sales:team:edit') && isAll.value)
const canAssign = computed(() => has('clue:assign') && !isSelf.value)

const POSITIONS = ['销售总监', '销售组长', '销售顾问']
const LEVELS = ['S', 'A', 'B', 'C']
const LEVEL_ROWS = [
  { level: 'S', name: '金牌 · 能带大单', weight: '1.20', quota: '8 - 10 条' },
  { level: 'A', name: '资深 · 独立打单', weight: '1.10', quota: '6 - 8 条' },
  { level: 'B', name: '熟练 · 稳定跟进', weight: '1.00', quota: '4 - 6 条' },
  { level: 'C', name: '新人 · 需带教', weight: '0.80', quota: '2 - 3 条' }
]
const levelRows = LEVEL_ROWS

const members = ref([])
const groups = ref([])
const stats = ref({})
const recent = ref([])

async function load() {
  const data = await request.get('/sales/team')
  members.value = data?.members || []
  groups.value = data?.groups || []
  stats.value = data?.stats || {}
  recent.value = data?.recent || []
}

onMounted(async () => {
  if (!orgStore.depts.length) await orgStore.loadDepartments()
  await load()
})

const groupOptions = computed(() => {
  const fromDepts = orgStore.depts.map((d) => d.name).filter((n) => n.includes('销售'))
  return [...new Set([...fromDepts, ...members.value.map((m) => m.grp).filter(Boolean)])]
})
const leaderOptions = computed(() =>
  members.value.filter((m) => m.position === '销售总监' || m.position === '销售组长')
)

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
function goClue() {
  router.push('/customer/clue')
}

/** 点成员 → 客户档案按这个负责人筛，直接看他手上的客户 */
function openMemberCustomers(m) {
  if (!has('customer:view')) {
    ElMessage.info('当前岗位没有客户档案权限')
    return
  }
  router.push(`/customer/list?owner=${encodeURIComponent(m.name)}`)
}

// ---------------- 一键智能分配 ----------------
async function autoAssignAll() {
  try {
    await ElMessageBox.confirm(
      `将待分配池里的 ${stats.value.pending} 条线索，按「剩余容量 × 能力权重」自动分给团队，是否继续？`,
      '智能分配',
      { type: 'info', confirmButtonText: '开始分配', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const res = await request.post('/sales/clues/auto-assign', { ids: [], grp: '' })
  const parts = Object.entries(res.byOwner || {}).map(([name, n]) => `${name} ${n} 条`)
  ElMessage.success(`已分配 ${res.assigned} 条${parts.length ? '：' + parts.join('，') : ''}`)
  await load()
}

// ---------------- 调整 ----------------


const editVisible = ref(false)
const createVisible = ref(false)
const saving = ref(false)
const form = reactive({})
const newForm = reactive({
  empNo: '',
  name: '',
  phone: '',
  grp: '销售一组',
  position: '销售顾问',
  level: 'C',
  capacity: 3,
  leaderId: null
})

function openEdit(m) {
  Object.assign(form, {
    id: m.id,
    name: m.name,
    position: m.position,
    grp: m.grp,
    level: m.level,
    capacity: m.capacity,
    leaderId: m.leaderId,
    status: m.status
  })
  editVisible.value = true
}

async function saveEdit() {
  if (form.capacity === null || form.capacity === undefined) {
    ElMessage.warning('请填写线索配额')
    return
  }
  saving.value = true
  try {
    await request.put(`/sales/team/${form.id}`, {
      empNo: form.empNo,
      name: form.name,
      position: form.position,
      grp: form.grp,
      level: form.level,
      capacity: form.capacity,
      leaderId: form.leaderId || null,
      status: form.status
    })
    ElMessage.success('已更新团队信息')
    editVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openCreate() {
  // 默认落到「销售一组」，直属组长自动带出该组的组长，避免新人挂到总监名下
  const grp = groupOptions.value.find((g) => g.includes('一')) || groupOptions.value[0] || '销售一组'
  const leader = members.value.find((m) => m.grp === grp && m.position === '销售组长')
  Object.assign(newForm, {
    empNo: '',
    name: '',
    phone: '',
    grp,
    position: '销售顾问',
    level: 'C',
    capacity: 3,
    leaderId: leader?.id || null
  })
  createVisible.value = true
}

async function saveCreate() {
  if (!newForm.empNo || !newForm.name || !newForm.phone) {
    ElMessage.warning('工号、姓名、手机号都要填')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(newForm.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  saving.value = true
  try {
    const res = await request.post('/sales/team', { ...newForm, status: 1 })
    ElMessage.success(`已创建 ${res.name}，初始密码 ${res.password}`)
    createVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* ---------- 指标 ---------- */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.kpi {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--border);
  box-shadow: 0 1px 2px rgba(23, 43, 77, 0.04);
}

.kpi-label {
  font-size: 12.5px;
  color: #7d8ca3;
}

.kpi-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2b3d;
  line-height: 1.25;
  font-variant-numeric: tabular-nums;
}

.kpi-value i {
  font-style: normal;
  font-size: 12.5px;
  font-weight: 400;
  color: #97a3b6;
  margin-left: 3px;
}

.kpi-sub {
  font-size: 11.5px;
  color: #97a3b6;
  min-height: 18px;
}

.kpi.warn .kpi-value {
  color: #c47a00;
}

/* ---------- 分组 ---------- */
.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  align-items: start;
  gap: 14px;
  margin-bottom: 16px;
}

.group-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(90deg, #f5f8ff, #fbfcff);
  border-bottom: 1px solid #edf1f8;
}

.gh-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.gh-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2b3d;
}

.gh-leader {
  font-size: 11.5px;
  color: #2f5fe0;
  background: #eef4ff;
  border-radius: 999px;
  padding: 1px 8px;
  white-space: nowrap;
}

.gh-leader.muted {
  color: #97a3b6;
  background: #f4f6fa;
}

.gh-right {
  font-size: 11.5px;
  color: #7d8ca3;
  white-space: nowrap;
}

.gh-right b {
  color: #1f2b3d;
}

.member-list {
  padding: 6px 12px 10px;
}

.member-row {
  padding: 10px 4px 9px;
  border-bottom: 1px dashed #f0f3f8;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.15s ease;
}

.member-row:hover {
  background: #f8faff;
}

.member-row:last-child {
  border-bottom: none;
}

.member-row.leader {
  background: linear-gradient(90deg, rgba(47, 95, 224, 0.035), transparent);
  border-radius: 8px;
  padding-left: 8px;
  padding-right: 8px;
}

.mr-top {
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

.mr-name {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.mr-name b {
  font-size: 13px;
  color: #1f2b3d;
  font-weight: 600;
}

.mr-post {
  font-size: 11px;
  color: #97a3b6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mr-post i {
  font-style: normal;
}

.mr-tags {
  display: flex;
  gap: 4px;
  flex: none;
}

.mr-remain {
  flex: none;
  font-size: 11px;
  color: #2f5fe0;
  background: #eef4ff;
  border-radius: 999px;
  padding: 1px 8px;
  font-variant-numeric: tabular-nums;
}

.mr-remain.zero {
  color: #c47a00;
  background: #fdf4e3;
}

.mr-remain.off {
  color: #8493a8;
  background: #f4f6fa;
}

.mr-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0 5px;
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
  transition: width 0.3s ease;
}

.bar i.high {
  background: #f0a020;
}

.bar i.full {
  background: #e05252;
}

.mr-load {
  font-size: 11px;
  color: #7d8ca3;
  font-variant-numeric: tabular-nums;
}

.mr-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
  color: #97a3b6;
}

.mr-meta b {
  color: #52627a;
  font-weight: 600;
}

.mr-op {
  margin-left: auto;
  font-size: 11.5px;
}

.group-empty {
  padding: 22px 0;
  text-align: center;
  font-size: 12.5px;
  color: #a7b2c4;
}

/* ---------- 底部 ---------- */
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 14px;
}

.panel {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 18px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.panel-head h3 {
  margin: 0;
  font-size: 14px;
  color: #1f2b3d;
}

.panel-sub {
  font-size: 11.5px;
  color: #97a3b6;
}

.log-list {
  max-height: 240px;
  overflow-y: auto;
}

.log-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #f1f4f9;
  font-size: 12px;
  color: #52627a;
}

.log-row:last-child {
  border-bottom: none;
}

.log-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-text b {
  color: #1f2b3d;
  font-weight: 600;
}

.log-time {
  flex: none;
  font-size: 11px;
  color: #a7b2c4;
  font-variant-numeric: tabular-nums;
}

.panel-empty {
  padding: 22px 0;
  text-align: center;
  font-size: 12.5px;
  color: #a7b2c4;
}

.level-table {
  border: 1px solid #eef2f8;
  border-radius: 8px;
  overflow: hidden;
}

.lt-row {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) 62px 82px;
  gap: 8px;
  align-items: center;
  padding: 7px 12px;
  font-size: 12px;
  color: #52627a;
  border-top: 1px solid #f4f6fa;
}

.lt-row:first-child {
  border-top: none;
}

.lt-head {
  background: #f8fafd;
  font-size: 11px;
  color: #8493a8;
}

.lv-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: 1px;
}

.lv-dot.lv-S {
  background: #d97706;
}
.lv-dot.lv-A {
  background: #2f5fe0;
}
.lv-dot.lv-B {
  background: #2b8a5f;
}
.lv-dot.lv-C {
  background: #8b98ab;
}

.rule-text {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #8493a8;
}

/* ---------- 弹窗 ---------- */
.member-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.form-tip {
  margin-left: 10px;
  font-size: 11.5px;
  color: #97a3b6;
}

.create-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f6f9ff;
  border: 1px solid #e2eaf8;
  font-size: 12px;
  color: #52627a;
}

.create-tip b {
  color: #2f5fe0;
}

@media (max-width: 1360px) {
  .kpi-row {
    gap: 10px;
  }
  .kpi {
    padding: 12px 13px;
  }
  .kpi-value {
    font-size: 21px;
  }
  .bottom-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 1100px) {
  .kpi-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .kpi-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
