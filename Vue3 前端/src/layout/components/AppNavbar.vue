<template>
  <header class="navbar">
    <div class="nav-left">
      <el-tooltip :content="collapsed ? '展开菜单' : '收起菜单'" placement="bottom" :show-after="300">
        <button class="icon-btn" @click="$emit('toggle')">
          <el-icon :size="18"><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
        </button>
      </el-tooltip>

      <el-breadcrumb separator="/" class="crumbs">
        <el-breadcrumb-item v-for="(item, idx) in breadcrumbs" :key="idx">
          <span :class="{ current: idx === breadcrumbs.length - 1 }">{{ item.meta.title }}</span>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="nav-right">
      <!-- 待办提醒：和「工作台 › 我的待办」是同一份数据，这里可直接勾掉办结 -->
      <el-popover
        v-if="canTodo"
        v-model:visible="todoVisible"
        trigger="click"
        placement="bottom-end"
        :width="344"
        :show-arrow="false"
        :offset="10"
        popper-class="todo-popper"
      >
        <template #reference>
          <button class="icon-btn" :class="{ 'has-overdue': overdueCount > 0 }" title="待办提醒">
            <el-badge :value="pendingCount" :max="99" :hidden="!pendingCount" type="danger">
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
          </button>
        </template>

        <div class="todo-pop">
          <div class="pop-head">
            <span class="pop-title">待办提醒</span>
            <span class="pop-sub">
              <template v-if="!pendingCount">今天没有待处理事项</template>
              <template v-else-if="overdueCount">
                <b class="danger">{{ overdueCount }}</b> 项已逾期
              </template>
              <template v-else>暂无逾期，按计划推进</template>
            </span>
          </div>

          <ul v-if="reminders.length" class="pop-list">
            <li v-for="t in reminders" :key="t.id" class="pop-row" :class="{ overdue: isOverdue(t) }">
              <el-tooltip :disabled="canDone" content="当前账号没有办结待办权限" placement="left">
                <span class="pop-check">
                  <el-checkbox :model-value="false" :disabled="!canDone" @change="() => doneOne(t)" />
                </span>
              </el-tooltip>
              <div class="pop-main" @click="gotoTodo">
                <span class="pop-text">{{ t.title }}</span>
                <span class="pop-due" :class="{ warn: isOverdue(t) }">
                  {{ dueText(t) }}
                  <span class="pop-sep">·</span>
                  {{ t.type }}
                </span>
              </div>
              <span class="pop-pri" :class="'pri-' + t.priority">{{ t.priority }}</span>
            </li>
          </ul>

          <div v-else class="pop-empty">
            <el-icon :size="16"><CircleCheck /></el-icon>
            待办都办完了
          </div>

          <div class="pop-foot">
            <span class="foot-count">共 {{ pendingCount }} 项待处理</span>
            <el-button link type="primary" @click="gotoTodo">查看全部待办 →</el-button>
          </div>
        </div>
      </el-popover>

      <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'" placement="bottom" :show-after="300">
        <button class="icon-btn" @click="toggleFullscreen">
          <el-icon :size="18"><FullScreen /></el-icon>
        </button>
      </el-tooltip>

      <span class="divider"></span>

      <el-dropdown trigger="click" @command="onCommand">
        <div class="user-box">
          <div class="avatar" :class="{ 'is-admin': isAdmin }">{{ avatarText }}</div>
          <div class="user-meta">
            <span class="user-name">{{ userName }}</span>
            <span class="user-role">{{ userRole }}</span>
          </div>
          <el-icon class="arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人信息
            </el-dropdown-item>
            <el-dropdown-item command="switch">
              <el-icon><Switch /></el-icon>切换账号
              <span v-if="userStore.sessions.length > 1" class="cmd-badge">{{ userStore.sessions.length }}</span>
            </el-dropdown-item>
            <el-dropdown-item command="password">
              <el-icon><Lock /></el-icon>修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <ChangePasswordDialog v-model="passwordVisible" />
    <SwitchAccountDialog v-model="switchVisible" @switched="onSwitched" />
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useTodoStore } from '@/stores/todo'
import { usePermission } from '@/composables/usePermission'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import SwitchAccountDialog from '@/components/SwitchAccountDialog.vue'

