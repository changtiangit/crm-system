<template>
  <aside class="sidebar" :class="{ 'is-collapsed': collapsed }">
    <div class="logo-area">
      <div class="logo-mark">C</div>
      <transition name="fade">
        <div v-show="!collapsed" class="logo-text">
          <b>CRM 系统</b>
          <span>客户关系管理</span>
        </div>
      </transition>
    </div>

    <div class="menu-scroll">
      <el-menu
        class="sidebar-menu"
        :default-active="activePath"
        :collapse="collapsed"
        :collapse-transition="false"
        unique-opened
        router
      >
        <template v-for="item in menus" :key="item.path">
          <!-- 有子菜单：一级 + 二级 -->
          <el-sub-menu v-if="item.children?.length" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>

          <!-- 无子菜单：一级直达 -->
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </div>

    <div v-show="!collapsed" class="sidebar-footer">
      <div class="version">版本 v0.1.0</div>
      <div class="copyright">© 2026 CRM</div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { menuRoutes } from '@/router'
import { usePermission } from '@/composables/usePermission'

defineProps({
  collapsed: { type: Boolean, default: false }
})

const route = useRoute()
const { has } = usePermission()

// 详情页（如订单详情）菜单里没有独立入口，用 meta.activeMenu 指回列表，保证父菜单展开且高亮
const activePath = computed(() => route.meta?.activeMenu || route.path)

// 按当前账号的权限过滤菜单：无权限的一级菜单与二级菜单都不展示
const menus = computed(() => {
  const list = []
  for (const r of menuRoutes) {
    if (r.meta?.hidden) continue
    const children = (r.children || [])
      .filter((c) => !c.meta?.hidden && has(c.meta?.permission))
      .map((c) => ({ title: c.meta?.title, path: `/${r.path}/${c.path}` }))

    if (r.children?.length) {
      if (children.length) {
        list.push({ title: r.meta?.title, icon: r.meta?.icon, path: `/${r.path}`, children })
      }
    } else if (has(r.meta?.permission)) {
      list.push({ title: r.meta?.title, icon: r.meta?.icon, path: `/${r.path}` })
    }
  }
  return list
})
</script>

<style scoped>
.sidebar {
  width: 226px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--sidebar-bg);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  transition: width 0.2s ease;
  overflow: hidden;
}

.sidebar.is-collapsed {
  width: 64px;
}

/* Logo */
.logo-area {
  height: 62px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo-mark {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 16px;
  color: #fff;
  background: linear-gradient(135deg, #5b8cff, #2f5fe0);
  box-shadow: 0 4px 12px rgba(59, 108, 255, 0.4);
}

.logo-text {
  display: flex;
  flex-direction: column;
  white-space: nowrap;
  overflow: hidden;
}
.logo-text b {
  color: #fff;
  font-size: 15px;
  letter-spacing: 0.5px;
}
.logo-text span {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.42);
  margin-top: 2px;
  letter-spacing: 0.5px;
}

/* 菜单 */
.menu-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px 0 16px;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #a8b3c5;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-active-color: #ffffff;
  --el-menu-item-height: 44px;
  --el-menu-sub-item-height: 42px;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 226px;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  margin: 4px 10px;
  border-radius: 8px;
  font-size: 14px;
  transition: background 0.15s ease, color 0.15s ease;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  color: #fff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: linear-gradient(90deg, #3b6cff, #5a86ff);
  box-shadow: 0 6px 16px rgba(59, 108, 255, 0.32);
  font-weight: 500;
}

.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff;
}

.sidebar-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  padding-left: 46px !important;
  font-size: 13.5px;
  min-width: auto;
  color: #94a1b5;
}

/* 二级菜单的选中态需要更高优先级，避免被上面的子项颜色覆盖 */
.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  color: #fff;
  font-weight: 500;
}

.sidebar-menu :deep(.el-icon) {
  font-size: 17px;
}

.sidebar-menu :deep(.el-sub-menu__icon-arrow) {
  font-size: 12px;
  right: 14px;
}

/* 折叠态 */
.sidebar.is-collapsed .logo-area {
  justify-content: center;
  padding: 0;
}
.sidebar.is-collapsed .sidebar-menu :deep(.el-menu-item),
.sidebar.is-collapsed .sidebar-menu :deep(.el-sub-menu__title) {
  margin: 4px 8px;
  padding: 0 !important;
  display: flex;
  justify-content: center;
}
.sidebar.is-collapsed .sidebar-menu :deep(.el-icon) {
  margin-right: 0;
}

/* 底部 */
.sidebar-footer {
  flex-shrink: 0;
  padding: 14px 16px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  display: flex;
  justify-content: space-between;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
