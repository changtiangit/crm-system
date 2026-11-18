import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

/**
 * 组织数据仓库：功能点、角色、员工、部门。
 * 全部数据来自后端接口（MySQL），页面只读这里的镜像，写操作后自动重新拉取。
 */

const roleStatusText = (v) => (v === 0 ? '停用' : '启用')
const roleStatusCode = (v) => (v === '停用' ? 0 : 1)
const employeeStatusText = (v) => (v === 0 ? '停用' : '在职')
const employeeStatusCode = (v) => (v === '停用' ? 0 : 1)

export const useOrgStore = defineStore('org', () => {
  // ---------------- 功能点 ----------------
  const permissions = ref([])
  const functionsLoaded = ref(false)

  const allPermissionCodes = computed(() => permissions.value.map((p) => p.code))

  // 按模块分组，供授权界面渲染复选框
  const functionGroups = computed(() => {
    const groups = new Map()
    permissions.value.forEach((p) => {
      if (!groups.has(p.module)) groups.set(p.module, { module: p.module, items: [] })
      groups.get(p.module).items.push({ code: p.code, name: p.name })
    })
    return [...groups.values()]
  })

  function permissionName(code) {
    return permissions.value.find((p) => p.code === code)?.name || code
  }

  function rolePermissionNames(role) {
    if (!role) return []
    if (role.builtin) return permissions.value.map((p) => p.name)
    return (role.permissionIds || []).map(permissionName)
  }

  async function loadFunctions(force = false) {
    if (functionsLoaded.value && !force) return
    const rows = await request.get('/functions')
    permissions.value = (rows || []).map((f) => ({ module: f.module || '其他', name: f.name, code: f.code }))
    functionsLoaded.value = true
  }

  // ---------------- 角色 ----------------
  const roles = ref([])
  const rolesLoaded = ref(false)

  function normalizeRole(row) {
    return {
      id: row.id,
      code: row.code,
      name: row.name,
      desc: row.description || '',
      status: roleStatusText(row.status),
      parentId: row.parentId || null,
      builtin: !!row.builtin,
      permissionIds: row.permissionCodes || [],
      employeeCount: row.employeeCount || 0
    }
  }

  const roleTree = computed(() => {
    const nodes = new Map()
    roles.value.forEach((r) => nodes.set(r.id, { ...r, children: [] }))
    const roots = []
    roles.value.forEach((r) => {
      const node = nodes.get(r.id)
      const parent = r.parentId ? nodes.get(r.parentId) : null
      if (parent) parent.children.push(node)
      else roots.push(node)
    })
    return roots
  })

  const roleById = (id) => roles.value.find((r) => r.id === id)
  const roleName = (id) => roleById(id)?.name || '—'
  const employeeCountOfRole = (id) => roleById(id)?.employeeCount || 0

  function rolePermissionCount(role) {
    if (!role) return 0
    if (role.builtin) return allPermissionCodes.value.length
    return (role.permissionIds || []).filter((c) => allPermissionCodes.value.includes(c)).length
  }

  async function loadRoles() {
    const rows = await request.get('/roles')
    roles.value = (rows || []).map(normalizeRole)
    rolesLoaded.value = true
  }

  async function addRole({ name, code, parentId, desc, status }) {
    await request.post('/roles', {
      name,
      code: String(code).trim().toUpperCase(),
      parentId: parentId || null,
      description: desc || '',
      status: roleStatusCode(status)
    })
    await loadRoles()
  }

  async function updateRole(id, patch) {
    const current = roleById(id) || {}
    await request.put(`/roles/${id}`, {
      name: patch.name ?? current.name,
      code: String(patch.code ?? current.code).trim().toUpperCase(),
      parentId: patch.parentId ?? current.parentId ?? null,
      description: patch.desc ?? current.desc ?? '',
      status: patch.status ? roleStatusCode(patch.status) : roleStatusCode(current.status)
    })
    await loadRoles()
  }

  async function removeRole(id) {
    await request.delete(`/roles/${id}`)
    await loadRoles()
  }

  async function setRolePermissions(id, codes) {
    await request.put(`/roles/${id}/permissions`, { codes: [...codes] })
    await loadRoles()
  }

  // ---------------- 员工 ----------------
  const employees = ref([])
  const employeesLoaded = ref(false)

  function normalizeEmployee(row) {
    return {
      id: row.id,
      empNo: row.empNo,
      name: row.name,
      position: row.position || '',
      dataScope: row.dataScope || 'ALL',
      deptId: row.deptId,
      dept: row.deptName || '—',
      roleIds: row.roleIds || [],
      roleNames: row.roleNames || [],
      phone: row.phone || '',
      email: row.email || '',
      status: employeeStatusText(row.status)
    }
  }

  async function loadEmployees() {
    const rows = await request.get('/employees')
    employees.value = (rows || []).map(normalizeEmployee)
    employeesLoaded.value = true
  }

  const employeeByEmpNo = (empNo) =>
    employees.value.find((e) => e.empNo?.toLowerCase() === String(empNo).trim().toLowerCase())

  function employeeRoles(emp) {
    return (emp?.roleIds || []).map((id) => roleById(id)).filter(Boolean)
  }

  function employeeRoleNames(emp) {
    if (emp?.roleNames?.length) return emp.roleNames
    return employeeRoles(emp).map((r) => r.name)
  }

  function isAdminRoleIds(roleIds = []) {
    return roleIds.some((id) => roleById(id)?.code === 'ADMIN')
  }

  async function saveEmployee(id, values) {
    const payload = {
      empNo: values.empNo,
      name: values.name,
      position: values.position || null,
      dataScope: values.dataScope || null,
      deptId: values.deptId,
      roleIds: values.roleIds || [],
      phone: values.phone || null,
      email: values.email || null,
      status: employeeStatusCode(values.status),
      password: values.password ? String(values.password) : null
    }
    if (id) await request.put(`/employees/${id}`, payload)
    else await request.post('/employees', payload)
    await Promise.all([loadEmployees(), loadRoles()])
  }

  async function removeEmployee(id) {
    await request.delete(`/employees/${id}`)
    await Promise.all([loadEmployees(), loadRoles()])
  }

  // ---------------- 部门 ----------------
  const depts = ref([])

  async function loadDepartments() {
    // 接口返回树形结构，这里拍平成表格用的列表
    const tree = (await request.get('/departments')) || []
    const flat = []
    const walk = (nodes, parentName = '—') => {
      ;(nodes || []).forEach((d) => {
        flat.push({
          id: d.id,
          name: d.name,
          parentId: d.parentId,
          parent: parentName,
          leader: d.leader || '',
          count: d.employeeCount ?? 0,
          code: d.code,
          sortOrder: d.sortOrder ?? 0,
          roleId: d.roleId || null,
          roleName: d.roleName || '',
          status: d.status === 0 ? '停用' : '启用'
        })
        if (d.children?.length) walk(d.children, d.name)
      })
    }
    walk(tree)
    depts.value = flat
  }

  async function saveDepartment(id, values) {
    const parent = depts.value.find((d) => d.name === values.parent)
    const payload = {
      parentId: values.parent === '—' ? null : parent?.id,
      code: values.code || `DEPT_${Date.now()}`,
      name: values.name,
      sortOrder: values.sortOrder || 0,
      status: values.status === '停用' ? 0 : 1,
      // 部门绑定的角色：注册该部门的新员工自动获得
      roleId: values.roleId || null
    }
    if (id) await request.put(`/departments/${id}`, payload)
    else await request.post('/departments', payload)
    await loadDepartments()
  }

  async function removeDepartment(id) {
    await request.delete(`/departments/${id}`)
    await loadDepartments()
  }

  // ---------------- 统一加载 ----------------
  const ready = ref(false)

  async function loadAll() {
    const tasks = [
      loadFunctions().catch(() => {}),
      loadRoles().catch(() => {}),
      loadEmployees().catch(() => {}),
      loadDepartments().catch(() => {})
    ]
    await Promise.all(tasks)
    ready.value = true
  }

  return {
    // 功能点
    permissions,
    allPermissionCodes,
    functionGroups,
    permissionName,
    rolePermissionNames,
    loadFunctions,
    // 角色
    roles,
    roleTree,
    roleById,
    roleName,
    rolePermissionCount,
    employeeCountOfRole,
    loadRoles,
    addRole,
    updateRole,
    removeRole,
    setRolePermissions,
    // 员工
    employees,
    employeeByEmpNo,
    employeeRoles,
    employeeRoleNames,
    isAdminRoleIds,
    loadEmployees,
    saveEmployee,
    removeEmployee,
    // 部门
    depts,
    loadDepartments,
    saveDepartment,
    removeDepartment,
    // 其它
    loadAll,
    ready
  }
})
