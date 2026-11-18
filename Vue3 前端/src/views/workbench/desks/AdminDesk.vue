<template>
  <div class="admin-desk">
    <!-- 管理员看的是「系统本身」：有多少人在用、角色怎么配、权限有没有漏 -->
    <div class="dk-kpi-grid">
      <div
        v-for="k in kpis"
        :key="k.label"
        class="dk-kpi page-card"
        :class="{ click: !!k.to }"
        :title="k.to ? `查看${k.drill}` : ''"
        @click="k.to && router.push(k.to)"
      >
        <div class="dk-kpi-top">
          <span class="dk-kpi-label">{{ k.label }}</span>
          <span class="dk-kpi-icon" :style="{ background: k.bg, color: k.color }">
            <el-icon :size="15"><component :is="k.icon" /></el-icon>
          </span>
        </div>
        <div class="dk-kpi-value">{{ k.value }}</div>
        <div class="dk-kpi-foot">
          <span class="dk-delta" :class="k.trend">{{ k.delta }}</span>
          <span>{{ k.sub }}</span>
        </div>
      </div>
    </div>

    <div class="dk-two-col">
      <!-- 角色编制：每个角色几个人、多少功能 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>角色编制</h3>
            <p>每个角色配了多少功能、几个在岗人员；点一行去角色管理调配</p>
          </div>
          <button class="dk-link" @click="router.push('/system/role')">
            角色管理<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="roleRows.length" class="dk-table">
          <thead>
            <tr>
              <th>角色</th>
              <th class="w-code">编码</th>
              <th class="num">功能数</th>
              <th class="num">在岗</th>
              <th class="w-bar">功能覆盖</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="r in roleRows"
              :key="r.id"
              class="click"
              :title="`去角色管理查看「${r.name}」`"
              @click="router.push('/system/role')"
            >
              <td>
                <span class="role-name">{{ r.name }}</span>
                <span v-if="r.builtin" class="dk-tag info">内置</span>
              </td>
              <td class="mono muted">{{ r.code }}</td>
              <td class="num">{{ r.count }}</td>
              <td class="num">{{ r.people }}</td>
              <td>
                <div class="dk-bar"><span :style="{ width: r.rate + '%' }"></span></div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">正在读取角色配置…</div>
      </section>

      <!-- 权限覆盖：按模块统计功能点，防止「角色漏配」 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>权限模块</h3>
            <p>系统共 {{ permissionTotal }} 个功能点，按模块分布</p>
          </div>
          <button class="dk-link" @click="router.push('/system/permission')">
            权限授权<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <ul class="dk-list">
          <li
            v-for="g in moduleGroups"
            :key="g.module"
            class="dk-row click"
            :title="`去权限授权查看「${g.module}」`"
            @click="router.push('/system/permission')"
          >
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ g.module }}</span>
                <span class="dk-row-side">{{ g.items.length }} 项</span>
              </div>
              <div class="dk-bar"><span :style="{ width: (g.items.length / maxModule) * 100 + '%' }"></span></div>
            </div>
          </li>
        </ul>
      </section>
    </div>

    <div class="dk-two-col even">
      <!-- 账号清单：最近新增的账号与岗位 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>账号与岗位</h3>
            <p>按工号排序；点一行去员工管理调整部门与角色</p>
          </div>
          <button class="dk-link" @click="router.push('/system/employee')">
            员工管理<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <table v-if="staffPreview.length" class="dk-table">
          <thead>
            <tr>
              <th>工号</th>
              <th>姓名</th>
              <th>部门</th>
              <th class="w-role">角色</th>
              <th class="w-scope">范围</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="e in staffPreview"
              :key="e.id"
              class="click"
              @click="router.push('/system/employee')"
            >
              <td class="mono muted">{{ e.empNo }}</td>
              <td class="strong">{{ e.name }}</td>
              <td class="muted">{{ e.dept }}</td>
              <td>{{ (e.roleNames || []).join(' / ') || '—' }}</td>
              <td><span class="dk-tag" :class="scopeTone(e.dataScope)">{{ scopeText(e.dataScope) }}</span></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dk-empty">正在读取账号列表…</div>
      </section>

      <!-- 部门结构：部门决定注册时拿到的角色 -->
      <section class="dk-panel page-card">
        <header class="dk-head">
          <div>
            <h3>部门结构</h3>
            <p>新用户注册时按所选部门自动分配角色，部门配置别漏</p>
          </div>
          <button class="dk-link" @click="router.push('/system/dept')">
            部门管理<el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <ul class="dk-list">
          <li
            v-for="d in deptRows"
            :key="d.id"
            class="dk-row click"
            @click="router.push('/system/dept')"
          >
            <span class="dept-dot" :class="{ child: d.parentId }"></span>
            <div class="dk-row-main">
              <div class="dk-row-line">
                <span class="dk-row-title">{{ d.name }}</span>
                <span class="dk-row-side">{{ deptHeadcount(d) }} 人</span>
              </div>
            </div>
          </li>
          <li v-if="!deptRows.length" class="dk-empty" style="padding: 18px 0">正在读取部门…</li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Connection, Key, OfficeBuilding, User } from '@element-plus/icons-vue'
