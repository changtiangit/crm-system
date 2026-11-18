<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title-line">
          <h2 class="page-title">{{ title }}</h2>
          <!-- 数据范围：让用户知道为什么只看到这些行，而不是以为数据丢了 -->
          <el-tooltip v-if="showScope" :content="scopeTip" placement="bottom">
            <span class="scope-chip" :class="'scope-' + scopeType">
              <el-icon><Lock /></el-icon>
              {{ scopeLabel }}
            </span>
          </el-tooltip>
          <!-- 只读岗位（如售后看销售订单）：明确标出来，别让一排灰按钮被误读成有权操作 -->
          <el-tooltip v-if="isReadonly" content="当前岗位对该模块只读：可查看与筛选，不能新增、修改或删除" placement="bottom">
            <span class="readonly-chip">
              <el-icon><View /></el-icon>
              只读
            </span>
          </el-tooltip>
        </div>
        <p v-if="desc" class="page-desc">{{ desc }}</p>
      </div>
      <div class="header-actions">
        <el-button v-if="formFields.length && canCreate" type="primary" :icon="Plus" @click="openCreate">
          {{ createText }}
        </el-button>
        <!-- 只读岗位不摆一个禁用的「新建」；能编辑但不能新增的岗位才给提示 -->
        <el-tooltip
          v-else-if="formFields.length && !isReadonly"
          content="当前账号没有新增权限"
          placement="bottom"
        >
          <span>
            <el-button type="primary" :icon="Plus" disabled>{{ createText }}</el-button>
          </span>
        </el-tooltip>
      </div>
    </div>

    <slot name="before" />

    <div class="page-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            :placeholder="searchPlaceholder"
            clearable
            :prefix-icon="Search"
            class="search-input"
          />
          <el-select
            v-for="f in filters"
            :key="f.prop"
            v-model="activeFilters[f.prop]"
            :placeholder="f.label"
            clearable
            class="filter-select"
          >
            <el-option v-for="o in f.options" :key="o" :label="o" :value="o" />
          </el-select>
          <el-button :icon="Refresh" @click="onRefresh">刷新</el-button>
        </div>
        <div class="toolbar-right">
          <!-- 勾选后批量操作：如线索批量分配 -->
          <slot name="selection-actions" :selected="selectedRows" :clear="clearSelection" />
          <span class="count-text">共 {{ filtered.length }} 条</span>
        </div>
      </div>

      <!-- 从别的模块跳进来时带的筛选条件，清掉就回到全部数据 -->
      <div v-if="presetChips.length" class="preset-bar">
        <span class="preset-tip">当前筛选</span>
        <el-tag
          v-for="c in presetChips"
          :key="c.key"
          closable
          round
          effect="plain"
          class="preset-tag"
          @close="clearPreset(c.key)"
        >
          {{ c.label }}：{{ c.value }}
        </el-tag>
        <el-button link type="primary" @click="clearAllPreset">查看全部</el-button>
      </div>

      <!-- 表格 -->
      <el-table
        ref="tableRef"
        :data="paged"
        style="width: 100%"
        row-key="id"
        :row-class-name="rowClassName"
        @selection-change="onSelectionChange"
        @row-click="onRowClick"
      >
        <!-- 批量操作（如线索分配）时打开勾选列 -->
        <el-table-column
          v-if="selectable"
          type="selection"
          width="46"
          :reserve-selection="false"
          :selectable="rowSelectable || (() => true)"
        />

        <!-- 列多的宽表可以关掉序号，把宽度让给业务字段 -->
        <el-table-column
          v-if="showIndex"
          type="index"
          label="#"
          width="56"
          align="center"
          :index="indexMethod"
        />

        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth || 120"
          :align="col.align || 'left'"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <!-- 可点击的业务主键：点客户名进客户 360，点单号进对应单据 -->
            <span v-if="col.type === 'link'" class="cell-link" @click="col.onClick && col.onClick(row)">
              {{ cellValue(col, row) }}
            </span>
            <el-tag
              v-else-if="col.type === 'tag'"
              :type="tagTypeOf(col, cellValue(col, row), row)"
              effect="light"
              round
              size="small"
              :class="{ 'tag-link': !!col.onClick }"
              @click="col.onClick && col.onClick(row)"
            >
              {{ cellValue(col, row) }}
            </el-tag>
            <div v-else-if="col.type === 'tags'" class="tag-list">
              <el-tag
                v-for="t in asArray(cellValue(col, row))"
                :key="t"
                size="small"
                effect="plain"
                round
              >
                {{ t }}
              </el-tag>
              <span v-if="!asArray(cellValue(col, row)).length" class="muted">—</span>
            </div>
            <div v-else-if="col.type === 'stack'" class="stack-cell">
              <span
                class="stack-main"
                :class="{ 'cell-link': !!col.onClick }"
                @click="col.onClick && col.onClick(row)"
              >{{ cellValue(col, row)?.main || '—' }}</span>
              <span v-if="cellValue(col, row)?.sub" class="stack-sub">
                {{ cellValue(col, row).sub }}
                <!-- 主文本可点但不好认出来时，用小字提示能点进哪里（如订单号的「清单」） -->
                <span
                  v-if="cellValue(col, row).hint"
                  class="stack-hint"
                  @click="col.onClick && col.onClick(row)"
                >{{ cellValue(col, row).hint }} ›</span>
              </span>
            </div>
            <span v-else-if="col.type === 'money'" class="money">{{ formatMoney(cellValue(col, row)) }}</span>
            <span v-else-if="col.type === 'strong'" class="strong-cell">{{ cellValue(col, row) }}</span>
            <span v-else>{{ cellValue(col, row) }}</span>
          </template>
        </el-table-column>

        <el-table-column
          v-if="showOpColumn"
          label="操作"
          :width="isReadonly ? 84 : opWidth"
          align="right"
          fixed="right"
          class-name="op-col"
        >
          <template #default="{ row }">
            <!-- 各业务页可以在这里插入自己的行内操作（如库存调整、上下架、领取公海客户） -->
            <slot name="row-actions" :row="row" />

            <!-- 只读岗位：没有能做的动作，就不摆一排灰按钮；有详情页的给一个「查看」 -->
            <el-button
              v-if="isReadonly"
              v-show="!!rowClick"
              link
              type="primary"
              @click="viewRow(row)"
            >
              查看
            </el-button>

            <template v-else-if="!hideDefaultActions">
              <el-tooltip v-if="!canEdit" content="当前账号没有编辑权限" placement="top">
                <span><el-button link type="primary" disabled>编辑</el-button></span>
              </el-tooltip>
              <el-button v-else link type="primary" @click="openEdit(row)">编辑</el-button>

              <el-tooltip v-if="deleteBlockReason(row)" :content="deleteBlockReason(row)" placement="top">
                <span><el-button link type="danger" disabled>删除</el-button></span>
              </el-tooltip>
              <el-button v-else link type="danger" @click="onDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-box">
            <el-icon :size="28" color="#c3cddd"><Files /></el-icon>
            <p>暂无数据</p>
          </div>
        </template>
      </el-table>

      <!-- 分页 -->
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="filtered.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? `编辑${entityName}` : `新建${entityName}`"
      :width="dialogWidth"
      align-center
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formModel"
        :rules="formRules"
        :label-width="labelWidth"
        class="dialog-form"
      >
        <el-form-item v-for="f in formFields" :key="f.prop" :label="f.label" :prop="f.prop">
          <el-input
            v-if="!f.type || f.type === 'input'"
            v-model="formModel[f.prop]"
            :placeholder="f.placeholder || `请输入${f.label}`"
            :maxlength="f.maxlength"
            :disabled="fieldDisabled(f)"
            clearable
          />
          <el-input
            v-else-if="f.type === 'password'"
            v-model="formModel[f.prop]"
            type="password"
            show-password
            :placeholder="f.placeholder || `请输入${f.label}`"
          />
          <el-input
            v-else-if="f.type === 'textarea'"
            v-model="formModel[f.prop]"
            type="textarea"
            :rows="3"
            :placeholder="`请输入${f.label}`"
          />
          <el-input-number
            v-else-if="f.type === 'number'"
            v-model="formModel[f.prop]"
            :min="f.min ?? 0"
            :precision="f.precision"
            :controls="false"
            :disabled="fieldDisabled(f)"
            style="width: 100%"
            :placeholder="`请输入${f.label}`"
            @change="(v) => emitFieldChange(f.prop, v)"
          />
          <el-select
            v-else-if="f.type === 'select-multi'"
            v-model="formModel[f.prop]"
            multiple
            :placeholder="`请选择${f.label}`"
            style="width: 100%"
            @change="(v) => emitFieldChange(f.prop, v)"
          >
            <el-option v-for="o in f.options" :key="optValue(o)" :label="optLabel(o)" :value="optValue(o)" />
          </el-select>
          <el-select
            v-else-if="f.type === 'select'"
            v-model="formModel[f.prop]"
            :placeholder="`请选择${f.label}`"
            :filterable="!!f.filterable"
            :allow-create="!!f.allowCreate"
            :default-first-option="!!f.allowCreate"
            :disabled="fieldDisabled(f)"
            style="width: 100%"
            @change="(v) => emitFieldChange(f.prop, v)"
          >
            <el-option v-for="o in f.options" :key="optValue(o)" :label="optLabel(o)" :value="optValue(o)" />
          </el-select>
          <el-date-picker
            v-else-if="f.type === 'date'"
            v-model="formModel[f.prop]"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="`请选择${f.label}`"
            :disabled="fieldDisabled(f)"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 业务页可以往表单里塞自定义区域（如订单的商品清单），共享同一个 formModel -->
        <slot name="form-extra" :model="formModel" :editing="!!editingId" />
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, useSlots } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Files, Lock, View } from '@element-plus/icons-vue'
import { useDataScope } from '@/composables/useDataScope'

