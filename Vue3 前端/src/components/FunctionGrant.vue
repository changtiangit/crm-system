<template>
  <div class="grant" :class="{ disabled }">
    <div v-for="group in orgStore.functionGroups" :key="group.module" class="group">
      <div class="group-head">
        <div class="group-title">
          <span class="group-name">{{ group.module }}</span>
          <span class="group-count">
            <b>{{ checkedCount(group) }}</b> / {{ group.items.length }}
          </span>
        </div>
        <el-checkbox
          :model-value="isAllChecked(group)"
          :indeterminate="isIndeterminate(group)"
          :disabled="disabled"
          @change="(val) => toggleGroup(group, val)"
        >
          全选
        </el-checkbox>
      </div>

      <div class="group-body">
        <el-checkbox
          v-for="item in group.items"
          :key="item.code"
          :model-value="modelValue.includes(item.code)"
          :disabled="disabled"
          @change="(val) => toggle(item.code, val)"
        >
          {{ item.name }}
        </el-checkbox>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useOrgStore } from '@/stores/org'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const orgStore = useOrgStore()

const checkedCount = (group) => group.items.filter((i) => props.modelValue.includes(i.code)).length
const isAllChecked = (group) => checkedCount(group) === group.items.length
const isIndeterminate = (group) => {
  const n = checkedCount(group)
  return n > 0 && n < group.items.length
}

function toggle(code, val) {
  const next = new Set(props.modelValue)
  val ? next.add(code) : next.delete(code)
  emit('update:modelValue', [...next])
}

function toggleGroup(group, val) {
  const next = new Set(props.modelValue)
  group.items.forEach((i) => (val ? next.add(i.code) : next.delete(i.code)))
  emit('update:modelValue', [...next])
}
</script>

<style scoped>
.grant {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.group {
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 14px;
  background: #f7f9fd;
  border-bottom: 1px solid var(--border);
}

.group-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.group-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #1f2b3d;
}

.group-count {
  font-size: 11.5px;
  color: #97a3b6;
  font-variant-numeric: tabular-nums;
}

.group-count b {
  color: #2f5fe0;
  font-weight: 600;
}

.group-body {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(126px, 1fr));
  gap: 2px 8px;
  padding: 10px 14px 12px;
}

.group-body :deep(.el-checkbox) {
  margin-right: 0;
  height: 30px;
}

.group-body :deep(.el-checkbox__label) {
  font-size: 13px;
  color: #33415a;
}

.group-head :deep(.el-checkbox__label) {
  font-size: 12.5px;
  color: #52627a;
}

.disabled .group-head {
  background: #fafbfd;
}
</style>
