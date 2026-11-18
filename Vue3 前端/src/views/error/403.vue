<template>
  <div class="forbidden">
    <div class="code">403</div>
    <h2>没有访问权限</h2>
    <p>
      当前账号
      <b v-if="userStore.userInfo?.name">「{{ userStore.userInfo.name }}」</b>
      的角色（{{ roleNames.join('、') || '未分配角色' }}）无权访问该页面，请联系管理员分配权限。
    </p>
    <div class="actions">
      <el-button v-if="fallbackPath" type="primary" @click="$router.replace(fallbackPath)">
        返回可访问页面
      </el-button>
      <el-button @click="onLogout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { menuRoutes } from '@/router'
import { useUserStore } from '@/stores/user'
import { usePermission } from '@/composables/usePermission'

const router = useRouter()
const userStore = useUserStore()
const { has, roleNames } = usePermission()

// 找到当前账号第一个可访问的菜单，避免再次回到 403
const fallbackPath = computed(() => {
  const candidates = []
  menuRoutes.forEach((r) => {
    if (r.children?.length) {
      r.children.forEach((c) => candidates.push([`/${r.path}/${c.path}`, c.meta?.permission]))
    } else {
      candidates.push([`/${r.path}`, r.meta?.permission])
    }
  })
  const hit = candidates.find(([, perm]) => !perm || has(perm))
  return hit ? hit[0] : ''
})

async function onLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  userStore.logout()
  ElMessage.success('已安全退出')
  router.replace('/login')
}
</script>

<style scoped>
.forbidden {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #f4f6fa;
  padding: 24px;
  text-align: center;
}

.code {
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #f0b429, #d97706);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

h2 {
  margin: 8px 0 0;
  font-size: 20px;
  color: #273449;
}

p {
  margin: 0;
  font-size: 13.5px;
  color: #8493a8;
  max-width: 460px;
  line-height: 1.7;
}

p b {
  color: #52627a;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
</style>