const slots = useSlots()

const { label: scopeLabel, type: scopeType } = useDataScope()

const scopeTip = computed(() =>
  scopeType.value === 'ALL'
    ? '数据范围：全公司 · 可查看全部客户、线索与订单'
    : scopeType.value === 'GROUP'
      ? '数据范围：本组 · 可查看本人与直属组员负责的数据，待分配线索可拉进本组'
      : '数据范围：仅本人 · 只显示自己负责的客户、线索、订单和跟进记录'
)

const props = defineProps({
  title: { type: String, required: true },
  desc: { type: String, default: '' },
  entityName: { type: String, default: '记录' },
  createText: { type: String, default: '新建' },
  columns: { type: Array, default: () => [] },
  formFields: { type: Array, default: () => [] },
  /** 是否显示「数据范围」标签：只有按负责人隔离的客户类数据才需要 */
  showScope: { type: Boolean, default: false },
  rows: { type: Array, required: true },
  searchKeys: { type: Array, default: () => [] },
  searchPlaceholder: { type: String, default: '搜索关键字' },
  filters: { type: Array, default: () => [] },
  createRow: { type: Function, default: () => ({}) },
  saveRow: { type: Function, default: null },
  removeRow: { type: Function, default: null },
  refreshRows: { type: Function, default: null },
  // 操作权限
  canCreate: { type: Boolean, default: true },
  canEdit: { type: Boolean, default: true },
  canDelete: { type: Boolean, default: true },
  // 操作列宽度：业务页按钮多时可加宽，避免换行
  opWidth: { type: Number, default: 150 },
  // 没有表单字段但需要行内操作时（如公海客户只能「领取」），用这个开关强制显示操作列
  showActions: { type: Boolean, default: false },
  // 隐藏内置的「编辑 / 删除」，只保留业务页自己的行内操作
  hideDefaultActions: { type: Boolean, default: false },
  // 表单标签宽度：标签字数多（如「对接联系人」）时调大，避免折行
  labelWidth: { type: String, default: '92px' },
  // 弹窗宽度：表单里还要放明细表（如订单的商品清单）时调宽
  dialogWidth: { type: String, default: '560px' },
  // 是否显示序号列：列数多的表建议关掉，把宽度让给客户名称等业务字段
  showIndex: { type: Boolean, default: true },
  // 返回不可删除的原因（返回空字符串表示可删除）
  deleteGuard: { type: Function, default: null },
  // 允许从地址栏带入的筛选字段，如 ['customer']；跳转方拼 ?customer=xxx 即可筛到该客户
  queryFilterKeys: { type: Array, default: () => [] },
  // 上述字段的中文名，用于顶部条件标签
  filterLabels: { type: Object, default: () => ({}) },

  /**
   * 条件标签上「值」的中文显示。
   * 例：{ stock: { low: '不足 10' } } → 标签显示「库存：不足 10」而不是「库存：low」
   */
  filterValueLabels: { type: Object, default: () => ({}) },
  // 特殊匹配规则：如 month 用「下单日期以 2026-11 开头」来筛，而不是全等
  queryMatchers: { type: Object, default: () => ({}) },
  // 是否显示勾选列（线索批量分配等场景）
  selectable: { type: Boolean, default: false },
  // 某一行是否允许勾选，默认全部可勾
  rowSelectable: { type: Function, default: null },

  /**
   * 整行点击：列表页给出行级入口（点客户行进 360、点订单行进合同清单）。
   * 行内的按钮、链接、勾选框会自己处理点击，不会重复触发。
   */
  rowClick: { type: Function, default: null }
})

