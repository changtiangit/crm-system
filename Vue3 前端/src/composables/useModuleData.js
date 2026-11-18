import request from '@/utils/request'

export async function loadModuleRows(module, seedRows) {
  const data = await request.get(`/modules/${module}`)
  if (data.length === 0) await request.post(`/modules/${module}/seed`, seedRows)
  return request.get(`/modules/${module}`)
}

/**
 * 将业务演示数据首次导入数据库，之后统一由模块 API 读写。
 *
 * decorate：接口返回的是「原始业务字段」，页面上的未收金额、账龄、状态这些
 * 都是算出来的派生字段。从数据库读回来的行必须重新算一遍，否则列表会出现
 * 「未收金额 —」「账龄空白」这种问题（应收页面踩过）。
 */
export function useModuleData(module, rows, seedRows, decorate = (row) => row) {
  const apply = (list) => (list || []).map((row) => decorate({ ...row }))

  async function refreshRows() {
    const data = await request.get(`/modules/${module}`)
    rows.splice(0, rows.length, ...apply(data))
  }

  async function initialize() {
    rows.splice(0, rows.length, ...apply(await loadModuleRows(module, seedRows)))
  }

  async function saveRow(id, row) {
    const saved = id
      ? await request.put(`/modules/${module}/${id}`, row)
      : await request.post(`/modules/${module}`, row)
    const next = decorate({ ...saved })
    const index = rows.findIndex((item) => item.id === next.id)
    if (index < 0) rows.unshift(next)
    else Object.assign(rows[index], next)
    return next
  }

  async function removeRow(id) {
    await request.delete(`/modules/${module}/${id}`)
    const index = rows.findIndex((item) => item.id === id)
    if (index >= 0) rows.splice(index, 1)
  }

  return { initialize, refreshRows, saveRow, removeRow }
}
