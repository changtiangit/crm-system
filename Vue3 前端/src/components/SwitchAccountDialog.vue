<template>
  <el-dialog
    :model-value="modelValue"
    width="540px"
    align-center
    destroy-on-close
    class="switch-dialog"
    @update:model-value="(v) => emit('update:modelValue', v)"
    @closed="reset"
  >
    <template #header>
      <div class="dlg-head">
        <h3>切换账号</h3>
        <p>同一浏览器最多保留 {{ MAX_SESSIONS }} 个账号，切换时无需再次输入密码</p>
      </div>
    </template>

    <div class="acct-list">
      <div
        v-for="s in userStore.sessions"
        :key="s.empNo"
        class="acct-card"
        :class="{ 'is-current': s.empNo === userStore.currentEmpNo }"
      >
        <div class="avatar" :class="{ 'is-admin': s.isAdmin }">{{ (s.name || 'U').slice(0, 1) }}</div>

        <div class="acct-info">
          <div class="acct-line">
            <span class="acct-name">{{ s.name }}</span>
            <span class="acct-no">{{ s.empNo }}</span>
            <el-tag v-if="s.empNo === userStore.currentEmpNo" size="small" type="success" effect="light" round>
              当前
            </el-tag>
          </div>
          <div class="acct-meta">
            {{ s.deptName || '—' }}
            <span class="sep">·</span>
            {{ (s.roleNames || []).join('、') || '未分配角色' }}
          </div>
        </div>

        <div class="acct-ops">
          <el-button
            v-if="s.empNo !== userStore.currentEmpNo"
            size="small"
            type="primary"
            plain
            :loading="switching === s.empNo"
            @click="onSwitch(s)"
          >
            切换
          </el-button>
          <span v-else class="in-use">使用中</span>

          <el-tooltip v-if="s.empNo !== userStore.currentEmpNo" content="移除该账号记录" placement="top">
            <button class="remove-btn" @click="onRemove(s)">
              <el-icon :size="13"><Close /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>

      <el-empty v-if="!userStore.sessions.length" description="暂无可切换的账号" :image-size="70" />
    </div>

    <!-- 登录其他账号 -->
    <div class="add-account">
      <button class="add-toggle" @click="addVisible = !addVisible">
        <el-icon :size="14"><component :is="addVisible ? 'Minus' : 'Plus'" /></el-icon>
        {{ addVisible ? '收起' : '登录其他账号' }}
      </button>

      <div v-if="addVisible" class="add-form">
        <el-input v-model="form.account" placeholder="手机号或员工编号" size="default" clearable @keyup.enter="onAddLogin" />
        <el-input
          v-model="form.password"
          type="password"
          placeholder="登录密码"
          show-password
          size="default"
          @keyup.enter="onAddLogin"
        />
        <el-button type="primary" :loading="adding" @click="onAddLogin">登录并切换</el-button>
      </div>
      <p v-if="addVisible" class="add-tip">登录成功后会自动加入上方账号列表，可随时切回。</p>
    </div>

    <template #footer>
      <div class="footer">
        <el-button text type="danger" @click="onLogoutAll">退出全部账号</el-button>
        <div class="spacer"></div>
        <el-button @click="close">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Plus, Minus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const MAX_SESSIONS = 5

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'switched'])

const userStore = useUserStore()
const switching = ref('')
const adding = ref(false)
const addVisible = ref(false)
const form = reactive({ account: '', password: '' })

function close() {
  emit('update:modelValue', false)
}

function reset() {
  addVisible.value = false
  form.account = ''
  form.password = ''
}

async function onSwitch(s) {
  switching.value = s.empNo
  try {
    const user = await userStore.switchAccount(s.empNo)
    ElMessage.success(`已切换为 ${user.name}（${(user.roles || []).map((r) => r.name).join('、') || '无角色'}）`)
    emit('switched', user)
    close()
  } catch (err) {
    ElMessage.error(err.message || '切换失败')
    if (err.stale) {
      form.account = s.phone || s.empNo
      form.password = ''
      addVisible.value = true
    }
  } finally {
    switching.value = ''
  }
}

async function onRemove(s) {
  try {
    await ElMessageBox.confirm(`移除后需要重新输入密码才能登录 ${s.name}（${s.empNo}），确定移除？`, '移除账号', {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  userStore.removeSession(s.empNo)
  ElMessage.success('已从账号列表移除')
}

async function onAddLogin() {
  if (adding.value) return
  if (!form.account.trim() || !form.password) {
    ElMessage.warning('请输入手机号（或工号）和密码')
    return
  }
  adding.value = true
  try {
    const user = await userStore.login({ mode: 'password', account: form.account.trim(), password: form.password })
    ElMessage.success(`已登录并切换为 ${user.name}`)
    emit('switched', user)
    close()
  } catch (err) {
    ElMessage.error(err.message || '登录失败')
  } finally {
    adding.value = false
  }
}

async function onLogoutAll() {
  try {
    await ElMessageBox.confirm('将退出全部已登录账号，并清空本机保存的账号列表，确定继续？', '退出全部账号', {
      confirmButtonText: '全部退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await userStore.logoutAll()
  emit('switched', null)
  close()
  ElMessage.success('已退出全部账号')
}
</script>

<style scoped>
.dlg-head h3 {
  margin: 0 0 6px;
  font-size: 17px;
  color: #1f2b3d;
}

.dlg-head p {
  margin: 0;
  font-size: 12.5px;
  color: #8b98ab;
}

.acct-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}

.acct-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border: 1px solid #e8edf6;
  border-radius: 10px;
  background: #fff;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.acct-card:hover {
  border-color: #c9d8ff;
  background: #fbfcff;
}

.acct-card.is-current {
  border-color: #b9d3c9;
  background: #f6fbf8;
}

.avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #5b8cff, #2f5fe0);
}

.avatar.is-admin {
  background: linear-gradient(135deg, #f0b429, #d97706);
}

.acct-info {
  flex: 1;
  min-width: 0;
}

.acct-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.acct-name {
  font-size: 14px;
  font-weight: 600;
  color: #273449;
}

.acct-no {
  font-size: 12px;
  color: #8b98ab;
  font-family: ui-monospace, Menlo, Consolas, monospace;
}

.acct-meta {
  margin-top: 3px;
  font-size: 12px;
  color: #97a3b6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.acct-meta .sep {
  color: #ccd4e0;
  margin: 0 2px;
}

.acct-ops {
  display: flex;
  align-items: center;
  gap: 6px;
}

.in-use {
  font-size: 12px;
  color: #4c9a7c;
}

.remove-btn {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #b6c0cf;
  cursor: pointer;
}

.remove-btn:hover {
  background: #fdecec;
  color: #e35d5d;
}

.add-account {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e8edf6;
}

.add-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: var(--brand);
  font-size: 12.5px;
  cursor: pointer;
  padding: 0;
}

.add-form {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.add-form :deep(.el-input) {
  flex: 1;
}

.add-tip {
  margin: 8px 0 0;
  font-size: 11.5px;
  color: #97a3b6;
}

.footer {
  display: flex;
  align-items: center;
  width: 100%;
}

.spacer {
  flex: 1;
}
</style>
