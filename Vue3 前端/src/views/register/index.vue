<template>
  <div class="register-page">
    <!-- 左侧品牌区：与登录页共用同一组件，保证两页视觉一致 -->
    <AuthBrand />

    <section class="form-panel">
      <div class="form-box">
        <div class="form-head">
          <h2>注册账号</h2>
          <p>使用手机号接收验证码，注册后即可登录系统</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleRegister">
          <el-form-item prop="name">
            <el-input v-model="form.name" placeholder="真实姓名" maxlength="20" clearable :prefix-icon="User" />
          </el-form-item>

          <el-form-item prop="deptId">
            <el-tree-select
              v-model="form.deptId"
              :data="deptOptions"
              :props="{ value: 'id', label: 'name', children: 'children' }"
              :loading="deptLoading"
              placeholder="所属部门"
              check-strictly
              :render-after-expand="false"
              :expand-on-click-node="false"
              default-expand-all
              clearable
              style="width: 100%"
            />
            <p v-if="selectedDept?.roleName" class="role-hint">
              该部门对应角色：<b>{{ selectedDept.roleName }}</b>，注册后即拥有该角色的功能权限
            </p>
            <p v-else-if="selectedDept" class="role-hint warn">
              该部门暂未配置角色，注册后需联系管理员在「员工管理」里分配
            </p>
            <p v-if="deptLoadFailed" class="field-tip">
              部门列表加载失败，<el-link type="primary" :underline="false" @click="loadDepartments">重新加载</el-link>
            </p>
          </el-form-item>

          <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="手机号" maxlength="11" clearable :prefix-icon="Iphone" />
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
            <el-link type="primary" :underline="false" @click="fillMockCode">填入</el-link>
          </div>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="设置登录密码（6-32 位）"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item prop="agree">
            <el-checkbox v-model="form.agree">
              我已阅读并同意《用户协议》与《隐私政策》
            </el-checkbox>
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
            {{ loading ? '提交中...' : '注 册' }}
          </el-button>
        </el-form>

        <p class="back-tip">
          已有账号？<el-link type="primary" :underline="false" @click="goLogin">返回登录</el-link>
        </p>

        <div class="notice">
          <p class="notice-title">注册说明</p>
          <ul>
            <li>请如实选择所属部门，注册后即归入该部门并自动获得该部门对应的角色。</li>
            <li>注册页会实时显示部门对应的角色，能看哪些菜单、能做什么操作，都由这个角色决定。</li>
            <li>手机号即登录账号，一个号码只能注册一次；验证码 5 分钟内有效、仅可使用一次。</li>
          </ul>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Iphone, Lock, ChatDotRound } from '@element-plus/icons-vue'
import request from '@/utils/request'
import AuthBrand from '@/components/AuthBrand.vue'

const router = useRouter()
const route = useRoute()

const formRef = ref()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const mockCode = ref('')
const codeLength = ref(6)
const deptOptions = ref([])
const deptLoading = ref(false)
const deptLoadFailed = ref(false)
let timer = null

/** 在部门树里找选中的部门，用于提示该部门对应的角色 */
function findDept(nodes, id) {
  for (const node of nodes || []) {
    if (node.id === id) return node
    const hit = findDept(node.children, id)
    if (hit) return hit
  }
  return null
}

const selectedDept = computed(() => findDept(deptOptions.value, form.deptId))

const form = reactive({
  name: '',
  deptId: null,
  phone: '',
  code: '',
  password: '',
  confirmPassword: '',
  agree: true
})

