<template>
  <div class="login-page">
    <!-- 左侧品牌区（与注册页共用） -->
    <AuthBrand />

    <!-- 右侧表单区 -->
    <section class="form-panel">
      <div class="form-box">
        <div class="form-head">
          <h2>欢迎登录</h2>
          <p>{{ mode === 'sms' ? '输入手机号，获取短信验证码即可登录' : '使用手机号（或工号）与密码进入系统' }}</p>
        </div>

        <div class="login-tabs">
          <button :class="['tab', { active: mode === 'password' }]" @click="setMode('password')">密码登录</button>
          <button :class="['tab', { active: mode === 'sms' }]" @click="setMode('sms')">验证码登录</button>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <template v-if="mode === 'password'">
            <el-form-item prop="account">
              <el-input v-model="form.account" placeholder="手机号或员工编号" clearable :prefix-icon="Iphone" />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="登录密码"
                show-password
                :prefix-icon="Lock"
              />
            </el-form-item>
          </template>

          <template v-else>
            <el-form-item prop="phone">
              <el-input
                v-model="form.phone"
                placeholder="手机号"
                maxlength="11"
                clearable
                :prefix-icon="Iphone"
              />
            </el-form-item>

            <el-form-item prop="code">
              <div class="code-row">
                <el-input
                  v-model="form.code"
                  :placeholder="`${codeLength} 位短信验证码`"
                  :maxlength="codeLength"
                  :prefix-icon="ChatDotRound"
                />
                <el-button class="code-btn" :loading="sending" :disabled="countdown > 0" @click="sendCode">
                  {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>

            <div v-if="mockCode" class="mock-code">
              模拟通道未真实发送，验证码：<b>{{ mockCode }}</b>
              <el-link type="primary" :underline="false" @click="form.code = mockCode">填入</el-link>
            </div>
          </template>

          <div class="form-options">
            <el-checkbox v-model="form.remember">记住{{ mode === 'sms' ? '手机号' : '账号' }}</el-checkbox>
            <el-link type="primary" :underline="false" @click="onForgot">忘记密码？</el-link>
          </div>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>

        <p class="register-tip">
          还没有账号？<el-link type="primary" :underline="false" @click="goRegister">立即注册</el-link>
        </p>

        <div class="demo-tip">
          <div class="demo-tip-head">
            <span class="demo-tip-text">演示账号（密码均为 <b>123456</b>）</span>
            <el-link type="primary" :underline="false" @click="openDemo">全部 {{ demoCount }} 个账号 ›</el-link>
          </div>
          <div class="demo-btns">
            <el-button size="small" @click="fillDemo('ADMIN')">管理员 ADMIN</el-button>
            <el-button size="small" @click="fillDemo('EMP002')">销售一组 EMP002</el-button>
          </div>
          <p class="demo-note">所有角色共用这一个登录页，登录后左侧菜单按权限自动收窄</p>
        </div>

        <div v-if="recentSessions.length" class="recent-box">
          <span class="recent-label">最近登录</span>
          <div class="recent-list">
            <button v-for="s in recentSessions" :key="s.empNo" class="recent-chip" @click="switchBack(s)">
              <i class="chip-dot" :class="{ admin: s.isAdmin }"></i>
              {{ s.name }}
              <em>{{ s.empNo }}</em>
            </button>
          </div>
          <p class="recent-note">点击可直接切回该账号（登录状态仍有效时无需输入密码）</p>
        </div>
      </div>

      <p class="copyright">© 2026 CRM System. All rights reserved.</p>
    </section>

    <!-- 演示账号一览 -->
    <el-dialog v-model="demoVisible" width="900px" align-center class="demo-dialog" title="演示账号一览">
      <p class="dlg-tip">
        密码统一为 <b>123456</b>；也可以用「验证码登录」，当前短信通道为模拟模式，验证码会直接提示在登录框下方。
        不同角色登录的是同一套页面，只是左侧菜单、顶部角色与页面按钮会按权限收窄。
      </p>

      <el-table :data="demoAccounts" size="small" v-loading="demoLoading" max-height="420">
        <el-table-column prop="empNo" label="工号" width="82">
          <template #default="{ row }">
            <span class="mono">{{ row.empNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="96" />
        <el-table-column label="登录手机号" width="116">
          <template #default="{ row }">
            <span class="mono">{{ row.phone || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" width="90" />
        <el-table-column label="角色" width="118">
          <template #default="{ row }">
            <el-tag
              v-for="r in row.roleNames"
              :key="r"
              size="small"
              :type="row.isAdmin ? 'warning' : 'info'"
              effect="light"
            >
              {{ r }}
            </el-tag>
            <span v-if="!row.roleNames?.length" class="muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="登录后可见菜单">
          <template #default="{ row }">
            <div class="menu-tags">
              <el-tag v-for="m in row.menus" :key="m" size="small" type="primary" effect="plain">{{ m }}</el-tag>
              <span v-if="!row.menus?.length" class="muted">无（仅可登录）</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="86" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="quickLogin(row)">一键登录</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span class="muted">未获取到账号列表，请确认后端服务已启动</span>
        </template>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import AuthBrand from '@/components/AuthBrand.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const mockCode = ref('')
/** 验证码位数由后端按通道/模板下发（云通讯测试模板为 4 位） */
const codeLength = ref(6)
/** password=密码登录，sms=手机号 + 验证码登录 */
const mode = ref('password')
let timer = null

const REMEMBER_KEY = 'crm_remember_account'
const DEMO_PASSWORD = '123456'

/** 后端不可用时的兜底账号列表（无菜单信息） */
const FALLBACK_ACCOUNTS = [
  { empNo: 'ADMIN', name: '系统管理员', phone: '15236406010', deptName: '总经办', roleNames: ['管理员'], menus: ['工作台', '客户管理', '销售管理', '交付与回款', '售后管理', '设置'], isAdmin: true },
  { empNo: 'EMP001', name: '张明', phone: '13810245566', deptName: '销售部', roleNames: ['销售部'], menus: ['我的待办', '客户管理', '销售管理', '售后管理'] },
  { empNo: 'EMP002', name: '李思', phone: '13922887711', deptName: '销售一组', roleNames: ['销售一组'], menus: ['我的待办', '客户管理', '销售管理', '售后管理'] },
  { empNo: 'EMP003', name: '周婷', phone: '13766003322', deptName: '销售二组', roleNames: ['销售二组'], menus: ['我的待办', '客户管理', '销售管理', '售后管理'] },
  { empNo: 'EMP004', name: '陈默', phone: '13588991200', deptName: '市场中心', roleNames: ['营销'], menus: ['工作台', '客户管理', '销售管理', '售后管理'] },
  { empNo: 'EMP005', name: '王芳', phone: '13677884455', deptName: '市场部', roleNames: ['营销'], menus: ['工作台', '客户管理', '销售管理', '售后管理'] },
  { empNo: 'EMP006', name: '刘洋', phone: '15066772233', deptName: '财务部', roleNames: ['财务'], menus: ['工作台', '客户管理', '销售管理', '交付与回款'] },
  { empNo: 'EMP007', name: '吴磊', phone: '13122339900', deptName: '技术部', roleNames: ['售后'], menus: ['工作台', '客户管理', '销售管理', '交付与回款', '售后管理'] },
  { empNo: 'EMP008', name: '孙浩', phone: '18855667788', deptName: '售后部', roleNames: ['售后'], menus: ['工作台', '客户管理', '销售管理', '交付与回款', '售后管理'] },
  { empNo: 'EMP009', name: '赵敏', phone: '13099887766', deptName: '生产部', roleNames: ['生产'], menus: ['工作台', '销售管理', '交付与回款'] }
]

const demoVisible = ref(false)
const demoAccounts = ref([])
const demoLoading = ref(false)
const demoCount = computed(() => demoAccounts.value.length || FALLBACK_ACCOUNTS.length)
/** 本机登录过的其他账号，可直接切回 */
const recentSessions = computed(() => userStore.sessions)

const form = reactive({
  account: '',
  password: '',
  phone: '',
  code: '',
  remember: true
})

const rules = computed(() => {
  if (mode.value === 'sms') {
    return {
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
      ],
      code: [
        { required: true, message: '请输入短信验证码', trigger: 'blur' },
        { pattern: new RegExp(`^\\d{${codeLength.value}}$`), message: `验证码为 ${codeLength.value} 位数字`, trigger: 'blur' }
      ]
    }
  }
  return {
    account: [
      { required: true, message: '请输入手机号或员工编号', trigger: 'blur' },
      { min: 3, message: '账号至少 3 位', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入登录密码', trigger: 'blur' },
      { min: 6, message: '密码至少 6 位', trigger: 'blur' }
    ]
  }
})

onMounted(() => {
  // 注册成功后跳回登录页会带 ?account=手机号，直接回填，省得手输
  const fromRegister = typeof route.query.account === 'string' ? route.query.account : ''
  const saved = fromRegister || localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    if (/^1[3-9]\d{9}$/.test(saved)) {
      form.account = saved
      form.phone = saved
    } else {
      form.account = saved
    }
  }
  loadDemoAccounts()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

async function loadDemoAccounts() {
  demoLoading.value = true
  try {
    const list = await request.get('/auth/demo-accounts')
    demoAccounts.value = Array.isArray(list) && list.length ? list : FALLBACK_ACCOUNTS
  } catch {
    demoAccounts.value = FALLBACK_ACCOUNTS
  } finally {
    demoLoading.value = false
  }
}

function openDemo() {
  demoVisible.value = true
  if (!demoAccounts.value.length) loadDemoAccounts()
}

function setMode(next) {
  if (mode.value === next) return
  mode.value = next
  mockCode.value = ''
  form.code = ''
  formRef.value?.clearValidate()
}

function startCountdown(seconds) {
  countdown.value = seconds > 0 ? seconds : 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
      countdown.value = 0
    }
  }, 1000)
}

/** 请求拦截器已提示过接口错误，这里只兜网络异常，避免重复弹两条 */
function notifyError(err, fallback) {
  if (!err?.response) ElMessage.error(err?.message || fallback)
}

async function sendCode() {
  if (sending.value || countdown.value > 0) return
  const valid = await formRef.value?.validateField('phone').catch(() => false)
  if (!valid) return

  sending.value = true
  try {
    const res = await request.post('/sms/code', { phone: form.phone.trim(), scene: 'login' })
    if (res?.codeLength) codeLength.value = res.codeLength
    startCountdown(res?.interval || 60)
    if (res?.debugCode) {
      mockCode.value = res.debugCode
      ElMessage.success(`模拟通道验证码：${res.debugCode}`)
    } else {
      ElMessage.success(`验证码已发送至 ${res?.phone || form.phone}`)
    }
  } catch (err) {
    notifyError(err, '验证码发送失败')
  } finally {
    sending.value = false
  }
}

async function handleLogin() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const payload =
      mode.value === 'sms'
        ? { mode: 'sms', phone: form.phone.trim(), code: form.code.trim() }
        : { mode: 'password', account: form.account.trim(), password: form.password }

    const info = await userStore.login(payload)
    const remembered = mode.value === 'sms' ? form.phone.trim() : form.account.trim()
    if (form.remember) localStorage.setItem(REMEMBER_KEY, remembered)
    else localStorage.removeItem(REMEMBER_KEY)

    ElMessage.success(`欢迎回来，${info.name}`)
    if (demoVisible.value) demoVisible.value = false
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (err) {
    notifyError(err, '登录失败，请重试')
  } finally {
    loading.value = false
  }
}

/** 演示账号一键登录：手机号 + 统一密码 123456 */
async function quickLogin(row) {
  if (loading.value) return
  if (!row.phone) {
    ElMessage.warning('该演示账号未绑定手机号，无法一键登录')
    return
  }
  loading.value = true
  try {
    const info = await userStore.login({ mode: 'password', account: row.phone, password: DEMO_PASSWORD })
    localStorage.setItem(REMEMBER_KEY, row.phone)
    demoVisible.value = false
    ElMessage.success(`已以「${row.roleNames?.[0] || ''}」身份登录：${info.name}`)
    router.replace('/')
  } catch (err) {
    notifyError(err, '登录失败')
  } finally {
    loading.value = false
  }
}

/** 切回本机已登录过的账号，无需重新输入密码 */
async function switchBack(s) {
  try {
    const user = await userStore.switchAccount(s.empNo)
    ElMessage.success(`已切换为 ${user.name}`)
    router.replace('/')
  } catch (err) {
    ElMessage.error(err.message || '切换失败')
    if (err.stale) {
      form.account = s.empNo
      form.password = ''
      form.phone = s.phone || ''
    }
  }
}

function fillDemo(account = '18800000001') {
  setMode('password')
  form.account = account
  form.password = DEMO_PASSWORD
}

function onForgot() {
  ElMessage.info('请联系系统管理员重置密码')
}

function goRegister() {
  router.push('/register')
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #fff;
}

/* ---------- 表单区 ---------- */
.form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  position: relative;
}

.form-box {
  width: 100%;
  max-width: 372px;
}

.form-head {
  margin-bottom: 30px;
}

.form-head h2 {
  font-size: 26px;
  margin: 0 0 10px;
  color: #1f2b3d;
  letter-spacing: 0.5px;
}

.form-head p {
  margin: 0;
  font-size: 13.5px;
  color: #8493a8;
}

/* ---------- 登录方式切换 ---------- */
.login-tabs {
  display: flex;
  gap: 4px;
  padding: 3px;
  margin-bottom: 20px;
  background: #f4f7fc;
  border-radius: 9px;
}

.login-tabs .tab {
  flex: 1;
  height: 34px;
  border: none;
  border-radius: 7px;
  background: transparent;
  font-size: 13.5px;
  color: #6b7a90;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}

.login-tabs .tab:hover {
  color: var(--brand);
}

.login-tabs .tab.active {
  background: #fff;
  color: var(--brand);
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(31, 43, 61, 0.08);
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-row :deep(.el-input) {
  flex: 1;
}

.code-btn {
  min-width: 112px;
  margin-left: 0;
}

.mock-code {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: -8px 0 14px;
  font-size: 12px;
  color: #8b98ab;
}

.mock-code b {
  color: var(--brand);
  letter-spacing: 1px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 22px;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  letter-spacing: 4px;
  border-radius: 9px;
}

.register-tip {
  margin: 14px 0 0;
  text-align: center;
  font-size: 13px;
  color: #8493a8;
}

.demo-tip {
  margin-top: 22px;
  padding: 12px 14px;
  border-radius: 9px;
  background: #f5f8ff;
  border: 1px dashed #c8d7ff;
  font-size: 12.5px;
  color: #52627a;
}

.demo-tip-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.demo-tip-head :deep(.el-link) {
  font-size: 12.5px;
  font-weight: 500;
}

.demo-btns {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.demo-btns .el-button {
  flex: 1;
  margin-left: 0;
  font-size: 12px;
}

.demo-tip-text b {
  color: var(--brand);
  font-weight: 600;
}

.demo-note {
  margin: 9px 0 0;
  font-size: 11.5px;
  color: #93a1b5;
  line-height: 1.5;
}

/* ---------- 最近登录的账号 ---------- */
.recent-box {
  margin-top: 16px;
  padding: 11px 12px;
  border-radius: 9px;
  border: 1px solid #eef1f7;
  background: #fcfdff;
}

.recent-label {
  font-size: 11.5px;
  color: #97a3b6;
}

.recent-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 8px;
}

.recent-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 9px;
  border: 1px solid #e2e9f5;
  border-radius: 20px;
  background: #fff;
  font-size: 12px;
  color: #3c4a62;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}

.recent-chip:hover {
  border-color: var(--brand);
  color: var(--brand);
  box-shadow: 0 2px 8px rgba(47, 95, 224, 0.1);
}

.recent-chip em {
  font-style: normal;
  font-size: 11px;
  color: #a5b0c2;
}

.chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #5b8cff;
}

.chip-dot.admin {
  background: #e0a52a;
}

.recent-note {
  margin: 8px 0 0;
  font-size: 11px;
  color: #aab4c4;
  line-height: 1.5;
}

/* ---------- 演示账号弹窗 ---------- */
.dlg-tip {
  margin: -4px 0 14px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f5f8ff;
  border: 1px solid #e2ebff;
  font-size: 12.5px;
  line-height: 1.7;
  color: #52627a;
}

.dlg-tip b {
  color: var(--brand);
}

.mono {
  font-family: ui-monospace, Menlo, Consolas, monospace;
  font-size: 12px;
  color: #3c4a62;
}

.muted {
  font-size: 12px;
  color: #aab4c4;
}

.menu-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.menu-tags :deep(.el-tag) {
  height: 20px;
  padding: 0 7px;
  font-size: 11.5px;
}

.demo-dialog :deep(.el-table .cell) {
  line-height: 1.5;
}

.copyright {
  position: absolute;
  bottom: 26px;
  font-size: 12px;
  color: #b3bdcc;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  padding: 4px 12px;
  border-radius: 9px;
  box-shadow: 0 0 0 1px #dfe5ef inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--brand) inset;
}

@media (max-width: 960px) {
  .form-panel {
    width: 100%;
  }
}
</style>
