<template>
  <div class="page desk" :style="{ '--accent': profile.accent, '--soft': profile.soft }">
    <!-- 岗位头：这个账号是谁、该干什么、能看到多大范围，一进来就说清楚 -->
    <section class="hero">
      <div class="hero-main">
        <div class="hero-eyebrow">
          <span class="role-dot"></span>
          岗位工作台 · {{ profile.name }}
        </div>
        <h2 class="hero-title">{{ profile.deskTitle }}</h2>
        <p class="hero-desc">{{ profile.tagline }}</p>
        <div class="duty-row">
          <span class="duty-label">核心职责</span>
          <span v-for="d in profile.duty" :key="d" class="duty-chip">{{ d }}</span>
        </div>
      </div>

      <div class="hero-side">
        <div class="who">
          <span class="who-name">{{ userName }}</span>
          <span class="who-meta">{{ deptName }} · {{ scopeLabel }}</span>
        </div>
        <div class="chip-row">
          <button
            v-if="has('todo:view')"
            class="hero-chip"
            title="查看今天到期的待办"
            @click="router.push('/workbench/todo?tab=today')"
          >
            <span class="dot warn"></span>今日待办 <b>{{ todoToday }}</b> 项
            <el-icon class="chip-go"><ArrowRight /></el-icon>
          </button>
          <button
            v-if="has('todo:view')"
            class="hero-chip"
            :class="{ alert: todoOverdue > 0 }"
            title="查看已逾期的待办"
            @click="router.push('/workbench/todo?tab=overdue')"
          >
            <span class="dot danger"></span>逾期 <b>{{ todoOverdue }}</b> 项
            <el-icon class="chip-go"><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </section>

    <!-- 快捷入口：只放这个岗位真有权限进的页 -->
    <section v-if="shortcuts.length" class="quick">
      <button
        v-for="s in shortcuts"
        :key="s.path"
        class="quick-item"
        :title="`进入${s.label}`"
        @click="router.push(s.path)"
      >
        <span class="quick-label">{{ s.label }}</span>
        <el-icon class="quick-go"><ArrowRight /></el-icon>
      </button>
    </section>

    <!-- 角色专属面板：营销看线索、财务看回款、售后看工单、生产看交付… -->
    <component :is="deskComponent" :profile="profile" />

    <!-- 今日待办：与「我的待办」同一份数据，只展示未办结的前几条 -->
    <section v-if="has('todo:view')" class="page-card panel">
      <header class="dk-head">
        <div>
          <h3>我的待办 · {{ profile.name }}专属</h3>
          <p>{{ profile.todoTypes.slice(0, 3).join(' / ') }} —— 待办只对本人可见，不受数据范围影响</p>
        </div>
        <el-button link type="primary" @click="router.push('/workbench/todo')">
          全部待办<el-icon><ArrowRight /></el-icon>
        </el-button>
      </header>

      <ul v-if="topTodos.length" class="todo-list">
        <li
          v-for="t in topTodos"
          :key="t.id"
          class="todo-row"
          :class="{ over: isOverdue(t) }"
          @click="openTodo(t)"
        >
          <span class="prio" :class="'p-' + t.priority"></span>
          <div class="todo-main">
            <div class="todo-line">
              <span class="todo-title">{{ t.title }}</span>
              <span class="dk-tag info">{{ t.type }}</span>
            </div>
            <div class="todo-meta">
              <span>{{ t.relatedType || '关联' }}：{{ t.related || '—' }}</span>
              <span :class="{ 'dk-danger': isOverdue(t) }">{{ dueText(t) }}</span>
            </div>
          </div>
          <el-icon class="todo-go"><ArrowRight /></el-icon>
        </li>
      </ul>
      <div v-else class="dk-empty">
        今天的岗位待办都清空了；需要补一条可以直接去「我的待办」新增
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useTodoStore } from '@/stores/todo'
import { usePermission } from '@/composables/usePermission'
import { scopeLabel } from '@/utils/scope'
import { getRoleProfile } from '@/config/roleProfile'
import '@/styles/desk.css'

const AdminDesk = defineAsyncComponent(() => import('./desks/AdminDesk.vue'))
const MarketDesk = defineAsyncComponent(() => import('./desks/MarketDesk.vue'))
const FinanceDesk = defineAsyncComponent(() => import('./desks/FinanceDesk.vue'))
const ServiceDesk = defineAsyncComponent(() => import('./desks/ServiceDesk.vue'))
const ProductionDesk = defineAsyncComponent(() => import('./desks/ProductionDesk.vue'))
const SalesDesk = defineAsyncComponent(() => import('./desks/SalesDesk.vue'))

const DESKS = {
  admin: AdminDesk,
  market: MarketDesk,
  finance: FinanceDesk,
  service: ServiceDesk,
  production: ProductionDesk,
  sales_lead: SalesDesk,
  sales_group: SalesDesk,
  sales_rep: SalesDesk,
  guest: null
}

const router = useRouter()
const userStore = useUserStore()
const todoStore = useTodoStore()
const { has } = usePermission()

todoStore.load()

