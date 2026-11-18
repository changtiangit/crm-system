<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">角色管理</h2>
        <p class="page-desc">角色与部门一一对应：销售部下设销售一组 / 销售二组，售后负责技术支持与产品维修，生产负责商品新增、下架与库存增减；角色能用哪些功能，在「权限授权」里勾选</p>
      </div>
      <el-tooltip v-if="!has('system:role:add')" content="当前账号没有新增角色权限" placement="bottom">
        <span><el-button type="primary" :icon="Plus" disabled>新增角色</el-button></span>
      </el-tooltip>
      <el-button v-else type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
    </div>

    <!-- 概览 -->
    <div class="stat-row">
      <div class="stat-card page-card">
        <span class="stat-label">角色总数</span>
        <b class="stat-value">{{ orgStore.roles.length }}</b>
      </div>
      <div class="stat-card page-card">
        <span class="stat-label">启用中</span>
        <b class="stat-value">{{ enabledCount }}</b>
      </div>
      <div class="stat-card page-card">
        <span class="stat-label">关联员工</span>
        <b class="stat-value">{{ orgStore.employees.length }}</b>
      </div>
      <div class="stat-card page-card">
        <span class="stat-label">功能总数</span>
        <b class="stat-value">{{ orgStore.permissions.length }}</b>
      </div>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索角色名称 / 角色编号" clearable :prefix-icon="Search" class="search-input" />
        <span class="count-text">共 {{ filteredRoles.length }} 个角色</span>
      </div>

      <el-table
        :data="tableData"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        style="width: 100%"
      >
        <el-table-column prop="name" label="角色名称" min-width="190">
          <template #default="{ row }">
            <span class="role-name">
              {{ row.name }}
              <el-tag v-if="row.builtin" size="small" type="warning" effect="light" round>内置</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="角色编号" width="150" />
        <el-table-column prop="desc" label="角色说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="已授功能" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.builtin" class="perm-all">全部</span>
            <span v-else class="perm-count">
              {{ orgStore.rolePermissionCount(row) }} <em>/ {{ orgStore.permissions.length }}</em>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="员工数" width="90" align="center">
          <template #default="{ row }">{{ orgStore.employeeCountOfRole(row.id) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small" effect="light" round>
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="right" class-name="op-col">
          <template #default="{ row }">
            <el-tooltip v-if="row.builtin" content="管理员固定拥有全部功能" placement="top">
              <span><el-button link type="primary" disabled>授权</el-button></span>
            </el-tooltip>
            <el-tooltip v-else-if="!has('system:role:assign')" content="当前账号没有授权的操作权限" placement="top">
              <span><el-button link type="primary" disabled>授权</el-button></span>
            </el-tooltip>
            <el-button v-else link type="primary" @click="goGrant(row)">授权</el-button>

            <el-button v-if="row.builtin" link type="primary" disabled>编辑</el-button>
            <el-button v-else link type="primary" :disabled="!has('system:role:edit')" @click="openEdit(row)">
              编辑
            </el-button>

            <el-tooltip v-if="deleteBlockReason(row)" :content="deleteBlockReason(row)" placement="top">
              <span><el-button link type="danger" disabled>删除</el-button></span>
            </el-tooltip>
            <el-button v-else link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增 / 编辑角色 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑角色' : '新增角色'" width="560px" align-center destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="如：销售部、财务、营销、售后、生产" clearable />
        </el-form-item>
        <el-form-item label="角色编号" prop="code">
          <el-input v-model="form.code" placeholder="如：SALES、FINANCE" clearable />
        </el-form-item>
        <el-form-item label="上级角色">
          <el-select v-model="form.parentId" placeholder="无（一级角色）" style="width: 100%" clearable>
            <el-option v-for="o in parentOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色说明">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="描述该角色的职责范围" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['启用', '停用']" :key="s" :label="s" :value="s" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useOrgStore } from '@/stores/org'
import { usePermission } from '@/composables/usePermission'

const orgStore = useOrgStore()
const router = useRouter()
const { has } = usePermission()