defineProps({
  collapsed: { type: Boolean, default: false }
})
defineEmits(['toggle'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const todoStore = useTodoStore()
const { roleNames, isAdmin, has } = usePermission()

const isFullscreen = ref(false)
const passwordVisible = ref(false)
const switchVisible = ref(false)

// ---------- 待办提醒（与工作台 › 我的待办同一份数据，按账号存本地） ----------
const todoVisible = ref(false)
const canTodo = computed(() => has('todo:view'))
const canDone = computed(() => has('todo:done'))
const pendingCount = computed(() => todoStore.pending.length)
const overdueCount = computed(() => todoStore.overdue.length)

// 顶栏一直存在，登录后就把当前账号的待办读进来，徽标数才不会只有进了待办页才更新
todoStore.load()

const todayStr = () => new Date().toISOString().slice(0, 10)
const isOverdue = (t) => !t.done && t.due && t.due < todayStr()

/** 提醒列表：逾期排前面，其次按截止日期，最多 5 条 */
const reminders = computed(() =>
  [...todoStore.pending]
    .sort((a, b) => String(a.due || '9999').localeCompare(String(b.due || '9999')))
    .slice(0, 5)
)

function dueText(t) {
  const today = todayStr()
  if (!t.due) return '未设截止日期'
  if (t.due === today) return '今天到期'
  const diff = Math.round((new Date(t.due + 'T00:00:00') - new Date(today + 'T00:00:00')) / 86400000)
  if (diff === 1) return '明天到期'
  if (diff === -1) return '已逾期 1 天'
  if (diff < -1) return `已逾期 ${-diff} 天`
  return `${t.due.slice(5).replace('-', '月')}日到期`
}

async function doneOne(t) {
  if (!canDone.value) return
  await todoStore.toggle(t.id, true)
  ElMessage.success(`已办结：${t.title}`)
}

function gotoTodo() {
  todoVisible.value = false
  if (route.path !== '/workbench/todo') router.push('/workbench/todo')
}

const breadcrumbs = computed(() => route.matched.filter((r) => r.meta?.title))
const userName = computed(() => userStore.userInfo?.name || '未登录')
const avatarText = computed(() => (userStore.userInfo?.name || 'U').slice(0, 1))
const userRole = computed(() => {
  const names = roleNames.value
  if (!names.length) return '未分配角色'
  if (names.length <= 2) return names.join(' · ')
  return `${names[0]} 等 ${names.length} 个角色`
})

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.()
    isFullscreen.value = true
  } else {
    document.exitFullscreen?.()
    isFullscreen.value = false
  }
}

async function onCommand(cmd) {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm(
        userStore.sessions.length > 1
          ? `确定退出当前账号？其他 ${userStore.sessions.length - 1} 个账号的登录状态仍会保留。`
          : '确定要退出登录吗？',
        '提示',
        {
          confirmButtonText: '退出',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return
    }
    await userStore.logout()
    ElMessage.success('已退出当前账号')
    router.replace('/login')
    return
  }
  if (cmd === 'switch') {
    switchVisible.value = true
    return
  }
  if (cmd === 'profile') {
    ElMessage.info(
      `员工编号：${userStore.userInfo?.empNo || '—'}｜部门：${userStore.userInfo?.deptName || '—'}｜角色：${roleNames.value.join('、') || '未分配'}`
    )
    return
  }
  if (cmd === 'password') {
    passwordVisible.value = true
    return
  }
  ElMessage.info('功能开发中')
}

/** 切换账号后：回到首页，由路由守卫按新账号权限落到第一个可访问页面 */
function onSwitched(user) {
  if (!user) {
    router.replace('/login')
    return
  }
  router.replace('/').catch(() => {})
}
</script>

<style scoped>
.navbar {
  height: 62px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px 0 12px;
  background: #fff;
  border-bottom: 1px solid var(--border);
  z-index: 10;
}

.nav-left,
.nav-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: #5c6b82;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.icon-btn:hover {
  background: #f2f5fa;
  color: var(--brand);
}

/* 有逾期待办时铃铛轻微跳动，避免被忽略 */
.icon-btn.has-overdue :deep(.el-icon) {
  animation: bell-swing 2.4s ease-in-out infinite;
}

@keyframes bell-swing {
  0%,
  86%,
  100% {
    transform: rotate(0);
  }
  90% {
    transform: rotate(12deg);
  }
  94% {
    transform: rotate(-10deg);
  }
  98% {
    transform: rotate(6deg);
  }
}