const profile = computed(() => getRoleProfile(userStore.userInfo))
const deskComponent = computed(() => DESKS[profile.value.key] || null)
const userName = computed(() => userStore.userInfo?.name || '同事')
const deptName = computed(() => userStore.userInfo?.deptName || '未分配部门')

/** 快捷入口按权限过滤：营销不会看到「客户档案」这种点了就 403 的入口 */
const shortcuts = computed(() => (profile.value.shortcuts || []).filter((s) => has(s.perm)))

const today = new Date().toISOString().slice(0, 10)
const todoToday = computed(() => todoStore.pending.filter((t) => t.due === today).length)
const todoOverdue = computed(() => todoStore.overdue.length)

/** 逾期的排前面，其余按截止日期升序，最多 5 条 */
const topTodos = computed(() =>
  todoStore.pending
    .slice()
    .sort((a, b) => {
      const ao = isOverdue(a) ? 0 : 1
      const bo = isOverdue(b) ? 0 : 1
      if (ao !== bo) return ao - bo
      return String(a.due || '9999').localeCompare(String(b.due || '9999'))
    })
    .slice(0, 5)
)

const isOverdue = (t) => !t.done && t.due && t.due < today

function dueText(t) {
  if (!t.due) return '未设截止日期'
  if (t.due === today) return '今天到期'
  const diff = Math.round((new Date(t.due + 'T00:00:00') - new Date(today + 'T00:00:00')) / 86400000)
  if (diff === 1) return '明天到期'
  if (diff === -1) return '已逾期 1 天'
  if (diff < -1) return `已逾期 ${-diff} 天`
  return `${t.due.slice(5).replace('-', '月')}日到期`
}

/** 待办点进去，落到这条待办关联的业务页；没有关联页就回待办列表 */
function openTodo(t) {
  if (t.link) router.push(t.link)
  else router.push('/workbench/todo')
}
</script>

<style scoped>
.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  padding: 20px 22px;
  border-radius: var(--radius);
  background: linear-gradient(120deg, var(--soft), #ffffff 62%, var(--soft));
  border: 1px solid #e3eaf9;
  margin-bottom: 14px;
}

.hero-main {
  min-width: 0;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--accent);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.9);
  padding: 3px 10px;
  border-radius: 999px;
  margin-bottom: 8px;
}

.role-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
}

.hero-title {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 650;
  color: #1f2b3d;
}

.hero-desc {
  margin: 0 0 10px;
  font-size: 13px;
  color: #5c6b82;
  max-width: 720px;
  line-height: 1.6;
}

.duty-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.duty-label {
  font-size: 11.5px;
  color: #8b98ab;
  margin-right: 2px;
}

.duty-chip {
  font-size: 11.5px;
  color: #41506a;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  padding: 2px 8px;
}

.hero-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.who {
  text-align: right;
}

.who-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1f2b3d;
}

.who-meta {
  font-size: 12px;
  color: #8b98ab;
}

.chip-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.hero-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-family: inherit;
  color: #41506a;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid #e6ebf5;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
  appearance: none;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.hero-chip:hover {
  border-color: #c3d3f7;
  background: #fff;
  box-shadow: 0 6px 14px rgba(47, 95, 224, 0.12);
  transform: translateY(-1px);
}

.hero-chip.alert {
  border-color: #f6d2ce;
}

.hero-chip b {
  color: #1f2b3d;
}

.chip-go {
  font-size: 11px;
  color: #9aa8bd;
  transition: transform 0.16s ease, color 0.16s ease;
}

.hero-chip:hover .chip-go {
  color: #2f5fe0;
  transform: translateX(2px);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.dot.warn {
  background: #f0b429;
}

.dot.danger {
  background: #e05252;
}

/* 快捷入口 */
.quick {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.quick-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: inherit;
  font-size: 13px;
  color: #33415a;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.quick-item:hover {
  border-color: var(--accent);
  color: var(--accent);
  box-shadow: 0 6px 16px rgba(24, 42, 76, 0.08);
}

.quick-go {
  font-size: 11px;
  color: #a7b2c4;
}

.quick-item:hover .quick-go {
  color: var(--accent);
  transform: translateX(2px);
}

/* 待办 */
.todo-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.todo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 8px;
  border-bottom: 1px dashed #eef1f7;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.todo-row:last-child {
  border-bottom: none;
}

.todo-row:hover {
  background: #f8faff;
}

.prio {
  width: 3px;
  height: 26px;
  border-radius: 3px;
  background: #dfe5f0;
  flex-shrink: 0;
}

.prio.p-紧急 {
  background: #e05252;
}

.prio.p-重要 {
  background: #f0b429;
}

.prio.p-一般 {
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
  font-size: 13.5px;
  color: #273449;
  font-weight: 500;
}

.todo-meta {
  display: flex;
  gap: 14px;
  margin-top: 4px;
  font-size: 12px;
  color: #97a3b6;
  flex-wrap: wrap;
}

.todo-go {
  font-size: 12px;
  color: #c3cddd;
}

.todo-row:hover .todo-go {
  color: var(--accent);
}
</style>
