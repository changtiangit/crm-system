<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">我的待办</h2>
        <p class="page-desc">
          {{ profile.name }}岗位的活都在这里：{{ profile.todoTypes.slice(0, 3).join(' / ') }} 等；办完勾掉，逾期项标红
        </p>
      </div>
      <div class="header-actions">
        <el-tooltip :disabled="canAdd" content="当前账号没有新增待办权限" placement="bottom">
          <span>
            <el-button type="primary" :icon="Plus" :disabled="!canAdd" @click="openCreate">新增待办</el-button>
          </span>
        </el-tooltip>
      </div>
    </div>

    <!-- 岗位视角条：说明这套待办是按什么岗位生成的，切账号就换一套 -->
    <div class="role-bar" :style="{ '--accent': profile.accent, '--soft': profile.soft }">
      <span class="role-tag">{{ profile.name }}</span>
      <span class="role-desk">{{ profile.deskTitle }}</span>
      <span class="role-duty">
        <span v-for="d in profile.duty" :key="d">{{ d }}</span>
      </span>
      <span class="role-note">待办按岗位初始化，只对本人可见</span>
    </div>

    <div class="stat-row">
      <div
        v-for="s in stats"
        :key="s.label"
        class="stat-card page-card"
        :class="{ active: active === s.tab }"
        :title="`筛选出「${s.label}」的待办`"
        @click="active = s.tab"
      >
        <span class="stat-label">{{ s.label }}</span>
        <span class="stat-value" :class="s.tone">{{ s.value }}</span>
      </div>
    </div>

    <div class="page-card">
      <div class="board-head">
        <div class="tabs">
          <button
            v-for="t in tabs"
            :key="t.key"
            class="tab"
            :class="{ active: active === t.key }"
            @click="active = t.key"
          >
            {{ t.label }}
            <span class="tab-count" :class="{ warn: t.key === 'overdue' && t.count > 0 }">{{ t.count }}</span>
          </button>
        </div>
        <span class="board-tip">
          <el-icon><InfoFilled /></el-icon>
          待办只对当前账号可见，切换账号后互不影响
        </span>
      </div>

      <ul v-if="visible.length" class="todo-list">
        <li v-for="t in visible" :key="t.id" class="todo-row" :class="{ done: t.done }">
          <el-tooltip :disabled="canDone" content="当前账号没有办结待办权限" placement="top">
            <span class="check-wrap">
              <el-checkbox
                :model-value="t.done"
                :disabled="!canDone"
                @change="(v) => todoStore.toggle(t.id, v)"
              />
            </span>
          </el-tooltip>

          <span class="priority-bar" :class="'p-' + t.priority"></span>

          <div class="todo-main">
            <div class="todo-line">
              <span class="todo-title">{{ t.title }}</span>
              <el-tag size="small" round effect="light" :type="priorityType(t.priority)">
                {{ t.priority }}
              </el-tag>
              <el-tag size="small" round effect="plain" type="info">{{ t.type }}</el-tag>
            </div>
            <div class="todo-meta">
              <span class="meta-item">
                {{ t.relatedType || '关联' }}：<span class="rel-link" @click.stop="openRelated(t)">{{ t.related || '—' }}</span>
              </span>
              <span class="meta-item due" :class="{ overdue: isOverdue(t) }">
                <el-icon :size="12"><Calendar /></el-icon>
                {{ dueText(t) }}
              </span>
            </div>
          </div>

          <el-button link type="danger" @click="onRemove(t)">删除</el-button>
        </li>
      </ul>

      <div v-else class="empty-box">
        <el-icon :size="30" color="#c3cddd"><CircleCheck /></el-icon>
        <p>{{ active === 'done' ? '还没有办结的待办' : '这里空了，今天的活都干完了' }}</p>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新增待办" width="520px" align-center destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px" class="dialog-form">
        <el-form-item label="待办事项" prop="title">
          <el-input v-model="form.title" placeholder="如：车匠连锁：发年度框架报价单" maxlength="60" clearable />
        </el-form-item>
        <el-form-item label="待办类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="o in TYPES" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio-button v-for="o in PRIORITIES" :key="o" :value="o">{{ o }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截止日期" prop="due">
          <el-date-picker v-model="form.due" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="关联对象">
          <el-input v-model="form.related" placeholder="客户单位 / 合同 / 内部事项" maxlength="40" clearable />
        </el-form-item>
        <el-form-item label="关联模块">
          <el-select v-model="form.link" style="width: 100%" clearable placeholder="选中后，待办可一键跳到该页面">
            <el-option v-for="o in LINK_OPTIONS" :key="o.path" :label="o.label" :value="o.path" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Calendar, InfoFilled, CircleCheck } from '@element-plus/icons-vue'