// 表单字段变更事件：业务页可据此做联动（例：选了部门就自动带出该部门的角色）
const emit = defineEmits(['field-change', 'selection-change'])

/**
 * 只读岗位：三个写权限都没有（如售后看销售订单、财务看商品）。
 * 这类角色进页面只应该「看」，所以顶部打「只读」标、不摆新增/编辑/删除，
 * 有详情页的给一个「查看」入口。
 */
const isReadonly = computed(() => !props.canCreate && !props.canEdit && !props.canDelete)

/** 业务页是否自带行内操作（公海领取、库存调整等） */
const hasOwnActions = computed(() => !!slots['row-actions'])

/** 操作列是否出：只读岗位只有能进详情时才出「查看」，否则整列不占宽度 */
const showOpColumn = computed(() => {
  if (hasOwnActions.value) return true
  if (isReadonly.value) return !!props.rowClick
  return props.formFields.length > 0 || props.showActions
})

function viewRow(row) {
  props.rowClick?.(row)
}

// ---------- 勾选 ----------
const tableRef = ref(null)
const selectedRows = ref([])

function onSelectionChange(rows) {
  selectedRows.value = rows || []
  emit('selection-change', selectedRows.value)
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

defineExpose({ clearSelection })

function emitFieldChange(prop, value) {
  emit('field-change', prop, value, formModel.value)
}

// ---------- 搜索 / 筛选 ----------
const route = useRoute()
const router = useRouter()
const keyword = ref('')
const activeFilters = reactive({})
props.filters.forEach((f) => {
  activeFilters[f.prop] = ''
})

/**
 * 跨模块跳转带的固定条件（精确匹配，不参与「筛选下拉」）。
 * 例：从客户 360 点「销售订单」→ /sales/order?customer=杭州车匠汽车维修连锁
 */
const preset = reactive({})

function syncPreset() {
  Object.keys(preset).forEach((k) => delete preset[k])
  props.queryFilterKeys.forEach((k) => {
    const v = route.query[k]
    if (v !== undefined && v !== null && String(v) !== '') preset[k] = String(v)
  })
  // 搜索关键词也支持从地址栏带入：/sales/product?q=机油滤芯
  if (route.query.q) keyword.value = String(route.query.q)
}

watch(() => route.query, syncPreset, { immediate: true, deep: true })

// 只在 queryFilterKeys 里声明过的字段才展示为条件标签，避免把分页之类的参数也标出来
const presetChips = computed(() =>
  Object.entries(preset).map(([key, value]) => ({
    key,
    value: props.filterValueLabels[key]?.[value] ?? value,
    label: props.filterLabels[key] || key
  }))
)

/** 清掉单个条件：改地址栏而不是只改内存，刷新后不会又冒出来 */
function clearPreset(key) {
  const query = { ...route.query }
  delete query[key]
  router.replace({ path: route.path, query })
}

function clearAllPreset() {
  const query = { ...route.query }
  props.queryFilterKeys.forEach((k) => delete query[k])
  router.replace({ path: route.path, query })
}

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return props.rows.filter((row) => {
    if (kw && props.searchKeys.length) {
      const hit = props.searchKeys.some((k) => String(row[k] ?? '').toLowerCase().includes(kw))
      if (!hit) return false
    }
    for (const key of Object.keys(activeFilters)) {
      const v = activeFilters[key]
      if (v && row[key] !== v) return false
    }
    for (const key of Object.keys(preset)) {
      const match = props.queryMatchers[key]
      if (match) {
        if (!match(row, preset[key])) return false
      } else if (String(row[key] ?? '') !== preset[key]) {
        return false
      }
    }
    return true
  })
})

