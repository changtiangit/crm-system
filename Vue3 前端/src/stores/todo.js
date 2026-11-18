import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { getRoleProfile, seedTodosOf } from '@/config/roleProfile'
import request from '@/utils/request'

/**
 * 我的待办：存在 MySQL 的 crm_module_records 里，但**永远只属于本人**。
 * - 后端按 owner=当前登录人过滤，数据范围是「全公司」的账号也不会看到别人的待办
 * - 首次进入时按岗位画像播种（营销=线索分配，财务=回款催收，售后=工单受理…）
 */

export const useTodoStore = defineStore('todo', () => {
  const userStore = useUserStore()
  const items = ref([])
  const loadedFor = ref(null)

  const currentKey = () => userStore.userInfo?.empNo || null

  async function load() {
    const key = currentKey()
    if (loadedFor.value === key) return
    let rows = await request.get('/modules/todos')
    if (rows.length === 0 && userStore.userInfo) {
      // 这个岗位该干什么，由 roleProfile 决定；owner 由后端强制写成当前登录人
      const profile = getRoleProfile(userStore.userInfo)
      const seeds = seedTodosOf(profile, userStore.userInfo.name || '')
      rows = await request.post('/modules/todos/seed', seeds)
    }
    items.value = rows
    loadedFor.value = key
  }

  // 切换账号后要换成新账号的待办，不能把上一个账号的待办留在页面上
  watch(
    () => userStore.userInfo?.empNo,
    (empNo) => {
      loadedFor.value = null
      if (!empNo) {
        items.value = []
        return
      }
      load()
    }
  )

  const pending = computed(() => items.value.filter((t) => !t.done))
  const done = computed(() => items.value.filter((t) => t.done))
  const todayStr = () => new Date().toISOString().slice(0, 10)
  const overdue = computed(() => pending.value.filter((t) => t.due && t.due < todayStr()))

  async function add(todo) {
    const row = await request.post('/modules/todos', {
      done: false,
      owner: userStore.userInfo?.name || '',
      ...todo
    })
    items.value.unshift(row)
    return row
  }

  async function remove(id) {
    await request.delete(`/modules/todos/${id}`)
    const idx = items.value.findIndex((t) => t.id === id)
    if (idx > -1) items.value.splice(idx, 1)
  }

  async function toggle(id, done) {
    const row = items.value.find((t) => t.id === id)
    if (row) {
      const updated = await request.put(`/modules/todos/${id}`, {
        ...row,
        done: done === undefined ? !row.done : !!done
      })
      Object.assign(row, updated)
    }
  }

  return { items, pending, done, overdue, load, add, remove, toggle }
})