import { useTodoStore } from '@/stores/todo'
import { useUserStore } from '@/stores/user'
import { usePermission } from '@/composables/usePermission'
import { findCustomer } from '@/utils/links'
import { getRoleProfile } from '@/config/roleProfile'

const router = useRouter()
const route = useRoute()

const todoStore = useTodoStore()
const userStore = useUserStore()
const { has } = usePermission()

/** 当前岗位画像：决定待办类型、岗位说明与「关联对象」跳去哪儿 */
const profile = computed(() => getRoleProfile(userStore.userInfo))

todoStore.load()

const canAdd = computed(() => has('todo:add'))
const canDone = computed(() => has('todo:done'))

/** 类型下拉 = 本岗位的专属类型 + 历史待办里出现过的类型，老数据也能编辑 */
const TYPES = computed(() => {
  const extra = todoStore.items.map((t) => t.type).filter(Boolean)
  return [...new Set([...profile.value.todoTypes, ...extra])]
})
const PRIORITIES = ['紧急', '重要', '一般']

/** 新增待办时可以把「关联模块」选成自己有权限的页面 */
const LINK_OPTIONS = computed(() => (profile.value.shortcuts || []).filter((s) => has(s.perm)))

const today = new Date().toISOString().slice(0, 10)
const dayOf = (d) => new Date(d + 'T00:00:00')
const todayDate = dayOf(today)

const isOverdue = (t) => !t.done && t.due && t.due < today

function dueText(t) {
  if (!t.due) return '未设截止日期'
  if (t.due === today) return '今天到期'
  const diff = Math.round((dayOf(t.due) - todayDate) / 86400000)
  if (diff === 1) return '明天到期'
  if (diff === -1) return '已逾期 1 天'
  if (diff < -1) return `已逾期 ${-diff} 天`
  return `${t.due.slice(5).replace('-', '月')}日到期`
}

// 顶部四张统计卡本身就是页签入口，点哪张就切到对应列表
const stats = computed(() => [
  { label: '待处理', value: todoStore.pending.length, tone: '', tab: 'all' },
  {
    label: '今日到期',
    value: todoStore.pending.filter((t) => t.due === today).length,
    tone: 'warn',
    tab: 'today'
  },
  { label: '已逾期', value: todoStore.overdue.length, tone: 'danger', tab: 'overdue' },
  { label: '已办结', value: todoStore.done.length, tone: 'ok', tab: 'done' }
])

const active = ref('all')

// 概览顶部的「今日待办 / 逾期」胶囊会带 ?tab= 过来，落地就停在那一个页签
const TAB_KEYS = ['all', 'today', 'overdue', 'done']
if (TAB_KEYS.includes(String(route.query.tab))) active.value = String(route.query.tab)
watch(
  () => route.query.tab,
  (v) => {
    if (TAB_KEYS.includes(String(v))) active.value = String(v)
  }
)

const tabs = computed(() => [
  { key: 'all', label: '全部', count: todoStore.pending.length },
  {
    key: 'today',
    label: '今天',
    count: todoStore.pending.filter((t) => t.due === today).length
  },
  { key: 'overdue', label: '已逾期', count: todoStore.overdue.length },
  { key: 'done', label: '已完成', count: todoStore.done.length }
])

const visible = computed(() => {
  const list = todoStore.items
  if (active.value === 'today') return list.filter((t) => !t.done && t.due === today)
  if (active.value === 'overdue') return list.filter((t) => isOverdue(t))
  if (active.value === 'done') return list.filter((t) => t.done)
  return list.filter((t) => !t.done)
})

function priorityType(p) {
  return p === '紧急' ? 'danger' : p === '重要' ? 'warning' : 'info'
}

/**
 * 点「关联对象」去哪儿，按岗位决定：
 * 1. 待办自带 link（岗位模板生成、或新增时选了关联模块）→ 直接去那个业务页
 * 2. 关联的是客户名 → 有客户档案权限的进客户 360，没有的提示去对应模块
 * 营销的待办关联线索、财务的关联应收、生产的关联发货，都是靠第 1 条实现的
 */
function openRelated(t) {
  if (t.link) {
    router.push(t.link)
    return
  }
  if (!t.related) return
  // 没有客户档案权限的岗位（营销 / 生产）不跳 360，避免点一下就落到 403
  if (!has('customer:view')) {
    ElMessage.info(`这条待办关联的是「${t.related}」，当前岗位没有客户档案权限`)
    return
  }
  const hit = findCustomer(t.related)
  if (hit) router.push(`/customer/detail/${hit.id}`)
  else ElMessage.info('这条待办关联的不是客户档案，可在对应模块里搜索')
}

