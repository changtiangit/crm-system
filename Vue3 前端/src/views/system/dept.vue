<template>
  <DataPage
    title="部门管理"
    desc="维护企业组织架构，销售部下设销售一组、销售二组；每个部门绑一个角色，新员工注册时选部门即自动获得该角色"
    entity-name="部门"
    create-text="新增部门"
    search-placeholder="搜索部门 / 负责人"
    :rows="orgStore.depts"
    :columns="columns"
    :form-fields="formFields"
    :search-keys="['name', 'parent', 'leader']"
    :filters="filters"
    :create-row="createRow"
    :save-row="orgStore.saveDepartment"
    :remove-row="orgStore.removeDepartment"
    :can-create="has('system:dept:add')"
    :can-edit="has('system:dept:edit')"
    :can-delete="has('system:dept:delete')"
    :delete-guard="deleteGuard"
  >
    <template #before>
      <div class="org-card page-card">
        <div class="org-head">
          <span class="org-title">组织架构</span>
          <span class="org-sub">共 {{ orgStore.depts.length }} 个部门</span>
        </div>
        <el-tree
          :data="treeData"
          :props="{ label: 'label', children: 'children' }"
          default-expand-all
          :expand-on-click-node="false"
          class="org-tree"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <el-icon class="tree-icon"><OfficeBuilding /></el-icon>
              <span class="tree-label">{{ data.label }}</span>
              <span v-if="data.roleName" class="tree-role">{{ data.roleName }}</span>
              <span v-if="data.count !== undefined" class="tree-count">{{ data.count }} 人</span>
            </span>
          </template>
        </el-tree>
      </div>
    </template>
  </DataPage>
</template>

<script setup>
import { computed } from 'vue'
import { OfficeBuilding } from '@element-plus/icons-vue'
import DataPage from '@/components/DataPage.vue'
import { useOrgStore } from '@/stores/org'
import { usePermission } from '@/composables/usePermission'

const orgStore = useOrgStore()
const { has } = usePermission()

const DEPT_NAMES = computed(() => orgStore.depts.map((d) => d.name))
const LEADER_OPTIONS = computed(() => orgStore.employees.map((e) => e.name))
const ROLE_OPTIONS = computed(() => orgStore.roles.map((r) => ({ label: r.name, value: r.id })))

const columns = [
  { prop: 'name', label: '部门名称', type: 'strong', minWidth: 160 },
  { prop: 'parent', label: '上级部门', width: 130 },
  { prop: 'roleName', label: '对应角色', width: 120, type: 'tag' },
  { prop: 'leader', label: '部门负责人', width: 130 },
  { prop: 'count', label: '在编人数', width: 110, align: 'center' }
]

const formFields = computed(() => [
  { prop: 'name', label: '部门名称', required: true },
  { prop: 'parent', label: '上级部门', type: 'select', options: ['—', ...DEPT_NAMES.value], required: true },
  { prop: 'code', label: '部门编码', required: true },
  { prop: 'roleId', label: '对应角色', type: 'select', options: ROLE_OPTIONS.value, filterable: true },
  { prop: 'sortOrder', label: '排序', type: 'number' },
  { prop: 'status', label: '状态', type: 'select', options: ['启用', '停用'], required: true }
])

const filters = computed(() => [{ prop: 'parent', label: '全部上级', options: ['—', ...DEPT_NAMES.value] }])

const createRow = () => ({ name: '', parent: '—', code: '', roleId: null, sortOrder: 0, status: '启用' })

function deleteGuard(row) {
  if (orgStore.depts.some((d) => d.parent === row.name)) return '请先删除或移走该部门的下级部门'
  const employees = orgStore.employees.filter((e) => e.dept === row.name)
  if (employees.length) return `该部门下还有 ${employees.length} 名员工，请先调整员工部门`
  return ''
}

// 根据「上级部门」组装组织架构树
const treeData = computed(() => {
  const nodes = new Map(orgStore.depts.map((d) => [d.id, { ...d, label: d.name, children: [] }]))
  const roots = []
  orgStore.depts.forEach((d) => {
    const node = nodes.get(d.id)
    const parentNode = nodes.get(d.parentId)
    if (parentNode) parentNode.children.push(node)
    else roots.push(node)
  })
  return roots
})

orgStore.loadAll()
</script>

<style scoped>
.org-card {
  padding: 16px 18px 18px;
  margin-bottom: 16px;
}

.org-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.org-title {
  font-size: 15px;
  font-weight: 600;
  color: #273449;
}

.org-sub {
  font-size: 12.5px;
  color: var(--text-sub);
}

.org-tree {
  --el-tree-node-hover-bg-color: #f6f9ff;
  background: transparent;
}

.org-tree :deep(.el-tree-node__content) {
  height: 34px;
  border-radius: 6px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  color: #33415a;
}

.tree-icon {
  color: #93a4bf;
  font-size: 15px;
}

.tree-label {
  font-weight: 500;
}

.tree-role {
  font-size: 11.5px;
  color: #3b6cff;
  background: #eef4ff;
  border-radius: 10px;
  padding: 1px 8px;
}

.tree-count {
  font-size: 11.5px;
  color: #8b98ab;
  background: #f2f6fd;
  border-radius: 10px;
  padding: 1px 8px;
}
</style>
