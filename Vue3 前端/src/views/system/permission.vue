<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">功能授权</h2>
        <p class="page-desc">选一个角色，勾上它要用的功能，保存即可。员工通过角色获得功能，一个员工多个角色时功能取并集。</p>
      </div>
    </div>

    <div v-if="!canAssign" class="notice notice-warn">
      当前账号没有授权操作权限，下面内容仅可查看。
    </div>

    <div class="grant-layout">
      <!-- 角色 -->
      <aside class="role-panel page-card">
        <div class="role-panel-head">角色（{{ orgStore.roles.length }}）</div>
        <div class="role-list">
          <div
            v-for="r in roleList"
            :key="r.id"
            class="role-item"
            :class="{ active: r.id === selectedRoleId, child: r.depth > 0 }"
            @click="selectRole(r.id)"
          >
            <div class="role-item-main">
              <span class="role-item-name">{{ r.name }}</span>
              <span class="role-item-code">{{ r.code }}</span>
            </div>
            <span class="role-item-count" :class="{ full: r.builtin }">
              {{ r.builtin ? '全部' : orgStore.rolePermissionCount(r) }}
            </span>
          </div>
        </div>
      </aside>

      <!-- 功能 -->
      <section class="grant-panel page-card">
        <div class="panel-head">
          <div class="head-main">
            <h3>
              {{ selectedRole?.name }}
              <el-tag v-if="selectedRole?.builtin" size="small" type="warning" effect="light" round>内置</el-tag>
              <el-tag v-if="selectedRole?.status === '停用'" size="small" type="info" effect="light" round>已停用</el-tag>
            </h3>
            <p class="head-desc">{{ selectedRole?.desc || '该角色暂无说明' }}</p>
            <p class="head-meta">
              已选 <b>{{ checkedCodes.length }}</b> / {{ orgStore.permissions.length }} 项功能 ·
              关联员工 {{ employeeCount }} 名
            </p>
          </div>
        </div>

        <div v-if="selectedRole?.builtin" class="notice notice-lock">
          <el-icon><Lock /></el-icon>
          管理员固定拥有全部功能，不需要也不能修改。
        </div>

        <div class="panel-body">
          <FunctionGrant v-model="checkedCodes" :disabled="!canEdit" />
        </div>

        <div class="panel-foot">
          <span class="foot-tip" :class="{ active: dirty }">
            {{ dirty ? '有改动尚未保存' : '当前已是最新授权' }}
          </span>
          <el-button :disabled="!canEdit || !dirty" @click="resetChecked">还原</el-button>
          <el-button type="primary" :disabled="!canEdit || !dirty" @click="saveChecked">保存授权</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import FunctionGrant from '@/components/FunctionGrant.vue'
import { useOrgStore } from '@/stores/org'
import { useUserStore } from '@/stores/user'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const orgStore = useOrgStore()
const userStore = useUserStore()
const { has } = usePermission()

const canAssign = computed(() => has('system:permission:assign'))

// 角色列表（带层级缩进，销售下面挂小组）
const roleList = computed(() => {
  const out = []
  const walk = (list, depth) => {
    list.forEach((r) => {
      out.push({ ...r, depth })
      const children = orgStore.roles.filter((c) => c.parentId === r.id)
      if (children.length) walk(children, depth + 1)
    })
  }
  walk(
    orgStore.roles.filter((r) => !r.parentId || !orgStore.roleById(r.parentId)),
    0
  )
  return out
})

const selectedRoleId = ref(orgStore.roles.find((r) => !r.builtin)?.id || 1)
const selectedRole = computed(() => orgStore.roleById(selectedRoleId.value))
const canEdit = computed(() => canAssign.value && !selectedRole.value?.builtin)
const employeeCount = computed(() =>
  selectedRole.value ? orgStore.employeeCountOfRole(selectedRole.value.id) : 0
)

const checkedCodes = ref([])
const originalCodes = ref([])
const dirty = computed(() => {
  const a = [...checkedCodes.value].sort().join(',')
  const b = [...originalCodes.value].sort().join(',')
  return a !== b
})

function syncFromRole() {
  const role = selectedRole.value
  if (!role) return
  const codes = role.builtin ? [...orgStore.allPermissionCodes] : [...(role.permissionIds || [])]
  checkedCodes.value = [...codes]
  originalCodes.value = [...codes]
}

function selectRole(id) {
  if (dirty.value) {
    ElMessage.warning('当前改动还没保存，切换角色后已还原')
  }
  selectedRoleId.value = id
}