// ---------- 新增 ----------
const dialogVisible = ref(false)
const formRef = ref()
const defaultType = () => profile.value.todoTypes[0] || '其他事项'
const form = reactive({ title: '', type: defaultType(), priority: '重要', due: today, related: '', link: '' })
const rules = {
  title: [{ required: true, message: '请填写待办事项', trigger: 'blur' }],
  type: [{ required: true, message: '请选择待办类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  due: [{ required: true, message: '请选择截止日期', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, { title: '', type: defaultType(), priority: '重要', due: today, related: '', link: '' })
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const picked = LINK_OPTIONS.value.find((o) => o.path === form.link)
  await todoStore.add({ ...form, relatedType: picked ? picked.label : '关联' })
  dialogVisible.value = false
  ElMessage.success('待办已添加')
}

async function onRemove(t) {
  try {
    await ElMessageBox.confirm(`确定删除「${t.title}」吗？`, '删除待办', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await todoStore.remove(t.id)
  ElMessage.success('已删除')
}
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 10px;
}

/* 岗位视角条：一条浅色带，说明这套待办是按什么岗位给的 */
.role-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 9px 14px;
  border-radius: 10px;
  background: var(--soft, #eef3ff);
  border: 1px solid #e6ebf5;
  margin-bottom: 14px;
  font-size: 12.5px;
  color: #41506a;
}

.role-tag {
  font-weight: 600;
  color: var(--accent, #2f5fe0);
  background: #fff;
  border-radius: 6px;
  padding: 2px 9px;
}

.role-desk {
  font-weight: 500;
  color: #1f2b3d;
}

.role-duty {
  display: inline-flex;
  gap: 6px;
  flex-wrap: wrap;
}

.role-duty span {
  color: #5c6b82;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 999px;
  padding: 1px 9px;
  font-size: 11.5px;
}

.role-note {
  margin-left: auto;
  color: #8b98ab;
  font-size: 11.5px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.stat-card {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  transition: box-shadow 0.18s ease, border-color 0.18s ease;
}

.stat-card:hover {
  border-color: #c9d8ff;
  box-shadow: 0 8px 18px rgba(47, 95, 224, 0.1);
}

/* 当前页签对应的统计卡给出明确状态 */
.stat-card.active {
  border-color: #9db8ff;
  box-shadow: 0 6px 16px rgba(47, 95, 224, 0.14);
}

.stat-label {
  font-size: 12.5px;
  color: var(--text-sub);
}

.stat-value {
  font-size: 22px;
  font-weight: 650;
  color: #1f2b3d;
  font-variant-numeric: tabular-nums;
}

.stat-value.warn {
  color: #b45309;
}
.stat-value.danger {
  color: #c0392b;
}
.stat-value.ok {
  color: #0f9d76;
}

.board-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}

.tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid transparent;
  background: #f5f7fb;
  color: #52627a;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tab:hover {
  color: #2f5fe0;
  background: #eef3ff;
}

.tab.active {
  background: #eef3ff;
  border-color: #cad9ff;
  color: #2f5fe0;
  font-weight: 500;
}

.tab-count {
  font-size: 11.5px;
  color: #97a3b6;
  font-variant-numeric: tabular-nums;
}

.tab.active .tab-count {
  color: #2f5fe0;
}

.tab-count.warn {
  color: #c0392b;
}

.board-tip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #97a3b6;
}

.todo-list {
  list-style: none;
  margin: 0;
  padding: 4px 0;
}

.todo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-bottom: 1px solid #f2f5fa;
  transition: background 0.15s ease;
}

.todo-row:hover {
  background: #fafbfe;
}

.todo-row:last-child {
  border-bottom: none;
}

.todo-row.done .todo-title {
  color: #a7b2c4;
  text-decoration: line-through;
}

.check-wrap {
  display: inline-flex;
}

.priority-bar {
  width: 3px;
  height: 26px;
  border-radius: 3px;
  background: #dfe5f0;
  flex-shrink: 0;
}
.priority-bar.p-紧急 {
  background: #e05252;
}
.priority-bar.p-重要 {
  background: #f0b429;
}
.priority-bar.p-一般 {
  background: #9db6ff;
}

.todo-main {
  flex: 1;
  min-width: 0;
}

.todo-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.todo-title {
  font-size: 14px;
  color: #273449;
  font-weight: 500;
}

.todo-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 5px;
  font-size: 12px;
  color: #97a3b6;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 关联客户可点：直接进客户 360 看全貌 */
.rel-link {
  color: #2f5fe0;
  cursor: pointer;
  border-bottom: 1px dashed transparent;
}

.rel-link:hover {
  border-bottom-color: #2f5fe0;
}

.due.overdue {
  color: #c0392b;
  font-weight: 500;
}

.empty-box {
  padding: 46px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #97a3b6;
  font-size: 13px;
}

.empty-box p {
  margin: 0;
}
</style>