// ---------- 分页 ----------
const page = ref(1)
const pageSize = ref(10)
const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})
const indexMethod = (i) => (page.value - 1) * pageSize.value + i + 1

watch(keyword, () => (page.value = 1))
watch(activeFilters, () => (page.value = 1), { deep: true })
// 顶部统计 / 其他模块带过来的条件变化时回到第一页，否则可能停在一个空页上
watch(preset, () => (page.value = 1), { deep: true })
watch(pageSize, () => (page.value = 1))

function onRefresh() {
  keyword.value = ''
  Object.keys(activeFilters).forEach((k) => (activeFilters[k] = ''))
  if (props.refreshRows) props.refreshRows().then(() => ElMessage.success('已刷新')).catch(() => {})
  else ElMessage.success('已刷新')
}

// ---------- 新增后定位到该行 ----------
const flashId = ref(null)
let flashTimer = null

function rowClassName({ row }) {
  const flash = flashId.value !== null && String(row.id) === String(flashId.value) ? 'row-flash' : ''
  return [flash, props.rowClick ? 'row-pointer' : ''].filter(Boolean).join(' ')
}

/** 整行可点：行内按钮/链接/勾选框优先，避免点「编辑」也跳详情 */
function onRowClick(row, column, event) {
  if (!props.rowClick) return
  const el = event?.target
  if (el && el.closest && el.closest('button, a, .el-button, .cell-link, .el-checkbox, .el-tag, .el-switch')) return
  props.rowClick(row)
}

