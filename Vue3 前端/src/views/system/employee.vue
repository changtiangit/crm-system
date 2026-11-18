<template>
  <DataPage
    title="员工管理"
    desc="维护员工账号、所属部门与角色定位；选部门会自动带出该部门对应的角色，也可再加其它角色，实际权限为各角色权限的并集。新增员工默认密码为 123456，可在此重置。"
    entity-name="员工"
    create-text="新增员工"
    search-placeholder="搜索工号 / 姓名 / 手机号"
    :rows="orgStore.employees"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['empNo', 'name', 'phone', 'dept']"
    :filters="filters"
    :create-row="createRow"
    :save-row="orgStore.saveEmployee"
    :remove-row="orgStore.removeEmployee"
    :can-create="has('system:employee:add')"
    :can-edit="has('system:employee:edit')"
    :can-delete="has('system:employee:delete')"
    :delete-guard="deleteGuard"
    @field-change="onFieldChange"
  >
    <template #before>
      <div class="identity-bar">
        <div class="identity-left">
          <span class="badge" :class="isAdmin ? 'admin' : 'normal'">
            {{ isAdmin ? '管理员' : '普通员工' }}
          </span>
          <span class="identity-text">
            当前账号角色：<b>{{ roleNames.join('、') || '未分配' }}</b>
          </span>
        </div>
        <span class="identity-tip">
          {{ isAdmin ? '拥有全部功能，可新增、删除任意员工账号' : '按所受功能维护员工，不能删除管理员账号' }}
        </span>
      </div>
    </template>
  </DataPage>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import DataPage from '@/components/DataPage.vue'
import { useOrgStore } from '@/stores/org'
import { useUserStore } from '@/stores/user'
import { usePermission } from '@/composables/usePermission'

const orgStore = useOrgStore()
const userStore = useUserStore()
const { has, isAdmin, roleNames } = usePermission()

const DEPT_OPTIONS = computed(() =>
  orgStore.depts.map((d) => ({ label: d.name, value: d.id }))
)
const ROLE_OPTIONS = computed(() =>
  orgStore.roles.map((r) => ({
    label: r.status === '停用' ? `${r.name}（停用）` : r.name,
    value: r.id
  }))
)
const STATUS = ['在职', '停用']
const POSITIONS = ['系统管理员', '销售总监', '销售组长', '销售顾问', '市场运营', '财务', '售后', '技术', '生产']
const SCOPE_OPTIONS = [
  { label: '全公司数据', value: 'ALL' },
  { label: '本组数据（本人 + 组员）', value: 'GROUP' },
  { label: '仅本人负责数据', value: 'SELF' }
]
const SCOPE_TEXT = { ALL: '全公司', GROUP: '本组', SELF: '仅本人' }

/** 换岗位时给出对应的默认数据范围，仍可手工覆盖 */
function defaultScope(position) {
  if (!position) return 'ALL'
  if (position.includes('总监') || position.includes('管理员')) return 'ALL'
  if (position.includes('组长') || position.includes('主管') || position.includes('经理')) return 'GROUP'
  if (position.startsWith('销售')) return 'SELF'
  return 'ALL'
}

const columns = [
  { prop: 'empNo', label: '员工编号', width: 120 },
  { prop: 'name', label: '姓名', type: 'strong', width: 110 },
  {
    prop: 'position',
    label: '职位',
    width: 112,
    type: 'tag',
    tagType: (v) =>
      ({ 销售总监: 'warning', 销售组长: 'primary', 销售顾问: 'success', 系统管理员: 'danger' }[v] || 'info'),
    formatter: (row) => row.position || '—'
  },
  {
    prop: 'dataScope',
    label: '数据范围',
    width: 96,
    type: 'tag',
    tagType: { ALL: 'info', GROUP: 'primary', SELF: 'warning' },
    formatter: (row) => SCOPE_TEXT[row.dataScope] || '全公司'
  },
  { prop: 'dept', label: '所属部门', minWidth: 130 },
  {
    prop: 'roleIds',
    label: '角色定位',
    type: 'tags',
    minWidth: 220,
    formatter: (row) => orgStore.employeeRoleNames(row)
  },
  { prop: 'phone', label: '手机号', width: 150 },
  {
    prop: 'status',
    label: '账号状态',
    width: 110,
    type: 'tag',
    tagType: { 在职: 'success', 停用: 'info' }
  }
]

const formFields = computed(() => [
  { prop: 'empNo', label: '员工编号', required: true },
  { prop: 'name', label: '姓名', required: true },
  { prop: 'position', label: '职位', type: 'select', options: POSITIONS },
  {
    prop: 'dataScope',
    label: '数据范围',
    type: 'select',
    options: SCOPE_OPTIONS,
    placeholder: '留空按职位自动推导'
  },
  { prop: 'deptId', label: '所属部门', type: 'select', options: DEPT_OPTIONS.value, required: true },
  { prop: 'roleIds', label: '角色定位', type: 'select-multi', options: ROLE_OPTIONS.value, required: true },
  { prop: 'phone', label: '手机号', required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入 11 位手机号（同时作为登录账号）', maxlength: 11 },
  { prop: 'status', label: '账号状态', type: 'select', options: STATUS, required: true },
  {
    prop: 'password',
    label: '登录密码',
    type: 'password',
    placeholder: '留空则不修改，新增时默认 123456'
  }
])

const filters = computed(() => [
  { prop: 'dept', label: '全部部门', options: orgStore.depts.map((d) => d.name) },
  { prop: 'status', label: '全部状态', options: STATUS }
])

const createRow = () => ({
  empNo: '',
  name: '',
  position: '',
  dataScope: 'ALL',
  deptId: orgStore.depts[0]?.id,
  roleIds: [],
  phone: '',
  status: '在职',
  password: ''
})

/** 选了部门就自动带出该部门对应的角色，避免部门与角色对不上 */
function onFieldChange(prop, value, model) {
  if (prop === 'position') {
    // 换岗位时同步一个合理的数据范围默认值，管理员仍可再改
    model.dataScope = defaultScope(value)
    return
  }
  if (prop !== 'deptId') return
  const dept = orgStore.depts.find((d) => d.id === value)
  if (dept?.roleId) model.roleIds = [dept.roleId]
}

/** 删除限制：不能删自己、非管理员不能删管理员账号、系统保留至少一个管理员 */
function deleteGuard(row) {
  if (row.empNo === userStore.userInfo?.empNo) return '不能删除当前登录账号'
  const isAdminEmployee = orgStore.isAdminRoleIds(row.roleIds)
  if (isAdminEmployee && !isAdmin.value) return '仅管理员可删除管理员账号'
  if (isAdminEmployee) {
    const adminCount = orgStore.employees.filter((e) => orgStore.isAdminRoleIds(e.roleIds)).length
    if (adminCount <= 1) return '系统需保留至少一个管理员账号'
  }
  return ''
}

onMounted(() => {
  if (!orgStore.employees.length) orgStore.loadAll()
})
</script>

<style scoped>
.identity-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 10px 16px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: linear-gradient(90deg, #f2f7ff, #f8faff);
  border: 1px solid #dde8ff;
  font-size: 13px;
  color: #52627a;
}

.identity-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.badge {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 20px;
  font-weight: 500;
}
.badge.admin {
  color: #9a6a00;
  background: #fdf3d9;
  border: 1px solid #f3dda5;
}
.badge.normal {
  color: #2f5fe0;
  background: #eef3ff;
  border: 1px solid #cfe0ff;
}

.identity-text b {
  color: #273449;
  font-weight: 600;
}

.identity-tip {
  font-size: 12.5px;
  color: #8493a8;
}
</style>