watch(selectedRole, syncFromRole, { immediate: true })

// 角色是异步加载的：加载完成后再决定选中哪个角色，
// 优先用地址栏的 ?role=<id>（从角色管理点「授权」跳过来），否则选第一个非内置角色
function applyPreferredRole() {
  const rid = Number(route.query.role)
  if (rid && orgStore.roleById(rid)) {
    selectedRoleId.value = rid
    return true
  }
  if (orgStore.roleById(selectedRoleId.value)) return true
  selectedRoleId.value =
    orgStore.roles.find((r) => !r.builtin)?.id || orgStore.roles[0]?.id || 0
  return false
}

watch([() => orgStore.roles.length, () => route.query.role], applyPreferredRole, { immediate: true })

onMounted(() => {
  if (!orgStore.roles.length) orgStore.loadAll()
})

function resetChecked() {
  syncFromRole()
  ElMessage.info('已还原为保存前的授权')
}

async function saveChecked() {
  const role = selectedRole.value
  if (!role) return
  try {
    await orgStore.setRolePermissions(role.id, checkedCodes.value)
  } catch {
    return
  }
  originalCodes.value = [...checkedCodes.value]
  ElMessage.success(
    employeeCount.value
      ? `「${role.name}」已保存，${employeeCount.value} 名员工的功能同步更新`
      : `「${role.name}」已保存`
  )
  // 若改的是自己所属角色，立即刷新当前用户的权限
  await userStore.refreshMe()
}
</script>

<style scoped>
.notice {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  border-radius: 8px;
  padding: 9px 13px;
  margin-bottom: 12px;
  line-height: 1.5;
}

.notice-warn {
  color: #9a6a00;
  background: #fdf8ec;
  border: 1px solid #f5e3bd;
}

.notice-lock {
  margin: 0 18px 12px;
  color: #b07d1e;
  background: #fdf8ec;
  border: 1px solid #f5e3bd;
}

.grant-layout {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

/* ---------- 左：角色 ---------- */
.role-panel {
  width: 236px;
  flex-shrink: 0;
  padding: 6px;
  align-self: stretch;
}

.role-panel-head {
  font-size: 12px;
  color: #97a3b6;
  padding: 9px 10px 7px;
}

.role-list {
  max-height: calc(100vh - 260px);
  overflow-y: auto;
}

.role-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s ease;
}

.role-item.child {
  padding-left: 24px;
  position: relative;
}

.role-item.child::before {
  content: '';
  position: absolute;
  left: 13px;
  top: 50%;
  width: 6px;
  height: 1px;
  background: #d3dcea;
}

.role-item:hover {
  background: #f2f6fd;
}

.role-item.active {
  background: #eef3ff;
  border-color: #cfe0ff;
}

.role-item-main {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.role-item-name {
  font-size: 13.5px;
  color: #273449;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-item-code {
  font-size: 11px;
  color: #a3afc2;
  font-family: Consolas, Menlo, monospace;
}

.role-item-count {
  font-size: 11.5px;
  color: #6b7a90;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 1px 8px;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.role-item-count.full {
  color: #c8891a;
  border-color: #f3dda5;
  background: #fdf8ec;
}

/* ---------- 右：功能 ---------- */
.grant-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px 14px;
  border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}

.head-main {
  min-width: 0;
}

.panel-head h3 {
  margin: 0 0 5px;
  font-size: 16px;
  color: #1f2b3d;
  display: flex;
  align-items: center;
  gap: 8px;
}

.head-desc {
  margin: 0 0 6px;
  font-size: 12.5px;
  color: #8493a8;
  line-height: 1.55;
  max-width: 560px;
}

.head-meta {
  margin: 0;
  font-size: 12.5px;
  color: #6b7a90;
}

.head-meta b {
  color: #2f5fe0;
  font-weight: 600;
}

.panel-body {
  padding: 14px 18px;
  max-height: calc(100vh - 390px);
  overflow-y: auto;
}

.panel-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 11px 18px;
  border-top: 1px solid var(--border);
  background: #fbfcfe;
  border-radius: 0 0 12px 12px;
}

.foot-tip {
  margin-right: auto;
  font-size: 12.5px;
  color: #a3afc2;
}

.foot-tip.active {
  color: #d98b12;
}

@media (max-width: 980px) {
  .grant-layout {
    flex-direction: column;
  }
  .role-panel {
    width: 100%;
  }
  .role-list {
    max-height: 200px;
  }
}
</style>