const rules = {
  name: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为 2-20 个字符', trigger: 'blur' }
  ],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入短信验证码', trigger: ['blur', 'change'] },
    {
      validator: (rule, value, callback) => {
        if (!value) return callback(new Error('请输入短信验证码'))
        if (!new RegExp(`^\\d{${codeLength.value}}$`).test(value)) {
          return callback(new Error(`验证码为 ${codeLength.value} 位数字`))
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  password: [
    { required: true, message: '请设置登录密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: ['blur', 'change'] },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  agree: [
    {
      validator: (rule, value, callback) => {
        if (!value) return callback(new Error('请先同意用户协议'))
        callback()
      },
      trigger: 'change'
    }
  ]
}

onMounted(() => {
  loadDepartments()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

/** 部门接口是公开的，未登录也能取到启用中的部门树 */
async function loadDepartments() {
  deptLoading.value = true
  deptLoadFailed.value = false
  try {
    const list = await request.get('/departments/options')
    deptOptions.value = Array.isArray(list) ? list : []
    if (!deptOptions.value.length) deptLoadFailed.value = true
  } catch {
    deptOptions.value = []
    deptLoadFailed.value = true
  } finally {
    deptLoading.value = false
  }
}

function startCountdown(seconds) {
  countdown.value = seconds
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function sendCode() {
  if (sending.value || countdown.value > 0) return
  const valid = await formRef.value?.validateField('phone').catch(() => false)
  if (!valid) return

  sending.value = true
  try {
    // scene=register：后端只允许未注册的手机号取码
    const res = await request.post('/sms/code', { phone: form.phone.trim(), scene: 'register' })
    if (res?.codeLength) codeLength.value = res.codeLength
    startCountdown(res?.interval || 60)
    if (res?.debugCode) {
      mockCode.value = res.debugCode
      ElMessage.success(`模拟通道验证码：${res.debugCode}`)
    } else {
      ElMessage.success(`验证码已发送至 ${res?.phone || form.phone}`)
    }
  } catch (err) {
    if (!err?.response) ElMessage.error(err?.message || '验证码发送失败')
  } finally {
    sending.value = false
  }
}

async function handleRegister() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await request.post('/auth/register', {
      phone: form.phone.trim(),
      code: form.code.trim(),
      password: form.password,
      name: form.name.trim(),
      deptId: form.deptId
    })
    const who = res?.deptName ? `${res.name}（${res.deptName}）` : res?.name || form.name
    const role = res?.roleName ? `，角色：${res.roleName}` : ''
    ElMessage.success(`注册成功，${who}${role}，请使用手机号 ${res?.phone || form.phone} 登录`)
    router.replace({ path: '/login', query: { account: form.phone.trim() } })
  } catch (err) {
    if (!err?.response) ElMessage.error(err?.message || '注册失败，请重试')
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}

/** 模拟通道一键填入：填入后立即清掉该字段的报错，避免红字残留 */
function fillMockCode() {
  form.code = mockCode.value
  formRef.value?.clearValidate('code')
}
</script>

<style scoped>
.register-page {
  display: flex;
  min-height: 100vh;
  background: #fff;
}

.form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  position: relative;
  /* 内容高于视口时（1366x768 等）可滚动，表单不会被裁掉顶部 */
  overflow-y: auto;
}

.form-box {
  width: 100%;
  max-width: 392px;
  /* 用 auto 外边距垂直居中：内容超高时自动贴顶，不像 justify-content:center 会切掉上边 */
  margin: auto;
}

.form-head {
  margin-bottom: 16px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

/* 错误提示是绝对定位的，留够高度避免压到下一个输入框的边框 */
:deep(.el-form-item__error) {
  font-size: 11.5px;
  padding-top: 3px;
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

.role-hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #5a6a83;
}

.role-hint b {
  color: #2f5fe0;
}

.role-hint.warn {
  color: #d97706;
}

.field-tip {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: #d97706;
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
  margin: -6px 0 14px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f2f8f3;
  border: 1px dashed #b6ddc0;
  font-size: 12.5px;
  color: #2f7d4f;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  letter-spacing: 4px;
  border-radius: 9px;
  margin-top: 4px;
}

.back-tip {
  margin: 14px 0 0;
  text-align: center;
  font-size: 13px;
  color: #8493a8;
}

.notice {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 9px;
  background: #f5f8ff;
  border: 1px dashed #c8d7ff;
  font-size: 12.5px;
  color: #52627a;
}

.notice-title {
  margin: 0 0 6px;
  font-weight: 600;
  color: #3a4a63;
}

.notice ul {
  margin: 0;
  padding-left: 16px;
}

.notice li {
  line-height: 1.7;
}

@media (max-width: 960px) {
  .form-panel {
    width: 100%;
  }
}
</style>