/** 新增的记录可能不在第一页（按编码排序常在末页），这里跳页并短暂高亮 */
async function focusRow(id) {
  if (id === undefined || id === null) return
  keyword.value = ''
  Object.keys(activeFilters).forEach((k) => (activeFilters[k] = ''))
  await nextTick()
  const idx = filtered.value.findIndex((r) => String(r.id) === String(id))
  if (idx < 0) return
  page.value = Math.floor(idx / pageSize.value) + 1
  flashId.value = id
  if (flashTimer) clearTimeout(flashTimer)
  flashTimer = setTimeout(() => (flashId.value = null), 2600)
}

// ---------- 展示格式化 ----------
const moneyFormatter = new Intl.NumberFormat('zh-CN')
function formatMoney(v) {
  if (v === undefined || v === null || v === '') return '—'
  return '¥ ' + moneyFormatter.format(Number(v))
}

function cellValue(col, row) {
  return col.formatter ? col.formatter(row) : row[col.prop]
}

function asArray(v) {
  return Array.isArray(v) ? v : v ? [v] : []
}

function optLabel(o) {
  return typeof o === 'object' ? o.label : o
}

function optValue(o) {
  return typeof o === 'object' ? o.value : o
}

/**
 * 字段是否禁用。支持布尔或 (model) => boolean：
 * 订单金额这类字段，有商品明细时由明细汇总，就不让手填了。
 */
