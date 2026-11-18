import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

/**
 * 商品数据仓库：数据来自 MySQL（/api/products）。
 * 列表、分类下拉、增删改都在这里统一处理，页面只读镜像。
 */

const statusText = (v) => (v === 0 ? '停用' : '启用')
const statusCode = (v) => (v === '停用' ? 0 : 1)

/** 库存状态：缺货 / 偏低 / 充足，用于表格里的彩色标签 */
function stockLevel(stock) {
  const n = Number(stock) || 0
  if (n <= 0) return '缺货'
  if (n < 10) return '偏低'
  return '充足'
}

export const useProductStore = defineStore('product', () => {
  const products = ref([])
  const categories = ref([])
  const loaded = ref(false)
  const loading = ref(false)

  const total = computed(() => products.value.length)
  const lowStockCount = computed(() => products.value.filter((p) => p.stock < 10).length)

  function normalize(row) {
    return {
      id: row.id,
      code: row.code,
      name: row.name,
      category: row.category,
      spec: row.spec || '',
      unit: row.unit || '个',
      purchasePrice: Number(row.purchasePrice ?? 0),
      salePrice: Number(row.salePrice ?? 0),
      stock: Number(row.stock ?? 0),
      stockLabel: stockLevel(row.stock),
      status: statusText(row.status),
      statusCode: row.status ?? 1,
      remark: row.remark || '',
      techNote: row.techNote || '',
      techLabel: row.techNote ? '已录入' : '待补充',
      createdAt: row.createdAt
    }
  }

  async function loadProducts(force = false) {
    if (loaded.value && !force) return products.value
    loading.value = true
    try {
      const rows = (await request.get('/products')) || []
      products.value = rows.map(normalize)
      loaded.value = true
      return products.value
    } finally {
      loading.value = false
    }
  }

  async function loadCategories(force = false) {
    if (categories.value.length && !force) return categories.value
    categories.value = (await request.get('/products/categories')) || []
    return categories.value
  }

  async function loadAll(force = false) {
    await Promise.all([loadProducts(force), loadCategories(force)])
  }

  function toPayload(values) {
    return {
      code: String(values.code || '').trim(),
      name: String(values.name || '').trim(),
      category: String(values.category || '').trim(),
      spec: values.spec || null,
      unit: values.unit || '个',
      purchasePrice: values.purchasePrice == null ? 0 : Number(values.purchasePrice),
      salePrice: values.salePrice == null ? 0 : Number(values.salePrice),
      stock: values.stock == null ? 0 : Number(values.stock),
      status: statusCode(values.status),
      remark: values.remark || null
    }
  }

  async function saveProduct(id, values) {
    const payload = toPayload(values)
    const saved = id ? await request.put(`/products/${id}`, payload) : await request.post('/products', payload)
    await Promise.all([loadProducts(true), loadCategories(true)])
    // 返回后端结果，页面据此定位到新增的那一行
    return saved
  }

  /** 库存增减：delta 正数入库、负数出库，后端会校验不能出成负库存 */
  async function adjustStock(id, delta) {
    const saved = await request.patch(`/products/${id}/stock`, { delta })
    await loadProducts(true)
    return saved
  }

  /** 上架 / 下架 */
  async function changeStatus(id, status) {
    const saved = await request.patch(`/products/${id}/status`, { status })
    await loadProducts(true)
    return saved
  }

  /** 技术 / 维修资料（售后维护） */
  async function saveTechNote(id, techNote) {
    const saved = await request.patch(`/products/${id}/tech`, { techNote })
    await loadProducts(true)
    return saved
  }

  async function removeProduct(id) {
    await request.delete(`/products/${id}`)
    await Promise.all([loadProducts(true), loadCategories(true)])
  }

  return {
    products,
    categories,
    loading,
    loaded,
    total,
    lowStockCount,
    loadProducts,
    loadCategories,
    loadAll,
    saveProduct,
    removeProduct,
    adjustStock,
    changeStatus,
    saveTechNote
  }
})