const keyword = ref('')
const enabledCount = computed(() => orgStore.roles.filter((r) => r.status === '启用').length)

// ---------- 树形表格 + 搜索 ----------
function flatten(nodes, out = []) {
  nodes.forEach((n) => {
    out.push(n)
    if (n.children?.length) flatten(n.children, out)
  })
  return out
}

const filteredRoles = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const all = flatten(orgStore.roleTree)
  if (!kw) return all
  return all.filter(
    (r) => r.name.toLowerCase().includes(kw) || (r.code || '').toLowerCase().includes(kw)
  )
})

// 搜索时用扁平结果，避免层级折叠导致命中项被隐藏
const tableData = computed(() => {
  const kw = keyword.value.trim()
  if (kw) return flatten(orgStore.roleTree).map(({ children, ...rest }) => rest)
  return orgStore.roleTree
})

// ---------- 新增 / 编辑 ----------
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', code: '', parentId: 0, desc: '', status: '启用' })

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编号', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '建议使用大写字母、数字与下划线', trigger: 'blur' }
  ]
}

function descendantIds(id) {
  const set = new Set()
  const walk = (pid) =>
    orgStore.roles
      .filter((r) => r.parentId === pid)
      .forEach((r) => {
        set.add(r.id)
        walk(r.id)
      })
  walk(id)
  return set
}

const parentOptions = computed(() => {
  const exclude = new Set()
  if (editingId.value) {
    exclude.add(editingId.value)
    descendantIds(editingId.value).forEach((i) => exclude.add(i))
  }
  return [{ label: '无（一级角色）', value: 0 }, ...orgStore.roles.filter((r) => !exclude.has(r.id)).map((r) => ({ label: r.name, value: r.id }))]
})

function resetForm() {
  Object.assign(form, { name: '', code: '', parentId: 0, desc: '', status: '启用' })
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    code: row.code,
    parentId: row.parentId || 0,
    desc: row.desc || '',
    status: row.status || '启用'
  })
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = { ...form, parentId: form.parentId || null }
  try {
    if (editingId.value) {
      await orgStore.updateRole(editingId.value, payload)
      ElMessage.success('角色已更新')
    } else {
      await orgStore.addRole(payload)
      ElMessage.success('角色已创建，可点「授权」勾选可用功能')
    }
    dialogVisible.value = false
  } catch {
    // 错误提示由请求拦截器统一处理，弹窗保持打开
  }
}

function deleteBlockReason(row) {
  if (!has('system:role:delete')) return '当前账号没有删除角色的操作权限'
  if (row.builtin) return '内置角色不可删除'
  if (orgStore.roles.some((r) => r.parentId === row.id)) return '请先删除该角色的下级角色'
  const count = orgStore.employeeCountOfRole(row.id)
  if (count > 0) return `该角色下还有 ${count} 名员工，请先调整员工角色`
  return ''
}

async function onDelete(row) {
  const reason = deleteBlockReason(row)
  if (reason) {
    ElMessage.warning(reason)
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  try {
    await orgStore.removeRole(row.id)
    ElMessage.success('角色已删除')
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

// ---------- 授权：跳到功能授权页并定位当前角色 ----------
function goGrant(row) {
  router.push({ path: '/system/permission', query: { role: row.id } })
}

onMounted(() => {
  if (!orgStore.roles.length) orgStore.loadAll()
})
</script>

<style scoped>
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 12.5px;
  color: var(--text-sub);
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #1f2b3d;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid var(--border);
}

.search-input {
  width: 260px;
}

.count-text {
  font-size: 13px;
  color: var(--text-sub);
}

.role-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #273449;
}

.perm-count {
  font-weight: 500;
  color: #273449;
  font-variant-numeric: tabular-nums;
}
.perm-count em {
  font-style: normal;
  color: #a3afc2;
  font-size: 12px;
  margin-left: 2px;
}

.perm-all {
  color: #c8891a;
  font-weight: 600;
  font-size: 13px;
}

:deep(.op-col .cell) {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.op-col .el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 1100px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