function fieldDisabled(f) {
  return typeof f.disabled === 'function' ? !!f.disabled(formModel.value) : !!f.disabled
}

function tagTypeOf(col, value, row) {
  if (col.tagType) {
    if (typeof col.tagType === 'function') return col.tagType(value, row)
    return col.tagType[value] || 'info'
  }
  return 'primary'
}

// ---------- 新增 / 编辑 ----------
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const saving = ref(false)
const formModel = ref({})

const formRules = computed(() => {
  const rules = {}
  props.formFields.forEach((f) => {
    const list = []
    if (f.required) {
      const isSelect = ['select', 'select-multi', 'date'].includes(f.type)
      const rule = {
        required: true,
        message: `请${isSelect ? '选择' : '填写'}${f.label}`,
        trigger: isSelect ? 'change' : 'blur'
      }
      if (f.type === 'select-multi') rule.type = 'array'
      list.push(rule)
    }
    if (f.pattern) {
      list.push({ pattern: f.pattern, message: f.message || `请输入正确的${f.label}`, trigger: 'blur' })
    }
    if (list.length) rules[f.prop] = list
  })
  return rules
})

/** 返回不可删除的原因，空字符串表示允许删除 */
function deleteBlockReason(row) {
  if (!props.canDelete) return '当前账号没有删除权限'
  if (typeof props.deleteGuard === 'function') return props.deleteGuard(row) || ''
  return ''
}

function nextId() {
  return props.rows.reduce((max, r) => Math.max(max, Number(r.id) || 0), 0) + 1
}

function openCreate() {
  editingId.value = null
  formModel.value = { ...props.createRow() }
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  // 浅拷贝不够：数组里的对象（如订单商品明细）也要复制，否则弹窗里一改，表格数据就跟着变了
  const copy = { ...row }
  Object.keys(row).forEach((k) => {
    if (Array.isArray(row[k])) {
      copy[k] = row[k].map((v) => (v && typeof v === 'object' ? { ...v } : v))
    }
  })
  formModel.value = copy
  dialogVisible.value = true
}

/**
 * 从详情页点「编辑」进来时地址带 ?edit=id：
 * 等列表数据加载完，自动打开这一行的编辑弹窗，再把参数清掉（刷新不会再弹）。
 */
