<template>
  <div class="stats-tabs">
    <div class="tabs">
      <button
        v-for="t in tabs"
        :key="t.path"
        class="tab"
        :class="{ active: t.key === active }"
        @click="t.key !== active && router.push(t.path)"
      >
        <el-icon><component :is="t.icon" /></el-icon>
        {{ t.label }}
      </button>
    </div>
    <span class="note">{{ note }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { TrendCharts, User, Goods, Promotion } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'

const props = defineProps({
  active: { type: String, required: true }
})

const router = useRouter()
const { has } = usePermission()

// 三张经营报表按订单口径，市场分析按线索口径；没有权限的页签不展示
const allTabs = [
  { key: 'sales', label: '销售统计', path: '/stats/sales', icon: TrendCharts, permission: 'stats:sales:view' },
  { key: 'customer', label: '客户采购分析', path: '/stats/customer', icon: User, permission: 'stats:customer:view' },
  { key: 'product', label: '产品销售分析', path: '/stats/product', icon: Goods, permission: 'stats:product:view' },
  { key: 'market', label: '市场分析', path: '/stats/market', icon: Promotion, permission: 'stats:market:view' }
]

const tabs = computed(() => allTabs.filter((t) => has(t.permission)))

const note = computed(() =>
  props.active === 'market'
    ? '口径：线索按创建时间归月；转化率 = 状态为「已转化」的线索 ÷ 当月新增线索'
    : '口径：已取消订单不计入业绩；金额为含税销售额'
)
</script>

<style scoped>
.stats-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.tabs {
  display: inline-flex;
  padding: 3px;
  gap: 2px;
  background: #eef2f9;
  border-radius: 10px;
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 13px;
  color: #5c6b82;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.tab:hover {
  color: #2f5fe0;
}

.tab.active {
  background: #fff;
  color: #1f2b3d;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(24, 42, 76, 0.1);
}

.tab .el-icon {
  font-size: 14px;
}

.note {
  font-size: 11.5px;
  color: #97a3b6;
}
</style>