import { useOrgStore } from '@/stores/org'

defineProps({
  profile: { type: Object, required: true }
})

const router = useRouter()
const org = useOrgStore()

onMounted(() => {
  if (!org.ready) org.loadAll()
})

const permissionTotal = computed(() => org.permissions.length)
const maxModule = computed(() => Math.max(1, ...org.functionGroups.map((g) => g.items.length)))

const moduleGroups = computed(() =>
  org.functionGroups.slice().sort((a, b) => b.items.length - a.items.length)
)

const activeStaff = computed(() => org.employees.filter((e) => e.status !== '停用'))

const roleRows = computed(() => {
  const max = Math.max(1, ...org.roles.map((r) => org.rolePermissionCount(r)))
  return org.roles.map((r) => ({
    id: r.id,
    code: r.code,
    name: r.name,
    builtin: r.builtin,
    count: org.rolePermissionCount(r),
    people: org.employees.filter((e) => (e.roleIds || []).includes(r.id) && e.status !== '停用').length,
    rate: Math.round((org.rolePermissionCount(r) / max) * 100)
  }))
})

const staffPreview = computed(() =>
  org.employees
    .slice()
    .sort((a, b) => String(a.empNo).localeCompare(String(b.empNo)))
    .slice(0, 8)
)

/** 部门是后端返回的拍平列表（带 parentId / employeeCount），按父级+排序号展示 */
const deptRows = computed(() =>
  org.depts
    .slice()
    .sort((a, b) => String(a.parent || '').localeCompare(String(b.parent || '')) || (a.sortOrder || 0) - (b.sortOrder || 0))
)

const deptHeadcount = (d) => d.count ?? org.employees.filter((e) => e.dept === d.name).length

const scopeText = (s) => (s === 'SELF' ? '仅本人' : s === 'GROUP' ? '本组' : '全公司')
const scopeTone = (s) => (s === 'SELF' ? 'info' : s === 'GROUP' ? 'warn' : 'ok')

const kpis = computed(() => [
  {
    label: '在岗账号',
    value: activeStaff.value.length,
    delta: `全部 ${org.employees.length} 个`,
    trend: 'up',
    sub: '员工与登录账号',
    icon: User,
    bg: '#eef0ff',
    color: '#4b5bdc',
    to: '/system/employee',
    drill: '员工管理'
  },
  {
    label: '角色',
    value: org.roles.length,
    delta: `${org.roles.filter((r) => r.builtin).length} 个内置`,
    trend: 'info',
    sub: '含销售分组角色',
    icon: Connection,
    bg: '#e8f8f2',
    color: '#0f9d76',
    to: '/system/role',
    drill: '角色管理'
  },
  {
    label: '功能点',
    value: permissionTotal.value,
    delta: `${moduleGroups.value.length} 个模块`,
    trend: 'up',
    sub: '功能权限颗粒度',
    icon: Key,
    bg: '#f3edff',
    color: '#7c3aed',
    to: '/system/permission',
    drill: '权限授权'
  },
  {
    label: '部门',
    value: deptRows.value.length,
    delta: '决定新账号角色',
    trend: 'info',
    sub: '注册时按部门授权',
    icon: OfficeBuilding,
    bg: '#fdf4e3',
    color: '#b45309',
    to: '/system/dept',
    drill: '部门管理'
  }
])
</script>

<style scoped>
.w-code {
  width: 100px;
}

.w-bar {
  width: 110px;
}

.w-role {
  width: 140px;
}

.w-scope {
  width: 84px;
}

.role-name {
  font-weight: 500;
  margin-right: 6px;
}

.dept-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4b5bdc;
  flex-shrink: 0;
}

.dept-dot.child {
  background: #c3cddd;
  margin-left: 10px;
}
</style>