const autoEditOpened = ref(false)
watch(
  () => [props.rows.length, route.query.edit],
  () => {
    const id = route.query.edit
    if (!id || autoEditOpened.value) return
    const hit = props.rows.find((r) => String(r.id) === String(id))
    if (!hit) return
    autoEditOpened.value = true
    openEdit(hit)
    const query = { ...route.query }
    delete query.edit
    router.replace({ path: route.path, query })
  },
  { immediate: true }
)

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const isCreate = !editingId.value
    if (props.saveRow) {
      const saved = await props.saveRow(editingId.value, { ...formModel.value })
      ElMessage.success(isCreate ? '新增成功' : '修改成功')
      dialogVisible.value = false
      if (isCreate) await focusRow(saved?.id)
      return
    }
    if (editingId.value) {
      const idx = props.rows.findIndex((r) => r.id === editingId.value)
      if (idx > -1) Object.assign(props.rows[idx], formModel.value)
      ElMessage.success('修改成功')
    } else {
      const created = { id: nextId(), ...formModel.value }
      props.rows.unshift(created)
      ElMessage.success('新增成功')
      await focusRow(created.id)
    }
    dialogVisible.value = false
  } catch {
    // 具体错误信息已由请求拦截器提示，这里保持弹窗打开便于修改
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  const reason = deleteBlockReason(row)
  if (reason) {
    ElMessage.warning(reason)
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除该${props.entityName}吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  const idx = props.rows.findIndex((r) => r.id === row.id)
  try {
    if (props.removeRow) await props.removeRow(row.id)
    else if (idx > -1) props.rows.splice(idx, 1)
    ElMessage.success('删除成功')
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 10px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-input {
  width: 260px;
}

.filter-select {
  width: 132px;
}

.count-text {
  font-size: 13px;
  color: var(--text-sub);
}

/* 跨模块带来的筛选条件 */
.preset-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 18px;
  background: #f5f8ff;
  border-bottom: 1px solid var(--border);
}

.preset-tip {
  font-size: 12px;
  color: #7d8ca3;
}

.preset-tag {
  --el-tag-border-color: #cfdcff;
  color: #2f5fe0;
  background: #fff;
}

/* 表格里可点击的业务主键 */
.cell-link {
  color: #2f5fe0;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 1px dashed transparent;
  transition: border-color 0.15s ease;
}

.cell-link:hover {
  border-bottom-color: #2f5fe0;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-list :deep(.el-tag) {
  border-color: #dfe6f2;
  color: #52627a;
  background: #f8fafd;
}

.muted {
  color: #b3bdcc;
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

.money {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  color: #273449;
}

/* 新增成功后短暂高亮该行，方便在长列表里找到 */
:deep(.el-table__row.row-pointer) {
  cursor: pointer;
}

:deep(.el-table__row.row-flash > td) {
  animation: row-flash 2.6s ease-out;
}

@keyframes row-flash {
  0%,
  60% {
    background-color: #eaf2ff;
  }
  100% {
    background-color: transparent;
  }
}

/* 可点击的标签：让技术资料这类只读字段少占一个操作按钮 */
.tag-link {
  cursor: pointer;
}

.tag-link:hover {
  filter: brightness(0.96);
  text-decoration: underline;
}

/* 两行单元格：上面主信息、下面小字补充（如联系人 + 电话） */
.stack-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
  /* 单元格是 overflow:hidden，留 2px 余量，避免小字被行边框切掉半个字 */
  padding-bottom: 2px;
}

.stack-main {
  font-weight: 500;
  color: #273449;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 可点的两行单元格（如订单号）：主文本要显成链接色，否则用户不知道能点 */
.stack-main.cell-link {
  color: #2f5fe0;
}

.stack-sub {
  font-size: 11.5px;
  color: #8b98ab;
  font-variant-numeric: tabular-nums;
}

/* 小字提示：告诉用户主文本点了会去哪 */
.stack-hint {
  margin-left: 4px;
  color: #2f5fe0;
  cursor: pointer;
}

.stack-hint:hover {
  text-decoration: underline;
}

.strong-cell {
  font-weight: 500;
  color: #273449;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 18px;
}

.empty-box {
  padding: 26px 0;
  color: #97a3b6;
  font-size: 13px;
}
.empty-box p {
  margin: 8px 0 0;
}

.dialog-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.dialog-form :deep(.el-input-number .el-input__inner) {
  text-align: left;
}

/* 数据范围标签：一眼看清当前账号能看到谁的数据 */
.page-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.scope-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 11.5px;
  line-height: 1;
  white-space: nowrap;
  border: 1px solid transparent;
  cursor: default;
}

.scope-chip .el-icon {
  font-size: 12px;
}

.scope-ALL {
  color: #52627a;
  background: #f3f6fb;
  border-color: #e2e8f2;
}

.scope-GROUP {
  color: #2f5fe0;
  background: #eef3ff;
  border-color: #d9e4ff;
}

.scope-SELF {
  color: #a4620a;
  background: #fef6e8;
  border-color: #f6e2bd;
}

/* 只读岗位标记：灰蓝底，与「数据范围」的彩色标签区分开 */
.readonly-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 11.5px;
  line-height: 1;
  white-space: nowrap;
  color: #52627a;
  background: #eef1f7;
  border: 1px solid #dde3ee;
  cursor: default;
}

.readonly-chip .el-icon {
  font-size: 12px;
}
</style>