.crumbs {
  margin-left: 8px;
  font-size: 13.5px;
}
.crumbs :deep(.el-breadcrumb__inner) {
  color: #8b98ab;
  font-weight: 400;
}
.crumbs .current {
  color: #273449;
  font-weight: 500;
}

.divider {
  width: 1px;
  height: 20px;
  background: #e9edf5;
  margin: 0 8px;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 8px 5px 6px;
  border-radius: 9px;
  cursor: pointer;
  transition: background 0.15s ease;
  outline: none;
}
.user-box:hover {
  background: #f4f7fc;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #5b8cff, #2f5fe0);
}

.avatar.is-admin {
  background: linear-gradient(135deg, #f0b429, #d97706);
  box-shadow: 0 0 0 2px rgba(240, 180, 41, 0.2);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.user-name {
  font-size: 13.5px;
  color: #273449;
  font-weight: 500;
}
.user-role {
  font-size: 11.5px;
  color: #97a3b6;
}

.arrow {
  color: #a7b2c4;
  font-size: 12px;
}

.cmd-badge {
  margin-left: 6px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #eef3ff;
  color: #2f5fe0;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}

@media (max-width: 720px) {
  .crumbs {
    display: none;
  }
  .user-meta {
    display: none;
  }
}
</style>

<!-- 待办提醒弹层被 teleport 到 body，scoped 样式作用不到，所以单独用非 scoped 样式 -->
<style>
.todo-popper.el-popover.el-popper {
  padding: 0;
  border-radius: 12px;
  border-color: #e8edf6;
  box-shadow: 0 12px 32px rgba(24, 42, 76, 0.12);
  overflow: hidden;
}

.todo-pop {
  font-size: 13px;
  color: #273449;
}

.todo-pop .pop-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 13px 15px 10px;
  border-bottom: 1px solid #f1f4f9;
}

.todo-pop .pop-title {
  font-size: 13.5px;
  font-weight: 600;
}

.todo-pop .pop-sub {
  font-size: 11.5px;
  color: #93a0b4;
}

.todo-pop .pop-sub .danger {
  color: #e5484d;
  font-weight: 600;
}

.todo-pop .pop-list {
  margin: 0;
  padding: 4px 0;
  list-style: none;
  max-height: 296px;
  overflow-y: auto;
}

.todo-pop .pop-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 9px 15px 9px 11px;
  cursor: default;
  transition: background 0.15s ease;
}

.todo-pop .pop-row:hover {
  background: #f7f9fd;
}

.todo-pop .pop-row.overdue {
  background: #fef7f7;
}

.todo-pop .pop-row.overdue:hover {
  background: #fdeeee;
}

.todo-pop .pop-check {
  display: inline-flex;
  padding-top: 1px;
}

.todo-pop .pop-check .el-checkbox {
  height: auto;
}

.todo-pop .pop-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  cursor: pointer;
}

.todo-pop .pop-text {
  font-size: 12.8px;
  line-height: 1.4;
  color: #273449;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.todo-pop .pop-main:hover .pop-text {
  color: #2f5fe0;
}

.todo-pop .pop-due {
  font-size: 11.5px;
  color: #93a0b4;
  font-variant-numeric: tabular-nums;
}

.todo-pop .pop-due.warn {
  color: #e5484d;
}

.todo-pop .pop-sep {
  margin: 0 3px;
  color: #c8d2e0;
}

.todo-pop .pop-pri {
  flex-shrink: 0;
  margin-top: 1px;
  padding: 0 7px;
  height: 18px;
  line-height: 18px;
  border-radius: 9px;
  font-size: 11px;
}

.todo-pop .pri-紧急 {
  color: #d33a3f;
  background: #fdeceb;
}

.todo-pop .pri-重要 {
  color: #b87503;
  background: #fdf4e3;
}

.todo-pop .pri-一般 {
  color: #6b7b93;
  background: #f1f4f9;
}

.todo-pop .pop-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 26px 0;
  color: #97a3b6;
  font-size: 12.5px;
}

.todo-pop .pop-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 15px;
  border-top: 1px solid #f1f4f9;
  background: #fbfcfe;
}

.todo-pop .foot-count {
  font-size: 11.5px;
  color: #93a0b4;
}

.todo-pop .pop-foot .el-button {
  font-size: 12.5px;
}
</style>
